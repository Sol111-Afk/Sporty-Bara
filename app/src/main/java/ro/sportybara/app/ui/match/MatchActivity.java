package ro.sportybara.app.ui.match;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Map;

import ro.sportybara.app.MainActivity;
import ro.sportybara.app.R;
import ro.sportybara.app.data.Match;
import ro.sportybara.app.data.Outcome;
import ro.sportybara.app.data.SportyStore;
import ro.sportybara.app.data.Verdict;
import ro.sportybara.app.ui.Odds;

public class MatchActivity extends AppCompatActivity {

    private static final String EXTRA_MATCH_ID = "match_id";

    private final SportyStore store = SportyStore.get();
    private Match match;

    public static Intent intent(Context context, String matchId) {
        return new Intent(context, MatchActivity.class).putExtra(EXTRA_MATCH_ID, matchId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_match);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.match_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        match = store.findMatch(getIntent().getStringExtra(EXTRA_MATCH_ID));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(match.getLeague());

        ((TextView) findViewById(R.id.match_title)).setText(match.title());
        ((TextView) findViewById(R.id.match_kickoff)).setText(match.getKickoff());

        buildOddsTable(findViewById(R.id.odds_table));
        bindVerdicts();
        bindButton(R.id.pick_home, Outcome.HOME);
        bindButton(R.id.pick_draw, Outcome.DRAW);
        bindButton(R.id.pick_away, Outcome.AWAY);
    }

    private void buildOddsTable(TableLayout table) {
        table.addView(row(getString(R.string.bookmaker), Outcome.HOME.label(), Outcome.DRAW.label(), Outcome.AWAY.label(), true));
        for (Map.Entry<String, double[]> entry : match.getOdds().entrySet()) {
            TableRow row = new TableRow(this);
            row.addView(cell(entry.getKey(), false, Gravity.START));
            for (Outcome outcome : Outcome.values()) {
                boolean best = entry.getKey().equals(match.bestBookmaker(outcome));
                TextView cell = cell(Odds.format(entry.getValue()[outcome.ordinal()]), best, Gravity.END);
                if (best) {
                    cell.setTextColor(ContextCompat.getColor(this, R.color.sb_win));
                    cell.setContentDescription(getString(R.string.best_odd_description, cell.getText()));
                }
                row.addView(cell);
            }
            table.addView(row);
        }
        table.addView(row(getString(R.string.sporty_bara_odd),
                Odds.format(match.fairOdd(Outcome.HOME)),
                Odds.format(match.fairOdd(Outcome.DRAW)),
                Odds.format(match.fairOdd(Outcome.AWAY)), true));
    }

    private TableRow row(String first, String home, String draw, String away, boolean bold) {
        TableRow row = new TableRow(this);
        row.addView(cell(first, bold, Gravity.START));
        row.addView(cell(home, bold, Gravity.END));
        row.addView(cell(draw, bold, Gravity.END));
        row.addView(cell(away, bold, Gravity.END));
        return row;
    }

    private TextView cell(String text, boolean bold, int gravity) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextAppearance(R.style.TextAppearance_SportyBara_Number);
        view.setTypeface(view.getTypeface(), bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setGravity(gravity);
        int padding = getResources().getDimensionPixelSize(R.dimen.table_cell_padding);
        view.setPadding(padding, padding, padding, padding);
        return view;
    }

    private void bindVerdicts() {
        StringBuilder text = new StringBuilder();
        for (Outcome outcome : Outcome.values()) {
            Verdict verdict = match.verdict(outcome);
            int res = verdict == Verdict.LOW ? R.string.verdict_low : verdict == Verdict.HIGH ? R.string.verdict_high : R.string.verdict_fair;
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(getString(res, outcome.label(), Odds.format(match.bestOdd(outcome)), Odds.format(match.fairOdd(outcome))));
        }
        ((TextView) findViewById(R.id.verdicts)).setText(text);
    }

    private void bindButton(int id, Outcome outcome) {
        MaterialButton button = findViewById(id);
        button.setText(getString(R.string.pick_button, outcome.label(), Odds.format(match.bestOdd(outcome))));
        button.setOnClickListener(v -> {
            store.addToSlip(match, outcome);
            Snackbar.make(v, getString(R.string.added_to_slip, outcome.label(), match.title()), Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(this, R.color.sb_accent))
                    .setAction(R.string.view_slip, a -> {
                        startActivity(MainActivity.openTab(this, R.id.tab_tickets));
                        finish();
                    })
                    .show();
        });
    }
}
