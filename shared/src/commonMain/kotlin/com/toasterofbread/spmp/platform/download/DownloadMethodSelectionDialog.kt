package com.toasterofbread.spmp.platform.download

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.composable.WidthShrinkText
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.StreamingSettings
import com.toasterofbread.spmp.model.settings.getEnum
import com.toasterofbread.spmp.model.settings.rememberMutableEnumState
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong

@Composable
fun DownloadMethodSelectionDialog(onCancelled: () -> Unit, onSelected: (DownloadMethod) -> Unit, modifier: Modifier = Modifier, songs: List<Song>? = null) {
    var download_method: DownloadMethod by StreamingSettings.Key.DOWNLOAD_METHOD.rememberMutableEnumState()
    var skip_confirmation: Boolean by StreamingSettings.Key.SKIP_DOWNLOAD_METHOD_CONFIRMATION.rememberMutableState()
    var show: Boolean by remember { mutableStateOf(false) }

    val initial_download_method: DownloadMethod = remember { StreamingSettings.Key.DOWNLOAD_METHOD.getEnum() }
    val initial_skip_confirmation: Boolean = remember { StreamingSettings.Key.SKIP_DOWNLOAD_METHOD_CONFIRMATION.get() }

    fun cancel() {
        StreamingSettings.Key.DOWNLOAD_METHOD.set(initial_download_method)
        StreamingSettings.Key.SKIP_DOWNLOAD_METHOD_CONFIRMATION.set(initial_skip_confirmation)
        onCancelled()
    }

    LaunchedEffect(Unit) {
        if (skip_confirmation) {
            onSelected(download_method)
        }
        else {
            show = true
        }
    }

    if (!show) {
        return
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            cancel()
        },
        dismissButton = {
            Button({
                cancel()
            }) {
                Text(getString("action_cancel"))
            }
        },
        confirmButton = {
            Button({ onSelected(download_method) }) {
                Text(getString("action_download_start"))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Download, null)
                WidthShrinkText(getString("download_method_select_title"))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                val first_song: Song? = songs?.firstOrNull()
                if (first_song != null) {
                    MediaItemPreviewLong(first_song, Modifier.fillMaxWidth())
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (method in DownloadMethod.available) {
                        Row(
                            Modifier.selectable(download_method == method) {
                                download_method = method
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = download_method == method,
                                onClick = { download_method = method }
                            )

                            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(method.getTitle(), style = MaterialTheme.typography.titleMedium)
                                Text(method.getDescription(), Modifier.alpha(0.75f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Row(
                    Modifier.clickable {
                        skip_confirmation = !skip_confirmation
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = skip_confirmation,
                        onCheckedChange = { checked ->
                            skip_confirmation = checked
                        }
                    )

                    Text(getString("s_key_skip_download_method_confirmation"), Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    )
}
