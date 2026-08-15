package com.pepperonas.brutus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the migration discipline that `exportSchema = true` exists for.
 *
 * Adding a field to [AlarmEntity] without bumping the database version and
 * writing a migration compiles fine and passes every in-memory test — it only
 * blows up on a user's device, at 6 a.m., with `IllegalStateException: Room
 * cannot verify the data integrity`. Comparing the entity against the newest
 * exported schema catches that in CI instead.
 */
@RunWith(RobolectricTestRunner::class)
class RoomSchemaExportTest {

    private val schemaDir = listOf(
        File("schemas/com.pepperonas.brutus.data.AlarmDatabase"),
        File("app/schemas/com.pepperonas.brutus.data.AlarmDatabase"),
    ).firstOrNull { it.isDirectory }

    private fun schema(version: Int): JSONObject {
        val dir = requireNotNull(schemaDir) {
            "exported schemas not found — is exportSchema still enabled? (cwd=${File("").absolutePath})"
        }
        val file = File(dir, "$version.json")
        assertTrue(file.isFile, "missing exported schema $version.json — run a build after bumping the version")
        return JSONObject(file.readText()).getJSONObject("database")
    }

    private fun columnsOf(version: Int): List<String> {
        val fields = schema(version).getJSONArray("entities").getJSONObject(0).getJSONArray("fields")
        return (0 until fields.length()).map { fields.getJSONObject(it).getString("columnName") }
    }

    /** The declaration order of the Kotlin data class, via Java reflection. */
    private fun entityProperties(): List<String> =
        AlarmEntity::class.java.declaredFields
            .filterNot { it.isSynthetic || it.name == "Companion" || it.name.startsWith("\$") }
            .map { it.name }

    /** Opens a real Room database and reports what the generated code actually creates. */
    private fun <T> withDatabase(block: (AlarmDatabase) -> T): T {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AlarmDatabase::class.java
        ).allowMainThreadQueries().build()
        return try {
            db.openHelper.writableDatabase          // force creation
            block(db)
        } finally {
            db.close()
        }
    }

    @Test
    fun `the database the app opens carries the exported version number`() {
        val runtimeVersion = withDatabase { it.openHelper.writableDatabase.version }

        assertEquals(7, runtimeVersion, "database version changed — export the schema and add a migration")
        assertEquals(runtimeVersion, schema(runtimeVersion).getInt("version"))
    }

    @Test
    fun `the runtime identity hash matches the exported schema`() {
        // Room stores the hash of the schema it generated; the exported JSON
        // carries the hash of the schema that was committed. A field added
        // without re-exporting makes these two diverge — which is exactly the
        // situation that throws "Room cannot verify the data integrity" on a
        // user's device after an update.
        val runtimeHash = withDatabase { db ->
            db.openHelper.writableDatabase
                .query("SELECT identity_hash FROM room_master_table LIMIT 1")
                .use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }

        assertEquals(
            schema(7).getString("identityHash"),
            runtimeHash,
            "AlarmEntity and the committed schema 7.json have drifted apart"
        )
    }

    @Test
    fun `every entity property is a column in the exported schema`() {
        val columns = columnsOf(7).toSet()
        val missing = entityProperties().filterNot { it in columns }

        assertTrue(
            missing.isEmpty(),
            "AlarmEntity has field(s) $missing with no column in schema 7.json — " +
                "bump the database version, add a Migration and re-export"
        )
    }

    @Test
    fun `the exported schema has no column the entity dropped`() {
        val properties = entityProperties().toSet()
        val orphaned = columnsOf(7).filterNot { it in properties }

        assertTrue(orphaned.isEmpty(), "schema 7.json still lists removed column(s) $orphaned")
    }

    @Test
    fun `the table name and auto-generated key stay stable`() {
        val entity = schema(7).getJSONArray("entities").getJSONObject(0)

        assertEquals("alarms", entity.getString("tableName"))
        val pk = entity.getJSONObject("primaryKey")
        assertTrue(pk.getBoolean("autoGenerate"))
        assertEquals("id", pk.getJSONArray("columnNames").getString(0))
    }

    @Test
    fun `each shipped schema version is exported so migrations have a baseline`() {
        // 4 → 5 → 6 → 7 are the migrations wired into AlarmDatabase.
        listOf(5, 6, 7).forEach { version ->
            assertEquals(version, schema(version).getInt("version"))
        }
    }

    @Test
    fun `the columns added by the later migrations are present`() {
        // v5 added hardcoreMode, v6 the Ultra Hardcore + preset columns, v7 sunrise.
        assertTrue("hardcoreMode" in columnsOf(5))
        assertTrue(
            listOf("ultraHardcoreMode", "mathDifficulty", "shakeSensitivity")
                .all { it in columnsOf(6) }
        )
        assertTrue("sunriseEnabled" in columnsOf(7))
        // …and each one arrived exactly when the migration says it did.
        assertTrue("ultraHardcoreMode" !in columnsOf(5))
        assertTrue("sunriseEnabled" !in columnsOf(6))
    }
}
