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
        private DatabaseHelper parent;

        public DatabaseProxy(DatabaseHelper parent, SQLiteDatabase db)
        {
            this.parent = parent;
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
            item.id = -1;
            item.obj = object;
            item.tableName = tableName;
            item.commit();

            parent.changeLog.add(item);

            return db.update(tableName, values, "id=?", new String[]{String.valueOf(item.targetId)});
        }

        public int delete(String tableName, DatabaseProxyParcelable object)
        {
            DBDelta item = new DBDelta();
            ContentValues values = object.getContentValues();
            item.targetId = values.getAsLong("id");
            item.action = "DELETE";
            item.date = System.currentTimeMillis();
            item.id = -1;
            item.obj = object;
            item.tableName = tableName;
            item.commit();

            parent.changeLog.add(item);

            return db.delete(tableName, "id=?", new String[]{String.valueOf(item.targetId)});
        }

        public long insert(String tableName, DatabaseProxyParcelable object)
        {
            DBDelta item = new DBDelta();
            ContentValues values = object.getContentValues();
            item.targetId = values.getAsLong("id");
            item.action = "CREATE";
            item.date = System.currentTimeMillis();
            item.id = -1;
            item.obj = object;
            item.tableName = tableName;
            item.commit();

            parent.changeLog.add(item);

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
        typeList.put(CashFlow.TABLE_NAME, CashFlow.CREATOR);
    }

    private SQLiteDatabase database;
    public DatabaseProxy db;
    public Context context;

    public ArrayList<TransactionCategory> transactionCategories = null;
    public ArrayList<DBDelta> changeLog = null;

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        database = getWritableDatabase();
        db = new DatabaseProxy(this, database);
    }

    public DatabaseHelper(Context context, Bundle state)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        database = getWritableDatabase();
        db = new DatabaseProxy(this, database);

        Parcelable[] pararr = state.getParcelableArray("transactionCategories");
        transactionCategories = new ArrayList<>();
        for (Parcelable p : pararr)
            transactionCategories.add((TransactionCategory) p);

        Parcelable[] deltaarr = state.getParcelableArray("changeLog");
        changeLog = new ArrayList<>();
        for (Parcelable p : deltaarr)
            changeLog.add((DBDelta) p);
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


        if (changeLog == null)
            changeLog = new ArrayList<>();

        sz = changeLog.size();
        DBDelta[] deltaArr = new DBDelta[sz];
        for (int i = 0; i < sz; i++)
            deltaArr[i] = changeLog.get(i);
        b.putParcelableArray("changeLog", deltaArr);

        return b;
    }

    private static long lastUID = 0;

    public static long getUniqueID(long minRandom)
    {
        long ret = System.currentTimeMillis();

        ret *= 100000;

        ret += ((int)(Math.random() * (100000 - minRandom))) + minRandom;
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
        this.db = new DatabaseProxy(this, database);
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

        changeLog = null;
        loadChangeLog();
    }

    public void nuke()
    {
        database.execSQL("drop table if exists " + Transaction.TABLE_NAME);
        database.execSQL("drop table if exists " + TransactionCategory.TABLE_NAME);
        database.execSQL("drop table if exists " + CashFlow.TABLE_NAME);
        database.execSQL("drop table if exists " + DBDelta.TABLE_NAME);
        create();
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }

    public ArrayList<DBDelta> loadChangeLog()
    {
        if (changeLog != null)
            return changeLog;

        changeLog = new ArrayList<>();

        Cursor cur = database.query(DBDelta.TABLE_NAME, new String[]{"id", "tableName", "action", "targetId", "size", "obj", "date"},
                "", new String[]{}, null, null, "date ASC");

        boolean success = cur.moveToFirst();
        while(success)
        {
            DBDelta item = new DBDelta();
            item.id = cur.getLong(0);
            item.tableName = cur.getString(1);
            item.action = cur.getString(2);
            item.targetId = cur.getLong(3);

            int sz = cur.getInt(4);
            byte[] objEnc = cur.getBlob(5);
            Object o = unmarshall(objEnc, typeList.get(item.tableName));
            item.obj = (Parcelable) o;

            item.date = cur.getLong(6);
            changeLog.add(item);

            success = cur.moveToNext();
        }

        cur.close();
        return changeLog;
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
