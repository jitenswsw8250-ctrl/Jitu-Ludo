package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AiDifficulty
import com.example.model.GameMode
import com.example.model.MatchHistoryRecord
import com.example.model.MatchPlayerSummary
import com.example.model.PlayerColor
import org.json.JSONArray
import org.json.JSONObject

class GameHistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jitu_ludo_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY_ARRAY = "completed_matches_history"
        private const val MAX_HISTORY_RECORDS = 50
    }

    @Synchronized
    fun getHistory(): List<MatchHistoryRecord> {
        val jsonStr = prefs.getString(KEY_HISTORY_ARRAY, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<MatchHistoryRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "")
                val timestampMs = obj.optLong("timestampMs", 0L)
                val formattedDate = obj.optString("formattedDate", "")
                val modeName = obj.optString("gameMode", GameMode.LOCAL_MULTIPLAYER.name)
                val gameMode = runCatching { GameMode.valueOf(modeName) }.getOrDefault(GameMode.LOCAL_MULTIPLAYER)
                val diffName = obj.optString("aiDifficulty", "")
                val aiDifficulty = if (diffName.isNotEmpty()) runCatching { AiDifficulty.valueOf(diffName) }.getOrNull() else null
                val winnerName = obj.optString("winnerName", "Winner")
                val winnerColorName = obj.optString("winnerColor", PlayerColor.RED.name)
                val winnerColor = runCatching { PlayerColor.valueOf(winnerColorName) }.getOrDefault(PlayerColor.RED)
                val winnerAvatarName = obj.optString("winnerAvatar", "")
                val winnerAvatar = if (winnerAvatarName.isNotEmpty()) {
                    com.example.model.PlayerAvatar.fromId(winnerAvatarName)
                } else {
                    com.example.model.PlayerAvatar.defaultFor(winnerColor)
                }
                val durationSeconds = obj.optLong("durationSeconds", 0L)

                val playersArray = obj.optJSONArray("players") ?: JSONArray()
                val playerSummaries = mutableListOf<MatchPlayerSummary>()
                for (j in 0 until playersArray.length()) {
                    val pObj = playersArray.getJSONObject(j)
                    val pColorName = pObj.optString("color", PlayerColor.RED.name)
                    val pColor = runCatching { PlayerColor.valueOf(pColorName) }.getOrDefault(PlayerColor.RED)
                    val pAvatarName = pObj.optString("avatar", "")
                    val pAvatar = if (pAvatarName.isNotEmpty()) {
                        com.example.model.PlayerAvatar.fromId(pAvatarName)
                    } else {
                        com.example.model.PlayerAvatar.defaultFor(pColor)
                    }
                    playerSummaries.add(
                        MatchPlayerSummary(
                            name = pObj.optString("name", "Player"),
                            color = pColor,
                            isBot = pObj.optBoolean("isBot", false),
                            captures = pObj.optInt("captures", 0),
                            tokensHome = pObj.optInt("tokensHome", 0),
                            avatar = pAvatar
                        )
                    )
                }

                list.add(
                    MatchHistoryRecord(
                        id = id,
                        timestampMs = timestampMs,
                        formattedDate = formattedDate,
                        gameMode = gameMode,
                        aiDifficulty = aiDifficulty,
                        winnerName = winnerName,
                        winnerColor = winnerColor,
                        winnerAvatar = winnerAvatar,
                        durationSeconds = durationSeconds,
                        players = playerSummaries
                    )
                )
            }
            list.sortedByDescending { it.timestampMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun recordMatch(record: MatchHistoryRecord) {
        val current = getHistory().toMutableList()
        // Prevent duplicate record if same match ID is recorded again
        if (current.any { it.id == record.id }) return

        current.add(0, record)
        val trimmed = current.take(MAX_HISTORY_RECORDS)

        val array = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestampMs", item.timestampMs)
                put("formattedDate", item.formattedDate)
                put("gameMode", item.gameMode.name)
                item.aiDifficulty?.let { put("aiDifficulty", it.name) }
                put("winnerName", item.winnerName)
                put("winnerColor", item.winnerColor.name)
                put("winnerAvatar", item.winnerAvatar.id)
                put("durationSeconds", item.durationSeconds)

                val playersArray = JSONArray()
                for (p in item.players) {
                    val pObj = JSONObject().apply {
                        put("name", p.name)
                        put("color", p.color.name)
                        put("isBot", p.isBot)
                        put("captures", p.captures)
                        put("tokensHome", p.tokensHome)
                        put("avatar", p.avatar.id)
                    }
                    playersArray.put(pObj)
                }
                put("players", playersArray)
            }
            array.put(obj)
        }

        prefs.edit().putString(KEY_HISTORY_ARRAY, array.toString()).apply()
    }

    @Synchronized
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY_ARRAY).apply()
    }
}
