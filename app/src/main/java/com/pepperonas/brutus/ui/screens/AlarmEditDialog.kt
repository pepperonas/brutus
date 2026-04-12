package com.pepperonas.brutus.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import com.pepperonas.brutus.util.QrGenerator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditDialog(
    existingAlarm: AlarmEntity?,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String, Int, Int, Int, String, Int) -> Unit,
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
    var qrCodeData by remember { mutableStateOf(existingAlarm?.qrCodeData ?: "") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var soundId by remember { mutableIntStateOf(existingAlarm?.soundId ?: AlarmSound.KLAXON.id) }

    val days = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val snoozeOptions = listOf(5, 10, 15)
    val qrEnabled = ChallengeFlags.has(challengeFlags, ChallengeFlags.QR)

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
                text = AlarmSound.fromId(soundId).description + " - Tippe zum Vorhoeren",
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
                text = "Alle ausgewaehlten Challenges muessen nacheinander bestanden werden",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChallengeChip("Mathe", challengeFlags, ChallengeFlags.MATH) {
                    challengeFlags = challengeFlags xor ChallengeFlags.MATH
                }
                ChallengeChip("Schuetteln", challengeFlags, ChallengeFlags.SHAKE) {
                    challengeFlags = challengeFlags xor ChallengeFlags.SHAKE
                }
                ChallengeChip("QR-Code", challengeFlags, ChallengeFlags.QR) {
                    challengeFlags = challengeFlags xor ChallengeFlags.QR
                }
            }

            if (qrEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        qrCodeData = QrGenerator.generateData()
                        qrBitmap = QrGenerator.generateBitmap(qrCodeData)
                    }) {
                        Text("QR-Code generieren")
                    }
                    if (qrCodeData.isNotBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ID: ${qrCodeData.takeLast(8)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                qrBitmap?.let { bmp ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Drucke diesen QR-Code aus und klebe ihn z.B. ins Bad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (qrBitmap == null && qrCodeData.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val existingBmp = remember(qrCodeData) {
                        QrGenerator.generateBitmap(qrCodeData)
                    }
                    Image(
                        bitmap = existingBmp.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .align(Alignment.CenterHorizontally)
                    )
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
                        qrCodeData,
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
