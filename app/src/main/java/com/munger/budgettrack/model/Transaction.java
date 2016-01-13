package com.munger.budgettrack.model;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import com.munger.budgettrack.Main;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * Created by codymunger on 12/15/15.
 */
public class Transaction implements DatabaseHelper.DatabaseProxyParcelable
{
    public static final Parcelable.Creator<Transaction> CREATOR = new Parcelable.Creator<Transaction>()
    {
        public Transaction createFromParcel(Parcel in)
        {
            return new Transaction(in);
        }

        public Transaction[] newArray(int size)
        {
            return new Transaction[size];
        }
    };

    public static String TABLE_NAME = "transactions";

    public long id;
    public String date;
    public String desc;
    public float amount;
    public boolean catastrophe;
    public long categoryId;

    public static ArrayList<TransactionCategory> transCats;
    private static HashMap<Long, TransactionCategory> transCatIndex;

    public Transaction()
    {
        id = -1;
        date = "00000000";
        desc = "";
        amount = 0.0f;
        catastrophe = false;
        categoryId = -1;

        if (transCats == null)
        {
            transCats = Main.instance.dbHelper.loadTransactionCategories();
            indexCats();
        }
    }

    public Transaction(Parcel p)
    {
        id = p.readLong();
        date = p.readString();
        desc = p.readString();
        amount = p.readFloat();
        catastrophe = (p.readByte() == 0) ? false : true;
        categoryId = p.readLong();

        if (transCats == null)
        {
            transCats = Main.instance.dbHelper.loadTransactionCategories();
            indexCats();
        }
    }

    private void indexCats()
    {
        transCatIndex = new HashMap<>();
        for(TransactionCategory cat : transCats)
        {
            transCatIndex.put(cat.id, cat);
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(id);
        dest.writeString(date);
        dest.writeString(desc);
        dest.writeFloat(amount);
        dest.writeByte((catastrophe) ? (byte) 1 : 0);
        dest.writeLong(categoryId);
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("amount", amount);
        values.put("desc", desc);
        values.put("catastrophe", (catastrophe) ? 1 : 0);
        values.put("categoryId", categoryId);

        if (id == -1)
            values.put("id", DatabaseHelper.getUniqueID());
        else
            values.put("id", id);

        return values;
    }

    public void commit()
    {
        if (id > -1)
            Main.instance.dbHelper.db.update(TABLE_NAME, this);
        else
            id = Main.instance.dbHelper.db.insert(TABLE_NAME, this);
    }

    public void delete()
    {
        Main.instance.dbHelper.db.delete(TABLE_NAME, this);
        id = -1;
    }

    public static String getDateString(long date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);
        cal.setTimeZone(TimeZone.getDefault());

        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String dt = String.valueOf(month) + "/" + String.valueOf(day) + "/" + cal.get(Calendar.YEAR);
        return dt;
    }

    public static String dateToKey(long date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);
        return dateToKey(cal);
    }

    public static String dateToKey(Calendar cal)
    {
        String ret = "";

        ret += String.valueOf(cal.get(Calendar.YEAR));


        int month = cal.get(Calendar.MONTH) + 1;

        if (month < 10)
            ret += "0";

        ret += String.valueOf(month);


        int day = cal.get(Calendar.DAY_OF_MONTH);

        if (day < 10)
            ret += "0";

        ret += String.valueOf(day);


        return ret;
    }

    public static Calendar keyToDate(String key)
    {
        Calendar ret = Calendar.getInstance();

        ret.setTimeZone(TimeZone.getDefault());

        String year = key.substring(0, 4);
        String month = key.substring(4, 6);
        String day = key.substring(6);

        ret.set(Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day), 12, 0);
        return ret;
    }

    public static String keyToDateString(String key)
    {
        Calendar cal = keyToDate(key);
        return getDateString(cal.getTimeInMillis());
    }

    public static TransactionCategory getCategory(long id)
    {
        TransactionCategory cat = transCatIndex.get(id);

        if (cat == null)
            cat = transCats.get(0);

        return cat;
    }

    public static String getCreateTable()
    {
        String ret = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY," +
                "date TEXT," +
                "desc TEXT," +
                "amount FLOAT," +
                "catastrophe INTEGER," +
                "categoryId INTEGER" +
                ")";

        return ret;
    }

    public static String[] getUpdateTable(int oldversion)
    {
        return new String[]{};
    }

    public static String[] getCreateIndices()
    {
        String[] ret = {
                "CREATE INDEX transactionDateIdx ON " + TABLE_NAME + " (date)",
                "CREATE INDEX transactionCatastropheIdx ON " + TABLE_NAME + " (catastrophe)"
        };

        return ret;
    }
}
