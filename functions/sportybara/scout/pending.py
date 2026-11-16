from datetime import timezone

from .models import Factor

FACTOR_FIELDS = ("type", "team", "player", "playProbability", "effect", "source", "url", "date")


def to_pending_docs(match_id: str, factors: list[Factor]) -> list[dict]:
    docs = []
    for factor in factors:
        doc = {field: getattr(factor, field) for field in FACTOR_FIELDS}
        if doc["date"].tzinfo is None:
            doc["date"] = doc["date"].replace(tzinfo=timezone.utc)
        else:
            doc["date"] = doc["date"].astimezone(timezone.utc)
        doc["matchId"] = match_id
        doc["status"] = "pending"
        docs.append(doc)
    return docs
