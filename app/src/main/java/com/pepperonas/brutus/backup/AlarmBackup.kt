package com.pepperonas.brutus.backup

import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeFlags
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The export file: alarms, the global QR code (so a printed copy keeps working on a new phone) and
 * the app settings, as JSON. Reading is forgiving about what is missing (an older or newer file
 * still works, missing fields take today's defaults) and strict about what is wrong: values outside
 * what the app offers are clamped, alarms with an impossible time are dropped and counted.
 *
 * The field names are a contract with files people keep — never rename one, only add.
 */
object AlarmBackup {

    const val FORMAT = "brutus-backup"
    const val VERSION = 1
    const val MIME_TYPE = "application/json"

    class FormatException(message: String) : Exception(message)

    data class Settings(
        val crescendoSeconds: Int? = null,
        val ringLimitMinutes: Int? = null,
        val upcomingLeadMinutes: Int? = null,
        val sunriseSoundId: Int? = null,
    )

    data class Contents(
        val alarms: List<AlarmEntity>,
        val qrData: String?,
        val settings: Settings,
        /** Alarms in the file that could not be read (impossible time, not an object). */
        val skipped: Int = 0,
    )

    fun encode(alarms: List<AlarmEntity>, qrData: String?, settings: Settings, exportedAt: Long): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
        if (qrData != null) root.put("qr", qrData)
        root.put(
            "settings",
            JSONObject().apply {
                settings.crescendoSeconds?.let { put("crescendoSeconds", it) }
                settings.ringLimitMinutes?.let { put("ringLimitMinutes", it) }
                settings.upcomingLeadMinutes?.let { put("upcomingLeadMinutes", it) }
                settings.sunriseSoundId?.let { put("sunriseSoundId", it) }
            },
        )
        root.put("alarms", JSONArray().apply { alarms.forEach { put(alarmToJson(it)) } })
        return root.toString(2)
    }

    private fun alarmToJson(a: AlarmEntity) = JSONObject()
        .put("hour", a.hour)
        .put("minute", a.minute)
        .put("enabled", a.enabled)
        .put("label", a.label)
        .put("repeatDays", a.repeatDays)
        .put("challengeFlags", a.challengeFlags)
        .put("snoozeDuration", a.snoozeDuration)
        .put("soundId", a.soundId)
        .put("mathProblemCount", a.mathProblemCount)
        .put("shakeCount", a.shakeCount)
        .put("hardcoreMode", a.hardcoreMode)
        .put("ultraHardcoreMode", a.ultraHardcoreMode)
        .put("mathDifficulty", a.mathDifficulty)
        .put("shakeSensitivity", a.shakeSensitivity)
        .put("sunriseEnabled", a.sunriseEnabled)

    /** @throws FormatException when the text is not a Brutus backup at all. */
    fun decode(text: String): Contents {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw FormatException("not JSON")
        }
        if (root.optString("format") != FORMAT) throw FormatException("not a Brutus backup")
        val version = root.optInt("version", -1)
        if (version < 1) throw FormatException("unknown version $version")

        val array = root.optJSONArray("alarms") ?: JSONArray()
        val alarms = mutableListOf<AlarmEntity>()
        var skipped = 0
        for (i in 0 until array.length()) {
            val alarm = array.optJSONObject(i)?.let(::alarmFromJson)
            if (alarm == null) skipped++ else alarms += alarm
        }
        val s = root.optJSONObject("settings") ?: JSONObject()
        return Contents(
            alarms = alarms,
            qrData = root.optString("qr").takeIf { it.isNotEmpty() },
            settings = Settings(
                crescendoSeconds = s.optIntOrNull("crescendoSeconds"),
                ringLimitMinutes = s.optIntOrNull("ringLimitMinutes"),
                upcomingLeadMinutes = s.optIntOrNull("upcomingLeadMinutes"),
                sunriseSoundId = s.optIntOrNull("sunriseSoundId"),
            ),
            skipped = skipped,
        )
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) (opt(key) as? Number)?.toInt() else null

    private fun alarmFromJson(o: JSONObject): AlarmEntity? {
        val hour = o.optIntOrNull("hour") ?: return null
        val minute = o.optIntOrNull("minute") ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        val d = AlarmEntity(hour = hour, minute = minute) // today's defaults for anything missing
        return AlarmEntity(
            hour = hour,
            minute = minute,
            enabled = o.optBoolean("enabled", d.enabled),
            label = o.optString("label", d.label).take(MAX_LABEL),
            repeatDays = (o.optIntOrNull("repeatDays") ?: d.repeatDays) and 0x7F,
            challengeFlags = ChallengeFlags.sanitize((o.optIntOrNull("challengeFlags") ?: d.challengeFlags) and ALL_CHALLENGES),
            snoozeDuration = (o.optIntOrNull("snoozeDuration") ?: d.snoozeDuration).coerceIn(0, 60),
            // fromId maps retired sounds to their successor and unknown ones to the system tone.
            soundId = AlarmSound.fromId(o.optIntOrNull("soundId") ?: d.soundId).id,
            mathProblemCount = (o.optIntOrNull("mathProblemCount") ?: d.mathProblemCount).coerceIn(1, 10),
            shakeCount = (o.optIntOrNull("shakeCount") ?: d.shakeCount).coerceIn(10, 100),
            hardcoreMode = o.optBoolean("hardcoreMode", d.hardcoreMode),
            ultraHardcoreMode = o.optBoolean("ultraHardcoreMode", d.ultraHardcoreMode),
            mathDifficulty = (o.optIntOrNull("mathDifficulty") ?: d.mathDifficulty).coerceIn(0, 2),
            shakeSensitivity = (o.optIntOrNull("shakeSensitivity") ?: d.shakeSensitivity).coerceIn(0, 2),
            sunriseEnabled = o.optBoolean("sunriseEnabled", d.sunriseEnabled),
        )
    }

    /**
     * The alarms from [incoming] that are not already there. "Already there" = same time, same
     * days and same label — importing the same file twice does not double every alarm.
     */
    fun newAlarms(existing: List<AlarmEntity>, incoming: List<AlarmEntity>): List<AlarmEntity> {
        val seen = existing.map(::identity).toMutableSet()
        return incoming.filter { seen.add(identity(it)) }.map { it.copy(id = 0) }
    }

    private fun identity(a: AlarmEntity) = listOf(a.hour, a.minute, a.repeatDays, a.label.trim())

    /** "brutus-backup-2026-11-08.json" in the phone's time zone. */
    fun suggestedFileName(now: Long): String =
        "brutus-backup-" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date(now)) + ".json"

    private const val MAX_LABEL = 100
    private const val ALL_CHALLENGES = ChallengeFlags.MATH or ChallengeFlags.SHAKE or ChallengeFlags.QR
}
