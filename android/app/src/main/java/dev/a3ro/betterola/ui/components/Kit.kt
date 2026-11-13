package dev.a3ro.betterola.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.a3ro.betterola.ui.theme.tokens
import kotlinx.coroutines.launch

/** 4x4 Bayer matrix, the same ordered dither the HUD uses. */
private val Bayer = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

/** True when the user turned animations off in system settings. */
@Composable
fun reducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * A bar whose fill is drawn as ordered dither. [fraction] is the charge, so the
 * dither edge itself reads as the number. Null means unknown and draws only the frame.
 */
@Composable
fun DitherBar(
    fraction: Float?,
    modifier: Modifier = Modifier,
    color: Color = tokens.amber,
    cell: Dp = 2.dp,
    description: String,
) {
    val frame = tokens.hairline
    Canvas(
        modifier
            .height(28.dp)
            .fillMaxWidth()
            .border(1.dp, frame)
            .padding(2.dp)
            .semantics { contentDescription = description }
    ) {
        if (fraction == null) return@Canvas
        val px = cell.toPx()
        val cols = (size.width / px).toInt()
        val rows = (size.height / px).toInt()
        val edge = 0.12f
        for (j in 0 until rows) for (i in 0 until cols) {
            val u = i / cols.toFloat()
            if (u > fraction) continue
            val density = if (u > fraction - edge) ((fraction - u) / edge).coerceIn(0f, 1f) else 1f
            if (Bayer[(j % 4) * 4 + (i % 4)] / 16f < density) {
                drawRect(color, Offset(i * px, j * px), Size(px, px))
            }
        }
    }
}

/** Flat amber action. One per screen at most; amber means "this acts". */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) { Text(text, style = MaterialTheme.typography.titleMedium) }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/**
 * Hold to confirm. The fill is a dither sweep so a slip never triggers it.
 * With animations off (or TalkBack on, where holding is awkward) it falls back to a plain tap.
 */
@Composable
fun HoldButton(
    text: String,
    doneText: String,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Int = 600,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val still = reducedMotion()
    var done by remember { mutableStateOf(false) }
    val amber = tokens.amber
    val shape = MaterialTheme.shapes.small

    Box(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(1.dp, amber, shape)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = text) { onConfirmed(); true }
            }
            .pointerInput(still) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (still) {
                        if (waitForUpOrCancellation() != null) onConfirmed()
                        return@awaitEachGesture
                    }
                    val job = scope.launch {
                        progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        done = true
                        onConfirmed()
                    }
                    waitForUpOrCancellation()
                    if (!done) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f, tween(200)) }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val f = if (done) 1f else progress.value
            val px = 2.dp.toPx()
            val cols = (size.width / px).toInt()
            val rows = (size.height / px).toInt()
            for (j in 0 until rows) for (i in 0 until cols) {
                val u = i / cols.toFloat()
                if (u > f) continue
                val d = if (done) 1f else (1f - u * 0.7f)
                if (Bayer[(j % 4) * 4 + (i % 4)] / 16f < d) drawRect(amber, Offset(i * px, j * px), Size(px, px))
            }
        }
        Text(
            if (done) doneText else text,
            style = MaterialTheme.typography.titleMedium,
            color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

/** A label on the left, a value on the right, a hairline under it. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = tokens.ink2, onClick: (() -> Unit)? = null) {
    Column(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelMedium, color = valueColor, textAlign = TextAlign.End)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = tokens.ink2)
        }
    }
}

/** Monospace is for code and data only: addresses, logs. */
val Mono = FontFamily.Monospace

@Composable
fun StatusDot(live: Boolean) {
    Box(
        Modifier.size(6.dp).background(if (live) tokens.amber else tokens.ink2)
    )
}
