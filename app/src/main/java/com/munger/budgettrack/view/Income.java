package com.munger.budgettrack.view;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.service.CashFlowService;

import java.util.ArrayList;
import java.util.Calendar;

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
        message += "$" + tr.amount;
        return message;
    }

    protected void addSelected()
    {
        Main.instance.loadIncomeEntry();
    }

    protected ArrayList<CashFlow> getData(Calendar cal, int days)
    {
        ArrayList<CashFlow> list = Main.instance.cashFlowService.getList(CashFlowService.Type.INCOME);
        return list;
    }

    protected String getAmountText(float amount)
    {
        return "$" + String.valueOf(amount);
    }

    protected String getTotalText(Calendar cal, int days)
    {
        float total = Main.instance.cashFlowService.getTotal(CashFlowService.Type.INCOME);
        return "$" + total;
    }
}
