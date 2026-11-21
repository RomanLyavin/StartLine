package com.startline.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startline.app.data.Fmt
import com.startline.app.data.Sport
import com.startline.app.data.Workout
import com.startline.app.domain.DailyPoint
import kotlin.math.max
import kotlin.math.min

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = TextPrimary,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription, tint = if (enabled) tint else TextSecondary)
    }
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier.fillMaxWidth()
    Surface(
        modifier = if (onClick != null) base.clickable { onClick() } else base,
        shape = RoundedCornerShape(20.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        letterSpacing = 1.sp,
    )
}

@Composable
fun Pill(text: String, color: Color = Lime, solid: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (solid) color else color.copy(alpha = 0.12f),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (solid) Bg else color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SportIcon(sport: Sport, size: Int = 18) {
    Icon(
        imageVector = if (sport == Sport.RUN) Icons.Filled.DirectionsRun else Icons.Filled.DirectionsBike,
        contentDescription = sport.label,
        tint = if (sport == Sport.RUN) Lime else Cyan,
        modifier = Modifier.size(size.dp),
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Lime,
            contentColor = Bg,
            disabledContainerColor = Surface3,
            disabledContentColor = TextSecondary,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Bg,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text, color = if (enabled) Lime else TextSecondary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = TextSecondary) } },
        singleLine = singleLine,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        supportingText = supporting?.let { { Text(it, color = TextSecondary) } },
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(),
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = Lime,
    unfocusedBorderColor = Border,
    focusedLabelColor = Lime,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Lime,
    focusedContainerColor = Surface2,
    unfocusedContainerColor = Surface2,
    focusedPlaceholderColor = TextSecondary,
    unfocusedPlaceholderColor = TextSecondary,
)

@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = TextPrimary,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
    }
}

@Composable
fun EmptyCard(text: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            GhostButton(actionText, onAction, modifier = Modifier.padding(start = 30.dp))
        }
    }
}

@Composable
fun FitnessChart(points: List<DailyPoint>, modifier: Modifier = Modifier) {
    if (points.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Недостаточно данных для графика", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val maxValue = max(
        points.maxOf { it.ctl },
        points.maxOf { it.atl },
    ).coerceAtLeast(10.0) * 1.12

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp),
    ) {
        val w = size.width
        val h = size.height
        val pad = 6f
        val chartH = h - pad * 2
        for (i in 0..4) {
            val y = pad + chartH * i / 4f
            drawLine(Border.copy(alpha = 0.55f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        fun xAt(index: Int): Float = if (points.size == 1) 0f else w * index / (points.size - 1f)
        fun yAt(v: Double): Float = pad + chartH - (v / maxValue * chartH).toFloat()

        fun pathFor(selector: (DailyPoint) -> Double): Path {
            val path = Path()
            points.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(xAt(i), yAt(selector(p))) else path.lineTo(xAt(i), yAt(selector(p)))
            }
            return path
        }

        val ctlPath = pathFor { it.ctl }
        val atlPath = pathFor { it.atl }

        val area = Path().apply {
            addPath(ctlPath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(Lime.copy(alpha = 0.22f), Color.Transparent)))

        drawPath(atlPath, Orange, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawPath(ctlPath, Lime, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        val last = points.last()
        drawCircle(Lime, radius = 3.5.dp.toPx(), center = Offset(w, yAt(last.ctl)))
    }
}

@Composable
fun ReadinessRing(score: Int, label: String, modifier: Modifier = Modifier) {
    val color = when {
        score >= 70 -> Lime
        score >= 45 -> Orange
        else -> Red
    }
    Box(modifier = modifier.size(116.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            drawArc(
                color = Surface3,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.headlineMedium, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
        }
    }
}

@Composable
fun HrLineChart(heartrate: List<Int?>, modifier: Modifier = Modifier) {
    val valid = heartrate.filterNotNull()
    if (valid.size < 5) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Пульсовой ряд недоступен", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val minHr = valid.min().toDouble()
    val maxHr = valid.max().toDouble()
    val span = max(10.0, maxHr - minHr)
    val step = max(1, heartrate.size / 500)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            val w = size.width
            val h = size.height
            val pad = 6f
            val chartH = h - pad * 2
            for (i in 0..3) {
                val y = pad + chartH * i / 3f
                drawLine(Border.copy(alpha = 0.5f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            val count = heartrate.size
            var lastX = 0f
            var lastY = 0f
            val path = Path()
            var started = false
            var idx = 0
            while (idx < count) {
                val hr = heartrate[idx]
                if (hr != null) {
                    val x = w * idx / (count - 1f)
                    val y = pad + chartH - ((hr - minHr) / span * chartH).toFloat()
                    if (!started) {
                        path.moveTo(x, y)
                        started = true
                    } else {
                        path.lineTo(x, y)
                    }
                    lastX = x
                    lastY = y
                }
                idx += step
            }
            if (started) {
                drawPath(path, Cyan, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(Cyan, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("мин $minHr", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("макс $maxHr", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = Lime,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) color else Surface2,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) color else Border),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() },
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Bg else TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ZoneBar(
    fractions: List<Double>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(TextSecondary, Cyan, Lime, Orange, Red),
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp)),
    ) {
        var x = 0f
        fractions.forEachIndexed { index, fraction ->
            val w = (size.width * fraction).toFloat()
            if (w > 0f) {
                drawRect(colors[index % colors.size], topLeft = Offset(x, 0f), size = Size(w, size.height))
                x += w
            }
        }
        if (x < size.width) {
            drawRect(Surface3, topLeft = Offset(x, 0f), size = Size(size.width - x, size.height))
        }
    }
}

@Composable
fun WorkoutRow(workout: Workout, showDate: Boolean = true, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDate) {
            Column(
                modifier = Modifier.width(42.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    workout.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    Fmt.weekday(workout.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (workout.sport == Sport.RUN) Lime.copy(alpha = 0.12f) else Cyan.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            SportIcon(workout.sport, 18)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                workout.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${workout.durationMin} мин · ${workout.target}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            Fmt.load(workout.load),
            style = MaterialTheme.typography.labelMedium,
            color = if (workout.type == "race") Orange else TextSecondary,
        )
    }
}

@Composable
fun RaceRow(
    name: String,
    sport: Sport,
    dateText: String,
    priority: String,
    extra: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Flag, null, tint = if (priority == "A") Orange else Cyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1)
            Text(
                listOfNotNull(dateText, sport.label, extra).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
            )
        }
        Pill("$priority", if (priority == "A") Orange else if (priority == "B") Cyan else TextSecondary)
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            trailing()
        }
    }
}

@Composable
fun LabelValue(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Lime, strokeWidth = 2.dp)
    }
}

@Composable
fun AuthSpacer() {
    Spacer(Modifier.height(14.dp))
}

@Composable
fun RefreshAction(syncing: Boolean, onClick: () -> Unit) {
    if (syncing) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Lime, strokeWidth = 2.dp)
        }
    } else {
        IconAction(Icons.Filled.Refresh, "Обновить", onClick)
    }
}

@Composable
fun TrendIcon(up: Boolean) {
    Icon(
        if (up) Icons.Filled.TrendingUp else Icons.Filled.TrendingUp,
        null,
        tint = if (up) Lime else Orange,
        modifier = Modifier.size(16.dp),
    )
}
