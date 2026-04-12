package com.pepperonas.brutus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.data.AlarmRepository
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.QrGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository

    init {
        val db = (application as BrutusApplication).database
        repository = AlarmRepository(db.alarmDao())
    }

    val alarms = repository.allAlarms.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun addAlarm(
        hour: Int,
        minute: Int,
        label: String,
        repeatDays: Int,
        challengeFlags: Int,
        snoozeDuration: Int,
        qrCodeData: String,
        soundId: Int,
    ) {
        viewModelScope.launch {
            val finalQr = if (ChallengeFlags.has(challengeFlags, ChallengeFlags.QR) && qrCodeData.isBlank()) {
                QrGenerator.generateData()
            } else qrCodeData

            val alarm = AlarmEntity(
                hour = hour,
                minute = minute,
                label = label,
                repeatDays = repeatDays,
                challengeFlags = if (challengeFlags == 0) ChallengeFlags.MATH else challengeFlags,
                snoozeDuration = snoozeDuration,
                qrCodeData = finalQr,
                soundId = soundId,
            )
            val id = repository.insert(alarm)
            val saved = alarm.copy(id = id)
            AlarmScheduler.schedule(getApplication(), saved)
        }
    }

    fun updateAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.update(alarm)
            if (alarm.enabled) {
                AlarmScheduler.schedule(getApplication(), alarm)
            } else {
                AlarmScheduler.cancel(getApplication(), alarm)
            }
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        val updated = alarm.copy(enabled = !alarm.enabled)
        viewModelScope.launch {
            repository.update(updated)
            if (updated.enabled) {
                AlarmScheduler.schedule(getApplication(), updated)
            } else {
                AlarmScheduler.cancel(getApplication(), updated)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), alarm)
            repository.delete(alarm)
        }
    }
}
