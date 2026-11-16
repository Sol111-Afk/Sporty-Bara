package ro.sportybara.app.ui;

import java.util.Locale;

public final class Odds {

    private Odds() {
    }

    public static String format(double odd) {
        return String.format(Locale.ROOT, "%.2f", odd);
    }
}
