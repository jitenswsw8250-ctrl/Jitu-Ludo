package com.example.logic

import com.example.model.CaptureAnimationEvent
import com.example.model.DiceState
import com.example.model.GameState
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.Token
import com.example.model.TokenPosition
import com.example.model.TurnPhase
import kotlin.random.Random

data class MoveOutcome(
    val updatedState: GameState,
    val capturedToken: Token? = null,
    val hasBonusTurn: Boolean = false,
    val bonusReason: String? = null,
    val captureEvent: CaptureAnimationEvent? = null
)

object LudoEngine {

    /**
     * Determines whether a given token can move with the given dice roll.
     */
    fun canTokenMove(token: Token, diceRoll: Int): Boolean {
        if (token.isFinished) return false
        return when (val pos = token.position) {
            is TokenPosition.InBase -> diceRoll == 6
            is TokenPosition.OnBoard -> (pos.step + diceRoll) <= LudoBoardMap.MAX_STEPS
            is TokenPosition.Finished -> false
        }
    }

    /**
     * Returns list of tokens that can legally move for the given player and dice roll.
     */
    fun getMovableTokens(player: Player, diceRoll: Int): List<Token> {
        if (player.isFinished) return emptyList()
        return player.tokens.filter { canTokenMove(it, diceRoll) }
    }

    /**
     * Rolls the dice (1..6) and returns updated state with turn phase and movable tokens.
     *
     * STRICT CONSECUTIVE-SIX RULE:
     * - If the same player rolls a six twice consecutively (consecutiveSixes >= 2),
     *   their third consecutive roll MUST NOT be six.
     * - On the third roll, only dice values 1, 2, 3, 4, or 5 are allowed.
     * - Consecutive sixes are tracked separately for each player and reset when a non-six
     *   is rolled or the turn changes.
     */
    fun rollDice(state: GameState, forcedRoll: Int? = null): GameState {
        val player = state.currentPlayer ?: return state
        val currentConsecutive = player.consecutiveSixes.coerceAtLeast(state.diceState.consecutiveSixes)

        // Enforce strict consecutive-six rule:
        // If current player already rolled two 6s, their third roll MUST NOT be 6. Allow ONLY 1..5.
        val rollValue = if (currentConsecutive >= 2) {
            if (forcedRoll != null && forcedRoll in 1..5) {
                forcedRoll
            } else {
                // Strictly disallow 6; select randomly from 1, 2, 3, 4, 5
                Random.nextInt(1, 6)
            }
        } else {
            forcedRoll ?: Random.nextInt(1, 7)
        }

        val newConsecutiveSixes = if (rollValue == 6) currentConsecutive + 1 else 0

        val updatedDice = DiceState(
            value = rollValue,
            isRolling = false,
            consecutiveSixes = newConsecutiveSixes
        )

        val updatedPlayers = state.players.mapIndexed { idx, p ->
            if (idx == state.currentTurnIndex) p.copy(consecutiveSixes = newConsecutiveSixes)
            else p
        }

        val movable = getMovableTokens(updatedPlayers[state.currentTurnIndex], rollValue)
        val movableIds = movable.map { it.id }.toSet()

        return if (movable.isEmpty()) {
            // No legal moves: show roll result then transition turn
            state.copy(
                players = updatedPlayers,
                diceState = DiceState(value = rollValue, isRolling = false, consecutiveSixes = 0),
                phase = TurnPhase.TURN_TRANSITION,
                movableTokenIds = emptySet(),
                statusMessage = "${player.name} rolled $rollValue. No legal moves available!"
            )
        } else {
            val msg = if (movable.size == 1) {
                if (player.isBot) "${player.name} rolled $rollValue. Bot moving..."
                else "${player.name} rolled $rollValue. Auto-moving token..."
            } else {
                "${player.name} rolled $rollValue. Select a token to move!"
            }
            state.copy(
                players = updatedPlayers,
                diceState = updatedDice,
                phase = TurnPhase.SELECTING_TOKEN,
                movableTokenIds = movableIds,
                statusMessage = msg
            )
        }
    }

    /**
     * Executes moving the specified token for the current player.
     */
    fun executeMove(state: GameState, tokenId: Int): MoveOutcome {
        val player = state.currentPlayer ?: return MoveOutcome(state)
        val token = player.tokens.find { it.id == tokenId } ?: return MoveOutcome(state)
        val roll = state.diceState.value

        if (!canTokenMove(token, roll)) return MoveOutcome(state)

        // Calculate new position
        val newPosition: TokenPosition = when (val pos = token.position) {
            is TokenPosition.InBase -> TokenPosition.OnBoard(step = 0)
            is TokenPosition.OnBoard -> {
                val nextStep = pos.step + roll
                if (nextStep == LudoBoardMap.MAX_STEPS) {
                    TokenPosition.Finished
                } else {
                    TokenPosition.OnBoard(step = nextStep)
                }
            }
            is TokenPosition.Finished -> TokenPosition.Finished
        }

        val updatedToken = token.copy(position = newPosition)
        val updatedTokens = player.tokens.map { if (it.id == tokenId) updatedToken else it }
        var updatedPlayer = player.copy(tokens = updatedTokens)

        // Check if token reached finish
        val justFinishedToken = (token.position !is TokenPosition.Finished) && (newPosition is TokenPosition.Finished)

        // Check captures
        var capturedToken: Token? = null
        val updatedPlayers = state.players.map { p ->
            if (p.color == player.color) {
                updatedPlayer
            } else {
                p
            }
        }.toMutableList()

        if (newPosition is TokenPosition.OnBoard && newPosition.step < 51) {
            val landTrackIndex = LudoBoardMap.getTrackIndex(player.color, newPosition.step)
            val isSafe = LudoBoardMap.isSafeSquare(player.color, newPosition.step)

            if (landTrackIndex != null && !isSafe) {
                // Check all opponent tokens on this track cell
                for (i in updatedPlayers.indices) {
                    val otherPlayer = updatedPlayers[i]
                    if (otherPlayer.color == player.color || otherPlayer.isFinished) continue

                    var capturedInThisPlayer = false
                    val modifiedOtherTokens = otherPlayer.tokens.map { t ->
                        val tStep = (t.position as? TokenPosition.OnBoard)?.step
                        if (tStep != null && tStep < 51) {
                            val otherTrack = LudoBoardMap.getTrackIndex(otherPlayer.color, tStep)
                            if (otherTrack == landTrackIndex) {
                                capturedToken = t
                                capturedInThisPlayer = true
                                t.copy(position = TokenPosition.InBase)
                            } else {
                                t
                            }
                        } else {
                            t
                        }
                    }
                    if (capturedInThisPlayer) {
                        updatedPlayers[i] = otherPlayer.copy(
                            tokens = modifiedOtherTokens,
                            losses = otherPlayer.losses + 1
                        )
                    } else {
                        updatedPlayers[i] = otherPlayer.copy(tokens = modifiedOtherTokens)
                    }
                }
            }
        }

        // Increment capturing player's captures counter if capture happened
        if (capturedToken != null) {
            val curIdx = updatedPlayers.indexOfFirst { it.color == player.color }
            if (curIdx >= 0) {
                updatedPlayers[curIdx] = updatedPlayers[curIdx].copy(
                    captures = updatedPlayers[curIdx].captures + 1
                )
            }
        }

        // Check player victory status
        val podium = state.winnerPodium.toMutableList()
        val currentPlayerIndex = updatedPlayers.indexOfFirst { it.color == player.color }
        val playerAllHome = updatedPlayers[currentPlayerIndex].isFinished

        if (playerAllHome && updatedPlayers[currentPlayerIndex].rank == null) {
            val rank = podium.size + 1
            updatedPlayers[currentPlayerIndex] = updatedPlayers[currentPlayerIndex].copy(rank = rank)
            podium.add(updatedPlayers[currentPlayerIndex])
        }

        // Determine bonus turn
        var bonusTurn = false
        var bonusReason: String? = null

        if (!playerAllHome) {
            if (roll == 6) {
                bonusTurn = true
                bonusReason = "Rolled a 6! Extra turn!"
            } else if (capturedToken != null) {
                bonusTurn = true
                bonusReason = "Captured ${capturedToken?.color?.title} token! Extra turn!"
            } else if (justFinishedToken) {
                bonusTurn = true
                bonusReason = "Token reached Home! Extra turn!"
            }
        }

        // Check game over (e.g. only 1 active player left or 2-player game winner)
        val activeRemaining = updatedPlayers.count { !it.isFinished }
        val isGameOver = (updatedPlayers.size == 2 && podium.isNotEmpty()) || (activeRemaining <= 1)

        val nextTurnIndex = if (bonusTurn && !playerAllHome) {
            state.currentTurnIndex
        } else {
            getNextActivePlayerIndex(updatedPlayers, state.currentTurnIndex)
        }

        val nextPlayer = updatedPlayers[nextTurnIndex]
        val statusMsg = when {
            isGameOver -> "Game Over! ${podium.firstOrNull()?.name ?: "Winner"} wins the crown!"
            bonusTurn -> "${player.name} gets a bonus turn! ($bonusReason)"
            else -> "${player.name} moved. Next is ${nextPlayer.name}'s turn."
        }

        val nextDiceState = if (bonusTurn && roll == 6) {
            state.diceState.copy(isRolling = false)
        } else {
            DiceState(value = state.diceState.value, isRolling = false, consecutiveSixes = 0)
        }

        // Feature 1: Build CaptureAnimationEvent when a token is legally captured
        var captureEvent: CaptureAnimationEvent? = null
        if (capturedToken != null && newPosition is TokenPosition.OnBoard && newPosition.step < 51) {
            val landTrackIndex = LudoBoardMap.getTrackIndex(player.color, newPosition.step)
            val gridCoord = landTrackIndex?.let { LudoBoardMap.OUTER_TRACK.getOrNull(it) }
            if (gridCoord != null) {
                val capturePoint = FloatPoint(gridCoord.col + 0.5f, gridCoord.row + 0.5f)
                val capturedPlayer = updatedPlayers.find { it.color == capturedToken.color }
                val baseSlot = LudoBoardMap.getBaseSlotCoordinate(capturedToken.color, capturedToken.id)
                val celebratoryOptions = listOf("😎", "🔥")
                val cryingOptions = listOf("😭", "😵‍💫")
                captureEvent = CaptureAnimationEvent(
                    eventId = System.currentTimeMillis(),
                    capturingPlayerColor = player.color,
                    capturingPlayerName = player.name,
                    capturingTokenId = tokenId,
                    capturedPlayerColor = capturedToken.color,
                    capturedPlayerName = capturedPlayer?.name ?: capturedToken.color.title,
                    capturedTokenId = capturedToken.id,
                    captureBoardCoord = capturePoint,
                    capturedBaseCoord = baseSlot,
                    capturingEmoji = celebratoryOptions.random(),
                    capturedEmoji = cryingOptions.random()
                )
            }
        }

        val playersWithConsecutiveTracked = updatedPlayers.mapIndexed { idx, p ->
            if (idx == nextTurnIndex) {
                // If same player gets bonus turn from rolling a 6, preserve their count
                if (bonusTurn && roll == 6 && nextTurnIndex == state.currentTurnIndex) {
                    p.copy(consecutiveSixes = state.diceState.consecutiveSixes)
                } else {
                    p.copy(consecutiveSixes = 0)
                }
            } else {
                p.copy(consecutiveSixes = 0)
            }
        }

        val playersWithWinChances = calculateLiveWinChances(playersWithConsecutiveTracked)

        val newState = state.copy(
            players = playersWithWinChances,
            currentTurnIndex = nextTurnIndex,
            phase = TurnPhase.WAITING_FOR_ROLL,
            diceState = nextDiceState,
            movableTokenIds = emptySet(),
            statusMessage = statusMsg,
            winnerPodium = podium,
            isGameOver = isGameOver,
            captureEvent = captureEvent,
            extraTurnReason = if (bonusTurn) bonusReason else null
        )

        return MoveOutcome(
            updatedState = newState,
            capturedToken = capturedToken,
            hasBonusTurn = bonusTurn,
            bonusReason = bonusReason,
            captureEvent = captureEvent
        )
    }

    /**
     * Gets the index of the next player who has not finished all their tokens.
     */
    fun getNextActivePlayerIndex(players: List<Player>, currentIndex: Int): Int {
        if (players.isEmpty()) return 0
        var next = (currentIndex + 1) % players.size
        var checks = 0
        while (players[next].isFinished && checks < players.size) {
            next = (next + 1) % players.size
            checks++
        }
        return next
    }

    /**
     * Smart Bot AI decision logic with Easy, Medium, and Hard difficulty modes.
     */
    fun pickBestBotToken(
        state: GameState,
        botPlayer: Player,
        diceRoll: Int,
        difficulty: com.example.model.AiDifficulty = state.aiDifficulty
    ): Token? {
        val movable = getMovableTokens(botPlayer, diceRoll)
        if (movable.isEmpty()) return null
        if (movable.size == 1) return movable.first()

        return when (difficulty) {
            com.example.model.AiDifficulty.EASY -> {
                // Easy AI: casual, simple decision-making with occasional random moves
                if (kotlin.random.Random.nextFloat() < 0.40f) {
                    movable.random()
                } else {
                    movable.maxByOrNull { t ->
                        when (val pos = t.position) {
                            is TokenPosition.InBase -> 60.0
                            is TokenPosition.OnBoard -> pos.step.toDouble() + if (pos.step + diceRoll == LudoBoardMap.MAX_STEPS) 300.0 else 0.0
                            is TokenPosition.Finished -> 0.0
                        }
                    } ?: movable.first()
                }
            }
            com.example.model.AiDifficulty.MEDIUM -> {
                scoreMovableTokens(state, botPlayer, movable, diceRoll, isHard = false)
            }
            com.example.model.AiDifficulty.HARD -> {
                scoreMovableTokens(state, botPlayer, movable, diceRoll, isHard = true)
            }
        }
    }

    private fun scoreMovableTokens(
        state: GameState,
        botPlayer: Player,
        movable: List<Token>,
        diceRoll: Int,
        isHard: Boolean
    ): Token {
        var bestToken = movable.first()
        var maxScore = Double.NEGATIVE_INFINITY

        for (token in movable) {
            var score = 0.0

            when (val pos = token.position) {
                is TokenPosition.InBase -> {
                    val activeTokens = botPlayer.tokens.count { it.position is TokenPosition.OnBoard }
                    score += if (isHard) {
                        when (activeTokens) {
                            0 -> 1000.0
                            1 -> 650.0
                            else -> 400.0
                        }
                    } else {
                        if (activeTokens == 0) 700.0 else 400.0
                    }
                }
                is TokenPosition.OnBoard -> {
                    val nextStep = pos.step + diceRoll

                    // 1. Can finish home?
                    if (nextStep == LudoBoardMap.MAX_STEPS) {
                        score += if (isHard) 3000.0 else 1000.0
                    }

                    // 2. Can enter home lane (safe from capture)?
                    if (pos.step < 51 && nextStep >= 51) {
                        score += if (isHard) 1600.0 else 600.0
                    }

                    // 3. Can capture an opponent?
                    if (nextStep < 51) {
                        val targetTrack = LudoBoardMap.getTrackIndex(botPlayer.color, nextStep)
                        val isSafe = LudoBoardMap.isSafeSquare(botPlayer.color, nextStep)
                        if (targetTrack != null && !isSafe) {
                            var canCapture = false
                            for (other in state.players) {
                                if (other.color == botPlayer.color || other.isFinished) continue
                                for (otherToken in other.tokens) {
                                    val otherStep = (otherToken.position as? TokenPosition.OnBoard)?.step
                                    if (otherStep != null && otherStep < 51) {
                                        val otherTrack = LudoBoardMap.getTrackIndex(other.color, otherStep)
                                        if (otherTrack == targetTrack) {
                                            canCapture = true
                                            break
                                        }
                                    }
                                }
                                if (canCapture) break
                            }
                            if (canCapture) {
                                score += if (isHard) 2500.0 else 1300.0
                            }

                            // On Hard mode: check if landing square is vulnerable to an opponent 1..6 steps behind
                            if (isHard && !canCapture) {
                                for (other in state.players) {
                                    if (other.color == botPlayer.color || other.isFinished) continue
                                    for (otherToken in other.tokens) {
                                        val otherStep = (otherToken.position as? TokenPosition.OnBoard)?.step
                                        if (otherStep != null && otherStep < 51) {
                                            val otherTrack = LudoBoardMap.getTrackIndex(other.color, otherStep)
                                            if (otherTrack != null) {
                                                val distToTarget = (targetTrack - otherTrack + LudoBoardMap.TRACK_LENGTH) % LudoBoardMap.TRACK_LENGTH
                                                if (distToTarget in 1..6) {
                                                    score -= 800.0 // Vulnerability penalty on Hard!
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Land on a safe star square?
                        if (isSafe) {
                            score += if (isHard) 900.0 else 300.0
                        }

                        // 5. Escaping danger from current square?
                        val curTrack = LudoBoardMap.getTrackIndex(botPlayer.color, pos.step)
                        val curSafe = LudoBoardMap.isSafeSquare(botPlayer.color, pos.step)
                        if (curTrack != null && !curSafe) {
                            var inDanger = false
                            for (other in state.players) {
                                if (other.color == botPlayer.color || other.isFinished) continue
                                for (otherToken in other.tokens) {
                                    val otherStep = (otherToken.position as? TokenPosition.OnBoard)?.step
                                    if (otherStep != null && otherStep < 51) {
                                        val otherTrack = LudoBoardMap.getTrackIndex(other.color, otherStep)
                                        if (otherTrack != null) {
                                            val dist = (curTrack - otherTrack + LudoBoardMap.TRACK_LENGTH) % LudoBoardMap.TRACK_LENGTH
                                            if (dist in 1..6) {
                                                inDanger = true
                                                break
                                            }
                                        }
                                    }
                                }
                                if (inDanger) break
                            }
                            if (inDanger) {
                                score += if (isHard) 1200.0 else 400.0
                            }
                        }
                    }

                    // 6. Advancement progress bonus
                    score += nextStep * (if (isHard) 6.0 else 3.5)
                }
                is TokenPosition.Finished -> {}
            }

            if (score > maxScore) {
                maxScore = score
                bestToken = token
            }
        }

        return bestToken
    }

    /**
     * Calculates live win probability percentage for each active player.
     * At game start, every active player has an exactly equal chance:
     * - 2 players: 50% each
     * - 3 players: 33.3% each
     * - 4 players: 25% each
     * As tokens advance, safe squares are reached, captures are made, or tokens enter home,
     * the live probability updates dynamically and normalizes to approximately 100%.
     */
    fun calculateLiveWinChances(players: List<Player>): List<Player> {
        if (players.isEmpty()) return players

        // If a player has finished all tokens, they are the confirmed winner (100%)
        val winner = players.firstOrNull { it.isFinished }
        if (winner != null) {
            return players.map { p ->
                val isWinner = p.color == winner.color
                p.copy(
                    liveWinChancePercent = if (isWinner) 100 else 0,
                    estimatedWinChanceText = if (isWinner) "100%" else "0%"
                )
            }
        }

        // Check if game has not started yet (all tokens in base for all players, 0 captures, 0 losses)
        val isGameStart = players.all { p ->
            p.tokens.all { it.isInBase } && p.captures == 0 && p.losses == 0
        }

        if (isGameStart) {
            return players.map { it.copy(liveWinChancePercent = 0, estimatedWinChanceText = "0%") }
        }

        val scores = players.map { player ->
            var score = 5.0 // Baseline
            for (token in player.tokens) {
                when (val pos = token.position) {
                    is TokenPosition.InBase -> {}
                    is TokenPosition.OnBoard -> {
                        score += 15.0 // Token successfully moved out of base onto board
                        score += pos.step * 1.5
                        if (LudoBoardMap.isSafeSquare(player.color, pos.step)) {
                            score += 5.0
                        }
                        if (pos.step in 51..55) {
                            score += 15.0 // Safe inside home lane
                        }
                    }
                    is TokenPosition.Finished -> {
                        score += 85.0
                    }
                }
            }
            score += player.captures * 15.0
            score -= player.losses * 3.0
            score.coerceAtLeast(1.0)
        }

        val totalScore = scores.sum()
        if (totalScore <= 0.0) {
            val equal = 100 / players.size
            return players.map { it.copy(liveWinChancePercent = equal, estimatedWinChanceText = "$equal%") }
        }

        val rawPercents = scores.map { (it / totalScore * 100.0).toInt() }
        val sumRaw = rawPercents.sum()
        val diff = 100 - sumRaw

        return players.mapIndexed { index, player ->
            val pct = if (index == 0) (rawPercents[index] + diff).coerceIn(0, 100) else rawPercents[index].coerceIn(0, 100)
            player.copy(
                liveWinChancePercent = pct,
                estimatedWinChanceText = "$pct%"
            )
        }
    }
}
