package com.munger.budgettrack.view;

import android.app.FragmentTransaction;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.model.TransactionCategory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public class Entry extends Fragment
{
    public EditText dateTxt;
    public EditText descTxt;
    public EditText amountTxt;
    public CheckBox catastropheBox;
    public Spinner categorySpn;

    private Transaction data;

    public Entry()
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
        View ret = inflater.inflate(R.layout.fragment_entry, container, false);
        dateTxt = (EditText) ret.findViewById(R.id.g_entry_dateInput);
        descTxt = (EditText) ret.findViewById(R.id.g_entry_noteInput);
        amountTxt = (EditText) ret.findViewById(R.id.g_entry_amountInput);
        amountTxt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        catastropheBox = (CheckBox) ret.findViewById(R.id.g_entry_catastropheInput);
        categorySpn = (Spinner) ret.findViewById(R.id.g_entry_categorySpinner);

        ArrayList<TransactionCategory> transCats = Main.instance.dbHelper.loadTransactionCategories();
        ArrayAdapter<TransactionCategory> arrad = new ArrayAdapter<TransactionCategory>(Main.instance, android.R.layout.simple_list_item_1, transCats);
        categorySpn.setAdapter(arrad);

        Bundle b = getArguments();
        if (b != null && b.containsKey("data"))
        {
            data = b.getParcelable("data");

            dateTxt.setText(Transaction.keyToDateString(data.date));
            descTxt.setText(data.desc);
            amountTxt.setText(String.valueOf(data.amount));
            catastropheBox.setChecked(data.catastrophe);

            TransactionCategory cat = Transaction.getCategory(data.categoryId);
            int idx = transCats.indexOf(cat);
            categorySpn.setSelection(idx);
        }
        else
        {
            String dateStr = Transaction.getDateString(System.currentTimeMillis());
            dateTxt.setText(dateStr);
        }

        return ret;
    }



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
                        data = new Transaction();

                    constructTransaction(data);

                    Main.instance.transactionService.commitTransaction(data);
                    Main.instance.goBack();
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private boolean validate()
    {
        String date = dateTxt.getText().toString();
        if (date.trim().length() == 0)
        {
            dateTxt.setError("date is required");
            return false;
        }

        try
        {
            getDate();
        }
        catch(Exception e){
            dateTxt.setError("invalid date");
            return false;
        }

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

    private void constructTransaction(Transaction ret)
    {
        ret.date = getDate();
        ret.desc = descTxt.getText().toString();
        ret.amount = getAmount();
        ret.catastrophe = catastropheBox.isChecked();

        TransactionCategory cat = (TransactionCategory) categorySpn.getSelectedItem();
        ret.categoryId = cat.id;
    }

    private String getDate()
    {
        String date = dateTxt.getText().toString().trim();

        String[] parts = date.split("[/\\-\\.]");
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[0]) - 1, Integer.parseInt(parts[1]), 12, 0);
        return Transaction.dateToKey(cal);
    }

    private float getAmount()
    {
        return Float.parseFloat(amountTxt.getText().toString());
    }
}
