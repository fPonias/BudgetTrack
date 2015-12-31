package com.munger.budgettrack.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Parcelable;

import com.munger.budgettrack.Main;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by codymunger on 12/23/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper
{
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "Transaction.db";

    public SQLiteDatabase db;

    public ArrayList<TransactionCategory> transactionCategories = null;

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }

    public DatabaseHelper(Context context, Bundle state)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();

        Parcelable[] pararr = state.getParcelableArray("transactionCategories");
        transactionCategories = new ArrayList<>();
        for (Parcelable p : pararr)
            transactionCategories.add((TransactionCategory) p);
    }

    public Parcelable getState()
    {
        Bundle b = new Bundle();

        int sz = transactionCategories.size();
        TransactionCategory[] tranCatArr = new TransactionCategory[sz];
        for (int i = 0; i < sz; i++)
            tranCatArr[i] = transactionCategories.get(i);
        b.putParcelableArray("transactionCategories", tranCatArr);

        return b;
    }

    public void onCreate(SQLiteDatabase db)
    {
        create(db);
    }

    public void nuke()
    {
        db.execSQL("DROP TABLE IF EXISTS " + Transaction.TABLE_NAME);
        db.execSQL("DROP INDEX IF EXISTS transactionDateIdx");
        db.execSQL("DROP INDEX IF EXISTS transactionCatastropheIdx");

        db.execSQL("DROP TABLE IF EXISTS " + RecurringCashFlow.TABLE_NAME);

        create(db);
    }

    private void create(SQLiteDatabase db)
    {
        db.execSQL(Transaction.getCreateTable());
        String[] trans = Transaction.getCreateIndices();

        for (int i = 0; i < trans.length; i++)
        {
            db.execSQL(trans[i]);
        }

        db.execSQL(RecurringCashFlow.getCreateTable());
        trans = RecurringCashFlow.getCreateIndices();

        for (int i = 0; i < trans.length; i++)
        {
            db.execSQL(trans[i]);
        }
        
        db.execSQL(TransactionCategory.getCreateTable());
        
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        if (oldVersion < 2)
            db.execSQL(TransactionCategory.getCreateTable());

        String[] trans = Transaction.getUpdateTable(oldVersion);
        for (int i = 0; i < trans.length; i++)
        {
            db.execSQL(trans[i]);
        }

        if (oldVersion < 3)
        {
            String[] cats = new String[] {"food", "medicine", "allowance", "fun", "transport", "hardware", "softwear"};

            for (String cat : cats)
            {
                ContentValues values = new ContentValues();
                values.put("category", cat);
                db.insert(TransactionCategory.TABLE_NAME, "", values);
            }
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }

    public ArrayList<TransactionCategory> loadTransactionCategories()
    {
        if (transactionCategories != null)
            return transactionCategories;

        transactionCategories = new ArrayList<>();

        Cursor cur = db.query(TransactionCategory.TABLE_NAME, new String[]{"category", "id"},
                "", new String[]{}, null, null, "id ASC");

        boolean success = cur.moveToFirst();
        while(success)
        {
            TransactionCategory cat = new TransactionCategory();
            cat.category = cur.getString(0);
            cat.id = cur.getLong(1);

            transactionCategories.add(cat);
            success = cur.moveToNext();
        }

        cur.close();

        return transactionCategories;
    }

    public ArrayList<RecurringCashFlow> getRecurringExpenses()
    {
        return getRecurringCashFlow("amount<0");
    }

    public ArrayList<RecurringCashFlow> getRecurringIncome()
    {
        return getRecurringCashFlow("amount>0");
    }

    private ArrayList<RecurringCashFlow> getRecurringCashFlow(String where)
    {
        Cursor cur = db.query(RecurringCashFlow.TABLE_NAME, new String[]{"amount", "desc", "id"},
                where, new String[]{}, null, null, "id ASC");

        ArrayList<RecurringCashFlow> ret = new ArrayList<>();
        boolean success = cur.moveToFirst();
        while (success)
        {
            RecurringCashFlow r = new RecurringCashFlow();
            r.id = cur.getLong(2);
            r.amount = cur.getFloat(0);
            r.desc = cur.getString(1);

            ret.add(r);
            success = cur.moveToNext();
        }

        cur.close();

        return ret;
    }
}
