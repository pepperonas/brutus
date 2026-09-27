package com.pepperonas.brutus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.data.AlarmRepository
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.UltraHardcoreNotifier
import com.pepperonas.brutus.util.UltraHardcoreStore
import com.pepperonas.brutus.widget.NextAlarmWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository

    init {
        val db = (application as BrutusApplication).database
        repository = AlarmRepository(db.alarmDao())
    }

    /**
     * `null` until the database has answered once. An empty list starting value made "not loaded yet"
     * look like "no alarms": after a cold start the list showed *No alarms yet — Create alarm* and the
     * header *No alarm set* for a moment although alarms existed.
     */
    val alarms: StateFlow<List<AlarmEntity>?> = repository.allAlarms.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
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
                // Ultra Hardcore switched off on an enabled alarm: its in-flight follow-ups go too.
                if (!alarm.ultraHardcoreMode) disarmUltraHardcore(alarm.id)
            } else {
                disarm(alarm)
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
                disarm(updated)
            }
            NextAlarmWidget.refresh(getApplication())
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            disarm(alarm)
            repository.delete(alarm)
            NextAlarmWidget.refresh(getApplication())
        }
    }

    /**
     * Everything that can still ring or remind for [alarm]: the regular registration and its
     * sunrise, a pending snooze, the Ultra Hardcore follow-ups and their reminder notification
     * (which otherwise stayed on screen until the next reboot).
     */
    private fun disarm(alarm: AlarmEntity) {
        AlarmScheduler.cancel(getApplication(), alarm)
        AlarmScheduler.cancelSnooze(getApplication(), alarm.id)
        disarmUltraHardcore(alarm.id)
    }

    private fun disarmUltraHardcore(alarmId: Long) {
        AlarmScheduler.cancelAllFollowups(getApplication(), alarmId)
        UltraHardcoreStore.clearAllFor(getApplication(), alarmId)
        UltraHardcoreNotifier.cancel(getApplication(), alarmId)
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
