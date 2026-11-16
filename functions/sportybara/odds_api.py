from datetime import datetime

from sportybara.match_id import match_id
from sportybara.odds import OUTCOMES, best_odds


def _parse_time(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def _bookmaker_odds(bookmaker: dict, home: str, away: str):
    h2h = next((m for m in bookmaker.get("markets", []) if m.get("key") == "h2h"), None)
    if h2h is None:
        return None
    names = {home: "home", away: "away", "Draw": "draw"}
    prices = {}
    for outcome in h2h.get("outcomes", []):
        key = names.get(outcome.get("name"))
        if key is not None:
            prices[key] = round(float(outcome["price"]), 2)
    if set(prices) != set(OUTCOMES):
        return None
    return prices


def parse_odds_events(events: list, league: str) -> list:
    result = []
    for ev in events:
        home, away = ev["home_team"], ev["away_team"]
        odds = {}
        for bookmaker in ev.get("bookmakers", []):
            prices = _bookmaker_odds(bookmaker, home, away)
            if prices is not None:
                odds[bookmaker["key"]] = prices
        if not odds:
            continue

        kickoff = _parse_time(ev["commence_time"])
        result.append({
            "matchId": match_id(league, kickoff, home, away),
            "match": {
                "league": league,
                "home": home,
                "away": away,
                "kickoff": kickoff,
                "status": "scheduled",
                "bestOdds": best_odds(odds),
                "externalIds": {"oddsApi": ev["id"]},
            },
            "odds": odds,
        })
    return result
