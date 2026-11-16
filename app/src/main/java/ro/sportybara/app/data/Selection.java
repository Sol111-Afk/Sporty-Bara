package ro.sportybara.app.data;

/** O selecție de pe bilet. Cota se fixează în momentul adăugării și nu se mai schimbă. */
public final class Selection {
    public final String matchId;
    public final String matchTitle;
    public final Pick pick;
    public final double odd;
    public final String bookmaker;

    public Selection(String matchId, String matchTitle, Pick pick, double odd, String bookmaker) {
        this.matchId = matchId;
        this.matchTitle = matchTitle;
        this.pick = pick;
        this.odd = odd;
        this.bookmaker = bookmaker;
    }
}
