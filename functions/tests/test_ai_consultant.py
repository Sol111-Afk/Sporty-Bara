import json
from types import SimpleNamespace

import anthropic
import httpx2
import pytest

from sportybara.ai.client import MODEL
from sportybara.ai.consultant import ExplainError, build_context, explain

MATCH = {"home": "Real Madrid", "away": "Barcelona", "league": "laliga",
         "verdict": {"home": "low", "draw": "fair", "away": "high"}}
BEST = {"home": {"odd": 2.1, "bookmaker": "bet365"}, "draw": {"odd": 3.4, "bookmaker": "unibet"},
        "away": {"odd": 3.6, "bookmaker": "betano"}}
FAIR = {"home": 2.4, "draw": 3.3, "away": 3.1}
FACTORS = [
    {"type": "injury", "team": "home", "player": "Mbappe", "playProbability": 0.1,
     "effect": {"home": -0.03}, "source": "Marca", "url": "https://example.com/a"},
    {"type": "weather", "team": "away", "player": None, "playProbability": None,
     "effect": {}, "source": "GSP", "url": "https://example.com/b"},
]


class FakeClient:
    def __init__(self, text=None, stop_reason="end_turn", error=None):
        self.text, self.stop_reason, self.error = text, stop_reason, error
        self.calls = []
        self.messages = SimpleNamespace(create=self.create)

    def create(self, **kwargs):
        self.calls.append(kwargs)
        if self.error:
            raise self.error
        return SimpleNamespace(stop_reason=self.stop_reason,
                               content=[SimpleNamespace(type="text", text=self.text)])


def answer(urls, text="Răspuns scurt."):
    return json.dumps({"answer": text, "sourceUrls": urls})


def test_context_has_odds_verdicts_factors_and_urls():
    context = build_context(MATCH, FAIR, BEST, FACTORS)
    for expected in ("Real Madrid - Barcelona", "2.1", "bet365", "3.4", "unibet", "3.6", "betano",
                     "2.4", "3.3", "3.1", "prea mică", "corectă", "Mbappe", "0.1", "-0.03",
                     "Marca", "https://example.com/a", "GSP", "https://example.com/b"):
        assert expected in context


def test_context_without_data_invents_nothing():
    context = build_context({"home": "A", "away": "B"}, None, None, [])
    assert "nu există cote" in context and "nu există factori" in context
    assert "http" not in context and "Sporty-Bara" not in context


def test_explain_returns_only_sources_present_in_context():
    context = build_context(MATCH, FAIR, BEST, FACTORS)
    client = FakeClient(answer(["https://example.com/a", "https://invented.example/x", "https://example.com/a"]))
    result = explain(client, context, "De ce e cota mică?")
    assert result["answer"] == "Răspuns scurt."
    assert result["sources"] == [{"title": "Marca", "url": "https://example.com/a"}]


def test_explain_request_shape():
    client = FakeClient(answer([]))
    explain(client, "ctx", "întrebare")
    call = client.calls[0]
    assert call["model"] == MODEL
    assert call["output_config"]["effort"] == "low"
    assert not {"temperature", "top_p", "top_k"} & set(call)
    assert "Profesorul Bara" in call["system"]
    assert "ctx" in call["messages"][0]["content"] and "întrebare" in call["messages"][0]["content"]


def test_explain_refusal_and_max_tokens_raise():
    for reason in ("refusal", "max_tokens"):
        with pytest.raises(ExplainError):
            explain(FakeClient(answer([]), stop_reason=reason), "ctx", "q")


def test_explain_invalid_json_raises():
    with pytest.raises(ExplainError):
        explain(FakeClient("nu e json"), "ctx", "q")


def test_explain_api_error_raises():
    request = httpx2.Request("POST", "https://api.anthropic.com/v1/messages")
    error = anthropic.APIConnectionError(request=request)
    with pytest.raises(ExplainError):
        explain(FakeClient(error=error), "ctx", "q")
