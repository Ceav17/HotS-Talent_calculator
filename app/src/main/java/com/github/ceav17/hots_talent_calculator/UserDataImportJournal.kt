package com.github.ceav17.hots_talent_calculator

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class UserDataImportJournal(private val directory: File) {
    private val active = File(directory, "data-import-rollback.json")
    private val pending = File(directory, "data-import-rollback.pending")

    fun read(): ByteArray? = if (active.exists()) {
        // Stored build JSON is escaped again inside the preferences snapshot.
        active.inputStream().use { readBackupBytes(it, MaxBackupBytes * 2) }
    } else null

    fun prepare(previous: ByteArray) {
        if (active.exists()) throw IOException("A data import still needs recovery")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Could not create recovery directory")
        // A partial first write must not look like a ready journal, including on Android 10.
        FileOutputStream(pending).use {
            it.write(previous)
            it.fd.sync()
        }
        Files.move(pending.toPath(), active.toPath(), StandardCopyOption.ATOMIC_MOVE)
    }

    fun clear() {
        if (active.exists() && !active.delete()) throw IOException("Could not finish data import/recovery")
    }
}
