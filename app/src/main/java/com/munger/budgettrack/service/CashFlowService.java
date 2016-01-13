package com.munger.budgettrack.service;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by codymunger on 12/31/15.
 */
public class CashFlowService
{
    public ArrayList<CashFlow> income;
    public ArrayList<CashFlow> expenditures;
    public HashMap<Long, CashFlow> indexedData;

    public float incomeTotal;
    public float expenditureTotal;

    public ArrayList<CashFlow> cashFlows;

    public CashFlowService()
    {
        income = new ArrayList<>();
        expenditures = new ArrayList<>();
        indexedData = new HashMap<>();
        cashFlows = new ArrayList<>();
    }

    public CashFlowService(Bundle state)
    {
        Parcelable[] pararr = state.getParcelableArray("transactions");
        cashFlows = new ArrayList<>();
        for(Parcelable p : pararr)
            cashFlows.add((CashFlow) p);

        sortData();
    }

    public Bundle getState()
    {
        Bundle b = new Bundle();

        int sz = cashFlows.size();
        CashFlow[] tranArr = new CashFlow[sz];
        for (int i = 0; i < sz; i++)
            tranArr[i] = cashFlows.get(i);

        b.putParcelableArray("cashFlows", tranArr);

        return b;
    }

    public void loadData()
    {
        Cursor cur = Main.instance.dbHelper.db.query(CashFlow.TABLE_NAME, new String[]{"date", "amount", "desc", "id"},
                "", new String[]{},
                "date DESC"
        );

        int sz = cur.getCount();
        cashFlows = new ArrayList<>();
        boolean success = cur.moveToFirst();
        while (success)
        {
            CashFlow t = new CashFlow();
            t.id = cur.getLong(3);
            t.date = cur.getString(0);
            t.amount = cur.getFloat(1);
            t.desc = cur.getString(2);

            cashFlows.add(t);
            success = cur.moveToNext();
        }

        cur.close();
        sortData();

        for(CashFlowChangedListener listener : listeners)
        {
            listener.changed();
        }
    }

    private void sortData()
    {
        income = new ArrayList<>();
        expenditures = new ArrayList<>();
        indexedData = new HashMap<>();
        incomeTotal = 0;
        expenditureTotal = 0;

        int sz = cashFlows.size();
        for (int i = 0; i < sz; i++)
        {
            CashFlow item = cashFlows.get(i);

            if (item.amount >= 0)
            {
                incomeTotal += item.amount;
                income.add(item);
            }
            else
            {
                expenditures.add(item);
                expenditureTotal += item.amount;
            }

            indexedData.put(item.id, item);
        }

        expenditureTotal = -expenditureTotal;
    }

    public static interface CashFlowChangedListener
    {
        public void changed();
    }

    private ArrayList<CashFlowChangedListener> listeners = new ArrayList<>();

    public void addListener(CashFlowChangedListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(CashFlowChangedListener listener)
    {
        listeners.remove(listener);
    }

    public void deleteCashFlow(CashFlow tr)
    {
        tr.delete();
        cashFlows.remove(tr);
        sortData();

        for(CashFlowChangedListener listener : listeners)
        {
            listener.changed();
        }
    }

    public void commitCashFlow(CashFlow tr)
    {
        long oldid = tr.id;
        tr.commit();

        if (oldid == -1)
            cashFlows.add(tr);

        sortData();

        for(CashFlowChangedListener listener : listeners)
        {
            listener.changed();
        }
    }
}
