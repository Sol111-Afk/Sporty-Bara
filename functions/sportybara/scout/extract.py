from typing import Protocol

from .models import Article, Factor


class Extractor(Protocol):
    def extract(self, article: Article, home: str, away: str) -> list[Factor]:
        ...


class FakeExtractor:

    def __init__(self, factors: list[Factor]):
        self.factors = list(factors)
        self.calls: list[tuple[Article, str, str]] = []

    def extract(self, article: Article, home: str, away: str) -> list[Factor]:
        self.calls.append((article, home, away))
        return list(self.factors)
