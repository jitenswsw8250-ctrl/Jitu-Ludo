package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.model.TokenPosition
import com.example.model.TurnPhase

@Composable
fun TopPlayerProfileCard(
    player: Player,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("top_profile_${player.color.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2B1610).copy(alpha = 0.92f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = player.color.primaryColor.copy(alpha = 0.75f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Avatar Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF1E3A8A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (player.isBot) "🤖" else "😊",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Captures & Losses (Kills & Deaths matching reference photo)
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${player.captures}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "⚔️", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${player.losses}",
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "💀", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PlayerStatusBar(
    players: List<Player>,
    currentTurnIndex: Int,
    phase: TurnPhase,
    movableTokenIds: Set<Int>,
    onTokenClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        players.forEachIndexed { index, player ->
            val isCurrentTurn = index == currentTurnIndex
            PlayerMiniCard(
                player = player,
                isCurrentTurn = isCurrentTurn,
                phase = phase,
                movableTokenIds = if (isCurrentTurn) movableTokenIds else emptySet(),
                onTokenClicked = onTokenClicked,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PlayerMiniCard(
    player: Player,
    isCurrentTurn: Boolean,
    phase: TurnPhase,
    movableTokenIds: Set<Int>,
    onTokenClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isCurrentTurn) player.color.primaryColor else Color.Transparent,
        label = "borderGlow"
    )

    Card(
        modifier = modifier
            .border(
                width = if (isCurrentTurn) 2.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("player_card_${player.color.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTurn) {
                player.color.lightColor.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentTurn) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Player Color Pill & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(player.color.primaryColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = player.name,
                    fontSize = 11.sp,
                    fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Token progress indicators (4 mini dots/badges)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                player.tokens.forEach { token ->
                    val isMovable = movableTokenIds.contains(token.id) && phase == TurnPhase.SELECTING_TOKEN
                    val tokenBg = when (token.position) {
                        is TokenPosition.InBase -> player.color.primaryColor.copy(alpha = 0.3f)
                        is TokenPosition.OnBoard -> player.color.primaryColor
                        is TokenPosition.Finished -> Color(0xFFF59E0B) // Gold
                    }

                    Box(
                        modifier = Modifier
                            .size(if (isMovable) 18.dp else 14.dp)
                            .clip(CircleShape)
                            .background(tokenBg)
                            .then(
                                if (isMovable) {
                                    Modifier.border(2.dp, Color(0xFFFBBF24), CircleShape)
                                } else Modifier
                            )
                            .clickable(enabled = isMovable) {
                                onTokenClicked(token.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (token.isFinished) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Finished",
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                        } else if (token.position is TokenPosition.OnBoard) {
                            Text(
                                text = "${token.id + 1}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            if (player.rank != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF59E0B)
                ) {
                    Text(
                        text = when (player.rank) {
                            1 -> "🏆 1st"
                            2 -> "🥈 2nd"
                            3 -> "🥉 3rd"
                            else -> "4th"
                        },
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else {
                Text(
                    text = "${player.finishedTokensCount}/4 Home",
                    fontSize = 9.sp,
                    color = if (player.finishedTokensCount > 0) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
