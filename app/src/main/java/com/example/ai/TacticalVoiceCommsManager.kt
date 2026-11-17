package com.example.ai

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface VoiceCommsState {
    data object Idle : VoiceCommsState
    data object Connecting : VoiceCommsState
    data class Live(val channelInfo: String) : VoiceCommsState
    data class Error(val message: String) : VoiceCommsState
}

@OptIn(PublicPreviewAPI::class)
class TacticalVoiceCommsManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<VoiceCommsState>(VoiceCommsState.Idle)
    val state: StateFlow<VoiceCommsState> = _state.asStateFlow()

    private val _transcriptLog = MutableStateFlow<List<String>>(emptyList())
    val transcriptLog: StateFlow<List<String>> = _transcriptLog.asStateFlow()

    private var activeSession: LiveSession? = null
    private var connectJob: Job? = null

    private val liveModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
            modelName = LIVE_MODEL_NAME,
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                speechConfig = SpeechConfig(voice = Voice("FENRIR"))
            },
            systemInstruction = content {
                text(
                    "You are Ghost-6, an elite BGMI and PUBG tactical squadmate and dropzone spotter in Blitz Fire. " +
                        "Respond concisely in 1-2 crisp tactical radio sentences with positioning, weapon, cover, and zone rotation callouts."
                )
            }
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @SuppressLint("MissingPermission")
    fun startLiveConversation(mapTitle: String, modeTitle: String) {
        if (_state.value is VoiceCommsState.Connecting || _state.value is VoiceCommsState.Live) {
            return
        }
        connectJob?.cancel()
        connectJob = scope.launch {
            _state.value = VoiceCommsState.Connecting
            try {
                val session = withTimeout(30_000L) {
                    liveModel.connect()
                }
                activeSession = session
                session.startAudioConversation()
                session.send(
                    content {
                        text("Squad channel open. We are deploying on $mapTitle in $modeTitle. Give a brief ready check.")
                    }
                )
                _state.value = VoiceCommsState.Live("$mapTitle • LIVE SQUAD RADIO")
                appendLog("RADIO: Connected to Ghost-6 ($mapTitle)")
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Live voice connection timed out", e)
                cleanupSession()
                _state.value = VoiceCommsState.Error("Voice comms connection timed out. Check network and retry.")
            } catch (e: CancellationException) {
                cleanupSession()
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Live API voice conversation", e)
                cleanupSession()
                _state.value = VoiceCommsState.Error(
                    e.localizedMessage ?: "Failed to connect live squad voice channel."
                )
            }
        }
    }

    fun sendTacticalRadioPrompt(promptText: String) {
        val clean = promptText.trim()
        if (clean.isEmpty()) return
        val session = activeSession
        if (session == null) {
            _state.value = VoiceCommsState.Error("Start the Live Voice Radio first before sending a callout.")
            return
        }
        scope.launch {
            try {
                appendLog("YOU: $clean")
                session.send(content { text(clean) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send callout over LiveSession", e)
                _state.value = VoiceCommsState.Error(e.localizedMessage ?: "Failed to transmit callout.")
            }
        }
    }

    fun stopLiveConversation() {
        connectJob?.cancel()
        connectJob = null
        scope.launch {
            cleanupSession()
            _state.value = VoiceCommsState.Idle
            appendLog("RADIO: Channel closed.")
        }
    }

    fun clearError() {
        if (_state.value is VoiceCommsState.Error) {
            _state.value = VoiceCommsState.Idle
        }
    }

    fun release() {
        connectJob?.cancel()
        scope.launch {
            cleanupSession()
            scope.cancel()
        }
    }

    private suspend fun cleanupSession() {
        val session = activeSession
        activeSession = null
        if (session != null) {
            runCatching { session.stopAudioConversation() }
            runCatching { session.close() }
        }
    }

    private fun appendLog(entry: String) {
        _transcriptLog.value = (_transcriptLog.value + entry).takeLast(8)
    }

    companion object {
        private const val TAG = "TacticalVoiceComms"
        const val LIVE_MODEL_NAME = "gemini-3.8-live"
    }
}
