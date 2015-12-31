package com.munger.budgettrack.model;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import com.munger.budgettrack.Main;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by codymunger on 12/15/15.
 */
public class Transaction implements Parcelable
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
    public long date;
    public String desc;
    public float amount;
    public boolean catastrophe;
    public long categoryId;

    public static ArrayList<TransactionCategory> transCats;
    private static HashMap<Long, TransactionCategory> transCatIndex;

    public Transaction()
    {
        id = -1;
        date = System.currentTimeMillis();
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
        date = p.readLong();
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
        dest.writeLong(date);
        dest.writeString(desc);
        dest.writeFloat(amount);
        dest.writeByte((catastrophe) ? (byte) 1 : 0);
        dest.writeLong(categoryId);
    }

    public void commit()
    {
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("amount", amount);
        values.put("desc", desc);
        values.put("catastrophe", (catastrophe) ? 1 : 0);
        values.put("categoryId", categoryId);

        if (id > -1)
            Main.instance.dbHelper.db.update(TABLE_NAME, values, "id=?", new String[] {String.valueOf(id)});
        else
            id = Main.instance.dbHelper.db.insert(TABLE_NAME, "", values);
    }

    public void delete()
    {
        Main.instance.dbHelper.db.delete(TABLE_NAME, "id=?", new String[]{String.valueOf(id)});
        id = -1;
    }

    public static String getDateString(long date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);

        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH) + 1;
        String dt = String.valueOf(month) + "/" + String.valueOf(day) + "/" + cal.get(Calendar.YEAR);
        return dt;
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
                "date INTEGER," +
                "desc TEXT," +
                "amount FLOAT," +
                "catastrophe INTEGER," +
                "categoryId INTEGER," +
                "FOREIGN KEY(categoryId) REFERENCES " + TransactionCategory.TABLE_NAME + "(id)" +
                ")";

        return ret;
    }

    public static String[] getUpdateTable(int oldversion)
    {
        String[] ret = null;
        if (oldversion < 2)
        {
            ret = new String[] {"ALTER TABLE " + TABLE_NAME + " ADD COLUMN categoryId INTEGER REFERENCES " + TransactionCategory.TABLE_NAME + "(id)"};
        }

        if (ret == null)
            return new String[0];
        else
            return ret;
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
