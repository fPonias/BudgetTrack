package com.munger.budgettrack.service;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.common.BTree;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by codymunger on 12/31/15.
 */
public class CashFlowService
{
    private static class MinCompare implements Comparable<MinCompare>
    {
        public CashFlow target;

        public MinCompare(CashFlow target)
        {
            this.target = target;
        }

        public int compareTo(MinCompare c)
        {
            return (int) (target.startDate - c.target.startDate);
        }
    }

    private static class MaxCompare implements Comparable<MaxCompare>
    {
        public CashFlow target;

        public MaxCompare(CashFlow target)
        {
            this.target = target;
        }

        public int compareTo(MaxCompare c)
        {
            return (int) (target.endDate - c.target.endDate);
        }
    }

    public HashMap<String, CashFlow> income;
    public HashMap<String, CashFlow> expenditures;
    public ArrayList<CashFlow> cashFlows;
    private BTree<MinCompare> incomeMinimums;
    private BTree<MaxCompare> incomeMaximums;
    private BTree<MinCompare> expMinimums;
    private BTree<MaxCompare> expMaximums;

    public CashFlowService()
    {
        income = new HashMap<>();
        expenditures = new HashMap<>();
        cashFlows = new ArrayList<>();
        incomeMinimums = new BTree<>(null);
        incomeMaximums = new BTree<>(null);
        expMinimums = new BTree<>(null);
        expMaximums = new BTree<>(null);
    }

    public CashFlowService(Bundle state)
    {
        Parcelable[] pararr = state.getParcelableArray("cashFlows");
        cashFlows = new ArrayList<>();
        incomeMinimums = new BTree<>(null);
        incomeMaximums = new BTree<>(null);
        expMinimums = new BTree<>(null);
        expMaximums = new BTree<>(null);

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
        Cursor cur = Main.instance.dbHelper.db.query(CashFlow.TABLE_NAME, new String[]{"startDate", "endDate", "amount", "desc", "categoryId", "id"},
                "", new String[]{},
                "startDate DESC"
        );

        int sz = cur.getCount();
        cashFlows = new ArrayList<>();
        boolean success = cur.moveToFirst();
        while (success)
        {
            CashFlow t = new CashFlow();
            t.id = cur.getLong(5);
            String startKey = cur.getString(0);
            t.startDate = Transaction.keyToDate(startKey).getTimeInMillis();
            String endKey = cur.getString(1);
            t.endDate = Transaction.keyToDate(endKey).getTimeInMillis();
            t.amount = cur.getFloat(2);
            t.desc = cur.getString(3);
            t.categoryId = cur.getLong(4);

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
        income = new HashMap<>();
        expenditures = new HashMap<>();

        int sz = cashFlows.size();
        for (int i = 0; i < sz; i++)
        {
            CashFlow item = cashFlows.get(i);
            String key = Transaction.dateToKey(item.startDate);
            MinCompare minCmp = new MinCompare(item);
            MaxCompare maxCmp = new MaxCompare(item);

            if (item.amount >= 0)
            {
                income.put(key, item);
                incomeMinimums.add(minCmp);
                incomeMaximums.add(maxCmp);
            }
            else
            {
                expenditures.put(key, item);
                expMinimums.add(minCmp);
                expMaximums.add(maxCmp);
            }
        }
    }

    public float getTotal(boolean isExpenditure, Calendar start, int days)
    {
        return 0.0f;
    }

    public ArrayList<CashFlow> getList(boolean isExpenditure, Calendar start, int days)
    {
        ArrayList<CashFlow> ret = new ArrayList<>();
        BTree<MinCompare> flowTreeMin;
        BTree<MaxCompare> flowTreeMax;
        CashFlow cmp = new CashFlow();
        cmp.startDate = start.getTimeInMillis();
        start.add(Calendar.DAY_OF_MONTH, days);
        cmp.endDate = start.getTimeInMillis();
        MinCompare cmpMin = new MinCompare(cmp);
        MaxCompare cmpMax = new MaxCompare(cmp);

        if (isExpenditure)
        {
            flowTreeMin = expMinimums;
            flowTreeMax = expMaximums;
        }
        else
        {
            flowTreeMin = incomeMinimums;
            flowTreeMax = incomeMaximums;
        }

        ArrayList<MinCompare> minList = flowTreeMin.getOrderedList(cmpMin, false);
        ArrayList<MaxCompare> maxList = flowTreeMax.getOrderedList(cmpMax, true);

        HashSet<Long> indices = new HashSet<>();
        for(MinCompare i : minList)
        {
            indices.add(i.target.id);
        }

        for(MaxCompare i : maxList)
        {
            if (indices.contains(i.target.id))
                ret.add(i.target);
        }

        return ret;
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
