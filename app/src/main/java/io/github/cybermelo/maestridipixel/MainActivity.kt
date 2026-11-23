package io.github.cybermelo.maestridipixel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cybermelo.maestridipixel.core.GameBalance
import io.github.cybermelo.maestridipixel.core.LearningEngine
import io.github.cybermelo.maestridipixel.core.MinigameLaunchConfig
import io.github.cybermelo.maestridipixel.core.SfxType
import io.github.cybermelo.maestridipixel.ui.components.ConfirmModal
import io.github.cybermelo.maestridipixel.ui.components.DailyMissionsModal
import io.github.cybermelo.maestridipixel.ui.components.DialogBoxModal
import io.github.cybermelo.maestridipixel.ui.components.PackPickerModal
import io.github.cybermelo.maestridipixel.ui.components.PinModal
import io.github.cybermelo.maestridipixel.ui.components.QuickMapModal
import io.github.cybermelo.maestridipixel.ui.components.TrophyHallModal
import io.github.cybermelo.maestridipixel.ui.screens.BootScreen
import io.github.cybermelo.maestridipixel.ui.screens.EnglishMatchScreen
import io.github.cybermelo.maestridipixel.ui.screens.GeoBalloonScreen
import io.github.cybermelo.maestridipixel.ui.screens.GymRhythmScreen
import io.github.cybermelo.maestridipixel.ui.screens.HubScreen
import io.github.cybermelo.maestridipixel.ui.screens.LibraryBooksScreen
import io.github.cybermelo.maestridipixel.ui.screens.LockerRoomScreen
import io.github.cybermelo.maestridipixel.ui.screens.ParentAreaScreen
import io.github.cybermelo.maestridipixel.ui.screens.ProfileSelectScreen
import io.github.cybermelo.maestridipixel.ui.screens.ScienceCatapultScreen
import io.github.cybermelo.maestridipixel.ui.screens.YardBattleScreen
import io.github.cybermelo.maestridipixel.ui.theme.DeepBlueAccent
import io.github.cybermelo.maestridipixel.ui.theme.ScuolaRetroTheme
import io.github.cybermelo.maestridipixel.viewmodel.ActiveModal
import io.github.cybermelo.maestridipixel.viewmodel.AppScreen
import io.github.cybermelo.maestridipixel.viewmodel.ScuolaRetroViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ScuolaRetroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScuolaRetroApp(
                viewModel = viewModel,
                onExitApp = { finish() }
            )
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppPaused()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }
}

@Composable
fun ScuolaRetroApp(
    viewModel: ScuolaRetroViewModel,
    onExitApp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldExitApp) {
        if (uiState.shouldExitApp) {
            onExitApp()
        }
    }

    ScuolaRetroTheme(darkTheme = uiState.config.studyPreferences.darkMode) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = uiState.screen) {
            AppScreen.Boot -> BootScreen()
            AppScreen.ProfileSelect -> ProfileSelectScreen(
                onCreateProfile = { name, avatarId ->
                    viewModel.createProfileAndEnterHub(name, avatarId)
                }
            )
            is AppScreen.Hub -> HubScreen(
                hubId = screen.hubId,
                spawnId = screen.spawnId,
                uiState = uiState,
                onNpcInteract = { viewModel.onNpcInteract(it) },
                onPortalTrigger = { viewModel.onPortalTrigger(it) },
                onGuidanceClick = { viewModel.onGuidancePillClicked() },
                onOpenQuickMap = { viewModel.openQuickMap() },
                onOpenDailyMissions = { viewModel.openDailyMissions() },
                onOpenTrophyHall = { viewModel.openTrophyHall() },
                onPetTap = { viewModel.onPetInteracted() },
                onToggleMute = { viewModel.toggleMute() },
                onOpenParentGate = { viewModel.openParentPinGate() },
                onPlayStepSfx = { viewModel.audioManager.playSfx(SfxType.STEP) },
                onBack = { viewModel.handleGlobalBack() }
            )
            is AppScreen.YardBattle -> YardBattleScreen(
                config = screen.config,
                viewModel = viewModel
            )
            is AppScreen.GymRhythm -> GymRhythmScreen(
                config = screen.config,
                viewModel = viewModel
            )
            is AppScreen.ScienceCatapult -> ScienceCatapultScreen(
                config = screen.config,
                viewModel = viewModel
            )
            is AppScreen.LibraryBooks -> LibraryBooksScreen(
                config = screen.config,
                viewModel = viewModel
            )
            is AppScreen.GeoBalloon -> GeoBalloonScreen(
                config = screen.config,
                viewModel = viewModel
            )
            is AppScreen.EnglishMatch -> EnglishMatchScreen(
                config = screen.config,
                viewModel = viewModel
            )
            AppScreen.LockerRoom -> LockerRoomScreen(
                uiState = uiState,
                viewModel = viewModel
            )
            AppScreen.ParentArea -> ParentAreaScreen(
                uiState = uiState,
                viewModel = viewModel
            )
        }

        // Global Modals
        when (val modal = uiState.activeModal) {
            ActiveModal.None -> {}
            is ActiveModal.Dialog -> DialogBoxModal(
                title = modal.title,
                message = modal.message,
                speakerSpriteId = modal.speakerSpriteId,
                onDismiss = { viewModel.dismissModal() }
            )
            is ActiveModal.Confirm -> ConfirmModal(
                title = modal.title,
                message = modal.message,
                confirmLabel = modal.confirmLabel,
                onConfirm = {
                    val action = modal.onConfirm
                    viewModel.dismissModal()
                    action()
                },
                onCancel = { viewModel.dismissModal() }
            )
            is ActiveModal.PackPicker -> PackPickerModal(
                title = modal.title,
                subtitle = modal.subtitle,
                packs = modal.packs,
                masteryProvider = { viewModel.learningEngine.getPackMasteryPercent(it) },
                hasMedalProvider = { viewModel.learningEngine.hasMedal(it) },
                focusTopicsProvider = { uiState.config.focusTopics[it].orEmpty() },
                onSelectPack = { pack, isBoss ->
                    val resolvedMinigame = if (modal.minigameId == "auto") {
                        LearningEngine.minigameIdForSubject(pack.subject)
                    } else {
                        modal.minigameId
                    }
                    viewModel.launchMinigame(
                        MinigameLaunchConfig(
                            packId = pack.packId,
                            minigameId = resolvedMinigame,
                            isBoss = isBoss,
                            returnScene = modal.returnScene,
                            returnSpawnId = modal.returnSpawnId
                        )
                    )
                },
                onDismiss = { viewModel.dismissModal() }
            )
            is ActiveModal.PinGate -> PinModal(
                mode = modal.mode,
                showDefaultPinHint = uiState.config.parentPin == GameBalance.DEFAULT_PARENT_PIN,
                onVerifyPin = { viewModel.verifyParentPin(it) },
                onSetNewPin = { viewModel.updateParentPin(it) },
                onDismiss = { viewModel.dismissModal() }
            )
            ActiveModal.TrophyHall -> TrophyHallModal(
                uiState = uiState,
                onDismiss = { viewModel.dismissModal() }
            )
            ActiveModal.DailyMissions -> DailyMissionsModal(
                missions = uiState.dailyMissions,
                onClaim = { viewModel.claimDailyMission(it) },
                onDismiss = { viewModel.dismissModal() }
            )
            ActiveModal.QuickMap -> QuickMapModal(
                uiState = uiState,
                currentHubId = (uiState.screen as? AppScreen.Hub)?.hubId ?: "school_hallway",
                onSelectAction = { viewModel.onPortalTrigger(it) },
                onDismiss = { viewModel.dismissModal() }
            )
        }

        // Scene transition fade overlay
        AnimatedVisibility(
            visible = uiState.isTransitioning,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
            )
        }

        // Toast Banner
        AnimatedVisibility(
            visible = uiState.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 22.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DeepBlueAccent,
                shadowElevation = 6.dp
            ) {
                Text(
                    text = uiState.toastMessage.orEmpty(),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
    }
    }
}
