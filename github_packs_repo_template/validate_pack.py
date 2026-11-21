#!/usr/bin/env python3
"""Validatore CLI per i pacchetti JSON di Maestri di Pixel (schema_version = 1)."""
import json
import re
import sys
from pathlib import Path

ALLOWED_SUBJECTS = {"storia", "geografia", "scienze", "matematica", "italiano", "inglese"}
ALLOWED_TYPES = {"multiple_choice", "true_false", "numeric_input", "classification"}
ALLOWED_TEMPLATES = {"multiplication_table", "mental_add_sub", "unit_conversion"}
PACK_ID_RE = re.compile(r"^[a-z0-9_]{3,48}$")
Q_ID_RE = re.compile(r"^[a-z0-9_]{3,64}$")


def strip_markers(text: str) -> str:
    return text.replace("**", "")


def is_whole_int(val) -> bool:
    return isinstance(val, int) and not isinstance(val, bool)


def validate_pack(path: Path) -> int:
    errors = []
    warnings = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"[ERRORE] Impossibile leggere o analizzare JSON: {exc}")
        return 1

    if not isinstance(data, dict):
        print("[ERRORE] La radice del file JSON deve essere un oggetto.")
        return 1

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

    if playable < 5:
        errors.append(f"Servono almeno 5 domande giocabili (trovate {playable}).")
    if subject != "matematica" and subject in ALLOWED_SUBJECTS and choice_or_tf < 5:
        warnings.append(f"Meno di 5 domande scelta multipla/Vero-Falso per il Cortile ({choice_or_tf}).")

    for w in warnings:
        print(f"[AVVISO] {w}")
    if errors:
        for e in errors:
            print(f"[ERRORE] {e}")
        return 1

    print(f"[OK] Pacchetto valido ({playable} domande giocabili, {len(generators)} generatori)")
    return 0


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python3 validate_pack.py percorso/al/pacchetto.json")
        sys.exit(1)
    sys.exit(validate_pack(Path(sys.argv[1])))
