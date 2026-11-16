package ro.sportybara.app.data;

public class Selection {

    private final Match match;
    private final Outcome outcome;
    private final double odd;
    private final String bookmaker;

    public Selection(Match match, Outcome outcome, double odd, String bookmaker) {
        this.match = match;
        this.outcome = outcome;
        this.odd = odd;
        this.bookmaker = bookmaker;
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

    public String getBookmaker() {
        return bookmaker;
    }
}
