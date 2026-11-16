import pytest

from sportybara.ai.limits import LimitExceeded, check_and_count


def test_counts_below_limit():
    store = {}
    assert check_and_count(store, "u1", "2026-11-16", limit=3) == 1
    assert check_and_count(store, "u1", "2026-11-16", limit=3) == 2


def test_raises_at_limit():
    store = {}
    for _ in range(3):
        check_and_count(store, "u1", "2026-11-16", limit=3)
    with pytest.raises(LimitExceeded):
        check_and_count(store, "u1", "2026-11-16", limit=3)
    assert store["u1_2026-11-16"] == 3


def test_new_day_resets():
    store = {}
    for _ in range(3):
        check_and_count(store, "u1", "2026-11-16", limit=3)
    assert check_and_count(store, "u1", "2026-11-17", limit=3) == 1


def test_users_are_independent():
    store = {}
    for _ in range(3):
        check_and_count(store, "u1", "2026-11-16", limit=3)
    assert check_and_count(store, "u2", "2026-11-16", limit=3) == 1


def test_default_limit_is_ten():
    store = {}
    for _ in range(10):
        check_and_count(store, "u1", "d")
    with pytest.raises(LimitExceeded):
        check_and_count(store, "u1", "d")
