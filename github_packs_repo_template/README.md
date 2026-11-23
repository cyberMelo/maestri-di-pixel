# Maestri di Pixel — Pacchetti Didattici

Repository dei pacchetti di domande che l'app **Maestri di Pixel** scarica sui tablet. Si aggiungono capitoli e verifiche man mano, senza ricompilare l'app.

---

## Come arrivano i pacchetti sul tablet

- Il repository è **fisso nell'app** (`cyberMelo/maestri-di-pixel-packs`, branch `main`, cartella `packs/`): sul tablet non c'è niente da configurare.
- Se il repository è **privato**, l'APK deve essere compilato con un token di sola lettura (vedi sotto); se è pubblico non serve niente.
- **A ogni avvio** l'app sincronizza in background. Senza rete usa i pacchetti già scaricati e continua a funzionare.
- **Vengono scaricati solo i file cambiati**: l'app confronta l'impronta (SHA Git) di ogni file con quella dell'ultimo download. Basta caricare il file modificato, non serve alzare `version`.
- Per forzare subito un aggiornamento: **Area Genitori** (tieni premuto 2 secondi l'ingranaggio, PIN iniziale `1234`) → **Pacchetti & Sync** → **Sincronizza ora**. Lì compaiono anche esito e avvisi.

---

## Token di lettura (solo per repository privato)

1. Su GitHub: **Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token**.
2. **Repository access → Only select repositories** → solo `maestri-di-pixel-packs`.
3. **Permissions → Repository permissions → Contents: `Read-only`**, tutto il resto *No access*.
4. Nel progetto dell'app, copia `.env.example` in `.env` e imposta `GITHUB_PACKS_PAT=github_pat_...`, poi ricompila l'APK. Il token finisce solo nell'APK, mai nel codice sorgente (`.env` è escluso da git).

Alla scadenza del token la sincronizzazione mostra "Accesso negato" in Area Genitori: generane uno nuovo e ricompila.

---

## Aggiungere un nuovo capitolo

1. Trasforma 1–2 pagine del sussidiario in un file `.json` (`schema_version = 1`) seguendo `schemas/pack.schema.json`.
2. **Usa ID domanda unici** con un prefisso legato al pacchetto (es. `sto_romani_001`). I progressi sono salvati per ID: se un ID è già usato da un altro pacchetto, anche da uno di quelli inclusi nell'app, l'app scarta il pacchetto e lo segnala in Area Genitori.
3. Aggiungi la voce in `packs/manifest.json` (schema in `schemas/manifest.schema.json`). Metti `"priority": true` nella settimana della verifica per il badge `!` nell'Hub e il bonus stelle `x1.5`; `"archived": true` lo nasconde mantenendo le medaglie.
4. Controlla tutto insieme prima di caricare:
   ```bash
   python3 validate_pack.py --all packs
   ```
   Verifica ogni pacchetto, il manifest, la corrispondenza tra i due e gli ID duplicati tra pacchetti. Per un solo file: `python3 validate_pack.py packs/nuovo_capitolo.json`.
5. Carica su GitHub il file del pacchetto e `manifest.json`.

---

## Consigli sulle domande

- Bilancia le risposte Vero/Falso: se sono quasi tutte "Vero" il bambino impara a rispondere sempre Vero.
- Usa `topic` per dividere un capitolo in argomenti: dall'Area Genitori puoi far allenare solo quelli della prossima interrogazione.
- I `**grassetti**` evidenziano le parole chiave e non contano nei limiti di lunghezza.

---

## Provare il funzionamento offline

1. Avvia l'app con il Wi-Fi acceso e attendi la sincronizzazione.
2. Attiva la **Modalità Aereo**, chiudi e riavvia l'app.
3. Verifica che i pacchetti scaricati restino giocabili.
