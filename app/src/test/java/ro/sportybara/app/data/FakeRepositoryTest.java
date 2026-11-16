package ro.sportybara.app.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class FakeRepositoryTest {

    private FakeRepository repo;

    @Before
    public void setUp() {
        repo = FakeRepository.newForTests();
    }

    @Test
    public void filtreazaMeciurilePeLiga() {
        assertEquals(8, repo.getMatches(null).size());
        for (Match m : repo.getMatches(Match.SUPERLIGA)) assertEquals(Match.SUPERLIGA, m.league);
    }

    @Test
    public void ceaMaiBunaCota() {
        Match m = repo.getMatch("laliga-rma-fcb");
        assertEquals("Betano", m.best(Pick.HOME).bookmaker);
        assertEquals(2.40, m.best(Pick.HOME).home, 1e-9);
        assertEquals("Unibet", m.best(Pick.AWAY).bookmaker);
    }

    @Test
    public void oSinguraSelectiePeMeci() {
        repo.addToSlip("laliga-rma-fcb", Pick.HOME);
        repo.addToSlip("laliga-rma-fcb", Pick.AWAY);
        assertEquals(1, repo.getSlip().size());
        assertEquals(Pick.AWAY, repo.pickInSlip("laliga-rma-fcb"));
    }

    @Test
    public void plasareScadeMizaSiGolesteSlipul() {
        repo.addToSlip("laliga-rma-fcb", Pick.HOME);   // 2.40
        repo.addToSlip("superliga-fcsb-cfr", Pick.HOME); // 2.10
        Ticket t = repo.placeTicket(100);

        assertEquals(5.04, t.getTotalOdd(), 1e-9);
        assertEquals(504, t.getPotentialWin());
        assertEquals(900, repo.getBalance());
        assertTrue(repo.getSlip().isEmpty());
        assertEquals(1, repo.getTickets().size());
    }

    @Test
    public void nuPlaseziFaraSoldSauFaraSelectii() {
        assertThrows(IllegalStateException.class, () -> repo.placeTicket(10));
        repo.addToSlip("laliga-rma-fcb", Pick.DRAW);
        assertThrows(IllegalArgumentException.class, () -> repo.placeTicket(0));
        assertThrows(IllegalArgumentException.class, () -> repo.placeTicket(1001));
        assertEquals(1000, repo.getBalance());
    }

    @Test
    public void modificareMizaAjusteazaSoldul() {
        repo.addToSlip("laliga-rma-fcb", Pick.HOME);
        Ticket t = repo.placeTicket(100);
        repo.updateStake(t.id, 250);
        assertEquals(750, repo.getBalance());
        repo.updateStake(t.id, 50);
        assertEquals(950, repo.getBalance());
        // poate folosi tot soldul + miza veche
        repo.updateStake(t.id, 1000);
        assertEquals(0, repo.getBalance());
        assertThrows(IllegalArgumentException.class, () -> repo.updateStake(t.id, 1001));
    }

    @Test
    public void stergereReturneazaMiza() {
        repo.addToSlip("laliga-rma-fcb", Pick.HOME);
        Ticket t = repo.placeTicket(300);
        repo.deleteTicket(t.id);
        assertEquals(1000, repo.getBalance());
        assertNull(repo.getTicket(t.id));
    }

    @Test
    public void ultimaSelectieScoasaStergeBiletul() {
        repo.addToSlip("laliga-rma-fcb", Pick.HOME);
        repo.addToSlip("laliga-atm-sev", Pick.HOME);
        Ticket t = repo.placeTicket(100);

        repo.removeSelection(t.id, "laliga-rma-fcb");
        assertEquals(1, repo.getTicket(t.id).getSelections().size());

        repo.removeSelection(t.id, "laliga-atm-sev");
        assertNull(repo.getTicket(t.id));
        assertEquals(1000, repo.getBalance());
    }

    @Test
    public void numeProfilValidat() {
        repo.setDisplayName("  Bara  ");
        assertEquals("Bara", repo.getDisplayName());
        assertThrows(IllegalArgumentException.class, () -> repo.setDisplayName("   "));
    }

    @Test
    public void ascultatoriiSuntAnuntati() {
        int[] calls = {0};
        repo.addListener(() -> calls[0]++);
        repo.addToSlip("laliga-rma-fcb", Pick.HOME);
        repo.clearSlip();
        assertEquals(2, calls[0]);
    }
}
