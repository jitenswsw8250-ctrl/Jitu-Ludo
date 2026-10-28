package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PlayerStatsRecord
import com.example.data.StatsRepository
import com.example.logic.LudoEngine
import com.example.model.DiceState
import com.example.model.GameMode
import com.example.model.GameState
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.TurnPhase
import com.example.sound.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class LudoViewModel(application: Application) : AndroidViewModel(application) {

    private val soundManager = SoundManager(application)
    private val statsRepository = StatsRepository(application)

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _playerStats = MutableStateFlow<List<PlayerStatsRecord>>(emptyList())
    val playerStats: StateFlow<List<PlayerStatsRecord>> = _playerStats.asStateFlow()

    private var botJob: Job? = null
    private var autoMoveJob: Job? = null
    private var captureDismissJob: Job? = null

    @Volatile
    private var isActionLocked = false

    @Volatile
    private var hasRecordedGameCompletion = false

    init {
        // Load persistent stats
        _playerStats.value = statsRepository.getAllStats()

        // Initialize with default 2-player local game
        startNewGame(
            mode = GameMode.LOCAL_MULTIPLAYER,
            playerCount = 2,
            humanColor = PlayerColor.RED,
            customNames = mapOf(
                PlayerColor.RED to "Player 1",
                PlayerColor.YELLOW to "Player 2"
            )
        )
    }

    fun startNewGame(
        mode: GameMode,
        playerCount: Int,
        humanColor: PlayerColor = PlayerColor.RED,
        customNames: Map<PlayerColor, String> = emptyMap()
    ) {
        botJob?.cancel()
        autoMoveJob?.cancel()
        captureDismissJob?.cancel()
        moveJob?.cancel()
        isActionLocked = false
        hasRecordedGameCompletion = false

        // Correctly assemble players and colors for each mode
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
                2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW) // Classic opposite colors
                3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
                else -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
            }
        }

        val players = selectedColors.map { color ->
            val isBot = (mode == GameMode.VS_COMPUTER && color != humanColor)
            val defaultName = if (isBot) "Bot ${color.title}" else "${color.title} Player"
            val name = customNames[color] ?: defaultName
            Player(
                color = color,
                name = name,
                isBot = isBot
            )
        }

        val initialPlayers = LudoEngine.calculateLiveWinChances(players)

        // Starting player index is 0 (human player in Vs Computer, or first player in Pass & Play)
        val startingPlayerIndex = 0
        val startingPlayer = initialPlayers[startingPlayerIndex]

        _gameState.update {
            GameState(
                players = initialPlayers,
                currentTurnIndex = startingPlayerIndex,
                diceState = DiceState(value = 1, isRolling = false, consecutiveSixes = 0),
                phase = TurnPhase.WAITING_FOR_ROLL,
                gameMode = mode,
                statusMessage = "${startingPlayer.name}'s turn. Roll the dice!",
                movableTokenIds = emptySet(),
                winnerPodium = emptyList(),
                isGameOver = false,
                soundEnabled = it.soundEnabled,
                hapticsEnabled = it.hapticsEnabled,
                autoRollBots = it.autoRollBots
            )
        }

        checkBotTurn()
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

                // Rapid tumble animation (snappy, smooth and non-blocking)
                repeat(4) {
                    _gameState.update {
                        it.copy(diceState = it.diceState.copy(value = Random.nextInt(1, 7)))
                    }
                    delay(35)
                }

                // Final roll resolution
                val finalRoll = Random.nextInt(1, 7)
                val updatedState = LudoEngine.rollDice(_gameState.value, forcedRoll = finalRoll)
                _gameState.value = updatedState

                val currentPlayer = updatedState.currentPlayer
                if (updatedState.phase == TurnPhase.SELECTING_TOKEN && currentPlayer != null) {
                    if (currentPlayer.isBot) {
                        scheduleBotMove(delayMs = 420)
                    } else {
                        // Feature 2: If exactly one token can legally move, auto-move smoothly after 420ms
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
                            statusMessage = "${nextPlayer.name}'s turn. Roll the dice!"
                        )
                    }
                    soundManager.playTurnChange(_gameState.value.soundEnabled)
                    checkBotTurn(delayMs = 450)
                }
            } finally {
                isActionLocked = false
            }
        }
    }

    private var moveJob: Job? = null

    fun selectToken(tokenId: Int) {
        if (isActionLocked) return
        val state = _gameState.value
        if (state.phase != TurnPhase.SELECTING_TOKEN || state.isGameOver) return
        // Prevent accidental human taps from interrupting a bot's turn
        if (state.currentPlayer?.isBot == true) return
        if (!state.movableTokenIds.contains(tokenId)) return

        // Prevent duplicate movement: cancel any scheduled auto-move
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

        // Calculate the authoritative final outcome from the start state
        val outcome = LudoEngine.executeMove(startState, tokenId)
        if (outcome.updatedState == startState) return

        // 1. Visibly animate intermediate squares (approx. 100ms per square)
        when (val pos = token.position) {
            is com.example.model.TokenPosition.InBase -> {
                // Leaving base yard onto start square (step 0)
                _gameState.update { current ->
                    val updatedPlayers = current.players.map { p ->
                        if (p.color == player.color) {
                            p.copy(tokens = p.tokens.map { t ->
                                if (t.id == tokenId) t.copy(position = com.example.model.TokenPosition.OnBoard(step = 0)) else t
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
                delay(100)
            }
            is com.example.model.TokenPosition.OnBoard -> {
                val startStep = pos.step
                val targetStep = startStep + roll

                for (s in (startStep + 1)..targetStep) {
                    val intermediatePos = if (s >= com.example.logic.LudoBoardMap.MAX_STEPS) {
                        com.example.model.TokenPosition.Finished
                    } else {
                        com.example.model.TokenPosition.OnBoard(step = s)
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
                    delay(100) // 100ms per square (target: 80-120ms)
                }
            }
            is com.example.model.TokenPosition.Finished -> {}
        }

        // 2. Token arrived at final square - trigger destination sounds & resolution
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

        _gameState.value = outcome.updatedState

        // Record completed game stats once when game is over
        if (outcome.updatedState.isGameOver && !hasRecordedGameCompletion) {
            hasRecordedGameCompletion = true
            val winner = outcome.updatedState.winnerPodium.firstOrNull() ?: outcome.updatedState.players.firstOrNull()
            if (winner != null) {
                statsRepository.recordCompletedGame(outcome.updatedState.players, winner)
                _playerStats.value = statsRepository.getAllStats()
            }
        }

        // Dismiss capture emoji animation automatically after ~1.3 seconds
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

    private fun checkBotTurn(delayMs: Long = 450) {
        val state = _gameState.value
        if (state.isGameOver) return

        val currentPlayer = state.currentPlayer ?: return
        if (currentPlayer.isBot) {
            botJob?.cancel()
            botJob = viewModelScope.launch {
                delay(delayMs)
                // Wait for any prior animation lock to clear
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

            // Wait for any prior animation lock to clear
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
                // If no legal moves available, safely advance turn to prevent getting stuck
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
                soundManager.playTurnChange(_gameState.value.soundEnabled)
                checkBotTurn(delayMs = 450)
                return@launch
            }

            // Always select and execute a legal move using fresh state and bot player
            val roll = curState.diceState.value
            val chosenToken = LudoEngine.pickBestBotToken(curState, curPlayer, roll)
            val selectedTokenId = if (chosenToken != null && chosenToken.id in movableIds) {
                chosenToken.id
            } else {
                // Guaranteed fallback: pick the first eligible token from movableIds
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
        _gameState.update { it.copy(hapticsEnabled = !it.hapticsEnabled) }
    }

    fun toggleAutoRollBots() {
        _gameState.update { it.copy(autoRollBots = !it.autoRollBots) }
    }

    override fun onCleared() {
        super.onCleared()
        botJob?.cancel()
        autoMoveJob?.cancel()
        captureDismissJob?.cancel()
        moveJob?.cancel()
        soundManager.release()
    }
}
