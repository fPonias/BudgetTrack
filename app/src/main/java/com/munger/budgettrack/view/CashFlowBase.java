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
import android.widget.ListView;
import android.widget.TextView;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.service.CashFlowService;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by codymunger on 12/31/15.
 */
public abstract class CashFlowBase extends Fragment
{
    public static class IncomeViewStruct
    {
        public View rowView;

        public ImageButton deleteBtn;
        public TextView amountLbl;
        public TextView descLbl;

        public CashFlow data;

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

    public static class IncomeAdapter extends ArrayAdapter<CashFlow>
    {
        public CashFlowBase parent;
        public ArrayList<CashFlow> data;

        public IncomeAdapter(CashFlowBase parent, ArrayList<CashFlow> data)
        {
            super(Main.instance, R.layout.income_item, data);
            this.data = data;
            this.parent = parent;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater = (LayoutInflater) Main.instance.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final IncomeViewStruct str = new IncomeViewStruct();
            str.rowView = inflater.inflate(R.layout.income_item, parent, false);

            str.deleteBtn = (ImageButton) str.rowView.findViewById(R.id.g_income_listitem_deleteBtn);
            str.amountLbl = (TextView) str.rowView.findViewById(R.id.g_income_listitem_amountTxt);
            str.descLbl = (TextView) str.rowView.findViewById(R.id.g_income_listitem_descTxt);

            CashFlow tr = data.get(position);
            str.data = tr;

            str.amountLbl.setText(this.parent.getAmountText(tr.amount));

            str.descLbl.setText(tr.desc);

            str.setEditable(this.parent.editable);


            final IncomeAdapter that = this;
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
    public TextView title;
    public TextView total;
    public ArrayList<CashFlow> data;
    public IncomeAdapter adapter;

    private HashMap<Long, IncomeViewStruct> viewIndex;
    private CashFlowService.CashFlowChangedListener changeListener;

    public CashFlowBase()
    {
        viewIndex = new HashMap<>();

        changeListener = new CashFlowService.CashFlowChangedListener(){public void changed()
        {
            update();
        }};

        Main.instance.cashFlowService.addListener(changeListener);
    }

    @Override
    public void onDestroy()
    {
        Main.instance.cashFlowService.removeListener(changeListener);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View ret = inflater.inflate(R.layout.fragment_income, container, false);

        list = (ListView) ret.findViewById(R.id.g_income_list);
        title = (TextView) ret.findViewById(R.id.g_income_title);
        total = (TextView) ret.findViewById(R.id.g_income_total);

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

        MenuItem item = (MenuItem) this.menu.findItem(R.id.action_add);
        item.setVisible(editable);

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
                addSelected();

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

        for (CashFlow tr : data)
        {
            IncomeViewStruct view = viewIndex.get(tr.id);
            view.setEditable(editable);
        }

        list.invalidate();
    }

    public void deleteClicked(IncomeViewStruct adapter)
    {
        final CashFlow tr = adapter.data;
        AlertDialog.Builder builder = new AlertDialog.Builder(Main.instance);

        builder.setMessage(getDeleteMessage(tr));
        builder.setTitle(getDeleteTitle());

        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Main.instance.cashFlowService.deleteCashFlow(tr);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected abstract String getTitle();
    protected abstract String getDeleteTitle();
    protected abstract String getDeleteMessage(CashFlow tr);
    protected abstract void itemClicked(IncomeViewStruct adapter);
    protected abstract void addSelected();
    protected abstract ArrayList<CashFlow> getData();
    protected abstract String getAmountText(float amount);
    protected abstract String getTotalText();

    public void update()
    {
        title.setText(getTitle());
        data = getData();
        if (data != null && list != null)
        {
            adapter = new IncomeAdapter(this, data);
            list.setAdapter(adapter);
        }

        total.setText(getTotalText());
    }

    public void indexView(CashFlow tr, IncomeViewStruct v)
    {
        viewIndex.put(tr.id, v);
    }
}
