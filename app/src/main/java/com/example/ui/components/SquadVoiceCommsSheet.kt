package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.TacticalVoiceCommsManager
import com.example.ai.VoiceCommsState
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SquadVoicePill(
    commsManager: TacticalVoiceCommsManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceState by commsManager.state.collectAsStateWithLifecycle()
    val isLive = voiceState is VoiceCommsState.Live
    val isConnecting = voiceState is VoiceCommsState.Connecting
    val accent = when {
        isLive -> TacticalGreen
        isConnecting -> BlazeGold
        voiceState is VoiceCommsState.Error -> DangerRed
        else -> DiamondCyan
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianCard)
            .border(1.2.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("lobby_voice_comms_pill"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLive) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = "Squad Tactical Voice Radio",
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (isLive) "SQUAD VOICE LIVE" else "SQUAD TACTICAL RADIO",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (voiceState) {
                        is VoiceCommsState.Live -> "Ghost-6 Squadmate • Live Audio"
                        is VoiceCommsState.Connecting -> "Connecting voice channel..."
                        is VoiceCommsState.Error -> "Audio alert • Tap to retry"
                        is VoiceCommsState.Idle -> "Live Voice Comms • Tap to open"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Surface(
            color = accent.copy(alpha = 0.2f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = if (isLive) "ON AIR" else "MIC",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun SquadVoiceDialog(
    commsManager: TacticalVoiceCommsManager,
    mapTitle: String,
    modeTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val voiceState by commsManager.state.collectAsStateWithLifecycle()
    val transcriptLog by commsManager.transcriptLog.collectAsStateWithLifecycle()

    var permissionDeniedNote by remember { mutableStateOf<String?>(null) }
    var draftPrompt by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = null
            commsManager.startLiveConversation(mapTitle = mapTitle, modeTitle = modeTitle)
        } else {
            permissionDeniedNote = "Microphone permission is required for real-time Squad Voice Comms."
        }
    }

    fun requestAndStartVoice() {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            permissionDeniedNote = null
            commsManager.startLiveConversation(mapTitle = mapTitle, modeTitle = modeTitle)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val isLive = voiceState is VoiceCommsState.Live
    val isConnecting = voiceState is VoiceCommsState.Connecting
    val errorText = (voiceState as? VoiceCommsState.Error)?.message ?: permissionDeniedNote

    val quickCallouts = listOf(
        "Drop callout for $mapTitle?",
        "Enemy pushing! Cover advice?",
        "Next safe zone rotation tip?",
        "Best rifle setup right now?"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = 390.dp)
                .border(1.5.dp, if (isLive) TacticalGreen else BlazeOrange, RoundedCornerShape(18.dp))
                .testTag("squad_voice_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background((if (isLive) TacticalGreen else BlazeOrange).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLive) Icons.Default.GraphicEq else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isLive) TacticalGreen else BlazeOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SQUAD TACTICAL VOICE COMMS",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "$mapTitle • $modeTitle • Channel: Ghost-6",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLive) TacticalGreen else BlazeGold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Radio", tint = TextSecondary)
                    }
                }

                if (errorText != null) {
                    Surface(
                        color = DangerRed.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DangerRed.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isLive) {
                        Button(
                            onClick = { commsManager.stopLiveConversation() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerRed,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("voice_stop_button")
                        ) {
                            Icon(Icons.Default.MicOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("END VOICE CHANNEL", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Button(
                            onClick = {
                                commsManager.clearError()
                                requestAndStartVoice()
                            },
                            enabled = !isConnecting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlazeOrange,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("voice_start_button")
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ObsidianBg,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CONNECTING AUDIO...", style = MaterialTheme.typography.labelLarge)
                            } else {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("START LIVE SQUAD VOICE", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("MINIMIZE", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                }

                Text(
                    text = "QUICK RADIO CALLOUTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickCallouts) { callout ->
                        Surface(
                            color = ObsidianCard,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                                .clickable(enabled = isLive) {
                                    commsManager.sendTacticalRadioPrompt(callout)
                                }
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(
                                text = callout,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLive) DiamondCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftPrompt,
                        onValueChange = { draftPrompt = it },
                        placeholder = { Text("Send tactical callout over radio...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DiamondCyan,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedContainerColor = ObsidianCard,
                            unfocusedContainerColor = ObsidianCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            commsManager.sendTacticalRadioPrompt(draftPrompt)
                            draftPrompt = ""
                        },
                        enabled = isLive && draftPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DiamondCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Callout")
                    }
                }

                if (transcriptLog.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianElevated)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        transcriptLog.forEach { entry ->
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
