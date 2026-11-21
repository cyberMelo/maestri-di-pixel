# Piano Implementativo Tecnico Definitivo — Scuola Retro (Pilota 1)

> **Destinatario:** Agente AI di sviluppo software.
> **Obiettivo:** Realizzare un'applicazione educativa Android per tablet ($9\text{"}$, formato $16:10$) basata su **Godot 4.3+ (GDScript 2.0)** per bambini di 4ª e 5ª elementare italiana: hub scolastico esplorabile in pixel-art 16-bit, 2 mini-giochi completi, motore didattico Leitner, opzioni di supporto allo studio personalizzabili (TTS con ducking audio, pausa di lettura, indizi testuali, aiuto `50/50` protetto da PIN genitore, evidenziazione parole chiave, modalità a ridotta stimolazione), sync offline-first da repository GitHub privato, stanza Armadietto/Spaccio e Area Genitori protetta da PIN.
> **Principio di non ambiguità:** Ogni contratto dati, segnale, coordinata tileset, formula matematica, regola UX/touch, edge case Android/Godot e criterio di verifica è specificato esplicitamente in questo documento. Non inventare formati alternativi né omettere validazioni.

---

## 0. Regole Operative Vincolanti per l'Agente

1. **REGOLA ASSOLUTA DI PRIVACY (ZERO TERMINOLOGIA CLINICA — MAI VIOLABILE):**
   * In **nessun punto dell'applicazione** — né nelle schermate del bambino, né nell'Area Genitori, né in etichette, pulsanti, preset, toast, messaggi di sistema, descrizioni, pacchetti JSON, file di configurazione/salvataggio, `project.godot`, script in `res://tools/`, nomi di variabili visibili o `README.md` di distribuzione — deve **MAI** comparire alcun riferimento scritto a condizioni cliniche o etichette scolastiche speciali (es. mai usare parole o espressioni come `ADHD`, `BES`, `DSA`, `dislessia`, `dislessico`, `discalculia`, `discalculico`, `disturbo`, `disturbi`, `bisogni speciali`, `terapia`, `terapie`, `compensativo`, `compensativi`, `clinico`, `clinica`, `diagnosi`, `iperattivita`, `iperattività`, `neurodivergente`).
   * Tutti i testi e le opzioni devono usare esclusivamente **linguaggio educativo neutro, naturale e positivo** (es. *"Studio Sereno"*, *"Ritmo Calmo"*, *"Pausa di lettura"*, *"Aiuto 50/50"*, *"Indizio"*, *"Riduci animazioni"*).
   * **Nota workspace:** non salvare questo file di specifica (o altri documenti di analisi) dentro `res://` del progetto Godot, così non potrà mai essere incluso nell'APK o scansionato nei file di progetto.
   * Questa regola è **tassativa** e viene verificata automaticamente dalla suite di test headless (Test 7).
2. **Engine e Linguaggio:** Godot **4.3 o superiore** (stabile). Tutto il codice applicativo deve essere scritto in **GDScript 2.0 strettamente tipizzato** (type hints espliciti su parametri, valori di ritorno e variabili membro). Usare `Variant` solo ai boundary di parsing JSON.
3. **Nodi Godot 4.3+ (Zero API Deprecate):**
   * Usare esclusivamente **`TileMapLayer`** (MAI il deprecato `TileMap`).
   * **NON scrivere a mano** risorse complesse `.tres` (`TileSet`, `Theme`) se rischiose: generarle programmaticamente tramite script Godot (`res://tools/generate_placeholders.gd` e `res://tools/build_resources.gd`) eseguiti in modalità `--headless`.
4. **Zero Dipendenze Esterne Obbligatorie:** Usare solo API native di Godot 4 (`HTTPRequest`, `FileAccess`, `DirAccess`, `AStarGrid2D`, `DisplayServer.tts_*`, `DisplayServer.clipboard_get`, `AudioStreamWAV`, `AudioStreamOggVorbis`, `Image`, `TileMapLayer`).
5. **Lingua Utente vs Codice:** Tutti i testi visibili all'utente (UI, dialoghi, bottoni, messaggi errore, pacchetti bundled) devono essere in **italiano corretto**, caldo, incoraggiante e adatto a bambini di $9$–$11$ anni (mai tono punitivo o colpevolizzante sull'errore). Nomi di classi, file, metodi, segnali e commenti nel codice devono essere in **inglese**.
6. **Modificabilità in Corso d'Opera:** Nessun parametro di bilanciamento (HP, danni, tempi, stelle, soglie Leitner, soglia Boss, colori materia, pausa di lettura, limite recuperi) deve essere hardcoded nelle scene. Ogni costante di tuning risiede in `res://core/game_balance.gd`.
7. **Sicurezza Credenziali:** Il token GitHub (`pat`) e l'header `Authorization` non devono **MAI** essere stampati in `print()`, `push_error()`, `push_warning()`, `last_sync_message` o `last_sync_errors`.
8. **Eseguibilità Immediata (Zero Asset Mancanti):** Nella Milestone 1 l'agente deve generare tutti gli sprite PNG, il `TileSet`, il `Theme` e gli audio WAV chiptune tramite script deterministici in `res://tools/`, committando gli asset generati in `res://assets/` e `res://ui/theme/`.
9. **Nessuno Stub Silenzioso:** Ogni controllo previsto dal Pilota 1 deve funzionare realmente. Solo le due porte future (`Laboratorio di Scienze` e `Biblioteca`) mostrano una `DialogBox` esplicita *"Lavori in corso"*.
10. **Verifica Automatica Obbligatoria:** Mantenere sempre verde `godot --headless --script res://tests/run_tests.gd` (exit code `0`) al termine di ogni milestone.
11. **Ordine Finale Obbligatorio:** La **Sezione 14** (preparazione template repository GitHub privato e script CLI di validazione pacchetti) va eseguita **per ultima**, solo dopo aver completato e verificato tutte le Milestone $1$–$8$.

---

## 1. Configurazione Progetto Godot (`project.godot`)

Impostare esattamente i seguenti parametri in `project.godot`:

* **Application:**
  * `config/name = "Scuola Retro"`
  * `run/main_scene = "res://scenes/boot/boot.tscn"`
  * `config/features = PackedStringArray("4.3", "GL Compatibility")`
* **Display / Window (Samsung Galaxy Tab $9\text{"}$, $16:10$ e $16.75:10$):**
  * `window/size/viewport_width = 1280`
  * `window/size/viewport_height = 800`
  * `window/size/mode = 0` (finestra in debug desktop; fullscreen immersivo su Android via export preset)
  * `window/stretch/mode = "canvas_items"`
  * `window/stretch/aspect = "expand"`
  * `window/handheld/orientation = 0` (`landscape` fisso)
* **Rendering:**
  * `renderer/rendering_method = "gl_compatibility"`
  * `renderer/rendering_method.mobile = "gl_compatibility"`
  * `textures/canvas_textures/default_texture_filter = 1` (`Linear` di default per nitidezza UI/font; tutti i root del mondo 2D pixel-art impostano esplicitamente `texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST`).
  * `2d/snap/snap_2d_transforms_to_pixel = true`
  * `2d/snap/snap_2d_vertices_to_pixel = true`
* **Input Devices:**
  * `pointing/emulate_touch_from_mouse = true`
* **Audio Buses (`res://default_bus_layout.tres`):**
  * Bus `0`: `Master`
  * Bus `1`: `Music` (send $\rightarrow$ `Master`)
  * Bus `2`: `SFX` (send $\rightarrow$ `Master`)
* **Input Map Actions:**
  * `move_up`: W, Up Arrow
  * `move_down`: S, Down Arrow
  * `move_left`: A, Left Arrow
  * `move_right`: D, Right Arrow
  * `interact`: Space, Enter, E
  * `ui_cancel`: Escape

### Autoload Singletons (ordine obbligatorio)

| Ordine | Nome Autoload | Percorso Script | Responsabilità |
| :--- | :--- | :--- | :--- |
| 1 | `GameBalance` | `res://core/game_balance.gd` | Costanti di bilanciamento, colori materia, parametri opzioni di studio |
| 2 | `EventBus` | `res://core/event_bus.gd` | Segnali globali tipizzati disaccoppiati |
| 3 | `SaveManager` | `res://core/save_manager.gd` | `config.json`, profili con salvataggio atomico cross-platform, reset, lifecycle |
| 4 | `AudioManager` | `res://core/audio_manager.gd` | Musica loop `.wav`/`.ogg`, ducking automatico durante TTS, pool SFX (6 player), mute |
| 5 | `TTSManager` | `res://core/tts_manager.gd` | Sintesi vocale offline `it` con risoluzione lazy Android, pulizia `**`/BBCode, ducking |
| 6 | `PackManager` | `res://core/pack_manager.gd` | Cache offline, manifest bundled, sync GitHub sequenziale con mutex `is_syncing`, import file/appunti |
| 7 | `LearningEngine` | `res://core/learning_engine.gd` | Leitner 4-box (con `used_help`), padronanza su `playable_questions`, deck builder, settimana solare, guida consigliata |
| 8 | `SceneRouter` | `res://core/scene_router.gd` | Transizioni unificate ($0.35\text{ s}$), `TTSManager.stop()` automatico, passaggio config/spawn, gestione Back/Pause |

---

## 2. Struttura Directory del Progetto (`res://`)

```text
res://
├── project.godot
├── default_bus_layout.tres
├── export_presets.cfg
├── core/
│   ├── game_balance.gd
│   ├── event_bus.gd
│   ├── save_manager.gd
│   ├── audio_manager.gd
│   ├── tts_manager.gd
│   ├── text_formatter.gd          # Parsing sicuro **parole chiave** -> BBCode & plain text
│   ├── pack_manager.gd
│   ├── pack_validator.gd
│   ├── math_generator.gd
│   ├── learning_engine.gd
│   ├── scene_router.gd
│   └── models/
│       ├── pack_data.gd
│       ├── question_data.gd
│       ├── generator_spec.gd
│       ├── validation_result.gd
│       ├── minigame_launch_config.gd
│       └── minigame_result.gd
├── data/
│   └── shop_catalog.json
├── packs/
│   ├── manifest.json
│   ├── storia_egizi.json
│   ├── scienze_fotosintesi.json
│   └── matematica_calcolo_base.json
├── assets/
│   ├── fonts/
│   ├── sprites/
│   │   ├── avatars/        # avatar_0.png .. avatar_3.png (48x64)
│   │   ├── npcs/           # npc_history.png, npc_science.png, npc_math.png, npc_boss.png (48x64)
│   │   ├── companions/     # pet_owl.png, pet_cat.png, pet_dog.png, pet_robot.png (48x64)
│   │   ├── tiles/
│   │   │   ├── tileset_school.png   # 128x128 (8x8 tile 16x16)
│   │   │   └── school_tileset.tres  # generato via script
│   │   ├── icons/          # icone 32x32 (incluse icon_hint.png e icon_5050.png)
│   │   └── ui/             # ui_frame_gba.png, ui_frame_dark.png (24x24)
│   └── audio/
│       ├── sfx/            # 12 file .wav
│       └── music/          # hub_theme.wav, battle_theme.wav
├── ui/
│   ├── theme/
│   │   └── gba_theme.tres  # generato via script (fondo crema #FAF8F2, interlinea ampia)
│   ├── common/
│   │   ├── scene_transition.tscn / .gd
│   │   ├── hub_hud.tscn / .gd
│   │   ├── virtual_dpad.tscn / .gd
│   │   ├── dialog_box.tscn / .gd
│   │   ├── confirm_modal.tscn / .gd
│   │   ├── explanation_modal.tscn / .gd
│   │   ├── result_summary_modal.tscn / .gd
│   │   ├── progress_stepper.tscn / .gd   # Barra pallini fissa "Domanda X di N"
│   │   ├── retro_numpad.tscn / .gd
│   │   └── pack_picker_modal.tscn / .gd
│   └── parent_area/
│       ├── pin_modal.tscn / .gd          # Tastierino numerico custom on-screen (no tastiera Android)
│       └── parent_area.tscn / .gd
├── scenes/
│   ├── boot/
│   │   ├── boot.tscn / .gd
│   │   └── profile_select.tscn / .gd
│   ├── hub/
│   │   ├── hub_base.gd
│   │   ├── player_controller.tscn / .gd
│   │   ├── companion_follower.tscn / .gd
│   │   ├── interactable_npc.tscn / .gd
│   │   ├── portal_zone.tscn / .gd
│   │   ├── school_yard.tscn / .gd
│   │   └── school_hallway.tscn / .gd
│   └── locker/
│       └── locker_room.tscn / .gd
├── minigames/
│   ├── base_minigame.gd
│   ├── yard_battle/
│   │   └── yard_battle.tscn / .gd
│   └── gym_rhythm/
│       └── gym_rhythm.tscn / .gd
├── tools/
│   ├── generate_placeholders.gd
│   ├── build_resources.gd
│   └── validate_pack.gd           # Step finale (Sez. 14): validatore CLI per nuovi JSON
├── tests/
│   └── run_tests.gd
└── github_packs_repo_template/    # Step finale (Sez. 14): da creare SOLO dopo Milestone 8
```

---

## 3. Contratti Esatti: Modelli, `EventBus`, `SceneRouter`, `GameBalance` e `shop_catalog.json`

### 3.1 Modelli Dati (`res://core/models/*.gd`, tutti `extends RefCounted`)

1. **`question_data.gd` (`class_name QuestionData`):**
   * `id: String`
   * `type: String` (`"multiple_choice"`, `"true_false"`, `"numeric_input"`, `"classification"`)
   * `difficulty: int` ($\in \{1, 2, 3\}$)
   * `prompt: String` (può contenere marcatori opzionali `**parola chiave**`)
   * `hint: String = ""` (opzionale, $\le 90$ caratteri; indizio testuale mostrato su richiesta)
   * `options: PackedStringArray` (usato se `multiple_choice`)
   * `correct_index: int = -1` (usato se `multiple_choice`)
   * `correct_bool: bool = false` (usato se `true_false`)
   * `correct_value: float = 0.0` (usato se `numeric_input`, sempre $\ge 0.0$)
   * `unit: String = ""` (usato se `numeric_input`)
   * `tolerance: float = 0.0` (usato se `numeric_input`)
   * `explanation: String` (può contenere marcatori opzionali `**parola chiave**`)
   * `is_generated: bool = false`
   * Metodo helper: `get_correct_answer_display() -> String` (restituisce testo opzione corretta, `"Vero"`/`"Falso"`, o valore numerico formattato + eventuale unità).

2. **`generator_spec.gd` (`class_name GeneratorSpec`):**
   * `id: String`
   * `template: String` (`"multiplication_table"`, `"mental_add_sub"`, `"unit_conversion"`)
   * `difficulty: int`
   * `params: Dictionary`
   * `pool_size: int`

3. **`pack_data.gd` (`class_name PackData`):**
   * `pack_id: String`
   * `subject: String`
   * `grade: int`
   * `title: String`
   * `version: int`
   * `boss_name: String`
   * `boss_title: String`
   * `medal_name: String`
   * `medal_icon: String`
   * `source: String` (`"bundled"`, `"github"`, `"local"`)
   * `priority: bool`
   * `archived: bool = false`
   * `static_questions: Array[QuestionData]`
   * `generators: Array[GeneratorSpec]`
   * `playable_questions: Array[QuestionData]`

4. **`validation_result.gd` (`class_name ValidationResult`):**
   * `is_valid: bool = true`
   * `errors: Array[String] = []`
   * `warnings: Array[String] = []`
   * Metodo helper: `add_error(msg: String) -> void` (imposta automaticamente `is_valid = false`).

5. **`minigame_launch_config.gd` (`class_name MinigameLaunchConfig`):**
   * `pack_id: String`
   * `minigame_id: String` (`"yard_battle"` o `"gym_rhythm"`)
   * `is_boss: bool = false`
   * `return_scene_path: String`
   * `return_spawn_id: String`

6. **`minigame_result.gd` (`class_name MinigameResult`):**
   * `pack_id: String`
   * `minigame_id: String`
   * `is_boss: bool`
   * `victory: bool`
   * `correct_first_try: int`
   * `total_questions: int` (numero di domande uniche effettivamente affrontate e concluse nello scontro, es. `3` su KO anticipato in `3` mosse speciali)
   * `helps_used: int`
   * `stars_earned: int`
   * `priority_bonus_applied: bool`
   * `medal_unlocked: bool`
   * `suggest_micro_break: bool`
   * `mastery_before: float`
   * `mastery_after: float`

### 3.2 Segnali Globali (`res://core/event_bus.gd`)

```gdscript
extends Node

signal profile_changed(profile_id: String)
signal stars_changed(new_total: int, delta: int)
signal medal_unlocked(pack_id: String, medal_name: String)
signal companion_changed(companion_id: String)
signal frame_changed(frame_id: String)
signal packs_updated()
signal sync_started()
signal sync_finished(status: String, message: String)
signal settings_changed()
signal audio_mute_changed(is_muted: bool)
```

### 3.3 Helper Testo & Parole Chiave (`res://core/text_formatter.gd`)

Classe statica `class_name TextFormatter`:
* `static func strip_markers(raw: String) -> String`: rimuove tutti i marcatori `**` (usato da `PackValidator` per misurare la lunghezza reale del testo e da `TTSManager` prima di parlare, così la voce non legge mai "asterisco").
* `static func to_bbcode(raw: String, highlight_hex: String = "#0F4C81") -> String`:
  1. Esegue escape sicuro delle parentesi quadre (`raw.replace("[", "[lb]")`) per evitare injection BBCode accidentali.
  2. Converte ogni coppia `**testo**` in `[b][color=%s]testo[/color][/b]` (usando `RegEx` con pattern `\*\*([^\*]+)\*\*`).
  3. Tutte le domande, spiegazioni e indizi nelle scene vanno renderizzati in `RichTextLabel` con `bbcode_enabled = true` passando per `TextFormatter.to_bbcode(...)`.

### 3.4 Contratto `SceneRouter` (`res://core/scene_router.gd`)

* Proprietà di stato: `next_spawn_id: String = "default"`, `pending_minigame_config: MinigameLaunchConfig = null`, `is_transitioning: bool = false`.
* Metodi pubblici:
  * `go_to_scene(scene_path: String, spawn_id: String = "default")`
  * `launch_minigame(config: MinigameLaunchConfig)`
  * `return_from_minigame(config: MinigameLaunchConfig)`
  * **Regole di guardia:** se `is_transitioning == true`, ignorare chiamate duplicate; su ogni transizione chiamare immediatamente `TTSManager.stop()` prima del fade out.
* **Gestione globale ciclo di vita Android e tasto Back (`_notification(what: int)`):**
  * `NOTIFICATION_APPLICATION_PAUSED` / `NOTIFICATION_APPLICATION_FOCUS_OUT`: chiama immediatamente `TTSManager.stop()` e `SaveManager.flush_all()`.
  * `NOTIFICATION_WM_GO_BACK_REQUEST`: chiama `TTSManager.stop()` e `handle_back_request()` $\rightarrow$ delega a `on_back_requested() -> bool` della scena corrente (chiude modali aperte; nei mini-giochi chiede conferma uscita; in `LockerRoom` torna in corridoio; nell'Hub apre `ConfirmModal` *"Vuoi uscire da Scuola Retro?"*). Mai chiusura brusca improvvisa.

### 3.5 Parametri Centralizzati (`res://core/game_balance.gd`)

* **Sessioni e Lunghezza Adattiva:**
  * `SESSION_LENGTH_OPTIONS: Array[int] = [4, 6, 8]` (Breve = $4$, Standard = $6$, Estesa = $8$).
  * `DEFAULT_SESSION_QUESTIONS: int = 6` (il profilo *"Studio Sereno"* imposta `4`).
  * `BOSS_EXTRA_QUESTIONS: int = 2` (quindi Boss dura `session_length + 2`: $6$, $8$ o $10$ domande, clamped a `deck.size()`).
  * `BOSS_UNLOCK_MASTERY_PCT: float = 80.0`
  * `BOSS_MIN_FIRST_TRY_ACCURACY: float = 0.75` ($75\%$)
  * `REQUEUE_DELAY_MIN: int = 2`
  * `REQUEUE_DELAY_MAX: int = 3`
  * `MAX_RETRIES_PER_QUESTION: int = 2` (limite anti-loop: dopo $2$ errori sulla stessa domanda nella medesima sfida, la domanda non viene più rimessa in coda in questa sessione; resta in `Box 0` per la sessione futura).
  * `BREAK_REMINDER_SESSIONS: int = 2` (suggerisce una micro-pausa di $2$ minuti ogni $2$ sfide consecutive completate).
* **Leitner (4 Box: $0, 1, 2, 3$):**
  * `LEITNER_MAX_BOX: int = 3`
  * `BOX_COOLDOWN_SECONDS: Dictionary = {0: 0, 1: 0, 2: 172800, 3: 604800}`
* **Economia Stelle:**
  * `STARS_PER_CORRECT: int = 10`
  * `STARS_FIRST_TRY_BONUS: int = 5` (assegnato solo se corretta al primo tentativo E senza usare `Indizio` o `50/50`)
  * `STARS_SESSION_COMPLETE: int = 25`
  * `STARS_BOSS_VICTORY_BONUS: int = 60` (assegnato **solo alla prima vittoria Boss** che sblocca la Medaglia; le rivincite Boss successive assegnano `STARS_SESSION_COMPLETE`)
  * `PRIORITY_MULTIPLIER: float = 1.5`
  * `WEEKLY_GOAL_SESSIONS: int = 3`
* **Cortile (`yard_battle`):**
  * `PLAYER_MAX_HP: int = 100`
  * `BASE_ATTACK_DAMAGE: int = 20` (`difficulty == 1`)
  * `SPECIAL_ATTACK_DAMAGE: int = 36` (`difficulty in [2, 3]`, fattore $1.8\times$)
  * `ENEMY_COUNTER_DAMAGE: int = 20`
  * `BOSS_COUNTER_DAMAGE: int = 25`
  * **Formula HP dinamica:** `enemy_max_hp = deck.size() * BASE_ATTACK_DAMAGE` (es. $4$ domande $\rightarrow 80\text{ HP}$, $6$ domande $\rightarrow 120\text{ HP}$, $8$ domande Boss $\rightarrow 160\text{ HP}$).
* **Palestra (`gym_rhythm`):**
  * `GYM_TIME_NUMERIC_SEC: float = 15.0`
  * `GYM_TIME_CHOICE_SEC: float = 12.0`
  * `GYM_FAST_BONUS_RATIO: float = 0.45`
  * `GYM_COMBO_SUPER_THRESHOLD: int = 3`
  * `NUMPAD_MAX_CHARS: int = 8`
  * `NUMERIC_MIN_EPSILON: float = 0.0001`
* **UI, Safe Area, Comfort Visivo e Opzioni di Studio:**
  * `SAFE_MARGIN_X: int = 16`
  * `SAFE_MARGIN_Y: int = 12`
  * `SCENE_TRANSITION_DURATION: float = 0.35`
  * `EXPLANATION_UNLOCK_DELAY_SEC: float = 0.8`
  * `DEFAULT_READING_PAUSE_SEC: float = 1.0` (opzioni in Area Genitori: `0.0`, `1.0`, `2.0`)
  * `TTS_MUSIC_DUCK_DB: float = -12.0`
  * `PARENT_HOLD_DURATION_SEC: float = 2.0`
  * `DEFAULT_PARENT_PIN: String = "1234"`
  * `WORLD_CAMERA_ZOOM: Vector2 = Vector2(3.0, 3.0)`
  * `TILE_SIZE: int = 16`
  * `PLAYER_SPEED_PX: float = 80.0`
  * `CARD_BG_COLOR: Color = Color("#FAF8F2")` (crema morbido anti-abbagliamento)
  * `CARD_TEXT_COLOR: Color = Color("#1E293B")` (antracite ad alto contrasto)
  * `KEYWORD_HIGHLIGHT_HEX: String = "#0F4C81"` (blu scuro leggibile)
  * `ERROR_WARM_COLOR: Color = Color("#D97706")` (ambra caldo non ansiogeno, mai rosso aggressivo)
  * `ENCOURAGING_ERROR_HEADERS: PackedStringArray = PackedStringArray(["Quasi! Scopriamo il trucco:", "Ci sei vicino! Guarda qui:", "Nessun problema, impariamo questo:", "Ottimo tentativo! Ecco il segreto:"])`
* **Palette Colori Materie (Hex):**
  * `storia`: `#D97706`, `geografia`: `#059669`, `scienze`: `#0284C7`, `matematica`: `#DC2626`, `italiano`: `#7C3AED`, `inglese`: `#DB2777`.

### 3.6 Schema Esatto Catalogo Spaccio (`res://data/shop_catalog.json`)

```json
{
  "schema_version": 1,
  "items": [
    {"id": "pet_owl", "category": "companion", "name": "Gufo Saggio", "price": 80, "icon": "res://assets/sprites/companions/pet_owl.png"},
    {"id": "pet_cat", "category": "companion", "name": "Micio Pixel", "price": 100, "icon": "res://assets/sprites/companions/pet_cat.png"},
    {"id": "pet_dog", "category": "companion", "name": "Cagnolino Lampo", "price": 120, "icon": "res://assets/sprites/companions/pet_dog.png"},
    {"id": "pet_robot", "category": "companion", "name": "Robottino Bit", "price": 150, "icon": "res://assets/sprites/companions/pet_robot.png"},
    {"id": "sticker_rocket", "category": "sticker", "name": "Razzo Spaziale", "price": 30, "icon": "res://assets/sprites/icons/sticker_rocket.png"},
    {"id": "sticker_dino", "category": "sticker", "name": "Dinosauro Rex", "price": 35, "icon": "res://assets/sprites/icons/sticker_dino.png"},
    {"id": "sticker_planet", "category": "sticker", "name": "Pianeta Saturno", "price": 40, "icon": "res://assets/sprites/icons/sticker_planet.png"},
    {"id": "sticker_book", "category": "sticker", "name": "Libro Magico", "price": 45, "icon": "res://assets/sprites/icons/sticker_book.png"},
    {"id": "sticker_trophy", "category": "sticker", "name": "Coppa d'Oro", "price": 50, "icon": "res://assets/sprites/icons/sticker_trophy.png"},
    {"id": "sticker_lightning", "category": "sticker", "name": "Fulmine Turbo", "price": 60, "icon": "res://assets/sprites/icons/sticker_lightning.png"},
    {"id": "frame_classic", "category": "frame", "name": "Cornice Classica", "price": 0, "color_hex": "#1E293B"},
    {"id": "frame_emerald", "category": "frame", "name": "Cornice Smeraldo", "price": 70, "color_hex": "#059669"},
    {"id": "frame_gold", "category": "frame", "name": "Cornice Oro", "price": 90, "color_hex": "#D97706"},
    {"id": "frame_space", "category": "frame", "name": "Cornice Galassia", "price": 110, "color_hex": "#7C3AED"}
  ]
}
```

---

## 4. JSON Schema Rigido (`schema_version = 1`) e Validazione

### 4.1 Schema `manifest.json`

```json
{
  "schema_version": 1,
  "updated_at": "2026-11-21T09:00:00Z",
  "packs": [
    {
      "pack_id": "storia_egizi",
      "file": "storia_egizi.json",
      "subject": "storia",
      "title": "Gli Antichi Egizi",
      "version": 1,
      "priority": false,
      "archived": false
    }
  ]
}
```

### 4.2 Schema Pacchetto Didattico (`<pack_id>.json`)

```json
{
  "schema_version": 1,
  "pack_id": "storia_egizi",
  "subject": "storia",
  "grade": 4,
  "title": "Gli Antichi Egizi",
  "version": 1,
  "boss_name": "Faraone Tutankhamon",
  "boss_title": "Guardiano del Nilo",
  "medal_name": "Medaglia dello Scarabeo",
  "medal_icon": "medal_egypt",
  "questions": [
    {
      "id": "sto_egizi_001",
      "type": "multiple_choice",
      "difficulty": 1,
      "prompt": "Quale **fiume** ha reso fertile la terra dell'antico Egitto?",
      "hint": "È il fiume più lungo dell'Africa e scorre fino al Mediterraneo.",
      "options": ["Nilo", "Tigri", "Eufrate", "Po"],
      "correct_index": 0,
      "explanation": "Le piene annuali del **Nilo** lasciavano il **limo**, un fango ricco e fertile."
    },
    {
      "id": "sto_egizi_002",
      "type": "true_false",
      "difficulty": 1,
      "prompt": "Gli antichi Egizi erano **politeisti**, cioè credevano in molti dei.",
      "hint": "La parola 'poli' in greco antico significa 'molti'.",
      "correct_bool": true,
      "explanation": "Sì! Adoravano molte divinità legate alla natura, come **Ra**, **Osiride** e **Anubi**."
    },
    {
      "id": "mat_calc_001",
      "type": "numeric_input",
      "difficulty": 2,
      "prompt": "Quanti **centimetri** ci sono in **3 metri**?",
      "hint": "Ricorda: 1 metro vale 100 centimetri.",
      "correct_value": 300,
      "unit": "cm",
      "tolerance": 0,
      "explanation": "Ogni metro vale 100 centimetri, quindi **3 x 100 = 300 cm**."
    }
  ],
  "generators": [
    {
      "id": "gen_tabelline_6_9",
      "template": "multiplication_table",
      "difficulty": 1,
      "params": {"min_a": 6, "max_a": 9, "min_b": 2, "max_b": 9},
      "pool_size": 10
    }
  ]
}
```
*Regola frazioni e numeri negativi:* le domande la cui risposta è una frazione (es. `"3/4"`, `"1/2"`) usano sempre `type = "multiple_choice"`; le domande `numeric_input` devono avere sempre `correct_value >= 0` (il `RetroNumpad` non ha il segno `-`).

### 4.3 Regole Rigorose di `PackValidator` (`res://core/pack_validator.gd`)

1. **`PackValidator.validate_manifest(dict: Dictionary) -> ValidationResult`:**
   * `schema_version` intero `== 1`.
   * `packs` deve essere `TYPE_ARRAY` (anche vuoto è permesso in un manifest remoto, mai `null`).
   * Per ogni entry `packs[i]` (`TYPE_DICTIONARY`):
     * `pack_id`: regex `^[a-z0-9_]{3,48}$`, univoco dentro il manifest.
     * `file`: stringa non vuota che termina con `.json`, **blocco path traversal obbligatorio** (rifiutare se contiene `..`, `\`, `:`, o inizia con `/`; regex `^[a-zA-Z0-9_\-/]+\.json$` senza `..` né `/` iniziale).
     * `subject` in `["storia", "geografia", "scienze", "matematica", "italiano", "inglese"]`.
     * `title` stringa non vuota ($3$–$60$ char).
     * `version` intero $\ge 1$.
     * `priority` e `archived` opzionali, ma se presenti devono essere `TYPE_BOOL`.

2. **`PackValidator.validate_pack(dict: Dictionary, filename: String) -> ValidationResult`:**
   * **Header pacchetto:**
     * `schema_version` intero `== 1` (validare `typeof(v) in [TYPE_INT, TYPE_FLOAT]` e `is_equal_approx(float(v), floor(float(v)))` per i campi interi JSON).
     * `pack_id` regex `^[a-z0-9_]{3,48}$`.
     * `subject` in `["storia", "geografia", "scienze", "matematica", "italiano", "inglese"]`.
     * `grade` intero $\in \{4, 5\}$.
     * `title` stringa $3$–$60$ char; `boss_name` e `medal_name` stringhe non vuote ($\le 40$ char).
     * `version` intero $\ge 1$.
     * Totale domande giocabili nel Pilota 1 $\ge 5$.
     * **Warning di compatibilità mini-gioco:** se `subject != "matematica"` e il conteggio di domande `multiple_choice` + `true_false` è $< 5$, aggiungere un `warning` esplicito (non bloccante).
   * **Singola domanda (`questions[i]`):**
     * `id` univoco nel pacchetto, regex `^[a-z0-9_]{3,64}$`.
     * `type` in `["multiple_choice", "true_false", "numeric_input", "classification"]` (`classification` $\rightarrow$ accettata con warning non bloccante).
     * `difficulty` intero $\in \{1, 2, 3\}$.
     * `prompt` stringa non vuota, lunghezza di `TextFormatter.strip_markers(prompt)` $\le 140$ char.
     * `explanation` stringa non vuota, lunghezza di `TextFormatter.strip_markers(explanation)` $\le 160$ char.
     * `hint` **opzionale**: se presente, deve essere stringa con lunghezza di `TextFormatter.strip_markers(hint)` $\le 90$ char.
     * Se `multiple_choice`: `options` array di esattamente $4$ stringhe non vuote, ciascuna $\le 45$ char, nessuna duplicata (case-insensitive trimmed); `correct_index` intero $\in \{0, 1, 2, 3\}$.
     * Se `true_false`: `correct_bool` di tipo `TYPE_BOOL`.
     * Se `numeric_input`: `correct_value` numerico **`>= 0.0` obbligatorio** (rifiuta negativi), `tolerance` opzionale numerico $\ge 0$ (default $0.0$), `unit` opzionale stringa $\le 12$ char.
   * **Generatori (`generators[j]`):**
     * `id` univoco (non in collisione con ID statici); `template` in `["multiplication_table", "mental_add_sub", "unit_conversion"]`; `difficulty` $\in \{1, 2, 3\}$; `pool_size` intero $\in [4, 30]$.

### 4.4 Generatori Procedurali Deterministici (`res://core/math_generator.gd`)

* `RandomNumberGenerator.new()` con `rng.seed = hash(pack_id + "::" + spec.id)`.
* Ogni domanda generata ha ID `"%s_%02d" % [spec.id, idx]`, `type = "numeric_input"`, `correct_value >= 0.0`, `is_generated = true`, e un `hint` educativo automatico:
  1. `multiplication_table`: `prompt = "Quanto fa **%d x %d**?"`, `hint = "Prova a sommare %d per %d volte (o parti da %d x %d)."`, `explanation = "**%d x %d** fa **%d**."`.
  2. `mental_add_sub`: `min_val` (default $10$), `max_val` (default $90$), `allow_sub` (default `true`), `result_non_negative` (sempre `true`); `hint` suggerisce di separare decine e unità.
  3. `unit_conversion`: `family` in `["length", "mass", "capacity"]`; `hint` ricorda il fattore di conversione.

---

## 5. Sync da Repository GitHub Privato, Cache Offline & Import Locale (`PackManager`)

### 5.1 Gerarchia Cartelle e Precedenza (Compatibilità APK `.pck` Android)

Directory in `user://`: `user://packs_cache/`, `user://packs_staging/`, `user://packs_local/`.

Ordine di caricamento all'avvio (**immediato e 100% offline**):
1. **Pacchetti Bundled (`res://packs/`, `source = "bundled"`):**
   * **Regola critica Android `.pck`:** NON usare `DirAccess` per scansionare `res://packs/`. Leggere **sempre e solo** `res://packs/manifest.json` con `FileAccess`, validarlo con `PackValidator.validate_manifest(...)` e caricare ciascun file indicato nel manifest (`"res://packs/" + entry["file"]`).
2. **Cache GitHub (`user://packs_cache/`, `source = "github"`):**
   * Leggere `user://packs_cache/manifest.json` (se presente) e i corrispondenti file sul filesystem reale `user://`.
   * Rispetta `priority` e `archived` dal manifest in cache.
   * **Regola `archived: true`:** esclude il pacchetto dall'Hub, **senza MAI cancellare** medaglie o storico Leitner salvati in `save.json`.
3. **Importati Locali (`user://packs_local/*.json`, `source = "local"`):**
   * Enumerare via `DirAccess` su `user://packs_local/`, sovrascrivendo pacchetti con stesso `pack_id` e forzando `archived = false`.
4. **Precedenza `priority`:** `SaveManager.config.priority_overrides[pack_id]` ha precedenza sul manifest.

### 5.2 Protocollo HTTP GitHub Contents API (Sequenziale, Mutex, Normalizzazione URL & Guardia Sincrona)

* **Posizionamento nodo `HTTPRequest`:** istanziare un singolo nodo `HTTPRequest` (`timeout = 10.0`) come **figlio diretto dell'Autoload `PackManager`** in `_ready()` (MAI dentro `boot.tscn` o altre scene transitorie).
* **Mutex `is_syncing: bool = false`:** se `sync_from_github()` o `test_github_connection()` viene chiamata mentre `is_syncing == true`, uscire subito restituendo `{"status": "busy", "message": "Sincronizzazione già in corso..."}`.
* **Helper `build_github_contents_url(owner: String, repo: String, branch: String, base_path: String, rel_path: String) -> String`:**
  * Applica `.strip_edges()` a tutti i parametri.
  * Rimuove eventuali `/` iniziali e finali da `base_path` e `rel_path`.
  * Se `base_path == ""`: `https://api.github.com/repos/%s/%s/contents/%s?ref=%s` (`[owner, repo, rel_path, branch]`).
  * Altrimenti: `https://api.github.com/repos/%s/%s/contents/%s/%s?ref=%s` (`[owner, repo, base_path, rel_path, branch]`).
  * Garantisce **zero doppi slash `//`** nel path.
* **Header:** `Authorization: Bearer {pat.strip_edges()}`, `Accept: application/vnd.github.raw+json`, `X-GitHub-Api-Version: 2022-11-28`, `User-Agent: ScuolaRetroApp/1.0`.
* **Guardia sincrona obbligatoria in `_http_get_text(url: String, headers: PackedStringArray) -> Dictionary`:**
  ```gdscript
  var req_err: Error = _http.request(url, headers)
  if req_err != OK:
      return {"ok": false, "code": 0, "body": "", "error": error_string(req_err)}
  var res: Array = await _http.request_completed
  # ... elaborazione res ...
  ```
  Questo garantisce che `is_syncing` non resti **mai** bloccato a `true` anche in caso di errore sincrono o URL non valido.
* **Redazione PAT:** nessun messaggio di errore o log deve mai includere `pat` o l'header `Authorization`.

### 5.3 Procedura `sync_from_github()` e `test_github_connection()`

1. Controlla `is_syncing`; se libero imposta `is_syncing = true` ed emette `sync_started`.
2. Se credenziali mancanti (`owner`, `repo`, `branch` o `pat` vuoti dopo `.strip_edges()`) $\rightarrow$ `status = "not_configured"`, rilascia `is_syncing`.
3. Scarica `manifest.json`: offline/timeout $\rightarrow$ `status = "offline"` (cache precedente intatta!); `401`/`403` $\rightarrow$ errore permessi PAT; `404` $\rightarrow$ errore repo/branch/path.
4. Valida `manifest.json` tramite `PackValidator.validate_manifest(...)` (se invalido, annulla senza toccare `packs_cache/`).
5. Svuota `packs_staging/`, scarica sequenzialmente solo i file con `archived == false` nuovi o con `version` superiore alla cache, valida ciascuno con `PackValidator.validate_pack(...)` (loggando errori specifici per file scartati, senza mai loggare il PAT).
6. Promozione atomica `.tmp` $\rightarrow$ rename portabile in `packs_cache/`, aggiornamento atomico `manifest.json`, pulizia `packs_staging/`, ricarica pacchetti, salva `sync_state`, rilascia `is_syncing = false`, emette `packs_updated` e `sync_finished`.

### 5.4 Import Locale A Prova di Scoped Storage Android 11+

1. `import_pack_from_json_text(raw_json: String, source_label: String) -> ValidationResult`: usato dal pulsante primario **"Incolla JSON dagli appunti"** (`DisplayServer.clipboard_get()`).
2. `import_pack_from_file(abs_path: String) -> ValidationResult`: pulsante secondario da `FileDialog`.
3. `delete_local_pack(pack_id: String) -> bool`: elimina file da `user://packs_local/` per i pacchetti `source == "local"`.

---

## 6. Motore Didattico Leitner 4-Box, Aiuti allo Studio & Guida Hub (`LearningEngine`)

### 6.1 Stato per Domanda (in `save.json["leitner"]`)

```json
"sto_egizi_001": {
  "box": 2,
  "last_seen_unix": 1763712000,
  "correct_total": 3,
  "wrong_total": 1,
  "last_wrong_unix": 1763500000
}
```

### 6.2 Regole di Avanzamento (Incluso `used_help` per Indizio o 50/50)

`record_answer(question_id: String, is_correct: bool, is_retry_in_session: bool, used_help: bool = false, now_unix: int = -1) -> void`:
* **Corretta al primo tentativo SENZA aiuto (`is_correct && !is_retry_in_session && !used_help`):** `box = min(box + 1, 3)`, `correct_total += 1`, `last_seen_unix = now`.
* **Corretta al primo tentativo CON aiuto (`Indizio` oppure `50/50`, cioè `is_correct && !is_retry_in_session && used_help`):** `box = max(box, 1)` se era `0`, altrimenti invariato (non promuove a `2` o `3` finché non risponde in autonomia); `correct_total += 1`, `last_seen_unix = now`.
* **Sbagliata (`!is_correct`):** `box = 0`, `wrong_total += 1`, `last_wrong_unix = now`, `last_seen_unix = now`.
* **Corretta su recupero nella stessa sessione (`is_correct && is_retry_in_session`):** `box` resta `0`, `correct_total += 1`, `last_seen_unix = now`.

### 6.3 Calcolo Padronanza, Sblocco Boss, Deck Builder e Shuffle Obbligatorio

* **Padronanza sempre su `playable_questions` (mai solo `static_questions`, così pacchetti matematici 100% procedurali non causano divisioni per zero):**
  * Se `N = pack.playable_questions.size() == 0`, restituisce `0.0`.
  * Altrimenti:
    $$\text{Padronanza}(P) = \frac{\sum_{i=1}^{N} \text{box}_i}{3 \cdot N} \times 100\%$$
  * Boss sbloccato se $\text{Padronanza}(P) \ge 80.0\%$.
* `build_session_deck(pack_id: String, count: int, allowed_types: Array[String]) -> Array[QuestionData]`: filtra `playable_questions` su `allowed_types` e ordina per priorità: `box == 0` con errori recenti $\rightarrow$ `box in [1, 2]` scadute $\rightarrow$ nuove $\rightarrow$ `box in [1, 2]` $\rightarrow$ `box == 3`.
* `get_shuffled_options(q: QuestionData, rng: RandomNumberGenerator = null) -> Dictionary`: restituisce `{"options": PackedStringArray, "correct_display_index": int}` senza mai mutare `q`. Obbligatorio in tutti i mini-giochi.

### 6.4 Guida Rapida Hub (`get_recommended_activity() -> Dictionary`)

Calcola sempre la prossima attività consigliata tra i pacchetti attivi (con almeno $1$ domanda giocabile compatibile):
1. Priorità 1: pacchetto con `priority == true` (Modalità Verifica) non ancora al $100\%$ (se $\ge 80\%$ e senza medaglia, suggerisce il Boss).
2. Priorità 2: pacchetto con Boss sbloccato ($\ge 80\%$) ma Medaglia non ancora vinta.
3. Priorità 3: pacchetto attivo con la padronanza più bassa.

Ritorna `{"pack_id": String, "title": String, "subject": String, "is_boss": bool, "target_location_label": String, "minigame_id": String}` (o `{}` se nessun pacchetto attivo).

### 6.5 Settimana Solare (Lunedì `00:00` Locale), Micro-Pause & Diagnostica Genitori

* `get_sessions_this_week(now_unix: int = -1) -> int`: conta sessioni completate con `timestamp_unix >= week_start_unix`, dove `days_since_monday = (dt["weekday"] + 6) % 7` e `week_start_unix = now_unix - (days_since_monday * 86400) - seconds_since_midnight`.
* `sessions_in_current_run: int = 0` (in memoria): incrementato solo al completamento della sfida (mai su uscita anticipata); se `sessions_in_current_run > 0 && sessions_in_current_run % GameBalance.BREAK_REMINDER_SESSIONS == 0`, imposta `result.suggest_micro_break = true`.
* `get_top_weak_questions(limit: int = 5) -> Array[Dictionary]`: top 5 domande con più errori recenti (`pack_title`, `prompt`, `correct_answer_text`, `wrong_total`, `box`).

---

## 7. Sistema Salvataggi Atomici Cross-Platform e Profili (`SaveManager`)

### 7.1 Schema `user://config.json`

```json
{
  "schema_version": 1,
  "active_profile_id": "prof_1763712000",
  "parent_pin": "1234",
  "audio": {
    "master_muted": false,
    "music_volume": 0.7,
    "sfx_volume": 0.9
  },
  "study_preferences": {
    "calm_mode": false,
    "reduced_motion": false,
    "reading_pause_sec": 1.0,
    "session_length": 6,
    "enable_50_50": false,
    "show_virtual_dpad": true,
    "show_guidance_pill": true,
    "auto_tts_questions": false
  },
  "priority_overrides": {},
  "github": {
    "owner": "",
    "repo": "",
    "branch": "main",
    "base_path": "packs",
    "pat": ""
  },
  "sync_state": {
    "last_sync_unix": 0,
    "last_sync_status": "never",
    "last_sync_message": "",
    "last_sync_errors": []
  }
}
```
*Nota di compatibilità:* all'avvio, se mancano chiavi in `config.json`, eseguire merge non distruttivo con i default sopra. `enable_50_50` parte **`false`** ed è modificabile **solo** all'interno dell'Area Genitori protetta da PIN.

### 7.2 Schema `user://profiles/<profile_id>/save.json` e Gestione Profili

```json
{
  "schema_version": 1,
  "profile_id": "prof_1763712000",
  "name": "Marco",
  "avatar_id": 0,
  "stars": 120,
  "equipped_companion": "pet_owl",
  "equipped_frame": "frame_classic",
  "owned_items": ["frame_classic"],
  "locker_stickers": ["", "", "", "", "", ""],
  "medals": ["storia_egizi"],
  "leitner": {},
  "recent_sessions": []
}
```
* **Cap storico:** `recent_sessions` mantiene al massimo le ultime $30$ voci (FIFO).
* **Contratto metodi profilo:**
  * `list_profiles() -> Array[Dictionary]`: scansiona le sottocartelle di `user://profiles/` contenenti `save.json` (o `.bak` recuperabile), restituendo `{"profile_id", "name", "avatar_id", "stars"}`.
  * `create_profile(raw_name: String, avatar_id: int) -> String`: valida `name = raw_name.strip_edges()` (lunghezza $1$–$16$ char), crea directory e `save.json`, imposta `active_profile_id` ed emette `profile_changed`.
  * `switch_profile(profile_id: String) -> bool`: salva il profilo corrente, carica quello scelto ed emette `profile_changed`.
  * `reset_active_profile_progress() -> void`: azzera `leitner = {}`, `recent_sessions = []`, `medals = []`, `stars = 0`, `owned_items = ["frame_classic"]`, `equipped_companion = ""`, `equipped_frame = "frame_classic"`, `locker_stickers = ["", "", "", "", "", ""]`, preservando `profile_id`, `name` e `avatar_id`; esegue salvataggio atomico ed emette `profile_changed`.

### 7.3 Scrittura Atomica Portabile (Linux/Android/Windows), Recovery e `flush_all()`

* `write_json_atomic(path: String, data: Dictionary) -> bool` (sequenza portabile su tutti gli OS):
  1. Scrive `path + ".tmp"` e chiude il file; riapre e verifica che `JSON.parse_string(...)` sia un `Dictionary` valido.
  2. Se esiste `path + ".bak"`, lo rimuove con `DirAccess.remove_absolute(...)`.
  3. Se esiste `path`, lo rinomina in `path + ".bak"` con `DirAccess.rename_absolute(...)`.
  4. Rinomina `path + ".tmp"` in `path` con `DirAccess.rename_absolute(...)`.
* `read_json_with_recovery(path: String, default_data: Dictionary) -> Dictionary`: prova `path`; se assente o corrotto, tenta `path + ".bak"` (ripristinando `path`), altrimenti restituisce `default_data.duplicate(true)`.
* `flush_all()`: salva subito `config.json` e il profilo attivo.

---

## 8. Asset Drop-in, Mappa Esatta Tileset, Audio Ducking e Comfort Visivo

### 8.1 Generazione Programmatica (`res://tools/generate_placeholders.gd` & `build_resources.gd`)

1. `res://tools/generate_placeholders.gd`: genera tutti i PNG e WAV in `res://assets/`.
2. `res://tools/build_resources.gd`: genera programmaticamente:
   * `res://assets/sprites/tiles/school_tileset.tres` (`TileSet` $16 \times 16$ con custom data layer `"solid": bool`);
   * `res://ui/theme/gba_theme.tres` (`Theme` GBA con pannelli card in crema morbido `#FAF8F2`, testo `#1E293B`, interlinea ampia `line_separation = 6`, font ad alta leggibilità e bottoni $\ge 64 \times 64\text{ px}$).

### 8.2 Tabella Esatta Coordinate `tileset_school.png` ($128 \times 128\text{ px}$, griglia $8 \times 8$ tile $16 \times 16$)

| Atlas `(col, row)` | Nome Tile | `solid` (`AStarGrid2D`) | Uso / Aspetto |
| :---: | :--- | :---: | :--- |
| `(0, 0)` | `grass_plain` | `false` | Erba cortile verde morbido |
| `(1, 0)` | `grass_flowers` | `false` | Erba con piccoli fiori gialli/bianchi |
| `(2, 0)` | `path_stone` | `false` | Vialetto pietra chiara cortile |
| `(3, 0)` | `fence_h` | `true` | Recinzione perimetrale orizzontale |
| `(4, 0)` | `fence_v` | `true` | Recinzione perimetrale verticale |
| `(5, 0)` | `tree_bush` | `true` | Albero/siepe verde scuro perimetrale |
| `(6, 0)` | `school_wall_ext` | `true` | Muro esterno scuola (mattoni caldi) |
| `(7, 0)` | `school_roof` | `true` | Tetto scuola (tegole rosso mattone) |
| `(0, 1)` | `hall_floor_a` | `false` | Pavimento corridoio chiaro |
| `(1, 1)` | `hall_floor_b` | `false` | Pavimento corridoio alternato |
| `(2, 1)` | `hall_wall` | `true` | Muro interno corridoio con zoccolino |
| `(3, 1)` | `door_active` | `false` | Porta attiva neutra |
| `(4, 1)` | `door_wip` | `true` | Porta con nastri giallo/neri "Lavori in corso" |
| `(5, 1)` | `door_gym` | `false` | Porta Palestra (cornice rossa `#DC2626`) |
| `(6, 1)` | `door_locker` | `false` | Porta Armadietto/Spaccio (cornice ambra `#D97706`) |
| `(7, 1)` | `doormat` | `false` | Zerbino ingresso |
| `(0, 2)` | `window_ext` | `true` | Finestra facciata esterna |
| `(1, 2)` | `window_int` | `true` | Finestra interna corridoio |
| `(2, 2)` | `locker_prop` | `true` | Armadietto decorativo |
| `(3, 2)` | `notice_board` | `true` | Bacheca avvisi scolastica |
| `(4, 2)` | `bench` | `true` | Panchina cortile |
| `(5, 2)` | `potted_plant` | `true` | Pianta in vaso corridoio |

### 8.3 Spritesheet, Icone & Audio (`AudioManager` + Ducking TTS)

* **Spritesheet ($48 \times 64\text{ px}$):** $3$ colonne (`idle`, `step_left`, `step_right`) $\times$ $4$ righe (`down`, `left`, `right`, `up`), celle $16 \times 16$: `avatar_0..3`, `npc_history`, `npc_science`, `npc_math`, `npc_boss`, `pet_owl`, `pet_cat`, `pet_dog`, `pet_robot`.
* **UI & Icone ($32 \times 32$):** `icon_star`, `icon_medal_default`, `icon_medal_locked`, `icon_speaker`, `icon_hint` (lampadina calda), `icon_5050` (badge metà/metà blu-oro), `icon_gear`, `icon_sound_on`, `icon_sound_off`, `icon_calm`, `icon_alert`, `icon_crown`, `sticker_rocket`, `sticker_dino`, `sticker_planet`, `sticker_book`, `sticker_trophy`, `sticker_lightning`; cornici `ui_frame_gba.png` e `ui_frame_dark.png` ($24 \times 24$, 9-slice $6\text{ px}$).
* **Audio WAV Chiptune (PCM 16-bit mono $22050\text{ Hz}$):**
  * 12 SFX (`ui_click`, `ui_back`, `step`, `door_open`, `encounter_start`, `attack_hit`, `attack_super`, `answer_correct`, `answer_wrong` [tono basso morbido e mai stridente], `star_gain`, `victory_fanfare`, `medal_unlock`) + 2 musiche (`hub_theme.wav`, `battle_theme.wav`).
  * **Loop `.wav` (mono/stereo) e `.ogg` drop-in garantito:** in `AudioManager.play_music(stream: AudioStream)`, gestire entrambi i formati:
    ```gdscript
    if stream is AudioStreamWAV:
        var wav := stream as AudioStreamWAV
        var bytes_per_sample: int = 2 if wav.format == AudioStreamWAV.FORMAT_16_BITS else 1
        var channels: int = 2 if wav.stereo else 1
        wav.loop_mode = AudioStreamWAV.LOOP_FORWARD
        wav.loop_begin = 0
        wav.loop_end = int(wav.data.size() / (bytes_per_sample * channels))
    elif stream is AudioStreamOggVorbis:
        (stream as AudioStreamOggVorbis).loop = true
    ```
  * **Audio Ducking durante TTS (`AudioManager.set_music_ducked(ducked: bool)`):** abbassa `"Music"` di `GameBalance.TTS_MUSIC_DUCK_DB` ($-12\text{ dB}$) mentre `TTSManager` legge e ripristina subito al termine o su `TTSManager.stop()`.

### 8.4 Tipografia & `TTSManager` (Risoluzione Voce Lazy per Android + Igiene Stop)

* **Font:** `SystemFont` (`["Atkinson Hyperlegible", "Lexend", "Roboto", "Noto Sans", "DejaVu Sans"]`). Dimensioni minime: domanda $24\text{ px}$, risposte $21\text{ px}$, spiegazione $22\text{ px}$, HUD $18\text{ px}$ bold. Touch target minimo: $64 \times 64\text{ px}$ ($84\text{ px}$ altezza bottoni opzione in battaglia).
* **`TTSManager` (Android-safe):**
  * Risoluzione **lazy** `_resolve_italian_voice_id()` al primo `speak()` (riprovando finché `DisplayServer.tts_get_voices()` è vuota su Android), cercando `voice["language"].to_lower().begins_with("it")` (`"it-IT"`, `"it_IT"`, `"ita-ITA"`).
  * Legge sempre `TextFormatter.strip_markers(text)` (pulito anche da eventuali BBCode); attiva ducking audio all'avvio e ripristina al termine. No-op sicuro in modalità headless.
  * **Igiene `TTSManager.stop()` obbligatoria:** chiamata automatica in `SceneRouter` (su ogni cambio scena/minigioco/Back/Pause), all'inizio di ogni nuova domanda, all'apertura e alla chiusura di `ExplanationModal` (se `auto_tts_questions == true`, legge la spiegazione all'apertura e si ferma su *"Ho capito!"*).

---

## 9. Specifiche Dettagliate delle Scene, Opzioni di Studio e Mini-Giochi

### 9.1 Boot & Profilo (`boot.tscn`, `profile_select.tscn`)

* `boot.tscn`: inizializza `SaveManager` e `PackManager` offline, lancia `PackManager.sync_from_github()` in background non bloccante se configurato (l'`HTTPRequest` vive in `PackManager`), instrada a `school_yard.tscn` o `profile_select.tscn`.
* `profile_select.tscn`: campo nome (validazione `name.strip_edges()`, lunghezza $1$–$16$ char; bottone disabilitato se vuoto), scelta tra 4 avatar animati, bottone *"Inizia l'avventura!"*.

### 9.2 Hub Scolastico (`HubBase`, `SchoolYard`, `SchoolHallway`) & `PackPickerModal`

* **Protezione anti double-tap & Safe Area:** ignorare tutti i tap su NPC, portali e pillola guida mentre `SceneRouter.is_transitioning == true` o una modale è aperta; applicare margini `SAFE_MARGIN_X = 16` e `SAFE_MARGIN_Y = 12` a tutti gli elementi `HubHud`.
* **Regola anti-loop `PortalZone`:** `SpawnPoint` sempre almeno $1$ tile ($16\text{ px}$) fuori dall'`Area2D` del portale + cooldown ingresso $0.4\text{ s}$.
* **Movimento GBA 4-direzioni & Tap diretto su NPC/Porte (`AStarGrid2D`):**
  * `astar.diagonal_mode = AStarGrid2D.DIAGONAL_MODE_NEVER` + `astar.get_id_path(start_cell, target_cell, true)` (`allow_partial_path = true`): toccare direttamente un NPC o una porta fa camminare il personaggio alla casella adiacente e attiva l'interazione.
  * Supporto parallelo a D-Pad virtuale opzionale + tastiera.
* **Targhette leggibili nel mondo:** ogni NPC e ogni porta mostra una piccola etichetta compatta ad alto contrasto (`"Storia"`, `"Scienze"`, `"Preside"`, `"Ingresso"`, `"Palestra"`, `"Armadietto"`, `"Laboratorio"`, `"Biblioteca"`).
* **Mappe, Spawn, NPC & Badge Verifica (`!`):**
  1. `SchoolYard` ($26 \times 18$ tile, spawn: `"default"`, `"from_hallway"`, `"from_npc_history"`, `"from_npc_science"`, `"from_npc_preside"`):
     * **Portale Nord (Ingresso Scuola)** $\rightarrow$ `school_hallway.tscn` (`"from_yard"`): mostra badge `!` se almeno un pacchetto attivo di `matematica` ha `priority == true`.
     * **Maestro Erodoto** (`subject == "storia"`): badge `!` se un pacchetto `storia` ha `priority == true`.
     * **Maestra Flora** (`subject == "scienze"`): badge `!` se un pacchetto `scienze` ha `priority == true`.
     * **Preside Stella** (regola Day-1 a 3 livelli, zero vicoli ciechi):
       1. Se esistono pacchetti attivi non-matematica con `priority == true` oppure materie `["geografia", "italiano", "inglese"]`, apre `PackPickerModal` con quelli.
       2. Altrimenti, se esiste almeno un pacchetto non-matematica con Boss sbloccato ($\ge 80\%$) senza medaglia, lo propone in `PackPickerModal` come *"Sfida del Preside"*.
       3. Altrimenti apre `DialogBox` incoraggiante: *"Benvenuto! Quando ci sarà una verifica importante o arriveranno nuove materie come Geografia e Italiano, potrai allenarti qui con me. Intanto prova a parlare con Maestro Erodoto o Maestra Flora!"*.
  2. `SchoolHallway` ($24 \times 14$ tile, spawn: `"from_yard"`, `"from_gym"`, `"from_locker"`):
     * Porta Sud $\rightarrow$ `school_yard.tscn` (`"from_hallway"`).
     * **Porta Palestra** (`subject == "matematica"` $\rightarrow$ `gym_rhythm`, badge `!` se un pacchetto `matematica` ha `priority == true`).
     * **Porta Armadietto & Spaccio** (`locker_room.tscn`).
     * Porte WIP (**Laboratorio**, **Biblioteca**): `DialogBox` *"Lavori in corso — Presto disponibile!"*.
* **Contratto Esplicito `PackPickerModal` (`pack_picker_modal.tscn`):**
  * Aperto sempre quando si parla con un NPC con pacchetti disponibili o si entra in Palestra (anche se c'è $1$ solo pacchetto).
  * Mostra **solo** pacchetti con almeno $1$ domanda giocabile compatibile con gli `allowed_types` del mini-gioco di destinazione.
  * Ogni scheda pacchetto mostra: titolo, badge colore materia, eventuale badge `VERIFICA! (x1.5)`, barra padronanza (`X%`), stato medaglia, bottone primario **"Allenati"** (sempre attivo) e bottone **"Sfida Boss: [boss_name]"** (disabilitato con lucchetto e sottotitolo *"Raggiungi 80% per sbloccare"* se $< 80\%$; attivo in oro se $\ge 80\%$, con etichetta *"Rivincita Boss"* se medaglia già vinta).
* **HUD Hub (`hub_hud.tscn`):**
  * Top-left: cornice profilo (`equipped_frame`), avatar, nome, **3 timbri stella settimanali** (`get_sessions_this_week()`).
  * Top-center (se `show_guidance_pill == true`): pillola cliccabile *"Sfida consigliata: [Titolo]"* (da `get_recommended_activity()`) $\rightarrow$ `ConfirmModal` *"Vuoi iniziare subito [Titolo]?"* $\rightarrow$ lancia direttamente il mini-gioco.
  * Top-right: Stelle, Medaglie, Mute rapido, Ingranaggio Area Genitori con **feedback visivo progressivo di riempimento ($0\% \to 100\%$ in $2.0\text{ s}$)** durante la pressione prolungata (annullata solo se il dito esce dai bounds del bottone) $\rightarrow$ **apre sempre `pin_modal.tscn`**.

### 9.3 Meccaniche Comuni di Supporto allo Studio nei Mini-Giochi (`BaseMinigame`)

Entrambi i mini-giochi ereditano da `BaseMinigame` e implementano **identicamente** questi 9 pilastri:
1. **Guardia `deck.is_empty()`:** se il deck costruito è vuoto, mostra `DialogBox` sicura (*"Questo argomento non ha domande pronte per questa aula!"*) e torna all'Hub senza crash.
2. **Protezione Anti Double-Tap (`_is_input_locked: bool`):** appena viene toccata una risposta o premuto `OK` valido, blocca immediatamente tutti i controlli di risposta fino al termine di `RESOLVE_TURN`.
3. **`ProgressStepper` ancorato agli $N$ pallini originali:** mostra sempre esattamente gli $N$ pallini delle domande uniche (`0..N-1`); non cresce mai su errori/recuperi. Stati: grigio = in attesa, blu = corrente, ambra (`#D97706`) = da riprovare, verde (`#059669`) = completata.
4. **Limite anti-loop recuperi (`MAX_RETRIES_PER_QUESTION = 2`):** dopo $2$ errori sulla stessa domanda nella medesima sfida, mostra `ExplanationModal`, tiene la domanda in `Box 0` per sessioni future e **non** la rimette più in coda in questa sessione.
5. **Pausa di lettura (`reading_pause_sec`):** all'apertura di ogni domanda, se `reading_pause_sec > 0.0`, i bottoni risposta, il `RetroNumpad` e il pulsante `50/50` restano disabilitati (`modulate.a = 0.65`) mentre una barra discreta si riempie. **Il pulsante TTS resta cliccabile fin dal millisecondo $0$**, e in `gym_rhythm` il timer della domanda parte **solo dopo la fine della pausa di lettura**.
6. **Pulsante `Indizio` (`icon_hint.png`, 1 volta per domanda):** visibile se `q.hint != ""` (o su `numeric_input` con fallback `"Suggerimento: il numero ha %d cifre"`). Apre un banner azzurro sotto la domanda con `TextFormatter.to_bbcode(q.hint)` e registra `used_help = true`.
7. **Pulsante `50/50` (`icon_5050.png`, controllato da Area Genitori con PIN):**
   * Visibile **ESCLUSIVAMENTE** se `SaveManager.config["study_preferences"]["enable_50_50"] == true` **E** la domanda corrente è `multiple_choice`. Con `enable_50_50 == false` (default) resta `visible = false`.
   * Utilizzabile $1$ volta per domanda (dopo la pausa di lettura): disabilita e attenua (`disabled = true`, `modulate.a = 0.25`) esattamente $2$ opzioni sbagliate casuali, registrando `used_help = true` senza togliere HP.
8. **Feedback Emotivo Gentile, `ExplanationModal` & `reduced_motion`:** bordo ambra caldo (`#D97706`), intestazione da `ENCOURAGING_ERROR_HEADERS`, parole chiave evidenziate, TTS con `TTSManager.stop()` su chiusura, *"Ho capito!"* sbloccato dopo $0.8\text{ s}$; se `reduced_motion == true`, niente shake o flash rapidi.
9. **Politica di Uscita Anticipata (tasto Esci o Back + conferma):**
   * Chiama `TTSManager.stop()`.
   * Mantiene salvati tutti i progressi Leitner delle risposte già date e accredita le stelle delle sole risposte corrette già date (approccio non punitivo), chiamando `SaveManager.flush_all()`.
   * **Non** assegna bonus completamento (`+25`), medaglia Boss, timbro settimanale né incrementa `sessions_in_current_run`.

### 9.4 Mini-Gioco 1: Cortile — Scontro Pokémon GBA (`yard_battle.tscn`)

* **Tipi:** `multiple_choice` (sempre mescolate con `get_shuffled_options`), `true_false`.
* **Budget Verticale Fisso a $1280 \times 800$ (Zero Overflow con testi massimi $140$/$90$/$45$ char):**
  * Top bar (`ProgressStepper` + esci): `64 px`.
  * Arena sprite + barre HP: `250 px` (~31%).
  * Area domanda + banner `Indizio` + griglia $2 \times 2$ risposte: `486 px` (~61%), con `autowrap_mode = TextServer.AUTOWRAP_WORD_SMART` e `custom_minimum_size.y = 84` px su tutti i bottoni opzione.
* **Dimensione & HP:**
  * `base_len = SaveManager.get_session_length()` ($4$, $6$ o $8$); `target = base_len + BOSS_EXTRA_QUESTIONS` se `is_boss` else `base_len`.
  * `deck = LearningEngine.build_session_deck(pack_id, target, ["multiple_choice", "true_false"])`.
  * `enemy_max_hp = deck.size() * GameBalance.BASE_ATTACK_DAMAGE`.
* **Stati (`INTRO` $\rightarrow$ `CHOOSE_MOVE` $\rightarrow$ `SHOW_QUESTION` $\rightarrow$ `RESOLVE_TURN` $\rightarrow$ `END_CHECK`):**
  * **`CHOOSE_MOVE` (senza exploit né soft-lock):** *Attacco Base* ($20$ danni, estrae `difficulty == 1`) vs *Mossa Speciale* ($36$ danni, estrae `difficulty in [2, 3]`). Se nella coda residua manca una fascia di difficoltà, il relativo bottone viene disabilitato con sottotitolo esplicito (*"Solo attacchi base rimasti"* / *"Solo mosse speciali rimaste"*).
  * **KO Anticipato & `END_CHECK`:**
    * Se `enemy_hp <= 0` prima della fine del deck, i pallini rimanenti dello stepper passano a completati, la partita termina subito con **vittoria** e `MinigameResult.total_questions` riporta il conteggio esatto delle domande uniche effettivamente affrontate (`answered_unique_count`, es. `3 / 3`).
    * Sconfitta incoraggiante se `player_hp <= 0` (salva Leitner e stelle risposte già date).
    * Vittoria Boss se `enemy_hp <= 0` o (coda esaurita con `player_hp > 0` E accuratezza primo tentativo $\ge 75\%$). Prima vittoria Boss assegna `STARS_BOSS_VICTORY_BONUS` ($+60$) + Medaglia; rivincita Boss assegna `STARS_SESSION_COMPLETE` ($+25$).

### 9.5 Mini-Gioco 2: Palestra — Sfida a Ritmo (`gym_rhythm.tscn`)

* **Tipi:** `numeric_input`, `multiple_choice` (mescolate; obbligatorio per le frazioni), `true_false`.
* **Dimensione:** `base_len + BOSS_EXTRA_QUESTIONS` se `is_boss` else `base_len` (clamped a `deck.size()`).
* **Layout & Controlli:**
  * Pista $2$ corsie + `ProgressStepper` fisso a $N$ pallini + badge Combo / *"Ritmo Calmo"* in alto; domanda + TTS + `Indizio` + (se attivo in Area Genitori e `multiple_choice`) `50/50` a sinistra; `RetroNumpad` custom o bottoni scelta a destra. Mai aprire tastiera Android.
  * **Timer sincronizzato con la Pausa di Lettura:** se `calm_mode == false`, la barra del timer rimane ferma al $100\%$ durante `reading_pause_sec` e inizia a scendere **solo** allo sblocco dei comandi di risposta.
  * **Layout esatto `RetroNumpad` (`res://ui/common/retro_numpad.tscn`):** griglia $4 \times 4$, ogni tasto $\ge 68 \times 68\text{ px}$:
    * Riga 1: `7` | `8` | `9` | `⌫`
    * Riga 2: `4` | `5` | `6` | `C`
    * Riga 3: `1` | `2` | `3` | `,` (max $1$ volta)
    * Riga 4: `0` (span $2$ colonne) | `OK` (span $2$ colonne)
    * Limite massimo: `GameBalance.NUMPAD_MAX_CHARS` ($8$ caratteri).
    * **Guardia `OK` su input vuoto:** se il campo è `""`, `","` o `"."`, il tap su `OK` viene ignorato con un leggero pulse neutro del box input e **mai** conteggiato come errore.
* **Validazione Numerica & Esito:**
  * Normalizza `,` in `.`, converte a `float` e verifica `abs(user_val - q.correct_value) <= max(q.tolerance, GameBalance.NUMERIC_MIN_EPSILON)`.
  * Timer scaduto non segna errore (*"Prenditi il tuo tempo!"*); rispetta `MAX_RETRIES_PER_QUESTION = 2`; vittoria Boss richiede tutte le tappe completate E accuratezza primo tentativo $\ge 75\%$.

### 9.6 `ResultSummaryModal` + Micro-Pausa Ricarica

Mostra esito incoraggiante, conteggio `correct_first_try / total_questions`, Stelle guadagnate (con eventuale badge `x1.5 Verifica!`), barra padronanza `prima -> dopo`, eventuale Medaglia sbloccata e, se `result.suggest_micro_break == true`, un riquadro verde gentile: *"Hai fatto 2 sfide di fila, grande! Prenditi 2 minuti di pausa per muoverti e bere un bicchiere d'acqua, poi torna quando vuoi."*

### 9.7 Stanza Armadietto, Bacheca Medaglie & Spaccio (`locker_room.tscn`)

3 tab GBA + *"Torna in corridoio"* (`school_hallway.tscn`, spawn `"from_locker"`):
1. **Bacheca Medaglie:** mostra tutte le medaglie dei pacchetti attivi e le medaglie già conquistate anche di eventuali pacchetti archiviati.
2. **Il Mio Armadietto:** $6$ slot adesivi cliccabili (al tocco apre mini-selettore con tutti gli adesivi posseduti + opzione **"Rimuovi"**; uno stesso adesivo può essere usato su più slot), selettore Mascotte e selettore Cornice.
3. **Spaccio Ricompense (`shop_catalog.json`):** bottone acquisto protetto da lock anti double-tap durante la transazione.

### 9.8 Area Genitori — Accesso Rigorosamente Protetto da PIN (`pin_modal.tscn` & `parent_area.tscn`)

* **Protezione PIN & Tastierino Custom (`pin_modal.tscn`):**
  * `parent_area.tscn` è accessibile **esclusivamente** superando `pin_modal.tscn`.
  * **ZERO tastiera Android nativa:** `pin_modal.tscn` usa un tastierino numerico custom on-screen ($0$–$9$, `⌫`, `Annulla`, $4$ pallini mascherati `••••`).
  * **Nota PIN Giorno 1:** mostra una piccola dicitura grigia *"PIN iniziale: 1234 (modificabile nelle Impostazioni)"* **solo** finché `SaveManager.config["parent_pin"] == "1234"`.
* **Linguaggio 100% neutro:** nessun riferimento clinico o sigla in nessun tab, etichetta o messaggio (Regola 0.1).
* **Tab 1 — Progressi:** padronanza pacchetti, *"Da ripassare (Top 5)"*, ultime $10$ sessioni.
* **Tab 2 — Pacchetti & Sync:** stato sync + *"Sincronizza ora"* (disabilitato con *"Connessione in corso..."* mentre `PackManager.is_syncing == true`), **"Incolla JSON dagli appunti"** (primario), **"Importa da file .json"** (secondario), toggle `priority`, **"Elimina"** su pacchetti `local`, log errori validazione/sync (sempre senza PAT).
* **Tab 3 — Impostazioni (dentro `ScrollContainer` con `follow_focus = true` e margine inferiore extra $\ge 320\text{ px}$ per tastiera Android landscape):**
  * Pulsante evidenziato **"Attiva Profilo Studio Sereno"** (sottotitolo: *"Sfide brevi, senza timer, lettura vocale e pausa di lettura"*) che imposta in un tocco: `calm_mode = true`, `reduced_motion = true`, `reading_pause_sec = 1.0`, `auto_tts_questions = true`, `session_length = 4`, `show_guidance_pill = true` (toast: *"Profilo Studio Sereno attivato! Puoi regolare ogni opzione qui sotto."*). Lascia invariato `enable_50_50`.
  * Controlli individuali:
    * Toggle **`Attiva aiuto 50/50 nelle domande a scelta multipla`** (`enable_50_50`, default `false`).
    * Selettore **Lunghezza sfida**: `Breve (4)` / `Standard (6)` / `Estesa (8)`.
    * Selettore **Pausa di lettura prima di rispondere**: `Spenta (0s)` / `Breve (1s)` / `Media (2s)`.
    * Toggle: *Modalità Ritmo Calmo (senza timer)*, *Riduci animazioni e scosse*, *Voce TTS automatica sulle domande*, *Mostra guida "Sfida consigliata" nell'Hub*, *Mostra D-Pad virtuale*.
  * Form GitHub (`owner`, `repo`, `branch`, `base_path`, `pat` mascherato con pulsante dedicato **"Incolla PAT dagli appunti"**) + *"Salva e testa connessione"* (disabilitato durante `is_syncing`).
  * Slider audio `Music`/`SFX`, gestione profili (`list_profiles`, cambia, crea nuovo, **Reset progressi** con doppia conferma) e cambio PIN (tramite `pin_modal.tscn` custom).

---

## 10. Contenuti Didattici Bundled (`res://packs/`)

Creare 4 file validati senza errori da `PackValidator`, per **4ª e 5ª elementare italiana**, includendo **parole chiave `**...**`** e il campo **`hint`** su tutte le domande statiche (senza alcuna terminologia vietata dalla Regola 0.1):
1. `res://packs/manifest.json`
2. `res://packs/storia_egizi.json` (12 domande: `9 multiple_choice` + `3 true_false`, tutte con `hint` e parole chiave `**...**`).
3. `res://packs/scienze_fotosintesi.json` (12 domande: `9 multiple_choice` + `3 true_false`, tutte con `hint` e parole chiave `**...**`).
4. `res://packs/matematica_calcolo_base.json` (8 domande statiche con `hint`, usando `multiple_choice` per le frazioni e `numeric_input` con `correct_value >= 0` per equivalenze/operazioni + 2 generatori `gen_tabelline_6_9` e `gen_calcolo_mentale` da `10` domande ciascuno).

---

## 11. Suite di Test Automatici Headless (`res://tests/run_tests.gd`)

Script `extends SceneTree` isolato in `user://test_sandbox/`, exit code `0` se tutti `[PASS]`:

1. **`PackValidator` & `TextFormatter`:**
   * `validate_manifest`: accetta `res://packs/manifest.json`; rifiuta `file` con `../secret.json`, `/etc/passwd`, `\evil.json` o `pack_id` duplicati.
   * `validate_pack`: pacchetti bundled validi con $0$ errori; `strip_markers` e `to_bbcode` (con escape `[` sicuro); rifiuto `prompt > 140`, `hint > 90`, opzioni duplicate, `correct_index = 4`, `explanation` vuota, **`numeric_input` con `correct_value < 0`**; warning non bloccante su `classification` e su pacchetto non-matematica con $< 5$ domande `multiple_choice`/`true_false`.
2. **`MathGenerator`:** determinismo identico, zero negativi (`correct_value >= 0`), `hint` popolato, validità `PackValidator`.
3. **`LearningEngine`:** Leitner `0 -> 3`, reset `0` su errore, regola `used_help == true` (non promuove a `2`/`3`), padronanza su `playable_questions` (anche per pacchetti con $0$ domande statiche e soli generatori, senza divisione per zero), Boss $80\%$, `get_top_weak_questions`, `get_shuffled_options`, `get_recommended_activity`, settimana solare lunedì `00:00`, contatore `suggest_micro_break` ogni $2$ sessioni.
4. **`SaveManager`:** scrittura atomica cross-platform + `.bak` + recovery su corruzione; merge sicuro default `study_preferences` con verifica `enable_50_50 == false`; `list_profiles`, `create_profile`, `reset_active_profile_progress`.
5. **`PackManager`:** caricamento bundled tramite `res://packs/manifest.json`, `import_pack_from_json_text`, precedenza `local > cache > bundled`, `archived: true` preserva medaglie, staging corrotto non tocca cache, mutex `is_syncing`, `build_github_contents_url` con `base_path = ""`, `"/packs/"`, `" packs "` (zero doppi slash `//`), verifica assenza PAT in messaggi/errori sync.
6. **Bilanciamento, `50/50`, Anti-Loop, Epsilon & Boss:** HP dinamici per `session_length` $4/6/8$, logica helper `50/50` ($2$ errate distinte, mai `correct_display_index`), limite `MAX_RETRIES_PER_QUESTION = 2`, fallback disabilitazione mossa in `yard_battle`, validazione numerica con epsilon `NUMERIC_MIN_EPSILON` (`0.3` / `,`), guardia `deck.is_empty()`, prima vittoria Boss (`+60` + medaglia) vs rivincita Boss (`+25`, no medaglia duplicata), regola accuratezza Boss $\ge 75\%$ in `gym_rhythm`.
7. **Privacy Guard (Regola 0.1 — OBBLIGATORIO):** scansione ricorsiva case-insensitive di `res://project.godot` e di tutti i file `.gd`, `.tscn`, `.json`, `.md` dentro `res://core/`, `res://data/`, `res://packs/`, `res://ui/`, `res://scenes/`, `res://minigames/`, `res://tools/` (e `res://github_packs_repo_template/` se presente; escludere esplicitamente `res://tests/run_tests.gd` stesso) verificando con regex:
   ```text
   (?i)(\b(adhd|bes|dsa|dislessia|dislessico|discalculia|discalculico|disturbo|disturbi|terapia|terapie|compensativo|compensativi|clinico|clinica|diagnosi|iperattivita|iperattività|neurodivergente)\b|bisogni\s+speciali)
   ```
   che esistano **ZERO occorrenze** (altrimenti fallisce con file + riga ed exit code `1`).
8. **Export Config Guard (attivo appena esiste `res://export_presets.cfg`):** verifica che `res://export_presets.cfg` contenga:
   * `*.json` dentro `include_filter`;
   * `permissions/internet=true` e `permissions/access_network_state=true`;
   * `tests/*`, `tools/*` e `github_packs_repo_template/*` dentro `exclude_filter`.

---

## 12. Piano di Esecuzione a Milestone (Ordine Sequenziale)

Completare una milestone alla volta e verificarne la **Definition of Done (DoD)** prima di procedere.

### Milestone 1 — Bootstrap, Core Singletons, Placeholder Assets e Risorse
* **Task:** `project.godot`, `default_bus_layout.tres`, Autoload, `GameBalance`, `EventBus`, `TextFormatter`, `SaveManager` (scrittura atomica cross-platform + gestione profili + reset), `AudioManager` (loop `.wav` mono/stereo + `.ogg` + ducking TTS + pool SFX), `TTSManager` (risoluzione lazy Android `"it"`), `SceneRouter` + `SceneTransition` (`TTSManager.stop()` + lifecycle Android/Back), `generate_placeholders.gd`, `build_resources.gd`, `shop_catalog.json`.
* **DoD:** `generate_placeholders.gd` e `build_resources.gd` terminano con exit `0`; tutti gli asset PNG, WAV, `school_tileset.tres` e `gba_theme.tres` sono presenti e caricabili senza errori.

### Milestone 2 — Motore Dati, PackValidator, Sync GitHub, Leitner e Test Headless
* **Task:** Modelli in `core/models/`, `PackValidator` (`validate_manifest`, `correct_value >= 0`, warning compatibilità), `MathGenerator`, `PackManager` (manifest bundled + `HTTPRequest` sull'Autoload + guardia sincrona + `build_github_contents_url` + mutex `is_syncing` + redazione PAT), `LearningEngine` (`playable_questions`, `used_help`, guida consigliata, micro-pausa), 3 pacchetti bundled + `manifest.json`, `res://tests/run_tests.gd` (gruppi 1–7 verdi).
* **DoD:** `godot --headless --script res://tests/run_tests.gd` termina con exit `0` e tutti i test `[PASS]`.

### Milestone 3 — Boot, Creazione Profilo e Hub Scolastico Navigabile
* **Task:** `Boot`, `ProfileSelect`, `HubHud` (Safe Area, 3 timbri settimanali, pillola *"Sfida consigliata"*, long-press Area Genitori con barra progresso visiva $\rightarrow$ `pin_modal.tscn`), `VirtualDpad`, `DialogBox`, `ConfirmModal`, `PackPickerModal` (Allenati vs Sfida Boss + filtro `allowed_types`), `HubBase` (`DIAGONAL_MODE_NEVER` + `allow_partial_path = true` + lock su transizione/modali), `PlayerController`, `CompanionFollower`, `InteractableNPC` (targhette + regola 3-livelli Preside Stella Day-1), `PortalZone` (targhette + badge `!` su Ingresso Scuola per matematica), `SchoolYard`, `SchoolHallway`.
* **DoD:** Esplorazione fluida cortile/corridoio senza loop portali, tap diretto su NPC/porte funzionante, Preside Stella Day-1 gestita senza schermate vuote, `PackPickerModal` funzionante, test headless verdi.

### Milestone 4 — Mini-Gioco 1: Cortile (`yard_battle`) + Supporti Condivisi
* **Task:** `BaseMinigame` (guardia `deck.is_empty()`, `_is_input_locked`, `MAX_RETRIES_PER_QUESTION = 2`, uscita anticipata non punitiva, igiene `TTSManager.stop()`), `ProgressStepper` (fisso a $N$ pallini + chiusura pulita su KO anticipato), `ExplanationModal`, `ResultSummaryModal`, `yard_battle.tscn/.gd` (budget verticale `64/250/486 px`, pausa di lettura, `Indizio`, `50/50` condizionale, fallback mossa, `reduced_motion`, shuffle opzioni, HP dinamici, prima vittoria Boss vs rivincita).
* **DoD:** Flusso end-to-end completo con verifica double-tap, KO anticipato, `enable_50_50` off/on, uscita anticipata, pausa di lettura e salvataggio Leitner; test headless verdi.

### Milestone 5 — Mini-Gioco 2: Palestra (`gym_rhythm`) + Tastierino Custom
* **Task:** `RetroNumpad` (griglia $4 \times 4$, limite $8$ char, max $1$ virgola, guardia `OK` su vuoto), `gym_rhythm.tscn/.gd` (timer che parte **dopo** `reading_pause_sec`, validazione epsilon `NUMERIC_MIN_EPSILON`, `ProgressStepper` fisso, `MAX_RETRIES_PER_QUESTION = 2`, `Indizio`, `50/50` su `multiple_choice`, pista, combo, `calm_mode`, `reduced_motion`, Boss $\ge 75\%$).
* **DoD:** Flusso end-to-end Corridoio $\rightarrow$ Palestra con tastierino custom, verifica sincronizzazione pausa lettura/timer, `OK` su vuoto ignorato, `calm_mode` e salvataggio; test headless verdi.

### Milestone 6 — Stanza Armadietto, Bacheca Medaglie e Spaccio Ricompense
* **Task:** `locker_room.tscn/.gd` (3 tab, mini-selettore 6 slot adesivi con *"Rimuovi"*, lock anti double-tap sugli acquisti), sincronizzazione live con `CompanionFollower` e `HubHud`.
* **DoD:** Acquisto/equipaggiamento mascotte, adesivi e cornici visibili subito e persistenti al riavvio; test headless verdi.

### Milestone 7 — Area Genitori Completa (PIN Custom On-Screen + 3 Tab + Studio Sereno + Toggle 50/50)
* **Task:** `pin_modal.tscn/.gd` (tastierino numerico on-screen senza tastiera Android + nota PIN iniziale `1234` visibile solo finché non modificato), `parent_area.tscn/.gd` (Progressi + Top 5, Pacchetti & Sync con bottoni disabilitati durante `is_syncing` + Incolla JSON + File + Elimina locale + Priority, Impostazioni dentro `ScrollContainer` `follow_focus = true` con margine inferiore $\ge 320\text{ px}$ + **1-tap Profilo Studio Sereno** + **toggle `Attiva aiuto 50/50`** + **"Incolla PAT dagli appunti"** + Test GitHub + Profili + Reset con doppia conferma + Cambio PIN).
* **DoD:** Accesso negato con PIN errato; nessuna tastiera Android su `pin_modal`; form GitHub scorrevole e usabile in landscape; toggle `50/50` e Profilo Studio Sereno persistono e aggiornano live i mini-giochi; test headless verdi (zero termini vietati).

### Milestone 8 — Export Android, Verifica Layout Tablet 9" e Consegna QA
* **Task:** Configurare `export_presets.cfg` Android con tutti i parametri obbligatori:
  * `package/unique_name="it.scuolaretro.pilota1"`, `package/name="Scuola Retro"`
  * `architectures/arm64-v8a=true`, `gradle_build/min_sdk=26`, `gradle_build/target_sdk=34`
  * `permissions/internet=true`, `permissions/access_network_state=true`
  * `screen/immersive_mode=true`, `screen/orientation=0`
  * `include_filter="*.json"`
  * `exclude_filter="tests/*,tools/*,github_packs_repo_template/*,*.md"`
  * Verificare zero overflow a $1280 \times 800$ e $1340 \times 800$ con stringhe al limite massimo ($140$/$45$/$160$/$90$ char).
  * Eseguire `res://tests/run_tests.gd` (tutti gli 8 gruppi inclusi Privacy Guard ed Export Config Guard).
  * Creare `BUILD_ANDROID.md` con istruzioni compilazione APK + **Checklist QA sul campo per i 2 Galaxy Tab $9\text{"}$**:
    1. *Offline cold start* (Modalità Aereo $\rightarrow$ avvio $\rightarrow$ $3$ pacchetti giocabili, zero crash).
    2. *Sync GitHub + fallback offline* (sync Wi-Fi $\rightarrow$ Modalità Aereo $\rightarrow$ riavvio $\rightarrow$ pacchetti in cache intatti).
    3. *TTS Samsung + Audio Ducking* (tasto altoparlante $\rightarrow$ voce italiana chiara e musica che si attenua).
    4. *Stress test tastiera & orientamento* (Area Genitori $\rightarrow$ *"Incolla PAT"* $\rightarrow$ campo visibile sopra tastiera e landscape bloccato).
    5. *Double-tap, Back & Home* (5 tap rapidi su risposta, tasto *Indietro* Android in battaglia, tasto *Home* a metà sfida con salvataggio preservato al rientro).
* **DoD:**
  * **Obbligatoria sempre:** `export_presets.cfg` conforme, tutti gli 8 gruppi test verdi (`exit 0`), zero errori critici in `godot --headless --quit`, `BUILD_ANDROID.md` completo.
  * **Condizionale (se Android SDK + template export presenti):** compilazione `build/scuola_retro_debug.apk` valida.

---

## 13. Appendice — Prompt Pronto per Trasformare Pagine del Sussidiario in JSON

Usa questo prompt con un LLM multimodale allegando la foto o il testo di $1$–$2$ pagine del sussidiario di 4ª/5ª elementare:

```text
Sei un insegnante esperto di scuola primaria italiana (classe 4ª/5ª), specializzato in chiarezza espositiva e didattica efficace.
Trasforma il contenuto delle pagine allegate in un pacchetto JSON didattico valido per l'app "Scuola Retro".

REGOLE RIGOROSE (non violarne nessuna):
1. Restituisci SOLO JSON valido, senza testo prima o dopo e senza blocchi markdown.
2. Usa esattamente schema_version = 1.
3. Genera 12 domande: 9 di tipo "multiple_choice" e 3 di tipo "true_false", distribuite tra difficulty 1 (4 domande), difficulty 2 (4 domande), difficulty 3 (4 domande).
4. Lingua: italiano semplice, frasi brevi e dirette, adatte a bambini di 9-11 anni (evita doppie negazioni e trabocchetti linguistici).
5. Evidenzia 1 o 2 parole chiave importanti per ogni prompt e ogni explanation racchiudendole tra doppi asterischi (es. **fiume**, **fotosintesi**).
6. Includi SEMPRE il campo "hint" su ogni domanda: un piccolo indizio incoraggiante che aiuta a ragionare senza dire subito la risposta.
7. Limiti lunghezza OBBLIGATORI (esclusi i marcatori **):
   - prompt <= 140 caratteri
   - hint <= 90 caratteri
   - ogni opzione <= 45 caratteri (esattamente 4 opzioni tutte diverse per multiple_choice)
   - explanation <= 160 caratteri (tono gentile e incoraggiante)
8. pack_id e id delle domande solo lettere minuscole, numeri e underscore (es. "sto_greci_001").
9. subject deve essere uno tra: "storia", "geografia", "scienze", "matematica", "italiano", "inglese".
10. Per domande di matematica sulle frazioni (es. "3/4"), usa sempre "multiple_choice", mai "numeric_input". Per "numeric_input" usa solo numeri >= 0.
11. Inventa boss_name, boss_title e medal_name coerenti e divertenti per l'argomento.

Struttura JSON richiesta:
{
  "schema_version": 1,
  "pack_id": "...",
  "subject": "...",
  "grade": 4,
  "title": "...",
  "version": 1,
  "boss_name": "...",
  "boss_title": "...",
  "medal_name": "...",
  "medal_icon": "medal_default",
  "questions": [ ... ]
}
```

---

## 14. Ultimissimo Punto (Da Eseguire SOLO a App Completata) — Template Repository GitHub Privato & Validatore CLI

> **Vincolo di sequenza:** Non iniziare questa sezione prima che tutte le Milestone $1$–$8$ siano completate e con test verdi. Questo deliverable prepara l'infrastruttura esterna pronta da caricare sul repository GitHub privato del genitore.

### 14.1 Cartella Pronta all'Uso `res://github_packs_repo_template/`

Creare una cartella autonoma pronta da inizializzare come repository GitHub privato separato (`scuola-retro-packs`):

```text
github_packs_repo_template/
├── README.md
└── packs/
    ├── manifest.json
    ├── storia_egizi.json
    ├── scienze_fotosintesi.json
    └── matematica_calcolo_base.json
```

* I file dentro `github_packs_repo_template/packs/` devono essere copiati dai pacchetti bundled già validati (versione `1`), pronti come base per i primi test di sync reale (basterà incrementare `version` a `2` o attivare `priority: true` sul repo per verificare il download sul tablet).
* **`github_packs_repo_template/README.md`** (in italiano, chiaro, operativo e conforme alla Regola 0.1 di privacy) deve contenere:
  1. Istruzioni passo-passo per creare il **repository privato** su GitHub e caricare la cartella `packs/`.
  2. Istruzioni esatte per generare un **Fine-grained Personal Access Token** su GitHub con permessi minimi sicuri:
     * *Repository access:* `Only select repositories` $\rightarrow$ selezionare solo il repo dei pacchetti;
     * *Permissions $\rightarrow$ Repository permissions $\rightarrow$ Contents:* **`Read-only`** (nessun altro permesso).
  3. Come inserire `owner`, `repo`, `branch` (`main`), `base_path` (`packs`) e `PAT` (usando il tasto *"Incolla PAT dagli appunti"*) nell'Area Genitori $\rightarrow$ Tab Impostazioni sui 2 Galaxy Tab e premere *"Salva e testa connessione"*.
  4. Flusso rapido settimanale per aggiungere un nuovo capitolo dal sussidiario:
     * fotografare la pagina $\rightarrow$ generare il JSON col prompt della Sezione 13 $\rightarrow$ validarlo (opzionalmente con `validate_pack.gd`) $\rightarrow$ caricarlo in `packs/` su GitHub $\rightarrow$ aggiungere la voce in `packs/manifest.json` (con `"priority": true` nella settimana della verifica).
  5. Come testare il **fallback offline**: fare un sync con Wi-Fi acceso, attivare la Modalità Aereo sul Galaxy Tab, riavviare l'app e verificare che tutti i pacchetti sincronizzati restino giocabili senza errori bloccanti.

### 14.2 Script Validatore CLI (`res://tools/validate_pack.gd`)

Creare uno script eseguibile da terminale sul PC del genitore prima di fare push su GitHub:
```bash
godot --headless --script res://tools/validate_pack.gd -- percorso/al/nuovo_pacchetto.json
```
* Riusa esattamente `PackValidator.validate_pack(...)`.
* Stampa `[OK] Pacchetto valido (N domande giocabili, M generatori)` con exit code `0`, oppure l'elenco puntato leggibile in italiano di ogni errore/warning (es. lunghezza superata, opzione duplicata, numero negativo su `numeric_input`, campo mancante) con exit code `1`.

### DoD Finale (Sezione 14)
* `github_packs_repo_template/` contiene `README.md`, `packs/manifest.json` e i 3 pacchetti iniziali.
* `godot --headless --script res://tools/validate_pack.gd -- res://github_packs_repo_template/packs/storia_egizi.json` restituisce `[OK]` con exit code `0`.
* `godot --headless --script res://tests/run_tests.gd` resta 100% verde (`exit 0`), inclusi il Test 7 (Privacy Guard) e il Test 8 (Export Config Guard).