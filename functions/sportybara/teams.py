import re
import unicodedata

ALIASES = {
    "atletico-madrid": "atletico-madrid",
    "atletico-de-madrid": "atletico-madrid",
    "atl-madrid": "atletico-madrid",
    "athletic-bilbao": "athletic-bilbao",
    "athletic-club": "athletic-bilbao",
    "real-betis": "real-betis",
    "betis": "real-betis",
    "celta-vigo": "celta-vigo",
    "celta-de-vigo": "celta-vigo",
    "alaves": "alaves",
    "deportivo-alaves": "alaves",
}


def slug(name: str) -> str:
    fara_diacritice = unicodedata.normalize("NFKD", name)
    fara_diacritice = "".join(c for c in fara_diacritice if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", "-", fara_diacritice.lower()).strip("-")


def canonical(name: str) -> str:
    s = slug(name)
    return ALIASES.get(s, s)
