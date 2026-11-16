import json
import logging
from datetime import datetime, timezone

import anthropic

from sportybara.scout.models import Article, Factor

from .client import MODEL

log = logging.getLogger(__name__)

FACTOR_TYPES = ("injury", "suspension", "lineup", "referee", "weather", "coach", "motivation", "news")
TEAMS = ("home", "away")
OUTCOMES = ("home", "draw", "away")
MAX_EFFECT = 0.10
MAX_TOKENS = 4096
MAX_SUMMARY_CHARS = 3000

SYSTEM_PROMPT = (
    "Ești un analist de fotbal care citește articole de presă. "
    "Extragi DOAR factorii menționați explicit în articol care pot influența rezultatul "
    "meciului dintre echipa gazdă (home) și echipa oaspete (away) primite în cerere. "
    "Factori posibili: accidentări (injury), suspendări (suspension), formația probabilă (lineup), "
    "arbitrul (referee), vremea (weather), antrenorul (coach), motivația (motivation), "
    "alte știri relevante (news). "
    "Dacă articolul nu e despre acest meci sau nu conține factori, întoarce lista goală. "
    "Nu inventa nimic și nu deduce ce nu scrie în articol. "
    "Pentru fiecare factor: team = home sau away (echipa afectată), player = numele jucătorului sau null, "
    "playProbability = probabilitatea ca jucătorul să joace (0-1) sau null, "
    "effect = modificarea estimată a probabilității rezultatelor, valori mici între -0.10 și 0.10 "
    "(de exemplu home = -0.03 înseamnă că șansa de victorie a gazdelor scade cu 3 puncte procentuale), "
    "null pentru rezultatele neafectate."
)

NULLABLE_NUMBER = {"anyOf": [{"type": "number"}, {"type": "null"}]}
NULLABLE_STRING = {"anyOf": [{"type": "string"}, {"type": "null"}]}

FACTORS_SCHEMA = {
    "type": "object",
    "properties": {
        "factors": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "type": {"type": "string", "enum": list(FACTOR_TYPES)},
                    "team": {"type": "string", "enum": list(TEAMS)},
                    "player": NULLABLE_STRING,
                    "playProbability": NULLABLE_NUMBER,
                    "effect": {
                        "type": "object",
                        "properties": {outcome: NULLABLE_NUMBER for outcome in OUTCOMES},
                        "required": list(OUTCOMES),
                        "additionalProperties": False,
                    },
                },
                "required": ["type", "team", "player", "playProbability", "effect"],
                "additionalProperties": False,
            },
        }
    },
    "required": ["factors"],
    "additionalProperties": False,
}


def is_number(value) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and value == value


def clip(value: float, low: float, high: float) -> float:
    return max(low, min(high, value))


def clean_effect(raw: dict) -> dict:
    return {
        outcome: round(clip(float(raw[outcome]), -MAX_EFFECT, MAX_EFFECT), 4)
        for outcome in OUTCOMES
        if is_number(raw.get(outcome))
    }


def clean_probability(value) -> float | None:
    return clip(float(value), 0.0, 1.0) if is_number(value) else None


def build_user_message(article: Article, home: str, away: str) -> str:
    published = article.published.date().isoformat() if article.published else "necunoscută"
    return (
        f"Meci: {home} (home) - {away} (away)\n\n"
        f"<articol>\n"
        f"Titlu: {article.title}\n"
        f"Sursă: {article.source}\n"
        f"Data: {published}\n"
        f"Rezumat: {article.summary[:MAX_SUMMARY_CHARS]}\n"
        f"</articol>"
    )


def parse_factors(raw_factors: list, article: Article) -> list[Factor]:
    date = article.published or datetime.now(timezone.utc)
    factors = []
    for raw in raw_factors:
        if not isinstance(raw, dict) or raw.get("type") not in FACTOR_TYPES or raw.get("team") not in TEAMS:
            continue
        effect = raw.get("effect")
        factors.append(Factor(
            type=raw["type"],
            team=raw["team"],
            player=raw.get("player") or None,
            playProbability=clean_probability(raw.get("playProbability")),
            effect=clean_effect(effect if isinstance(effect, dict) else {}),
            source=article.source,
            url=article.url,
            date=date,
        ))
    return factors


class ClaudeExtractor:
    def __init__(self, client, model: str = MODEL):
        self.client = client
        self.model = model

    def extract(self, article: Article, home: str, away: str) -> list[Factor]:
        try:
            response = self.client.messages.create(
                model=self.model,
                max_tokens=MAX_TOKENS,
                system=SYSTEM_PROMPT,
                messages=[{"role": "user", "content": build_user_message(article, home, away)}],
                output_config={
                    "effort": "low",
                    "format": {"type": "json_schema", "schema": FACTORS_SCHEMA},
                },
            )
        except (anthropic.APIStatusError, anthropic.APIConnectionError) as exc:
            log.warning("extractor: eroare API pentru %s: %s", article.url, exc)
            return []

        if response.stop_reason != "end_turn":
            log.warning("extractor: stop_reason=%s pentru %s", response.stop_reason, article.url)
            return []

        text = next((block.text for block in response.content if block.type == "text"), None)
        if text is None:
            log.warning("extractor: răspuns fără text pentru %s", article.url)
            return []
        try:
            raw_factors = json.loads(text)["factors"]
        except (ValueError, KeyError, TypeError):
            log.warning("extractor: JSON invalid pentru %s", article.url)
            return []
        if not isinstance(raw_factors, list):
            return []
        return parse_factors(raw_factors, article)
