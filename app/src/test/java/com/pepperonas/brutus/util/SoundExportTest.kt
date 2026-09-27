package com.pepperonas.brutus.util

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes every shipped sound as a WAV file for listening on a computer, plus an overview page — how
 * the v2.5.0 sounds were chosen. Skipped unless `BRUTUS_SOUND_EXPORT=<folder>` is set, so CI never
 * writes anything:
 *
 *     BRUTUS_SOUND_EXPORT=~/Desktop/Brutus-Sounds ./gradlew :app:testDebugUnitTest --tests '*SoundExportTest'
 *
 * The files come straight from [AlarmSoundGenerator.generatePcm] — the exact buffers the app plays.
 * Each holds the buffer looped the way it rings: 15 s for harsh, 30 s for gentle.
 */
class SoundExportTest {

    private data class Entry(
        val code: String, val folder: String, val name: String, val description: String,
        val harsh: Boolean, val pcm: ShortArray,
    )

    @Test
    fun `export all sounds as wav`() {
        val target = System.getenv("BRUTUS_SOUND_EXPORT")?.replaceFirst("~", System.getProperty("user.home"))
        assumeTrue("set BRUTUS_SOUND_EXPORT to export", !target.isNullOrBlank())
        val root = File(target!!)

        val strings = germanSoundStrings()
        val entries = AlarmSound.entries
            .filter { it != AlarmSound.SILENT && it != AlarmSound.SYSTEM }
            .let { list -> list.filter { !it.gentle } + list.filter { it.gentle } }
            .mapIndexed { i, s ->
                val key = "sound_" + s.name.lowercase()
                Entry(
                    "%02d".format(i + 1), if (s.gentle) "sanft" else "krass",
                    strings[key] ?: s.name, strings["${key}_description"] ?: "",
                    !s.gentle, AlarmSoundGenerator.generatePcm(s),
                )
            }

        entries.forEach { e ->
            val dir = File(root, e.folder).apply { mkdirs() }
            File(dir, fileName(e)).writeBytes(wav(loop(e.pcm, if (e.harsh) 15.0 else 30.0)))
        }
        File(root, "ÜBERSICHT.html").writeText(html(entries))
        File(root, "ÜBERSICHT.txt").writeText(text(entries))
    }

    /**
     * Short previews for the product page: `BRUTUS_SOUND_PREVIEWS=<folder>` writes `<slug>.wav` per
     * sound — 6 s harsh, 12 s gentle (two loops, so the seam can be heard), a 0.3 s fade-out, and 6 dB
     * quieter than on the phone for everything alike: the relation between sounds stays, a click on a
     * siren does not blast out of a laptop at full scale. `scripts/sound-previews.sh` turns them into
     * the page's m4a files.
     */
    @Test
    fun `export previews for the product page`() {
        val target = System.getenv("BRUTUS_SOUND_PREVIEWS")?.replaceFirst("~", System.getProperty("user.home"))
        assumeTrue("set BRUTUS_SOUND_PREVIEWS to export", !target.isNullOrBlank())
        val dir = File(target!!).apply { mkdirs() }
        val sr = AlarmSoundGenerator.SAMPLE_RATE
        AlarmSound.entries.filter { it != AlarmSound.SILENT && it != AlarmSound.SYSTEM }.forEach { s ->
            val clip = loop(AlarmSoundGenerator.generatePcm(s), if (s.gentle) 12.0 else 6.0)
                .copyOf(((if (s.gentle) 12.0 else 6.0) * sr).toInt())
            val fade = (0.3 * sr).toInt()
            val out = ShortArray(clip.size) { i ->
                val g = 0.5 * minOf(1.0, (clip.size - 1 - i).toDouble() / fade)
                (clip[i] * g).toInt().toShort()
            }
            File(dir, previewSlug(s) + ".wav").writeBytes(wav(out))
        }
    }

    private fun fileName(e: Entry) = "${e.code} ${e.name.replace('/', '-')}.wav"

    /** The file name the product page uses: FIRE_ALARM → fire-alarm. */
    private fun previewSlug(s: AlarmSound) = s.name.lowercase().replace('_', '-')

    private fun seconds(pcm: ShortArray) = pcm.size.toDouble() / AlarmSoundGenerator.SAMPLE_RATE

    /** Repeats the buffer to at least [seconds] — how it sounds while it rings. */
    private fun loop(pcm: ShortArray, seconds: Double): ShortArray {
        val want = (seconds * AlarmSoundGenerator.SAMPLE_RATE).toInt()
        val times = maxOf(1, (want + pcm.size - 1) / pcm.size)
        return ShortArray(pcm.size * times) { pcm[it % pcm.size] }
    }

    private fun wav(pcm: ShortArray): ByteArray {
        val sr = AlarmSoundGenerator.SAMPLE_RATE
        val data = pcm.size * 2
        return ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(sr); putInt(sr * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data)
            pcm.forEach { putShort(it) }
        }.array()
    }

    /** Names and descriptions of the shipped sounds as the German UI shows them. */
    private fun germanSoundStrings(): Map<String, String> {
        val xml = File("src/main/res/values-de/strings.xml").readText()
        return Regex("""<string name="(sound_[a-z_]+)">([^<]*)</string>""").findAll(xml)
            .associate { it.groupValues[1] to it.groupValues[2].replace("\\'", "'") }
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun text(entries: List<Entry>) = buildString {
        appendLine("Brutus — alle Wecktöne zum Probehören")
        appendLine("So klingen sie in der App — jeweils im Loop.")
        appendLine()
        listOf("krass" to "Krass — zum Wecken", "sanft" to "Sanft — für Timer und Sunrise")
            .forEach { (folder, title) ->
                appendLine("== $title")
                entries.filter { it.folder == folder }.forEach { e ->
                    appendLine("  ${e.code}  ${e.name.padEnd(20)} ${"%.1f".format(seconds(e.pcm))} s Loop  — ${e.description}")
                }
                appendLine()
            }
        appendLine("Nicht als Datei: „System-Alarm“ (der Weckton des Handys) und „Stumm“.")
    }

    private fun html(entries: List<Entry>) = buildString {
        append(
            """
            <!doctype html><html lang="de"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Brutus-Wecktöne</title>
            <style>
            :root{--bg:#141110;--card:#211e1d;--line:#534341;--text:#ede0dd;--mut:#d8c2bf;--pri:#ffb4ab;--ok:#9ad0a0}
            *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:15px/1.5 system-ui,-apple-system,sans-serif}
            main{max-width:960px;margin:0 auto;padding:32px 20px 64px}
            h1{margin:0 0 4px;font-size:1.7rem}p.lead{margin:0 0 28px;color:var(--mut)}
            h2{margin:32px 0 12px;font-size:1.05rem;letter-spacing:.06em;text-transform:uppercase;color:var(--pri)}
            .grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:12px}
            .card{background:var(--card);border:1px solid var(--line);border-radius:20px;padding:14px 16px;display:flex;flex-direction:column;gap:8px}
            .top{display:flex;align-items:baseline;gap:10px}
            .code{font:700 .85rem ui-monospace,monospace;color:var(--pri);flex:none}
            .name{font-weight:700}.meta{margin-left:auto;font:.78rem ui-monospace,monospace;color:var(--mut);flex:none}
            .desc{color:var(--mut);font-size:.9rem}audio{width:100%}
            label{display:flex;gap:8px;align-items:center;font-size:.88rem;color:var(--mut);cursor:pointer}
            input{accent-color:var(--pri);width:18px;height:18px}
            #pick{position:sticky;bottom:12px;margin-top:28px;background:var(--card);border:1px solid var(--line);border-radius:20px;padding:12px 16px;display:flex;gap:12px;align-items:center}
            #pick code{font:600 .95rem ui-monospace,monospace;color:var(--ok);flex:1;overflow-wrap:anywhere}
            button{font:inherit;background:var(--pri);color:#690005;border:0;border-radius:99px;padding:8px 16px;font-weight:700;cursor:pointer}
            </style></head><body><main>
            <h1>Brutus-Wecktöne</h1>
            <p class="lead">Alle Klänge so, wie die App sie im Loop spielt. Beim Start eines Klangs stoppt der vorherige.</p>
            """.trimIndent()
        )
        listOf("krass" to "Krass — zum Wecken", "sanft" to "Sanft — für Timer und Sunrise")
            .forEach { (folder, title) ->
                append("<h2>${esc(title)}</h2><div class=\"grid\">")
                entries.filter { it.folder == folder }.forEach { e ->
                    val src = "$folder/" + fileName(e).split("/").joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                    append(
                        "<div class=\"card\"><div class=\"top\"><span class=\"code\">${e.code}</span>" +
                            "<span class=\"name\">${esc(e.name)}</span><span class=\"meta\">${"%.1f".format(seconds(e.pcm))} s Loop</span></div>" +
                            "<div class=\"desc\">${esc(e.description)}</div>" +
                            "<audio controls loop preload=\"none\" src=\"$src\"></audio>" +
                            "<label><input type=\"checkbox\" value=\"${e.code}\"> auswählen</label></div>"
                    )
                }
                append("</div>")
            }
        append(
            """
            <div id="pick"><code id="list">Noch nichts gewählt</code><button id="copy" type="button">Kopieren</button></div>
            <p class="lead" style="margin-top:20px">Nicht als Datei: „System-Alarm“ (der Weckton des Handys) und „Stumm“.</p>
            </main><script>
            const boxes=[...document.querySelectorAll('input[type=checkbox]')];
            const list=document.getElementById('list');
            function sync(){const v=boxes.filter(b=>b.checked).map(b=>b.value);list.textContent=v.length?v.join(', '):'Noch nichts gewählt';try{localStorage.setItem('brutus-pick',JSON.stringify(v))}catch(e){}}
            try{const s=JSON.parse(localStorage.getItem('brutus-pick')||'[]');boxes.forEach(b=>b.checked=s.includes(b.value))}catch(e){}
            boxes.forEach(b=>b.addEventListener('change',sync));sync();
            document.getElementById('copy').onclick=()=>navigator.clipboard&&navigator.clipboard.writeText(list.textContent);
            document.querySelectorAll('audio').forEach(a=>a.addEventListener('play',()=>document.querySelectorAll('audio').forEach(o=>{if(o!==a)o.pause()})));
            </script></body></html>
            """.trimIndent()
        )
    }
}
