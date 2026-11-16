package ro.sportybara.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import ro.sportybara.app.ui.PlaceholderFragment;
import ro.sportybara.app.ui.home.HomeFragment;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_TAB = "tab";

    private BottomNavigationView nav;

    public static Intent openTab(Context context, int tabId) {
        return new Intent(context, MainActivity.class)
                .putExtra(EXTRA_TAB, tabId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            show(fragmentFor(item.getItemId()));
            return true;
        });
        if (savedInstanceState == null) {
            nav.setSelectedItemId(getIntent().getIntExtra(EXTRA_TAB, R.id.tab_home));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(EXTRA_TAB)) {
            nav.setSelectedItemId(intent.getIntExtra(EXTRA_TAB, R.id.tab_home));
        }
    }

    private void show(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .commit();
    }

    private static Fragment fragmentFor(int tabId) {
        if (tabId == R.id.tab_tickets) {
            return PlaceholderFragment.newInstance(R.string.tab_tickets, R.string.empty_tickets);
        } else if (tabId == R.id.tab_wheel) {
            return PlaceholderFragment.newInstance(R.string.tab_wheel, R.string.empty_wheel);
        } else if (tabId == R.id.tab_leaderboard) {
            return PlaceholderFragment.newInstance(R.string.tab_leaderboard, R.string.empty_leaderboard);
        } else if (tabId == R.id.tab_profile) {
            return PlaceholderFragment.newInstance(R.string.tab_profile, R.string.empty_profile);
        }
        return new HomeFragment();
    }
}
