import logging
from datetime import datetime, timezone

import requests
from firebase_admin import firestore, initialize_app
from firebase_functions import https_fn, options, scheduler_fn
from firebase_functions.params import SecretParam

from sportybara.ai.client import build_client
from sportybara.ai.consultant import ExplainError, build_context, explain
from sportybara.ai.limits import LimitExceeded, check_and_count, usage_key
from sportybara.api_football import parse_fixtures, season_for
from sportybara.config import LEAGUES
from sportybara.http import get_json
from sportybara.odds_api import parse_odds_events

options.set_global_options(region="europe-west1", max_instances=5)
initialize_app()

ODDS_API_KEY = SecretParam("ODDS_API_KEY")
API_FOOTBALL_KEY = SecretParam("API_FOOTBALL_KEY")
ANTHROPIC_API_KEY = SecretParam("ANTHROPIC_API_KEY")

ODDS_URL = "https://api.the-odds-api.com/v4/sports/{sport}/odds"
FIXTURES_URL = "https://v3.football.api-sports.io/fixtures"
BATCH_MAX = 500

log = logging.getLogger("sportybara")


@https_fn.on_call()
def ping(req: https_fn.CallableRequest) -> dict:
    return {"ok": True, "uid": req.auth.uid if req.auth else None}


def _commit(db, ops):
    for i in range(0, len(ops), BATCH_MAX):
        batch = db.batch()
        for ref, data in ops[i:i + BATCH_MAX]:
            batch.set(ref, data, merge=True)
        batch.commit()


@scheduler_fn.on_schedule(schedule="0 */6 * * *", secrets=[ODDS_API_KEY])
def fetch_odds(event: scheduler_fn.ScheduledEvent) -> None:
    db = firestore.client()
    session = requests.Session()
    ops = []
    for league, ids in LEAGUES.items():
        if ids["oddsApi"] is None:
            continue
        try:
            events = get_json(
                session,
                ODDS_URL.format(sport=ids["oddsApi"]),
                params={"apiKey": ODDS_API_KEY.value(), "regions": "eu", "markets": "h2h", "oddsFormat": "decimal"},
            )
        except Exception:
            log.exception("fetch_odds: eroare la liga %s", league)
            continue
        parsed = parse_odds_events(events, league)
        refs = [db.collection("matches").document(m["matchId"]) for m in parsed]
        existing = {snap.id for snap in db.get_all(refs) if snap.exists} if refs else set()

        for m, ref in zip(parsed, refs):
            match = dict(m["match"])
            if m["matchId"] in existing:
                match.pop("status")
            match["updatedAt"] = firestore.SERVER_TIMESTAMP
            ops.append((ref, match))
            for bookmaker, prices in m["odds"].items():
                ops.append((ref.collection("odds").document(bookmaker),
                            {**prices, "updatedAt": firestore.SERVER_TIMESTAMP}))
    _commit(db, ops)


@scheduler_fn.on_schedule(schedule="0 */2 * * *", secrets=[API_FOOTBALL_KEY])
def fetch_results(event: scheduler_fn.ScheduledEvent) -> None:
    db = firestore.client()
    session = requests.Session()
    season = season_for(datetime.now(timezone.utc))
    ops = []
    for league, ids in LEAGUES.items():
        try:
            payload = get_json(
                session,
                FIXTURES_URL,
                params={"league": ids["apiFootball"], "season": season},
                headers={"x-apisports-key": API_FOOTBALL_KEY.value()},
            )
        except Exception:
            log.exception("fetch_results: eroare la liga %s", league)
            continue
        parsed = parse_fixtures(payload, league)
        refs = [db.collection("matches").document(m["matchId"]) for m in parsed]
        snaps = {snap.id: snap for snap in db.get_all(refs)} if refs else {}

        unmatched = []
        for m, ref in zip(parsed, refs):
            snap = snaps[m["matchId"]]
            if snap.exists:
                ops.append((ref, {**m["update"], "updatedAt": firestore.SERVER_TIMESTAMP}))
            elif ids["oddsApi"] is None:
                ops.append((ref, {**m["match"], **m["update"], "bestOdds": {},
                                  "updatedAt": firestore.SERVER_TIMESTAMP}))
            elif m["update"]["status"] != "scheduled":
                unmatched.append(m["matchId"])
        if unmatched:
            log.warning("fetch_results: %d meciuri fără document în %s (aliasuri lipsă?): %s",
                        len(unmatched), league, ", ".join(unmatched[:20]))
    _commit(db, ops)


@firestore.transactional
def _count_question(transaction, usage_ref, uid, today):
    snapshot = usage_ref.get(transaction=transaction)
    store = dict(snapshot.to_dict() or {}) if snapshot.exists else {}
    check_and_count(store, uid, today)
    transaction.set(usage_ref, {usage_key(uid, today): store[usage_key(uid, today)],
                                "uid": uid, "day": today})


@https_fn.on_call(secrets=[ANTHROPIC_API_KEY], timeout_sec=60)
def ai_explain(req: https_fn.CallableRequest) -> dict:
    if req.auth is None:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.UNAUTHENTICATED, "autentificare necesară")
    data = req.data or {}
    match_id, question = data.get("matchId"), data.get("question")
    if not isinstance(match_id, str) or not match_id or not isinstance(question, str) or not question.strip():
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, "matchId și question sunt obligatorii")

    db = firestore.client()
    match_ref = db.collection("matches").document(match_id)
    match_snapshot = match_ref.get()
    if not match_snapshot.exists:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.NOT_FOUND, "meci inexistent")
    match = match_snapshot.to_dict()
    factors = [snap.to_dict() for snap in match_ref.collection("factors").stream()]
    context = build_context(match, match.get("fairOdds"), match.get("bestOdds"), factors)

    today = datetime.now(timezone.utc).date().isoformat()
    usage_ref = db.collection("ai_usage").document(usage_key(req.auth.uid, today))
    try:
        _count_question(db.transaction(), usage_ref, req.auth.uid, today)
    except LimitExceeded:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.RESOURCE_EXHAUSTED, "limita zilnică de întrebări a fost atinsă")

    try:
        return explain(build_client(), context, question.strip())
    except ExplainError:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.UNAVAILABLE, "consultantul nu e disponibil acum")
