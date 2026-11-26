package com.prince.whatstoolbox.ui.components
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import com.prince.whatstoolbox.ui.theme.Amber

/** Springy press-scale + ripple. Use instead of plain `clickable` on cards. */
fun Modifier.pressable(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val down by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (down) 0.96f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "press")
    this.graphicsLayer { scaleX = s; scaleY = s }.clickable(interactionSource = src, indication = LocalIndication.current, onClick = onClick)
}

/** Favorite star that pops (and ticks haptically) when it becomes a favorite. */
@Composable fun FavStar(fav: Boolean, onToggle: () -> Unit) {
    val pop = remember { Animatable(1f) }; val haptic = LocalHapticFeedback.current; var first by remember { mutableStateOf(true) }
    LaunchedEffect(fav) {
        if (first) { first = false; return@LaunchedEffect }
        if (fav) { pop.animateTo(1.4f, tween(90)); pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }
    }
    IconButton({ haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggle() }) {
        Icon(if (fav) Icons.Default.Star else Icons.Default.StarBorder, if (fav) "Remove favorite" else "Add favorite",
            tint = if (fav) Amber else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value })
    }
}

/** Number that counts up from 0 when [active] becomes true (e.g. when its pager page is shown). */
@Composable fun CountUp(target: Int, style: TextStyle, color: Color = Color.Unspecified, active: Boolean = true) {
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(target, active) { t = if (active) target else 0 }
    val v by animateIntAsState(t, tween(1200, easing = FastOutSlowInEasing), label = "count")
    Text("%,d".format(v), style = style, color = color)
}
