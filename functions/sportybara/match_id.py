from datetime import datetime, timezone

from sportybara.teams import canonical


def match_id(league: str, kickoff: datetime, home: str, away: str) -> str:
    if kickoff.tzinfo is not None:
        kickoff = kickoff.astimezone(timezone.utc)
    return f"{league}_{kickoff:%Y%m%d}_{canonical(home)}_{canonical(away)}"
