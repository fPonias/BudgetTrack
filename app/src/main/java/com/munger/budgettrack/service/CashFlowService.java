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
        index = new HashMap<>();

        for(Parcelable p : pararr)
        {
            CashFlow c = (CashFlow)p;
            cashFlows.add(c);
            index.put(c.id, c);
        }
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
        Cursor cur = Main.instance.dbHelper.db.query(CashFlow.TABLE_NAME, new String[]{"amount", "desc", "categoryId", "id"},
                "", new String[]{},
                "id ASC"
        );

        int sz = cur.getCount();
        cashFlows = new ArrayList<>();
        boolean success = cur.moveToFirst();
        while (success)
        {
            CashFlow t = new CashFlow();
            t.amount = cur.getFloat(0);
            t.desc = cur.getString(1);
            t.categoryId = cur.getLong(2);
            t.id = cur.getLong(3);

            cashFlows.add(t);
            success = cur.moveToNext();
            index.put(t.id, t);
        }

        cur.close();

        for(CashFlowChangedListener listener : listeners)
        {
            listener.changed();
        }
    }

    public static enum Type
    {
        EXPENDITURE,
        INCOME
    };

    public float getTotal(Type type)
    {
        ArrayList<CashFlow> list = getList(type);
        float ret = 0.0f;
        for (CashFlow flow : list)
            ret += flow.amount;

        if (type == Type.INCOME)
            return ret;
        else
            return -ret;
    }

    public ArrayList<CashFlow> getList(Type type)
    {
        String where = "";
        if (type == Type.EXPENDITURE)
            where += "amount < 0";
        else
            where += "amount > 0";

        Cursor cur = Main.instance.dbHelper.db.query(CashFlow.TABLE_NAME, new String[]{"id"},
                where, new String[]{},
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
        index.remove(tr.id);

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

        index.put(tr.id, tr);

        for(CashFlowChangedListener listener : listeners)
        {
            listener.changed();
        }
    }
}
