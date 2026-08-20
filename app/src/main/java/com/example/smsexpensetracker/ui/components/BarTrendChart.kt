package com.example.smsexpensetracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsexpensetracker.theme.PrimaryBlue

data class BarData(
    val label: String,
    val amount: Double,
    val isHighlighted: Boolean = false
)

@Composable
fun BarTrendChart(
    bars: List<BarData>,
    modifier: Modifier = Modifier,
    chartHeight: Float = 120f,
    barColor: Color = PrimaryBlue,
    highlightColor: Color = MaterialTheme.colorScheme.primary
) {
    val maxAmount = (bars.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(bars) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, tween(700))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight.dp)
                .padding(horizontal = 8.dp)
        ) {
            val count = bars.size
            if (count == 0) return@Canvas

            val spacing = size.width / count
            val barWidth = (spacing * 0.45f).coerceAtMost(28f)

            for ((index, bar) in bars.withIndex()) {
                val normalizedHeight = (bar.amount / maxAmount).toFloat() * size.height * animatedProgress.value
                val barHeight = normalizedHeight.coerceAtLeast(4f)
                val left = index * spacing + (spacing - barWidth) / 2f
                val top = size.height - barHeight

                val color = if (bar.isHighlighted) highlightColor else barColor.copy(alpha = 0.75f)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (bar in bars) {
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (bar.isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
