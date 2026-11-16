package ro.sportybara.app.ui.profile;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ro.sportybara.app.R;
import ro.sportybara.app.data.SportyStore;
import ro.sportybara.app.data.StoreException;

public class ProfileFragment extends Fragment implements SportyStore.Listener {

    private final SportyStore store = SportyStore.get();
    private TextView name;
    private TextView balance;
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
        store.addListener(this);
        onStoreChanged();
    }

    @Override
    public void onStop() {
        store.removeListener(this);
        super.onStop();
    }

    @Override
    public void onStoreChanged() {
        name.setText(store.getDisplayName());
        balance.setText(getString(R.string.coins_format, store.getBalance()));

        stats.removeViews(1, stats.getChildCount() - 1);
        addStat(getString(R.string.profile_tickets), String.valueOf(store.getTickets().size()));
        addStat(getString(R.string.profile_staked), getString(R.string.coins_format, store.getTotalStaked()));
        addStat(getString(R.string.profile_slip), String.valueOf(store.getSlip().size()));
    }

    private void addStat(String label, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Math.round(36 * getResources().getDisplayMetrics().density));

        TextView labelView = new AppCompatTextView(requireContext());
        labelView.setTextAppearance(R.style.TextAppearance_SportyBara_Body);
        labelView.setText(label);
        row.addView(labelView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView valueView = new AppCompatTextView(requireContext());
        valueView.setTextAppearance(R.style.TextAppearance_SportyBara_Number);
        valueView.setText(value);
        row.addView(valueView);
        stats.addView(row);
    }

    private void showEditName() {
        View content = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);
        TextInputLayout layout = content.findViewById(R.id.name_layout);
        TextInputEditText input = content.findViewById(R.id.name);
        input.setText(store.getDisplayName());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_edit_name)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            Editable text = input.getText();
            try {
                store.setDisplayName(text == null ? "" : text.toString());
                dialog.dismiss();
            } catch (StoreException e) {
                layout.setError(getString(R.string.error_invalid_name, SportyStore.MAX_NAME_LENGTH));
            }
        });
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_reset)
                .setMessage(getString(R.string.profile_reset_confirm, SportyStore.START_BALANCE))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.profile_reset, (d, w) -> {
                    store.reset();
                    Snackbar.make(requireView(), R.string.profile_reset_done, Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }
}
