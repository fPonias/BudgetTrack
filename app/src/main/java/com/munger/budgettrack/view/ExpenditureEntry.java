package com.munger.budgettrack.view;

import android.text.Editable;
import android.widget.EditText;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by codymunger on 1/1/16.
 */
public class ExpenditureEntry extends CashFlowEntryBase
{
    protected String getAmountString(CashFlow data)
    {
        return String.valueOf(-data.amount);
    }

    protected void constructCashFlow(CashFlow ret)
    {
        ret.desc = descTxt.getText().toString();
        ret.amount = getAmount();
        ret.amount = -ret.amount;
        int catIndex = categorySpn.getSelectedItemPosition();
        ret.categoryId = transCats.get(catIndex).id;
    }
}
