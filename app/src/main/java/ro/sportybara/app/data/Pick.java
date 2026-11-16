package ro.sportybara.app.data;

/** Rezultatul pe care se pariază (piața 1X2). */
public enum Pick {
    HOME("1"),
    DRAW("X"),
    AWAY("2");

    public final String label;

    Pick(String label) {
        this.label = label;
    }
}
