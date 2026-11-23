package com.example

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.core.MinigameLaunchConfig
import com.example.core.MinigameResult
import com.example.ui.components.DialogBoxModal
import com.example.ui.components.ExplanationModal
import com.example.ui.components.PackPickerModal
import com.example.ui.components.PinModal
import com.example.ui.components.ResultSummaryModal
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
import com.example.ui.theme.ScuolaRetroTheme
import com.example.viewmodel.PinMode
import com.example.viewmodel.ScuolaRetroViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UiSmokeCrashTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun createReadyViewModel(): ScuolaRetroViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ScuolaRetroViewModel(app, syncOnBoot = false)
        vm.saveManager.loadAll()
        TestPacks.installInto(vm.packManager)
        vm.createProfileAndEnterHub("Marco", 0)
        vm.updateStudyPreferences { it.copy(readingPauseSec = 0f, enable5050 = true) }
        vm.refreshSnapshot()
        return vm
    }

    @Composable
    private fun SizedFrame(portrait: Boolean, content: @Composable () -> Unit) {
        ScuolaRetroTheme {
            Box(modifier = Modifier.size(if (portrait) 390.dp else 1280.dp, if (portrait) 844.dp else 800.dp)) {
                content()
            }
        }
    }

    @Test
    fun renderBootScreenWithoutCrash() {
        composeRule.setContent {
            SizedFrame(portrait = true) {
                BootScreen()
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("boot_screen").assertExists()
    }

    @Test
    fun renderProfileSelectAndCreate() {
        var created = ""
        composeRule.setContent {
            SizedFrame(portrait = true) {
                ProfileSelectScreen(onCreateProfile = { n, _ -> created = n })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_name_input").performScrollTo().performTextInput("Giulia")
        composeRule.onNodeWithTag("avatar_option_2").performScrollTo().performClick()
        composeRule.onNodeWithTag("profile_start_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assert(created == "Giulia")
    }

    @Test
    fun renderFullAppContainerPortrait() {
        val vm = createReadyViewModel()
        composeRule.setContent {
            SizedFrame(portrait = true) {
                ScuolaRetroApp(viewModel = vm, onExitApp = {})
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun renderHubYardAndHallwayPortraitAndLandscape() {
        val vm = createReadyViewModel()
        composeRule.setContent {
            SizedFrame(portrait = true) {
                HubScreen(
                    hubId = "school_yard",
                    spawnId = "default",
                    uiState = vm.uiState.value,
                    onNpcInteract = {},
                    onPortalTrigger = {},
                    onGuidanceClick = {},
                    onToggleMute = {},
                    onOpenParentGate = {},
                    onPlayStepSfx = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hub_screen_school_yard").assertExists()
        composeRule.onNodeWithTag("virtual_dpad").assertExists()
        composeRule.onNodeWithTag("hud_mute_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun renderHubHallwayLandscape() {
        val vm = createReadyViewModel()
        composeRule.setContent {
            SizedFrame(portrait = false) {
                HubScreen(
                    hubId = "school_hallway",
                    spawnId = "from_yard",
                    uiState = vm.uiState.value,
                    onNpcInteract = {},
                    onPortalTrigger = {},
                    onGuidanceClick = {},
                    onToggleMute = {},
                    onOpenParentGate = {},
                    onPlayStepSfx = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hub_screen_school_hallway").assertExists()
    }

    @Test
    fun renderYardBattleAndInteractPortrait() {
        val vm = createReadyViewModel()
        val cfg = MinigameLaunchConfig("storia_egizi", "yard_battle", false, "school_yard", "default")
        composeRule.setContent {
            SizedFrame(portrait = true) {
                YardBattleScreen(config = cfg, viewModel = vm)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("move_base_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("battle_question_prompt").assertExists()
        composeRule.onNodeWithTag("battle_hint_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("battle_hint_banner").assertExists()
    }

    @Test
    fun renderYardBattlePhoneLandscapeAnswersVisible() {
        val vm = createReadyViewModel()
        val cfg = MinigameLaunchConfig("storia_egizi", "yard_battle", false, "school_yard", "default")
        composeRule.setContent {
            ScuolaRetroTheme {
                Box(modifier = Modifier.size(844.dp, 390.dp)) {
                    YardBattleScreen(config = cfg, viewModel = vm)
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("move_base_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("battle_question_prompt").assertExists()
    }

    @Test
    fun renderGymRhythmPortraitWithoutScrollCrash() {
        composeRule.mainClock.autoAdvance = false
        val vm = createReadyViewModel()
        val cfg = MinigameLaunchConfig("matematica_calcolo_base", "gym_rhythm", false, "school_hallway", "from_gym")
        composeRule.setContent {
            SizedFrame(portrait = true) {
                GymRhythmScreen(config = cfg, viewModel = vm)
            }
        }
        composeRule.mainClock.advanceTimeBy(150)
        composeRule.onNodeWithTag("gym_rhythm_screen").assertExists()
        composeRule.onNodeWithTag("gym_question_prompt").assertExists()
        composeRule.onNodeWithTag("space_invaders_arena").assertExists()
        composeRule.onNodeWithTag("gym_fire_lane_0").assertExists()
        // Toggle to classic Tastierino mode and back to Space Invaders
        composeRule.onNodeWithTag("gym_mode_toggle_button").performClick()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("gym_mode_toggle_button").performClick()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("space_invaders_arena").assertExists()
        // Fire laser on lane 0
        composeRule.onNodeWithTag("gym_fire_lane_0").performClick()
        composeRule.mainClock.advanceTimeBy(200)
    }

    @Test
    fun renderGymRhythmLandscapeWithoutCrash() {
        composeRule.mainClock.autoAdvance = false
        val vm = createReadyViewModel()
        val cfg = MinigameLaunchConfig("matematica_calcolo_base", "gym_rhythm", false, "school_hallway", "from_gym")
        composeRule.setContent {
            SizedFrame(portrait = false) {
                GymRhythmScreen(config = cfg, viewModel = vm)
            }
        }
        composeRule.mainClock.advanceTimeBy(150)
        composeRule.onNodeWithTag("gym_rhythm_screen").assertExists()
        composeRule.onNodeWithTag("gym_question_prompt").assertExists()
        composeRule.onNodeWithTag("space_invaders_arena").assertExists()
        composeRule.onNodeWithTag("gym_fire_lane_0").assertExists()
    }

    @Test
    fun renderScienceCatapultPortraitAndInteract() {
        composeRule.mainClock.autoAdvance = false
        val vm = createReadyViewModel()
        val cfg = MinigameLaunchConfig("scienze_fotosintesi", "science_catapult", false, "school_hallway", "from_lab")
        composeRule.setContent {
            SizedFrame(portrait = true) {
                ScienceCatapultScreen(config = cfg, viewModel = vm)
            }
        }
        composeRule.mainClock.advanceTimeBy(150)
        composeRule.onNodeWithTag("science_catapult_screen").assertExists()
        composeRule.onNodeWithTag("catapult_question_prompt").assertExists()
        composeRule.onNodeWithTag("catapult_arena").assertExists()
        composeRule.onNodeWithTag("catapult_hint_button").performClick()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("catapult_hint_banner").assertExists()
        composeRule.onNodeWithTag("catapult_target_0").performClick()
        composeRule.mainClock.advanceTimeBy(250)
    }

    @Test
    fun renderScienceCatapultLandscapeWithoutCrash() {
        composeRule.mainClock.autoAdvance = false
        val vm = createReadyViewModel()
        val cfg = MinigameLaunchConfig("scienze_fotosintesi", "science_catapult", false, "school_hallway", "from_lab")
        composeRule.setContent {
            SizedFrame(portrait = false) {
                ScienceCatapultScreen(config = cfg, viewModel = vm)
            }
        }
        composeRule.mainClock.advanceTimeBy(150)
        composeRule.onNodeWithTag("science_catapult_screen").assertExists()
        composeRule.onNodeWithTag("catapult_question_prompt").assertExists()
        composeRule.onNodeWithTag("catapult_arena").assertExists()
        composeRule.onNodeWithTag("catapult_target_0").assertExists()
    }

    @Test
    fun renderPhoneLandscapeAllMinigamesWithoutCrash() {
        composeRule.mainClock.autoAdvance = false
        val vm = createReadyViewModel()
        val gymCfg = MinigameLaunchConfig("matematica_calcolo_base", "gym_rhythm", false, "school_hallway", "from_gym")
        composeRule.setContent {
            ScuolaRetroTheme {
                Box(modifier = Modifier.size(844.dp, 390.dp)) {
                    GymRhythmScreen(config = gymCfg, viewModel = vm)
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithTag("gym_rhythm_screen").assertExists()
        composeRule.onNodeWithTag("space_invaders_arena").assertExists()
        composeRule.onNodeWithTag("gym_fire_lane_0").performClick()
        composeRule.mainClock.advanceTimeBy(250)
    }

    @Test
    fun renderPhoneLandscapeScienceCatapultWithoutCrash() {
        composeRule.mainClock.autoAdvance = false
        val vm = createReadyViewModel()
        val sciCfg = MinigameLaunchConfig("scienze_fotosintesi", "science_catapult", false, "school_hallway", "from_lab")
        composeRule.setContent {
            ScuolaRetroTheme {
                Box(modifier = Modifier.size(844.dp, 390.dp)) {
                    ScienceCatapultScreen(config = sciCfg, viewModel = vm)
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithTag("science_catapult_screen").assertExists()
        composeRule.onNodeWithTag("catapult_arena").assertExists()
        composeRule.onNodeWithTag("catapult_target_0").performClick()
        composeRule.mainClock.advanceTimeBy(250)
    }

    @Test
    fun renderPhoneLandscapeLibraryBooksWithoutCrash() {
        composeRule.mainClock.autoAdvance = false
        val vm = createReadyViewModel()
        val cfg = MinigameLaunchConfig("italiano_grammatica", "library_books", false, "school_hallway", "from_library")
        composeRule.setContent {
            ScuolaRetroTheme {
                Box(modifier = Modifier.size(844.dp, 390.dp)) {
                    LibraryBooksScreen(config = cfg, viewModel = vm)
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithTag("library_books_screen").assertExists()
        composeRule.onNodeWithTag("library_question_prompt").assertExists()
        composeRule.onNodeWithTag("library_arena").assertExists()
        composeRule.onNodeWithTag("library_hint_button").performClick()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("library_hint_banner").assertExists()
        composeRule.onNodeWithTag("library_book_0").performClick()
        composeRule.mainClock.advanceTimeBy(250)
    }

    @Test
    fun renderPhoneLandscapeGeoBalloonWithoutCrash() {
        composeRule.mainClock.autoAdvance = false
        val vm = createReadyViewModel()
        val cfg = MinigameLaunchConfig("geografia_italia", "geo_balloon", false, "school_hallway", "from_geo")
        composeRule.setContent {
            ScuolaRetroTheme {
                Box(modifier = Modifier.size(844.dp, 390.dp)) {
                    GeoBalloonScreen(config = cfg, viewModel = vm)
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithTag("geo_balloon_screen").assertExists()
        composeRule.onNodeWithTag("geo_question_prompt").assertExists()
        composeRule.onNodeWithTag("geo_arena").assertExists()
        composeRule.onNodeWithTag("geo_hint_button").performClick()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("geo_hint_banner").assertExists()
        composeRule.onNodeWithTag("geo_cloud_0").performClick()
        composeRule.mainClock.advanceTimeBy(250)
    }

    @Test
    fun renderPhoneLandscapeEnglishMatchWithoutCrash() {
        val vm = createReadyViewModel()
        val cfg = MinigameLaunchConfig("inglese_base", "english_match", false, "school_hallway", "from_english")
        composeRule.setContent {
            ScuolaRetroTheme {
                Box(modifier = Modifier.size(844.dp, 390.dp)) {
                    EnglishMatchScreen(config = cfg, viewModel = vm)
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("english_match_screen").assertExists()
        composeRule.onNodeWithTag("english_question_prompt").assertExists()
        composeRule.onNodeWithTag("english_speak_en_button").performClick()
        composeRule.onNodeWithTag("english_hint_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("english_hint_banner").assertExists()
        composeRule.onNodeWithTag("english_tile_0").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun renderLockerRoomAllTabsPortrait() {
        val vm = createReadyViewModel()
        composeRule.setContent {
            SizedFrame(portrait = true) {
                LockerRoomScreen(uiState = vm.uiState.value, viewModel = vm)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("locker_tab_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("locker_slot_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sticker_picker_modal").assertExists()
        composeRule.onNodeWithTag("sticker_remove_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("locker_tab_2").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun renderParentAreaAllTabsAndEmptyClipboardSafe() {
        val vm = createReadyViewModel()
        composeRule.setContent {
            SizedFrame(portrait = true) {
                ParentAreaScreen(uiState = vm.uiState.value, viewModel = vm)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("parent_tab_1").performClick()
        composeRule.waitForIdle()
        // Clicking empty clipboard button must never throw IndexOutOfBoundsException
        composeRule.onNodeWithTag("import_clipboard_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("parent_tab_2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("studio_sereno_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun renderAllModalsPortraitWithoutCrash() {
        val vm = createReadyViewModel()
        val packs = vm.packManager.getActivePacks()
        composeRule.setContent {
            SizedFrame(portrait = true) {
                Box {
                    PackPickerModal(
                        title = "Maestro Erodoto",
                        subtitle = "Storia",
                        packs = packs,
                        masteryProvider = { 45f },
                        hasMedalProvider = { false },
                        onSelectPack = { _, _ -> },
                        onDismiss = {}
                    )
                    ExplanationModal(
                        header = "Ci sei quasi!",
                        correctAnswerDisplay = "Nilo",
                        explanation = "Il Nilo fertilizzava le terre dell'antico Egitto.",
                        autoRead = false,
                        onSpeak = {},
                        onStopSpeak = {},
                        onContinue = {}
                    )
                    ResultSummaryModal(
                        result = MinigameResult(
                            packId = "storia_egizi",
                            minigameId = "yard_battle",
                            isBoss = false,
                            victory = true,
                            correctFirstTry = 5,
                            totalQuestions = 6,
                            helpsUsed = 1,
                            starsEarned = 18,
                            priorityBonusApplied = true,
                            masteryBefore = 20f,
                            masteryAfter = 55f,
                            medalUnlocked = false,
                            suggestMicroBreak = true
                        ),
                        packTitle = "Gli Antichi Egizi",
                        medalName = "Medaglia dello Scriba",
                        onContinue = {}
                    )
                    DialogBoxModal(
                        title = "Preside Stella",
                        message = "Benvenuto in Maestri di Pixel!",
                        speakerSpriteId = "npc_boss",
                        onDismiss = {}
                    )
                    PinModal(
                        mode = PinMode.ENTER_PARENT_AREA,
                        showDefaultPinHint = true,
                        onVerifyPin = { it == "1234" },
                        onSetNewPin = {},
                        onDismiss = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pin_key_1").performClick()
        composeRule.onNodeWithTag("pin_key_2").performClick()
        composeRule.onNodeWithTag("pin_key_3").performClick()
        composeRule.onNodeWithTag("pin_key_4").performClick()
        composeRule.waitForIdle()
    }
}
