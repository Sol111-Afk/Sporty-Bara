import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sportybara import league_crypto

VALUES = [120, -45, 0, 300]
KEY_BITS = 512
DEFAULT_OUTPUT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "..", "app", "src", "test", "resources", "paillier", "python_fixture.properties",
)


def build_fixture():
    pub, priv = league_crypto.generate_keypair(KEY_BITS)
    ciphertexts = [league_crypto.encrypt_int(pub, value) for value in VALUES]
    total = ciphertexts[0]
    for ciphertext in ciphertexts[1:]:
        total = league_crypto.add_ciphertexts(pub, total, ciphertext)
    return {
        "n": str(pub.n),
        "p": str(priv.p),
        "q": str(priv.q),
        "values": ",".join(str(value) for value in VALUES),
        "ciphertexts": ",".join(ciphertexts),
        "sumCiphertext": total,
        "sumExpected": str(sum(VALUES)),
    }


def main():
    output = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OUTPUT
    fixture = build_fixture()
    with open(output, "w") as handle:
        for key, value in fixture.items():
            handle.write(f"{key}={value}\n")


if __name__ == "__main__":
    main()
