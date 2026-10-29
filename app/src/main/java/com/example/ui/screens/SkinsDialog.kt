package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GameSkin
import com.example.ui.components.DiceFace

@Composable
fun SkinsDialog(
    currentSkin: GameSkin,
    onSkinSelected: (GameSkin) -> Unit,
    onDismiss: () -> Unit
) {
    var previewSkin by remember { mutableStateOf(currentSkin) }

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
                .testTag("skins_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
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
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TOKEN & DICE SKINS",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Personalize your game pieces",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("skins_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Preview Stage Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261208)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, previewSkin.primaryTone.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // 3D Token Preview
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TokenPreviewCanvas(skin = previewSkin, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Token Piece",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(46.dp)
                                .background(Color(0xFF451A03))
                        )

                        // 3D Die Preview
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DiceFace(
                                value = 6,
                                isRolling = false,
                                accentColor = previewSkin.primaryTone,
                                enabled = false,
                                onClick = {},
                                diceSizeDp = 46.dp,
                                skin = previewSkin
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lucky Die",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Available Skins
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GameSkin.values()) { skin ->
                        val isEquipped = currentSkin == skin
                        val isInspected = previewSkin == skin

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    previewSkin = skin
                                    onSkinSelected(skin)
                                }
                                .testTag("skin_item_${skin.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isInspected) Color(0xFF381A0B) else Color(0xFF1E0C04)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isEquipped) 2.dp else 1.dp,
                                color = if (isEquipped) Color(0xFFFDE047) else skin.primaryTone.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Colored swatch
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(skin.accentTone, skin.primaryTone, skin.rimTone)
                                                )
                                            )
                                            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = skin.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = skin.description,
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                if (isEquipped) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF16A34A)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "EQUIPPED",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            previewSkin = skin
                                            onSkinSelected(skin)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047))
                                    ) {
                                        Text(
                                            text = "EQUIP",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFDE047)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("skins_done_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "DONE", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TokenPreviewCanvas(skin: GameSkin, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cX = w / 2f
        val cY = h * 0.65f
        val tokenRadius = w * 0.38f

        val (bLight, bPrim, bDark) = when (skin) {
            GameSkin.CLASSIC -> Triple(Color(0xFFFCA5A5), Color(0xFFDC2626), Color(0xFF991B1B))
            GameSkin.RUBY -> Triple(Color(0xFFFDA4AF), Color(0xFFE11D48), Color(0xFF881337))
            GameSkin.EMERALD -> Triple(Color(0xFFA7F3D0), Color(0xFF059669), Color(0xFF064E3B))
            GameSkin.SAPPHIRE -> Triple(Color(0xFFBAE6FD), Color(0xFF2563EB), Color(0xFF1E3A8A))
            GameSkin.GOLD -> Triple(Color(0xFFFEF08A), Color(0xFFEAB308), Color(0xFF78350F))
            GameSkin.SILVER -> Triple(Color(0xFFF8FAFC), Color(0xFF94A3B8), Color(0xFF334155))
        }

        // 1. Pedestal / Bell
        val bellTop = cY - tokenRadius * 0.15f
        val bellHeight = tokenRadius * 0.95f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(bLight, bPrim, bDark),
                center = Offset(cX - tokenRadius * 0.35f, bellTop + bellHeight * 0.25f),
                radius = tokenRadius * 1.2f
            ),
            topLeft = Offset(cX - tokenRadius * 0.82f, bellTop),
            size = Size(tokenRadius * 1.64f, bellHeight)
        )
        // Red team collar accent
        drawOval(
            color = Color(0xFFDC2626),
            topLeft = Offset(cX - tokenRadius * 0.82f, bellTop),
            size = Size(tokenRadius * 1.64f, bellHeight),
            style = Stroke(width = 3f)
        )

        // 2. Collar
        val collarCenter = Offset(cX, cY - tokenRadius * 0.20f)
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(Color.White, Color(0xFFDC2626), Color(0xFF991B1B)),
                start = Offset(collarCenter.x - tokenRadius * 0.45f, collarCenter.y),
                end = Offset(collarCenter.x + tokenRadius * 0.45f, collarCenter.y)
            ),
            topLeft = Offset(collarCenter.x - tokenRadius * 0.48f, collarCenter.y - tokenRadius * 0.16f),
            size = Size(tokenRadius * 0.96f, tokenRadius * 0.32f)
        )

        // 3. Spherical Head
        val headCenter = Offset(cX, cY - tokenRadius * 0.50f)
        val headRadius = tokenRadius * 0.60f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, bLight, bPrim, bDark),
                center = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.35f),
                radius = headRadius * 1.3f
            ),
            radius = headRadius,
            center = headCenter
        )
        // Glint
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = headRadius * 0.25f,
            center = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.35f)
        )
    }
}
