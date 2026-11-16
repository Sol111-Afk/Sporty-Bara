package ro.sportybara.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ro.sportybara.app.R;
import ro.sportybara.app.data.Match;
import ro.sportybara.app.data.Outcome;
import ro.sportybara.app.data.Selection;
import ro.sportybara.app.data.SportyStore;
import ro.sportybara.app.ui.Odds;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.Holder> {

    public interface OnMatchClick {
        void onClick(Match match);
    }

    private final OnMatchClick onClick;
    private final List<Match> matches = new ArrayList<>();
    private SportyStore store;

    public MatchAdapter(OnMatchClick onClick) {
        this.onClick = onClick;
    }

    public void submit(List<Match> newMatches, SportyStore store) {
        this.store = store;
        matches.clear();
        matches.addAll(newMatches);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_match, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Match match = matches.get(position);
        h.meta.setText(h.itemView.getContext().getString(R.string.match_meta, match.getLeague(), match.getKickoff()));
        h.title.setText(match.title());
        h.home.setText(h.itemView.getContext().getString(R.string.odd_with_label, Outcome.HOME.label(), Odds.format(match.bestOdd(Outcome.HOME))));
        h.draw.setText(h.itemView.getContext().getString(R.string.odd_with_label, Outcome.DRAW.label(), Odds.format(match.bestOdd(Outcome.DRAW))));
        h.away.setText(h.itemView.getContext().getString(R.string.odd_with_label, Outcome.AWAY.label(), Odds.format(match.bestOdd(Outcome.AWAY))));
        Selection selection = store == null ? null : store.slipSelectionFor(match);
        h.onSlip.setVisibility(selection == null ? View.GONE : View.VISIBLE);
        if (selection != null) {
            h.onSlip.setText(h.itemView.getContext().getString(R.string.on_slip, selection.getOutcome().label()));
        }
        h.itemView.setOnClickListener(v -> onClick.onClick(match));
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView meta;
        final TextView title;
        final TextView home;
        final TextView draw;
        final TextView away;
        final TextView onSlip;

        Holder(View v) {
            super(v);
            meta = v.findViewById(R.id.match_meta);
            title = v.findViewById(R.id.match_title);
            home = v.findViewById(R.id.odd_home);
            draw = v.findViewById(R.id.odd_draw);
            away = v.findViewById(R.id.odd_away);
            onSlip = v.findViewById(R.id.on_slip);
        }
    }
}
