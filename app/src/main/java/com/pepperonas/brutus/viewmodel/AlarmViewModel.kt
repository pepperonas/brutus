package com.pepperonas.brutus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.data.AlarmRepository
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.UltraHardcoreStore
import com.pepperonas.brutus.widget.NextAlarmWidget
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
        soundId: Int,
        mathProblemCount: Int,
        shakeCount: Int,
        hardcoreMode: Boolean,
        ultraHardcoreMode: Boolean,
        mathDifficulty: Int,
        shakeSensitivity: Int,
        sunriseEnabled: Boolean,
    ) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                hour = hour,
                minute = minute,
                label = label,
                repeatDays = repeatDays,
                challengeFlags = ChallengeFlags.sanitize(challengeFlags),
                snoozeDuration = snoozeDuration,
                soundId = soundId,
                mathProblemCount = mathProblemCount,
                shakeCount = shakeCount,
                hardcoreMode = hardcoreMode,
                ultraHardcoreMode = ultraHardcoreMode,
                mathDifficulty = mathDifficulty,
                shakeSensitivity = shakeSensitivity,
                sunriseEnabled = sunriseEnabled,
            )
            val id = repository.insert(alarm)
            val saved = alarm.copy(id = id)
            AlarmScheduler.schedule(getApplication(), saved)
            NextAlarmWidget.refresh(getApplication())
        }
    }

    fun updateAlarm(alarm: AlarmEntity) {
        // Same guard as addAlarm — deselecting every challenge would otherwise
        // store flags=0 ("Keine") while the alarm screen enforces math anyway.
        @Suppress("NAME_SHADOWING")
        val alarm = alarm.copy(challengeFlags = ChallengeFlags.sanitize(alarm.challengeFlags))
        viewModelScope.launch {
            repository.update(alarm)
            if (alarm.enabled) {
                AlarmScheduler.schedule(getApplication(), alarm)
            } else {
                AlarmScheduler.cancel(getApplication(), alarm)
                // If Ultra Hardcore got turned off, also clear any in-flight follow-ups.
                AlarmScheduler.cancelAllFollowups(getApplication(), alarm.id)
                UltraHardcoreStore.clearAllFor(getApplication(), alarm.id)
            }
            NextAlarmWidget.refresh(getApplication())
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
                AlarmScheduler.cancelAllFollowups(getApplication(), updated.id)
                UltraHardcoreStore.clearAllFor(getApplication(), updated.id)
            }
            NextAlarmWidget.refresh(getApplication())
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), alarm)
            AlarmScheduler.cancelAllFollowups(getApplication(), alarm.id)
            UltraHardcoreStore.clearAllFor(getApplication(), alarm.id)
            repository.delete(alarm)
            NextAlarmWidget.refresh(getApplication())
        }
    }

    /**
     * Undo path for deletions: re-inserts the given alarms (fresh IDs) and
     * re-schedules the ones that were enabled.
     */
    fun restoreAlarms(alarms: List<AlarmEntity>) {
        if (alarms.isEmpty()) return
        viewModelScope.launch {
            alarms.forEach { alarm ->
                val id = repository.insert(alarm.copy(id = 0))
                if (alarm.enabled) {
                    AlarmScheduler.schedule(getApplication(), alarm.copy(id = id))
                }
            }
            NextAlarmWidget.refresh(getApplication())
        }
    }
}
