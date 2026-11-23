#!/usr/bin/env python3
"""Validatore CLI per i pacchetti JSON di Maestri di Pixel (schema_version = 1).

Uso:
  python3 validate_pack.py packs/nuovo_capitolo.json   # controlla un solo pacchetto
  python3 validate_pack.py --all [cartella_packs]      # controlla manifest + tutti i pacchetti insieme
"""
import json
import re
import sys
from pathlib import Path

ALLOWED_SUBJECTS = {"storia", "geografia", "scienze", "matematica", "italiano", "inglese"}
ALLOWED_TYPES = {"multiple_choice", "true_false", "numeric_input", "classification"}
ALLOWED_TEMPLATES = {"multiplication_table", "mental_add_sub", "unit_conversion"}
PACK_ID_RE = re.compile(r"^[a-z0-9_]{3,48}$")
Q_ID_RE = re.compile(r"^[a-z0-9_]{3,64}$")
MANIFEST_FILE_RE = re.compile(r"^[a-zA-Z0-9_\-/]+\.json$")


def strip_markers(text: str) -> str:
    return text.replace("**", "")


def is_whole_int(val) -> bool:
    return isinstance(val, int) and not isinstance(val, bool)


def check_pack(path: Path):
    """Restituisce (errori, avvisi, dati, id_domande, domande_giocabili, generatori)."""
    errors = []
    warnings = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        return [f"Impossibile leggere o analizzare JSON: {exc}"], [], None, set(), 0, 0

    if not isinstance(data, dict):
        return ["La radice del file JSON deve essere un oggetto."], [], None, set(), 0, 0

    if data.get("schema_version") != 1:
        errors.append("schema_version deve essere 1.")
    pack_id = data.get("pack_id", "")
    if not isinstance(pack_id, str) or not PACK_ID_RE.match(pack_id):
        errors.append(f"pack_id '{pack_id}' non valido (usa 3-48 caratteri a-z, 0-9, _).")
    subject = data.get("subject", "")
    if subject not in ALLOWED_SUBJECTS:
        errors.append(f"subject '{subject}' non supportato.")
    if data.get("grade") not in (4, 5):
        errors.append("grade deve essere 4 oppure 5.")
    title = str(data.get("title", "")).strip()
    if not (3 <= len(title) <= 60):
        errors.append("title deve avere tra 3 e 60 caratteri.")
    if not str(data.get("boss_name", "")).strip() or len(str(data.get("boss_name", "")).strip()) > 40:
        errors.append("boss_name obbligatorio (max 40 caratteri).")
    if not str(data.get("medal_name", "")).strip() or len(str(data.get("medal_name", "")).strip()) > 40:
        errors.append("medal_name obbligatorio (max 40 caratteri).")
    if not is_whole_int(data.get("version")) or data.get("version", 0) < 1:
        errors.append("version deve essere intero >= 1.")

    seen_ids = set()
    playable = 0
    choice_or_tf = 0

    questions = data.get("questions", [])
    if not isinstance(questions, list):
        errors.append("'questions' deve essere un array.")
        questions = []

    for i, q in enumerate(questions):
        if not isinstance(q, dict):
            errors.append(f"questions[{i}] non è un oggetto valido.")
            continue
        qid = q.get("id", "")
        if not isinstance(qid, str) or not Q_ID_RE.match(qid):
            errors.append(f"questions[{i}].id '{qid}' non valido.")
        elif qid in seen_ids:
            errors.append(f"id domanda duplicato '{qid}'.")
        else:
            seen_ids.add(qid)

        qtype = q.get("type", "")
        if qtype not in ALLOWED_TYPES:
            errors.append(f"questions[{i}].type '{qtype}' non valido.")
            continue
        if qtype == "classification":
            warnings.append(f"questions[{i}] di tipo 'classification' non è ancora attiva nel Pilota 1.")
        if q.get("difficulty") not in (1, 2, 3):
            errors.append(f"questions[{i}].difficulty deve essere 1, 2 o 3.")

        prompt = strip_markers(str(q.get("prompt", ""))).strip()
        if not prompt or len(prompt) > 140:
            errors.append(f"questions[{i}].prompt obbligatorio e max 140 caratteri (senza **).")
        expl = strip_markers(str(q.get("explanation", ""))).strip()
        if not expl or len(expl) > 160:
            errors.append(f"questions[{i}].explanation obbligatoria e max 160 caratteri (senza **).")
        if "hint" in q and q["hint"] is not None:
            if not isinstance(q["hint"], str) or len(strip_markers(q["hint"]).strip()) > 90:
                errors.append(f"questions[{i}].hint deve essere stringa max 90 caratteri (senza **).")
        if "topic" in q and q["topic"] is not None:
            if not isinstance(q["topic"], str) or len(q["topic"].strip()) > 40:
                errors.append(f"questions[{i}].topic deve essere stringa max 40 caratteri.")

        if qtype == "multiple_choice":
            opts = q.get("options")
            if not isinstance(opts, list) or len(opts) != 4:
                errors.append(f"questions[{i}].options deve avere esattamente 4 scelte.")
            else:
                norm = [str(o).strip().lower() for o in opts]
                if any(not str(o).strip() or len(str(o).strip()) > 45 for o in opts):
                    errors.append(f"questions[{i}].options: ogni scelta deve avere 1-45 caratteri.")
                if len(set(norm)) != 4:
                    errors.append(f"questions[{i}].options contiene scelte duplicate.")
            if q.get("correct_index") not in (0, 1, 2, 3):
                errors.append(f"questions[{i}].correct_index deve essere tra 0 e 3.")
            playable += 1
            choice_or_tf += 1
        elif qtype == "true_false":
            if not isinstance(q.get("correct_bool"), bool):
                errors.append(f"questions[{i}].correct_bool deve essere booleano.")
            playable += 1
            choice_or_tf += 1
        elif qtype == "numeric_input":
            cv = q.get("correct_value")
            if isinstance(cv, bool) or not isinstance(cv, (int, float)) or cv < 0:
                errors.append(f"questions[{i}].correct_value deve essere un numero >= 0.")
            playable += 1

    generators = data.get("generators", [])
    if not isinstance(generators, list):
        errors.append("'generators' deve essere un array.")
        generators = []
    for j, g in enumerate(generators):
        if not isinstance(g, dict):
            errors.append(f"generators[{j}] non è un oggetto valido.")
            continue
        gid = g.get("id", "")
        if not isinstance(gid, str) or not Q_ID_RE.match(gid) or gid in seen_ids:
            errors.append(f"generators[{j}].id '{gid}' non valido o duplicato.")
        else:
            seen_ids.add(gid)
        if g.get("template") not in ALLOWED_TEMPLATES:
            errors.append(f"generators[{j}].template non valido.")
        if g.get("difficulty") not in (1, 2, 3):
            errors.append(f"generators[{j}].difficulty deve essere 1, 2 o 3.")
        ps = g.get("pool_size")
        if not is_whole_int(ps) or not (4 <= ps <= 30):
            errors.append(f"generators[{j}].pool_size deve essere tra 4 e 30.")
        else:
            playable += ps
        if "topic" in g and g["topic"] is not None:
            if not isinstance(g["topic"], str) or len(g["topic"].strip()) > 40:
                errors.append(f"generators[{j}].topic deve essere stringa max 40 caratteri.")

    if playable < 5:
        errors.append(f"Servono almeno 5 domande giocabili (trovate {playable}).")
    if subject != "matematica" and subject in ALLOWED_SUBJECTS and choice_or_tf < 5:
        warnings.append(f"Meno di 5 domande scelta multipla/Vero-Falso per il Cortile ({choice_or_tf}).")

    return errors, warnings, data, seen_ids, playable, len(generators)


def report(prefix: str, errors, warnings) -> None:
    for w in warnings:
        print(f"[AVVISO] {prefix}{w}")
    for e in errors:
        print(f"[ERRORE] {prefix}{e}")


def validate_pack(path: Path) -> int:
    errors, warnings, _, _, playable, gen_count = check_pack(path)
    report("", errors, warnings)
    if errors:
        return 1
    print(f"[OK] Pacchetto valido ({playable} domande giocabili, {gen_count} generatori)")
    return 0


def check_manifest(manifest) -> list:
    """Stesse regole di PackValidator.validateManifest nell'app."""
    errors = []
    if manifest.get("schema_version") != 1:
        errors.append("schema_version deve essere 1.")
    packs = manifest.get("packs")
    if not isinstance(packs, list):
        return errors + ["campo 'packs' mancante o non è un array."]
    seen = set()
    for i, e in enumerate(packs):
        if not isinstance(e, dict):
            errors.append(f"packs[{i}] non è un oggetto valido.")
            continue
        pid = e.get("pack_id", "")
        if not isinstance(pid, str) or not PACK_ID_RE.match(pid):
            errors.append(f"packs[{i}].pack_id '{pid}' non valido.")
        elif pid in seen:
            errors.append(f"pack_id duplicato '{pid}'.")
        else:
            seen.add(pid)
        f = e.get("file", "")
        if (not isinstance(f, str) or not f.strip() or f.startswith("/") or ".." in f
                or "\\" in f or ":" in f or not MANIFEST_FILE_RE.match(f)):
            errors.append(f"packs[{i}].file '{f}' non consentito.")
        if e.get("subject") not in ALLOWED_SUBJECTS:
            errors.append(f"packs[{i}].subject '{e.get('subject')}' non valido.")
        if not (3 <= len(str(e.get("title", "")).strip()) <= 60):
            errors.append(f"packs[{i}].title deve avere tra 3 e 60 caratteri.")
        if not is_whole_int(e.get("version")) or e.get("version", 0) < 1:
            errors.append(f"packs[{i}].version deve essere intero >= 1.")
        for flag in ("priority", "archived"):
            if flag in e and not isinstance(e[flag], bool):
                errors.append(f"packs[{i}].{flag} deve essere booleano.")
    return errors


def validate_all(packs_dir: Path) -> int:
    """Controlla manifest e pacchetti insieme, come li vedrà l'app dopo la sincronizzazione."""
    try:
        manifest = json.loads((packs_dir / "manifest.json").read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"[ERRORE] manifest.json: impossibile leggerlo ({exc})")
        return 1
    if not isinstance(manifest, dict):
        print("[ERRORE] manifest.json: la radice deve essere un oggetto.")
        return 1

    m_errors = check_manifest(manifest)
    report("manifest.json: ", m_errors, [])
    failed = bool(m_errors)

    id_owner = {}
    listed = set()
    for entry in manifest.get("packs") or []:
        if not isinstance(entry, dict):
            continue
        fname = str(entry.get("file", ""))
        listed.add(fname)
        path = packs_dir / fname
        if not path.is_file():
            print(f"[ERRORE] {fname}: elencato nel manifest ma il file non esiste.")
            failed = True
            continue
        errors, warnings, data, ids, playable, gen_count = check_pack(path)
        if data is not None:
            if data.get("pack_id") != entry.get("pack_id"):
                errors.append(f"pack_id '{data.get('pack_id')}' diverso dal manifest ('{entry.get('pack_id')}').")
            if data.get("subject") != entry.get("subject"):
                errors.append(f"subject '{data.get('subject')}' diverso dal manifest ('{entry.get('subject')}').")
            if data.get("version") != entry.get("version"):
                warnings.append(f"version {data.get('version')} diversa dal manifest ({entry.get('version')}).")
            # I progressi sono salvati per ID domanda: un ID usato da due pacchetti mescolerebbe i progressi
            for qid in sorted(ids):
                other = id_owner.get(qid)
                if other and other != entry.get("pack_id"):
                    errors.append(f"ID domanda '{qid}' già usato dal pacchetto {other}.")
                else:
                    id_owner[qid] = entry.get("pack_id")
        report(f"{fname}: ", errors, warnings)
        if errors:
            failed = True
        else:
            print(f"[OK] {fname} ({playable} domande giocabili, {gen_count} generatori)")

    for path in sorted(packs_dir.glob("*.json")):
        if path.name != "manifest.json" and path.name not in listed:
            print(f"[AVVISO] {path.name}: non è elencato nel manifest, l'app lo ignorerà.")

    if failed:
        print("[ERRORE] Correggi gli errori prima di caricare i file su GitHub.")
        return 1
    print("[OK] Manifest e pacchetti pronti per GitHub.")
    return 0


if __name__ == "__main__":
    args = sys.argv[1:]
    if args and args[0] == "--all":
        default_dir = Path(__file__).resolve().parent / "packs"
        sys.exit(validate_all(Path(args[1]) if len(args) > 1 else default_dir))
    if len(args) != 1:
        print(__doc__)
        sys.exit(1)
    sys.exit(validate_pack(Path(args[0])))
