package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AiDifficulty
import com.example.model.AnimatedBackground
import com.example.model.AnimationSpeed
import com.example.model.BoardTheme
import com.example.model.GameSkin
import com.example.model.SoundPack

@Composable
fun SettingsDialog(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    animationSpeed: AnimationSpeed,
    currentSkin: GameSkin,
    aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    boardTheme: BoardTheme = BoardTheme.CLASSIC,
    soundPack: SoundPack = SoundPack.CLASSIC,
    soundVolume: Float = 1.0f,
    talkingTokensEnabled: Boolean = true,
    animatedBackground: AnimatedBackground = AnimatedBackground.ROYAL_PALACE,
    startupMusicEnabled: Boolean = true,
    onToggleSound: () -> Unit,
    onToggleStartupMusic: (() -> Unit)? = null,
    onToggleHaptics: () -> Unit,
    onSetAnimationSpeed: (AnimationSpeed) -> Unit,
    onSetAiDifficulty: ((AiDifficulty) -> Unit)? = null,
    onSetBoardTheme: ((BoardTheme) -> Unit)? = null,
    onSetSoundPack: ((SoundPack) -> Unit)? = null,
    onSetSoundVolume: ((Float) -> Unit)? = null,
    onToggleTalkingTokens: (() -> Unit)? = null,
    onSetAnimatedBackground: ((AnimatedBackground) -> Unit)? = null,
    onPreviewSoundPack: ((SoundPack) -> Unit)? = null,
    onOpenSkins: () -> Unit,
    onOpenRules: () -> Unit,
    onRestartMatch: (() -> Unit)? = null,
    onResetStats: (() -> Unit)? = null,
    onResetSettingsToDefaults: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showConfirmReset by remember { mutableStateOf(false) }
    var showConfirmResetSettings by remember { mutableStateOf(false) }
    var showConfirmRestartMatch by remember { mutableStateOf(false) }
    var resetSettingsSuccessMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF140804))
                .border(
                    2.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFF78350F))
                    ),
                    RoundedCornerShape(24.dp)
                )
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SETTINGS",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Preferences & Game Controls",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SUCCESS MESSAGE BANNER (After Reset to Default)
                if (resetSettingsSuccessMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("settings_reset_success_banner"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = resetSettingsSuccessMessage ?: "",
                                color = Color(0xFFD1FAE5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // SECTION 1: AUDIO & HAPTICS
                SettingsSectionHeader(title = "AUDIO & HAPTICS")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Sound Switch
                        SettingsToggleRow(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            iconTint = Color(0xFF38BDF8),
                            title = "Game Sound",
                            subtitle = "Sound effects for rolls, moves, captures",
                            checked = soundEnabled,
                            onToggle = onToggleSound,
                            testTag = "settings_sound_switch"
                        )

                        // Menu Background Music Switch (Separate control for home menu music)
                        if (onToggleStartupMusic != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            SettingsToggleRow(
                                icon = Icons.Default.MusicNote,
                                iconTint = Color(0xFFFDE047),
                                title = "Menu Music",
                                subtitle = "Play pleasant instrumental music on home menu",
                                checked = startupMusicEnabled,
                                onToggle = onToggleStartupMusic,
                                testTag = "settings_startup_music_switch"
                            )
                        }

                        // Sound Volume Slider
                        if (onSetSoundVolume != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Sound Volume",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE2E8F0)
                                        )
                                    }
                                    Text(
                                        text = "${(soundVolume * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (soundEnabled) Color(0xFFFDE047) else Color(0xFF64748B)
                                    )
                                }
                                Slider(
                                    value = soundVolume,
                                    onValueChange = { onSetSoundVolume(it) },
                                    valueRange = 0f..1f,
                                    enabled = soundEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFDE047),
                                        activeTrackColor = Color(0xFFF59E0B),
                                        inactiveTrackColor = Color(0xFF451A03),
                                        disabledThumbColor = Color(0xFF64748B),
                                        disabledActiveTrackColor = Color(0xFF334155)
                                    ),
                                    modifier = Modifier.testTag("settings_sound_volume_slider")
                                )
                            }
                        }

                        // Sound Pack Selector
                        if (onSetSoundPack != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFF451A03))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sound Pack",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SoundPack.values().forEach { pack ->
                                    val isSelected = soundPack == pack
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) {
                                                    Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF78350F)))
                                                } else {
                                                    Brush.verticalGradient(listOf(Color(0xFF1E0C04), Color(0xFF140804)))
                                                }
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isSelected) Color(0xFFFDE047) else Color(0xFF78350F).copy(alpha = 0.5f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                onSetSoundPack(pack)
                                                onPreviewSoundPack?.invoke(pack)
                                            }
                                            .testTag("sound_pack_${pack.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = pack.title,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                            )
                                            if (isSelected && onPreviewSoundPack != null) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Preview",
                                                    tint = Color(0xFFFDE047),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Talking Tokens Toggle Switch
                        if (onToggleTalkingTokens != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFF451A03))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SettingsToggleRow(
                                icon = Icons.Default.RecordVoiceOver,
                                iconTint = Color(0xFFA78BFA),
                                title = "Talking Tokens",
                                subtitle = "Tokens speak funny speech bubbles & voice cues",
                                checked = talkingTokensEnabled,
                                onToggle = onToggleTalkingTokens,
                                testTag = "settings_talking_tokens_switch"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF451A03))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Haptics Switch
                        SettingsToggleRow(
                            icon = Icons.Default.Vibration,
                            iconTint = Color(0xFFFBBF24),
                            title = "Haptic Vibration",
                            subtitle = "Tactile feedback on dice and token actions",
                            checked = hapticsEnabled,
                            onToggle = onToggleHaptics,
                            testTag = "settings_haptics_switch"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 2: GAMEPLAY SPEED
                SettingsSectionHeader(title = "ANIMATION SPEED")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimationSpeed.values().forEach { speed ->
                            val isSelected = animationSpeed == speed
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.verticalGradient(listOf(Color(0xFF16A34A), Color(0xFF15803D)))
                                        } else {
                                            Brush.verticalGradient(listOf(Color(0xFF1E0C04), Color(0xFF140804)))
                                        }
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isSelected) Color(0xFF86EFAC) else Color(0xFF78350F).copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSetAnimationSpeed(speed) }
                                    .testTag("speed_option_${speed.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (speed == AnimationSpeed.FAST) Icons.Default.FastForward else Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${speed.title} (${if (speed == AnimationSpeed.FAST) "1.5x" else "1.0x"})",
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 2b: AI DIFFICULTY
                if (onSetAiDifficulty != null) {
                    SettingsSectionHeader(title = "AI BOT DIFFICULTY")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AiDifficulty.values().forEach { diff ->
                                val isSelected = aiDifficulty == diff
                                val diffColor = when (diff) {
                                    AiDifficulty.EASY -> Color(0xFF4ADE80)
                                    AiDifficulty.MEDIUM -> Color(0xFF38BDF8)
                                    AiDifficulty.HARD -> Color(0xFFEF4444)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
                                            } else {
                                                Brush.verticalGradient(listOf(Color(0xFF1E0C04), Color(0xFF140804)))
                                            }
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF78350F).copy(alpha = 0.5f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSetAiDifficulty(diff) }
                                        .testTag("settings_difficulty_${diff.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = diff.title,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                        )
                                        Text(
                                            text = when (diff) {
                                                AiDifficulty.EASY -> "Casual"
                                                AiDifficulty.MEDIUM -> "Balanced"
                                                AiDifficulty.HARD -> "Tactical"
                                            },
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color(0xFFE0F2FE) else diffColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // SECTION 2c: BOARD THEME
                if (onSetBoardTheme != null) {
                    SettingsSectionHeader(title = "BOARD THEME")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BoardTheme.values().forEach { theme ->
                                val isSelected = boardTheme == theme
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF78350F)))
                                            } else {
                                                Brush.verticalGradient(listOf(Color(0xFF1E0C04), Color(0xFF140804)))
                                            }
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) Color(0xFFFDE047) else Color(0xFF78350F).copy(alpha = 0.5f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSetBoardTheme(theme) }
                                        .testTag("settings_theme_${theme.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = theme.title,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // SECTION 2d: ANIMATED BACKGROUND
                if (onSetAnimatedBackground != null) {
                    SettingsSectionHeader(title = "ANIMATED BACKGROUND")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnimatedBackground.values().forEach { bg ->
                                val isSelected = animatedBackground == bg
                                val chipColor = when (bg) {
                                    AnimatedBackground.ROYAL_GOLD -> Color(0xFFFDE047)
                                    AnimatedBackground.DEEP_OCEAN -> Color(0xFF38BDF8)
                                    AnimatedBackground.EMERALD_FOREST -> Color(0xFF86EFAC)
                                    AnimatedBackground.GALAXY -> Color(0xFFC084FC)
                                    AnimatedBackground.SUNSET -> Color(0xFFFB7185)
                                    AnimatedBackground.ROYAL_PALACE -> Color(0xFFF59E0B)
                                    AnimatedBackground.JUNGLE -> Color(0xFF10B981)
                                    AnimatedBackground.SPACE -> Color(0xFF818CF8)
                                    AnimatedBackground.FESTIVAL -> Color(0xFFF97316)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.verticalGradient(listOf(chipColor.copy(alpha = 0.45f), Color(0xFF140804)))
                                            } else {
                                                Brush.verticalGradient(listOf(Color(0xFF1E0C04), Color(0xFF140804)))
                                            }
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) chipColor else Color(0xFF78350F).copy(alpha = 0.5f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSetAnimatedBackground(bg) }
                                        .testTag("animated_bg_${bg.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = bg.title,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // SECTION 3: SKINS & APPEARANCE
                SettingsSectionHeader(title = "CUSTOMIZATION")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSkins() }
                        .testTag("settings_open_skins_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, currentSkin.primaryTone.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = currentSkin.primaryTone,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Game Skins & Styles",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Active: ${currentSkin.title}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFDE047)
                                )
                            }
                        }

                        Text(
                            text = "CHANGE >",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 4: HELP & MATCH ACTIONS
                SettingsSectionHeader(title = "GAME ACTIONS")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenRules,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_rules_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Rules", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }

                    if (onRestartMatch != null) {
                        Button(
                            onClick = { showConfirmRestartMatch = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_restart_match_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Restart", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // RESET TO DEFAULT OPTION
                if (onResetSettingsToDefaults != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showConfirmResetSettings = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("settings_reset_to_default_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reset Settings to Default",
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (onResetStats != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showConfirmReset = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("settings_reset_stats_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Reset All Player Statistics", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_done_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "DONE", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Confirm Reset Settings to Default Dialog
    if (showConfirmResetSettings && onResetSettingsToDefaults != null) {
        AlertDialog(
            onDismissRequest = { showConfirmResetSettings = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(text = "Reset Settings to Default?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will restore the following configurable preferences to defaults:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Sound Effects: ON\n• Haptic Vibration: ON\n• Animation Speed: Normal (1.0x)\n• AI Bot Difficulty: Medium (Balanced)\n• Board Theme: Classic\n• Game Skin: Classic Gold",
                        color = Color(0xFFFDE047),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Custom player names, avatars, match statistics, game history, and active saved games will NOT be deleted.",
                        color = Color(0xFF4ADE80),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmResetSettings = false
                        onResetSettingsToDefaults()
                        resetSettingsSuccessMessage = "Settings successfully restored to default!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_reset_settings_button")
                ) {
                    Text(text = "Reset to Default", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmResetSettings = false },
                    modifier = Modifier.testTag("cancel_reset_settings_button")
                ) {
                    Text(text = "Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E0E07),
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Confirm Restart Match Dialog
    if (showConfirmRestartMatch && onRestartMatch != null) {
        AlertDialog(
            onDismissRequest = { showConfirmRestartMatch = false },
            icon = {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
            },
            title = {
                Text(text = "Restart Current Match?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    text = "Are you sure you want to restart this match? All tokens will be returned to their base yard and the game will start over from turn 1.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmRestartMatch = false
                        onRestartMatch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_restart_match_button")
                ) {
                    Text(text = "Restart Match", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmRestartMatch = false },
                    modifier = Modifier.testTag("cancel_restart_match_button")
                ) {
                    Text(text = "Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E0E07),
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Confirm Reset Stats Dialog
    if (showConfirmReset && onResetStats != null) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            icon = {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
            },
            title = {
                Text(text = "Reset Player Statistics?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to reset all player stats? The following records will be permanently deleted:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Total matches played\n• Wins and losses for all player colors\n• Tokens captured count\n• Tokens reached home count\n• Win rate percentages",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Player names, avatars, and any active saved game will NOT be deleted.",
                        color = Color(0xFFFDE047),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmReset = false
                        onResetStats()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_settings_reset_stats")
                ) {
                    Text(text = "Confirm Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmReset = false },
                    modifier = Modifier.testTag("cancel_settings_reset_stats")
                ) {
                    Text(text = "Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E0E07),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFDE047),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onToggle() }
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF16A34A),
                uncheckedThumbColor = Color(0xFFCBD5E1),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}
