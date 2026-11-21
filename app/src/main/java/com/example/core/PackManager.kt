package com.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

fun interface BundledAssetReader {
    fun readAssetText(assetPath: String): String?
}

class PackManager(
    private val rootDir: File,
    private val saveManager: SaveManager,
    private val assetReader: BundledAssetReader,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    val cacheDir: File = File(rootDir, "packs_cache")
    val stagingDir: File = File(rootDir, "packs_staging")
    val localDir: File = File(rootDir, "packs_local")

    private val syncLock = AtomicBoolean(false)
    val isSyncing: Boolean get() = syncLock.get()

    private val allPacksMap = linkedMapOf<String, PackData>()

    init {
        cacheDir.mkdirs()
        stagingDir.mkdirs()
        localDir.mkdirs()
    }

    fun getAllPacks(includeArchived: Boolean = false): List<PackData> {
        val overrides = saveManager.config.priorityOverrides
        return allPacksMap.values
            .filter { includeArchived || !it.archived }
            .map { pack ->
                val effectivePriority = overrides[pack.packId] ?: pack.priority
                pack.copy(priority = effectivePriority)
            }
    }

    fun getActivePacks(): List<PackData> = getAllPacks(includeArchived = false)

    fun getPack(packId: String): PackData? {
        val pack = allPacksMap[packId] ?: return null
        val overrides = saveManager.config.priorityOverrides
        return pack.copy(priority = overrides[packId] ?: pack.priority)
    }

    fun reloadPacks() {
        val loaded = linkedMapOf<String, PackData>()

        // 1. Bundled packs from assets/packs/manifest.json (NEVER directory scan APK assets)
        val bundledManifestRaw = assetReader.readAssetText("packs/manifest.json")
        if (bundledManifestRaw != null) {
            try {
                val mObj = JSONObject(bundledManifestRaw)
                if (PackValidator.validateManifest(mObj).isValid) {
                    val entries = PackValidator.parseManifestEntries(mObj)
                    for (entry in entries) {
                        val rawPack = assetReader.readAssetText("packs/${entry.file}") ?: continue
                        val pObj = JSONObject(rawPack)
                        if (PackValidator.validatePack(pObj, entry.file).isValid) {
                            val pack = PackValidator.parsePack(
                                obj = pObj,
                                source = "bundled",
                                priority = entry.priority,
                                archived = entry.archived
                            )
                            loaded[pack.packId] = pack
                        }
                    }
                }
            } catch (_: Exception) {
                // Bundled manifest fallback safe
            }
        }

        // 2. GitHub cached packs in packs_cache/
        val cacheManifestFile = File(cacheDir, "manifest.json")
        if (cacheManifestFile.exists()) {
            try {
                val mObj = JSONObject(cacheManifestFile.readText(Charsets.UTF_8))
                if (PackValidator.validateManifest(mObj).isValid) {
                    val entries = PackValidator.parseManifestEntries(mObj)
                    for (entry in entries) {
                        if (entry.archived) {
                            val existing = loaded[entry.packId]
                            if (existing != null) {
                                loaded[entry.packId] = existing.copy(archived = true)
                            } else {
                                // Still try reading cached pack for medal metadata, marked archived
                                val cachedFile = resolveSafeChild(cacheDir, entry.file)
                                if (cachedFile != null && cachedFile.exists()) {
                                    val pObj = JSONObject(cachedFile.readText(Charsets.UTF_8))
                                    if (PackValidator.validatePack(pObj, entry.file).isValid) {
                                        loaded[entry.packId] = PackValidator.parsePack(
                                            obj = pObj,
                                            source = "github",
                                            priority = entry.priority,
                                            archived = true
                                        )
                                    }
                                }
                            }
                            continue
                        }
                        val cachedFile = resolveSafeChild(cacheDir, entry.file) ?: continue
                        if (!cachedFile.exists()) continue
                        val pObj = JSONObject(cachedFile.readText(Charsets.UTF_8))
                        if (PackValidator.validatePack(pObj, entry.file).isValid) {
                            val pack = PackValidator.parsePack(
                                obj = pObj,
                                source = "github",
                                priority = entry.priority,
                                archived = false
                            )
                            loaded[pack.packId] = pack
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep bundled packs intact if cache manifest is corrupted
            }
        }

        // 3. Locally imported packs in packs_local/*.json (override and force archived = false)
        val localFiles = localDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.sortedBy { it.name } ?: emptyList()
        for (file in localFiles) {
            try {
                val pObj = JSONObject(file.readText(Charsets.UTF_8))
                if (PackValidator.validatePack(pObj, file.name).isValid) {
                    val pack = PackValidator.parsePack(
                        obj = pObj,
                        source = "local",
                        priority = loaded[pObj.getString("pack_id")]?.priority ?: false,
                        archived = false
                    )
                    loaded[pack.packId] = pack
                }
            } catch (_: Exception) {
                // Skip invalid local file
            }
        }

        allPacksMap.clear()
        allPacksMap.putAll(loaded)
    }

    fun importPackFromJsonText(rawJson: String, sourceLabel: String = "clipboard.json"): ValidationResult {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult().addError("Testo JSON vuoto.")
        }
        val obj = try {
            JSONObject(trimmed)
        } catch (e: Exception) {
            return ValidationResult().addError("JSON non valido: sintassi errata.")
        }

        val validation = PackValidator.validatePack(obj, sourceLabel)
        if (!validation.isValid) {
            return validation
        }

        val packId = obj.getString("pack_id")
        val targetFile = File(localDir, "$packId.json")
        val ok = saveManager.writeJsonAtomic(targetFile, obj)
        if (!ok) {
            return validation.addError("Impossibile salvare il pacchetto in memoria locale.")
        }
        reloadPacks()
        return validation
    }

    fun importPackFromFile(file: File): ValidationResult {
        if (!file.exists() || !file.isFile) {
            return ValidationResult().addError("File non trovato.")
        }
        return try {
            val content = file.readText(Charsets.UTF_8)
            importPackFromJsonText(content, file.name)
        } catch (_: Exception) {
            ValidationResult().addError("Impossibile leggere il file selezionato.")
        }
    }

    fun deleteLocalPack(packId: String): Boolean {
        val file = File(localDir, "$packId.json")
        val bak = File(localDir, "$packId.json.bak")
        val deleted = if (file.exists()) file.delete() else false
        if (bak.exists()) bak.delete()
        if (deleted) {
            reloadPacks()
        }
        return deleted
    }

    suspend fun testGithubConnection(): SyncResult {
        if (!syncLock.compareAndSet(false, true)) {
            return SyncResult("busy", "Sincronizzazione già in corso...")
        }
        return try {
            val gh = saveManager.config.github
            val owner = gh.owner.trim()
            val repo = gh.repo.trim()
            val branch = gh.branch.trim()
            val pat = gh.pat.trim()
            if (owner.isEmpty() || repo.isEmpty() || branch.isEmpty() || pat.isEmpty()) {
                return SyncResult("not_configured", "Inserisci owner, repository, branch e token GitHub.")
            }
            val url = buildGithubContentsUrl(owner, repo, branch, gh.basePath, "manifest.json")
            val resp = httpGetText(url, pat)
            if (!resp.ok) {
                val safeMsg = redactSecrets(resp.errorMessage, pat)
                SyncResult(resp.status, safeMsg)
            } else {
                val mObj = try {
                    JSONObject(resp.body)
                } catch (_: Exception) {
                    return SyncResult("error", "manifest.json remoto non è un JSON valido.")
                }
                val v = PackValidator.validateManifest(mObj)
                if (!v.isValid) {
                    SyncResult("error", "manifest.json remoto non valido.", v.errors)
                } else {
                    val count = mObj.optJSONArray("packs")?.length() ?: 0
                    SyncResult("ok", "Connessione riuscita! Trovati $count pacchetti nel manifest.")
                }
            }
        } finally {
            syncLock.set(false)
        }
    }

    suspend fun syncFromGithub(): SyncResult {
        if (!syncLock.compareAndSet(false, true)) {
            return SyncResult("busy", "Sincronizzazione già in corso...")
        }
        return try {
            val gh = saveManager.config.github
            val owner = gh.owner.trim()
            val repo = gh.repo.trim()
            val branch = gh.branch.trim()
            val pat = gh.pat.trim()
            if (owner.isEmpty() || repo.isEmpty() || branch.isEmpty() || pat.isEmpty()) {
                val res = SyncResult("not_configured", "GitHub non configurato.")
                persistSyncState(res)
                return res
            }

            val manifestUrl = buildGithubContentsUrl(owner, repo, branch, gh.basePath, "manifest.json")
            val manifestResp = httpGetText(manifestUrl, pat)
            if (!manifestResp.ok) {
                val res = SyncResult(manifestResp.status, redactSecrets(manifestResp.errorMessage, pat))
                persistSyncState(res)
                return res
            }

            val manifestObj = try {
                JSONObject(manifestResp.body)
            } catch (_: Exception) {
                val res = SyncResult("error", "manifest.json remoto corrotto. Cache locale preservata.")
                persistSyncState(res)
                return res
            }

            val manifestValidation = PackValidator.validateManifest(manifestObj)
            if (!manifestValidation.isValid) {
                val res = SyncResult(
                    "error",
                    "manifest.json remoto non valido. Cache locale preservata.",
                    manifestValidation.errors.map { redactSecrets(it, pat) }
                )
                persistSyncState(res)
                return res
            }

            val entries = PackValidator.parseManifestEntries(manifestObj)
            clearDirectory(stagingDir)

            val errors = mutableListOf<String>()
            var downloadedCount = 0
            val stagedFiles = mutableListOf<Pair<File, File>>() // staging -> cache

            for (entry in entries) {
                if (entry.archived) continue
                val cachedPack = allPacksMap[entry.packId]
                val cachedFile = resolveSafeChild(cacheDir, entry.file) ?: continue
                val needsDownload = !cachedFile.exists() ||
                    cachedPack == null ||
                    cachedPack.source != "github" ||
                    entry.version > cachedPack.version

                if (!needsDownload) continue

                val packUrl = buildGithubContentsUrl(owner, repo, branch, gh.basePath, entry.file)
                val packResp = httpGetText(packUrl, pat)
                if (!packResp.ok) {
                    errors.add("${entry.file}: ${redactSecrets(packResp.errorMessage, pat)}")
                    continue
                }

                val pObj = try {
                    JSONObject(packResp.body)
                } catch (_: Exception) {
                    errors.add("${entry.file}: JSON non valido, scartato.")
                    continue
                }

                val packVal = PackValidator.validatePack(pObj, entry.file)
                if (!packVal.isValid) {
                    errors.addAll(packVal.errors.map { redactSecrets(it, pat) })
                    continue
                }

                val stagingTarget = resolveSafeChild(stagingDir, entry.file) ?: continue
                stagingTarget.parentFile?.mkdirs()
                if (saveManager.writeJsonAtomic(stagingTarget, pObj)) {
                    stagedFiles.add(stagingTarget to cachedFile)
                    downloadedCount++
                }
            }

            // Promote valid staged files atomically into packs_cache/
            for ((src, dst) in stagedFiles) {
                val obj = JSONObject(src.readText(Charsets.UTF_8))
                saveManager.writeJsonAtomic(dst, obj)
            }
            saveManager.writeJsonAtomic(File(cacheDir, "manifest.json"), manifestObj)
            clearDirectory(stagingDir)
            reloadPacks()

            val msg = if (errors.isEmpty()) {
                if (downloadedCount > 0) {
                    "Sincronizzazione completata: $downloadedCount pacchetti aggiornati."
                } else {
                    "Tutti i pacchetti sono già aggiornati."
                }
            } else {
                "Sync parziale ($downloadedCount aggiornati, ${errors.size} avvisi)."
            }
            val res = SyncResult("ok", redactSecrets(msg, pat), errors.map { redactSecrets(it, pat) })
            persistSyncState(res)
            res
        } finally {
            syncLock.set(false)
        }
    }

    private fun persistSyncState(res: SyncResult) {
        val pat = saveManager.config.github.pat
        val nowUnix = System.currentTimeMillis() / 1000L
        saveManager.updateConfig { cfg ->
            cfg.copy(
                syncState = SyncState(
                    lastSyncUnix = nowUnix,
                    lastSyncStatus = res.status,
                    lastSyncMessage = redactSecrets(res.message, pat),
                    lastSyncErrors = res.errors.map { redactSecrets(it, pat) }
                )
            )
        }
    }

    private data class HttpFetchResult(
        val ok: Boolean,
        val status: String,
        val code: Int,
        val body: String,
        val errorMessage: String
    )

    private suspend fun httpGetText(url: String, pat: String): HttpFetchResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${pat.trim()}")
                .header("Accept", "application/vnd.github.raw+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "ScuolaRetroApp/1.0")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> HttpFetchResult(true, "ok", code, body, "")
                    code == 401 || code == 403 -> HttpFetchResult(
                        false,
                        "error",
                        code,
                        "",
                        "Accesso negato (HTTP $code): verifica i permessi di lettura del token GitHub."
                    )
                    code == 404 -> HttpFetchResult(
                        false,
                        "error",
                        code,
                        "",
                        "Percorso non trovato (HTTP 404): controlla owner, repository, branch e cartella."
                    )
                    else -> HttpFetchResult(
                        false,
                        "error",
                        code,
                        "",
                        "Errore dal server GitHub (HTTP $code)."
                    )
                }
            }
        } catch (_: IOException) {
            HttpFetchResult(false, "offline", 0, "", "Dispositivo offline o connessione scaduta. Uso cache locale.")
        } catch (_: Exception) {
            HttpFetchResult(false, "error", 0, "", "Impossibile completare la richiesta di sincronizzazione.")
        }
    }

    private fun resolveSafeChild(baseDir: File, relPath: String): File? {
        if (relPath.contains("..") || relPath.startsWith("/") || relPath.contains("\\")) return null
        val canonicalBase = baseDir.canonicalFile
        val candidate = File(canonicalBase, relPath).canonicalFile
        val basePrefix = canonicalBase.path + File.separator
        return if (candidate.path.startsWith(basePrefix)) candidate else null
    }

    private fun clearDirectory(dir: File) {
        dir.listFiles()?.forEach { f ->
            if (f.isDirectory) {
                f.deleteRecursively()
            } else {
                f.delete()
            }
        }
    }

    companion object {
        fun buildGithubContentsUrl(
            owner: String,
            repo: String,
            branch: String,
            basePath: String,
            relPath: String
        ): String {
            val cleanOwner = owner.trim().trim('/')
            val cleanRepo = repo.trim().trim('/')
            val cleanBranch = branch.trim()
            val cleanBase = basePath.trim().trim('/')
            val cleanRel = relPath.trim().trim('/')
            val combinedPath = if (cleanBase.isEmpty()) cleanRel else "$cleanBase/$cleanRel"
            return "https://api.github.com/repos/$cleanOwner/$cleanRepo/contents/$combinedPath?ref=$cleanBranch"
        }

        fun redactSecrets(message: String, pat: String): String {
            var sanitized = message
            val trimmedPat = pat.trim()
            if (trimmedPat.isNotEmpty()) {
                sanitized = sanitized.replace(trimmedPat, "[REDACTED]")
            }
            sanitized = sanitized.replace(Regex("Bearer\\s+\\S+", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
            sanitized = sanitized.replace(Regex("github_pat_[A-Za-z0-9_]+"), "[REDACTED]")
            sanitized = sanitized.replace(Regex("ghp_[A-Za-z0-9_]+"), "[REDACTED]")
            return sanitized
        }
    }
}
