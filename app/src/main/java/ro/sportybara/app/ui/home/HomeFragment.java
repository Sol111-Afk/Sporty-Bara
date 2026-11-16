package ro.sportybara.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import ro.sportybara.app.R;
import ro.sportybara.app.data.FakeData;
import ro.sportybara.app.data.SportyStore;
import ro.sportybara.app.ui.match.MatchActivity;

public class HomeFragment extends Fragment implements SportyStore.Listener {

    private final SportyStore store = SportyStore.get();
    private MatchAdapter adapter;
    private TextView slipSummary;
    private String league;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        slipSummary = v.findViewById(R.id.slip_summary);

        adapter = new MatchAdapter(match -> startActivity(MatchActivity.intent(requireContext(), match.getId())));
        RecyclerView list = v.findViewById(R.id.match_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        ChipGroup filters = v.findViewById(R.id.league_filter);
        filters.setOnCheckedStateChangeListener((group, ids) -> {
            int id = ids.isEmpty() ? R.id.chip_all : ids.get(0);
            league = id == R.id.chip_laliga ? FakeData.LALIGA : id == R.id.chip_superliga ? FakeData.SUPERLIGA : null;
            refresh();
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        store.addListener(this);
        refresh();
    }

    @Override
    public void onStop() {
        store.removeListener(this);
        super.onStop();
    }

    @Override
    public void onStoreChanged() {
        refresh();
    }

    private void refresh() {
        adapter.submit(store.getMatches(league), store);
        int count = store.getSlip().size();
        slipSummary.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        slipSummary.setText(getResources().getQuantityString(R.plurals.slip_summary, count, count));
    }
}
