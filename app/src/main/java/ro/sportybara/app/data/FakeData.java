package ro.sportybara.app.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FakeData {

    public static final String LALIGA = "La Liga";
    public static final String SUPERLIGA = "Superliga";

    private FakeData() {
    }

    public static List<Match> matches() {
        List<Match> matches = new ArrayList<>();
        matches.add(match("m1", LALIGA, "Barcelona", "Sevilla", "Sâm 22:00",
                new double[]{1.45, 4.60, 6.50}, new double[]{1.48, 4.50, 6.20}, new double[]{1.42, 4.75, 6.80}));
        matches.add(match("m2", LALIGA, "Real Madrid", "Valencia", "Dum 17:15",
                new double[]{1.38, 5.00, 7.50}, new double[]{1.40, 4.80, 7.25}, new double[]{1.36, 5.20, 8.00}));
        matches.add(match("m3", LALIGA, "Atlético Madrid", "Real Betis", "Dum 22:00",
                new double[]{1.80, 3.60, 4.50}, new double[]{1.85, 3.50, 4.40}, new double[]{1.75, 3.70, 4.60}));
        matches.add(match("m4", SUPERLIGA, "FCSB", "Rapid", "Sâm 20:30",
                new double[]{2.10, 3.30, 3.40}, new double[]{2.05, 3.40, 3.50}, new double[]{2.20, 3.25, 3.30}));
        matches.add(match("m5", SUPERLIGA, "CFR Cluj", "Universitatea Craiova", "Dum 20:00",
                new double[]{2.40, 3.20, 2.90}, new double[]{2.35, 3.25, 3.00}, new double[]{2.50, 3.10, 2.85}));
        matches.add(match("m6", SUPERLIGA, "Dinamo", "U Cluj", "Lun 19:00",
                new double[]{2.60, 3.10, 2.75}, new double[]{2.70, 3.05, 2.70}, new double[]{2.55, 3.20, 2.80}));
        return matches;
    }

    private static Match match(String id, String league, String home, String away, String kickoff,
                               double[] superbet, double[] betano, double[] unibet) {
        Map<String, double[]> odds = new LinkedHashMap<>();
        odds.put("Superbet", superbet);
        odds.put("Betano", betano);
        odds.put("Unibet", unibet);
        return new Match(id, league, home, away, kickoff, odds);
    }
}
