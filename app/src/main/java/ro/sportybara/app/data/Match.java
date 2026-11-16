package ro.sportybara.app.data;

import java.util.Collections;
import java.util.List;

/** Un meci cu cotele tuturor caselor (versiune simulată a matches/{matchId}). */
public final class Match {
    public static final String LALIGA = "laliga";
    public static final String SUPERLIGA = "superliga";

    public final String id;
    public final String league;
    public final String home;
    public final String away;
    /** Ex. „Sâm, 21:00”. În Partea 1 datele sunt simulate, deci ora e doar text. */
    public final String kickoff;
    public final List<BookmakerOdds> odds;

    public Match(String id, String league, String home, String away, String kickoff,
                 List<BookmakerOdds> odds) {
        this.id = id;
        this.league = league;
        this.home = home;
        this.away = away;
        this.kickoff = kickoff;
        this.odds = Collections.unmodifiableList(odds);
    }

    /** Cea mai bună cotă pentru un rezultat (echivalentul lui bestOdds). */
    public BookmakerOdds best(Pick pick) {
        BookmakerOdds best = odds.get(0);
        for (BookmakerOdds o : odds) {
            if (o.get(pick) > best.get(pick)) best = o;
        }
        return best;
    }

    public String title() {
        return home + " – " + away;
    }
}
