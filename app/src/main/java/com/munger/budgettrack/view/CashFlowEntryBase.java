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
import android.widget.EditText;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.CashFlow;

/**
 * Created by codymunger on 1/1/16.
 */
public abstract class CashFlowEntryBase extends Fragment
{
    public EditText descTxt;
    public EditText amountTxt;

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
        amountTxt = (EditText) ret.findViewById(R.id.g_incomeentry_amountInput);
        amountTxt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);

        Bundle b = getArguments();
        if (b != null && b.containsKey("data"))
        {
            data = b.getParcelable("data");

            descTxt.setText(data.desc);
            amountTxt.setText(getAmountString(data));
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


        return true;
    }

    protected float getAmount()
    {
        return Float.parseFloat(amountTxt.getText().toString());
    }
}

