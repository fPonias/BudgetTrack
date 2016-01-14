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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.model.TransactionCategory;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by codymunger on 1/1/16.
 */
public class IncomeEntry extends CashFlowEntryBase
{
    protected String getAmountString(CashFlow data)
    {
        return String.valueOf(data.amount);
    }

    protected void constructCashFlow(CashFlow ret)
    {
        ret.startDate = System.currentTimeMillis();
        ret.desc = descTxt.getText().toString();
        ret.amount = getAmount();
    }
}
