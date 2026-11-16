package ro.sportybara.app.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class LeagueCrypto {

    public static final int PRODUCTION_KEY_BITS = 2048;

    private LeagueCrypto() {
    }

    public static final class PublicKey {
        private final BigInteger n;
        private final BigInteger nSquared;

        public PublicKey(BigInteger n) {
            this.n = n;
            this.nSquared = n.multiply(n);
        }

        public static PublicKey fromDecimal(String decimal) {
            return new PublicKey(new BigInteger(decimal));
        }

        public String toDecimal() {
            return n.toString();
        }

        public BigInteger n() {
            return n;
        }

        public BigInteger nSquared() {
            return nSquared;
        }
    }

    public static final class PrivateKey {
        private final PublicKey publicKey;
        private final BigInteger lambda;
        private final BigInteger mu;

        public PrivateKey(BigInteger p, BigInteger q) {
            this.publicKey = new PublicKey(p.multiply(q));
            this.lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
            this.mu = lambda.modInverse(publicKey.n());
        }

        public PublicKey publicKey() {
            return publicKey;
        }
    }

    public static final class KeyPair {
        private final PublicKey publicKey;
        private final PrivateKey privateKey;

        KeyPair(PublicKey publicKey, PrivateKey privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public PublicKey publicKey() {
            return publicKey;
        }

        public PrivateKey privateKey() {
            return privateKey;
        }
    }

    public static KeyPair generate(int bits, SecureRandom random) {
        int primeBits = bits / 2;
        while (true) {
            BigInteger p = BigInteger.probablePrime(primeBits, random);
            BigInteger q = BigInteger.probablePrime(primeBits, random);
            if (p.equals(q)) {
                continue;
            }
            BigInteger n = p.multiply(q);
            BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
            if (n.bitLength() != bits || !n.gcd(phi).equals(BigInteger.ONE)) {
                continue;
            }
            PrivateKey privateKey = new PrivateKey(p, q);
            return new KeyPair(privateKey.publicKey(), privateKey);
        }
    }

    public static BigInteger encrypt(BigInteger message, PublicKey key, SecureRandom random) {
        BigInteger n = key.n();
        BigInteger encoded = message.mod(n);
        BigInteger r;
        do {
            r = new BigInteger(n.bitLength(), random);
        } while (r.signum() == 0 || r.compareTo(n) >= 0 || !r.gcd(n).equals(BigInteger.ONE));
        BigInteger gm = n.multiply(encoded).add(BigInteger.ONE).mod(key.nSquared());
        return gm.multiply(r.modPow(n, key.nSquared())).mod(key.nSquared());
    }

    public static BigInteger encrypt(long message, PublicKey key, SecureRandom random) {
        return encrypt(BigInteger.valueOf(message), key, random);
    }

    public static BigInteger add(BigInteger c1, BigInteger c2, PublicKey key) {
        return c1.multiply(c2).mod(key.nSquared());
    }

    public static long decryptSigned(BigInteger ciphertext, PrivateKey key) {
        BigInteger n = key.publicKey.n();
        BigInteger u = ciphertext.modPow(key.lambda, key.publicKey.nSquared());
        BigInteger l = u.subtract(BigInteger.ONE).divide(n);
        BigInteger decoded = l.multiply(key.mu).mod(n);
        if (decoded.compareTo(n.shiftRight(1)) > 0) {
            decoded = decoded.subtract(n);
        }
        if (decoded.bitLength() > 63) {
            throw new ArithmeticException("valoare prea mare pentru long");
        }
        return decoded.longValue();
    }

    public static BigInteger ciphertextFromDecimal(String decimal) {
        return new BigInteger(decimal);
    }

    public static String ciphertextToDecimal(BigInteger ciphertext) {
        return ciphertext.toString();
    }
}
