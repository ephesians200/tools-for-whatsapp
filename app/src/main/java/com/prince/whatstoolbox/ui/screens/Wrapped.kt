package com.prince.whatstoolbox.ui.screens
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prince.whatstoolbox.domain.usecase.ChatReport
import com.prince.whatstoolbox.ui.components.*
import com.prince.whatstoolbox.ui.theme.Amber
import com.prince.whatstoolbox.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** [pdf] = the "Chat to PDF" tool (Pro). The free Chat Wrapped screen has no PDF export, so it can't be used to get around the lock. */
@Composable fun ChatAnalytics(pdf: Boolean = false) { val c = LocalContext.current
    Text("In WhatsApp: open a chat → ⋮ → More → Export chat → Without media → choose ${androidx.compose.ui.res.stringResource(com.prince.whatstoolbox.R.string.app_name)}.", style = MaterialTheme.typography.bodySmall)
    ChatInput { r -> r.stats?.let { s ->
        WrappedStories(s, r)
        SectionCard("Busiest days", s.byDay.map { "${it.first}: ${it.second}" })
        SectionCard("Silent members (<3 msgs)", r.silent)
        SectionCard("Links (${r.links.size})", r.links.take(10))
        if (pdf) {
        Text("Styled PDF", style = MaterialTheme.typography.titleSmall)
        var me by remember(s) { mutableStateOf(s.bySender.firstOrNull()?.first) }
        Text("Which one is you? (your bubbles go on the right)", style = MaterialTheme.typography.bodySmall)
        val names = s.bySender.take(6).map { it.first }
        Chips(names, names.indexOf(me)) { me = names[it] }
        ShareBar("application/pdf") { listOf(ChatParser.toPdf(c, r.msgs, me)) } }
        else Text("Want a printable PDF of this chat? Open “Chat to PDF” (Pro) from the Tools tab.", style = MaterialTheme.typography.bodySmall) } } }

private val Peak = Amber
/** "Wrapped": swipeable story cards, numbers count up and bars grow when a card becomes the current page; every card can be shared as an image. */
@Composable private fun WrappedStories(s: ChatStats, r: ChatReport) {
    val pager = rememberPagerState { 4 }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalPager(pager, Modifier.fillMaxWidth().height(470.dp), pageSpacing = 12.dp) { i ->
            val active = pager.currentPage == i
            when (i) {
                0 -> StoryCard(listOf(Color(0xFF6D5BE8), Color(0xFF3A2A9C)), "total") {
                    Text("Your chat in numbers", color = Color.White.copy(0.8f), style = MaterialTheme.typography.titleMedium); Spacer(Modifier.weight(1f))
                    CountUp(s.total, MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp), Color.White, active); Text("messages", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.weight(1f)); Text("${s.bySender.size} people · ${s.byDay.firstOrNull()?.let { "busiest day ${it.first} (${it.second})" } ?: ""}", color = Color.White.copy(0.85f)) }
                1 -> StoryCard(listOf(Color(0xFF128C7E), Color(0xFF075E54)), "senders") {
                    Text("Who talks the most", color = Color.White.copy(0.8f), style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(16.dp))
                    val top = s.bySender.take(5); val max = (top.firstOrNull()?.second ?: 1).toFloat()
                    top.forEachIndexed { idx, (name, n) -> Column(Modifier.padding(vertical = 6.dp)) {
                        Row { Text(name, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1); Text("$n", color = Color.White) }
                        val f by animateFloatAsState(if (active) n / max else 0f, tween(800, delayMillis = idx * 120, easing = FastOutSlowInEasing), label = "sender")
                        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color.White.copy(0.2f))) { Box(Modifier.fillMaxWidth(f.coerceIn(0.02f, 1f)).fillMaxHeight().clip(CircleShape).background(if (idx == 0) Peak else Color.White)) } } } }
                2 -> StoryCard(listOf(Color(0xFFFF7E5F), Color(0xFFB4408A)), "hours") {
                    Text("When you chat", color = Color.White.copy(0.85f), style = MaterialTheme.typography.titleMedium)
                    val peak = s.byHour.maxByOrNull { it.value }?.key; val max = (s.byHour.values.maxOrNull() ?: 1).toFloat()
                    Text(if (peak != null) "Peak hour: %02d:00".format(peak) else "", color = Color.White, style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth().height(220.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (h in 0..23) { val f by animateFloatAsState(if (active) (s.byHour[h] ?: 0) / max else 0f, tween(700, delayMillis = h * 25, easing = FastOutSlowInEasing), label = "hour")
                            Box(Modifier.weight(1f).fillMaxHeight(f.coerceIn(0.02f, 1f)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(if (h == peak) Peak else Color.White.copy(0.85f))) } }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { listOf("0", "6", "12", "18", "23").forEach { Text(it, color = Color.White.copy(0.85f), style = MaterialTheme.typography.labelSmall) } } }
                else -> StoryCard(listOf(Color(0xFFF5A623), Color(0xFFE0457B)), "emoji") {
                    Text("Your top emoji", color = Color.White.copy(0.9f), style = MaterialTheme.typography.titleMedium); Spacer(Modifier.weight(1f))
                    if (s.topEmoji.isEmpty()) Text("No emoji found", color = Color.White) else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { s.topEmoji.take(3).forEach { (e, n) -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val sc by animateFloatAsState(if (active) 1f else 0.3f, spring(Spring.DampingRatioMediumBouncy), label = "emoji")
                            Text(e, fontSize = (56 * sc).sp); CountUp(n, MaterialTheme.typography.titleMedium, Color.White, active) } } }
                        Spacer(Modifier.height(16.dp)); Text(s.topEmoji.drop(3).joinToString("   ") { "${it.first}×${it.second}" }, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    Spacer(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { repeat(4) { i -> val w by animateDpAsState(if (pager.currentPage == i) 22.dp else 8.dp, label = "dot")
            Box(Modifier.padding(3.dp).height(8.dp).width(w).clip(CircleShape).background(if (pager.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) } }
    }
}

/** A gradient story card that can be captured to a PNG with its own graphics layer and shared. */
@Composable private fun StoryCard(colors: List<Color>, name: String, content: @Composable ColumnScope.() -> Unit) {
    val layer = rememberGraphicsLayer(); val scope = rememberCoroutineScope(); val c = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp)).drawWithContent { layer.record { this@drawWithContent.drawContent() }; drawLayer(layer) }
            .background(Brush.verticalGradient(colors)).padding(22.dp)) { Column(Modifier.fillMaxSize(), content = content) }
        TextButton({ scope.launch { val bmp = layer.toImageBitmap().asAndroidBitmap()
            val f = withContext(Dispatchers.IO) { Out.saveBitmap(c, bmp, "wrapped_$name.png") }; Out.shareToWhatsApp(c, listOf(f), "image/png") } }, Modifier.align(Alignment.CenterHorizontally)) { Text("Share this card") }
    }
}
