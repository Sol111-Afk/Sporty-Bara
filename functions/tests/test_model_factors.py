from datetime import datetime

import pytest

from sportybara.model.factors import apply_factors, factor_summary, sporty_bara_odds
from sportybara.odds import fair_probabilities
from sportybara.scout.models import Factor

MARKET = {"home": 2.0, "draw": 3.5, "away": 4.0}
BEST = {"home": 2.1, "draw": 3.4, "away": 3.9}


def make_factor(team="home", type="injury", effect=None, play=None, player="Jucator"):
    return Factor(
        type=type,
        team=team,
        player=player,
        playProbability=play,
        effect=effect if effect is not None else {"home": -0.05},
        source="test",
        url="https://example.invalid",
        date=datetime(2026, 11, 1),
    )


def test_fara_factori_da_probabilitatile_pietei_fara_marja():
    probs = apply_factors(fair_probabilities(MARKET), [])
    expected = fair_probabilities(MARKET)
    for k in ("home", "draw", "away"):
        assert probs[k] == pytest.approx(expected[k])
    assert sum(probs.values()) == pytest.approx(1)


def test_accidentare_scade_probabilitatea_gazdelor():
    base = fair_probabilities(MARKET)
    probs = apply_factors(base, [make_factor(play=0.0)])
    assert sum(probs.values()) == pytest.approx(1)
    assert probs["home"] < base["home"]
    assert probs["home"] == pytest.approx((base["home"] - 0.05) / (1 - 0.05), abs=1e-9)


def test_jucator_care_joaca_sigur_nu_are_efect():
    base = fair_probabilities(MARKET)
    probs = apply_factors(base, [make_factor(play=1.0)])
    for k in ("home", "draw", "away"):
        assert probs[k] == pytest.approx(base[k])


def test_probabilitate_de_joc_0_4_da_efect_60_la_suta():
    base = fair_probabilities(MARKET)
    partial = apply_factors(base, [make_factor(play=0.4)])
    shifted_home = base["home"] - 0.6 * 0.05
    expected_home = shifted_home / (shifted_home + base["draw"] + base["away"])
    assert partial["home"] == pytest.approx(expected_home, abs=1e-9)
    assert sum(partial.values()) == pytest.approx(1)


def test_factorii_care_imping_mult_sunt_limitati_la_max_shift():
    base = fair_probabilities(MARKET)
    factors = [make_factor(effect={"home": -0.2}, play=0.0) for _ in range(5)]
    limited = apply_factors(base, factors, max_shift=0.15)
    single_capped = apply_factors(base, [make_factor(effect={"home": -0.15}, play=0.0)])
    assert limited["home"] == pytest.approx(single_capped["home"])
    assert sum(limited.values()) == pytest.approx(1)


def test_clipare_in_intervalul_0_01_0_98():
    extreme = {"home": 1.01, "draw": 60.0, "away": 60.0}
    base = fair_probabilities(extreme)
    probs = apply_factors(base, [make_factor(effect={"home": 0.5}, play=0.0)])
    expected_home = 0.98 / (0.98 + base["draw"] + base["away"])
    assert probs["home"] == pytest.approx(expected_home, abs=1e-9)
    assert sum(probs.values()) == pytest.approx(1)

    low = {"home": 1000.0, "draw": 2.0, "away": 2.0}
    base_low = fair_probabilities(low)
    probs_low = apply_factors(base_low, [make_factor(effect={"home": -0.5}, play=0.0)])
    expected_low = 0.01 / (0.01 + base_low["draw"] + base_low["away"])
    assert probs_low["home"] == pytest.approx(expected_low, abs=1e-9)
    assert sum(probs_low.values()) == pytest.approx(1)


def test_sporty_bara_odds_cota_prea_mica_da_low():
    result = sporty_bara_odds(MARKET, [], {"home": 1.5, "draw": 3.5, "away": 4.0})
    assert result["fairOdds"]["home"] == round(1 / fair_probabilities(MARKET)["home"], 2)
    assert result["verdict"]["home"] == "low"
    assert result["verdict"]["draw"] == "fair"


def test_sporty_bara_odds_cota_buna_da_high_si_fair():
    factors = [make_factor(team="away", effect={"away": 0.1}, play=0.0, type="injury")]
    result = sporty_bara_odds(MARKET, factors, {"home": 2.6, "draw": 3.5, "away": 3.3})
    assert result["fairOdds"]["away"] == round(1 / result["probs"]["away"], 2)
    assert result["verdict"]["home"] == "high"
    assert result["verdict"]["away"] == "fair"
    assert sum(result["probs"].values()) == pytest.approx(1)


def test_factor_summary_sortat_dupa_impact_absolut_si_rotunjit():
    factors = [
        make_factor(team="home", effect={"home": -0.0123}, play=0.0, player="A"),
        make_factor(team="away", effect={"away": -0.2}, play=0.5, type="suspension", player="B"),
        make_factor(team="home", effect={"home": 0.01}, play=None, type="news", player=None),
    ]
    summary = factor_summary(factors)
    assert [item["player"] for item in summary] == ["B", "A", None]
    assert summary[0] == {"type": "suspension", "team": "away", "player": "B", "impact": -0.1}
    assert summary[1]["impact"] == -0.012
    assert summary[2]["impact"] == 0.01
    assert set(summary[0]) == {"type", "team", "player", "impact"}


def test_aceeasi_accidentare_din_mai_multe_articole_se_numara_o_data():
    market = {"home": 0.5, "draw": 0.3, "away": 0.2}
    once = apply_factors(market, [make_factor(play=0.0)])
    repeated = apply_factors(market, [make_factor(play=0.0, player="Jucator"), make_factor(play=0.0, player=" jucator ")])
    assert repeated == pytest.approx(once)


def test_factorul_cel_mai_recent_castiga():
    market = {"home": 0.5, "draw": 0.3, "away": 0.2}
    old = make_factor(play=0.0)
    new = make_factor(play=1.0)
    new.date = datetime(2026, 11, 2)
    assert apply_factors(market, [old, new]) == pytest.approx(market)
    assert apply_factors(market, [new, old]) == pytest.approx(market)


def test_jucatori_diferiti_se_aduna():
    market = {"home": 0.5, "draw": 0.3, "away": 0.2}
    one = apply_factors(market, [make_factor(play=0.0, player="A")])
    two = apply_factors(market, [make_factor(play=0.0, player="A"), make_factor(play=0.0, player="B")])
    assert two["home"] < one["home"]
