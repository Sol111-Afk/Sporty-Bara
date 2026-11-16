package ro.sportybara.app.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SportyStore {

    public static final int START_BALANCE = 1000;
    public static final int MIN_STAKE = 10;
    public static final int MAX_STAKE = 500;

    public interface Listener {
        void onStoreChanged();
    }

    private static SportyStore instance;

    private final List<Match> matches;
    private final List<Selection> slip = new ArrayList<>();
    private final List<Ticket> tickets = new ArrayList<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private int balance;
    private int nextTicketNumber = 1;

    public SportyStore(List<Match> matches, int balance) {
        this.matches = Collections.unmodifiableList(new ArrayList<>(matches));
        this.balance = balance;
    }

    public static synchronized SportyStore get() {
        if (instance == null) {
            instance = new SportyStore(FakeData.matches(), START_BALANCE);
        }
        return instance;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public List<Match> getMatches() {
        return matches;
    }

    public List<Match> getMatches(String league) {
        List<Match> result = new ArrayList<>();
        for (Match match : matches) {
            if (league == null || match.getLeague().equals(league)) {
                result.add(match);
            }
        }
        return result;
    }

    public Match findMatch(String id) {
        for (Match match : matches) {
            if (match.getId().equals(id)) {
                return match;
            }
        }
        throw new StoreException(StoreException.Reason.NOT_FOUND);
    }

    public int getBalance() {
        return balance;
    }

    public List<Selection> getSlip() {
        return Collections.unmodifiableList(slip);
    }

    public Selection slipSelectionFor(Match match) {
        for (Selection selection : slip) {
            if (selection.getMatch().getId().equals(match.getId())) {
                return selection;
            }
        }
        return null;
    }

    public void addToSlip(Match match, Outcome outcome) {
        Selection existing = slipSelectionFor(match);
        if (existing != null) {
            slip.remove(existing);
        }
        slip.add(new Selection(match, outcome, match.bestOdd(outcome)));
        notifyListeners();
    }

    public void removeFromSlip(Match match) {
        Selection existing = slipSelectionFor(match);
        if (existing != null) {
            slip.remove(existing);
            notifyListeners();
        }
    }

    public void clearSlip() {
        slip.clear();
        notifyListeners();
    }

    public double slipTotalOdd() {
        double total = 1;
        for (Selection selection : slip) {
            total *= selection.getOdd();
        }
        return Match.round(total);
    }

    public Ticket placeTicket(int stake) {
        if (slip.isEmpty()) {
            throw new StoreException(StoreException.Reason.EMPTY_SLIP);
        }
        checkStake(stake);
        if (stake > balance) {
            throw new StoreException(StoreException.Reason.INSUFFICIENT_FUNDS);
        }
        Ticket ticket = new Ticket("B" + nextTicketNumber++, slip, stake);
        tickets.add(0, ticket);
        balance -= stake;
        slip.clear();
        notifyListeners();
        return ticket;
    }

    public void updateStake(String ticketId, int newStake) {
        Ticket ticket = findTicket(ticketId);
        checkStake(newStake);
        int difference = newStake - ticket.getStake();
        if (difference > balance) {
            throw new StoreException(StoreException.Reason.INSUFFICIENT_FUNDS);
        }
        balance -= difference;
        ticket.setStake(newStake);
        notifyListeners();
    }

    public void deleteTicket(String ticketId) {
        Ticket ticket = findTicket(ticketId);
        tickets.remove(ticket);
        balance += ticket.getStake();
        notifyListeners();
    }

    public List<Ticket> getTickets() {
        return Collections.unmodifiableList(tickets);
    }

    public Ticket findTicket(String ticketId) {
        for (Ticket ticket : tickets) {
            if (ticket.getId().equals(ticketId)) {
                return ticket;
            }
        }
        throw new StoreException(StoreException.Reason.NOT_FOUND);
    }

    private void checkStake(int stake) {
        if (stake < MIN_STAKE || stake > MAX_STAKE) {
            throw new StoreException(StoreException.Reason.INVALID_STAKE);
        }
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onStoreChanged();
        }
    }
}
