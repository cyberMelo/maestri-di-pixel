# Maestri di Pixel — Guida Compilazione Android e Checklist QA su Tablet 9"

## 1. Compilazione dell'APK / AAB

L'applicazione è sviluppata in **Kotlin + Jetpack Compose** (Android nativo).

- **Debug APK (test diretto sui Galaxy Tab 9"):**
  ```bash
  gradle :app:assembleDebug
  ```
- **Test automatici JVM + Robolectric (tutti gli 8 gruppi di verifica):**
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Esportazione da AI Studio:** utilizza il menu impostazioni per scaricare l'APK/AAB oppure esportare lo ZIP del progetto per Android Studio.

---

## 2. Checklist QA sul Campo per i 2 Galaxy Tab 9"

1. **Offline cold start (Modalità Aereo):**
   - Attiva la Modalità Aereo sul tablet e avvia **Maestri di Pixel** da zero.
   - Verifica creazione profilo, ingresso nel Cortile e accesso ai 6 pacchetti integrati delle materie senza alcun errore bloccante.
2. **Sync GitHub + fallback offline:**
   - Con Wi-Fi attivo avvia l'app: la sincronizzazione parte da sola in background. In **Area Genitori → Pacchetti & Sync** (tieni premuto 2 secondi sull'ingranaggio, PIN iniziale `1234`) verifica l'esito; *Sincronizza ora* la ripete a mano.
   - Attiva la Modalità Aereo, chiudi e riavvia l'app: verifica che tutti i pacchetti sincronizzati restino giocabili dalla cache locale.
3. **TTS italiano + Audio Ducking:**
   - Avvia una sfida in Cortile o Palestra e tocca il pulsante Altoparlante.
   - Verifica che la voce italiana legga chiaramente domanda e spiegazione senza pronunciare asterischi (`**`) e che la musica di sottofondo si attenui automaticamente durante la lettura.
4. **Stress test tastiera & orientamento:**
   - In **Area Genitori → Impostazioni**, in orientamento orizzontale, verifica che la pagina scorra fluidamente e che il `PinModal` usi esclusivamente il tastierino numerico integrato.
5. **Double-tap, Back & Home:**
   - Tocca rapidamente più volte la stessa risposta (verifica assenza di doppie valutazioni).
   - Premi il tasto *Indietro* di Android durante una sfida (verifica modale di conferma e uscita non punitiva con salvataggio dei progressi già ottenuti).
   - Premi *Home* a metà sfida e rientra nell'app verificando che TTS si interrompa e i salvataggi restino integri.
