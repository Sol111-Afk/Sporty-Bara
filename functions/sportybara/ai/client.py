import anthropic

MODEL = "claude-haiku-5-5"
REQUEST_TIMEOUT_SECONDS = 60.0


def build_client() -> anthropic.Anthropic:
    return anthropic.Anthropic(timeout=REQUEST_TIMEOUT_SECONDS)
