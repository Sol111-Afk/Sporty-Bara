package ro.sportybara.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;

import ro.sportybara.app.MainActivity;
import ro.sportybara.app.R;
import ro.sportybara.app.data.BookmakerOdds;
import ro.sportybara.app.data.FakeRepository;
import ro.sportybara.app.data.Match;
import ro.sportybara.app.data.Pick;

/** Ecran 2 — Meci: cotele tuturor caselor una lângă alta, cea mai bună evidențiată; adăugare pe bilet. */
public class MatchFragment extends Fragment {

    private static final String ARG_MATCH_ID = "matchId";

    public static MatchFragment newInstance(String matchId) {
        MatchFragment f = new MatchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MATCH_ID, matchId);
        f.setArguments(args);
        return f;
    }

    private final FakeRepository repo = FakeRepository.get();
    private Match match;
    private MaterialButtonToggleGroup pickGroup;
    /** Ca să nu reacționăm la schimbările făcute din cod (sincronizarea cu slip-ul). */
    private boolean syncing;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_match, container, false);
        match = repo.getMatch(requireArguments().getString(ARG_MATCH_ID));

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setTitle(Format.league(match.league));
        toolbar.setNavigationOnClickListener(x -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        ((TextView) v.findViewById(R.id.teams)).setText(match.title());
        ((TextView) v.findViewById(R.id.kickoff)).setText(match.kickoff);
        buildOddsTable(v.findViewById(R.id.odds_table));

        pickGroup = v.findViewById(R.id.pick_group);
        bindPick(v.findViewById(R.id.pick_home), Pick.HOME);
        bindPick(v.findViewById(R.id.pick_draw), Pick.DRAW);
        bindPick(v.findViewById(R.id.pick_away), Pick.AWAY);
        syncPickWithSlip();

        pickGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (syncing) return;
            Pick pick = pickFor(checkedId);
            if (isChecked) {
                repo.addToSlip(match.id, pick);
                showSnack(getString(R.string.match_added, pick.label + " @ " + Format.odd(match.best(pick).get(pick))));
            } else if (group.getCheckedButtonId() == View.NO_ID) {
                repo.removeFromSlip(match.id);
                showSnack(getString(R.string.match_removed));
            }
        });

        v.findViewById(R.id.go_to_ticket).setOnClickListener(x ->
                ((MainActivity) requireActivity()).openTab(R.id.tab_tickets));
        return v;
    }

    private void bindPick(Button b, Pick pick) {
        b.setText(getString(R.string.match_pick_button, pick.label, Format.odd(match.best(pick).get(pick))));
        b.setTag(pick);
    }

    private Pick pickFor(int buttonId) {
        if (buttonId == R.id.pick_home) return Pick.HOME;
        if (buttonId == R.id.pick_draw) return Pick.DRAW;
        return Pick.AWAY;
    }

    private void syncPickWithSlip() {
        syncing = true;
        Pick inSlip = repo.pickInSlip(match.id);
        if (inSlip == null) pickGroup.clearChecked();
        else pickGroup.check(inSlip == Pick.HOME ? R.id.pick_home : inSlip == Pick.DRAW ? R.id.pick_draw : R.id.pick_away);
        syncing = false;
    }

    private void showSnack(String text) {
        Snackbar.make(requireView(), text, Snackbar.LENGTH_SHORT)
                .setAction(R.string.view_ticket, x -> ((MainActivity) requireActivity()).openTab(R.id.tab_tickets))
                .show();
    }

    /** Tabel: Casa | 1 | X | 2, câte un rând pe casă; cea mai bună cotă pe fiecare coloană e evidențiată. */
    private void buildOddsTable(LinearLayout table) {
        Context c = requireContext();
        table.addView(row(c, getString(R.string.match_bookmaker),
                new String[]{Pick.HOME.label, Pick.DRAW.label, Pick.AWAY.label}, new boolean[3], true));
        for (BookmakerOdds o : match.odds) {
            boolean[] best = new boolean[3];
            String[] values = new String[3];
            Pick[] picks = Pick.values();
            for (int i = 0; i < 3; i++) {
                values[i] = Format.odd(o.get(picks[i]));
                best[i] = match.best(picks[i]) == o;
            }
            table.addView(row(c, o.bookmaker, values, best, false));
        }
    }

    private View row(Context c, String label, String[] values, boolean[] best, boolean header) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(header ? 32 : 44));

        TextView name = new AppCompatTextView(c);
        name.setTextAppearance(header ? R.style.TextAppearance_SportyBara_Small : R.style.TextAppearance_SportyBara_Body);
        name.setText(label);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f));

        for (int i = 0; i < 3; i++) {
            TextView cell = new AppCompatTextView(c);
            cell.setGravity(Gravity.CENTER);
            cell.setTextAppearance(header ? R.style.TextAppearance_SportyBara_Small : R.style.TextAppearance_SportyBara_Number);
            cell.setText(values[i]);
            cell.setPadding(0, dp(4), 0, dp(4));
            if (best[i]) {
                cell.setBackgroundResource(R.drawable.bg_best_odd);
                cell.setTextColor(ContextCompat.getColor(c, R.color.sb_on_accent));
                cell.setContentDescription(values[i] + ", cea mai bună cotă");
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginStart(dp(4));
            row.addView(cell, lp);
        }
        return row;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
