package com.github.ceav17.hots_talent_calculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CustomGroupLabelDialog(
    label: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(label, selection = TextRange(0, label.length)))
    }
    val valid = remember(input.text) { isValidCustomGroupLabel(input.text) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_group_edit), fontFamily = MenuFontFamily) },
        text = {
            OutlinedTextField(
                value = input,
                // Allow intermediate IME composition, but save only one complete visible character.
                onValueChange = { if (it.text.length <= 64) input = it },
                singleLine = true,
                isError = input.text.isNotEmpty() && !valid,
                label = { Text(stringResource(R.string.custom_group_character)) },
                supportingText = { Text(stringResource(R.string.custom_group_character_hint)) },
                textStyle = TextStyle(fontFamily = MenuFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) onSave(input.text) }),
                modifier = Modifier.focusRequester(focusRequester).testTag("custom_group_label_input")
            )
        },
        confirmButton = {
            Text(
                text = stringResource(R.string.custom_group_save),
                fontFamily = MenuFontFamily,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (valid) 1f else 0.38f),
                modifier = Modifier.testTag("custom_group_label_save")
                    .clickable(
                        enabled = valid, role = Role.Button, interactionSource = null, indication = null
                    ) { onSave(input.text) }
                    .padding(16.dp)
            )
        },
        dismissButton = {
            Text(
                text = stringResource(R.string.custom_group_cancel),
                fontFamily = MenuFontFamily,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    role = Role.Button, interactionSource = null, indication = null, onClick = onDismiss
                ).padding(16.dp)
            )
        }
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
