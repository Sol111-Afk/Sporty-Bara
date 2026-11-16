package ro.sportybara.app.data;

public enum Outcome {
    HOME("1"),
    DRAW("X"),
    AWAY("2");

    private final String label;

    Outcome(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
