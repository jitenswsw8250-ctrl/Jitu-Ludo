package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AnimationSpeed
import com.example.model.GameSkin

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jitu_ludo_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SOUND_ENABLED = "pref_sound_enabled"
        private const val KEY_HAPTICS_ENABLED = "pref_haptics_enabled"
        private const val KEY_ANIMATION_SPEED = "pref_animation_speed"
        private const val KEY_SELECTED_SKIN = "pref_selected_skin"
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
}
