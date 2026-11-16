package ro.sportybara.app.ui.tickets;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import ro.sportybara.app.R;
import ro.sportybara.app.data.Selection;
import ro.sportybara.app.data.SportyStore;
import ro.sportybara.app.data.StoreException;
import ro.sportybara.app.data.Ticket;
import ro.sportybara.app.ui.Odds;

public class TicketsFragment extends Fragment implements SportyStore.Listener {

    private final SportyStore store = SportyStore.get();

    private TextView balance;
    private TextView slipEmpty;
    private TextView totalOdd;
    private TextView potentialWin;
    private TextView ticketsNone;
    private View slipContent;
    private LinearLayout slipSelections;
    private LinearLayout ticketsList;
    private TextInputLayout stakeLayout;
    private TextInputEditText stake;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tickets, container, false);
        balance = v.findViewById(R.id.balance);
        slipEmpty = v.findViewById(R.id.slip_empty);
        slipContent = v.findViewById(R.id.slip_content);
        slipSelections = v.findViewById(R.id.slip_selections);
        stakeLayout = v.findViewById(R.id.stake_layout);
        stake = v.findViewById(R.id.stake);
        totalOdd = v.findViewById(R.id.slip_total_odd);
        potentialWin = v.findViewById(R.id.slip_potential_win);
        ticketsNone = v.findViewById(R.id.tickets_none);
        ticketsList = v.findViewById(R.id.tickets_list);

        stake.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                stakeLayout.setError(null);
                updateSlipTotals();
            }
        });
        v.findViewById(R.id.slip_clear).setOnClickListener(x -> store.clearSlip());
        v.findViewById(R.id.slip_place).setOnClickListener(x -> placeTicket());
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
        balance.setText(getString(R.string.coins_format, store.getBalance()));
        renderSlip();
        renderTickets();
    }

    private void renderSlip() {
        List<Selection> slip = store.getSlip();
        slipEmpty.setVisibility(slip.isEmpty() ? View.VISIBLE : View.GONE);
        slipContent.setVisibility(slip.isEmpty() ? View.GONE : View.VISIBLE);

        slipSelections.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Selection selection : slip) {
            View row = inflater.inflate(R.layout.item_slip_selection, slipSelections, false);
            ((TextView) row.findViewById(R.id.line)).setText(selectionLine(selection));
            ((TextView) row.findViewById(R.id.bookmaker)).setText(getString(R.string.selection_bookmaker, selection.getBookmaker()));
            View remove = row.findViewById(R.id.remove);
            remove.setContentDescription(getString(R.string.remove_selection, selection.getMatch().title()));
            remove.setOnClickListener(x -> store.removeFromSlip(selection.getMatch()));
            slipSelections.addView(row);
        }
        updateSlipTotals();
    }

    private void updateSlipTotals() {
        double odd = store.slipTotalOdd();
        totalOdd.setText(getString(R.string.slip_total_odd, Odds.format(odd)));
        Integer value = parseStake(stake);
        potentialWin.setText(getString(R.string.slip_potential_win, value == null ? 0 : Ticket.potentialWin(value, odd)));
    }

    private void placeTicket() {
        Integer value = parseStake(stake);
        try {
            store.placeTicket(value == null ? 0 : value);
            Snackbar.make(requireView(), R.string.slip_placed, Snackbar.LENGTH_SHORT).show();
        } catch (StoreException e) {
            stakeLayout.setError(errorMessage(e));
        }
    }

    private void renderTickets() {
        List<Ticket> tickets = store.getTickets();
        ticketsNone.setVisibility(tickets.isEmpty() ? View.VISIBLE : View.GONE);
        ticketsList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Ticket ticket : tickets) {
            View card = inflater.inflate(R.layout.item_ticket, ticketsList, false);
            int count = ticket.getSelections().size();
            ((TextView) card.findViewById(R.id.header))
                    .setText(getResources().getQuantityString(R.plurals.ticket_header, count, ticket.getId(), count));
            StringBuilder lines = new StringBuilder();
            for (Selection selection : ticket.getSelections()) {
                if (lines.length() > 0) {
                    lines.append('\n');
                }
                lines.append(selectionLine(selection));
            }
            ((TextView) card.findViewById(R.id.selections)).setText(lines);
            ((TextView) card.findViewById(R.id.summary)).setText(getString(R.string.ticket_stake_line,
                    ticket.getStake(), Odds.format(ticket.getTotalOdd()), ticket.getPotentialWin()));
            card.findViewById(R.id.edit).setOnClickListener(x -> showEditDialog(ticket));
            card.findViewById(R.id.delete).setOnClickListener(x -> confirmDelete(ticket));
            ticketsList.addView(card);
        }
    }

    private void confirmDelete(Ticket ticket) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.ticket_delete_title, ticket.getId()))
                .setMessage(getString(R.string.ticket_delete_message, ticket.getStake()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    store.deleteTicket(ticket.getId());
                    Snackbar.make(requireView(), R.string.ticket_deleted, Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showEditDialog(Ticket ticket) {
        View content = getLayoutInflater().inflate(R.layout.dialog_edit_ticket, null);
        TextInputLayout layout = content.findViewById(R.id.stake_layout);
        TextInputEditText input = content.findViewById(R.id.stake);
        input.setText(String.valueOf(ticket.getStake()));

        LinearLayout box = content.findViewById(R.id.selections);
        List<CheckBox> checks = new ArrayList<>();
        for (Selection selection : ticket.getSelections()) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(selectionLine(selection));
            checkBox.setChecked(true);
            checkBox.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
            checkBox.setTag(selection.getMatch().getId());
            box.addView(checkBox);
            checks.add(checkBox);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.ticket_edit_title, ticket.getId()))
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            Integer newStake = parseStake(input);
            try {
                store.updateStake(ticket.getId(), newStake == null ? 0 : newStake);
                for (CheckBox checkBox : checks) {
                    if (!checkBox.isChecked()) {
                        store.removeSelection(ticket.getId(), (String) checkBox.getTag());
                    }
                }
                dialog.dismiss();
                Snackbar.make(requireView(),
                        store.hasTicket(ticket.getId()) ? R.string.ticket_updated : R.string.ticket_deleted,
                        Snackbar.LENGTH_SHORT).show();
            } catch (StoreException e) {
                layout.setError(errorMessage(e));
            }
        });
    }

    private String selectionLine(Selection selection) {
        return getString(R.string.selection_line, selection.getMatch().title(),
                selection.getOutcome().label(), Odds.format(selection.getOdd()));
    }

    private String errorMessage(StoreException e) {
        switch (e.getReason()) {
            case EMPTY_SLIP:
                return getString(R.string.error_empty_slip);
            case INVALID_STAKE:
                return getString(R.string.error_invalid_stake, SportyStore.MIN_STAKE, SportyStore.MAX_STAKE);
            case INSUFFICIENT_FUNDS:
                return getString(R.string.error_insufficient_funds, store.getBalance());
            default:
                return getString(R.string.error_generic);
        }
    }

    @Nullable
    private static Integer parseStake(TextInputEditText field) {
        Editable text = field.getText();
        if (text == null || text.length() == 0) {
            return null;
        }
        try {
            return Integer.parseInt(text.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
