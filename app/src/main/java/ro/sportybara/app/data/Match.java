package ro.sportybara.app.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Match {

    private static final double VERDICT_TOLERANCE = 0.05;

    private final String id;
    private final String league;
    private final String home;
    private final String away;
    private final String kickoff;
    private final Map<String, double[]> odds;

    public Match(String id, String league, String home, String away, String kickoff, Map<String, double[]> odds) {
        this.id = id;
        this.league = league;
        this.home = home;
        this.away = away;
        this.kickoff = kickoff;
        this.odds = Collections.unmodifiableMap(new LinkedHashMap<>(odds));
    }

    public String getId() {
        return id;
    }

    public String getLeague() {
        return league;
    }

    public String getHome() {
        return home;
    }

    public String getAway() {
        return away;
    }

    public String getKickoff() {
        return kickoff;
    }

    public Map<String, double[]> getOdds() {
        return odds;
    }

    public double odd(String bookmaker, Outcome outcome) {
        return odds.get(bookmaker)[outcome.ordinal()];
    }

    public double bestOdd(Outcome outcome) {
        double best = 0;
        for (double[] values : odds.values()) {
            best = Math.max(best, values[outcome.ordinal()]);
        }
        return best;
    }

    public String bestBookmaker(Outcome outcome) {
        String best = null;
        double bestValue = 0;
        for (Map.Entry<String, double[]> entry : odds.entrySet()) {
            double value = entry.getValue()[outcome.ordinal()];
            if (value > bestValue) {
                bestValue = value;
                best = entry.getKey();
            }
        }
        return best;
    }

    public double fairOdd(Outcome outcome) {
        double[] probabilities = new double[3];
        double total = 0;
        for (Outcome o : Outcome.values()) {
            double sum = 0;
            for (double[] values : odds.values()) {
                sum += 1 / values[o.ordinal()];
            }
            probabilities[o.ordinal()] = sum / odds.size();
            total += probabilities[o.ordinal()];
        }
        return round(total / probabilities[outcome.ordinal()]);
    }

    public Verdict verdict(Outcome outcome) {
        double best = bestOdd(outcome);
        double fair = fairOdd(outcome);
        if (best < fair * (1 - VERDICT_TOLERANCE)) {
            return Verdict.LOW;
        }
        if (best > fair * (1 + VERDICT_TOLERANCE)) {
            return Verdict.HIGH;
        }
        return Verdict.FAIR;
    }

    public String title() {
        return home + " – " + away;
    }

    static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
