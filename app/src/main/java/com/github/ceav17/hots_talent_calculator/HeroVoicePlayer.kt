package com.github.ceav17.hots_talent_calculator

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.IOException

/** One short preview at a time; release also works while prepareAsync is pending. */
internal class HeroVoicePlayer(context: Context, private val onError: () -> Unit) {
    private val assets = context.applicationContext.assets
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attributes)
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener { change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN) stop()
        }
        .build()
    private var player: MediaPlayer? = null
    private var hasFocus = false

    var activeId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    fun toggle(line: HeroVoiceLine) {
        if (activeId == line.id) {
            stop()
            return
        }
        stop()
        activeId = line.id
        val current = MediaPlayer()
        player = current
        try {
            current.setAudioAttributes(attributes)
            current.setOnPreparedListener {
                if (player === current) {
                    hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                    if (hasFocus) {
                        try {
                            current.start()
                            isPlaying = true
                        } catch (_: IllegalStateException) {
                            fail(current)
                        }
                    } else {
                        fail(current)
                    }
                }
            }
            current.setOnCompletionListener { if (player === current) stop() }
            current.setOnErrorListener { _, _, _ ->
                fail(current)
                true
            }
            assets.openFd(line.audioPath).use { file ->
                current.setDataSource(file.fileDescriptor, file.startOffset, file.length)
            }
            current.prepareAsync()
        } catch (_: IOException) {
            fail(current)
        } catch (_: IllegalArgumentException) {
            fail(current)
        } catch (_: IllegalStateException) {
            fail(current)
        }
    }

    fun stop() {
        val previous = player
        player = null
        activeId = null
        isPlaying = false
        previous?.release()
        if (hasFocus) {
            hasFocus = false
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
    }

    private fun fail(current: MediaPlayer) {
        // Ignore late callbacks belonging to a preview that was already replaced.
        if (player === current) {
            stop()
            onError()
        }
    }
}
