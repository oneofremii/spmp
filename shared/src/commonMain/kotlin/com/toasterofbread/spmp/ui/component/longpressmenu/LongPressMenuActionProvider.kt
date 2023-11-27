package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.playerservice.PlatformPlayerService
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.appHover

class LongPressMenuActionProvider(
    val getContentColour: () -> Color,
    val getAccentColour: () -> Color,
    val getBackgroundColour: () -> Color,
    val onAction: () -> Unit
) {
    @Composable
    fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, onAction: () -> Unit = this.onAction, fill_width: Boolean = true) =
        ActionButton(icon, label, getAccentColour, modifier = modifier, onClick = onClick, onLongClick = onLongClick, onAction = onAction, fill_width = fill_width)

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ActiveQueueIndexAction(
        getText: (distance: Int) -> String,
        onClick: (active_queue_index: Int) -> Unit,
        onLongClick: ((active_queue_index: Int) -> Unit)? = null
    ) {
        val player: PlayerState = LocalPlayerState.current
        val service: PlatformPlayerService = LocalPlayerState.current.controller ?: return
        
        var active_queue_item: Song? by remember { mutableStateOf(null) }
        AnimatedVisibility(service.service_player.active_queue_index < player.status.m_song_count) {
            if (service.service_player.active_queue_index < player.status.m_song_count) {
                val current_song = service.getSong(service.service_player.active_queue_index)
                if (current_song?.id != active_queue_item?.id) {
                    active_queue_item = current_song
                }
            }

            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val distance = service.service_player.active_queue_index - service.current_song_index + 1
                    ActionButton(
                        Icons.Filled.SubdirectoryArrowRight,
                        getText(distance),
                        fill_width = false,
                        onClick = { onClick(service.service_player.active_queue_index) },
                        onLongClick = onLongClick?.let { { it.invoke(service.service_player.active_queue_index) } }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val button_modifier = Modifier
                            .size(30.dp)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .align(Alignment.CenterVertically)

                        Surface(
                            button_modifier.combinedClickable(
                                remember { MutableInteractionSource() },
                                rememberRipple(),
                                onClick = {
                                    service.service_player.updateActiveQueueIndex(-1)
                                },
                                onLongClick = {
                                    player.context.vibrateShort()
                                    service.service_player.updateActiveQueueIndex(Int.MIN_VALUE)
                                }
                            ),
                            color = getAccentColour(),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Remove, null, tint = getBackgroundColour())
                        }

                        Surface(
                            button_modifier.combinedClickable(
                                remember { MutableInteractionSource() },
                                rememberRipple(),
                                onClick = {
                                    service.service_player.updateActiveQueueIndex(1)
                                },
                                onLongClick = {
                                    player.context.vibrateShort()
                                    service.service_player.updateActiveQueueIndex(Int.MAX_VALUE)
                                }
                            ),
                            color = getAccentColour(),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Add, null, tint = getBackgroundColour())
                        }
                    }
                }

                CompositionLocalProvider(
                    LocalPlayerState provides remember { player.copy(onClickedOverride = { item, _ -> player.openMediaItem(item,) }) }
                ) {
                    Crossfade(active_queue_item, animationSpec = tween(100)) { active_item ->
                        if (active_item != null) {
                            MediaItemPreviewLong(active_item, contentColour = getContentColour)
                        }
                    }
                }
            }
        }
    }

    companion object {
        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun ActionButton(
            icon: ImageVector,
            label: String,
            icon_colour: () -> Color = { Color.Unspecified },
            text_colour: () -> Color = { Color.Unspecified },
            modifier: Modifier = Modifier,
            onClick: () -> Unit,
            onLongClick: (() -> Unit)? = null,
            onAction: () -> Unit,
            fill_width: Boolean = true
        ) {
            val player: PlayerState = LocalPlayerState.current

            Row(
                modifier
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onClick()
                            onAction()
                        },
                        onLongClick = if (onLongClick == null) null else {
                            {
                                player.context.vibrateShort()
                                onLongClick()
                                onAction()
                            }
                        }
                    )
                    .appHover(true)
                    .thenIf(fill_width) {
                        fillMaxWidth()
                    },
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon_col = icon_colour()
                Icon(icon, null, tint = if (icon_col.isUnspecified) LocalContentColor.current else icon_col)
                Text(label, fontSize = 15.sp, color = text_colour())
            }
        }
    }
}
