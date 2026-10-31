package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameHistoryRepository
import com.example.data.GameStateRepository
import com.example.data.PlayerStatsRecord
import com.example.data.SettingsRepository
import com.example.data.StatsRepository
import com.example.logic.LudoEngine
import com.example.model.AiDifficulty
import com.example.model.AnimationSpeed
import com.example.model.BoardTheme
import com.example.model.DiceState
import com.example.model.EmojiReactionEvent
import com.example.model.GameMode
import com.example.model.GameSkin
import com.example.model.GameState
import com.example.model.MatchHistoryRecord
import com.example.model.MatchPlayerSummary
import com.example.model.Player
import com.example.model.PlayerAvatar
import com.example.model.PlayerColor
import com.example.model.TokenPosition
import com.example.model.TurnPhase
import com.example.sound.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class LudoViewModel(application: Application) : AndroidViewModel(application) {

    private val soundManager = SoundManager(application)
    private val statsRepository = StatsRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val gameStateRepository = GameStateRepository(application)
    private val gameHistoryRepository = GameHistoryRepository(application)

    private val _gameState = MutableStateFlow(
        GameState(
            soundEnabled = settingsRepository.isSoundEnabled(),
            hapticsEnabled = settingsRepository.isHapticsEnabled(),
            animationSpeed = settingsRepository.getAnimationSpeed(),
            currentSkin = settingsRepository.getSelectedSkin(),
            boardTheme = settingsRepository.getBoardTheme(),
            aiDifficulty = settingsRepository.getAiDifficulty()
        )
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _playerStats = MutableStateFlow<List<PlayerStatsRecord>>(emptyList())
    val playerStats: StateFlow<List<PlayerStatsRecord>> = _playerStats.asStateFlow()

    private val _matchHistory = MutableStateFlow<List<MatchHistoryRecord>>(emptyList())
    val matchHistory: StateFlow<List<MatchHistoryRecord>> = _matchHistory.asStateFlow()

    private val _hasSavedGame = MutableStateFlow(false)
    val hasSavedGame: StateFlow<Boolean> = _hasSavedGame.asStateFlow()

    private var botJob: Job? = null
    private var autoMoveJob: Job? = null
    private var captureDismissJob: Job? = null
    private var moveJob: Job? = null
    private var reactionDismissJob: Job? = null

    @Volatile
    private var isActionLocked = false

    @Volatile
    private var hasRecordedGameCompletion = false

    init {
        // Load persistent stats and match history
        _playerStats.value = statsRepository.getAllStats()
        _matchHistory.value = gameHistoryRepository.getHistory()
        _hasSavedGame.value = gameStateRepository.hasActiveGame()

        // Check if unfinished match exists
        if (gameStateRepository.hasActiveGame()) {
            val loaded = gameStateRepository.loadActiveGame()
            if (loaded != null && !loaded.isGameOver) {
                _gameState.value = loaded
                _hasSavedGame.value = true
            } else {
                gameStateRepository.clearActiveGame()
                _hasSavedGame.value = false
                initDefaultGame()
            }
        } else {
            initDefaultGame()
        }
    }

    private fun initDefaultGame() {
        startNewGame(
            mode = GameMode.LOCAL_MULTIPLAYER,
            playerCount = 2,
            humanColor = PlayerColor.RED,
            customNames = mapOf(
                PlayerColor.RED to settingsRepository.getPlayerName(PlayerColor.RED, "Player 1"),
                PlayerColor.YELLOW to settingsRepository.getPlayerName(PlayerColor.YELLOW, "Player 2")
            ),
            customAvatars = mapOf(
                PlayerColor.RED to settingsRepository.getPlayerAvatar(PlayerColor.RED),
                PlayerColor.YELLOW to settingsRepository.getPlayerAvatar(PlayerColor.YELLOW)
            ),
            difficulty = settingsRepository.getAiDifficulty(),
            saveImmediately = false
        )
    }

    fun startNewGame(
        mode: GameMode,
        playerCount: Int,
        humanColor: PlayerColor = PlayerColor.RED,
        customNames: Map<PlayerColor, String> = emptyMap(),
        customAvatars: Map<PlayerColor, PlayerAvatar> = emptyMap(),
        difficulty: AiDifficulty = settingsRepository.getAiDifficulty(),
        saveImmediately: Boolean = true
    ) {
        botJob?.cancel()
        autoMoveJob?.cancel()
        captureDismissJob?.cancel()
        moveJob?.cancel()
        reactionDismissJob?.cancel()
        isActionLocked = false
        hasRecordedGameCompletion = false

        val selectedColors = if (mode == GameMode.VS_COMPUTER) {
            when (playerCount) {
                2 -> {
                    val opposite = when (humanColor) {
                        PlayerColor.RED -> PlayerColor.YELLOW
                        PlayerColor.YELLOW -> PlayerColor.RED
                        PlayerColor.GREEN -> PlayerColor.BLUE
                        PlayerColor.BLUE -> PlayerColor.GREEN
                    }
                    listOf(humanColor, opposite)
                }
                3 -> {
                    val all = listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
                    val others = all.filter { it != humanColor }.take(2)
                    listOf(humanColor) + others
                }
                else -> {
                    val all = listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
                    listOf(humanColor) + all.filter { it != humanColor }
                }
            }
        } else {
            when (playerCount) {
                2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW)
                3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
                else -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
            }
        }

        val players = selectedColors.map { color ->
            val isBot = (mode == GameMode.VS_COMPUTER && color != humanColor)
            val fallbackDefault = if (isBot) "Bot ${color.title}" else "${color.title} Player"
            val savedName = settingsRepository.getPlayerName(color, fallbackDefault)
            val name = customNames[color] ?: savedName

            // Save player name preference if provided
            customNames[color]?.let { settingsRepository.setPlayerName(color, it) }

            val avatar = customAvatars[color] ?: settingsRepository.getPlayerAvatar(color)
            customAvatars[color]?.let { settingsRepository.setPlayerAvatar(color, it) }

            Player(
                color = color,
                name = name,
                isBot = isBot,
                avatar = avatar
            )
        }

        val initialPlayers = LudoEngine.calculateLiveWinChances(players)
        val startingPlayerIndex = 0
        val startingPlayer = initialPlayers[startingPlayerIndex]

        val newState = GameState(
            matchId = UUID.randomUUID().toString(),
            players = initialPlayers,
            currentTurnIndex = startingPlayerIndex,
            diceState = DiceState(value = 1, isRolling = false, consecutiveSixes = 0),
            phase = TurnPhase.WAITING_FOR_ROLL,
            gameMode = mode,
            aiDifficulty = difficulty,
            statusMessage = "${startingPlayer.name}'s turn. Roll the dice!",
            movableTokenIds = emptySet(),
            winnerPodium = emptyList(),
            isGameOver = false,
            soundEnabled = settingsRepository.isSoundEnabled(),
            hapticsEnabled = settingsRepository.isHapticsEnabled(),
            autoRollBots = true,
            currentSkin = settingsRepository.getSelectedSkin(),
            boardTheme = settingsRepository.getBoardTheme(),
            animationSpeed = settingsRepository.getAnimationSpeed(),
            matchStartTimeMs = System.currentTimeMillis(),
            matchDurationSeconds = 0L,
            activeReaction = null,
            extraTurnReason = null
        )

        _gameState.value = newState

        if (saveImmediately) {
            gameStateRepository.saveActiveGame(newState)
            _hasSavedGame.value = true
        }

        checkBotTurn()
    }

    fun resumeSavedGame(): Boolean {
        val loaded = gameStateRepository.loadActiveGame() ?: return false
        if (loaded.isGameOver) {
            gameStateRepository.clearActiveGame()
            _hasSavedGame.value = false
            return false
        }

        botJob?.cancel()
        autoMoveJob?.cancel()
        captureDismissJob?.cancel()
        moveJob?.cancel()
        isActionLocked = false
        hasRecordedGameCompletion = false

        _gameState.value = loaded
        _hasSavedGame.value = true
        checkBotTurn(delayMs = 600)
        return true
    }

    fun discardSavedGame() {
        gameStateRepository.clearActiveGame()
        _hasSavedGame.value = false
    }

    fun rollDice() {
        if (isActionLocked) return
        val state = _gameState.value
        if (state.phase != TurnPhase.WAITING_FOR_ROLL || state.diceState.isRolling || state.isGameOver) {
            return
        }

        isActionLocked = true
        viewModelScope.launch {
            try {
                // Dice rolling animation
                _gameState.update {
                    it.copy(diceState = it.diceState.copy(isRolling = true))
                }
                soundManager.playDiceRoll(state.soundEnabled, state.hapticsEnabled)

                // Rapid tumble animation
                val curPlayerConsecutive = _gameState.value.currentPlayer?.consecutiveSixes
                    ?: _gameState.value.diceState.consecutiveSixes
                val maxTumble = if (curPlayerConsecutive >= 2) 6 else 7
                repeat(4) {
                    _gameState.update {
                        it.copy(diceState = it.diceState.copy(value = Random.nextInt(1, maxTumble)))
                    }
                    delay(35)
                }

                // Final roll resolution: strictly 1..5 if on third consecutive roll
                val finalRoll = if (curPlayerConsecutive >= 2) Random.nextInt(1, 6) else Random.nextInt(1, 7)
                val updatedState = LudoEngine.rollDice(_gameState.value, forcedRoll = finalRoll)
                _gameState.value = updatedState

                // Auto-save game state after dice roll
                if (!updatedState.isGameOver) {
                    gameStateRepository.saveActiveGame(updatedState)
                    _hasSavedGame.value = true
                }

                val currentPlayer = updatedState.currentPlayer
                if (updatedState.phase == TurnPhase.SELECTING_TOKEN && currentPlayer != null) {
                    if (currentPlayer.isBot) {
                        scheduleBotMove(delayMs = 420)
                    } else {
                        // If exactly one token can legally move, auto-move smoothly after 420ms
                        val movable = updatedState.movableTokenIds
                        if (movable.size == 1) {
                            val singleTokenId = movable.first()
                            autoMoveJob?.cancel()
                            autoMoveJob = viewModelScope.launch {
                                delay(420)
                                val cur = _gameState.value
                                if (cur.phase == TurnPhase.SELECTING_TOKEN &&
                                    cur.movableTokenIds.size == 1 &&
                                    cur.movableTokenIds.contains(singleTokenId) &&
                                    !cur.isGameOver
                                ) {
                                    selectToken(singleTokenId)
                                }
                            }
                        }
                    }
                } else if (updatedState.phase == TurnPhase.TURN_TRANSITION) {
                    // Turn transition delay: show roll message briefly before passing turn to next player
                    delay(750)
                    val nextTurnIndex = LudoEngine.getNextActivePlayerIndex(updatedState.players, updatedState.currentTurnIndex)
                    val nextPlayer = updatedState.players[nextTurnIndex]
                    _gameState.update {
                        it.copy(
                            currentTurnIndex = nextTurnIndex,
                            phase = TurnPhase.WAITING_FOR_ROLL,
                            statusMessage = "${nextPlayer.name}'s turn. Roll the dice!",
                            extraTurnReason = null
                        )
                    }
                    // Auto-save transitioned state
                    gameStateRepository.saveActiveGame(_gameState.value)
                    _hasSavedGame.value = true

                    soundManager.playTurnChange(_gameState.value.soundEnabled)
                    checkBotTurn(delayMs = 450)
                }
            } finally {
                isActionLocked = false
            }
        }
    }

    fun selectToken(tokenId: Int) {
        if (isActionLocked) return
        val state = _gameState.value
        if (state.phase != TurnPhase.SELECTING_TOKEN || state.isGameOver) return
        if (state.currentPlayer?.isBot == true) return
        if (!state.movableTokenIds.contains(tokenId)) return

        autoMoveJob?.cancel()
        autoMoveJob = null

        isActionLocked = true
        moveJob?.cancel()
        moveJob = viewModelScope.launch {
            try {
                executeAnimatedTokenMove(tokenId)
            } finally {
                isActionLocked = false
            }
        }
    }

    private suspend fun executeAnimatedTokenMove(tokenId: Int) {
        val startState = _gameState.value
        val player = startState.currentPlayer ?: return
        val token = player.tokens.find { it.id == tokenId } ?: return
        val roll = startState.diceState.value

        if (!LudoEngine.canTokenMove(token, roll)) return

        val outcome = LudoEngine.executeMove(startState, tokenId)
        if (outcome.updatedState == startState) return

        // 1. Visibly animate intermediate squares
        when (val pos = token.position) {
            is TokenPosition.InBase -> {
                _gameState.update { current ->
                    val updatedPlayers = current.players.map { p ->
                        if (p.color == player.color) {
                            p.copy(tokens = p.tokens.map { t ->
                                if (t.id == tokenId) t.copy(position = TokenPosition.OnBoard(step = 0)) else t
                            })
                        } else p
                    }
                    current.copy(
                        players = updatedPlayers,
                        phase = TurnPhase.ANIMATING_MOVE,
                        movableTokenIds = emptySet()
                    )
                }
                val stepDelay = startState.animationSpeed.stepDelayMs
                soundManager.playMove(startState.soundEnabled, startState.hapticsEnabled)
                delay(stepDelay)
            }
            is TokenPosition.OnBoard -> {
                val startStep = pos.step
                val targetStep = startStep + roll
                val stepDelay = startState.animationSpeed.stepDelayMs

                for (s in (startStep + 1)..targetStep) {
                    val intermediatePos = if (s >= com.example.logic.LudoBoardMap.MAX_STEPS) {
                        TokenPosition.Finished
                    } else {
                        TokenPosition.OnBoard(step = s)
                    }

                    _gameState.update { current ->
                        val updatedPlayers = current.players.map { p ->
                            if (p.color == player.color) {
                                p.copy(tokens = p.tokens.map { t ->
                                    if (t.id == tokenId) t.copy(position = intermediatePos) else t
                                })
                            } else p
                        }
                        current.copy(
                            players = updatedPlayers,
                            phase = TurnPhase.ANIMATING_MOVE,
                            movableTokenIds = emptySet()
                        )
                    }
                    soundManager.playMove(startState.soundEnabled, startState.hapticsEnabled)
                    delay(stepDelay)
                }
            }
            is TokenPosition.Finished -> {}
        }

        // 2. Token arrived at final square
        when {
            outcome.capturedToken != null -> {
                soundManager.playCapture(startState.soundEnabled, startState.hapticsEnabled)
            }
            outcome.updatedState.isGameOver -> {
                if (!hasRecordedGameCompletion) {
                    soundManager.playVictory(startState.soundEnabled, startState.hapticsEnabled)
                }
            }
            outcome.bonusReason?.contains("Home") == true -> {
                soundManager.playHomeSafe(startState.soundEnabled, startState.hapticsEnabled)
            }
        }

        val finalState = if (outcome.updatedState.isGameOver) {
            val durationSec = ((System.currentTimeMillis() - startState.matchStartTimeMs) / 1000L).coerceAtLeast(1L)
            outcome.updatedState.copy(matchDurationSeconds = durationSec)
        } else {
            outcome.updatedState
        }

        _gameState.value = finalState

        // Bot reactions based on move outcome
        if (player.isBot && outcome.capturedToken != null) {
            triggerReaction(listOf("😎", "🔥", "👏").random(), player.name, player.color)
        }
        val capturedOwner = startState.players.find { it.color == outcome.capturedToken?.color }
        if (capturedOwner?.isBot == true && capturedOwner.color != player.color) {
            triggerReaction(listOf("😮", "😭", "😂").random(), capturedOwner.name, capturedOwner.color)
        }
        if (finalState.isGameOver) {
            val winner = finalState.winnerPodium.firstOrNull() ?: finalState.players.firstOrNull()
            if (winner?.isBot == true) {
                triggerReaction("🎉", winner.name, winner.color)
            }
        }

        // Record completed game stats and history ONCE
        if (finalState.isGameOver && !hasRecordedGameCompletion) {
            hasRecordedGameCompletion = true
            val winner = finalState.winnerPodium.firstOrNull() ?: finalState.players.firstOrNull()
            if (winner != null) {
                // Update persistent player records
                statsRepository.recordCompletedGame(finalState.players, winner)
                _playerStats.value = statsRepository.getAllStats()

                // Save completed match to local game history
                val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                val formattedDate = dateFormat.format(Date())
                val summaries = finalState.players.map { p ->
                    MatchPlayerSummary(
                        name = p.name,
                        color = p.color,
                        isBot = p.isBot,
                        captures = p.captures,
                        tokensHome = p.tokens.count { it.position is TokenPosition.Finished },
                        avatar = p.avatar
                    )
                }

                val historyRecord = MatchHistoryRecord(
                    id = finalState.matchId,
                    timestampMs = System.currentTimeMillis(),
                    formattedDate = formattedDate,
                    gameMode = finalState.gameMode,
                    aiDifficulty = if (finalState.gameMode == GameMode.VS_COMPUTER) finalState.aiDifficulty else null,
                    winnerName = winner.name,
                    winnerColor = winner.color,
                    winnerAvatar = winner.avatar,
                    durationSeconds = finalState.matchDurationSeconds,
                    players = summaries
                )
                gameHistoryRepository.recordMatch(historyRecord)
                _matchHistory.value = gameHistoryRepository.getHistory()
            }

            // Clear saved active game so completed games are NEVER resumed
            gameStateRepository.clearActiveGame()
            _hasSavedGame.value = false
        } else if (!finalState.isGameOver) {
            // Auto-save active game state after move
            gameStateRepository.saveActiveGame(finalState)
            _hasSavedGame.value = true
        }

        // Dismiss capture emoji animation automatically
        if (outcome.captureEvent != null) {
            captureDismissJob?.cancel()
            captureDismissJob = viewModelScope.launch {
                delay(1300)
                _gameState.update { current ->
                    if (current.captureEvent?.eventId == outcome.captureEvent.eventId) {
                        current.copy(captureEvent = null)
                    } else current
                }
            }
        }

        if (!outcome.updatedState.isGameOver) {
            val botDelay = when {
                outcome.captureEvent != null -> 1350L
                outcome.hasBonusTurn -> 550L
                else -> 420L
            }
            checkBotTurn(delayMs = botDelay)
        }
    }

    fun resetStats() {
        statsRepository.resetAllStats()
        _playerStats.value = emptyList()
    }

    fun refreshStats() {
        _playerStats.value = statsRepository.getAllStats()
    }

    fun clearMatchHistory() {
        gameHistoryRepository.clearHistory()
        _matchHistory.value = emptyList()
    }

    private fun checkBotTurn(delayMs: Long = 450) {
        val state = _gameState.value
        if (state.isGameOver) return

        val currentPlayer = state.currentPlayer ?: return
        if (currentPlayer.isBot) {
            botJob?.cancel()
            botJob = viewModelScope.launch {
                delay(delayMs)
                var waitCount = 0
                while (isActionLocked && waitCount < 30) {
                    delay(50)
                    waitCount++
                }

                val curState = _gameState.value
                if (!curState.isGameOver &&
                    curState.phase == TurnPhase.WAITING_FOR_ROLL &&
                    curState.currentPlayer?.isBot == true &&
                    !curState.diceState.isRolling
                ) {
                    rollDice()
                }
            }
        }
    }

    private fun scheduleBotMove(delayMs: Long = 420) {
        botJob?.cancel()
        botJob = viewModelScope.launch {
            delay(delayMs)

            var waitCount = 0
            while (isActionLocked && waitCount < 30) {
                delay(50)
                waitCount++
            }

            val curState = _gameState.value
            if (curState.isGameOver) return@launch
            val curPlayer = curState.currentPlayer ?: return@launch
            if (!curPlayer.isBot) return@launch

            if (curState.phase != TurnPhase.SELECTING_TOKEN) {
                if (curState.phase == TurnPhase.WAITING_FOR_ROLL) {
                    rollDice()
                }
                return@launch
            }

            val movableIds = curState.movableTokenIds
            if (movableIds.isEmpty()) {
                val nextTurnIndex = LudoEngine.getNextActivePlayerIndex(curState.players, curState.currentTurnIndex)
                val nextPlayer = curState.players[nextTurnIndex]
                _gameState.update {
                    it.copy(
                        currentTurnIndex = nextTurnIndex,
                        phase = TurnPhase.WAITING_FOR_ROLL,
                        movableTokenIds = emptySet(),
                        statusMessage = "${nextPlayer.name}'s turn. Roll the dice!"
                    )
                }
                gameStateRepository.saveActiveGame(_gameState.value)
                _hasSavedGame.value = true

                soundManager.playTurnChange(_gameState.value.soundEnabled)
                checkBotTurn(delayMs = 450)
                return@launch
            }

            val roll = curState.diceState.value
            val chosenToken = LudoEngine.pickBestBotToken(curState, curPlayer, roll, curState.aiDifficulty)
            val selectedTokenId = if (chosenToken != null && chosenToken.id in movableIds) {
                chosenToken.id
            } else {
                curPlayer.tokens.firstOrNull { it.id in movableIds }?.id ?: movableIds.first()
            }

            executeBotMove(selectedTokenId)
        }
    }

    private fun executeBotMove(tokenId: Int) {
        val state = _gameState.value
        if (state.phase != TurnPhase.SELECTING_TOKEN || state.isGameOver) return
        if (!state.movableTokenIds.contains(tokenId)) return

        autoMoveJob?.cancel()
        autoMoveJob = null

        isActionLocked = true
        moveJob?.cancel()
        moveJob = viewModelScope.launch {
            try {
                executeAnimatedTokenMove(tokenId)
            } finally {
                isActionLocked = false
            }
        }
    }

    fun toggleSound() {
        _gameState.update {
            val next = !it.soundEnabled
            settingsRepository.setSoundEnabled(next)
            if (next) {
                soundManager.playButtonClick(true)
            }
            it.copy(soundEnabled = next)
        }
    }

    fun playButtonClick() {
        soundManager.playButtonClick(_gameState.value.soundEnabled)
    }

    fun toggleHaptics() {
        _gameState.update {
            val next = !it.hapticsEnabled
            settingsRepository.setHapticsEnabled(next)
            it.copy(hapticsEnabled = next)
        }
    }

    fun setSkin(skin: GameSkin) {
        settingsRepository.setSelectedSkin(skin)
        _gameState.update { it.copy(currentSkin = skin) }
    }

    fun setAnimationSpeed(speed: AnimationSpeed) {
        settingsRepository.setAnimationSpeed(speed)
        _gameState.update { it.copy(animationSpeed = speed) }
    }

    fun setAiDifficulty(difficulty: AiDifficulty) {
        settingsRepository.setAiDifficulty(difficulty)
        _gameState.update { it.copy(aiDifficulty = difficulty) }
    }

    fun toggleAutoRollBots() {
        _gameState.update { it.copy(autoRollBots = !it.autoRollBots) }
    }

    fun setBoardTheme(theme: BoardTheme) {
        settingsRepository.setBoardTheme(theme)
        _gameState.update { it.copy(boardTheme = theme) }
    }

    fun sendEmojiReaction(emoji: String) {
        val cur = _gameState.value
        val sender = cur.currentPlayer ?: cur.players.firstOrNull() ?: return
        triggerReaction(emoji, sender.name, sender.color)
    }

    fun triggerReaction(emoji: String, senderName: String, senderColor: PlayerColor) {
        reactionDismissJob?.cancel()
        val event = EmojiReactionEvent(
            emoji = emoji,
            senderName = senderName,
            senderColor = senderColor
        )
        _gameState.update { it.copy(activeReaction = event) }
        reactionDismissJob = viewModelScope.launch {
            delay(2400)
            _gameState.update {
                if (it.activeReaction?.id == event.id) it.copy(activeReaction = null) else it
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        botJob?.cancel()
        autoMoveJob?.cancel()
        captureDismissJob?.cancel()
        moveJob?.cancel()
        reactionDismissJob?.cancel()
        soundManager.release()
    }
}
