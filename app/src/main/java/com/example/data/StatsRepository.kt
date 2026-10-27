package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Player
import com.example.model.PlayerColor
import org.json.JSONObject
import java.util.Locale

data class PlayerStatsRecord(
    val id: String,
    val name: String,
    val color: PlayerColor,
    val isBot: Boolean,
    val wins: Int = 0,
    val completedGames: Int = 0
) {
    val winPercentageFormatted: String
        get() {
            if (completedGames == 0) return "—"
            val pct = (wins.toDouble() / completedGames) * 100.0
            return if (pct % 1.0 == 0.0) {
                "${pct.toInt()}%"
            } else {
                String.format(Locale.US, "%.1f%%", pct)
            }
        }
}

class StatsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jitu_ludo_stats", Context.MODE_PRIVATE)

    companion object {
        fun buildPlayerId(player: Player): String {
            return if (player.isBot) {
                "bot:${player.color.name.lowercase()}"
            } else {
                "human:${player.name.trim().lowercase()}"
            }
        }

        fun calculateWinPercentage(wins: Int, completedGames: Int): String {
            if (completedGames == 0) return "—"
            val pct = (wins.toDouble() / completedGames) * 100.0
            return if (pct % 1.0 == 0.0) {
                "${pct.toInt()}%"
            } else {
                String.format(Locale.US, "%.1f%%", pct)
            }
        }
    }

    @Synchronized
    fun getAllStats(): List<PlayerStatsRecord> {
        val jsonStr = prefs.getString("all_player_stats", null) ?: return emptyList()
        return try {
            val root = JSONObject(jsonStr)
            val list = mutableListOf<PlayerStatsRecord>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.getJSONObject(key)
                val colorName = obj.optString("color", PlayerColor.RED.name)
                val color = runCatching { PlayerColor.valueOf(colorName) }.getOrDefault(PlayerColor.RED)
                list.add(
                    PlayerStatsRecord(
                        id = key,
                        name = obj.optString("name", "Player"),
                        color = color,
                        isBot = obj.optBoolean("isBot", false),
                        wins = obj.optInt("wins", 0),
                        completedGames = obj.optInt("completedGames", 0)
                    )
                )
            }
            list.sortedWith(
                compareByDescending<PlayerStatsRecord> { it.wins }
                    .thenByDescending { it.completedGames }
                    .thenBy { it.name }
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun recordCompletedGame(players: List<Player>, winner: Player) {
        val currentStats = getAllStats().associateBy { it.id }.toMutableMap()

        for (player in players) {
            val id = buildPlayerId(player)
            val existing = currentStats[id] ?: PlayerStatsRecord(
                id = id,
                name = player.name,
                color = player.color,
                isBot = player.isBot,
                wins = 0,
                completedGames = 0
            )
            val isWinner = (player.color == winner.color && player.name == winner.name)
            currentStats[id] = existing.copy(
                name = player.name,
                color = player.color,
                isBot = player.isBot,
                completedGames = existing.completedGames + 1,
                wins = if (isWinner) existing.wins + 1 else existing.wins
            )
        }

        saveStatsMap(currentStats)
    }

    @Synchronized
    fun resetAllStats() {
        prefs.edit().remove("all_player_stats").apply()
    }

    private fun saveStatsMap(map: Map<String, PlayerStatsRecord>) {
        val root = JSONObject()
        for ((id, record) in map) {
            val obj = JSONObject().apply {
                put("id", record.id)
                put("name", record.name)
                put("color", record.color.name)
                put("isBot", record.isBot)
                put("wins", record.wins)
                put("completedGames", record.completedGames)
            }
            root.put(id, obj)
        }
        prefs.edit().putString("all_player_stats", root.toString()).apply()
    }
}
