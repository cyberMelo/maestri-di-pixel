package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppConfig
import com.example.core.AudioManager
import com.example.core.BundledAssetReader
import com.example.core.DailyMissionItem
import com.example.core.GameBalance
import com.example.core.LearningEngine
import com.example.core.MasteryRankInfo
import com.example.core.MinigameLaunchConfig
import com.example.core.MinigameResult
import com.example.core.MusicTrack
import com.example.core.PackData
import com.example.core.PackManager
import com.example.core.ProfileData
import com.example.core.ProfileSummary
import com.example.core.RecommendedActivity
import com.example.core.SaveManager
import com.example.core.SfxType
import com.example.core.ShopItem
import com.example.core.StudyPreferences
import com.example.core.SubjectMasterySummary
import com.example.core.SyncResult
import com.example.core.TTSManager
import com.example.core.ValidationResult
import com.example.core.WeakQuestionItem
import com.example.pixelart.HubNpc
import com.example.pixelart.PortalAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface AppScreen {
    data object Boot : AppScreen
    data object ProfileSelect : AppScreen
    data class Hub(val hubId: String, val spawnId: String) : AppScreen
    data class YardBattle(val config: MinigameLaunchConfig) : AppScreen
    data class GymRhythm(val config: MinigameLaunchConfig) : AppScreen
    data class ScienceCatapult(val config: MinigameLaunchConfig) : AppScreen
    data class LibraryBooks(val config: MinigameLaunchConfig) : AppScreen
    data class GeoBalloon(val config: MinigameLaunchConfig) : AppScreen
    data class EnglishMatch(val config: MinigameLaunchConfig) : AppScreen
    data object LockerRoom : AppScreen
    data object ParentArea : AppScreen
}

enum class PinMode {
    ENTER_PARENT_AREA,
    CHANGE_PIN
}

sealed interface ActiveModal {
    data object None : ActiveModal
    data class Dialog(
        val title: String,
        val message: String,
        val speakerSpriteId: String? = null
    ) : ActiveModal

    data class Confirm(
        val title: String,
        val message: String,
        val confirmLabel: String = "Conferma",
        val onConfirm: () -> Unit
    ) : ActiveModal

    data class PackPicker(
        val title: String,
        val subtitle: String,
        val packs: List<PackData>,
        val minigameId: String,
        val returnScene: String,
        val returnSpawnId: String
    ) : ActiveModal

    data class PinGate(val mode: PinMode) : ActiveModal
    data object TrophyHall : ActiveModal
    data object DailyMissions : ActiveModal
    data object QuickMap : ActiveModal
}

data class AppUiState(
    val screen: AppScreen = AppScreen.Boot,
    val isTransitioning: Boolean = false,
    val config: AppConfig = AppConfig(),
    val activeProfile: ProfileData? = null,
    val profiles: List<ProfileSummary> = emptyList(),
    val activePacks: List<PackData> = emptyList(),
    val allPacksIncludingArchived: List<PackData> = emptyList(),
    val shopCatalog: List<ShopItem> = emptyList(),
    val recommendedActivity: RecommendedActivity? = null,
    val sessionsThisWeek: Int = 0,
    val weakQuestions: List<WeakQuestionItem> = emptyList(),
    val dailyMissions: List<DailyMissionItem> = emptyList(),
    val subjectMastery: Map<String, SubjectMasterySummary> = emptyMap(),
    val explorerRank: MasteryRankInfo = MasteryRankInfo("Curioso", 0, 0),
    val isSyncing: Boolean = false,
    val activeModal: ActiveModal = ActiveModal.None,
    val toastMessage: String? = null,
    val shouldExitApp: Boolean = false
)

class ScuolaRetroViewModel(application: Application) : AndroidViewModel(application) {
    private val dataRoot = File(application.filesDir, "scuola_retro_data")
    val saveManager = SaveManager(dataRoot)
    private val assetReader = BundledAssetReader { path ->
        try {
            application.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }
    val packManager = PackManager(dataRoot, saveManager, assetReader)
    val learningEngine = LearningEngine(saveManager, packManager)
    val audioManager = AudioManager(saveManager)
    val ttsManager = TTSManager(application.applicationContext, audioManager)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var shopPurchaseLocked = false
    private var loadedShopCatalog: List<ShopItem> = emptyList()

    init {
        bootApplication()
    }

    private fun bootApplication() {
        viewModelScope.launch {
            saveManager.loadAll()
            packManager.reloadPacks()
            val shopJson = assetReader.readAssetText("data/shop_catalog.json")
            loadedShopCatalog = if (shopJson != null) {
                try {
                    SaveManager.parseShopCatalog(shopJson)
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            refreshSnapshot()

            // Il repository dei pacchetti è fisso nell'app: a ogni avvio scarica in background
            // i pacchetti nuovi o modificati (offline usa la cache senza disturbare)
            launchBackgroundSync()

            delay(350)
            if (saveManager.activeProfile != null) {
                navigateTo(AppScreen.Hub("school_yard", "default"), playFade = false)
            } else {
                navigateTo(AppScreen.ProfileSelect, playFade = false)
            }
        }
    }

    fun refreshSnapshot() {
        _uiState.update { state ->
            state.copy(
                config = saveManager.config,
                activeProfile = saveManager.activeProfile,
                profiles = saveManager.listProfiles(),
                activePacks = packManager.getActivePacks(),
                allPacksIncludingArchived = packManager.getAllPacks(includeArchived = true),
                shopCatalog = loadedShopCatalog,
                recommendedActivity = learningEngine.getRecommendedActivity(),
                sessionsThisWeek = learningEngine.getSessionsThisWeek(),
                weakQuestions = learningEngine.getTopWeakQuestions(5),
                dailyMissions = learningEngine.getDailyMissions(),
                subjectMastery = learningEngine.getSubjectMasteryMap(),
                explorerRank = learningEngine.getOverallExplorerRank(),
                isSyncing = packManager.isSyncing
            )
        }
    }

    fun navigateTo(target: AppScreen, playFade: Boolean = true) {
        if (_uiState.value.isTransitioning) return
        ttsManager.stop()
        dismissModal()
        viewModelScope.launch {
            if (playFade) {
                _uiState.update { it.copy(isTransitioning = true) }
                delay(GameBalance.SCENE_TRANSITION_MS / 2)
            }
            _uiState.update { it.copy(screen = target) }
            updateMusicForScreen(target)
            refreshSnapshot()
            if (playFade) {
                delay(GameBalance.SCENE_TRANSITION_MS / 2)
                _uiState.update { it.copy(isTransitioning = false) }
            }
        }
    }

    private fun updateMusicForScreen(screen: AppScreen) {
        when (screen) {
            is AppScreen.Hub, AppScreen.LockerRoom -> audioManager.playMusic(MusicTrack.HUB_THEME)
            is AppScreen.YardBattle,
            is AppScreen.GymRhythm,
            is AppScreen.ScienceCatapult,
            is AppScreen.LibraryBooks,
            is AppScreen.GeoBalloon,
            is AppScreen.EnglishMatch -> audioManager.playMusic(MusicTrack.BATTLE_THEME)
            AppScreen.Boot, AppScreen.ProfileSelect, AppScreen.ParentArea -> audioManager.stopMusic()
        }
    }

    fun createProfileAndEnterHub(rawName: String, avatarId: Int) {
        val clean = rawName.trim()
        if (clean.length !in 1..16) return
        audioManager.playSfx(SfxType.VICTORY_FANFARE)
        saveManager.createProfile(clean, avatarId)
        learningEngine.resetRunSessionCounter()
        refreshSnapshot()
        navigateTo(AppScreen.Hub("school_yard", "default"))
    }

    fun createAdditionalProfile(rawName: String, avatarId: Int): Boolean {
        val clean = rawName.trim()
        if (clean.length !in 1..16) return false
        saveManager.createProfile(clean, avatarId)
        learningEngine.resetRunSessionCounter()
        refreshSnapshot()
        showToast("Nuovo profilo '$clean' creato e attivato!")
        return true
    }

    fun switchProfile(profileId: String) {
        if (saveManager.switchProfile(profileId)) {
            learningEngine.resetRunSessionCounter()
            refreshSnapshot()
            showToast("Profilo cambiato!")
        }
    }

    fun resetActiveProfileProgress() {
        saveManager.resetActiveProfileProgress()
        learningEngine.resetRunSessionCounter()
        refreshSnapshot()
        showToast("Progressi azzerati per il profilo attivo.")
    }

    fun toggleMute() {
        audioManager.toggleMute()
        refreshSnapshot()
    }

    fun setAudioVolumes(musicVol: Float, sfxVol: Float) {
        audioManager.setVolumes(musicVol, sfxVol)
        refreshSnapshot()
    }

    fun showModal(modal: ActiveModal) {
        ttsManager.stop()
        _uiState.update { it.copy(activeModal = modal) }
    }

    fun dismissModal() {
        ttsManager.stop()
        _uiState.update { it.copy(activeModal = ActiveModal.None) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
        viewModelScope.launch {
            delay(3200)
            _uiState.update { current ->
                if (current.toastMessage == message) current.copy(toastMessage = null) else current
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    // Hub Interactions
    fun onNpcInteract(npc: HubNpc) {
        if (_uiState.value.isTransitioning || _uiState.value.activeModal != ActiveModal.None) return
        audioManager.playSfx(SfxType.UI_CLICK)

        if (npc.subjectFilter == "janitor") {
            val profile = saveManager.activeProfile
            val claimable = learningEngine.getDailyMissions().count { it.isClaimable }
            val msg = when {
                claimable > 0 -> "Ehi ${profile?.name ?: "campione"}! Hai $claimable premi pronti sulla Bacheca Missioni! Toccala o apri il pulsante Missioni in alto!"
                (profile?.medals?.size ?: 0) == 0 -> "Ciao! Ho appena lucidato la Teca dei Trofei qui al centro. Toccando la teca vedrai tutte le medaglie che puoi conquistare!"
                else -> "L'Atrio brilla oggi! Hai già **${profile?.medals?.size ?: 0} medaglie** e **${profile?.stars ?: 0} stelle**. Continua così!"
            }
            showModal(
                ActiveModal.Dialog(
                    title = "Bidello Pippo",
                    message = msg,
                    speakerSpriteId = npc.spriteId
                )
            )
            return
        }

        if (npc.subjectFilter == "classmate") {
            val rec = learningEngine.getRecommendedActivity()
            val rank = learningEngine.getOverallExplorerRank()
            val msg = if (rec != null) {
                "Ciao! Sei già di grado **${rank.title}**! Io oggi provo **${rec.title}** (${rec.targetLocationLabel}). Usa anche la **Mappa** in alto per volare subito in aula!"
            } else {
                "Ciao! Hai visto i nuovi cappelli e mascotte nell'Armadietto? Puoi indossarli mentre giri per la scuola!"
            }
            showModal(
                ActiveModal.Dialog(
                    title = "Compagna Gaia",
                    message = msg,
                    speakerSpriteId = npc.spriteId
                )
            )
            return
        }

        val yardTypes = setOf("multiple_choice", "true_false")
        val allActive = packManager.getActivePacks()

        if (npc.subjectFilter == "preside") {
            // Preside Stella 3-tier Day-1 rule (zero dead ends)
            val nonMathCompatible = allActive.filter { p ->
                p.subject != "matematica" && p.playableQuestions.any { it.type in yardTypes }
            }
            val tier1 = nonMathCompatible.filter { p ->
                p.priority || p.subject in setOf("geografia", "italiano", "inglese")
            }
            if (tier1.isNotEmpty()) {
                showModal(
                    ActiveModal.PackPicker(
                        title = "Preside Stella",
                        subtitle = "Sfide in evidenza e nuove materie",
                        packs = tier1,
                        minigameId = "auto",
                        returnScene = "school_yard",
                        returnSpawnId = npc.returnSpawnId
                    )
                )
                return
            }
            val tier2 = nonMathCompatible.filter { p ->
                learningEngine.isBossUnlocked(p.packId) && !learningEngine.hasMedal(p.packId)
            }
            if (tier2.isNotEmpty()) {
                showModal(
                    ActiveModal.PackPicker(
                        title = "Sfida del Preside",
                        subtitle = "Sei pronto a conquistare una Medaglia Boss!",
                        packs = tier2,
                        minigameId = "auto",
                        returnScene = "school_yard",
                        returnSpawnId = npc.returnSpawnId
                    )
                )
                return
            }
            showModal(
                ActiveModal.Dialog(
                    title = "Preside Stella",
                    message = "Benvenuto! Quando ci sarà una verifica importante o arriveranno nuove materie come Geografia e Italiano, potrai allenarti qui con me. Intanto prova a parlare con Maestro Erodoto o Maestra Flora!",
                    speakerSpriteId = npc.spriteId
                )
            )
            return
        }

        val matching = allActive.filter { p ->
            p.subject == npc.subjectFilter && p.playableQuestions.any { it.type in yardTypes }
        }
        if (matching.isEmpty()) {
            showModal(
                ActiveModal.Dialog(
                    title = npc.name,
                    message = "Al momento non ci sono argomenti attivi per questa materia. Torna presto!",
                    speakerSpriteId = npc.spriteId
                )
            )
        } else {
            val targetMinigame = if (npc.subjectFilter == "scienze") "science_catapult" else "yard_battle"
            val subtitleText = if (npc.subjectFilter == "scienze") {
                "Lancia la Catapulta di Scienze!"
            } else {
                "Scegli l'argomento di ${GameBalance.subjectLabel(npc.subjectFilter)}"
            }
            showModal(
                ActiveModal.PackPicker(
                    title = npc.name,
                    subtitle = subtitleText,
                    packs = matching,
                    minigameId = targetMinigame,
                    returnScene = "school_yard",
                    returnSpawnId = npc.returnSpawnId
                )
            )
        }
    }

    fun onPortalTrigger(action: PortalAction) {
        if (_uiState.value.isTransitioning || _uiState.value.activeModal != ActiveModal.None) return
        when (action) {
            is PortalAction.GoToHub -> {
                audioManager.playSfx(SfxType.DOOR_OPEN)
                navigateTo(AppScreen.Hub(action.targetHubId, action.targetSpawnId))
            }
            PortalAction.OpenHistoryPicker -> {
                audioManager.playSfx(SfxType.DOOR_OPEN)
                val types = setOf("multiple_choice", "true_false")
                val historyPacks = packManager.getActivePacks().filter { p ->
                    p.subject == "storia" && p.playableQuestions.any { it.type in types }
                }
                if (historyPacks.isEmpty()) {
                    showModal(
                        ActiveModal.Dialog(
                            title = "Aula di Storia",
                            message = "Non ci sono ancora capitoli di Storia pronti in aula!",
                            speakerSpriteId = "npc_history"
                        )
                    )
                } else {
                    showModal(
                        ActiveModal.PackPicker(
                            title = "Aula di Storia · Maestro Erodoto",
                            subtitle = "Sfida di Storia in stile GBA!",
                            packs = historyPacks,
                            minigameId = "yard_battle",
                            returnScene = "school_hallway",
                            returnSpawnId = "from_history"
                        )
                    )
                }
            }
            PortalAction.OpenGymPicker -> {
                audioManager.playSfx(SfxType.DOOR_OPEN)
                val gymTypes = setOf("numeric_input", "multiple_choice", "true_false")
                val mathPacks = packManager.getActivePacks().filter { p ->
                    p.subject == "matematica" && p.playableQuestions.any { it.type in gymTypes }
                }
                if (mathPacks.isEmpty()) {
                    showModal(
                        ActiveModal.Dialog(
                            title = "Palestra di Matematica",
                            message = "Non ci sono ancora sfide di Matematica pronte in Palestra!"
                        )
                    )
                } else {
                    showModal(
                        ActiveModal.PackPicker(
                            title = "Palestra Spaziale",
                            subtitle = "Space Invaders di Tabelline e Calcolo!",
                            packs = mathPacks,
                            minigameId = "gym_rhythm",
                            returnScene = "school_hallway",
                            returnSpawnId = "from_gym"
                        )
                    )
                }
            }
            PortalAction.OpenScienceLabPicker -> {
                audioManager.playSfx(SfxType.DOOR_OPEN)
                val sciTypes = setOf("multiple_choice", "true_false")
                val sciPacks = packManager.getActivePacks().filter { p ->
                    p.subject == "scienze" && p.playableQuestions.any { it.type in sciTypes }
                }
                if (sciPacks.isEmpty()) {
                    showModal(
                        ActiveModal.Dialog(
                            title = "Laboratorio di Scienze",
                            message = "Non ci sono ancora esperimenti di Scienze pronti!"
                        )
                    )
                } else {
                    showModal(
                        ActiveModal.PackPicker(
                            title = "Laboratorio di Scienze",
                            subtitle = "Catapulta a parabola sui bersagli giusti!",
                            packs = sciPacks,
                            minigameId = "science_catapult",
                            returnScene = "school_hallway",
                            returnSpawnId = "from_lab"
                        )
                    )
                }
            }
            PortalAction.OpenLibraryPicker -> {
                audioManager.playSfx(SfxType.DOOR_OPEN)
                val types = setOf("multiple_choice", "true_false")
                val itaPacks = packManager.getActivePacks().filter { p ->
                    p.subject == "italiano" && p.playableQuestions.any { it.type in types }
                }
                if (itaPacks.isEmpty()) {
                    showModal(
                        ActiveModal.Dialog(
                            title = "Biblioteca di Italiano",
                            message = "Non ci sono ancora libri pronti in Biblioteca!",
                            speakerSpriteId = "npc_library"
                        )
                    )
                } else {
                    showModal(
                        ActiveModal.PackPicker(
                            title = "Biblioteca · Bibliotecaria Sofia",
                            subtitle = "Caccia ai Libri Volanti di Italiano!",
                            packs = itaPacks,
                            minigameId = "library_books",
                            returnScene = "school_hallway",
                            returnSpawnId = "from_library"
                        )
                    )
                }
            }
            PortalAction.OpenGeoPicker -> {
                audioManager.playSfx(SfxType.DOOR_OPEN)
                val types = setOf("multiple_choice", "true_false")
                val geoPacks = packManager.getActivePacks().filter { p ->
                    p.subject == "geografia" && p.playableQuestions.any { it.type in types }
                }
                if (geoPacks.isEmpty()) {
                    showModal(
                        ActiveModal.Dialog(
                            title = "Aula di Geografia",
                            message = "Non ci sono ancora mappe pronte per volare!",
                            speakerSpriteId = "npc_geo"
                        )
                    )
                } else {
                    showModal(
                        ActiveModal.PackPicker(
                            title = "Aula di Geografia · Esploratore Marco",
                            subtitle = "Volo in Mongolfiera sui cieli d'Italia!",
                            packs = geoPacks,
                            minigameId = "geo_balloon",
                            returnScene = "school_hallway",
                            returnSpawnId = "from_geo"
                        )
                    )
                }
            }
            PortalAction.OpenEnglishPicker -> {
                audioManager.playSfx(SfxType.DOOR_OPEN)
                val types = setOf("multiple_choice", "true_false")
                val engPacks = packManager.getActivePacks().filter { p ->
                    p.subject == "inglese" && p.playableQuestions.any { it.type in types }
                }
                if (engPacks.isEmpty()) {
                    showModal(
                        ActiveModal.Dialog(
                            title = "Aula di Inglese",
                            message = "Non ci sono ancora sfide di Inglese pronte!",
                            speakerSpriteId = "npc_english"
                        )
                    )
                } else {
                    showModal(
                        ActiveModal.PackPicker(
                            title = "Aula di Inglese · Teacher Emma",
                            subtitle = "Abbinamento Lampo con pronuncia EN!",
                            packs = engPacks,
                            minigameId = "english_match",
                            returnScene = "school_hallway",
                            returnSpawnId = "from_english"
                        )
                    )
                }
            }
            PortalAction.OpenLocker -> {
                audioManager.playSfx(SfxType.DOOR_OPEN)
                navigateTo(AppScreen.LockerRoom)
            }
            PortalAction.OpenTrophyHall -> {
                openTrophyHall()
            }
            PortalAction.OpenDailyMissions -> {
                openDailyMissions()
            }
            is PortalAction.WipDoor -> {
                audioManager.playSfx(SfxType.UI_CLICK)
                showModal(
                    ActiveModal.Dialog(
                        title = action.roomName,
                        message = "Lavori in corso — Presto disponibile!"
                    )
                )
            }
        }
    }

    fun openTrophyHall() {
        if (_uiState.value.isTransitioning) return
        audioManager.playSfx(SfxType.UI_CLICK)
        refreshSnapshot()
        showModal(ActiveModal.TrophyHall)
    }

    fun openDailyMissions() {
        if (_uiState.value.isTransitioning) return
        audioManager.playSfx(SfxType.UI_CLICK)
        refreshSnapshot()
        showModal(ActiveModal.DailyMissions)
    }

    fun openQuickMap() {
        if (_uiState.value.isTransitioning) return
        audioManager.playSfx(SfxType.UI_CLICK)
        refreshSnapshot()
        showModal(ActiveModal.QuickMap)
    }

    fun claimDailyMission(missionKey: String) {
        val reward = learningEngine.claimDailyMission(missionKey)
        if (reward > 0) {
            audioManager.playSfx(SfxType.VICTORY_FANFARE)
            refreshSnapshot()
            showToast("Missione completata! +$reward stelle!")
        }
    }

    fun onPetInteracted() {
        audioManager.playSfx(SfxType.STAR_GAIN)
    }

    fun onGuidancePillClicked() {
        if (_uiState.value.isTransitioning || _uiState.value.activeModal != ActiveModal.None) return
        val rec = _uiState.value.recommendedActivity ?: return
        audioManager.playSfx(SfxType.UI_CLICK)
        val modeLabel = if (rec.isBoss) "Sfida Boss: ${rec.title}" else rec.title
        showModal(
            ActiveModal.Confirm(
                title = "Sfida consigliata",
                message = "Vuoi iniziare subito $modeLabel?",
                confirmLabel = "Inizia subito",
                onConfirm = {
                    val returnScene = when (rec.minigameId) {
                        "gym_rhythm", "library_books", "geo_balloon", "english_match" -> "school_hallway"
                        "science_catapult" -> "school_yard"
                        else -> "school_yard"
                    }
                    val returnSpawn = when (rec.minigameId) {
                        "gym_rhythm" -> "from_gym"
                        "library_books" -> "from_library"
                        "geo_balloon" -> "from_geo"
                        "english_match" -> "from_english"
                        "science_catapult" -> "from_npc_science"
                        else -> "default"
                    }
                    launchMinigame(
                        MinigameLaunchConfig(
                            packId = rec.packId,
                            minigameId = rec.minigameId,
                            isBoss = rec.isBoss,
                            returnScene = returnScene,
                            returnSpawnId = returnSpawn
                        )
                    )
                }
            )
        )
    }

    fun launchMinigame(config: MinigameLaunchConfig) {
        audioManager.playSfx(SfxType.ENCOUNTER_START)
        val target = when (config.minigameId) {
            "gym_rhythm" -> AppScreen.GymRhythm(config)
            "science_catapult" -> AppScreen.ScienceCatapult(config)
            "library_books" -> AppScreen.LibraryBooks(config)
            "geo_balloon" -> AppScreen.GeoBalloon(config)
            "english_match" -> AppScreen.EnglishMatch(config)
            else -> AppScreen.YardBattle(config)
        }
        navigateTo(target)
    }

    fun returnFromMinigame(config: MinigameLaunchConfig) {
        navigateTo(AppScreen.Hub(config.returnScene, config.returnSpawnId))
    }

    // Locker & Shop
    fun purchaseShopItem(item: ShopItem): Boolean {
        if (shopPurchaseLocked) return false
        shopPurchaseLocked = true
        try {
            val prof = saveManager.activeProfile ?: return false
            if (item.id in prof.ownedItems) return false
            if (prof.stars < item.price) {
                showToast("Servono ${item.price} stelle! Fai un'altra sfida per raccoglierle.")
                return false
            }
            audioManager.playSfx(SfxType.STAR_GAIN)
            saveManager.updateActiveProfile { p ->
                val newOwned = (p.ownedItems + item.id).distinct()
                val newCompanion = if (item.category == "companion" && p.equippedCompanion.isBlank()) item.id else p.equippedCompanion
                val newHat = if (item.category == "hat") item.id else p.equippedHat
                val newFrame = if (item.category == "frame") item.id else p.equippedFrame
                p.copy(
                    stars = p.stars - item.price,
                    ownedItems = newOwned,
                    equippedCompanion = newCompanion,
                    equippedHat = newHat,
                    equippedFrame = newFrame
                )
            }
            refreshSnapshot()
            showToast("Hai sbloccato ${item.name}!")
            return true
        } finally {
            viewModelScope.launch {
                delay(220)
                shopPurchaseLocked = false
            }
        }
    }

    fun equipCompanion(companionId: String) {
        audioManager.playSfx(SfxType.UI_CLICK)
        saveManager.updateActiveProfile { it.copy(equippedCompanion = companionId) }
        refreshSnapshot()
    }

    fun equipHat(hatId: String) {
        audioManager.playSfx(SfxType.UI_CLICK)
        saveManager.updateActiveProfile { it.copy(equippedHat = hatId) }
        refreshSnapshot()
    }

    fun equipFrame(frameId: String) {
        audioManager.playSfx(SfxType.UI_CLICK)
        saveManager.updateActiveProfile { it.copy(equippedFrame = frameId) }
        refreshSnapshot()
    }

    fun setLockerSticker(slotIndex: Int, stickerId: String) {
        if (slotIndex !in 0..5) return
        audioManager.playSfx(SfxType.UI_CLICK)
        saveManager.updateActiveProfile { p ->
            val mutable = p.lockerStickers.toMutableList()
            while (mutable.size < 6) mutable.add("")
            mutable[slotIndex] = stickerId
            p.copy(lockerStickers = mutable)
        }
        refreshSnapshot()
    }

    // Parent Area
    fun openParentPinGate() {
        if (_uiState.value.isTransitioning) return
        audioManager.playSfx(SfxType.UI_CLICK)
        showModal(ActiveModal.PinGate(PinMode.ENTER_PARENT_AREA))
    }

    fun openChangePinGate() {
        audioManager.playSfx(SfxType.UI_CLICK)
        showModal(ActiveModal.PinGate(PinMode.CHANGE_PIN))
    }

    fun verifyParentPin(enteredPin: String): Boolean {
        val ok = enteredPin == saveManager.config.parentPin
        if (ok) {
            audioManager.playSfx(SfxType.ANSWER_CORRECT)
            dismissModal()
            navigateTo(AppScreen.ParentArea)
        } else {
            audioManager.playSfx(SfxType.ANSWER_WRONG)
        }
        return ok
    }

    fun updateParentPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            saveManager.updateConfig { it.copy(parentPin = newPin) }
            refreshSnapshot()
            dismissModal()
            showToast("Nuovo PIN salvato con successo!")
        }
    }

    fun activateStudioSerenoPreset() {
        audioManager.playSfx(SfxType.ANSWER_CORRECT)
        saveManager.updateConfig { cfg ->
            cfg.copy(
                studyPreferences = cfg.studyPreferences.copy(
                    calmMode = true,
                    reducedMotion = true,
                    readingPauseSec = 1.0f,
                    autoTtsQuestions = true,
                    sessionLength = GameBalance.STUDIO_SERENO_SESSION_QUESTIONS,
                    showGuidancePill = true
                    // enable5050 is intentionally left unchanged per specification
                )
            )
        }
        refreshSnapshot()
        showToast("Profilo Studio Sereno attivato! Puoi regolare ogni opzione qui sotto.")
    }

    fun updateStudyPreferences(transform: (StudyPreferences) -> StudyPreferences) {
        saveManager.updateConfig { cfg ->
            cfg.copy(studyPreferences = transform(cfg.studyPreferences))
        }
        refreshSnapshot()
    }

    fun togglePackPriority(packId: String, enabled: Boolean) {
        saveManager.updateConfig { cfg ->
            val map = cfg.priorityOverrides.toMutableMap()
            map[packId] = enabled
            cfg.copy(priorityOverrides = map)
        }
        refreshSnapshot()
    }

    fun togglePackFocusTopic(packId: String, topic: String) {
        val clean = topic.trim()
        if (clean.isEmpty()) return
        saveManager.updateConfig { cfg ->
            val map = cfg.focusTopics.toMutableMap()
            val current = map[packId].orEmpty().toMutableList()
            if (clean in current) {
                current.remove(clean)
            } else {
                current.add(clean)
            }
            if (current.isEmpty()) {
                map.remove(packId)
            } else {
                map[packId] = current
            }
            cfg.copy(focusTopics = map)
        }
        refreshSnapshot()
    }

    fun clearPackFocusTopics(packId: String) {
        saveManager.updateConfig { cfg ->
            val map = cfg.focusTopics.toMutableMap()
            map.remove(packId)
            cfg.copy(focusTopics = map)
        }
        refreshSnapshot()
    }

    fun importPackFromClipboardText(rawText: String): ValidationResult {
        val res = packManager.importPackFromJsonText(rawText, "appunti.json")
        if (res.isValid) {
            audioManager.playSfx(SfxType.ANSWER_CORRECT)
            refreshSnapshot()
            showToast("Pacchetto importato con successo!")
        } else {
            audioManager.playSfx(SfxType.ANSWER_WRONG)
        }
        return res
    }

    fun importPackFromUriText(rawText: String, fileName: String): ValidationResult {
        val res = packManager.importPackFromJsonText(rawText, fileName)
        if (res.isValid) {
            audioManager.playSfx(SfxType.ANSWER_CORRECT)
            refreshSnapshot()
            showToast("Pacchetto '$fileName' importato!")
        } else {
            audioManager.playSfx(SfxType.ANSWER_WRONG)
        }
        return res
    }

    fun deleteLocalPack(packId: String) {
        if (packManager.deleteLocalPack(packId)) {
            refreshSnapshot()
            showToast("Pacchetto locale rimosso.")
        }
    }

    fun testLockedGithubConnection(onResult: (SyncResult) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = packManager.testGithubConnection()
            refreshSnapshot()
            onResult(result)
        }
    }

    fun syncNow(onDone: ((SyncResult) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = packManager.syncFromGithub()
            refreshSnapshot()
            onDone?.invoke(result)
        }
    }

    private fun launchBackgroundSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            packManager.syncFromGithub()
            refreshSnapshot()
        }
    }

    // Global Back handling
    fun handleGlobalBack() {
        ttsManager.stop()
        val state = _uiState.value
        if (state.activeModal != ActiveModal.None) {
            audioManager.playSfx(SfxType.UI_BACK)
            dismissModal()
            return
        }
        when (state.screen) {
            AppScreen.LockerRoom -> {
                audioManager.playSfx(SfxType.UI_BACK)
                navigateTo(AppScreen.Hub("school_hallway", "from_locker"))
            }
            AppScreen.ParentArea -> {
                audioManager.playSfx(SfxType.UI_BACK)
                navigateTo(AppScreen.Hub("school_yard", "default"))
            }
            is AppScreen.Hub -> {
                audioManager.playSfx(SfxType.UI_BACK)
                showModal(
                    ActiveModal.Confirm(
                        title = "Vuoi uscire da Maestri di Pixel?",
                        message = "Tutti i tuoi progressi e le tue stelle sono già al sicuro.",
                        confirmLabel = "Esci",
                        onConfirm = {
                            saveManager.flushAll()
                            _uiState.update { it.copy(shouldExitApp = true) }
                        }
                    )
                )
            }
            else -> {
                // Mini-games handle their own BackHandler confirmation modal
            }
        }
    }

    fun onAppPaused() {
        ttsManager.stop()
        audioManager.pause()
        saveManager.flushAll()
    }

    fun onAppResumed() {
        audioManager.resume()
    }

    override fun onCleared() {
        ttsManager.shutdown()
        audioManager.release()
        saveManager.flushAll()
        super.onCleared()
    }
}
