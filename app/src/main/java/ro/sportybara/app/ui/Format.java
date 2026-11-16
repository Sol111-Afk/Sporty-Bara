package ro.sportybara.app.ui;

import java.util.Locale;

import ro.sportybara.app.data.Match;

/** Formatări comune pentru ecrane. */
public final class Format {
    private Format() {
    }

    /** Cotele se afișează mereu cu punct și 2 zecimale (2.40), indiferent de limba telefonului. */
    public static String odd(double odd) {
        return String.format(Locale.US, "%.2f", odd);
    }

    public static String league(String league) {
        return Match.LALIGA.equals(league) ? "La Liga" : "Superliga";
    }
}
