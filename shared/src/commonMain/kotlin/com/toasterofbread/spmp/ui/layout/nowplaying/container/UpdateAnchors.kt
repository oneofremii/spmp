package com.toasterofbread.spmp.ui.layout.nowplaying.container

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.model.settings.category.PlayerSettings
import kotlinx.coroutines.*

@Composable
internal fun UpdateAnchors(
    swipe_state: AnchoredDraggableState<Int>,
    pages: List<NowPlayingPage>,
    page_height: Dp,
    coroutine_scope: CoroutineScope
) {
    require(pages.isNotEmpty())

    val density: Density = LocalDensity.current
    val minimised_now_playing_height: Dp = MINIMISED_NOW_PLAYING_HEIGHT_DP.dp

    val swipe_sensitivity: Float by PlayerSettings.Key.EXPAND_SWIPE_SENSITIVITY.rememberMutableState()

    LaunchedEffect(page_height, pages.size, minimised_now_playing_height, swipe_sensitivity) {
        val sensitivity: Float = processSwipeSensitivity(swipe_sensitivity)

        val anchors: DraggableAnchors<Int> =
            DraggableAnchors { with (density) {
                val base_position: Dp = minimised_now_playing_height
                0 at base_position.toPx() * sensitivity

                for (page in 1 .. pages.size) {
                    val position: Dp =
                        page_height * page

                    page at position.toPx() * sensitivity
                }
            } }

        val initial_position: Int = swipe_state.currentValue
        swipe_state.updateAnchors(anchors)

        if (initial_position == 0) {
            coroutine_scope.launch {
                swipe_state.snapTo(initial_position)
            }
        }
    }
}

fun Float.npAnchorToPx(density: Density): Float =
    this / processSwipeSensitivity(PlayerSettings.Key.EXPAND_SWIPE_SENSITIVITY.get())
fun Float.npAnchorToDp(density: Density): Dp =
    with (density) { (this@npAnchorToDp / processSwipeSensitivity(PlayerSettings.Key.EXPAND_SWIPE_SENSITIVITY.get())).toDp() }

private fun processSwipeSensitivity(sensitivity: Float): Float =
    1f / sensitivity
