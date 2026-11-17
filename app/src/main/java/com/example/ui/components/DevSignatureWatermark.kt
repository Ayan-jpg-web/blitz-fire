package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.security.MessageDigest

data class AppSignatureInfo(
    val appName: String,
    val packageId: String,
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val sha256Full: String,
    val sha256Short: String,
    val sha1Short: String
)

object DevWatermarkPrefs {
    private const val PREFS_NAME = "blitzfire_dev_watermark"
    private const val KEY_ENABLED = "dev_watermark_enabled"
    private const val KEY_LABEL = "dev_watermark_label"
    const val DEFAULT_LABEL = "DEV WATERMARK"

    fun isEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun getLabel(context: Context): String {
        val stored = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LABEL, DEFAULT_LABEL)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        return stored ?: DEFAULT_LABEL
    }

    fun setLabel(context: Context, label: String) {
        val clean = label.trim().take(24).ifEmpty { DEFAULT_LABEL }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LABEL, clean)
            .apply()
    }
}

fun readAppSignatureInfo(context: Context): AppSignatureInfo {
    val certBytes = runCatching {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo
            val signer = if (signingInfo?.hasMultipleSigners() == true) {
                signingInfo.apkContentsSigners?.firstOrNull()
            } else {
                signingInfo?.signingCertificateHistory?.firstOrNull()
                    ?: signingInfo?.apkContentsSigners?.firstOrNull()
            }
            signer?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures?.firstOrNull()?.toByteArray()
        }
    }.getOrNull()

    val sha256Full = if (certBytes != null) {
        digestHexColon(certBytes, "SHA-256")
    } else {
        "DEV-LOCAL-SIGNATURE"
    }
    val sha256Short = if (certBytes != null) {
        digestHexCompact(certBytes, "SHA-256").take(10)
    } else {
        "DEVLOCAL01"
    }
    val sha1Short = if (certBytes != null) {
        digestHexCompact(certBytes, "SHA-1").take(8)
    } else {
        "DEVLOCAL"
    }

    return AppSignatureInfo(
        appName = "BLITZ FIRE",
        packageId = BuildConfig.APPLICATION_ID,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildType = BuildConfig.BUILD_TYPE.uppercase(),
        sha256Full = sha256Full,
        sha256Short = sha256Short,
        sha1Short = sha1Short
    )
}

private fun digestHexColon(bytes: ByteArray, algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm).digest(bytes)
    return digest.joinToString(":") { b -> "%02X".format(b) }
}

private fun digestHexCompact(bytes: ByteArray, algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm).digest(bytes)
    return digest.joinToString("") { b -> "%02X".format(b) }
}

/**
 * Non-interactive, touch-transparent developer watermark stamp tied to the live APK signature.
 * Safe across menus and the 3D Battle Arena.
 */
@Composable
fun DevWatermarkOverlay(
    signatureInfo: AppSignatureInfo,
    devLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .alpha(0.56f)
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBg.copy(alpha = 0.55f))
            .border(1.dp, BlazeGold.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("dev_watermark_overlay"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = BlazeGold,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = "$devLabel • v${signatureInfo.versionName} • SIG ${signatureInfo.sha256Short}",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Compact app signature & dev watermark badge shown on the launch/auth screen footer.
 */
@Composable
fun AuthSignatureFooter(
    signatureInfo: AppSignatureInfo,
    devLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianCard.copy(alpha = 0.65f))
            .border(1.dp, BlazeGold.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("auth_signature_footer")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "App Signature",
                    tint = BlazeGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SIG ${signatureInfo.sha256Short} • ${signatureInfo.buildType} v${signatureInfo.versionName}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
            Surface(
                color = BlazeOrange.copy(alpha = 0.16f),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.border(0.75.dp, BlazeOrange.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
            ) {
                Text(
                    text = devLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlazeGold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Full App Signature Certificate & Dev Watermark card for Operative Dossier (ProfileScreen).
 * Features a stamped diagonal dev watermark directly over the signature credentials.
 */
@Composable
fun AppSignatureCard(
    signatureInfo: AppSignatureInfo,
    devLabel: String,
    watermarkEnabled: Boolean,
    onToggleWatermark: (Boolean) -> Unit,
    onUpdateDevLabel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var draftLabel by remember(devLabel) { mutableStateOf(devLabel) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(ObsidianCard, ObsidianBg.copy(alpha = 0.96f))
                )
            )
            .border(1.dp, BlazeGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .testTag("app_signature_card")
    ) {
        // Diagonal Dev Watermark stamped directly on the App Signature card
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .rotate(-12f)
                    .border(
                        width = 1.5.dp,
                        color = BlazeOrange.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$devLabel • SIG ${signatureInfo.sha256Short}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = BlazeGold.copy(alpha = 0.18f),
                    maxLines = 1
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "App Signature Certificate",
                        tint = BlazeGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "APP SIGNATURE & DEV WATERMARK",
                        style = MaterialTheme.typography.labelLarge,
                        color = BlazeGold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Dev Watermark",
                            tint = DiamondCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText(
                                    "App Signature SHA-256",
                                    "${signatureInfo.packageId} (${signatureInfo.buildType} v${signatureInfo.versionName}) SHA-256: ${signatureInfo.sha256Full}"
                                )
                            )
                            Toast.makeText(context, "App signature copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy App Signature",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PACKAGE ID",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = signatureInfo.packageId,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "BUILD",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "v${signatureInfo.versionName} (${signatureInfo.versionCode}) • ${signatureInfo.buildType}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = DiamondCyan
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianBg.copy(alpha = 0.65f))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "APK SIGNING CERTIFICATE (SHA-256)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalGreen
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = signatureInfo.sha256Full,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextPrimary,
                    lineHeight = 14.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WATERMARK STAMP: $devLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Show translucent dev signature stamp across HUD",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = watermarkEnabled,
                    onCheckedChange = onToggleWatermark,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBg,
                        checkedTrackColor = BlazeOrange,
                        uncheckedTrackColor = ObsidianBorder
                    ),
                    modifier = Modifier.testTag("watermark_toggle_switch")
                )
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Customize Dev Watermark Tag") },
            text = {
                OutlinedTextField(
                    value = draftLabel,
                    onValueChange = { if (it.length <= 24) draftLabel = it },
                    singleLine = true,
                    label = { Text("Watermark Tag (1-24 chars)") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDevLabel(draftLabel)
                        showEditDialog = false
                    }
                ) {
                    Text("APPLY")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
