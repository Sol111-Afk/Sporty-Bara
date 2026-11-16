package ro.sportybara.app.data;

/** Cotele 1X2 ale unei case de pariuri pentru un meci (ca în matches/{id}/odds/{bookmaker}). */
public final class BookmakerOdds {
    public final String bookmaker;
    public final double home;
    public final double draw;
    public final double away;

    public BookmakerOdds(String bookmaker, double home, double draw, double away) {
        this.bookmaker = bookmaker;
        this.home = home;
        this.draw = draw;
        this.away = away;
    }

    public double get(Pick pick) {
        switch (pick) {
            case HOME: return home;
            case DRAW: return draw;
            default: return away;
        }
    }
}
