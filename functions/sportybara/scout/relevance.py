import re
from datetime import datetime

from .dedup import strip_diacritics
from .models import Article


def _mentions(text: str, name: str) -> bool:
    pattern = r"\b" + re.escape(strip_diacritics(name).lower()) + r"\b"
    return re.search(pattern, strip_diacritics(text).lower()) is not None


FOOTBALL_PREFIXES = (
    "lesion", "lesiona", "alineaci", "convocatori", "sancion", "arbitr", "titular",
    "entrenador", "partido", "jornada", "laliga", "delanter", "portero", "recupera",
    "plantilla", "fichaj", "regres",
    "injur", "lineup", "line-up", "squad", "suspen", "fitness", "striker", "goalkeeper",
    "accidentat", "accidentar", "echip", "meci", "antrenor", "fotbal", "superliga",
    "jucator", "atacant", "portar", "fundas", "transfer", "revine", "revenire",
)
FOOTBALL_WORDS = (
    "gol", "goles", "golazo", "baja", "bajas", "medico", "vuelve", "once",
    "team news", "ruled out", "doubt", "doubtful", "coach", "manager", "match", "fixture",
    "back for", "return", "returns", "boost", "blow", "lotul",
)
_FOOTBALL_RE = re.compile(
    r"\b(?:" + "|".join(re.escape(p) for p in FOOTBALL_PREFIXES) + r")"
    + r"|\b(?:" + "|".join(re.escape(w) for w in FOOTBALL_WORDS) + r")\b"
)


def _football_context(text: str) -> bool:
    return _FOOTBALL_RE.search(strip_diacritics(text).lower()) is not None


def is_relevant(article: Article, home: str, away: str, since: datetime) -> bool:
    if article.published is not None and article.published < since:
        return False
    text = f"{article.title} {article.summary}"
    home_hit, away_hit = _mentions(text, home), _mentions(text, away)
    if not (home_hit or away_hit):
        return False
    return (home_hit and away_hit) or _football_context(text)
