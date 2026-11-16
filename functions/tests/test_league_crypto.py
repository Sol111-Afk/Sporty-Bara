import pytest

from sportybara import league_crypto


@pytest.fixture(scope="module")
def keys():
    return league_crypto.generate_keypair(512)


def test_encrypt_decrypt_roundtrip(keys):
    pub, priv = keys
    for value in [0, 1, 120, 987654321]:
        cipher = league_crypto.encrypt_int(pub, value)
        assert isinstance(cipher, str)
        assert league_crypto.decrypt_int(priv, cipher) == value


def test_negative_values(keys):
    pub, priv = keys
    for value in [-1, -45, -100000]:
        assert league_crypto.decrypt_int(priv, league_crypto.encrypt_int(pub, value)) == value


def test_homomorphic_addition(keys):
    pub, priv = keys
    a = league_crypto.encrypt_int(pub, 120)
    b = league_crypto.encrypt_int(pub, -45)
    assert league_crypto.decrypt_int(priv, league_crypto.add_ciphertexts(pub, a, b)) == 75


def test_zero_decrypts_to_zero(keys):
    pub, priv = keys
    assert league_crypto.decrypt_int(priv, league_crypto.zero(pub)) == 0


def test_public_key_from_n_roundtrip(keys):
    pub, priv = keys
    rebuilt = league_crypto.public_key_from_n(str(pub.n))
    assert league_crypto.decrypt_int(priv, league_crypto.encrypt_int(rebuilt, -7)) == -7


def test_same_value_gives_different_ciphertexts(keys):
    pub, _ = keys
    assert league_crypto.encrypt_int(pub, 5) != league_crypto.encrypt_int(pub, 5)


def test_apply_ticket_result_accumulates(keys):
    pub, priv = keys
    results = [(50, True), (-20, False), (-20, False), (130, True)]
    doc = None
    for profit, won in results:
        doc = league_crypto.apply_ticket_result(pub, doc, profit, won)
    assert league_crypto.decrypt_int(priv, doc["encProfit"]) == 140
    assert league_crypto.decrypt_int(priv, doc["encWon"]) == 2
    assert league_crypto.decrypt_int(priv, doc["encTotal"]) == 4


def test_apply_ticket_result_keeps_other_fields(keys):
    pub, _ = keys
    doc = league_crypto.apply_ticket_result(pub, {"leagueId": "l1", "uid": "u1"}, 10, True)
    assert doc["leagueId"] == "l1"
    assert doc["uid"] == "u1"


def test_document_has_no_plaintext(keys):
    pub, _ = keys
    doc = None
    for profit, won in [(777, True), (-333, False)]:
        doc = league_crypto.apply_ticket_result(pub, doc, profit, won)
    for field in league_crypto.STAT_FIELDS:
        assert isinstance(doc[field], str)
        assert len(doc[field]) > 100
    assert set(doc) == set(league_crypto.STAT_FIELDS)
    assert all(int(doc[field]) > 10**100 for field in league_crypto.STAT_FIELDS)
