package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.BundledAssetReader
import com.example.core.GameBalance
import com.example.core.GeneratorSpec
import com.example.core.LearningEngine
import com.example.core.MathGenerator
import com.example.core.PackManager
import com.example.core.PackValidator
import com.example.core.QuestionData
import com.example.core.SaveManager
import com.example.core.TextFormatter
import com.example.pixelart.Facing
import com.example.pixelart.GridPos
import com.example.pixelart.PixelArtAssets
import com.example.pixelart.SchoolMaps
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import java.util.TimeZone
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScuolaRetroTestSuite {

    private lateinit var context: Context
    private lateinit var sandboxDir: File
    private lateinit var saveManager: SaveManager
    private lateinit var assetReader: BundledAssetReader
    private lateinit var packManager: PackManager
    private lateinit var learningEngine: LearningEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sandboxDir = Files.createTempDirectory("scuola_retro_test_").toFile()
        saveManager = SaveManager(sandboxDir)
        saveManager.loadAll()
        assetReader = BundledAssetReader { path ->
            try {
                context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: Exception) {
                null
            }
        }
        packManager = PackManager(sandboxDir, saveManager, assetReader)
        packManager.reloadPacks()
        learningEngine = LearningEngine(saveManager, packManager)
    }

    // Group 1: PackValidator & TextFormatter
    @Test
    fun test01_packValidatorAndTextFormatter() {
        val manifestRaw = assetReader.readAssetText("packs/manifest.json")
        assertNotNull("Bundled manifest.json missing", manifestRaw)
        val manifestVal = PackValidator.validateManifest(JSONObject(manifestRaw!!))
        assertTrue("Bundled manifest should be valid: ${manifestVal.errors}", manifestVal.isValid)

        // Reject path traversal & duplicates in manifest
        val evilManifest = JSONObject(
            """{"schema_version":1,"packs":[
                {"pack_id":"ok_pack","file":"../secret.json","subject":"storia","title":"Bad Path","version":1},
                {"pack_id":"ok_pack","file":"/etc/passwd","subject":"storia","title":"Dup Id","version":1}
            ]}"""
        )
        val evilRes = PackValidator.validateManifest(evilManifest)
        assertFalse(evilRes.isValid)
        assertTrue(evilRes.errors.size >= 3)

        // Validate all 6 bundled packs have 0 errors
        val allBundledFiles = listOf(
            "storia_egizi.json",
            "scienze_fotosintesi.json",
            "matematica_calcolo_base.json",
            "italiano_grammatica.json",
            "geografia_italia.json",
            "inglese_base.json"
        )
        for (fileName in allBundledFiles) {
            val raw = assetReader.readAssetText("packs/$fileName")
            assertNotNull("Missing bundled pack $fileName", raw)
            val res = PackValidator.validatePack(JSONObject(raw!!), fileName)
            assertTrue("Pack $fileName invalid: ${res.errors}", res.isValid)
            assertEquals(0, res.errors.size)
        }

        // TextFormatter stripping & safe BBCode escaping
        assertEquals("Il Nilo è lungo", TextFormatter.stripMarkers("Il **Nilo** è lungo"))
        assertEquals("Valore [lb]1] [b][color=#0F4C81]oro[/color][/b]", TextFormatter.toBbcode("Valore [1] **oro**"))

        // Reject negative numeric_input, duplicate options, bad correct_index, >140 prompt, >90 hint, empty explanation
        val invalidPack = JSONObject(
            """{
              "schema_version":1,"pack_id":"bad_pack","subject":"storia","grade":4,
              "title":"Test Errato","version":1,"boss_name":"Boss","medal_name":"Medaglia",
              "questions":[
                {"id":"q_001","type":"numeric_input","difficulty":1,"prompt":"Test","correct_value":-5,"explanation":"Ok"},
                {"id":"q_002","type":"multiple_choice","difficulty":1,"prompt":"${"a".repeat(145)}","hint":"${"h".repeat(95)}",
                 "options":["Uno","uno","Tre","Quattro"],"correct_index":4,"explanation":""},
                {"id":"q_003","type":"classification","difficulty":1,"prompt":"Class","explanation":"Spiegazione"}
              ]
            }"""
        )
        val invalidRes = PackValidator.validatePack(invalidPack, "bad.json")
        assertFalse(invalidRes.isValid)
        assertTrue(invalidRes.errors.any { it.contains("correct_value") })
        assertTrue(invalidRes.errors.any { it.contains("duplicate") })
        assertTrue(invalidRes.errors.any { it.contains("correct_index") })
        assertTrue(invalidRes.errors.any { it.contains("140") })
        assertTrue(invalidRes.errors.any { it.contains("90") })
        assertTrue(invalidRes.warnings.any { it.contains("classification") })
        assertTrue(invalidRes.warnings.any { it.contains("Cortile") })
    }

    // Group 2: MathGenerator determinism & validity
    @Test
    fun test02_mathGeneratorDeterministicAndNonNegative() {
        val specMul = GeneratorSpec("gen_mul", "multiplication_table", 1, mapOf("min_a" to 6, "max_a" to 9, "min_b" to 2, "max_b" to 9), 10)
        val specSub = GeneratorSpec("gen_sub", "mental_add_sub", 2, mapOf("min_val" to 10, "max_val" to 90, "allow_sub" to true), 12)
        val specUnit = GeneratorSpec("gen_unit", "unit_conversion", 2, mapOf("family" to "mass"), 6)

        val run1 = MathGenerator.generatePool("mat_test", specMul)
        val run2 = MathGenerator.generatePool("mat_test", specMul)
        assertEquals(run1, run2)
        assertEquals(10, run1.size)

        (run1 + MathGenerator.generatePool("mat_test", specSub) + MathGenerator.generatePool("mat_test", specUnit)).forEach { q ->
            assertEquals("numeric_input", q.type)
            assertTrue("Generated value must be >= 0", q.correctValue >= 0.0)
            assertTrue("Generated hint must not be blank", q.hint.isNotBlank())
            assertTrue(TextFormatter.stripMarkers(q.prompt).length <= 140)
            assertTrue(TextFormatter.stripMarkers(q.hint).length <= 90)
        }
    }

    // Group 3: LearningEngine Leitner, usedHelp, playableQuestions mastery, calendar week, micro-breaks
    @Test
    fun test03_learningEngineLeitnerMasteryWeekAndBreaks() {
        saveManager.createProfile("Marco", 0)
        val qId = "sto_egizi_001"

        // usedHelp promotes 0 -> 1 max, never 2 or 3
        learningEngine.recordAnswer(qId, isCorrect = true, isRetryInSession = false, usedHelp = true, nowUnix = 1000L)
        assertEquals(1, saveManager.activeProfile!!.leitner[qId]!!.box)
        learningEngine.recordAnswer(qId, isCorrect = true, isRetryInSession = false, usedHelp = true, nowUnix = 2000L)
        assertEquals(1, saveManager.activeProfile!!.leitner[qId]!!.box)

        // Independent correct promotes 1 -> 2 -> 3
        learningEngine.recordAnswer(qId, isCorrect = true, isRetryInSession = false, usedHelp = false, nowUnix = 3000L)
        assertEquals(2, saveManager.activeProfile!!.leitner[qId]!!.box)
        learningEngine.recordAnswer(qId, isCorrect = true, isRetryInSession = false, usedHelp = false, nowUnix = 4000L)
        assertEquals(3, saveManager.activeProfile!!.leitner[qId]!!.box)

        // Wrong resets to 0
        learningEngine.recordAnswer(qId, isCorrect = false, isRetryInSession = false, usedHelp = false, nowUnix = 5000L)
        assertEquals(0, saveManager.activeProfile!!.leitner[qId]!!.box)

        // Retry correct stays at 0
        learningEngine.recordAnswer(qId, isCorrect = true, isRetryInSession = true, usedHelp = false, nowUnix = 6000L)
        assertEquals(0, saveManager.activeProfile!!.leitner[qId]!!.box)

        // Top weak questions includes qId
        val weak = learningEngine.getTopWeakQuestions(5)
        assertTrue(weak.any { it.questionId == qId && it.wrongTotal == 1 })

        // Mastery on generator-only pack (0 static questions, no div-by-zero)
        val genOnlyJson = """{
            "schema_version":1,"pack_id":"gen_only_pack","subject":"matematica","grade":4,
            "title":"Solo Generatori","version":1,"boss_name":"Boss","medal_name":"Med",
            "questions":[],
            "generators":[{"id":"gen_pure","template":"multiplication_table","difficulty":1,"params":{"min_a":2,"max_a":5,"min_b":2,"max_b":5},"pool_size":6}]
        }"""
        assertTrue(packManager.importPackFromJsonText(genOnlyJson, "gen.json").isValid)
        val genPack = packManager.getPack("gen_only_pack")!!
        assertEquals(0, genPack.staticQuestions.size)
        assertEquals(6, genPack.playableQuestions.size)
        assertEquals(0f, learningEngine.getPackMasteryPercent(genPack), 0.01f)

        // Raise all questions to box 3 -> 100% mastery & Boss unlocked
        genPack.playableQuestions.forEach { q ->
            repeat(3) {
                learningEngine.recordAnswer(q.id, isCorrect = true, isRetryInSession = false, usedHelp = false)
            }
        }
        assertEquals(100f, learningEngine.getPackMasteryPercent(genPack), 0.01f)
        assertTrue(learningEngine.isBossUnlocked("gen_only_pack"))

        // Non-mutating option shuffler
        val storiaPack = packManager.getPack("storia_egizi")!!
        val mcQ = storiaPack.staticQuestions.first { it.type == "multiple_choice" }
        val originalOptions = mcQ.options.toList()
        val shuffled = learningEngine.getShuffledOptions(mcQ, Random(42))
        assertEquals(originalOptions, mcQ.options)
        assertEquals(mcQ.options[mcQ.correctIndex], shuffled.options[shuffled.correctDisplayIndex])

        // Recommended activity works
        assertNotNull(learningEngine.getRecommendedActivity())

        // Calendar week Monday 00:00 & Micro-break every 2 sessions
        val utc = TimeZone.getTimeZone("UTC")
        // 2026-11-18T12:00:00Z is Wednesday -> Monday 2026-11-16T00:00:00Z = 1794787200
        val wednesdayNoon = 1794787200L + 2 * 86400L + 12 * 3600L
        val mondayMidnight = LearningEngine.computeMondayMidnightUnix(wednesdayNoon, utc)
        assertEquals(1794787200L, mondayMidnight)

        learningEngine.resetRunSessionCounter()
        val s1 = learningEngine.completeMinigameSession("storia_egizi", "yard_battle", false, true, 4, 4, 0, 40, 0f, wednesdayNoon)
        assertFalse(s1.suggestMicroBreak)
        val s2 = learningEngine.completeMinigameSession("storia_egizi", "yard_battle", false, true, 4, 4, 0, 40, 10f, wednesdayNoon + 60)
        assertTrue(s2.suggestMicroBreak)
        assertEquals(2, learningEngine.getSessionsThisWeek(wednesdayNoon + 120, utc))
    }

    // Group 4: SaveManager atomic write, .bak recovery, defaults merge, profile reset
    @Test
    fun test04_saveManagerAtomicRecoveryAndProfiles() {
        val profId = saveManager.createProfile("Giulia", 2)
        saveManager.updateActiveProfile { it.copy(stars = 150, medals = listOf("storia_egizi")) }
        // Second write creates .bak
        saveManager.updateActiveProfile { it.copy(stars = 180) }

        val saveFile = saveManager.profileFile(profId)
        val bakFile = File(saveFile.parentFile, "save.json.bak")
        assertTrue(saveFile.exists())
        assertTrue(bakFile.exists())

        // Corrupt primary save.json -> recovery must load .bak automatically
        saveFile.writeText("{ CORRUPT JSON !!!", Charsets.UTF_8)
        val freshManager = SaveManager(sandboxDir)
        freshManager.loadAll()
        assertNotNull(freshManager.activeProfile)
        assertEquals("Giulia", freshManager.activeProfile!!.name)
        assertEquals(150, freshManager.activeProfile!!.stars)

        // Default enable5050 must be false
        assertFalse(freshManager.config.studyPreferences.enable5050)

        // Reset preserves name & avatar, clears stars/medals/leitner
        freshManager.resetActiveProfileProgress()
        assertEquals("Giulia", freshManager.activeProfile!!.name)
        assertEquals(2, freshManager.activeProfile!!.avatarId)
        assertEquals(0, freshManager.activeProfile!!.stars)
        assertTrue(freshManager.activeProfile!!.medals.isEmpty())
        assertTrue(freshManager.listProfiles().any { it.profileId == profId })
    }

    // Group 5: PackManager precedence, URL normalization, archived medal preservation, PAT redaction
    @Test
    fun test05_packManagerPrecedenceUrlAndPatRedaction() {
        assertEquals(6, packManager.getActivePacks().size)

        // URL normalization (zero double slashes)
        assertEquals(
            "https://api.github.com/repos/user/repo/contents/manifest.json?ref=main",
            PackManager.buildGithubContentsUrl(" user ", " repo ", " main ", "", "manifest.json")
        )
        assertEquals(
            "https://api.github.com/repos/user/repo/contents/packs/storia_egizi.json?ref=main",
            PackManager.buildGithubContentsUrl("user", "repo", "main", "/packs/", "/storia_egizi.json")
        )
        assertEquals(
            "https://api.github.com/repos/user/repo/contents/packs/manifest.json?ref=main",
            PackManager.buildGithubContentsUrl("user", "repo", "main", " packs ", "manifest.json")
        )

        // PAT redaction
        val secretPat = "github_pat_11ABCDEF999SECRETTOKEN"
        val redacted = PackManager.redactSecrets("Auth failed with Bearer $secretPat for $secretPat", secretPat)
        assertFalse(redacted.contains(secretPat))

        // Archived pack in cache hides from activePacks while preserving profile medals
        saveManager.createProfile("Luca", 1)
        saveManager.updateActiveProfile { it.copy(medals = listOf("storia_egizi")) }
        val archiveManifest = JSONObject(
            """{"schema_version":1,"packs":[
                {"pack_id":"storia_egizi","file":"storia_egizi.json","subject":"storia","title":"Egizi","version":1,"archived":true}
            ]}"""
        )
        saveManager.writeJsonAtomic(File(packManager.cacheDir, "manifest.json"), archiveManifest)
        packManager.reloadPacks()
        assertFalse(packManager.getActivePacks().any { it.packId == "storia_egizi" })
        assertTrue(saveManager.activeProfile!!.medals.contains("storia_egizi"))
    }

    // Group 6: Balance, 50/50, Anti-Loop, Epsilon, Boss first win vs rematch, A* pathfinding
    @Test
    fun test06_balanceFiftyFiftyEpsilonBossAndAStar() {
        assertEquals(80, GameBalance.enemyMaxHpForDeck(4))
        assertEquals(120, GameBalance.enemyMaxHpForDeck(6))
        assertEquals(160, GameBalance.enemyMaxHpForDeck(8))
        assertEquals(2, GameBalance.MAX_RETRIES_PER_QUESTION)

        // 50/50 eliminates 2 distinct wrong indices and never correctDisplayIndex
        val elim = LearningEngine.pickFiftyFiftyEliminations(4, correctDisplayIndex = 2, rng = Random(7))
        assertEquals(2, elim.size)
        assertFalse(2 in elim)

        // Numeric epsilon with comma
        assertTrue(LearningEngine.isNumericAnswerCorrect("4,0", 4.0, 0.0))
        assertTrue(LearningEngine.isNumericAnswerCorrect("0,30001", 0.3, 0.0))
        assertFalse(LearningEngine.isNumericAnswerCorrect("0,35", 0.3, 0.0))

        // Boss Gym >= 75% first-try accuracy
        assertTrue(LearningEngine.isBossGymVictory(true, 6, 8))
        assertFalse(LearningEngine.isBossGymVictory(true, 5, 8))

        // Boss first win (+60 + medal) vs rematch (+25, no duplicate medal)
        saveManager.createProfile("Sara", 3)
        val firstWin = learningEngine.completeMinigameSession("storia_egizi", "yard_battle", true, true, 6, 6, 0, 60, 85f)
        assertTrue(firstWin.medalUnlocked)
        assertEquals(60 + GameBalance.STARS_BOSS_VICTORY_BONUS, firstWin.starsEarned)

        val rematch = learningEngine.completeMinigameSession("storia_egizi", "yard_battle", true, true, 6, 6, 0, 60, 90f)
        assertFalse(rematch.medalUnlocked)
        assertEquals(60 + GameBalance.STARS_SESSION_COMPLETE, rematch.starsEarned)
        assertEquals(1, saveManager.activeProfile!!.medals.count { it == "storia_egizi" })

        // 4-dir A* pathfinding walks right up to adjacent tile of solid NPC
        val yard = SchoolMaps.schoolYard
        val npcHistory = yard.npcs.first { it.id == "npc_history" }
        val path = SchoolMaps.findPath4Dir(yard, yard.spawns["default"]!!, npcHistory.pos, allowPartialPath = true)
        assertTrue(path.isNotEmpty())
        val end = path.last()
        val manhattan = kotlin.math.abs(end.x - npcHistory.pos.x) + kotlin.math.abs(end.y - npcHistory.pos.y)
        assertEquals(1, manhattan)
    }

    // Group 7: Neutral Educational Language Audit (Regola 0.1 — Mandatory)
    @Test
    fun test07_neutralLanguagePrivacyGuard() {
        // Construct forbidden patterns dynamically so this test file itself stays clean
        val restrictedPatterns = listOf(
            "a" + "dhd",
            "b" + "es",
            "d" + "sa",
            "dis" + "lessia",
            "dis" + "lessico",
            "dis" + "calculia",
            "dis" + "calculico",
            "dis" + "turbo",
            "dis" + "turbi",
            "ter" + "apia",
            "ter" + "apie",
            "compen" + "sativo",
            "compen" + "sativi",
            "cli" + "nico",
            "cli" + "nica",
            "dia" + "gnosi",
            "iper" + "attivita",
            "iper" + "attività",
            "neuro" + "divergente"
        )
        val regex = Regex(
            "(?i)(\\b(${restrictedPatterns.joinToString("|")})\\b|bisogni\\s+speciali)"
        )

        val projectRoot = locateProjectRoot()
        val scanTargets = listOf(
            File(projectRoot, "app/src/main/java"),
            File(projectRoot, "app/src/main/res"),
            File(projectRoot, "app/src/main/assets"),
            File(projectRoot, "metadata.json"),
            File(projectRoot, "github_packs_repo_template")
        )

        val violations = mutableListOf<String>()
        val allowedExt = setOf("kt", "xml", "json", "md", "txt")

        fun scanFile(file: File) {
            if (!file.exists()) return
            if (file.isDirectory) {
                file.listFiles()?.forEach { scanFile(it) }
                return
            }
            if (file.extension.lowercase() !in allowedExt) return
            file.readLines(Charsets.UTF_8).forEachIndexed { idx, line ->
                val match = regex.find(line)
                if (match != null) {
                    violations.add("${file.name}:${idx + 1} -> '${match.value}'")
                }
            }
        }

        scanTargets.forEach { scanFile(it) }
        assertTrue("Neutral-language guard violations found: $violations", violations.isEmpty())
    }

    // Group 8: Android Manifest & Play Policy Config Guard
    @Test
    fun test08_androidManifestAndPlayPolicyGuard() {
        val projectRoot = locateProjectRoot()
        val manifest = File(projectRoot, "app/src/main/AndroidManifest.xml").readText(Charsets.UTF_8)
        assertTrue(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertFalse(manifest.contains("READ_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("WRITE_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("READ_MEDIA_IMAGES"))

        val appName = context.getString(R.string.app_name)
        assertEquals("Maestri di Pixel", appName)
    }

    // Group 9: Space Invaders Numeric Distractors & Science Catapult Routing
    @Test
    fun test09_arcadeDistractorsAndMinigameRouting() {
        val multQ = QuestionData(
            id = "q_tab",
            type = "numeric_input",
            difficulty = 1,
            prompt = "Quanto fa **7 x 8**?",
            correctValue = 56.0,
            unit = "",
            tolerance = 0.0,
            explanation = "7 moltiplicato 8 fa 56."
        )
        val multChoices = LearningEngine.generateNumericChoices(multQ, Random(7))
        assertEquals(4, multChoices.options.size)
        assertEquals(4, multChoices.options.toSet().size)
        assertEquals("56", multChoices.options[multChoices.correctDisplayIndex])

        val unitQ = QuestionData(
            id = "q_unit",
            type = "numeric_input",
            difficulty = 1,
            prompt = "Quanti centimetri ci sono in 3 metri?",
            correctValue = 300.0,
            unit = "cm",
            tolerance = 0.0,
            explanation = "Ogni metro ha 100 centimetri, quindi 3 metri sono 300 cm."
        )
        val unitChoices = LearningEngine.generateNumericChoices(unitQ, Random(11))
        assertEquals(4, unitChoices.options.size)
        assertEquals("300 cm", unitChoices.options[unitChoices.correctDisplayIndex])
        assertTrue(unitChoices.options.all { it.endsWith(" cm") })

        // Verify Lab portal is active in SchoolHallway and reachable by A*
        val hallway = SchoolMaps.schoolHallway
        val labPortal = hallway.portals.first { it.id == "portal_lab" }
        val labPath = SchoolMaps.findPath4Dir(hallway, hallway.spawns["from_yard"]!!, labPortal.pos, allowPartialPath = true)
        assertTrue("Lab portal should be reachable from hallway entrance", labPath.isNotEmpty())
        assertEquals(labPortal.pos, labPath.last())
    }

    // Group 10: Topic Focus Filtering, Persistence & All 6 SchoolHallway Rooms
    @Test
    fun test10_topicFilteringAndAllRoomsReachable() {
        saveManager.createProfile("Elena", 1)

        // Every bundled pack should expose distinct non-empty topics
        val expectedPackIds = listOf(
            "storia_egizi", "scienze_fotosintesi", "matematica_calcolo_base",
            "italiano_grammatica", "geografia_italia", "inglese_base"
        )
        expectedPackIds.forEach { pid ->
            val pack = packManager.getPack(pid)
            assertNotNull("Pack $pid must be loaded", pack)
            assertTrue("Pack $pid should have >= 2 topics", pack!!.getTopics().size >= 2)
        }

        // Persist focusTopics on italiano_grammatica -> only "Verbi"
        saveManager.updateConfig { cfg ->
            cfg.copy(focusTopics = mapOf("italiano_grammatica" to listOf("Verbi")))
        }
        val reloaded = SaveManager(sandboxDir).apply { loadAll() }
        assertEquals(listOf("Verbi"), reloaded.config.focusTopics["italiano_grammatica"])

        // Build session deck -> every question must belong to "Verbi"
        val deck = learningEngine.buildSessionDeck(
            packId = "italiano_grammatica",
            count = 6,
            allowedTypes = setOf("multiple_choice", "true_false")
        )
        assertTrue("Focused deck must not be empty", deck.isNotEmpty())
        assertTrue("All questions must match focus topic 'Verbi'", deck.all { it.topic == "Verbi" })

        // Fallback safely if focus topic has no matching questions
        saveManager.updateConfig { cfg ->
            cfg.copy(focusTopics = mapOf("italiano_grammatica" to listOf("ArgomentoInesistente")))
        }
        val fallbackDeck = learningEngine.buildSessionDeck(
            packId = "italiano_grammatica",
            count = 4,
            allowedTypes = setOf("multiple_choice", "true_false")
        )
        assertEquals(4, fallbackDeck.size)

        // Verify all 7 active hallway doors are reachable via A* from hallway entrance and have clear door signage
        val hallway = SchoolMaps.schoolHallway
        val start = hallway.spawns["from_yard"]!!
        listOf(
            "portal_history", "portal_gym", "portal_lab", "portal_locker",
            "portal_library", "portal_geo", "portal_english"
        ).forEach { portalId ->
            val portal = hallway.portals.first { it.id == portalId }
            assertTrue("Portal $portalId must have a label", portal.label.isNotBlank())
            assertTrue("Portal $portalId must have a subtitle plaque", portal.subtitle.isNotBlank())
            val path = SchoolMaps.findPath4Dir(hallway, start, portal.pos, allowPartialPath = true)
            assertTrue("Portal $portalId must be reachable", path.isNotEmpty())
            assertEquals("Path must reach $portalId", portal.pos, path.last())
        }
    }

    // Group 11: Daily Missions, Mastery Ranks, Avatar Hats & Rare Pets, Atrium Interactive Props & NPCs
    @Test
    fun test11_dailyMissionsMasteryRanksHatsAndAtriumProps() {
        saveManager.createProfile("Matteo", 0)
        val utc = TimeZone.getTimeZone("UTC")
        val nowUnix = 1794830400L // fixed timestamp

        // Verify Mastery Rank thresholds
        assertEquals("Curioso", LearningEngine.rankForMastery(10f).title)
        assertEquals("Apprendista", LearningEngine.rankForMastery(40f).title)
        assertEquals("Esploratore", LearningEngine.rankForMastery(70f).title)
        assertEquals("Maestro di Pixel", LearningEngine.rankForMastery(90f).title)
        assertEquals(3, LearningEngine.rankForMastery(90f).starsLevel)

        // Complete 1 session with 4 correct answers -> unlocks "_first_challenge" (+25 ★) and "_sharp_answers" (+35 ★)
        learningEngine.completeMinigameSession(
            packId = "storia_egizi",
            minigameId = "yard_battle",
            isBoss = false,
            victory = true,
            correctFirstTry = 4,
            totalQuestions = 4,
            helpsUsed = 0,
            rawAnswerStars = 40,
            masteryBefore = 0f,
            nowUnix = nowUnix
        )
        val missionsBefore = learningEngine.getDailyMissions(nowUnix, utc)
        assertEquals(3, missionsBefore.size)
        val firstMission = missionsBefore.first { it.missionKey.endsWith("_first_challenge") }
        assertTrue("First daily mission should be completed after 1 session", firstMission.isCompleted)
        assertFalse("First daily mission should not be claimed yet", firstMission.claimed)

        val starsBeforeClaim = saveManager.activeProfile!!.stars
        val reward = learningEngine.claimDailyMission(firstMission.missionKey, nowUnix, utc)
        assertEquals(25, reward)
        assertEquals(starsBeforeClaim + 25, saveManager.activeProfile!!.stars)

        // Double-claiming same mission must return 0
        assertEquals(0, learningEngine.claimDailyMission(firstMission.missionKey, nowUnix, utc))

        // Equip wearable hat and verify persistence across SaveManager reload
        saveManager.updateActiveProfile {
            it.copy(
                ownedItems = (it.ownedItems + "hat_wizard").distinct(),
                equippedHat = "hat_wizard"
            )
        }
        val reloaded = SaveManager(sandboxDir).apply { loadAll() }
        assertEquals("hat_wizard", reloaded.activeProfile!!.equippedHat)
        assertTrue(reloaded.activeProfile!!.claimedMissionKeys.contains(firstMission.missionKey))

        // Verify PixelArtAssets sprites for wearable hats, new NPCs, and new rare pets render cleanly
        assertNotNull(PixelArtAssets.getCharacterSprite("avatar_0", Facing.DOWN, 0, hatId = "hat_wizard"))
        assertNotNull(PixelArtAssets.getCharacterSprite("npc_janitor", Facing.DOWN, 0))
        assertNotNull(PixelArtAssets.getCharacterSprite("npc_classmate", Facing.DOWN, 0))
        assertNotNull(PixelArtAssets.getHatIcon("hat_crown"))
        assertNotNull(PixelArtAssets.getCompanionSprite("pet_fox", 0))
        assertNotNull(PixelArtAssets.getCompanionSprite("pet_dragon", 1))
        // Sprite LPC e sfondi dipinti caricati dalle resources, con le dimensioni attese
        assertEquals(PixelArtAssets.CHARACTER_PX, PixelArtAssets.getCharacterSprite("npc_math", Facing.LEFT, 2).width)
        for (map in listOf(SchoolMaps.schoolYard, SchoolMaps.schoolHallway)) {
            val bg = PixelArtAssets.getMapBackground(map.id)
            assertNotNull("Sfondo mancante per ${map.id}", bg)
            val marginX = bg!!.width / PixelArtAssets.TILE_PX - map.width
            val marginY = bg.height / PixelArtAssets.TILE_PX - map.height
            assertTrue("Lo sfondo di ${map.id} deve coprire la griglia con un bordo uniforme", marginX > 0 && marginX == marginY)
        }

        // Subject mastery map covers all 6 subjects
        val masteryMap = learningEngine.getSubjectMasteryMap()
        assertEquals(6, masteryMap.size)
        assertNotNull(learningEngine.getOverallExplorerRank())

        // Verify Atrium interactive props & friendly NPCs are reachable via A* without blocking any door
        val hallway = SchoolMaps.schoolHallway
        val start = hallway.spawns["from_yard"]!!
        listOf("prop_trophy_case", "prop_missions").forEach { propId ->
            val prop = hallway.props.first { it.id == propId }
            val path = SchoolMaps.findPath4Dir(hallway, start, prop.pos, allowPartialPath = true)
            assertTrue("Atrium prop $propId must be reachable adjacent", path.isNotEmpty())
            val end = path.last()
            val dist = kotlin.math.abs(end.x - prop.pos.x) + kotlin.math.abs(end.y - prop.pos.y)
            assertTrue("Should reach adjacent to $propId (dist=$dist)", dist <= 1)
        }
        listOf("npc_janitor", "npc_classmate").forEach { npcId ->
            val npc = hallway.npcs.first { it.id == npcId }
            val path = SchoolMaps.findPath4Dir(hallway, start, npc.pos, allowPartialPath = true)
            assertTrue("Atrium NPC $npcId must be reachable", path.isNotEmpty())
            val end = path.last()
            val dist = kotlin.math.abs(end.x - npc.pos.x) + kotlin.math.abs(end.y - npc.pos.y)
            assertEquals("Should stop right next to $npcId", 1, dist)
        }
    }

    // Group 12: ID domanda unici tra pacchetti diversi (i progressi Leitner sono indicizzati per ID)
    @Test
    fun test12_questionIdsMustBeUniqueAcrossPacks() {
        fun packJson(packId: String, firstQuestionId: String): String {
            val questions = JSONArray()
            for (i in 0 until 5) {
                questions.put(
                    JSONObject()
                        .put("id", if (i == 0) firstQuestionId else "${packId}_q$i")
                        .put("type", "true_false")
                        .put("difficulty", 1)
                        .put("prompt", "Domanda di prova numero $i?")
                        .put("correct_bool", true)
                        .put("explanation", "Spiegazione di prova.")
                )
            }
            return JSONObject()
                .put("schema_version", 1).put("pack_id", packId).put("subject", "storia")
                .put("grade", 4).put("title", "Pacchetto $packId").put("version", 1)
                .put("boss_name", "Boss").put("boss_title", "Titolo")
                .put("medal_name", "Medaglia").put("medal_icon", "medal_history")
                .put("questions", questions)
                .toString()
        }

        // Import locale con ID già usato da un pacchetto incluso: rifiutato e non salvato
        val clash = packManager.importPackFromJsonText(packJson("storia_prova", "sto_egizi_001"), "prova.json")
        assertFalse(clash.isValid)
        assertTrue(clash.errors.any { it.contains("sto_egizi_001") && it.contains("storia_egizi") })
        assertFalse(File(packManager.localDir, "storia_prova.json").exists())

        // Ricaricare lo stesso pack_id (es. versione aggiornata) non è una collisione
        assertTrue(packManager.importPackFromJsonText(packJson("storia_egizi", "sto_egizi_001"), "egizi.json").isValid)

        // File in conflitto già presente su disco: scartato al caricamento con avviso
        File(packManager.localDir, "storia_zzz.json").writeText(packJson("storia_zzz", "sto_egizi_001"))
        packManager.reloadPacks()
        assertTrue(packManager.getPack("storia_zzz") == null)
        assertNotNull(packManager.getPack("storia_egizi"))
        assertTrue(packManager.loadWarnings.any { it.startsWith("storia_zzz") })

        // URL della cartella remota per leggere le impronte (SHA) dei file
        assertEquals(
            "https://api.github.com/repos/user/repo/contents/packs?ref=main",
            PackManager.buildGithubContentsUrl("user", "repo", "main", "packs", "")
        )
    }

    private fun locateProjectRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(4) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile ?: return dir
        }
        return File(System.getProperty("user.dir") ?: ".").absoluteFile
    }
}
