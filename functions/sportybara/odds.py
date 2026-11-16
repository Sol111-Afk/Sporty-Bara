OUTCOMES = ("home", "draw", "away")


def implied_probabilities(odds: dict) -> dict:
    return {k: 1 / odds[k] for k in OUTCOMES}


def margin(odds: dict) -> float:
    return sum(implied_probabilities(odds).values()) - 1


def fair_probabilities(odds: dict) -> dict:
    raw = implied_probabilities(odds)
    total = sum(raw.values())
    return {k: p / total for k, p in raw.items()}


def best_odds(bookmakers: dict) -> dict:
    best = {}
    for name, odds in bookmakers.items():
        for k in OUTCOMES:
            if k not in best or odds[k] > best[k]["odd"]:
                best[k] = {"odd": odds[k], "bookmaker": name}
    return best
