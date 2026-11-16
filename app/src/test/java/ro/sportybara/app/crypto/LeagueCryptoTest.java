package ro.sportybara.app.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

public class LeagueCryptoTest {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static LeagueCrypto.KeyPair keys;

    @BeforeClass
    public static void generateKeys() {
        keys = LeagueCrypto.generate(512, RANDOM);
    }

    private static Properties loadPythonFixture() throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = LeagueCryptoTest.class.getResourceAsStream("/paillier/python_fixture.properties")) {
            properties.load(stream);
        }
        return properties;
    }

    @Test
    public void decryptsCiphertextsProducedByPython() throws IOException {
        Properties fixture = loadPythonFixture();
        LeagueCrypto.PrivateKey privateKey = new LeagueCrypto.PrivateKey(
                new BigInteger(fixture.getProperty("p")), new BigInteger(fixture.getProperty("q")));
        assertEquals(fixture.getProperty("n"), privateKey.publicKey().toDecimal());
        String[] values = fixture.getProperty("values").split(",");
        String[] ciphertexts = fixture.getProperty("ciphertexts").split(",");
        assertEquals(values.length, ciphertexts.length);
        for (int i = 0; i < values.length; i++) {
            long decrypted = LeagueCrypto.decryptSigned(new BigInteger(ciphertexts[i]), privateKey);
            assertEquals(Long.parseLong(values[i]), decrypted);
        }
    }

    @Test
    public void decryptsHomomorphicSumComputedByPython() throws IOException {
        Properties fixture = loadPythonFixture();
        LeagueCrypto.PrivateKey privateKey = new LeagueCrypto.PrivateKey(
                new BigInteger(fixture.getProperty("p")), new BigInteger(fixture.getProperty("q")));
        long decrypted = LeagueCrypto.decryptSigned(new BigInteger(fixture.getProperty("sumCiphertext")), privateKey);
        assertEquals(375L, decrypted);
        assertEquals(Long.parseLong(fixture.getProperty("sumExpected")), decrypted);
    }

    @Test
    public void javaAddsPythonCiphertextsToSameSum() throws IOException {
        Properties fixture = loadPythonFixture();
        LeagueCrypto.PrivateKey privateKey = new LeagueCrypto.PrivateKey(
                new BigInteger(fixture.getProperty("p")), new BigInteger(fixture.getProperty("q")));
        LeagueCrypto.PublicKey publicKey = LeagueCrypto.PublicKey.fromDecimal(fixture.getProperty("n"));
        BigInteger total = null;
        for (String ciphertext : fixture.getProperty("ciphertexts").split(",")) {
            BigInteger value = new BigInteger(ciphertext);
            total = total == null ? value : LeagueCrypto.add(total, value, publicKey);
        }
        assertEquals(375L, LeagueCrypto.decryptSigned(total, privateKey));
    }

    @Test
    public void encryptDecryptRoundTrip() {
        for (long value : new long[]{0, 1, 120, 987654321L}) {
            BigInteger cipher = LeagueCrypto.encrypt(value, keys.publicKey(), RANDOM);
            assertEquals(value, LeagueCrypto.decryptSigned(cipher, keys.privateKey()));
        }
    }

    @Test
    public void negativeValuesRoundTrip() {
        for (long value : new long[]{-1, -45, -100000}) {
            BigInteger cipher = LeagueCrypto.encrypt(value, keys.publicKey(), RANDOM);
            assertEquals(value, LeagueCrypto.decryptSigned(cipher, keys.privateKey()));
        }
    }

    @Test
    public void homomorphicAddition() {
        BigInteger a = LeagueCrypto.encrypt(120, keys.publicKey(), RANDOM);
        BigInteger b = LeagueCrypto.encrypt(-45, keys.publicKey(), RANDOM);
        BigInteger sum = LeagueCrypto.add(a, b, keys.publicKey());
        assertEquals(75L, LeagueCrypto.decryptSigned(sum, keys.privateKey()));
    }

    @Test
    public void sameValueGivesDifferentCiphertexts() {
        BigInteger first = LeagueCrypto.encrypt(5, keys.publicKey(), RANDOM);
        BigInteger second = LeagueCrypto.encrypt(5, keys.publicKey(), RANDOM);
        assertNotEquals(first, second);
    }

    @Test
    public void decimalSerializationRoundTrip() {
        LeagueCrypto.PublicKey rebuilt = LeagueCrypto.PublicKey.fromDecimal(keys.publicKey().toDecimal());
        BigInteger cipher = LeagueCrypto.encrypt(-9, rebuilt, RANDOM);
        BigInteger parsed = LeagueCrypto.ciphertextFromDecimal(LeagueCrypto.ciphertextToDecimal(cipher));
        assertEquals(-9L, LeagueCrypto.decryptSigned(parsed, keys.privateKey()));
    }

    @Test
    public void generatedKeyHasRequestedSize() {
        assertEquals(512, keys.publicKey().n().bitLength());
        assertTrue(keys.publicKey().n().signum() > 0);
    }

    @Test
    public void standingIsSortedByProfitDescending() {
        Map<String, String> encrypted = new LinkedHashMap<>();
        encrypted.put("ana", encryptDecimal(-30));
        encrypted.put("bob", encryptDecimal(250));
        encrypted.put("cris", encryptDecimal(40));
        List<LeagueStanding.Entry> ranking = LeagueStanding.rank(encrypted, keys.privateKey());
        assertEquals("bob", ranking.get(0).uid());
        assertEquals(250L, ranking.get(0).profit());
        assertEquals("cris", ranking.get(1).uid());
        assertEquals("ana", ranking.get(2).uid());
        assertEquals(-30L, ranking.get(2).profit());
    }

    private static String encryptDecimal(long value) {
        return LeagueCrypto.ciphertextToDecimal(LeagueCrypto.encrypt(value, keys.publicKey(), RANDOM));
    }
}
