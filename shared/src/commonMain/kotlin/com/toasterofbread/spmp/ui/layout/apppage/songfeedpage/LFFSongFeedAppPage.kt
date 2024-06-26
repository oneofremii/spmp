package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.platform.composable.*
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.utils.modifier.*
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.*
import com.toasterofbread.spmp.model.mediaitem.db.getPinnedItems
import com.toasterofbread.spmp.model.mediaitem.layout.*
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.settings.category.FeedSettings
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.*
import com.toasterofbread.spmp.ui.component.NotImplementedMessage
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import dev.toastbits.ytmkt.uistrings.UiString

@Composable
internal fun SongFeedAppPage.LFFSongFeedAppPage(
    multiselect_context: MediaItemMultiSelectContext,
    modifier: Modifier,
    content_padding: PaddingValues,
    close: () -> Unit
) {
    val clipboard: ClipboardManager = LocalClipboardManager.current

    if (!feed_endpoint.isImplemented()) {
        feed_endpoint.NotImplementedMessage(modifier.fillMaxSize())
        return
    }

    BackHandler({ selected_filter_chip != null }) {
        selected_filter_chip = null
        loadFeed(false)
    }

    val player: PlayerState = LocalPlayerState.current
    val hidden_rows: Set<String> by FeedSettings.Key.HIDDEN_ROWS.rememberMutableState()
    val hidden_row_titles: List<String> = remember(hidden_rows) {
        hidden_rows.map { row_title ->
            UiString.deserialise(row_title).getString(player.context)
        }
    }

    val square_item_max_text_rows: Int by FeedSettings.Key.SQUARE_PREVIEW_TEXT_LINES.rememberMutableState()
    val show_download_indicators: Boolean by FeedSettings.Key.SHOW_SONG_DOWNLOAD_INDICATORS.rememberMutableState()

    val grid_rows: Int by
        when (player.form_factor) {
            FormFactor.PORTRAIT -> FeedSettings.Key.GRID_ROW_COUNT
            FormFactor.LANDSCAPE -> FeedSettings.Key.LANDSCAPE_GRID_ROW_COUNT
        }.rememberMutableState()
    val grid_rows_expanded: Int by
        when (player.form_factor) {
            FormFactor.PORTRAIT -> FeedSettings.Key.GRID_ROW_COUNT_EXPANDED
            FormFactor.LANDSCAPE -> FeedSettings.Key.LANDSCAPE_GRID_ROW_COUNT_EXPANDED
        }.rememberMutableState()

    Column(modifier) {
        multiselect_context.InfoDisplay(
            getAllItems = {
                (listOf(artists_layout) + layouts.orEmpty()).map {
                    it.items.map { Pair(it, null) }
                } + listOf(player.database.getPinnedItems().map { Pair(it, null) })
            }
        )

        // Main scrolling view
        SwipeRefresh(
            state = load_state == FeedLoadState.LOADING,
            onRefresh = { loadFeed(false) },
            swipe_enabled = load_state == FeedLoadState.NONE,
            indicator = false
        ) {
            val target_state: Any? = if (load_state == FeedLoadState.LOADING || load_state == FeedLoadState.PREINIT) null else layouts ?: false
            var current_state: Any? by remember { mutableStateOf(target_state) }
            val state_alpha: Animatable<Float, AnimationVector1D> = remember { Animatable(1f) }

            Row(modifier, horizontalArrangement = Arrangement.spacedBy(content_padding.calculateStartPadding(LocalLayoutDirection.current))) {
                LaunchedEffect(target_state) {
                    if (current_state == target_state) {
                        state_alpha.animateTo(1f, tween(300))
                        return@LaunchedEffect
                    }

                    if (current_state is List<*> && target_state is List<*>) {
                        state_alpha.snapTo(1f)
                        current_state = target_state
                    }
                    else {
                        state_alpha.animateTo(0f, tween(300))
                        current_state = target_state
                        state_alpha.animateTo(1f, tween(300))
                    }
                }

                var hiding_layout: AppMediaItemLayout? by remember { mutableStateOf(null) }

                hiding_layout?.also { layout ->
                    val title: UiString = layout.title ?: return@also

                    AlertDialog(
                        onDismissRequest = { hiding_layout = null },
                        confirmButton = {
                            Button({
                                FeedSettings.Key.HIDDEN_ROWS.set(
                                    hidden_rows.plus(title.serialise())
                                )

                                hiding_layout = null
                            }) {
                                Text(getString("action_confirm_action"))
                            }
                        },
                        dismissButton = {
                            Button({
                                hiding_layout = null
                            }) {
                                Text(getString("action_deny_action"))
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(getString("prompt_confirm_action"))

                                Spacer(Modifier.fillMaxWidth().weight(1f))

                                IconButton({
                                    clipboard.setText(AnnotatedString(title.getString(player.context)))
                                }) {
                                    Icon(Icons.Default.ContentCopy, null)
                                }
                            }
                        },
                        text = {
                            Text(
                                getString("prompt_hide_feed_rows_with_\$title")
                                    .replace("\$title", title.getString(player.context))
                            )
                        }
                    )
                }

                val state: Any? = current_state

                when (state) {
                    // Loaded
                    is List<*> -> {
                        val onContinuationRequested: (() -> Unit)? =
                            if (continuation != null) {{ loadFeed(true) }}
                            else null
                        val loading_continuation: Boolean = load_state != FeedLoadState.NONE

                        ScrollBarLazyColumn(
                            Modifier.graphicsLayer { alpha = state_alpha.value },
                            state = scroll_state,
                            contentPadding = content_padding.vertical,
                            userScrollEnabled = !state_alpha.isRunning
                        ) {
                            items((state as List<AppMediaItemLayout>)) { layout ->
                                if (layout.items.isEmpty()) {
                                    return@items
                                }

                                val is_hidden: Boolean = remember(layout.title, hidden_row_titles) {
                                    layout.title?.let { layout_title ->
                                        val title: String = layout_title.getString(player.context)
                                        hidden_row_titles.any { it == title }
                                    } ?: false
                                }

                                if (is_hidden) {
                                    return@items
                                }

                                val type: ItemLayoutType = layout.type ?: ItemLayoutType.GRID

                                val rows: Int = if (type == ItemLayoutType.GRID_ALT) grid_rows * 2 else grid_rows
                                val expanded_rows: Int = if (type == ItemLayoutType.GRID_ALT) grid_rows_expanded * 2 else grid_rows_expanded

                                type.Layout(
                                    layout,
                                    MediaItemLayoutParams(
                                        is_song_feed = true,
                                        modifier = Modifier.padding(bottom = 20.dp),
                                        title_modifier = Modifier.platformClickable(
                                            onAltClick = {
                                                if (layout.title != null) {
                                                    hiding_layout = layout
                                                }
                                            }
                                        ),
                                        multiselect_context = player.main_multiselect_context,
                                        apply_filter = true,
                                        show_download_indicators = show_download_indicators,
                                        content_padding = content_padding.horizontal
                                    ),
                                    MediaItemGridParams(
                                        square_item_max_text_rows = square_item_max_text_rows,
                                        rows = Pair(rows, expanded_rows)
                                    )
                                )
                            }

                            item {
                                Crossfade(Pair(onContinuationRequested, loading_continuation), Modifier.padding(content_padding.horizontal)) { data ->
                                    val (requestContinuation, loading) = data

                                    if (loading || requestContinuation != null) {
                                        Box(Modifier.fillMaxWidth().heightIn(min = 60.dp), contentAlignment = Alignment.Center) {
                                            if (loading) {
                                                SubtleLoadingIndicator()
                                            }
                                            else if (requestContinuation != null) {
                                                IconButton({ requestContinuation() }) {
                                                    Icon(Icons.Filled.KeyboardDoubleArrowDown, null, tint = player.theme.on_background)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Loading
                    null -> {
                        Column(Modifier.fillMaxSize()) {
                            SongFeedPageLoadingView(Modifier.graphicsLayer { alpha = state_alpha.value }.fillMaxSize())
                        }
                    }

                    // Load failed
                    else -> {
                        LaunchedEffect(load_error) {
                            if (load_error == null) {
                                return@LaunchedEffect
                            }

                            val library = player.app_page_state.Library
                            library.external_load_error = load_error
                            load_error = null

                            player.openAppPage(library)
                        }
                    }
                }
            }
        }
    }
}
