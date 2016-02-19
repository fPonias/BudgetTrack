package com.munger.budgettrack.view;

import android.app.Fragment;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.TransactionCategory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by codymunger on 1/1/16.
 */
public abstract class CashFlowEntryBase extends Fragment
{
    public EditText descTxt;
    public EditText amountTxt;
    public EditText startTxt;
    public EditText endTxt;
    public Spinner categorySpn;

    protected ArrayList<TransactionCategory> transCats;

    private CashFlow data;

    public CashFlowEntryBase()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInf)
    {
        menuInf.inflate(R.menu.entry_menu, menu);

        super.onCreateOptionsMenu(menu, menuInf);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View ret = inflater.inflate(R.layout.fragment_incomeentry, container, false);
        descTxt = (EditText) ret.findViewById(R.id.g_incomeentry_descInput);

        startTxt = (EditText) ret.findViewById(R.id.g_incomeentry_startDateInput);
        endTxt = (EditText) ret.findViewById(R.id.g_incomeentry_endDateInput);
        amountTxt = (EditText) ret.findViewById(R.id.g_incomeentry_amountInput);
        amountTxt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);

        categorySpn = (Spinner) ret.findViewById(R.id.g_incomeentry_categorySpinner);
        transCats = Main.instance.dbHelper.loadTransactionCategories();
        ArrayAdapter<TransactionCategory> arrad = new ArrayAdapter<TransactionCategory>(Main.instance, android.R.layout.simple_list_item_1, transCats);
        categorySpn.setAdapter(arrad);


        Bundle b = getArguments();
        if (b != null && b.containsKey("data"))
        {
            data = b.getParcelable("data");

            descTxt.setText(data.desc);
            amountTxt.setText(getAmountString(data));

            int i = 0;
            int sz = transCats.size();
            for (i = 0; i < sz; i++)
            {
                TransactionCategory cat = transCats.get(i);
                if (cat.id == data.categoryId)
                {
                    categorySpn.setSelection(i);
                    break;
                }
            }
        }
        else
        {
        }

        return ret;
    }

    protected abstract String getAmountString(CashFlow data);
    protected abstract void constructCashFlow(CashFlow data);

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_cancel:
                Main.instance.goBack();

                return true;

            case R.id.action_save:
                boolean okay = validate();

                if (okay)
                {
                    if (data == null)
                        data = new CashFlow();

                    constructCashFlow(data);

                    Main.instance.cashFlowService.commitCashFlow(data);
                    Main.instance.goBack();
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private boolean validate()
    {
        String amount = amountTxt.getText().toString();
        if (amount.trim().length() == 0)
        {
            amountTxt.setError("amount is required");
            return false;
        }

        try
        {
            getAmount();
        }
        catch(Exception e){
            amountTxt.setError("invalid amount");
            return false;
        }

        try
        {
            getDate(startTxt, 0);
        }
        catch(Exception e){
            startTxt.setError("invalid date");
            return false;
        }

        try
        {
            getDate(endTxt, Long.MAX_VALUE);
        }
        catch(Exception e){
            endTxt.setError("invalid date");
            return false;
        }


        return true;
    }

    protected float getAmount()
    {
        return Float.parseFloat(amountTxt.getText().toString());
    }

    protected long getDate(EditText dateInp, long defaultValue)
    {
        String date = dateInp.getText().toString().trim();

        if (date.isEmpty())
            return defaultValue;

        String[] parts = date.split("[/\\-\\.]");
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[0]) - 1, Integer.parseInt(parts[1]), 12, 0);
        return cal.getTimeInMillis();
    }
}

