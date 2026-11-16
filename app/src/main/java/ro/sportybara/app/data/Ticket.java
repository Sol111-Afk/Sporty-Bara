package ro.sportybara.app.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Un bilet plasat (versiune simulată a tickets/{ticketId}). */
public final class Ticket {
    public final String id;
    private final List<Selection> selections;
    private int stake;

    Ticket(String id, List<Selection> selections, int stake) {
        this.id = id;
        this.selections = new ArrayList<>(selections);
        this.stake = stake;
    }

    public List<Selection> getSelections() {
        return Collections.unmodifiableList(selections);
    }

    public int getStake() {
        return stake;
    }

    void setStake(int stake) {
        this.stake = stake;
    }

    void removeSelection(String matchId) {
        for (int i = 0; i < selections.size(); i++) {
            if (selections.get(i).matchId.equals(matchId)) {
                selections.remove(i);
                return;
            }
        }
    }

    public double getTotalOdd() {
        return totalOdd(selections);
    }

    public int getPotentialWin() {
        return potentialWin(stake, getTotalOdd());
    }

    /** Produsul cotelor, rotunjit la 2 zecimale (cum se afișează). */
    public static double totalOdd(List<Selection> selections) {
        double total = 1.0;
        for (Selection s : selections) total *= s.odd;
        return Math.round(total * 100.0) / 100.0;
    }

    /** floor(miză × cotă totală), ca în docs/API.md. Epsilonul evită 28.999… în loc de 29. */
    public static int potentialWin(int stake, double totalOdd) {
        return (int) Math.floor(stake * totalOdd + 1e-9);
    }
}
