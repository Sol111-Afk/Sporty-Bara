from phe import paillier

STAT_FIELDS = ("encProfit", "encWon", "encTotal")


def generate_keypair(bits):
    return paillier.generate_paillier_keypair(n_length=bits)


def public_key_from_n(n_str):
    return paillier.PaillierPublicKey(int(n_str))


def encrypt_int(pub, value):
    return str(pub.raw_encrypt(int(value) % pub.n))


def add_ciphertexts(pub, c1_str, c2_str):
    return str(int(c1_str) * int(c2_str) % pub.nsquare)


def zero(pub):
    return encrypt_int(pub, 0)


def decrypt_int(priv, c_str):
    raw = priv.raw_decrypt(int(c_str))
    n = priv.public_key.n
    return raw - n if raw > n // 2 else raw


def apply_ticket_result(pub, stats_doc, profit, won):
    current = stats_doc or {}
    deltas = {
        "encProfit": profit,
        "encWon": 1 if won else 0,
        "encTotal": 1,
    }
    updated = dict(current)
    for field in STAT_FIELDS:
        base = current.get(field) or zero(pub)
        updated[field] = add_ciphertexts(pub, base, encrypt_int(pub, deltas[field]))
    return updated
