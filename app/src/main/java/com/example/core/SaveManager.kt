package com.example.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class SaveManager(private val rootDir: File) {

    val configFile: File = File(rootDir, "config.json")
    val profilesDir: File = File(rootDir, "profiles")

    var config: AppConfig = AppConfig()
        private set

    var activeProfile: ProfileData? = null
        private set

    init {
        rootDir.mkdirs()
        profilesDir.mkdirs()
    }

    fun loadAll() {
        config = loadConfig()
        if (config.activeProfileId.isNotBlank()) {
            val loaded = loadProfile(config.activeProfileId)
            if (loaded != null) {
                activeProfile = loaded
            } else {
                val existing = listProfiles()
                if (existing.isNotEmpty()) {
                    switchProfile(existing.first().profileId)
                } else {
                    activeProfile = null
                }
            }
        } else {
            val existing = listProfiles()
            if (existing.isNotEmpty()) {
                switchProfile(existing.first().profileId)
            }
        }
    }

    fun writeJsonAtomic(targetFile: File, obj: JSONObject): Boolean {
        return try {
            targetFile.parentFile?.mkdirs()
            val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
            val bakFile = File(targetFile.parentFile, "${targetFile.name}.bak")
            val serialized = obj.toString(2)
            tmpFile.writeText(serialized, Charsets.UTF_8)

            // Verify integrity by re-reading and parsing
            val verifyText = tmpFile.readText(Charsets.UTF_8)
            JSONObject(verifyText)

            if (bakFile.exists()) {
                bakFile.delete()
            }
            if (targetFile.exists()) {
                if (!targetFile.renameTo(bakFile)) {
                    targetFile.copyTo(bakFile, overwrite = true)
                    targetFile.delete()
                }
            }
            if (!tmpFile.renameTo(targetFile)) {
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readJsonWithRecovery(targetFile: File, defaultObj: JSONObject): JSONObject {
        val bakFile = File(targetFile.parentFile, "${targetFile.name}.bak")
        if (targetFile.exists()) {
            try {
                return JSONObject(targetFile.readText(Charsets.UTF_8))
            } catch (_: Exception) {
                // Fall through to .bak recovery
            }
        }
        if (bakFile.exists()) {
            try {
                val recovered = JSONObject(bakFile.readText(Charsets.UTF_8))
                // Restore main file from backup
                writeJsonAtomic(targetFile, recovered)
                return recovered
            } catch (_: Exception) {
                // Fall through to default
            }
        }
        return JSONObject(defaultObj.toString())
    }

    private fun loadConfig(): AppConfig {
        val obj = readJsonWithRecovery(configFile, configToJson(AppConfig()))
        val audioObj = obj.optJSONObject("audio") ?: JSONObject()
        val studyObj = obj.optJSONObject("study_preferences") ?: JSONObject()
        val syncObj = obj.optJSONObject("sync_state") ?: JSONObject()
        val prioObj = obj.optJSONObject("priority_overrides") ?: JSONObject()

        val prioMap = mutableMapOf<String, Boolean>()
        val keys = prioObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            prioMap[k] = prioObj.optBoolean(k, false)
        }

        val focusObj = obj.optJSONObject("focus_topics") ?: JSONObject()
        val focusMap = mutableMapOf<String, List<String>>()
        val fKeys = focusObj.keys()
        while (fKeys.hasNext()) {
            val packId = fKeys.next()
            val arr = focusObj.optJSONArray(packId) ?: continue
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val t = arr.optString(i, "").trim()
                if (t.isNotEmpty() && t !in list) list.add(t)
            }
            if (list.isNotEmpty()) {
                focusMap[packId] = list
            }
        }

        val syncErrors = mutableListOf<String>()
        val errArr = syncObj.optJSONArray("last_sync_errors")
        if (errArr != null) {
            for (i in 0 until errArr.length()) {
                syncErrors.add(errArr.optString(i, ""))
            }
        }

        val rawSessionLen = studyObj.optInt("session_length", GameBalance.DEFAULT_SESSION_QUESTIONS)
        val safeSessionLen = if (rawSessionLen in GameBalance.SESSION_LENGTH_OPTIONS) {
            rawSessionLen
        } else {
            GameBalance.DEFAULT_SESSION_QUESTIONS
        }

        return AppConfig(
            schemaVersion = 1,
            activeProfileId = obj.optString("active_profile_id", ""),
            parentPin = obj.optString("parent_pin", GameBalance.DEFAULT_PARENT_PIN)
                .ifBlank { GameBalance.DEFAULT_PARENT_PIN },
            audio = AudioConfig(
                masterMuted = audioObj.optBoolean("master_muted", false),
                musicVolume = audioObj.optDouble("music_volume", 0.7).toFloat().coerceIn(0f, 1f),
                sfxVolume = audioObj.optDouble("sfx_volume", 0.9).toFloat().coerceIn(0f, 1f)
            ),
            studyPreferences = StudyPreferences(
                calmMode = studyObj.optBoolean("calm_mode", false),
                reducedMotion = studyObj.optBoolean("reduced_motion", false),
                readingPauseSec = studyObj.optDouble(
                    "reading_pause_sec",
                    GameBalance.DEFAULT_READING_PAUSE_SEC.toDouble()
                ).toFloat().coerceIn(0f, 3f),
                sessionLength = safeSessionLen,
                enable5050 = studyObj.optBoolean("enable_50_50", false),
                showVirtualDpad = studyObj.optBoolean("show_virtual_dpad", true),
                showGuidancePill = studyObj.optBoolean("show_guidance_pill", true),
                autoTtsQuestions = studyObj.optBoolean("auto_tts_questions", false),
                darkMode = studyObj.optBoolean("dark_mode", true)
            ),
            priorityOverrides = prioMap,
            focusTopics = focusMap,
            syncState = SyncState(
                lastSyncUnix = syncObj.optLong("last_sync_unix", 0L),
                lastSyncStatus = syncObj.optString("last_sync_status", "never"),
                lastSyncMessage = syncObj.optString("last_sync_message", ""),
                lastSyncErrors = syncErrors
            )
        )
    }

    fun updateConfig(transform: (AppConfig) -> AppConfig): AppConfig {
        config = transform(config)
        saveConfig()
        return config
    }

    fun saveConfig(): Boolean = writeJsonAtomic(configFile, configToJson(config))

    fun getSessionLength(): Int = config.studyPreferences.sessionLength

    fun profileFile(profileId: String): File = File(File(profilesDir, profileId), "save.json")

    fun listProfiles(): List<ProfileSummary> {
        val result = mutableListOf<ProfileSummary>()
        val dirs = profilesDir.listFiles { f -> f.isDirectory } ?: return result
        for (dir in dirs.sortedBy { it.name }) {
            val saveFile = File(dir, "save.json")
            val bakFile = File(dir, "save.json.bak")
            if (!saveFile.exists() && !bakFile.exists()) continue
            val prof = loadProfile(dir.name) ?: continue
            result.add(
                ProfileSummary(
                    profileId = prof.profileId,
                    name = prof.name,
                    avatarId = prof.avatarId,
                    stars = prof.stars
                )
            )
        }
        return result
    }

    fun createProfile(rawName: String, avatarId: Int): String {
        val cleanName = rawName.trim()
        require(cleanName.length in 1..16) { "Il nome deve avere tra 1 e 16 caratteri." }
        val safeAvatar = avatarId.coerceIn(0, 3)
        val profileId = "prof_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val profile = ProfileData(
            schemaVersion = 1,
            profileId = profileId,
            name = cleanName,
            avatarId = safeAvatar,
            stars = 0,
            equippedCompanion = "",
            equippedHat = "",
            equippedFrame = "frame_classic",
            ownedItems = listOf("frame_classic"),
            lockerStickers = listOf("", "", "", "", "", ""),
            medals = emptyList(),
            claimedMissionKeys = emptyList(),
            leitner = emptyMap(),
            recentSessions = emptyList()
        )
        activeProfile = profile
        saveActiveProfile()
        config = config.copy(activeProfileId = profileId)
        saveConfig()
        return profileId
    }

    fun switchProfile(profileId: String): Boolean {
        saveActiveProfile()
        val loaded = loadProfile(profileId) ?: return false
        activeProfile = loaded
        config = config.copy(activeProfileId = profileId)
        saveConfig()
        return true
    }

    fun updateActiveProfile(transform: (ProfileData) -> ProfileData): ProfileData? {
        val current = activeProfile ?: return null
        val updated = transform(current).let { p ->
            val cappedSessions = if (p.recentSessions.size > GameBalance.MAX_RECENT_SESSIONS) {
                p.recentSessions.takeLast(GameBalance.MAX_RECENT_SESSIONS)
            } else {
                p.recentSessions
            }
            val normalizedStickers = (p.lockerStickers + List(6) { "" }).take(6)
            p.copy(recentSessions = cappedSessions, lockerStickers = normalizedStickers)
        }
        activeProfile = updated
        saveActiveProfile()
        return updated
    }

    fun resetActiveProfileProgress(): ProfileData? {
        val current = activeProfile ?: return null
        val reset = ProfileData(
            schemaVersion = 1,
            profileId = current.profileId,
            name = current.name,
            avatarId = current.avatarId,
            stars = 0,
            equippedCompanion = "",
            equippedHat = "",
            equippedFrame = "frame_classic",
            ownedItems = listOf("frame_classic"),
            lockerStickers = listOf("", "", "", "", "", ""),
            medals = emptyList(),
            claimedMissionKeys = emptyList(),
            leitner = emptyMap(),
            recentSessions = emptyList()
        )
        activeProfile = reset
        saveActiveProfile()
        return reset
    }

    fun saveActiveProfile(): Boolean {
        val prof = activeProfile ?: return false
        return writeJsonAtomic(profileFile(prof.profileId), profileToJson(prof))
    }

    fun flushAll() {
        saveConfig()
        saveActiveProfile()
    }

    private fun loadProfile(profileId: String): ProfileData? {
        val file = profileFile(profileId)
        val bak = File(file.parentFile, "${file.name}.bak")
        if (!file.exists() && !bak.exists()) return null
        val fallback = profileToJson(
            ProfileData(profileId = profileId, name = "Studente")
        )
        val obj = readJsonWithRecovery(file, fallback)
        return jsonToProfile(obj, profileId)
    }

    private fun configToJson(cfg: AppConfig): JSONObject {
        val root = JSONObject()
        root.put("schema_version", cfg.schemaVersion)
        root.put("active_profile_id", cfg.activeProfileId)
        root.put("parent_pin", cfg.parentPin)

        root.put("audio", JSONObject().apply {
            put("master_muted", cfg.audio.masterMuted)
            put("music_volume", cfg.audio.musicVolume.toDouble())
            put("sfx_volume", cfg.audio.sfxVolume.toDouble())
        })

        root.put("study_preferences", JSONObject().apply {
            put("calm_mode", cfg.studyPreferences.calmMode)
            put("reduced_motion", cfg.studyPreferences.reducedMotion)
            put("reading_pause_sec", cfg.studyPreferences.readingPauseSec.toDouble())
            put("session_length", cfg.studyPreferences.sessionLength)
            put("enable_50_50", cfg.studyPreferences.enable5050)
            put("show_virtual_dpad", cfg.studyPreferences.showVirtualDpad)
            put("show_guidance_pill", cfg.studyPreferences.showGuidancePill)
            put("auto_tts_questions", cfg.studyPreferences.autoTtsQuestions)
            put("dark_mode", cfg.studyPreferences.darkMode)
        })

        val prioObj = JSONObject()
        cfg.priorityOverrides.forEach { (k, v) -> prioObj.put(k, v) }
        root.put("priority_overrides", prioObj)

        val focusObj = JSONObject()
        cfg.focusTopics.forEach { (packId, topics) ->
            val clean = topics.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (clean.isNotEmpty()) {
                val arr = JSONArray()
                clean.forEach { arr.put(it) }
                focusObj.put(packId, arr)
            }
        }
        root.put("focus_topics", focusObj)

        val errArr = JSONArray()
        cfg.syncState.lastSyncErrors.forEach { errArr.put(it) }
        root.put("sync_state", JSONObject().apply {
            put("last_sync_unix", cfg.syncState.lastSyncUnix)
            put("last_sync_status", cfg.syncState.lastSyncStatus)
            put("last_sync_message", cfg.syncState.lastSyncMessage)
            put("last_sync_errors", errArr)
        })

        return root
    }

    private fun profileToJson(p: ProfileData): JSONObject {
        val root = JSONObject()
        root.put("schema_version", p.schemaVersion)
        root.put("profile_id", p.profileId)
        root.put("name", p.name)
        root.put("avatar_id", p.avatarId)
        root.put("stars", p.stars)
        root.put("equipped_companion", p.equippedCompanion)
        root.put("equipped_hat", p.equippedHat)
        root.put("equipped_frame", p.equippedFrame)

        root.put("owned_items", JSONArray().apply { p.ownedItems.forEach { put(it) } })
        root.put("locker_stickers", JSONArray().apply { p.lockerStickers.forEach { put(it) } })
        root.put("medals", JSONArray().apply { p.medals.forEach { put(it) } })
        root.put("claimed_mission_keys", JSONArray().apply { p.claimedMissionKeys.takeLast(30).forEach { put(it) } })

        val leitnerObj = JSONObject()
        p.leitner.forEach { (qId, rec) ->
            leitnerObj.put(qId, JSONObject().apply {
                put("box", rec.box)
                put("last_seen_unix", rec.lastSeenUnix)
                put("correct_total", rec.correctTotal)
                put("wrong_total", rec.wrongTotal)
                put("last_wrong_unix", rec.lastWrongUnix)
            })
        }
        root.put("leitner", leitnerObj)

        val sessionsArr = JSONArray()
        p.recentSessions.forEach { s ->
            sessionsArr.put(JSONObject().apply {
                put("timestamp_unix", s.timestampUnix)
                put("pack_id", s.packId)
                put("pack_title", s.packTitle)
                put("minigame_id", s.minigameId)
                put("is_boss", s.isBoss)
                put("victory", s.victory)
                put("correct_first_try", s.correctFirstTry)
                put("total_questions", s.totalQuestions)
                put("stars_earned", s.starsEarned)
            })
        }
        root.put("recent_sessions", sessionsArr)
        return root
    }

    private fun jsonToProfile(obj: JSONObject, fallbackId: String): ProfileData {
        val owned = mutableListOf<String>()
        val ownedArr = obj.optJSONArray("owned_items")
        if (ownedArr != null) {
            for (i in 0 until ownedArr.length()) owned.add(ownedArr.optString(i, ""))
        }
        if ("frame_classic" !in owned) owned.add(0, "frame_classic")

        val stickers = mutableListOf<String>()
        val stArr = obj.optJSONArray("locker_stickers")
        if (stArr != null) {
            for (i in 0 until stArr.length()) stickers.add(stArr.optString(i, ""))
        }
        while (stickers.size < 6) stickers.add("")

        val medals = mutableListOf<String>()
        val medArr = obj.optJSONArray("medals")
        if (medArr != null) {
            for (i in 0 until medArr.length()) {
                val m = medArr.optString(i, "")
                if (m.isNotBlank()) medals.add(m)
            }
        }

        val leitnerMap = mutableMapOf<String, LeitnerRecord>()
        val lObj = obj.optJSONObject("leitner")
        if (lObj != null) {
            val keys = lObj.keys()
            while (keys.hasNext()) {
                val qId = keys.next()
                val rec = lObj.optJSONObject(qId) ?: continue
                leitnerMap[qId] = LeitnerRecord(
                    box = rec.optInt("box", 0).coerceIn(0, GameBalance.LEITNER_MAX_BOX),
                    lastSeenUnix = rec.optLong("last_seen_unix", 0L),
                    correctTotal = rec.optInt("correct_total", 0),
                    wrongTotal = rec.optInt("wrong_total", 0),
                    lastWrongUnix = rec.optLong("last_wrong_unix", 0L)
                )
            }
        }

        val sessions = mutableListOf<SessionRecord>()
        val sArr = obj.optJSONArray("recent_sessions")
        if (sArr != null) {
            for (i in 0 until sArr.length()) {
                val s = sArr.optJSONObject(i) ?: continue
                sessions.add(
                    SessionRecord(
                        timestampUnix = s.optLong("timestamp_unix", 0L),
                        packId = s.optString("pack_id", ""),
                        packTitle = s.optString("pack_title", ""),
                        minigameId = s.optString("minigame_id", "yard_battle"),
                        isBoss = s.optBoolean("is_boss", false),
                        victory = s.optBoolean("victory", true),
                        correctFirstTry = s.optInt("correct_first_try", 0),
                        totalQuestions = s.optInt("total_questions", 0),
                        starsEarned = s.optInt("stars_earned", 0)
                    )
                )
            }
        }

        val claimedMissions = mutableListOf<String>()
        val cmArr = obj.optJSONArray("claimed_mission_keys")
        if (cmArr != null) {
            for (i in 0 until cmArr.length()) {
                val key = cmArr.optString(i, "")
                if (key.isNotBlank()) claimedMissions.add(key)
            }
        }

        return ProfileData(
            schemaVersion = 1,
            profileId = obj.optString("profile_id", fallbackId).ifBlank { fallbackId },
            name = obj.optString("name", "Studente").ifBlank { "Studente" },
            avatarId = obj.optInt("avatar_id", 0).coerceIn(0, 3),
            stars = obj.optInt("stars", 0).coerceAtLeast(0),
            equippedCompanion = obj.optString("equipped_companion", ""),
            equippedHat = obj.optString("equipped_hat", ""),
            equippedFrame = obj.optString("equipped_frame", "frame_classic").ifBlank { "frame_classic" },
            ownedItems = owned.distinct(),
            lockerStickers = stickers.take(6),
            medals = medals.distinct(),
            claimedMissionKeys = claimedMissions.distinct().takeLast(30),
            leitner = leitnerMap,
            recentSessions = sessions.takeLast(GameBalance.MAX_RECENT_SESSIONS)
        )
    }

    companion object {
        fun parseShopCatalog(rawJson: String): List<ShopItem> {
            val result = mutableListOf<ShopItem>()
            val root = JSONObject(rawJson)
            val arr = root.optJSONArray("items") ?: return result
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                result.add(
                    ShopItem(
                        id = item.getString("id"),
                        category = item.getString("category"),
                        name = item.getString("name"),
                        price = item.getInt("price"),
                        icon = item.optString("icon", ""),
                        colorHex = item.optString("color_hex", "")
                    )
                )
            }
            return result
        }
    }
}
