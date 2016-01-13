package com.munger.budgettrack.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.munger.budgettrack.Main;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by codymunger on 12/23/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper
{
    public static interface DatabaseProxyParcelable extends Parcelable
    {
        public ContentValues getContentValues();
    }

    public static class DatabaseProxy
    {
        private SQLiteDatabase db;

        public DatabaseProxy(SQLiteDatabase db)
        {
            this.db = db;
        }

        public SQLiteDatabase getDb()
        {
            return db;
        }

        public Cursor query(String tableName, String[] columns, String where, String[] whereParams, String orderBy)
        {
            return db.query(tableName, columns, where, whereParams, null, null, orderBy);
        }

        public int update(String tableName, DatabaseProxyParcelable object)
        {
            DBDelta item = new DBDelta();
            ContentValues values = object.getContentValues();
            item.targetId = values.getAsLong("id");
            item.action = "UPDATE";
            item.date = System.currentTimeMillis();
            item.id = DatabaseHelper.getUniqueID();
            item.obj = object;
            item.tableName = tableName;
            item.commit();

            return db.update(tableName, values, "id=?", new String[]{String.valueOf(item.targetId)});
        }

        public int delete(String tableName, DatabaseProxyParcelable object)
        {
            DBDelta item = new DBDelta();
            ContentValues values = object.getContentValues();
            item.targetId = values.getAsLong("id");
            item.action = "DELETE";
            item.date = System.currentTimeMillis();
            item.id = DatabaseHelper.getUniqueID();
            item.obj = object;
            item.tableName = tableName;
            item.commit();

            return db.delete(tableName, "id=?", new String[]{String.valueOf(item.targetId)});
        }

        public long insert(String tableName, DatabaseProxyParcelable object)
        {
            DBDelta item = new DBDelta();
            ContentValues values = object.getContentValues();
            item.targetId = values.getAsLong("id");
            item.action = "CREATE";
            item.date = System.currentTimeMillis();
            item.id = DatabaseHelper.getUniqueID();
            item.obj = object;
            item.tableName = tableName;
            item.commit();

            return db.insert(tableName, "", values);
        }
    }

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Transaction.db";

    public static HashMap<String, Parcelable.Creator> typeList;

    static
    {
        typeList = new HashMap<String, Parcelable.Creator>();
        typeList.put(Transaction.TABLE_NAME, Transaction.CREATOR);
        typeList.put(TransactionCategory.TABLE_NAME, TransactionCategory.CREATOR);
        typeList.put(RecurringCashFlow.TABLE_NAME, RecurringCashFlow.CREATOR);
        typeList.put(CashFlow.TABLE_NAME, CashFlow.CREATOR);
    }

    private SQLiteDatabase database;
    public DatabaseProxy db;
    public Context context;

    public ArrayList<TransactionCategory> transactionCategories = null;

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        database = getWritableDatabase();
        db = new DatabaseProxy(database);
    }

    public DatabaseHelper(Context context, Bundle state)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        database = getWritableDatabase();
        db = new DatabaseProxy(database);

        Parcelable[] pararr = state.getParcelableArray("transactionCategories");
        transactionCategories = new ArrayList<>();
        for (Parcelable p : pararr)
            transactionCategories.add((TransactionCategory) p);
    }

    public Parcelable getState()
    {
        Bundle b = new Bundle();

        if (transactionCategories == null)
            transactionCategories = new ArrayList<>();

        int sz = transactionCategories.size();
        TransactionCategory[] tranCatArr = new TransactionCategory[sz];
        for (int i = 0; i < sz; i++)
            tranCatArr[i] = transactionCategories.get(i);
        b.putParcelableArray("transactionCategories", tranCatArr);

        return b;
    }

    private static long lastUID = 0;

    public static long getUniqueID(long minRandom)
    {
        long ret = System.currentTimeMillis();

        ret *= 100000;

        ret += Math.random() * (100000 - minRandom) + minRandom;
        lastUID = ret;
        return ret;
    }

    public static long getUniqueID()
    {
        long dt = System.currentTimeMillis();

        if (dt == lastUID / 100000)
        {
            long min = lastUID - dt * 100000 + 1;
            return getUniqueID(min);
        }
        else
            return getUniqueID(0);
    }

    public void onCreate(SQLiteDatabase db)
    {
        this.database = db;
        this.db = new DatabaseProxy(database);
        create();
    }

    private void create()
    {
        database.execSQL(Transaction.getCreateTable());
        String[] trans = Transaction.getCreateIndices();

        for (int i = 0; i < trans.length; i++)
        {
            database.execSQL(trans[i]);
        }

        database.execSQL(RecurringCashFlow.getCreateTable());
        trans = RecurringCashFlow.getCreateIndices();

        for (int i = 0; i < trans.length; i++)
        {
            database.execSQL(trans[i]);
        }

        database.execSQL(TransactionCategory.getCreateTable());

        database.execSQL(CashFlow.getCreateTable());
        trans = CashFlow.getCreateIndices();

        for (int i = 0; i < trans.length; i++)
        {
            database.execSQL(trans[i]);
        }

        String[] cats = TransactionCategory.getDefaultCategories();

        for (String cat : cats)
        {
            ContentValues values = new ContentValues();
            values.put("category", cat);
            database.insert(TransactionCategory.TABLE_NAME, "", values);
        }

        transactionCategories = null;
        loadTransactionCategories();

        database.execSQL(DBDelta.getCreateTable());
        trans = DBDelta.getCreateIndices();

        for (int i = 0; i < trans.length; i++)
        {
            database.execSQL(trans[i]);
        }
    }

    public void nuke()
    {
        database.execSQL("drop table if exists " + Transaction.TABLE_NAME);
        database.execSQL("drop table if exists " + TransactionCategory.TABLE_NAME);
        database.execSQL("drop table if exists " + CashFlow.TABLE_NAME);
        database.execSQL("drop table if exists " + RecurringCashFlow.TABLE_NAME);
        database.execSQL("drop table if exists " + DBDelta.TABLE_NAME);
        create();
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }

    public ArrayList<TransactionCategory> loadTransactionCategories()
    {
        if (transactionCategories != null)
            return transactionCategories;

        transactionCategories = new ArrayList<>();

        Cursor cur = database.query(TransactionCategory.TABLE_NAME, new String[]{"category", "id"},
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
        Cursor cur = database.query(RecurringCashFlow.TABLE_NAME, new String[]{"amount", "desc", "id"},
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

    public void syncData()
    {

    }

    public static byte[] marshall(Parcelable parceable) {
        Parcel parcel = Parcel.obtain();
        parceable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle(); // not sure if needed or a good idea
        return bytes;
    }

    public static Parcel unmarshall(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0); // this is extremely important!
        return parcel;
    }

    public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {
        Parcel parcel = unmarshall(bytes);
        return creator.createFromParcel(parcel);
    }
}
