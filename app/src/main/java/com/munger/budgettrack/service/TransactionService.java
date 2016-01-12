package com.munger.budgettrack.service;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.model.TransactionCategory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * Created by codymunger on 12/28/15.
 */
public class TransactionService
{
    public HashMap<String, ArrayList<Transaction>> sortedTransactions;
    public HashMap<String, ArrayList<Transaction>> sortedCatastrophe;
    public HashMap<String, ArrayList<Transaction>> sortedCategory;
    public HashMap<String, Float> totaledTransactions;
    public HashMap<String, Float> totaledCatastrophe;
    public HashMap<String, Float> totaledCategory;
    public HashMap<Long, Transaction> indexedTransactions;

    public ArrayList<Transaction> transactions;
    public int transYear = -1;
    public int transMonth = -1;
    public long transStart = -1;
    public long transEnd = -1;

    public TransactionService()
    {
        sortedTransactions = new HashMap<>();
        totaledTransactions = new HashMap<>();
        sortedCatastrophe = new HashMap<>();
        totaledCatastrophe = new HashMap<>();
        sortedCategory = new HashMap<>();
        totaledCategory = new HashMap<>();
        indexedTransactions = new HashMap<>();

        transactions = new ArrayList<>();
    }

    public TransactionService(Bundle state)
    {
        Parcelable[] pararr = state.getParcelableArray("transactions");
        transactions = new ArrayList<>();
        for(Parcelable p : pararr)
            transactions.add((Transaction) p);

        transYear = state.getInt("transYear");
        transMonth = state.getInt("transMonth");
        transStart = state.getLong("transStart");
        transEnd = state.getLong("transEnd");

        sortTransactions(transactions);
    }

    public Bundle getState()
    {
        Bundle b = new Bundle();

        int sz = transactions.size();
        Transaction[] tranArr = new Transaction[sz];
        for (int i = 0; i < sz; i++)
            tranArr[i] = transactions.get(i);

        b.putParcelableArray("transactions", tranArr);
        b.putInt("transYear", transYear);
        b.putInt("transMonth", transMonth);
        b.putLong("transStart", transStart);
        b.putLong("transEnd", transEnd);

        return b;
    }

    public void loadCurrentTransactions()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.setTimeZone(TimeZone.getDefault());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        loadTransactions(year, month);
    }

    public void loadTransactions(int year, int month)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(year, month, 0);
        cal.add(Calendar.MONTH, -1);
        long start = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 2);
        long end = cal.getTimeInMillis();


        Cursor cur = Main.instance.dbHelper.db.query(Transaction.TABLE_NAME, new String[]{"date", "amount", "desc", "catastrophe", "categoryId", "id"},
                "date>? AND date<=?", new String[]{String.valueOf(start), String.valueOf(end)},
                null, null, "date ASC"
        );

        int sz = cur.getCount();
        ArrayList<Transaction> ret = new ArrayList<>();
        boolean success = cur.moveToFirst();
        while (success)
        {
            Transaction t = new Transaction();
            t.id = cur.getLong(5);
            t.date = cur.getLong(0);
            t.amount = cur.getFloat(1);
            t.desc = cur.getString(2);
            t.catastrophe = (cur.getInt(3) == 1) ? true : false;
            t.categoryId = cur.getLong(4);

            ret.add(t);
            success = cur.moveToNext();
        }

        cur.close();

        transactions = ret;
        transYear = year;
        transMonth = month;
        transStart = start;
        transEnd = end;

        sortTransactions(transactions);
    }

    public void deleteTransaction(Transaction tr)
    {
        transactions.remove(tr);
        tr.delete();

        for(TransactionsChangedListener listener : changeListeners)
        {
            listener.changed();
        }
    }

    public void commitTransaction(Transaction tr)
    {
        if (tr.id == -1 && tr.date >= transStart && tr.date <= transEnd)
        {
            int sz = transactions.size();
            for (int i = 0; i < sz; i++)
            {
                Transaction trans = transactions.get(i);
                if (trans.date <= tr.date)
                {
                    transactions.add(i, tr);
                    break;
                }
            }
        }

        tr.commit();

        for(TransactionsChangedListener listener : changeListeners)
        {
            listener.changed();
        }
    }

    public static interface TransactionsChangedListener
    {
        public void changed();
    }

    private ArrayList<TransactionsChangedListener> changeListeners = new ArrayList<>();

    public void addChangeListener(TransactionsChangedListener listener)
    {
        changeListeners.add(listener);
    }

    public void removeChangeListener(TransactionsChangedListener listener)
    {
        changeListeners.remove(listener);
    }

    private void sortTransactions(ArrayList<Transaction> list)
    {
        sortedTransactions = new HashMap<>();
        totaledTransactions = new HashMap<>();
        sortedCatastrophe = new HashMap<>();
        totaledCatastrophe = new HashMap<>();
        sortedCategory = new HashMap<>();
        totaledCategory = new HashMap<>();
        indexedTransactions = new HashMap<>();

        for (Transaction t : list)
        {
            String key = millisToKey(t.date);

            HashMap<String, ArrayList<Transaction>> sortedList = null;
            HashMap<String, Float> totaledList = null;

            if (!t.catastrophe)
            {
                sortedList = sortedTransactions;
                totaledList = totaledTransactions;
            }
            else
            {
                sortedList = sortedCatastrophe;
                totaledList = totaledCatastrophe;
            }


            if (!sortedList.containsKey(key))
                sortedList.put(key, new ArrayList<Transaction>());

            sortedList.get(key).add(t);

            if (!totaledList.containsKey(key))
                totaledList.put(key, 0.0f);

            float newTotal = totaledList.get(key) + t.amount;
            totaledList.put(key, newTotal);


            String category = Transaction.getCategory(t.categoryId).category;
            if (!sortedCategory.containsKey(category))
                sortedCategory.put(category, new ArrayList<Transaction>());

            sortedCategory.get(category).add(t);

            if (!totaledCategory.containsKey(category))
                totaledCategory.put(category, 0.0f);

            newTotal = totaledCategory.get(category) + t.amount;
            totaledCategory.put(category, newTotal);


            indexedTransactions.put(t.id, t);
        }
    }

    public String millisToKey(long date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);
        cal.setTimeZone(TimeZone.getDefault());
        return calendarToKey(cal);
    }

    public String calendarToKey(Calendar cal)
    {
        String mon = String.valueOf(cal.get(Calendar.MONTH));
        if (mon.length() == 1)
            mon = "0" + mon;

        String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        if (day.length() == 1)
            day = "0" + day;

        String year = String.valueOf(cal.get(Calendar.YEAR));

        String ret = year + "-" + mon + "-" + day;
        return ret;
    }


    public float getMonthlyTotal(int year, int month)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(year, month, 0);
        float total = 0;

        int max = cal.getMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < max; i++)
        {
            long date = cal.getTimeInMillis();
            String key = millisToKey(date);

            if (totaledTransactions.containsKey(key))
                total += totaledTransactions.get(key);

            //if (totaledCatastrophe.containsKey(key))
            //    total += totaledCatastrophe.get(key);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return total;
    }

    public float getCatastropheTotal(int year, int month)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(year, month, 0);
        float total = 0;

        int max = cal.getMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < max; i++)
        {
            long date = cal.getTimeInMillis();
            String key = millisToKey(date);

            if (totaledCatastrophe.containsKey(key))
                total += totaledCatastrophe.get(key);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return total;
    }

    public float getWeeklyTotal(int year, int month, int day)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(year, month, day);
        int dow = getdow(cal);
        cal.add(Calendar.DAY_OF_MONTH, -(dow - 1));

        float ret = 0.0f;
        for (int i = 0; i < 7; i++)
        {
            long date = cal.getTimeInMillis();
            String key = millisToKey(date);
            if (totaledTransactions.containsKey(key))
                ret += totaledTransactions.get(key);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return ret;
    }

    public float getMonthlyBudget()
    {
        float monthlyBudget = Main.instance.cashFlowService.incomeTotal - Main.instance.cashFlowService.expenditureTotal;
        monthlyBudget -= Main.instance.settings.emergencyFund;

        return monthlyBudget;
    }

    public float getWeeklyBudget(int year, int month, int day)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(year, month, 0);

        cal.add(Calendar.MONTH, -1);
        float dailyBudgetLastMonth = getDailyBudget(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
        cal.add(Calendar.MONTH, 1);
        float dailyBudgetThisMonth = getDailyBudget(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
        cal.add(Calendar.MONTH, 1);
        float dailyBudgetNextMonth = getDailyBudget(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));

        cal.set(year, month, day);
        int dow = getdow(cal);
        cal.add(Calendar.DAY_OF_MONTH, -(dow - 1));

        float ret = 0.0f;
        for (int i = 0; i < 7; i++)
        {
            int mon = cal.get(Calendar.MONTH);
            if (mon < month)
                ret += dailyBudgetLastMonth;
            else if (mon == month)
                ret += dailyBudgetThisMonth;
            else
                ret += dailyBudgetNextMonth;

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return ret;
    }

    public float getDailyBudget(int year, int month)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(year, month, 0);
        int dayCount = cal.getMaximum(Calendar.DAY_OF_MONTH);
        float monthlyBudget = getMonthlyBudget();
        float dailyBudget = monthlyBudget / dayCount;

        return dailyBudget;
    }

    public static int getdow(Calendar cal)
    {
        int dow = ((cal.get(Calendar.DAY_OF_WEEK)) % 7) - 1;
        if (dow == 0) {dow = 7;}

        return dow;
    }

    public void syncData()
    {

    }
}
