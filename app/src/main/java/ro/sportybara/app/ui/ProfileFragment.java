package ro.sportybara.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ro.sportybara.app.R;
import ro.sportybara.app.data.FakeRepository;

/** Ecran 4 — Profil: nume (modificabil), sold, statistici simple, resetarea datelor demo. */
public class ProfileFragment extends Fragment implements FakeRepository.Listener {

    private final FakeRepository repo = FakeRepository.get();
    private TextView name, balance;
    private LinearLayout stats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        name = v.findViewById(R.id.name);
        balance = v.findViewById(R.id.balance);
        stats = v.findViewById(R.id.stats);
        v.findViewById(R.id.edit_name).setOnClickListener(x -> showEditName());
        v.findViewById(R.id.reset).setOnClickListener(x -> confirmReset());
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
        name.setText(repo.getDisplayName());
        balance.setText(getString(R.string.coins_format, repo.getBalance()));

        // primul copil e titlul „Statistici”
        stats.removeViews(1, stats.getChildCount() - 1);
        addStat(getString(R.string.profile_tickets), String.valueOf(repo.getTickets().size()));
        addStat(getString(R.string.profile_staked), getString(R.string.coins_format, repo.getTotalStaked()));
        addStat(getString(R.string.profile_slip), String.valueOf(repo.getSlip().size()));
    }

    private void addStat(String label, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Math.round(36 * getResources().getDisplayMetrics().density));

        TextView l = new AppCompatTextView(requireContext());
        l.setTextAppearance(R.style.TextAppearance_SportyBara_Body);
        l.setText(label);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView val = new AppCompatTextView(requireContext());
        val.setTextAppearance(R.style.TextAppearance_SportyBara_Number);
        val.setText(value);
        row.addView(val);
        stats.addView(row);
    }

    private void showEditName() {
        View content = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);
        TextInputLayout layout = content.findViewById(R.id.name_layout);
        TextInputEditText input = content.findViewById(R.id.name);
        input.setText(repo.getDisplayName());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_edit_name)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            Editable e = input.getText();
            try {
                repo.setDisplayName(e == null ? "" : e.toString());
                dialog.dismiss();
            } catch (IllegalArgumentException ex) {
                layout.setError(ex.getMessage());
            }
        });
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_reset)
                .setMessage(getString(R.string.profile_reset_confirm, FakeRepository.START_BALANCE))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.profile_reset, (d, w) -> {
                    repo.reset();
                    Snackbar.make(requireView(), R.string.profile_reset_done, Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }
}
