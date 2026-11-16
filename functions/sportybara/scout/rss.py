import calendar
import html
import logging
import re
import time
from datetime import datetime, timezone
from urllib.parse import quote

import feedparser

from .models import Article

log = logging.getLogger(__name__)

USER_AGENT = "SportyBaraScout/0.1 (proiect de facultate)"

GOOGLE_LOCALES = {
    "es": ("es", "ES"),
    "ro": ("ro", "RO"),
    "en": ("en-GB", "GB"),
}

FEEDS = [
    ("Marca", "https://e00-marca.uecdn.es/rss/futbol/primera-division.xml"),
    ("Mundo Deportivo", "https://www.mundodeportivo.com/feed/rss/home"),
    ("BBC Sport", "https://feeds.bbci.co.uk/sport/football/rss.xml"),
    ("GSP", "https://www.gsp.ro/rss.xml"),
]

LEAGUE_FEEDS = {
    "laliga": ("Marca", "Mundo Deportivo", "BBC Sport"),
    "superliga": ("GSP",),
}


def feeds_for(league: str) -> list[tuple[str, str]]:
    wanted = LEAGUE_FEEDS.get(league, ())
    return [(name, url) for name, url in FEEDS if name in wanted]


def google_news_url(query: str, lang: str) -> str:
    hl, gl = GOOGLE_LOCALES[lang]
    return (
        f"https://news.google.com/rss/search?q={quote(query, safe='')}"
        f"&hl={hl}&gl={gl}&ceid={gl}:{lang}"
    )


def fetch(session, url: str, delay: float = 1.0) -> bytes:
    time.sleep(delay)
    resp = session.get(url, headers={"User-Agent": USER_AGENT}, timeout=15)
    resp.raise_for_status()
    return resp.content


def _published(entry) -> datetime | None:
    parsed = entry.get("published_parsed") or entry.get("updated_parsed")
    if not parsed:
        return None
    return datetime.fromtimestamp(calendar.timegm(parsed), tz=timezone.utc)


def _plain(text: str) -> str:
    text = html.unescape(re.sub(r"<[^>]+>", " ", text or ""))
    return " ".join(text.split())


def parse_feed(xml_bytes: bytes, source: str, query: str) -> list[Article]:
    feed = feedparser.parse(xml_bytes)
    articles: list[Article] = []
    for entry in feed.entries:
        title = _plain(entry.get("title", ""))
        url = entry.get("link", "")
        if not title or not url:
            continue

        real_source = (entry.get("source") or {}).get("title")
        if real_source:
            suffix = f" - {real_source}"
            if title.endswith(suffix):
                title = title[: -len(suffix)]
        articles.append(
            Article(
                title=title,
                url=url,
                source=real_source or source,
                published=_published(entry),
                summary=_plain(entry.get("summary", "")),
                query=query,
            )
        )
    return articles
