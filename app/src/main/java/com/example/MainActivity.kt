package com.example

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
import com.example.core.GameBalance
import com.example.core.LearningEngine
import com.example.core.MinigameLaunchConfig
import com.example.core.SfxType
import com.example.ui.components.ConfirmModal
import com.example.ui.components.DialogBoxModal
import com.example.ui.components.PackPickerModal
import com.example.ui.components.PinModal
import com.example.ui.screens.BootScreen
import com.example.ui.screens.EnglishMatchScreen
import com.example.ui.screens.GeoBalloonScreen
import com.example.ui.screens.GymRhythmScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.LibraryBooksScreen
import com.example.ui.screens.LockerRoomScreen
import com.example.ui.screens.ParentAreaScreen
import com.example.ui.screens.ProfileSelectScreen
import com.example.ui.screens.ScienceCatapultScreen
import com.example.ui.screens.YardBattleScreen
import com.example.ui.theme.DeepBlueAccent
import com.example.ui.theme.ScuolaRetroTheme
import com.example.viewmodel.ActiveModal
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ScuolaRetroViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ScuolaRetroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScuolaRetroTheme {
                ScuolaRetroApp(
                    viewModel = viewModel,
                    onExitApp = { finish() }
                )
            }
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
