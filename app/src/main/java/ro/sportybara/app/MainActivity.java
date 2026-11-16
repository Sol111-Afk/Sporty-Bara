package ro.sportybara.app;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import ro.sportybara.app.ui.HomeFragment;
import ro.sportybara.app.ui.MatchFragment;
import ro.sportybara.app.ui.PlaceholderFragment;
import ro.sportybara.app.ui.ProfileFragment;
import ro.sportybara.app.ui.TicketsFragment;

/**
 * O singură activitate; ecranele sunt fragmente.
 * Navigare: bara de jos (5 tab-uri) + ecranul Meci deschis peste Acasă (cu back stack, butonul Înapoi revine).
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Bara de jos își pune singură padding pentru bara de navigare a sistemului;
        // aici tratăm doar marginea de sus și lateralele.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        // Apăsarea pe tab-ul deja activ închide ecranul Meci și revine la listă.
        nav.setOnItemReselectedListener(item -> popToRoot());
        if (savedInstanceState == null) {
            // Acasă e deja tab-ul selectat implicit, deci setSelectedItemId ar ajunge la „reselect”.
            showTab(R.id.tab_home);
        }
    }

    /** Deschide ecranul Meci peste tab-ul curent. */
    public void openMatch(String matchId) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, MatchFragment.newInstance(matchId))
                .addToBackStack("match")
                .commit();
    }

    /** Schimbă tab-ul (ex. „Vezi biletul” din ecranul Meci). */
    public void openTab(@IdRes int tabId) {
        nav.setSelectedItemId(tabId);
    }

    private void showTab(@IdRes int tabId) {
        popToRoot();
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragmentFor(tabId))
                .commit();
    }

    private void popToRoot() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    private static Fragment fragmentFor(int tabId) {
        if (tabId == R.id.tab_tickets) {
            return new TicketsFragment();
        } else if (tabId == R.id.tab_wheel) {
            return PlaceholderFragment.newInstance(R.string.tab_wheel, R.string.empty_wheel);
        } else if (tabId == R.id.tab_leaderboard) {
            return PlaceholderFragment.newInstance(R.string.tab_leaderboard, R.string.empty_leaderboard);
        } else if (tabId == R.id.tab_profile) {
            return new ProfileFragment();
        }
        return new HomeFragment();
    }
}
