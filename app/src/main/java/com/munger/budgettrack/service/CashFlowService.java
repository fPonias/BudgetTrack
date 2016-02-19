package com.munger.budgettrack.service;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.common.BTree;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

/**
 * Created by codymunger on 12/31/15.
 */
public class CashFlowService
{
    public HashMap<String, CashFlow> income;
    public HashMap<String, CashFlow> expenditures;
    public HashMap<Long, CashFlow> index;
    public ArrayList<CashFlow> cashFlows;

    public CashFlowService()
    {
        income = new HashMap<>();
        expenditures = new HashMap<>();
        cashFlows = new ArrayList<>();
        index = new HashMap<>();
    }

    public CashFlowService(Bundle state)
    {
        Parcelable[] pararr = state.getParcelableArray("cashFlows");
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

    public void loadAll()
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
            String key = cur.getString(0);
            t.startDate = Transaction.keyToDate(key).getTimeInMillis();
            key = cur.getString(1);
            t.endDate = Transaction.keyToDate(key).getTimeInMillis();
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
        index = new HashMap<>();

        int sz = cashFlows.size();
        for (int i = 0; i < sz; i++)
        {
            CashFlow item = cashFlows.get(i);
            String key = Transaction.dateToKey(item.startDate);

            if (item.amount >= 0)
            {
                income.put(key, item);
            }
            else
            {
                expenditures.put(key, item);
            }

            index.put(item.id, item);
        }
    }

    public static enum Type
    {
        EXPENDITURE,
        INCOME
    };

    public float getTotal(Type type, Calendar start, int days)
    {
        ArrayList<CashFlow> list = getList(type, start, days);
        float ret = 0.0f;
        for (CashFlow flow : list)
            ret += flow.amount;

        if (type == Type.INCOME)
            return ret;
        else
            return -ret;
    }

    public ArrayList<CashFlow> getList(Type type, Calendar c, int days)
    {
        Calendar start = Calendar.getInstance();
        start.setTimeZone(TimeZone.getDefault());
        start.setTimeInMillis(c.getTimeInMillis());
        start.set(Calendar.DAY_OF_MONTH, 1);
        String startkey = Transaction.dateToKey(start);
        start.add(Calendar.DAY_OF_MONTH, days);
        String endkey = Transaction.dateToKey(start);

        String where = "startDate < ? and endDate >= ? ";
        if (type == Type.EXPENDITURE)
            where += "and amount < 0";
        else
            where += "and amount > 0";

        Cursor cur = Main.instance.dbHelper.db.query(CashFlow.TABLE_NAME, new String[]{"id"},
                where, new String[]{endkey, startkey},
                "id ASC"
        );

        ArrayList<CashFlow> ret = new ArrayList<>();
        int sz = cur.getCount();
        boolean success = cur.moveToFirst();
        while (success)
        {
            Long id = cur.getLong(0);
            CashFlow t = index.get(id);
            ret.add(t);

            success = cur.moveToNext();
        }

        cur.close();

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
