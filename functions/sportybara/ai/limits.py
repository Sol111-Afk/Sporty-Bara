DAILY_LIMIT = 10


class LimitExceeded(Exception):
    pass


def usage_key(uid: str, today: str) -> str:
    return f"{uid}_{today}"


def check_and_count(store, uid: str, today: str, limit: int = DAILY_LIMIT) -> int:
    key = usage_key(uid, today)
    used = store.get(key, 0)
    if used >= limit:
        raise LimitExceeded(f"limita de {limit} întrebări pe zi a fost atinsă")
    store[key] = used + 1
    return used + 1
