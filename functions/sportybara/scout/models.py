from dataclasses import dataclass
from datetime import datetime


@dataclass
class Article:

    title: str
    url: str
    source: str
    published: datetime | None
    summary: str
    query: str


@dataclass
class Factor:

    type: str
    team: str
    player: str | None
    playProbability: float | None
    effect: dict
    source: str
    url: str
    date: datetime
