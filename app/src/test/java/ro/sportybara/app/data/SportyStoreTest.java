package ro.sportybara.app.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class SportyStoreTest {

    private SportyStore store;
    private Match barcaSevilla;
    private Match fcsbRapid;

    @Before
    public void setUp() {
        store = new SportyStore(FakeData.matches(), 1000);
        barcaSevilla = store.findMatch("m1");
        fcsbRapid = store.findMatch("m4");
    }

    @Test
    public void bestOddPicksHighestBookmaker() {
        assertEquals(1.48, barcaSevilla.bestOdd(Outcome.HOME), 0.001);
        assertEquals("Betano", barcaSevilla.bestBookmaker(Outcome.HOME));
        assertEquals(6.80, barcaSevilla.bestOdd(Outcome.AWAY), 0.001);
    }

    @Test
    public void fairOddsRemoveMargin() {
        double sum = 0;
        for (Outcome o : Outcome.values()) {
            sum += 1 / barcaSevilla.fairOdd(o);
        }
        assertEquals(1.0, sum, 0.01);
    }

    @Test
    public void addToSlipReplacesSelectionForSameMatch() {
        store.addToSlip(barcaSevilla, Outcome.HOME);
        store.addToSlip(barcaSevilla, Outcome.AWAY);
        assertEquals(1, store.getSlip().size());
        assertEquals(Outcome.AWAY, store.getSlip().get(0).getOutcome());
    }

    @Test
    public void placeTicketTakesStakeAndClearsSlip() {
        store.addToSlip(barcaSevilla, Outcome.HOME);
        store.addToSlip(fcsbRapid, Outcome.DRAW);
        Ticket ticket = store.placeTicket(100);
        assertEquals(900, store.getBalance());
        assertTrue(store.getSlip().isEmpty());
        assertEquals(2, ticket.getSelections().size());
        assertEquals(Match.round(1.48 * 3.40), ticket.getTotalOdd(), 0.001);
        assertEquals((int) Math.floor(100 * ticket.getTotalOdd()), ticket.getPotentialWin());
    }

    @Test
    public void updateStakeAdjustsBalance() {
        store.addToSlip(barcaSevilla, Outcome.HOME);
        Ticket ticket = store.placeTicket(100);
        store.updateStake(ticket.getId(), 250);
        assertEquals(750, store.getBalance());
        store.updateStake(ticket.getId(), 50);
        assertEquals(950, store.getBalance());
    }

    @Test
    public void deleteTicketRefundsStake() {
        store.addToSlip(barcaSevilla, Outcome.HOME);
        Ticket ticket = store.placeTicket(200);
        store.deleteTicket(ticket.getId());
        assertEquals(1000, store.getBalance());
        assertTrue(store.getTickets().isEmpty());
    }

    @Test
    public void invalidOperationsAreRejected() {
        expect(StoreException.Reason.EMPTY_SLIP, () -> store.placeTicket(100));
        store.addToSlip(barcaSevilla, Outcome.HOME);
        expect(StoreException.Reason.INVALID_STAKE, () -> store.placeTicket(5));
        expect(StoreException.Reason.INVALID_STAKE, () -> store.placeTicket(501));
        SportyStore poor = new SportyStore(FakeData.matches(), 50);
        poor.addToSlip(barcaSevilla, Outcome.HOME);
        expect(StoreException.Reason.INSUFFICIENT_FUNDS, () -> poor.placeTicket(100));
        expect(StoreException.Reason.NOT_FOUND, () -> store.deleteTicket("nu-exista"));
    }

    @Test
    public void removeFromSlip() {
        store.addToSlip(barcaSevilla, Outcome.HOME);
        store.removeFromSlip(barcaSevilla);
        assertNull(store.slipSelectionFor(barcaSevilla));
    }

    @Test
    public void listenersAreNotified() {
        int[] calls = {0};
        store.addListener(() -> calls[0]++);
        store.addToSlip(barcaSevilla, Outcome.HOME);
        store.placeTicket(100);
        assertEquals(2, calls[0]);
    }

    @Test
    public void removeSelectionKeepsTicketWhileSelectionsRemain() {
        store.addToSlip(barcaSevilla, Outcome.HOME);
        store.addToSlip(fcsbRapid, Outcome.DRAW);
        Ticket ticket = store.placeTicket(100);
        store.removeSelection(ticket.getId(), "m4");
        assertEquals(1, store.findTicket(ticket.getId()).getSelections().size());
        assertEquals(900, store.getBalance());
    }

    @Test
    public void removingLastSelectionDeletesTicketAndRefunds() {
        store.addToSlip(barcaSevilla, Outcome.HOME);
        Ticket ticket = store.placeTicket(100);
        store.removeSelection(ticket.getId(), "m1");
        assertTrue(store.getTickets().isEmpty());
        assertEquals(1000, store.getBalance());
        expect(StoreException.Reason.NOT_FOUND, () -> store.removeSelection(ticket.getId(), "m1"));
    }

    @Test
    public void selectionRemembersBestBookmaker() {
        store.addToSlip(barcaSevilla, Outcome.HOME);
        assertEquals("Betano", store.getSlip().get(0).getBookmaker());
    }

    @Test
    public void displayNameIsValidated() {
        store.setDisplayName("  Bara  ");
        assertEquals("Bara", store.getDisplayName());
        expect(StoreException.Reason.INVALID_NAME, () -> store.setDisplayName("   "));
        expect(StoreException.Reason.INVALID_NAME, () -> store.setDisplayName("x".repeat(21)));
    }

    @Test
    public void totalStakedAndReset() {
        store.addToSlip(barcaSevilla, Outcome.HOME);
        store.placeTicket(100);
        store.addToSlip(fcsbRapid, Outcome.AWAY);
        store.placeTicket(50);
        assertEquals(150, store.getTotalStaked());
        store.setDisplayName("Ana");
        store.reset();
        assertEquals(1000, store.getBalance());
        assertTrue(store.getTickets().isEmpty());
        assertEquals(SportyStore.DEFAULT_NAME, store.getDisplayName());
        store.addToSlip(barcaSevilla, Outcome.HOME);
        assertEquals("B1", store.placeTicket(10).getId());
    }

    private static void expect(StoreException.Reason reason, Runnable action) {
        try {
            action.run();
            fail("trebuia " + reason);
        } catch (StoreException e) {
            assertEquals(reason, e.getReason());
        }
    }
}
