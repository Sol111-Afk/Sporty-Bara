from ..odds import OUTCOMES, fair_probabilities
from .combine import verdict

PLAYER_FACTOR_TYPES = ("injury", "suspension", "lineup")
PROB_FLOOR = 0.01
PROB_CEILING = 0.98


def factor_weight(factor) -> float:
    if factor.type in PLAYER_FACTOR_TYPES and factor.playProbability is not None:
        return 1 - factor.playProbability
    return 1.0


def factor_key(factor) -> tuple:
    return (factor.type, factor.team, (factor.player or "").strip().lower())


def unique_factors(factors) -> list:
    latest = {}
    for factor in factors:
        key = factor_key(factor)
        if key not in latest or factor.date > latest[key].date:
            latest[key] = factor
    return list(latest.values())


def factor_deltas(factors) -> dict:
    deltas = {k: 0.0 for k in OUTCOMES}
    for factor in unique_factors(factors):
        weight = factor_weight(factor)
        for k in OUTCOMES:
            deltas[k] += factor.effect.get(k, 0.0) * weight
    return deltas


def apply_factors(probs: dict, factors, max_shift: float = 0.15) -> dict:
    deltas = factor_deltas(factors)
    shifted = {}
    for k in OUTCOMES:
        shift = max(-max_shift, min(max_shift, deltas[k]))
        shifted[k] = min(PROB_CEILING, max(PROB_FLOOR, probs[k] + shift))
    total = sum(shifted.values())
    return {k: shifted[k] / total for k in OUTCOMES}


def sporty_bara_odds(market_odds: dict, factors, best_odds: dict) -> dict:
    market_probs = fair_probabilities(market_odds)
    probs = apply_factors(market_probs, factors)
    fair = {k: round(1 / probs[k], 2) for k in OUTCOMES}
    result_verdict = {k: verdict(best_odds[k], fair[k]) for k in OUTCOMES}
    return {"fairOdds": fair, "verdict": result_verdict, "probs": probs}


def factor_summary(factors) -> list:
    summary = []
    for factor in unique_factors(factors):
        impact = factor.effect.get(factor.team, 0.0) * factor_weight(factor)
        summary.append(
            {
                "type": factor.type,
                "team": factor.team,
                "player": factor.player,
                "impact": round(impact, 3),
            }
        )
    return sorted(summary, key=lambda item: abs(item["impact"]), reverse=True)
