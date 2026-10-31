package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.logic.LudoEngine
import com.example.model.AiDifficulty
import com.example.model.AnimationSpeed
import com.example.model.BoardTheme
import com.example.model.DiceState
import com.example.model.GameMode
import com.example.model.GameSkin
import com.example.model.GameState
import com.example.model.Player
import com.example.model.PlayerAvatar
import com.example.model.PlayerColor
import com.example.model.Token
import com.example.model.TokenPosition
import com.example.model.TurnPhase
import org.json.JSONArray
import org.json.JSONObject

class GameStateRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jitu_ludo_active_match", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_GAME_JSON = "active_game_state_json"
    }

    @Synchronized
    fun hasActiveGame(): Boolean {
        val jsonStr = prefs.getString(KEY_ACTIVE_GAME_JSON, null) ?: return false
        return try {
            val root = JSONObject(jsonStr)
            val isGameOver = root.optBoolean("isGameOver", false)
            !isGameOver
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun saveActiveGame(state: GameState) {
        // Never save completed matches as active games
        if (state.isGameOver) {
            clearActiveGame()
            return
        }

        try {
            val root = JSONObject().apply {
                put("matchId", state.matchId)
                put("currentTurnIndex", state.currentTurnIndex)
                put("phase", state.phase.name)
                put("gameMode", state.gameMode.name)
                put("aiDifficulty", state.aiDifficulty.name)
                put("isGameOver", state.isGameOver)
                put("currentSkin", state.currentSkin.name)
                put("boardTheme", state.boardTheme.id)
                put("animationSpeed", state.animationSpeed.name)
                put("soundEnabled", state.soundEnabled)
                put("hapticsEnabled", state.hapticsEnabled)
                put("matchStartTimeMs", state.matchStartTimeMs)

                // Dice state (ensure isRolling is false so game never restores frozen)
                val diceObj = JSONObject().apply {
                    put("value", state.diceState.value)
                    put("isRolling", false)
                    put("consecutiveSixes", state.diceState.consecutiveSixes)
                }
                put("diceState", diceObj)

                // Players
                val playersArray = JSONArray()
                for (p in state.players) {
                    val pObj = JSONObject().apply {
                        put("color", p.color.name)
                        put("name", p.name)
                        put("avatar", p.avatar.id)
                        put("isBot", p.isBot)
                        put("captures", p.captures)
                        put("losses", p.losses)
                        put("consecutiveSixes", p.consecutiveSixes)

                        val tokensArray = JSONArray()
                        for (t in p.tokens) {
                            val tObj = JSONObject().apply {
                                put("id", t.id)
                                val pos = t.position
                                when (pos) {
                                    is TokenPosition.InBase -> put("posType", "BASE")
                                    is TokenPosition.Finished -> put("posType", "FINISHED")
                                    is TokenPosition.OnBoard -> {
                                        put("posType", "BOARD")
                                        put("step", pos.step)
                                    }
                                }
                            }
                            tokensArray.put(tObj)
                        }
                        put("tokens", tokensArray)
                    }
                    playersArray.put(pObj)
                }
                put("players", playersArray)
            }

            prefs.edit().putString(KEY_ACTIVE_GAME_JSON, root.toString()).apply()
        } catch (_: Exception) {
            // Fail gracefully
        }
    }

    @Synchronized
    fun loadActiveGame(): GameState? {
        val jsonStr = prefs.getString(KEY_ACTIVE_GAME_JSON, null) ?: return null
        return try {
            val root = JSONObject(jsonStr)
            val isGameOver = root.optBoolean("isGameOver", false)
            if (isGameOver) {
                clearActiveGame()
                return null
            }

            val matchId = root.optString("matchId", java.util.UUID.randomUUID().toString())
            val currentTurnIndex = root.optInt("currentTurnIndex", 0)
            val modeName = root.optString("gameMode", GameMode.LOCAL_MULTIPLAYER.name)
            val gameMode = runCatching { GameMode.valueOf(modeName) }.getOrDefault(GameMode.LOCAL_MULTIPLAYER)
            val diffName = root.optString("aiDifficulty", AiDifficulty.MEDIUM.name)
            val aiDifficulty = runCatching { AiDifficulty.valueOf(diffName) }.getOrDefault(AiDifficulty.MEDIUM)
            val skinName = root.optString("currentSkin", GameSkin.CLASSIC.name)
            val currentSkin = runCatching { GameSkin.valueOf(skinName) }.getOrDefault(GameSkin.CLASSIC)
            val boardThemeId = root.optString("boardTheme", BoardTheme.CLASSIC.id)
            val boardTheme = BoardTheme.fromId(boardThemeId)
            val speedName = root.optString("animationSpeed", AnimationSpeed.NORMAL.name)
            val animationSpeed = runCatching { AnimationSpeed.valueOf(speedName) }.getOrDefault(AnimationSpeed.NORMAL)
            val soundEnabled = root.optBoolean("soundEnabled", true)
            val hapticsEnabled = root.optBoolean("hapticsEnabled", true)
            val matchStartTimeMs = root.optLong("matchStartTimeMs", System.currentTimeMillis())

            val diceObj = root.optJSONObject("diceState")
            val diceVal = diceObj?.optInt("value", 1) ?: 1
            val diceConsecutive = diceObj?.optInt("consecutiveSixes", 0) ?: 0
            val diceState = DiceState(value = diceVal, isRolling = false, consecutiveSixes = diceConsecutive)

            val rawPhaseName = root.optString("phase", TurnPhase.WAITING_FOR_ROLL.name)
            var phase = runCatching { TurnPhase.valueOf(rawPhaseName) }.getOrDefault(TurnPhase.WAITING_FOR_ROLL)
            // If interrupted during mid-animation, safely reset to selecting or rolling
            if (phase == TurnPhase.ANIMATING_MOVE || phase == TurnPhase.TURN_TRANSITION) {
                phase = TurnPhase.WAITING_FOR_ROLL
            }

            val playersArray = root.getJSONArray("players")
            val playersList = mutableListOf<Player>()

            for (i in 0 until playersArray.length()) {
                val pObj = playersArray.getJSONObject(i)
                val colorName = pObj.getString("color")
                val color = runCatching { PlayerColor.valueOf(colorName) }.getOrDefault(PlayerColor.RED)
                val name = pObj.optString("name", color.title)
                val avatarId = pObj.optString("avatar", "")
                val avatar = if (avatarId.isNotEmpty()) PlayerAvatar.fromId(avatarId) else PlayerAvatar.defaultFor(color)
                val isBot = pObj.optBoolean("isBot", false)
                val captures = pObj.optInt("captures", 0)
                val losses = pObj.optInt("losses", 0)
                val consecutiveSixes = pObj.optInt("consecutiveSixes", 0)

                val tokensArray = pObj.getJSONArray("tokens")
                val tokensList = mutableListOf<Token>()
                for (j in 0 until tokensArray.length()) {
                    val tObj = tokensArray.getJSONObject(j)
                    val id = tObj.getInt("id")
                    val posType = tObj.optString("posType", "BASE")
                    val position: TokenPosition = when (posType) {
                        "FINISHED" -> TokenPosition.Finished
                        "BOARD" -> {
                            val step = tObj.optInt("step", 0)
                            TokenPosition.OnBoard(step)
                        }
                        else -> TokenPosition.InBase
                    }
                    tokensList.add(Token(id = id, color = color, position = position))
                }

                playersList.add(
                    Player(
                        color = color,
                        name = name,
                        isBot = isBot,
                        avatar = avatar,
                        tokens = tokensList,
                        captures = captures,
                        losses = losses,
                        consecutiveSixes = consecutiveSixes
                    )
                )
            }

            val playersWithWinChance = LudoEngine.calculateLiveWinChances(playersList)
            val curPlayer = playersWithWinChance.getOrNull(currentTurnIndex)

            val movableIds = if (phase == TurnPhase.SELECTING_TOKEN && curPlayer != null) {
                LudoEngine.getMovableTokens(curPlayer, diceState.value).map { it.id }.toSet()
            } else {
                emptySet()
            }

            // If in SELECTING_TOKEN but no legal moves, advance to waiting for roll safely
            val adjustedPhase = if (phase == TurnPhase.SELECTING_TOKEN && movableIds.isEmpty()) {
                TurnPhase.WAITING_FOR_ROLL
            } else {
                phase
            }

            GameState(
                matchId = matchId,
                players = playersWithWinChance,
                currentTurnIndex = currentTurnIndex.coerceIn(0, (playersList.size - 1).coerceAtLeast(0)),
                diceState = diceState,
                phase = adjustedPhase,
                gameMode = gameMode,
                aiDifficulty = aiDifficulty,
                movableTokenIds = movableIds,
                statusMessage = if (adjustedPhase == TurnPhase.SELECTING_TOKEN) "Select token to move" else "${curPlayer?.name ?: "Player"}'s turn",
                isGameOver = false,
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled,
                currentSkin = currentSkin,
                boardTheme = boardTheme,
                animationSpeed = animationSpeed,
                matchStartTimeMs = matchStartTimeMs
            )
        } catch (_: Exception) {
            clearActiveGame()
            null
        }
    }

    @Synchronized
    fun clearActiveGame() {
        prefs.edit().remove(KEY_ACTIVE_GAME_JSON).apply()
    }
}
