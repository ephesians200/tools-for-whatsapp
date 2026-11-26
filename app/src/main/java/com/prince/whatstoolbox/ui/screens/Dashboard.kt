package com.prince.whatstoolbox.ui.screens
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prince.whatstoolbox.domain.Category
import com.prince.whatstoolbox.domain.Tool
import com.prince.whatstoolbox.domain.ToolRegistry
import com.prince.whatstoolbox.presentation.DashboardIntent
import com.prince.whatstoolbox.presentation.DashboardViewModel
import com.prince.whatstoolbox.ui.components.EmptyState
import com.prince.whatstoolbox.ui.components.LocalPremium
import com.prince.whatstoolbox.ui.components.NativeAdTile
import com.prince.whatstoolbox.ui.components.FavStar
import com.prince.whatstoolbox.ui.components.pressable
import com.prince.whatstoolbox.ui.icon
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.prince.whatstoolbox.R
import java.util.Calendar

private val TIPS = listOf(
    "Share an exported WhatsApp chat (.txt or .zip) to this app to see your Chat Wrapped.",
    "Select a phone number in any app, tap ⋮ → “Chat on WhatsApp” to message it without saving the contact.",
    "Star a tool to pin it to the top of this screen.",
    "Add the Direct Chat widget to your home screen for one-tap messaging.",
    "Your chats and files are processed on your phone — never uploaded. No account needed.")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Dashboard(open: (String) -> Unit, vm: DashboardViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle(); val cs = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(s.searching) { if (s.searching) runCatching { focus.requestFocus() } }
    BackHandler(enabled = s.searching) { vm.onIntent(DashboardIntent.ToggleSearch) }
    val filtering = s.query.isNotBlank() || s.category != null || s.onlyFavorites
    val premium = LocalPremium.current
    // Locked tools open the paywall (AppRoot decides); they are not added to "Recently used".
    val launch = { id: String -> if (!ToolRegistry.isLocked(id, premium)) vm.onIntent(DashboardIntent.Open(id)); open(id) }
    val showAds = !premium && s.query.isBlank() && !s.onlyFavorites
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    // Featured tiles take their colours from the theme (so dynamic colour and dark mode both look right).
    val featured = listOf(Triple("DirectChat", cs.primaryContainer, cs.onPrimaryContainer), Triple("ChatAnalyticsWrapped", cs.tertiary, cs.onTertiary),
        Triple("StatusVideoSplitter", cs.primary, cs.onPrimary), Triple("UnicodeFancyFonts", cs.secondaryContainer, cs.onSecondaryContainer))

    Scaffold(containerColor = cs.background, topBar = {
        TopAppBar(title = { AnimatedContent(s.searching, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "title") { searching ->
            if (searching) TextField(s.query, { vm.onIntent(DashboardIntent.Query(it)) }, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true,
                placeholder = { Text("Search ${ToolRegistry.all.size} tools — try “pdf” or “qr”") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
            else Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), maxLines = 1, overflow = TextOverflow.Ellipsis) } },
            actions = { IconButton({ vm.onIntent(DashboardIntent.ToggleFavorites) }) { Icon(if (s.onlyFavorites) Icons.Default.Star else Icons.Default.StarBorder, "Show favorites only", tint = if (s.onlyFavorites) com.prince.whatstoolbox.ui.theme.Amber else cs.onSurfaceVariant) }
                IconButton({ vm.onIntent(DashboardIntent.ToggleSearch) }) { Icon(if (s.searching) Icons.Default.Close else Icons.Default.Search, if (s.searching) "Close search" else "Search") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)) }) { pad ->
        LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = full, key = "hello") { Column { Text("${ToolRegistry.all.size} tools and growing · no account needed", style = MaterialTheme.typography.labelLarge, color = cs.primary) } }
            if (!filtering) {
                items(featured, key = { "tile_" + it.first }) { (id, bg, fg) -> val t = ToolRegistry.byId(id)
                    Card(Modifier.height(124.dp).animateItem().pressable { launch(id) }, colors = CardDefaults.cardColors(bg, fg)) {
                        Column(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Icon(t.icon(), null, Modifier.size(28.dp)); if (ToolRegistry.isLocked(id, premium)) Icon(Icons.Default.Lock, "Pro", Modifier.size(18.dp)) }
                            Text(t.title, style = MaterialTheme.typography.titleMedium) } } }
                item(span = full, key = "tip") { val tip = TIPS[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % TIPS.size]
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(cs.surface)) { Column(Modifier.padding(16.dp)) {
                        Text("💡 Tip", style = MaterialTheme.typography.labelLarge, color = cs.primary); Text(tip, style = MaterialTheme.typography.bodyMedium) } } }
                if (s.favoriteTools.isNotEmpty()) { item(span = full, key = "h_fav") { Text("⭐ Favorites", style = MaterialTheme.typography.titleMedium) }
                    item(span = full, key = "row_fav") { ToolRow(s.favoriteTools, premium, launch) } }
                if (s.recentTools.isNotEmpty()) { item(span = full, key = "h_recent") { Text("🕘 Recently used", style = MaterialTheme.typography.titleMedium) }
                    item(span = full, key = "row_recent") { ToolRow(s.recentTools, premium, launch) } } }
            item(span = full, key = "chips") { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(s.category == null, { vm.onIntent(DashboardIntent.Select(null)) }, { Text("All") }) }
                items(Category.entries.toList()) { cat -> FilterChip(s.category == cat, { vm.onIntent(DashboardIntent.Select(if (s.category == cat) null else cat)) }, { Text(cat.label) }, leadingIcon = { Icon(cat.icon(), null, Modifier.size(18.dp)) }) } } }
            if (s.tools.isEmpty()) item(span = full, key = "empty") {
                if (s.onlyFavorites && s.query.isBlank() && s.category == null) EmptyState(Icons.Default.StarBorder, "No favorites yet", "Tap the ☆ on any tool to pin it here.")
                else EmptyState(Icons.Default.SearchOff, "No tools match", "Try a different word — for example “pdf”, “qr” or “invoice”.") }
            else if (filtering) {
                items(s.tools, key = { "t_" + it.id }) { t -> ToolCard(t, t.id in s.favorites, ToolRegistry.isLocked(t.id, premium), Modifier.animateItem(), { launch(t.id) }) { vm.onIntent(DashboardIntent.ToggleFavorite(t.id)) } }
                // A single category chip selected: its grid ends with the video ad too.
                if (showAds && s.category in ToolRegistry.AD_CATEGORIES) item(key = "ad_" + s.category!!.name) { NativeAdTile(s.category!!.name, Modifier.animateItem()) } }
            else Category.entries.forEach { cat -> val list = s.tools.filter { it.category == cat }
                if (list.isNotEmpty()) { item(span = full, key = "h_" + cat.name) { Text("${cat.emoji}  ${cat.label}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                    items(list, key = { "t_" + it.id }) { t -> ToolCard(t, t.id in s.favorites, ToolRegistry.isLocked(t.id, premium), Modifier.animateItem(), { launch(t.id) }) { vm.onIntent(DashboardIntent.ToggleFavorite(t.id)) } }
                    // Video ad = the last tile of the section (6th of 6, 4th of 4, 8th of 8 ...), same size as a tool tile.
                    if (showAds && cat in ToolRegistry.AD_CATEGORIES) item(key = "ad_" + cat.name) { NativeAdTile(cat.name, Modifier.animateItem()) } } }
        }
    }
}

@Composable private fun ToolRow(tools: List<Tool>, premium: Boolean, onOpen: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(tools, key = { it.id }) { t ->
        Card(Modifier.width(112.dp).pressable { onOpen(t.id) }, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconBox(t, ToolRegistry.isLocked(t.id, premium)); Text(t.title, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, minLines = 2) } } } } }

@Composable private fun IconBox(t: Tool, locked: Boolean = false) { Box(Modifier.size(44.dp)) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)), Alignment.Center) { Icon(t.icon(), null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
    if (locked) Box(Modifier.align(Alignment.BottomEnd).size(18.dp).background(com.prince.whatstoolbox.ui.theme.Amber, CircleShape), Alignment.Center) { Icon(Icons.Default.Lock, "Pro tool", Modifier.size(11.dp), tint = Color.Black) } } }

@Composable private fun ToolCard(t: Tool, fav: Boolean, locked: Boolean, modifier: Modifier, onOpen: () -> Unit, onFav: () -> Unit) {
    Card(modifier.fillMaxWidth().height(150.dp).pressable(onOpen), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(start = 14.dp, top = 10.dp, end = 4.dp, bottom = 12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { IconBox(t, locked); FavStar(fav, onFav) }
            Column(Modifier.padding(end = 10.dp)) { Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(t.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) } } } }
