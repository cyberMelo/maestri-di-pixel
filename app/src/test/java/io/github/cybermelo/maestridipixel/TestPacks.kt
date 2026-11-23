package io.github.cybermelo.maestridipixel

import io.github.cybermelo.maestridipixel.core.PackManager
import java.io.File

/**
 * Pacchetti di prova (copie in src/test/resources/fixtures/packs): l'app non contiene domande
 * proprie, quindi i test li mettono nella cache come se fossero appena stati scaricati da GitHub.
 */
object TestPacks {
    val FILES = listOf(
        "storia_egizi.json",
        "scienze_fotosintesi.json",
        "matematica_calcolo_base.json",
        "italiano_grammatica.json",
        "geografia_italia.json",
        "inglese_base.json"
    )

    fun read(fileName: String): String =
        TestPacks::class.java.getResourceAsStream("/fixtures/packs/$fileName")
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Pacchetto di prova mancante: $fileName")

    fun installInto(packManager: PackManager) {
        (FILES + "manifest.json").forEach { File(packManager.cacheDir, it).writeText(read(it), Charsets.UTF_8) }
        packManager.reloadPacks()
    }
}
