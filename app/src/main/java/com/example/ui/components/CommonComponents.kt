package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// Glassmorphic Card container with thin brushed metallic borders and gradient backing
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    glowColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerRadius = 18.dp
    
    Card(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .drawBehind {
                // Background glass gradient matching the Immersive UI spec (rgba(255,255,255,0.05) to rgba(255,255,255,0.01))
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.01f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
                if (glowColor != Color.Transparent) {
                    drawCircle(
                        color = glowColor.copy(alpha = 0.08f),
                        radius = size.maxDimension * 0.45f,
                        center = Offset(size.width * 0.8f, size.height * 0.2f)
                    )
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            content()
        }
    }
}

// Interactive Premium Slate Buttons with Metallic Hover Transitions
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = SilverPrimary,
    textColor: Color = PremiumBg,
    icon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .border(
                width = 1.dp,
                color = if (enabled) GlassBorder else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(14.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = textColor,
            disabledContainerColor = Color.White.copy(alpha = 0.08f),
            disabledContentColor = SilverSecondary.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

// Futuristic Secondary Glowing Outlined Button
@Composable
fun GlassOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = SilverSecondary.copy(alpha = 0.5f),
    textColor: Color = SilverPrimary,
    icon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = textColor,
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

// Stunning Custom Canvas-Based Monthly Revenue / Collection Chart
// Avoids heavy dependency files, supports precise Nothing-Phone and CRED dot design style
@Composable
fun GlanceChart(
    dataPoints: List<Pair<String, Double>>, // Month/Label -> Amount
    modifier: Modifier = Modifier,
    barColor: Color = SilverPrimary,
    accentColor: Color = NeonCyan
) {
    val maxVal = if (dataPoints.isEmpty()) 10000.0 else (dataPoints.maxOf { it.second }.coerceAtLeast(100.0))
    val textPaintColor = MaterialTheme.colorScheme.onBackground
    
    Column(modifier = modifier) {
        if (dataPoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No collection records plotted yet.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = SilverSecondary)
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(vertical = 12.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                val spacing = 28f
                val chartWidth = canvasWidth - (spacing * 2)
                val chartHeight = canvasHeight - 40f // Leave space for X-axis labels

                val barCount = dataPoints.size
                val barGap = 32f
                val totalGapWidth = barGap * (barCount - 1)
                val barWidth = (chartWidth - totalGapWidth) / barCount

                dataPoints.forEachIndexed { idx, item ->
                    val value = item.second
                    val ratio = (value / maxVal).toFloat()
                    val barHeight = chartHeight * ratio

                    val xOffset = spacing + idx * (barWidth + barGap)
                    val yOffset = chartHeight - barHeight

                    // Draw Glassmorphic Backing Shadow
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.03f),
                        topLeft = Offset(xOffset, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Draw primary gradient bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.85f),
                                barColor.copy(alpha = 0.2f)
                            )
                        ),
                        topLeft = Offset(xOffset, yOffset),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Draw sparkling top glow notch for active bars
                    if (barHeight > 10f) {
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(xOffset, yOffset),
                            size = Size(barWidth, 6.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            // Labels Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dataPoints.forEach { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = item.first,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SilverSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "₹${(item.second / 1000).toInt()}k",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SilverPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

// Infinite pulsing glow for cloud status & credentials sync
@Composable
fun PulsingGlowDot(
    state: com.example.ui.viewmodel.SyncState,
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    val color = when {
        isOffline -> AmberglowYellow()
        state == com.example.ui.viewmodel.SyncState.SYNCING -> NeonCyan
        state == com.example.ui.viewmodel.SyncState.SUCCESS -> NeonEmerald
        else -> NeonRose
    }

    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Pulsing Glow Circle
        if (state == com.example.ui.viewmodel.SyncState.SYNCING || state == com.example.ui.viewmodel.SyncState.SUCCESS) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .drawBehind {
                        drawCircle(
                            color = color,
                            radius = (size.width / 2) * pulseScale,
                            alpha = pulseAlpha
                        )
                    }
            )
        }
        
        // Inner Static Solid Core
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
    }
}

// Clean premium color helpers
fun AmberglowYellow() = Color(0xFFF59E0B)

// Stylized Material Input Field with liquid borders
@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = SilverSecondary, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = SilverSecondary.copy(alpha = 0.4f), fontSize = 14.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SilverPrimary,
            unfocusedTextColor = SilverPrimary,
            focusedContainerColor = PremiumCardBg.copy(alpha = 0.5f),
            unfocusedContainerColor = PremiumCardBg.copy(alpha = 0.5f),
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = GlassBorder,
            disabledBorderColor = GlassBorder,
            errorBorderColor = NeonRose,
            cursorColor = NeonCyan
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
