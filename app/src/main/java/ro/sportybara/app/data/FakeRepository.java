package ro.sportybara.app.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Date simulate, ținute în memorie (Partea 1: prototip fără Firebase).
 * Mai târziu, aceleași operații vor merge prin Firestore + Cloud Functions.
 *
 * Reguli (aceleași ca pe server):
 *  - miza se scade din sold la plasare și se returnează la ștergerea biletului;
 *  - la modificarea mizei se ajustează soldul cu diferența;
 *  - pe un bilet poate fi o singură selecție per meci.
 */
public final class FakeRepository {

    public static final int START_BALANCE = 1000;
    public static final int MAX_SELECTIONS = 10;

    /** Anunță ecranele când se schimbă datele. */
    public interface Listener {
        void onDataChanged();
    }

    private static FakeRepository instance;

    public static synchronized FakeRepository get() {
        if (instance == null) instance = new FakeRepository();
        return instance;
    }

    private final List<Match> matches = seedMatches();
    private final List<Selection> slip = new ArrayList<>();
    private final List<Ticket> tickets = new ArrayList<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private int balance = START_BALANCE;
    private int nextTicketNumber = 1;
    private String displayName = "Capibara Curajoasă";

    // ---- ascultători ----

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void notifyChanged() {
        for (Listener l : listeners) l.onDataChanged();
    }

    // ---- citire ----

    public List<Match> getMatches(String league) {
        if (league == null) return Collections.unmodifiableList(matches);
        List<Match> out = new ArrayList<>();
        for (Match m : matches) if (m.league.equals(league)) out.add(m);
        return out;
    }

    public Match getMatch(String id) {
        for (Match m : matches) if (m.id.equals(id)) return m;
        return null;
    }

    public List<Selection> getSlip() {
        return Collections.unmodifiableList(slip);
    }

    /** Biletele plasate, cel mai nou primul. */
    public List<Ticket> getTickets() {
        List<Ticket> out = new ArrayList<>(tickets);
        Collections.reverse(out);
        return out;
    }

    public Ticket getTicket(String id) {
        for (Ticket t : tickets) if (t.id.equals(id)) return t;
        return null;
    }

    public int getBalance() {
        return balance;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTotalStaked() {
        int sum = 0;
        for (Ticket t : tickets) sum += t.getStake();
        return sum;
    }

    // ---- bilet curent (slip) ----

    /** Adaugă sau înlocuiește selecția pentru meci, cu cea mai bună cotă din acel moment. */
    public Selection addToSlip(String matchId, Pick pick) {
        Match m = requireMatch(matchId);
        BookmakerOdds best = m.best(pick);
        Selection s = new Selection(m.id, m.title(), pick, best.get(pick), best.bookmaker);
        int existing = indexInSlip(matchId);
        if (existing >= 0) {
            slip.set(existing, s);
        } else {
            if (slip.size() >= MAX_SELECTIONS) {
                throw new IllegalStateException("Maxim " + MAX_SELECTIONS + " selecții pe bilet");
            }
            slip.add(s);
        }
        notifyChanged();
        return s;
    }

    public void removeFromSlip(String matchId) {
        int i = indexInSlip(matchId);
        if (i >= 0) {
            slip.remove(i);
            notifyChanged();
        }
    }

    public void clearSlip() {
        slip.clear();
        notifyChanged();
    }

    public Pick pickInSlip(String matchId) {
        int i = indexInSlip(matchId);
        return i >= 0 ? slip.get(i).pick : null;
    }

    /** Plasează biletul curent: scade miza din sold și golește slip-ul. */
    public Ticket placeTicket(int stake) {
        if (slip.isEmpty()) throw new IllegalStateException("Biletul nu are selecții");
        checkStake(stake, balance);
        Ticket t = new Ticket("T" + nextTicketNumber++, slip, stake);
        tickets.add(t);
        balance -= stake;
        slip.clear();
        notifyChanged();
        return t;
    }

    // ---- bilete plasate ----

    /** Modifică miza; soldul se ajustează cu diferența. */
    public void updateStake(String ticketId, int newStake) {
        Ticket t = requireTicket(ticketId);
        checkStake(newStake, balance + t.getStake());
        balance += t.getStake() - newStake;
        t.setStake(newStake);
        notifyChanged();
    }

    /** Scoate o selecție de pe bilet. Dacă nu mai rămâne niciuna, biletul se șterge (cu returnarea mizei). */
    public void removeSelection(String ticketId, String matchId) {
        Ticket t = requireTicket(ticketId);
        t.removeSelection(matchId);
        if (t.getSelections().isEmpty()) {
            deleteTicket(ticketId);
        } else {
            notifyChanged();
        }
    }

    /** Șterge biletul și returnează miza. */
    public void deleteTicket(String ticketId) {
        Ticket t = requireTicket(ticketId);
        tickets.remove(t);
        balance += t.getStake();
        notifyChanged();
    }

    // ---- profil ----

    public void setDisplayName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Numele nu poate fi gol");
        if (trimmed.length() > 30) throw new IllegalArgumentException("Maxim 30 de caractere");
        displayName = trimmed;
        notifyChanged();
    }

    /** Readuce datele demo la starea inițială. */
    public void reset() {
        slip.clear();
        tickets.clear();
        balance = START_BALANCE;
        nextTicketNumber = 1;
        notifyChanged();
    }

    // ---- intern ----

    private static void checkStake(int stake, int available) {
        if (stake < 1) throw new IllegalArgumentException("Miza minimă e 1 Bara Coin");
        if (stake > available) throw new IllegalArgumentException("Sold insuficient");
    }

    private int indexInSlip(String matchId) {
        for (int i = 0; i < slip.size(); i++) if (slip.get(i).matchId.equals(matchId)) return i;
        return -1;
    }

    private Match requireMatch(String id) {
        Match m = getMatch(id);
        if (m == null) throw new IllegalArgumentException("Meci inexistent: " + id);
        return m;
    }

    private Ticket requireTicket(String id) {
        Ticket t = getTicket(id);
        if (t == null) throw new IllegalArgumentException("Bilet inexistent: " + id);
        return t;
    }

    private static final String SUPERBET = "Superbet";
    private static final String BETANO = "Betano";
    private static final String UNIBET = "Unibet";
    private static final String CASA = "Casa Pariurilor";

    private static List<BookmakerOdds> odds(double[] sb, double[] bt, double[] ub, double[] cp) {
        return Arrays.asList(
                new BookmakerOdds(SUPERBET, sb[0], sb[1], sb[2]),
                new BookmakerOdds(BETANO, bt[0], bt[1], bt[2]),
                new BookmakerOdds(UNIBET, ub[0], ub[1], ub[2]),
                new BookmakerOdds(CASA, cp[0], cp[1], cp[2]));
    }

    private static double[] o(double h, double d, double a) {
        return new double[]{h, d, a};
    }

    private static List<Match> seedMatches() {
        return new ArrayList<>(Arrays.asList(
                new Match("laliga-rma-fcb", Match.LALIGA, "Real Madrid", "Barcelona", "Sâm, 22:00",
                        odds(o(2.35, 3.70, 2.85), o(2.40, 3.60, 2.80), o(2.30, 3.75, 2.90), o(2.38, 3.65, 2.82))),
                new Match("laliga-atm-sev", Match.LALIGA, "Atlético Madrid", "Sevilla", "Sâm, 19:30",
                        odds(o(1.62, 3.90, 5.50), o(1.65, 3.85, 5.25), o(1.60, 4.00, 5.60), o(1.63, 3.80, 5.40))),
                new Match("laliga-rso-ath", Match.LALIGA, "Real Sociedad", "Athletic Club", "Dum, 17:15",
                        odds(o(2.55, 3.20, 2.90), o(2.60, 3.15, 2.85), o(2.50, 3.25, 2.95), o(2.58, 3.10, 2.88))),
                new Match("laliga-vil-val", Match.LALIGA, "Villarreal", "Valencia", "Dum, 22:00",
                        odds(o(1.95, 3.50, 4.00), o(1.98, 3.45, 3.90), o(1.92, 3.55, 4.10), o(1.97, 3.40, 3.95))),
                new Match("superliga-fcsb-cfr", Match.SUPERLIGA, "FCSB", "CFR Cluj", "Sâm, 20:30",
                        odds(o(2.05, 3.30, 3.70), o(2.10, 3.25, 3.60), o(2.00, 3.35, 3.80), o(2.08, 3.20, 3.65))),
                new Match("superliga-rap-ucv", Match.SUPERLIGA, "Rapid", "Universitatea Craiova", "Dum, 20:00",
                        odds(o(2.45, 3.10, 2.95), o(2.50, 3.05, 2.90), o(2.40, 3.15, 3.00), o(2.48, 3.00, 2.92))),
                new Match("superliga-din-far", Match.SUPERLIGA, "Dinamo", "Farul", "Vin, 21:00",
                        odds(o(2.20, 3.15, 3.40), o(2.25, 3.10, 3.30), o(2.15, 3.20, 3.50), o(2.22, 3.05, 3.35))),
                new Match("superliga-ucj-pet", Match.SUPERLIGA, "U Cluj", "Petrolul", "Lun, 19:00",
                        odds(o(1.85, 3.25, 4.60), o(1.88, 3.20, 4.50), o(1.82, 3.30, 4.75), o(1.87, 3.15, 4.55)))
        ));
    }

    /** Doar pentru teste: o instanță nouă, separată de cea a aplicației. */
    static FakeRepository newForTests() {
        return new FakeRepository();
    }
}
