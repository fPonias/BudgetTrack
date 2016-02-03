package com.munger.budgettrack.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;

import com.munger.budgettrack.Main;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
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
    public static interface DatabaseProxyParcelable extends Parcelable, Serializable
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
    public static final int DATABASE_VERSION = 3;
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
        if (oldVersion < 2)
        {
            try
            {
                db.execSQL("ALTER TABLE " + CashFlow.TABLE_NAME + " ADD COLUMN categoryId INTEGER");
                db.execSQL("CREATE INDEX cashFlowCategoryIdIdx ON " + CashFlow.TABLE_NAME + "(categoryId)");
            }
            catch(SQLiteException e)
            {}
        }
        if (oldVersion < 3)
        {
            db.execSQL("UPDATE " + CashFlow.TABLE_NAME + " SET startDate=0, endDate=" + Long.MAX_VALUE);
        }
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

    public ArrayList<TransactionCategory> reloadTransactionCategories()
    {
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

    public ArrayList<TransactionCategory> loadTransactionCategories()
    {
        if (transactionCategories != null)
            return transactionCategories;

        reloadTransactionCategories();

        return transactionCategories;
    }

    public void syncData()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        ArrayList<DBDelta> remoteList = null;

        try
        {
            remoteList = Main.instance.remoteStorageService.getRemoteChangelog(cal);

        }
        catch(IOException e){
            System.console().printf("failed to load remote changelog");
            return;
        }

        int startIdx = 0;
        int rsz = remoteList.size();
        int lsz = changeLog.size();
        int i = 0;
        while (i < lsz && i < rsz)
        {
            DBDelta ritem = remoteList.get(i);
            DBDelta litem = changeLog.get(i);

            if (!ritem.equals(litem))
                break;

            i++;
        }

        boolean uploadChanges = false;
        if (i == lsz && i == rsz)
        {
            return;
        }
        else if (i == lsz)
        {
            mergeAndExecuteChangeLists(changeLog, remoteList);
        }
        else if (i == rsz)
        {
            mergeAndExecuteChangeLists(changeLog, remoteList);
            uploadChanges = true;
        }
        else
        {
            DBDeltaConflict conflicts = getConflicts(changeLog, remoteList);
            mergeAndExecuteChangeLists(changeLog, remoteList);
            resolveConflicts(conflicts);
            uploadChanges = true;
        }

        /*if (uploadChanges)
        {
            try
            {
                Main.instance.remoteStorageService.overwriteRemoteChangeLog(cal, changeLog);
            }
            catch(IOException e){
                System.console().printf("failed to upload new database changes with error " + e.getMessage());
                syncData();
            }
        }*/
    }

    private static class DBDeltaConflict
    {
        public ArrayList<DBDelta> localChange;
        public ArrayList<DBDelta> remoteChange;

        public DBDeltaConflict()
        {
            localChange = new ArrayList<>();
            remoteChange = new ArrayList<>();
        }
    }

    private DBDeltaConflict getConflicts(ArrayList<DBDelta> localList , ArrayList<DBDelta> remoteList)
    {
        DBDeltaConflict ret = new DBDeltaConflict();
        return ret;
    }

    private void mergeAndExecuteChangeLists(ArrayList<DBDelta> localList , ArrayList<DBDelta> remoteList)
    {

    }

    private HashMap<String, Long> resolvedToDate = new HashMap<>();

    private void resolveConflicts(DBDeltaConflict conflicts)
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

    public void backupToStorage(String outPath) throws IOException
    {
        File fileDir = Main.instance.getFilesDir();
        final String inFileName = fileDir.getParentFile().getAbsolutePath() + "/databases/" + DATABASE_NAME;
        File outFile = new File(outPath);
        File dbFile = new File(inFileName);
        outFile.createNewFile();

        FileInputStream fis = new FileInputStream(dbFile);;
        OutputStream output = new FileOutputStream(outFile);

        // Transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[4096];
        int length;
        while ((length = fis.read(buffer))>0)
        {
            output.write(buffer, 0, length);
        }

        // Close the streams
        output.flush();
        try {output.close();} catch(Exception e){}
        try {fis.close();} catch(Exception e){}

    }

    public void restoreFromStorage(String inPath) throws IOException
    {
        File backupFile = new File(inPath);

        if (!backupFile.exists())
            throw new IOException("backup file inPath doesn't exist");

        File fileDir = Main.instance.getFilesDir();
        final String dbFilePath = fileDir.getParentFile().getAbsolutePath() + "/databases/" + DATABASE_NAME;
        File dbFile = new File(dbFilePath);
        if (dbFile.exists())
            dbFile.delete();

        database.close();

        FileInputStream fis = new FileInputStream(backupFile);
        FileOutputStream fos = new FileOutputStream(dbFile);


        // Transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[4096];
        int length;
        while ((length = fis.read(buffer))>0)
        {
            fos.write(buffer, 0, length);
        }

        // Close the streams
        fos.flush();
        try {fos.close();} catch(Exception e){}
        try {fis.close();} catch(Exception e){}


        database = getWritableDatabase();
        db = new DatabaseProxy(this, database);

        loadTransactionCategories();
        loadChangeLog();
        Main.instance.transactionService.loadCurrentTransactions();
        Main.instance.cashFlowService.loadData();

        Main.instance.reloadView();
    }
}
