package ro.sportybara.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import ro.sportybara.app.MainActivity;
import ro.sportybara.app.R;
import ro.sportybara.app.data.FakeRepository;
import ro.sportybara.app.data.Match;
import ro.sportybara.app.data.Pick;

/** Ecran 1 — Acasă: lista de meciuri, filtrabilă pe ligă. Apăsarea pe un meci deschide ecranul Meci. */
public class HomeFragment extends Fragment implements FakeRepository.Listener {

    private final FakeRepository repo = FakeRepository.get();
    private final MatchAdapter adapter = new MatchAdapter();
    private TextView balance;
    private String league; // null = toate

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        balance = v.findViewById(R.id.balance);
        ((RecyclerView) v.findViewById(R.id.matches)).setAdapter(adapter);

        ChipGroup filter = v.findViewById(R.id.league_filter);
        filter.setOnCheckedStateChangeListener((group, ids) -> {
            int id = ids.isEmpty() ? R.id.chip_all : ids.get(0);
            league = id == R.id.chip_laliga ? Match.LALIGA
                    : id == R.id.chip_superliga ? Match.SUPERLIGA : null;
            onDataChanged();
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        repo.addListener(this);
        onDataChanged();
    }

    @Override
    public void onStop() {
        repo.removeListener(this);
        super.onStop();
    }

    @Override
    public void onDataChanged() {
        balance.setText(getString(R.string.coins_format, repo.getBalance()));
        adapter.submit(repo.getMatches(league));
    }

    private class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.Holder> {
        private final List<Match> items = new ArrayList<>();

        @SuppressWarnings("NotifyDataSetChanged") // listă mică, simulată
        void submit(List<Match> matches) {
            items.clear();
            items.addAll(matches);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_match, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Match m = items.get(position);
            h.meta.setText(getString(R.string.match_meta, Format.league(m.league), m.kickoff));
            h.teams.setText(m.title());
            h.home.setText(getString(R.string.match_odd_cell, Pick.HOME.label, Format.odd(m.best(Pick.HOME).home)));
            h.draw.setText(getString(R.string.match_odd_cell, Pick.DRAW.label, Format.odd(m.best(Pick.DRAW).draw)));
            h.away.setText(getString(R.string.match_odd_cell, Pick.AWAY.label, Format.odd(m.best(Pick.AWAY).away)));

            Pick inSlip = repo.pickInSlip(m.id);
            h.inSlip.setVisibility(inSlip == null ? View.GONE : View.VISIBLE);
            if (inSlip != null) h.inSlip.setText(getString(R.string.match_in_slip, inSlip.label));

            h.itemView.setOnClickListener(x -> ((MainActivity) requireActivity()).openMatch(m.id));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView meta, teams, home, draw, away, inSlip;

            Holder(View v) {
                super(v);
                meta = v.findViewById(R.id.meta);
                teams = v.findViewById(R.id.teams);
                home = v.findViewById(R.id.odd_home);
                draw = v.findViewById(R.id.odd_draw);
                away = v.findViewById(R.id.odd_away);
                inSlip = v.findViewById(R.id.in_slip);
            }
        }
    }
}
