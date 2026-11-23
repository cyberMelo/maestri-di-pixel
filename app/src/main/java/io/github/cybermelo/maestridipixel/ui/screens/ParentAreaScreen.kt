package io.github.cybermelo.maestridipixel.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cybermelo.maestridipixel.core.GameBalance
import io.github.cybermelo.maestridipixel.core.PackManager
import io.github.cybermelo.maestridipixel.core.ValidationResult
import io.github.cybermelo.maestridipixel.ui.components.ConfirmModal
import io.github.cybermelo.maestridipixel.ui.components.GbaCard
import io.github.cybermelo.maestridipixel.ui.theme.DeepBlueAccent
import io.github.cybermelo.maestridipixel.ui.theme.EmeraldSuccess
import io.github.cybermelo.maestridipixel.ui.theme.HudNightBg
import io.github.cybermelo.maestridipixel.ui.theme.IceWhiteText
import io.github.cybermelo.maestridipixel.ui.theme.SilkscreenFontFamily
import io.github.cybermelo.maestridipixel.ui.theme.WarmAmber
import io.github.cybermelo.maestridipixel.viewmodel.AppUiState
import io.github.cybermelo.maestridipixel.viewmodel.ScuolaRetroViewModel

@Composable
fun ParentAreaScreen(
    uiState: AppUiState,
    viewModel: ScuolaRetroViewModel
) {
    BackHandler { viewModel.handleGlobalBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Progressi", "Pacchetti & Sync", "Impostazioni")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudNightBg)
            .safeDrawingPadding()
            .imePadding()
            .testTag("parent_area_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.handleGlobalBack() },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("parent_back_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlueAccent, contentColor = IceWhiteText)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Torna a Scuola")
                    Spacer(Modifier.width(6.dp))
                    Text("Scuola")
                }

                Text(
                    text = "AREA GENITORI",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 16.sp,
                    color = IceWhiteText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = uiState.activeProfile?.name ?: "-",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            GbaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(0.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            modifier = Modifier.testTag("parent_tab_$idx"),
                            text = { Text(label, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> ParentProgressTab(uiState = uiState, viewModel = viewModel)
                    1 -> ParentPacksSyncTab(uiState = uiState, viewModel = viewModel)
                    2 -> ParentSettingsTab(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun ParentProgressTab(
    uiState: AppUiState,
    viewModel: ScuolaRetroViewModel
) {
    val sessions = uiState.activeProfile?.recentSessions?.reversed()?.take(10) ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Padronanza per Argomento", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        items(uiState.activePacks, key = { it.packId }) { pack ->
            val masteryPct = viewModel.learningEngine.getPackMasteryPercent(pack).toInt().coerceIn(0, 100)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(pack.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("$masteryPct%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { masteryPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = GameBalance.subjectColor(pack.subject),
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Da ripassare (Top 5)", style = MaterialTheme.typography.titleLarge, color = WarmAmber)
            if (uiState.weakQuestions.isEmpty()) {
                Text(
                    "Nessuna domanda da ripassare al momento: ottimo lavoro!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(uiState.weakQuestions, key = { "${it.packTitle}:${it.questionId}" }) { weak ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.5.dp, WarmAmber)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(weak.packTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(weak.prompt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(
                        "Risposta esatta: ${weak.correctAnswerText} · Errori: ${weak.wrongTotal} · Livello ${weak.box}/3",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Ultime 10 sfide", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            if (sessions.isEmpty()) {
                Text("Nessuna sfida ancora completata.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(sessions) { s ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (s.isBoss) "Boss: ${s.packTitle}" else s.packTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Al primo colpo: ${s.correctFirstTry}/${s.totalQuestions} · +${s.starsEarned} stelle",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (s.victory) "Completata" else "Allenamento",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (s.victory) EmeraldSuccess else WarmAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentPacksSyncTab(
    uiState: AppUiState,
    viewModel: ScuolaRetroViewModel
) {
    val context = LocalContext.current
    var importFeedback by remember { mutableStateOf<ValidationResult?>(null) }

    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (text != null) {
                    importFeedback = viewModel.importPackFromUriText(text, "file_importato.json")
                }
            } catch (_: Exception) {
                importFeedback = ValidationResult().addError("Impossibile leggere il file selezionato.")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val sync = uiState.config.syncState
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stato Sincronizzazione GitHub", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = sync.lastSyncMessage.ifBlank { "Nessuna sincronizzazione eseguita finora." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncNow() },
                            enabled = !uiState.isSyncing,
                            modifier = Modifier
                                .heightIn(min = 50.dp)
                                .testTag("sync_now_button")
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (uiState.isSyncing) "Connessione in corso..." else "Sincronizza ora")
                        }
                    }
                    if (sync.lastSyncErrors.isNotEmpty()) {
                        sync.lastSyncErrors.forEach { err ->
                            Text("• $err", style = MaterialTheme.typography.labelMedium, color = WarmAmber)
                        }
                    }
                }
            }
        }

        // Local Import (Clipboard primary, SAF File secondary)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Importazione Locale Pacchetti JSON", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipText = try {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = cm?.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        clip.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                    } else {
                                        ""
                                    }
                                } catch (_: Throwable) {
                                    ""
                                }
                                importFeedback = viewModel.importPackFromClipboardText(clipText)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("import_clipboard_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = IceWhiteText)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Incolla JSON dagli appunti")
                        }
                        OutlinedButton(
                            onClick = {
                                try {
                                    openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                } catch (_: Throwable) {
                                    importFeedback = ValidationResult().addError("Nessun selettore file disponibile su questo dispositivo.")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("import_file_button")
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Importa da file .json")
                        }
                    }
                    importFeedback?.let { fb ->
                        if (fb.isValid) {
                            Text("Pacchetto validato e aggiunto!", style = MaterialTheme.typography.labelLarge, color = EmeraldSuccess)
                        } else {
                            fb.errors.forEach { err ->
                                Text("• $err", style = MaterialTheme.typography.labelMedium, color = WarmAmber)
                            }
                        }
                        fb.warnings.forEach { w ->
                            Text("Avviso: $w", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Installed packs list with priority toggle, focus topics & local delete
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Pacchetti & Focus Argomenti", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Scegli uno o più argomenti per far concentrare le sfide solo su ciò che serve ora (oppure lascia 'Tutti').",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(uiState.activePacks, key = { it.packId }) { pack ->
            val packTopics = remember(pack.packId, pack.playableQuestions.size) { pack.getTopics() }
            val selectedTopics = uiState.config.focusTopics[pack.packId].orEmpty()
            val activeQuestionCount = remember(pack.playableQuestions, selectedTopics) {
                if (selectedTopics.isEmpty()) pack.playableQuestions.size
                else pack.playableQuestions.count { it.topic.trim() in selectedTopics }.coerceAtLeast(1)
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.5.dp, if (pack.priority) WarmAmber else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pack.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "${GameBalance.subjectLabel(pack.subject)} · v${pack.version} · $activeQuestionCount/${pack.playableQuestions.size} domande attive · ${pack.source}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Verifica!", style = MaterialTheme.typography.labelMedium, color = WarmAmber)
                            Switch(
                                checked = pack.priority,
                                onCheckedChange = { viewModel.togglePackPriority(pack.packId, it) },
                                modifier = Modifier.testTag("priority_switch_${pack.packId}")
                            )
                            if (pack.source == "local") {
                                OutlinedButton(
                                    onClick = { viewModel.deleteLocalPack(pack.packId) },
                                    modifier = Modifier.testTag("delete_local_${pack.packId}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina pacchetto locale")
                                    Text("Elimina")
                                }
                            }
                        }
                    }

                    if (packTopics.isNotEmpty()) {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val allSelected = selectedTopics.isEmpty()
                            Button(
                                onClick = { viewModel.clearPackFocusTopics(pack.packId) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("topic_all_${pack.packId}"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (allSelected) DeepBlueAccent else MaterialTheme.colorScheme.surface,
                                    contentColor = if (allSelected) IceWhiteText else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.5.dp, if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text("Tutti (${pack.playableQuestions.size})", style = MaterialTheme.typography.labelMedium)
                            }
                            packTopics.forEachIndexed { idx, topic ->
                                val isSelected = topic in selectedTopics
                                val countForTopic = pack.playableQuestions.count { it.topic.trim() == topic }
                                val subjCol = GameBalance.subjectColor(pack.subject)
                                Button(
                                    onClick = { viewModel.togglePackFocusTopic(pack.packId, topic) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("topic_chip_${pack.packId}_$idx"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) subjCol else MaterialTheme.colorScheme.surface,
                                        contentColor = if (isSelected) IceWhiteText else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.5.dp, if (isSelected) subjCol else MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text("$topic ($countForTopic)", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentSettingsTab(
    uiState: AppUiState,
    viewModel: ScuolaRetroViewModel
) {
    val prefs = uiState.config.studyPreferences
    var ghTestMessage by remember { mutableStateOf<String?>(null) }
    val hasPatConfigured = remember { PackManager.hasConfiguredBuildPat() }

    var newProfileName by remember { mutableStateOf("") }
    var resetStep by remember { mutableIntStateOf(0) } // 0=none, 1=first confirm, 2=second confirm

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Highlighted 1-tap Studio Sereno Preset
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            border = BorderStroke(2.5.dp, EmeraldSuccess)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attiva Profilo Studio Sereno",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Sfide brevi, senza timer, lettura vocale e pausa di lettura",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { viewModel.activateStudioSerenoPreset() },
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("studio_sereno_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = IceWhiteText)
                ) {
                    Icon(Icons.Default.Spa, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Attiva")
                }
            }
        }

        // Individual Study & Visual Comfort Options
        Text("Aspetto e Opzioni di Studio", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

        SettingToggleRow(
            title = "Tema Scuro Riposante (Consigliato su tablet)",
            subtitle = "Sfondo scuro ad alto contrasto senza abbagliamento; disattivalo per il tema chiaro crema",
            checked = prefs.darkMode,
            testTag = "toggle_dark_mode",
            onCheckedChange = { v -> viewModel.updateStudyPreferences { it.copy(darkMode = v) } }
        )

        SettingToggleRow(
            title = "Attiva aiuto 50/50 nelle domande a scelta multipla",
            subtitle = "Elimina 2 risposte sbagliate su richiesta durante la sfida",
            checked = prefs.enable5050,
            testTag = "toggle_5050",
            onCheckedChange = { v -> viewModel.updateStudyPreferences { it.copy(enable5050 = v) } }
        )

        // Session length selector: Breve (4) / Standard (6) / Estesa (8)
        Text("Lunghezza sfida", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(4 to "Breve (4)", 6 to "Standard (6)", 8 to "Estesa (8)").forEach { (len, label) ->
                val selected = prefs.sessionLength == len
                Button(
                    onClick = { viewModel.updateStudyPreferences { it.copy(sessionLength = len) } },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("session_len_$len"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) DeepBlueAccent else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) IceWhiteText else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(label)
                }
            }
        }

        // Reading pause selector: Spenta (0s) / Breve (1s) / Media (2s)
        Text("Pausa di lettura prima di rispondere", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0f to "Spenta (0s)", 1f to "Breve (1s)", 2f to "Media (2s)").forEach { (sec, label) ->
                val selected = kotlin.math.abs(prefs.readingPauseSec - sec) < 0.05f
                Button(
                    onClick = { viewModel.updateStudyPreferences { it.copy(readingPauseSec = sec) } },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("reading_pause_${sec.toInt()}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) DeepBlueAccent else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) IceWhiteText else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(label)
                }
            }
        }

        SettingToggleRow(
            title = "Modalità Ritmo Calmo (senza timer)",
            subtitle = "Ferma discesa invasori, bersagli in movimento e cronometro nei mini-giochi",
            checked = prefs.calmMode,
            testTag = "toggle_calm_mode",
            onCheckedChange = { v -> viewModel.updateStudyPreferences { it.copy(calmMode = v) } }
        )
        SettingToggleRow(
            title = "Riduci animazioni e scosse",
            subtitle = "Rende i movimenti più pacati nei mini-giochi",
            checked = prefs.reducedMotion,
            testTag = "toggle_reduced_motion",
            onCheckedChange = { v -> viewModel.updateStudyPreferences { it.copy(reducedMotion = v) } }
        )
        SettingToggleRow(
            title = "Voce TTS automatica sulle domande",
            subtitle = "Legge ad alta voce ogni nuova domanda e spiegazione",
            checked = prefs.autoTtsQuestions,
            testTag = "toggle_auto_tts",
            onCheckedChange = { v -> viewModel.updateStudyPreferences { it.copy(autoTtsQuestions = v) } }
        )
        SettingToggleRow(
            title = "Mostra guida \"Sfida consigliata\" nell'Hub",
            subtitle = "Mostra la pillola rapida in alto nel Cortile e Corridoio",
            checked = prefs.showGuidancePill,
            testTag = "toggle_guidance_pill",
            onCheckedChange = { v -> viewModel.updateStudyPreferences { it.copy(showGuidancePill = v) } }
        )
        SettingToggleRow(
            title = "Mostra D-Pad virtuale",
            subtitle = "Aggiunge i tasti direzionali oltre al tocco sulla mappa",
            checked = prefs.showVirtualDpad,
            testTag = "toggle_virtual_dpad",
            onCheckedChange = { v -> viewModel.updateStudyPreferences { it.copy(showVirtualDpad = v) } }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Locked GitHub Repository Status (configured at build level, not editable on device)
        Text("Repository Pacchetti GitHub", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Configurazione bloccata", tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "${PackManager.LOCKED_GITHUB_OWNER}/${PackManager.LOCKED_GITHUB_REPO}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Branch: ${PackManager.LOCKED_GITHUB_BRANCH} · Cartella: ${PackManager.LOCKED_GITHUB_BASE_PATH} (bloccato a livello di app)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hasPatConfigured) {
                        "Autenticazione: Token sicuro integrato nella build"
                    } else {
                        "Autenticazione: Accesso pubblico (imposta GITHUB_PACKS_PAT nei Secrets se il repo è privato)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (hasPatConfigured) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        viewModel.testLockedGithubConnection { res ->
                            ghTestMessage = res.message
                        }
                    },
                    enabled = !uiState.isSyncing,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("test_github_connection_button")
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (uiState.isSyncing) "Connessione in corso..." else "Testa connessione")
                }
                ghTestMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Audio Sliders
        Text("Volume Audio", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Musica: ${(uiState.config.audio.musicVolume * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = uiState.config.audio.musicVolume,
            onValueChange = { viewModel.setAudioVolumes(it, uiState.config.audio.sfxVolume) }
        )
        Text("Effetti sonori: ${(uiState.config.audio.sfxVolume * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = uiState.config.audio.sfxVolume,
            onValueChange = { viewModel.setAudioVolumes(uiState.config.audio.musicVolume, it) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Profile Management & PIN Change & Double-Confirmed Reset
        Text("Gestione Profili e Sicurezza", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        uiState.profiles.forEach { prof ->
            val isActive = prof.profileId == uiState.activeProfile?.profileId
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${prof.name} (${prof.stars} stelle)",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    if (!isActive) {
                        Button(onClick = { viewModel.switchProfile(prof.profileId) }) {
                            Text("Attiva")
                        }
                    } else {
                        Text("Attivo", style = MaterialTheme.typography.labelLarge, color = EmeraldSuccess)
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newProfileName,
                onValueChange = { if (it.length <= 16) newProfileName = it },
                label = { Text("Nuovo profilo (nome)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (viewModel.createAdditionalProfile(newProfileName, uiState.profiles.size % 4)) {
                        newProfileName = ""
                    }
                },
                enabled = newProfileName.trim().length in 1..16,
                modifier = Modifier.heightIn(min = 52.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Crea")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.openChangePinGate() },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .testTag("change_pin_button")
            ) {
                Icon(Icons.Default.Key, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Cambia PIN")
            }
            OutlinedButton(
                onClick = { resetStep = 1 },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .testTag("reset_progress_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmAmber),
                border = BorderStroke(2.dp, WarmAmber)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Reset progressi")
            }
        }

        // Generous bottom clearance >= 220dp so keyboard in landscape never covers fields
        Spacer(Modifier.height(260.dp))
    }

    if (resetStep == 1) {
        ConfirmModal(
            title = "Azzerare i progressi?",
            message = "Vuoi davvero azzerare stelle, medaglie, armadietto e storico di ${uiState.activeProfile?.name ?: "questo profilo"}?",
            confirmLabel = "Continua",
            onConfirm = { resetStep = 2 },
            onCancel = { resetStep = 0 }
        )
    } else if (resetStep == 2) {
        ConfirmModal(
            title = "Conferma definitiva",
            message = "Questa azione non può essere annullata. Il nome e l'avatar resteranno intatti.",
            confirmLabel = "Azzera tutto",
            onConfirm = {
                resetStep = 0
                viewModel.resetActiveProfileProgress()
            },
            onCancel = { resetStep = 0 }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
