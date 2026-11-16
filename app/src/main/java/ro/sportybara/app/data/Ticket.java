package ro.sportybara.app.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Ticket {

    private final String id;
    private final List<Selection> selections;
    private int stake;

    public Ticket(String id, List<Selection> selections, int stake) {
        this.id = id;
        this.selections = new ArrayList<>(selections);
        this.stake = stake;
    }

    public String getId() {
        return id;
    }

    public List<Selection> getSelections() {
        return Collections.unmodifiableList(selections);
    }

    boolean removeSelection(String matchId) {
        return selections.removeIf(selection -> selection.getMatch().getId().equals(matchId));
    }

    public int getStake() {
        return stake;
    }

    void setStake(int stake) {
        this.stake = stake;
    }

    public double getTotalOdd() {
        return totalOdd(selections);
    }

    public int getPotentialWin() {
        return potentialWin(stake, getTotalOdd());
    }

    public static double totalOdd(List<Selection> selections) {
        double total = 1;
        for (Selection selection : selections) {
            total *= selection.getOdd();
        }
        return Match.round(total);
    }

    public static int potentialWin(int stake, double totalOdd) {
        return (int) Math.floor(stake * totalOdd);
    }
}
