package com.munger.budgettrack.view;

import com.munger.budgettrack.model.CashFlow;

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
        ret.date = System.currentTimeMillis();
        ret.desc = descTxt.getText().toString();
        ret.amount = getAmount();
        ret.amount = -ret.amount;
    }
}
