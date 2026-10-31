package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AnimationSpeed
import com.example.model.BoardTheme
import com.example.model.GameSkin
import com.example.model.PlayerAvatar
import com.example.model.PlayerColor

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jitu_ludo_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SOUND_ENABLED = "pref_sound_enabled"
        private const val KEY_HAPTICS_ENABLED = "pref_haptics_enabled"
        private const val KEY_ANIMATION_SPEED = "pref_animation_speed"
        private const val KEY_SELECTED_SKIN = "pref_selected_skin"
        private const val KEY_SELECTED_BOARD_THEME = "pref_selected_board_theme"
        private const val KEY_AI_DIFFICULTY = "pref_ai_difficulty"
        private const val PREFIX_PLAYER_NAME = "pref_player_name_"
        private const val PREFIX_PLAYER_AVATAR = "pref_player_avatar_"
    }

    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isHapticsEnabled(): Boolean {
        return prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    fun getAnimationSpeed(): AnimationSpeed {
        val name = prefs.getString(KEY_ANIMATION_SPEED, AnimationSpeed.NORMAL.name)
        return runCatching { AnimationSpeed.valueOf(name ?: AnimationSpeed.NORMAL.name) }.getOrDefault(AnimationSpeed.NORMAL)
    }

    fun setAnimationSpeed(speed: AnimationSpeed) {
        prefs.edit().putString(KEY_ANIMATION_SPEED, speed.name).apply()
    }

    fun getSelectedSkin(): GameSkin {
        val name = prefs.getString(KEY_SELECTED_SKIN, GameSkin.CLASSIC.name)
        return runCatching { GameSkin.valueOf(name ?: GameSkin.CLASSIC.name) }.getOrDefault(GameSkin.CLASSIC)
    }

    fun setSelectedSkin(skin: GameSkin) {
        prefs.edit().putString(KEY_SELECTED_SKIN, skin.name).apply()
    }

    fun getBoardTheme(): BoardTheme {
        val id = prefs.getString(KEY_SELECTED_BOARD_THEME, BoardTheme.CLASSIC.id)
        return BoardTheme.fromId(id)
    }

    fun setBoardTheme(theme: BoardTheme) {
        prefs.edit().putString(KEY_SELECTED_BOARD_THEME, theme.id).apply()
    }

    fun getAiDifficulty(): com.example.model.AiDifficulty {
        val name = prefs.getString(KEY_AI_DIFFICULTY, com.example.model.AiDifficulty.MEDIUM.name)
        return runCatching { com.example.model.AiDifficulty.valueOf(name ?: com.example.model.AiDifficulty.MEDIUM.name) }
            .getOrDefault(com.example.model.AiDifficulty.MEDIUM)
    }

    fun setAiDifficulty(difficulty: com.example.model.AiDifficulty) {
        prefs.edit().putString(KEY_AI_DIFFICULTY, difficulty.name).apply()
    }

    fun getPlayerName(color: PlayerColor, defaultName: String): String {
        return prefs.getString(PREFIX_PLAYER_NAME + color.name, defaultName) ?: defaultName
    }

    fun setPlayerName(color: PlayerColor, name: String) {
        prefs.edit().putString(PREFIX_PLAYER_NAME + color.name, name).apply()
    }

    fun getPlayerAvatar(color: PlayerColor): PlayerAvatar {
        val id = prefs.getString(PREFIX_PLAYER_AVATAR + color.name, null)
        return if (id != null) PlayerAvatar.fromId(id) else PlayerAvatar.defaultFor(color)
    }

    fun setPlayerAvatar(color: PlayerColor, avatar: PlayerAvatar) {
        prefs.edit().putString(PREFIX_PLAYER_AVATAR + color.name, avatar.id).apply()
    }
}
