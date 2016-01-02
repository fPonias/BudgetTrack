package com.munger.budgettrack.view;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;

import java.util.ArrayList;

/**
 * Created by codymunger on 1/1/16.
 */
public class Income extends CashFlowBase
{
    protected void itemClicked(IncomeViewStruct adapter)
    {
        if (!editable)
            return;

        Main.instance.loadIncomeEntry(adapter.data);
    }

    protected String getTitle()
    {
        return "Income";
    }

    protected String getDeleteTitle()
    {
        return "Delete income?";
    }

    protected String getDeleteMessage(CashFlow tr)
    {
        String message = "Are you sure you want to delete ";
        message += "$" + tr.amount + " income from " + Transaction.getDateString(tr.date);
        return message;
    }

    protected void addSelected()
    {
        Main.instance.loadIncomeEntry();
    }

    protected ArrayList<CashFlow> getData()
    {
        return Main.instance.cashFlowService.income;
    }

    protected String getAmountText(float amount)
    {
        return "$" + String.valueOf(amount);
    }

    protected String getTotalText()
    {
        return "$" + Main.instance.cashFlowService.incomeTotal;
    }
}
