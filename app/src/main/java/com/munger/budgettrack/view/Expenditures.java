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
public class Expenditures extends CashFlowBase
{
    protected void itemClicked(IncomeViewStruct adapter)
    {
        if (!editable)
            return;

        Main.instance.loadExpenditureEntry(adapter.data);
    }

    protected String getTitle()
    {
        return "Expenditures";
    }

    protected String getDeleteTitle()
    {
        return "Delete expenditure?";
    }

    protected String getDeleteMessage(CashFlow tr)
    {
        String message = "Are you sure you want to delete ";
        message += "$" + tr.amount + " expenditure from " + Transaction.getDateString(tr.startDate);
        return message;
    }

    protected void addSelected()
    {
        Main.instance.loadExpenditureEntry();
    }

    protected ArrayList<CashFlow> getData(Calendar cal, int days)
    {
        return Main.instance.cashFlowService.getList(CashFlowService.Type.EXPENDITURE, cal, days);
    }

    protected String getAmountText(float amount)
    {
        return "$" + String.valueOf(-amount);
    }

    protected String getTotalText(Calendar cal, int days)
    {
        float total = Main.instance.cashFlowService.getTotal(CashFlowService.Type.EXPENDITURE, cal, days);
        return "$" + total;
    }
}
