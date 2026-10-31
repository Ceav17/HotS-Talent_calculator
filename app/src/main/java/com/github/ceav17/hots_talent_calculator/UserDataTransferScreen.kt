package com.github.ceav17.hots_talent_calculator

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

@Composable
internal fun UserDataTransferScreen(
    onImported: (UserDataBackup) -> Unit,
    onBack: () -> Unit,
    model: UserDataTransferViewModel = viewModel()
) {
    val state by model.state.collectAsState()
    var picking by rememberSaveable { mutableStateOf(false) }
    val enabled = !state.busy && !picking
    BackHandler(state.importing) { /* Finish the confirmed write before leaving. */ }
    LaunchedEffect(state.imported) {
        state.imported?.let {
            onImported(it)
            model.consumeImport()
        }
    }
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        picking = false
        uri?.let(model::exportFile)
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picking = false
        uri?.let(model::importFile)
    }
    fun launchPicker(action: () -> Unit) {
        picking = true
        try {
            action()
        } catch (_: ActivityNotFoundException) {
            picking = false
            model.showMessage(R.string.data_picker_unavailable, true)
        } catch (_: SecurityException) {
            picking = false
            model.showMessage(R.string.data_picker_unavailable, true)
        }
    }

    SettingsTextPage(
        title = stringResource(R.string.data_title),
        onBack = { if (!state.importing) onBack() }
    ) { textStyle ->
        OutlinedText(stringResource(R.string.data_description), textStyle)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            DataTransferButton(
                stringResource(R.string.data_export_file), "data_export_file", enabled,
                R.drawable.ui_editoricon_toolbar_export
            ) {
                launchPicker { exportFile.launch("HotS-data-${LocalDate.now()}.json") }
            }
            DataTransferButton(
                stringResource(R.string.data_import_file), "data_import_file", enabled,
                R.drawable.storm_ui_glues_replaypanel_icon_folder
            ) {
                launchPicker { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            }
        }
        if (state.busy) CircularProgressIndicator(modifier = Modifier.size(28.dp))
        state.message?.let { message ->
            OutlinedText(stringResource(message), textStyle.copy(color = if (state.error) Color(0xFFFFB4AB) else Color.White),
                modifier = Modifier.testTag("data_transfer_message"))
        }
    }

    state.pending?.let { data ->
        AlertDialog(
            onDismissRequest = model::cancelImport,
            properties = DialogProperties(dismissOnBackPress = !state.busy, dismissOnClickOutside = !state.busy),
            title = { Text(stringResource(R.string.data_confirm_title), fontFamily = MenuFontFamily) },
            text = { Text(stringResource(R.string.data_confirm_message, data.buildCount, data.favoriteIds.size,
                data.customGroups.groups.size, data.customGroups.visibleCount), fontFamily = MenuFontFamily) },
            confirmButton = { DataDialogAction(stringResource(R.string.data_replace), "data_confirm_import", !state.busy, model::confirmImport) },
            dismissButton = { DataDialogAction(stringResource(R.string.custom_group_cancel), "data_cancel_import", !state.busy, model::cancelImport) }
        )
    }
}

@Composable
private fun DataTransferButton(label: String, tag: String, enabled: Boolean, iconResId: Int, onClick: () -> Unit) {
    MainMenuItem(R.drawable.storm_standardbutton_blue_normal, label,
        trailingImageResId = iconResId,
        modifier = Modifier.widthIn(max = 292.dp).fillMaxWidth().aspectRatio(292f / 108f)
            .alpha(if (enabled) 1f else 0.45f).testTag(tag)
            .clickable(enabled = enabled, interactionSource = null, indication = null, role = Role.Button, onClick = onClick))
}

@Composable
private fun DataDialogAction(label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Text(label, fontFamily = MenuFontFamily, color = Color.White.copy(alpha = if (enabled) 1f else 0.45f),
        modifier = Modifier.testTag(tag).clickable(enabled = enabled, interactionSource = null,
            indication = null, role = Role.Button, onClick = onClick).padding(16.dp))
}
