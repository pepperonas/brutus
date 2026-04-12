package com.pepperonas.brutus.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pepperonas.brutus.TestAlarmActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.ui.theme.BrutusRed
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.GlobalQrStore
import com.pepperonas.brutus.util.QrGenerator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditDialog(
    existingAlarm: AlarmEntity?,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String, Int, Int, Int, Int) -> Unit,
    onPreviewSound: (AlarmSound) -> Unit,
    onStopPreview: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = existingAlarm?.hour ?: 7,
        initialMinute = existingAlarm?.minute ?: 0,
        is24Hour = true
    )
    var label by remember { mutableStateOf(existingAlarm?.label ?: "") }
    var repeatDays by remember { mutableIntStateOf(existingAlarm?.repeatDays ?: 0) }
    var challengeFlags by remember {
        mutableIntStateOf(existingAlarm?.challengeFlags ?: ChallengeFlags.MATH)
    }
    var snoozeDuration by remember { mutableIntStateOf(existingAlarm?.snoozeDuration ?: 5) }
    var soundId by remember { mutableIntStateOf(existingAlarm?.soundId ?: AlarmSound.KLAXON.id) }
    val ctxForQr = LocalContext.current
    val qrCodeData = remember { GlobalQrStore.get(ctxForQr) }
    val qrBitmap = remember(qrCodeData) { QrGenerator.generateBitmap(qrCodeData) }

    val days = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val snoozeOptions = listOf(2, 5, 10, 15)
    val qrEnabled = ChallengeFlags.has(challengeFlags, ChallengeFlags.QR)

    val ctx = LocalContext.current

    val shareQr: () -> Unit = {
        if (!QrGenerator.shareQr(ctx, qrCodeData)) {
            Toast.makeText(ctx, "Teilen fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
    }
    val launchTest: () -> Unit = {
        onStopPreview()
        val flags = if (challengeFlags == 0) ChallengeFlags.MATH else challengeFlags
        val i = Intent(ctx, TestAlarmActivity::class.java).apply {
            putExtra(TestAlarmActivity.EXTRA_CHALLENGE_FLAGS, flags)
            putExtra(TestAlarmActivity.EXTRA_QR_DATA, qrCodeData)
            putExtra(TestAlarmActivity.EXTRA_SOUND_ID, soundId)
        }
        ctx.startActivity(i)
    }
    val saveQr: () -> Unit = {
        val uri = QrGenerator.savePng(ctx, qrCodeData)
        if (uri != null) {
            Toast.makeText(ctx, "QR-Code in Pictures/Brutus gespeichert", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(ctx, "Speichern fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
    }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) saveQr() else
        Toast.makeText(ctx, "Speicher-Berechtigung benötigt", Toast.LENGTH_SHORT).show()
    }
    val onSaveQrClick: () -> Unit = {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveQr()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = if (existingAlarm != null) "Alarm bearbeiten" else "Neuer Alarm",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            TimePicker(
                state = timePickerState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Bezeichnung") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Repeat days
            Text("Wiederholen", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEachIndexed { index, day ->
                    val selected = (repeatDays and (1 shl index)) != 0
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (selected) BrutusRed else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { repeatDays = repeatDays xor (1 shl index) }
                    ) {
                        Text(
                            text = day,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sound picker
            Text("Wecker-Sound", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlarmSound.entries.forEach { snd ->
                    val selected = soundId == snd.id
                    FilterChip(
                        selected = selected,
                        onClick = {
                            soundId = snd.id
                            onPreviewSound(snd)
                        },
                        label = { Text(snd.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrutusRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Text(
                text = AlarmSound.fromId(soundId).description + " — Tippe zum Vorhören",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { onStopPreview() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Vorschau stoppen")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Challenges (multi-select)
            Text("Weckmodi (kombinierbar)", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Alle ausgewählten Challenges müssen nacheinander bestanden werden",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChallengeChip("Mathe", challengeFlags, ChallengeFlags.MATH) {
                    challengeFlags = challengeFlags xor ChallengeFlags.MATH
                }
                ChallengeChip("Schütteln", challengeFlags, ChallengeFlags.SHAKE) {
                    challengeFlags = challengeFlags xor ChallengeFlags.SHAKE
                }
                ChallengeChip("QR-Code", challengeFlags, ChallengeFlags.QR) {
                    challengeFlags = challengeFlags xor ChallengeFlags.QR
                }
            }

            if (qrEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Dein globaler QR-Code — gilt für alle Alarme. Einmal ausdrucken, immer gültig.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ID: ${qrCodeData.takeLast(8)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onSaveQrClick) { Text("Als PNG speichern") }
                    OutlinedButton(onClick = shareQr) { Text("Teilen") }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Snooze-Dauer", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                snoozeOptions.forEachIndexed { index, minutes ->
                    SegmentedButton(
                        selected = snoozeDuration == minutes,
                        onClick = { snoozeDuration = minutes },
                        shape = SegmentedButtonDefaults.itemShape(index, snoozeOptions.size)
                    ) {
                        Text("${minutes}min")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = launchTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Weckmodi jetzt testen")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onStopPreview()
                    onSave(
                        timePickerState.hour,
                        timePickerState.minute,
                        label,
                        repeatDays,
                        challengeFlags,
                        snoozeDuration,
                        soundId,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrutusRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (existingAlarm != null) "Speichern" else "Alarm erstellen",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }

}

@Composable
private fun ChallengeChip(label: String, flags: Int, mask: Int, onToggle: () -> Unit) {
    FilterChip(
        selected = ChallengeFlags.has(flags, mask),
        onClick = onToggle,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrutusRed,
            selectedLabelColor = Color.White
        )
    )
}
