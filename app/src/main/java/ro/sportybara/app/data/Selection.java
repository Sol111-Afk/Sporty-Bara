package ro.sportybara.app.data;

public class Selection {

    private final Match match;
    private final Outcome outcome;
    private final double odd;

    public Selection(Match match, Outcome outcome, double odd) {
        this.match = match;
        this.outcome = outcome;
        this.odd = odd;
    }

    public Match getMatch() {
        return match;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public double getOdd() {
        return odd;
    }
}
