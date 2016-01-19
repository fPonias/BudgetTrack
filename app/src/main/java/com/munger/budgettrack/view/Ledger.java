package com.munger.budgettrack.view;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.service.TransactionService;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by codymunger on 12/25/15.
 */
public class Ledger extends Fragment
{
    public static class LedgerViewStruct
    {
        public View rowView;

        public ImageButton deleteBtn;
        public TextView dateLbl;
        public TextView amountLbl;
        public ImageView cataImg;

        public Transaction data;

        public boolean editable = false;
        public void setEditable(boolean editable)
        {
            this.editable = editable;
            if (editable)
                deleteBtn.setVisibility(View.VISIBLE);
            else
                deleteBtn.setVisibility(View.GONE);
        }
    }

    public static class LedgerAdapter extends ArrayAdapter<Transaction>
    {
        public Ledger parent;
        public ArrayList<Transaction> data;

        public LedgerAdapter(Ledger parent, ArrayList<Transaction> data)
        {
            super(Main.instance, R.layout.ledger_item, data);
            this.data = data;
            this.parent = parent;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater = (LayoutInflater) Main.instance.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final LedgerViewStruct str = new LedgerViewStruct();
            str.rowView = inflater.inflate(R.layout.ledger_item, parent, false);

            str.deleteBtn = (ImageButton) str.rowView.findViewById(R.id.g_ledger_listitem_deleteBtn);
            str.dateLbl = (TextView) str.rowView.findViewById(R.id.g_ledger_listitem_dateTxt);
            str.amountLbl = (TextView) str.rowView.findViewById(R.id.g_ledger_listitem_amountTxt);
            str.cataImg = (ImageView) str.rowView.findViewById(R.id.g_ledger_listitem_catastrophic);

            Transaction tr = data.get(position);
            str.data = tr;

            String dt = Transaction.getDateString(tr.date);
            str.dateLbl.setText(dt);

            str.amountLbl.setText("$" + String.valueOf(tr.amount));

            if (tr.catastrophe)
                str.cataImg.setVisibility(View.VISIBLE);
            else
                str.cataImg.setVisibility(View.INVISIBLE);

            str.setEditable(this.parent.editable);


            final LedgerAdapter that = this;
            str.deleteBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
            {
                that.parent.deleteClicked(str);
            }});

            str.rowView.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
            {
                that.parent.itemClicked(str);
            }});


            this.parent.indexView(tr, str);

            return str.rowView;
        }
    }

    public ListView list;
    public ArrayList<Transaction> data;
    public LedgerAdapter adapter;

    private HashMap<Long, LedgerViewStruct> viewIndex;
    private TransactionService.TransactionsChangedListener changeListener;

    public Ledger()
    {
        viewIndex = new HashMap<>();

        changeListener = new TransactionService.TransactionsChangedListener() {public void changed()
        {
            update();
        }};

        Main.instance.transactionService.addChangeListener(changeListener);
    }

    @Override
    public void onDestroy()
    {
        Main.instance.transactionService.removeChangeListener(changeListener);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View ret = inflater.inflate(R.layout.fragment_ledger, container, false);

        list = (ListView) ret.findViewById(R.id.g_ledger_list);
        update();


        return ret;
    }

    private Menu menu;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInf)
    {
        menuInf.inflate(R.menu.ledger_menu, menu);
        this.menu = menu;

        super.onCreateOptionsMenu(menu, menuInf);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_edit:
                toggleEdit();
                return true;

            case R.id.action_add:
                Main.instance.loadEntry();

                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    public boolean editable = false;

    public void toggleEdit()
    {
        editable = !editable;

        MenuItem item = (MenuItem) menu.findItem(R.id.action_add);
        item.setVisible(editable);

        if (data == null)
            return;

        for (Transaction tr : data)
        {
            LedgerViewStruct view = viewIndex.get(tr.id);

            if (view != null)
                view.setEditable(editable);
        }

        list.invalidate();
    }

    public void deleteClicked(LedgerViewStruct adapter)
    {
        final Transaction tr = adapter.data;
        AlertDialog.Builder builder = new AlertDialog.Builder(Main.instance);

        String message = "Are you sure you want to delete ";
        if (tr.catastrophe)
            message += "catastrophic ";
        message += "$" + tr.amount + " transaction from " + Transaction.getDateString(tr.date);
        builder.setMessage(message);

        builder.setTitle("Delete transaction?");

        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Main.instance.transactionService.deleteTransaction(tr);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void itemClicked(LedgerViewStruct adapter)
    {
        if (!editable)
            return;

        Main.instance.loadEntry(adapter.data);
    }

    public void update()
    {
        data = Main.instance.transactionService.transactions;
        if (data != null && list != null)
        {
            adapter = new LedgerAdapter(this, data);
            list.setAdapter(adapter);
        }
    }

    public void indexView(Transaction tr, LedgerViewStruct v)
    {
        viewIndex.put(tr.id, v);
    }
}
