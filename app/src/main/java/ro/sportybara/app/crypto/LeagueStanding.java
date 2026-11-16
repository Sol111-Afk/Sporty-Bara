package ro.sportybara.app.crypto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class LeagueStanding {

    private LeagueStanding() {
    }

    public static final class Entry {
        private final String uid;
        private final long profit;

        public Entry(String uid, long profit) {
            this.uid = uid;
            this.profit = profit;
        }

        public String uid() {
            return uid;
        }

        public long profit() {
            return profit;
        }
    }

    public static List<Entry> rank(Map<String, String> encryptedProfitByUid, LeagueCrypto.PrivateKey key) {
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, String> source : encryptedProfitByUid.entrySet()) {
            long profit = LeagueCrypto.decryptSigned(
                    LeagueCrypto.ciphertextFromDecimal(source.getValue()), key);
            entries.add(new Entry(source.getKey(), profit));
        }
        Collections.sort(entries, (a, b) -> Long.compare(b.profit, a.profit));
        return entries;
    }
}
