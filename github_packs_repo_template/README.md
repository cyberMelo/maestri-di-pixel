# Maestri di Pixel — Template Repository Privato Pacchetti Didattici

Questa cartella contiene la base pronta all'uso per creare il tuo **repository GitHub privato** (es. `maestri-di-pixel-packs`) da cui l'app **Maestri di Pixel** scarica automaticamente nuovi capitoli, verifiche settimanali e aggiornamenti sui tablet.

---

## 1. Creare il repository privato su GitHub

1. Accedi al tuo account GitHub e seleziona **New repository**.
2. Scegli un nome (es. `maestri-di-pixel-packs`) e imposta la visibilità su **Private**.
3. Carica all'interno del repository questa cartella `packs/` completa di `manifest.json` e dei pacchetti delle materie (`storia`, `matematica`, `scienze`, `italiano`, `geografia`, `inglese`).

---

## 2. Generare un Fine-grained Personal Access Token (permessi minimi)

Per la massima sicurezza, crea un token con **soli permessi di lettura** sul singolo repository dei pacchetti:

1. Su GitHub vai su **Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token**.
2. Dai un nome descrittivo (es. `MaestriDiPixelTablet`) e scegli la durata.
3. In **Repository access**, seleziona **Only select repositories** e scegli **solo** `maestri-di-pixel-packs`.
4. In **Permissions → Repository permissions**, imposta:
   - **Contents:** **`Read-only`**
   - Lascia tutti gli altri permessi su *No access*.
5. Genera il token e copialo negli appunti (inizia con `github_pat_...`).

---

## 3. Configurare i tablet in Area Genitori

1. Apri **Maestri di Pixel** sul tablet e tieni premuto per **2 secondi** l'icona ingranaggio in alto a destra.
2. Inserisci il PIN (iniziale `1234`, poi cambialo subito dalle Impostazioni).
3. Vai nella scheda **Impostazioni → Repository GitHub Privato (Pacchetti)**:
   - **Owner GitHub:** il tuo username GitHub
   - **Repository:** `maestri-di-pixel-packs`
   - **Branch:** `main`
   - **Cartella:** `packs`
   - **Token GitHub (PAT):** usa il pulsante **Incolla PAT**
4. Tocca **Salva e testa connessione**: vedrai il numero di pacchetti trovati nel `manifest.json`.
5. Nella scheda **Pacchetti & Sync**, tocca **Sincronizza ora**.

---

## 4. Flusso rapido settimanale per aggiungere un nuovo capitolo

1. Trasforma 1–2 pagine del sussidiario in un file `.json` (`schema_version = 1`).
2. Verifica il file sul PC prima del caricamento usando lo script incluso:
   ```bash
   python3 validate_pack.py packs/nuovo_capitolo.json
   ```
3. Carica il file nella cartella `packs/` su GitHub.
4. Aggiungi la nuova voce in `packs/manifest.json` (imposta `"priority": true` nella settimana della verifica per attivare il badge `!` nell'Hub e il bonus `x1.5` stelle; quando modifichi un file esistente incrementa `"version"` di 1).

---

## 5. Testare il funzionamento offline (Modalità Aereo)

1. Esegui una sincronizzazione con Wi-Fi acceso (puoi provare cambiando `"version": 2` o `"priority": true` su uno dei pacchetti nel repository).
2. Attiva la **Modalità Aereo** sul tablet.
3. Chiudi completamente e riavvia **Maestri di Pixel**.
4. Verifica che tutti i pacchetti scaricati restino disponibili e giocabili senza errori.
