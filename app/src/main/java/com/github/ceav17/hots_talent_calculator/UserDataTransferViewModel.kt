package com.github.ceav17.hots_talent_calculator

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class UserDataTransferState(
    val busy: Boolean = false,
    val importing: Boolean = false,
    val pending: UserDataBackup? = null,
    val imported: UserDataBackup? = null,
    @param:StringRes val message: Int? = null,
    val error: Boolean = false
)

internal class UserDataTransferViewModel(application: Application) : AndroidViewModel(application) {
    private val store = UserDataStore(application)
    private val resolver = application.contentResolver
    private val mutableState = MutableStateFlow(UserDataTransferState())
    val state = mutableState.asStateFlow()

    private fun work(@StringRes failure: Int, operation: suspend () -> Unit) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, message = null, error = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { operation() }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showMessage(when (error) {
                    is UnsupportedBackupVersionException -> R.string.data_newer_format
                    is InvalidBackupException -> R.string.data_invalid_backup
                    is BackupTooLargeException -> R.string.data_file_too_large
                    else -> failure
                }, true)
            } finally {
                mutableState.update { it.copy(busy = false, importing = false) }
            }
        }
    }

    fun exportFile(uri: Uri) = work(R.string.data_export_failed) {
        val bytes = encodeUserDataBackup(store.export())
        (resolver.openOutputStream(uri, "wt") ?: throw IOException("Could not open output")).use {
            it.write(bytes)
            it.flush()
        }
        showMessage(R.string.data_exported)
    }

    fun importFile(uri: Uri) = work(R.string.data_import_failed) {
        val data = (resolver.openInputStream(uri) ?: throw IOException("Could not open backup")).use {
            decodeUserDataBackup(readBackupBytes(it))
        }
        prepareImport(data)
    }

    private fun prepareImport(data: UserDataBackup) {
        store.validate(data)
        mutableState.update { it.copy(pending = data) }
    }

    fun confirmImport() = work(R.string.data_import_failed) {
        val data = mutableState.value.pending ?: return@work
        mutableState.update { it.copy(importing = true) }
        store.restore(data)
        mutableState.update { it.copy(pending = null, imported = data) }
        showMessage(R.string.data_imported)
    }

    fun cancelImport() { if (!mutableState.value.busy) mutableState.update { it.copy(pending = null) } }
    fun consumeImport() { mutableState.update { it.copy(imported = null) } }
    fun showMessage(@StringRes message: Int, error: Boolean = false) {
        mutableState.update { it.copy(message = message, error = error) }
    }
}
