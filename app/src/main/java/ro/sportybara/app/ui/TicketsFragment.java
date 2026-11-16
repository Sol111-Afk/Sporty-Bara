package ro.sportybara.app.ui;

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
import ro.sportybara.app.data.FakeRepository;
import ro.sportybara.app.data.Selection;
import ro.sportybara.app.data.Ticket;

/**
 * Ecran 3 — Bilete.
 *  - Bilet curent: selecțiile adăugate din ecranul Meci, miză, plasare (ADĂUGARE).
 *  - Bilete active: modificare miză / scoatere selecții (MODIFICARE) și ștergere cu returnarea mizei (ȘTERGERE).
 */
public class TicketsFragment extends Fragment implements FakeRepository.Listener {

    private final FakeRepository repo = FakeRepository.get();

    private TextView balance, slipEmpty, totalOdd, potentialWin, ticketsNone;
    private View slipContent;
    private LinearLayout slipSelections, ticketsList;
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
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                stakeLayout.setError(null);
                updateSlipTotals();
            }
        });
        v.findViewById(R.id.slip_clear).setOnClickListener(x -> repo.clearSlip());
        v.findViewById(R.id.slip_place).setOnClickListener(x -> placeTicket());
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
        renderSlip();
        renderTickets();
    }

    // ---- bilet curent ----

    private void renderSlip() {
        List<Selection> slip = repo.getSlip();
        slipEmpty.setVisibility(slip.isEmpty() ? View.VISIBLE : View.GONE);
        slipContent.setVisibility(slip.isEmpty() ? View.GONE : View.VISIBLE);

        slipSelections.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Selection s : slip) {
            View row = inflater.inflate(R.layout.item_slip_selection, slipSelections, false);
            ((TextView) row.findViewById(R.id.line)).setText(selectionLine(s));
            ((TextView) row.findViewById(R.id.bookmaker)).setText(getString(R.string.selection_bookmaker, s.bookmaker));
            View remove = row.findViewById(R.id.remove);
            remove.setContentDescription(getString(R.string.remove_selection, s.matchTitle));
            remove.setOnClickListener(x -> repo.removeFromSlip(s.matchId));
            slipSelections.addView(row);
        }
        updateSlipTotals();
    }

    private void updateSlipTotals() {
        double odd = Ticket.totalOdd(repo.getSlip());
        totalOdd.setText(getString(R.string.slip_total_odd, Format.odd(odd)));
        Integer s = parseStake(stake);
        potentialWin.setText(getString(R.string.slip_potential_win, s == null ? 0 : Ticket.potentialWin(s, odd)));
    }

    private void placeTicket() {
        Integer s = parseStake(stake);
        try {
            repo.placeTicket(s == null ? 0 : s);
            Snackbar.make(requireView(), R.string.slip_placed, Snackbar.LENGTH_SHORT).show();
        } catch (IllegalArgumentException | IllegalStateException e) {
            stakeLayout.setError(e.getMessage());
        }
    }

    // ---- bilete plasate ----

    private void renderTickets() {
        List<Ticket> tickets = repo.getTickets();
        ticketsNone.setVisibility(tickets.isEmpty() ? View.VISIBLE : View.GONE);
        ticketsList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Ticket t : tickets) {
            View card = inflater.inflate(R.layout.item_ticket, ticketsList, false);
            ((TextView) card.findViewById(R.id.header))
                    .setText(getResources().getQuantityString(R.plurals.ticket_header,
                            t.getSelections().size(), t.id, t.getSelections().size()));
            StringBuilder lines = new StringBuilder();
            for (Selection s : t.getSelections()) {
                if (lines.length() > 0) lines.append('\n');
                lines.append(selectionLine(s));
            }
            ((TextView) card.findViewById(R.id.selections)).setText(lines);
            ((TextView) card.findViewById(R.id.summary)).setText(getString(R.string.ticket_stake_line,
                    t.getStake(), Format.odd(t.getTotalOdd()), t.getPotentialWin()));
            card.findViewById(R.id.edit).setOnClickListener(x -> showEditDialog(t));
            card.findViewById(R.id.delete).setOnClickListener(x -> confirmDelete(t));
            ticketsList.addView(card);
        }
    }

    private void confirmDelete(Ticket t) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.ticket_delete_title, t.id))
                .setMessage(getString(R.string.ticket_delete_message, t.getStake()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    repo.deleteTicket(t.id);
                    Snackbar.make(requireView(), R.string.ticket_deleted, Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    /** Dialog de modificare: miza nouă + selecțiile (debifate = scoase de pe bilet). */
    private void showEditDialog(Ticket t) {
        View content = getLayoutInflater().inflate(R.layout.dialog_edit_ticket, null);
        TextInputLayout layout = content.findViewById(R.id.stake_layout);
        TextInputEditText input = content.findViewById(R.id.stake);
        input.setText(String.valueOf(t.getStake()));

        LinearLayout box = content.findViewById(R.id.selections);
        List<CheckBox> checks = new ArrayList<>();
        for (Selection s : t.getSelections()) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(selectionLine(s));
            cb.setChecked(true);
            cb.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
            cb.setTag(s.matchId);
            box.addView(cb);
            checks.add(cb);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.ticket_edit_title, t.id))
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null) // validăm înainte să închidem
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            Integer newStake = parseStake(input);
            try {
                // Întâi miza (poate eșua la validare), apoi selecțiile scoase.
                repo.updateStake(t.id, newStake == null ? 0 : newStake);
                for (CheckBox cb : checks) {
                    if (!cb.isChecked()) repo.removeSelection(t.id, (String) cb.getTag());
                }
                dialog.dismiss();
                Snackbar.make(requireView(),
                        repo.getTicket(t.id) == null ? R.string.ticket_deleted : R.string.ticket_updated,
                        Snackbar.LENGTH_SHORT).show();
            } catch (IllegalArgumentException e) {
                layout.setError(e.getMessage());
            }
        });
    }

    // ---- utilitare ----

    private String selectionLine(Selection s) {
        return getString(R.string.selection_line, s.matchTitle, s.pick.label, Format.odd(s.odd));
    }

    @Nullable
    private static Integer parseStake(TextInputEditText field) {
        Editable e = field.getText();
        if (e == null || e.length() == 0) return null;
        try {
            return Integer.parseInt(e.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
