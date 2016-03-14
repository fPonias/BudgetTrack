package com.munger.budgettrack.service;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.model.TransactionCategory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;

public class TransactionService
{
    public static class SortedData
    {
        protected TransactionService parent;

        public HashMap<String, ArrayList<Transaction>> sortedTransactions;
        public HashMap<String, ArrayList<Transaction>> sortedCatastrophe;
        public HashMap<String, ArrayList<Transaction>> sortedCategory;
        public HashMap<String, Float> totaledTransactions;
        public HashMap<String, Float> totaledCatastrophe;
        public HashMap<String, Float> totaledCategory;
        public HashMap<String, Float> totaledCategorySansCatastrophe;
        public HashMap<String, Float> totaledCategoryWithExpenses;
        public HashMap<String, Float> trendingAverages;
        public ArrayList<Transaction> transactions;

        public Calendar startCal;
        public int daySpan;
        public Calendar endCal;
        public int trendLength = 5;

        public SortedData(TransactionService parent)
        {
            this.parent = parent;
        }

        public void load(Calendar cal, int days)
        {
            startCal = cal;
            daySpan = days;
            transactions = new ArrayList<>();

            long transStart = startCal.getTimeInMillis();

            endCal = Calendar.getInstance();
            endCal.setTimeZone(cal.getTimeZone());
            endCal.setTimeInMillis(transStart);
            endCal.add(Calendar.DAY_OF_YEAR, days);


            Cursor cur = Main.instance.dbHelper.db.query(Transaction.TABLE_NAME, new String[]{"id"},
                    "date >= ? and date < ?", new String[]{Transaction.dateToKey(startCal), Transaction.dateToKey(endCal)},
                    "date ASC"
            );

            while (cur.moveToNext())
            {
                long id = cur.getLong(0);

                if (parent.indexedTransactions.containsKey(id))
                {
                    Transaction t = parent.indexedTransactions.get(id);
                    transactions.add(t);
                }
            }

            sort();
            calculateTrend();

            ArrayList<CashFlow> list = Main.instance.cashFlowService.getList(CashFlowService.Type.EXPENDITURE, startCal, daySpan);
            sortExpenditures(list);
        }

        private void sort()
        {
            sortedTransactions = new HashMap<>();
            totaledTransactions = new HashMap<>();
            sortedCatastrophe = new HashMap<>();
            totaledCatastrophe = new HashMap<>();
            sortedCategory = new HashMap<>();
            totaledCategory = new HashMap<>();
            totaledCategorySansCatastrophe = new HashMap<>();
            totaledCategoryWithExpenses = new HashMap<>();

            long smallestDate = Long.MAX_VALUE;

            for (Transaction t : transactions)
            {
                String key = Transaction.dateToKey(t.date);

                if (t.date < smallestDate)
                    smallestDate = t.date;

                HashMap<String, ArrayList<Transaction>> sortedList = null;
                HashMap<String, Float> totaledList = null;

                if (!t.catastrophe)
                {
                    sortedList = sortedTransactions;
                    totaledList = totaledTransactions;
                } else
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
                {
                    totaledCategory.put(category, 0.0f);
                    totaledCategorySansCatastrophe.put(category, 0.0f);
                    totaledCategoryWithExpenses.put(category, 0.0f);
                }

                newTotal = totaledCategory.get(category) + t.amount;
                totaledCategory.put(category, newTotal);
                totaledCategoryWithExpenses.put(category, newTotal);

                if (t.catastrophe == false)
                {
                    newTotal = totaledCategorySansCatastrophe.get(category) + t.amount;
                    totaledCategorySansCatastrophe.put(category, newTotal);
                }
            }
        }

        private void calculateTrend()
        {
            trendingAverages = new HashMap<>();

            ArrayList<Float> valueQueue = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(startCal.getTimeInMillis());
            long transEnd = endCal.getTimeInMillis();
            while (cal.getTimeInMillis() <= transEnd)
            {
                String key = Transaction.dateToKey(cal);
                float value = 0;
                if (totaledTransactions.containsKey(key))
                    value = totaledTransactions.get(key);

                valueQueue.add(value);

                if (valueQueue.size() == trendLength + 1)
                    valueQueue.remove(0);

                if (valueQueue.size() == trendLength)
                {
                    float total = 0;
                    for (float item : valueQueue)
                        total += item;

                    float average = total / trendLength;
                    trendingAverages.put(key, average);
                }

                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        private void sortExpenditures(ArrayList<CashFlow> expenditures)
        {
            for (CashFlow item : expenditures)
            {
                TransactionCategory category = Main.instance.dbHelper.transactionCategoryIndex.get(item.categoryId);
                if (category == null)
                    category = Main.instance.dbHelper.transactionCategories.get(0);

                if(!totaledCategoryWithExpenses.containsKey(category.category))
                    totaledCategoryWithExpenses.put(category.category, 0.0f);

                float value = totaledCategoryWithExpenses.get(category.category);
                value -= item.amount;
                totaledCategoryWithExpenses.put(category.category, value);
            }
        }
    }


    public HashMap<Long, Transaction> indexedTransactions;
    public ArrayList<Transaction> transactions;

    public TransactionService()
    {
        indexedTransactions = new HashMap<>();
        transactions = new ArrayList<>();
    }

    public TransactionService(Bundle state)
    {
        Parcelable[] pararr = state.getParcelableArray("transactions");
        transactions = new ArrayList<>();
        for(Parcelable p : pararr)
            transactions.add((Transaction) p);
    }

    public Bundle getState()
    {
        Bundle b = new Bundle();

        int sz = transactions.size();
        Transaction[] tranArr = new Transaction[sz];
        for (int i = 0; i < sz; i++)
            tranArr[i] = transactions.get(i);

        return b;
    }

    public void loadAll()
    {
        Cursor cur = Main.instance.dbHelper.db.query(Transaction.TABLE_NAME, new String[]{"date", "amount", "desc", "catastrophe", "categoryId", "id"},
                "", new String[]{},
                "date ASC"
        );

        int sz = cur.getCount();
        ArrayList<Transaction> ret = new ArrayList<>();
        while (cur.moveToNext())
        {
            Transaction t = new Transaction();
            t.id = cur.getLong(5);
            String dateKey = cur.getString(0);
            t.date = Transaction.keyToDate(dateKey).getTimeInMillis();
            t.amount = cur.getFloat(1);
            t.desc = cur.getString(2);
            t.catastrophe = (cur.getInt(3) == 1) ? true : false;
            t.categoryId = cur.getLong(4);

            ret.add(t);
            indexedTransactions.put(t.id, t);
        }

        cur.close();

        transactions = ret;
    }

    public void deleteTransaction(Transaction tr)
    {
        transactions.remove(tr);
        tr.delete();
        invalidateData(tr.date);

        for(TransactionsChangedListener listener : changeListeners)
        {
            listener.changed();
        }
    }

    public void commitTransaction(Transaction tr)
    {
        int sz = transactions.size();
        for (int i = 0; i < sz; i++)
        {
            Transaction trans = transactions.get(i);
            if (trans.date < tr.date)
            {
                transactions.add(i, tr);
                invalidateData(tr.date);
                break;
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

    public float getTrend(int year, int month, int day)
    {
        return 0;

        /*
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(year, month, day);
        String key = Transaction.dateToKey(cal);

        if (trendingAverages.containsKey(key))
            return trendingAverages.get(key);
        else
            return 0;
            */
    }

    private HashMap<String, SortedData> sortedDataCache = new HashMap<>();

    private String calendarToKey(Calendar cal)
    {
        return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "-";
    }

    public SortedData getSortedData(Calendar cal, int days)
    {
        String key = calendarToKey(cal) + String.valueOf(days);
        if (sortedDataCache.containsKey(key))
            return sortedDataCache.get(key);

        SortedData data = new SortedData(this);
        data.load(cal, days);

        sortedDataCache.put(key, data);
        return data;
    }

    private void invalidateData(long stamp)
    {
        Object[] keys = sortedDataCache.keySet().toArray();
        int sz = keys.length;
        for (int i = 0; i < sz; i++)
        {
            String key = (String) keys[i];
            String[] parts = key.split("-");
            Calendar calStart = Calendar.getInstance();
            calStart.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            long keyStart = calStart.getTimeInMillis();
            Calendar calEnd = Calendar.getInstance();
            calEnd.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            calEnd.add(Calendar.DAY_OF_YEAR, Integer.parseInt(parts[3]));
            long keyEnd = calEnd.getTimeInMillis();


            if (stamp >= keyStart && stamp <= keyEnd)
                sortedDataCache.remove(key);
        }
    }

    public float getMonthlyTotal(Calendar c)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.setTimeInMillis(c.getTimeInMillis());
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int max = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        SortedData data = getSortedData(cal, max);
        float total = 0;

        for (int i = 0; i < max; i++)
        {
            long date = cal.getTimeInMillis();
            String key = Transaction.dateToKey(date);

            if (data.totaledTransactions.containsKey(key))
                total += data.totaledTransactions.get(key);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return total;
    }

    public float getCatastropheTotal(Calendar c)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.setTimeInMillis(c.getTimeInMillis());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        float total = 0;

        int max = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        SortedData data = getSortedData(cal, max);

        for (int i = 0; i < max; i++)
        {
            long date = cal.getTimeInMillis();
            String key = Transaction.dateToKey(date);

            if (data.totaledCatastrophe.containsKey(key))
                total += data.totaledCatastrophe.get(key);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return total;
    }

    public float getWeeklyTotal(Calendar c)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.setTimeInMillis(c.getTimeInMillis());
        int dow = getdow(cal);
        cal.add(Calendar.DAY_OF_MONTH, -dow);

        float ret = 0.0f;
        SortedData data = getSortedData(cal, 7);
        for (int i = 0; i < 7; i++)
        {
            long date = cal.getTimeInMillis();
            String key = Transaction.dateToKey(date);
            if (data.totaledTransactions.containsKey(key))
                ret += data.totaledTransactions.get(key);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return ret;
    }

    public float getMonthlyBudget(Calendar cal)
    {
        int sz = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        float income = Main.instance.cashFlowService.getTotal(CashFlowService.Type.INCOME, cal, sz);
        float expense = Main.instance.cashFlowService.getTotal(CashFlowService.Type.EXPENDITURE, cal, sz);
        float monthlyBudget = income - expense;
        monthlyBudget -= Main.instance.settings.emergencyFund;

        return monthlyBudget;
    }

    public float getWeeklyBudget(Calendar c)
    {
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(year, month, 1);

        cal.add(Calendar.MONTH, -1);
        float dailyBudgetLastMonth = getDailyBudget(cal);
        cal.add(Calendar.MONTH, 1);
        float dailyBudgetThisMonth = getDailyBudget(cal);
        cal.add(Calendar.MONTH, 1);
        float dailyBudgetNextMonth = getDailyBudget(cal);

        cal.set(year, month, c.get(Calendar.DAY_OF_YEAR));
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

    public float getDailyBudget(Calendar cal)
    {
        int dayCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        float monthlyBudget = getMonthlyBudget(cal);
        float dailyBudget = monthlyBudget / dayCount;

        return dailyBudget;
    }

    public static int getdow(Calendar cal)
    {
        int dow = ((cal.get(Calendar.DAY_OF_WEEK)) % 7) - 2;
        if (dow < 0) {dow += 7;}

        return dow;
    }

    public void syncData()
    {

    }
}
