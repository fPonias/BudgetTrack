package com.munger.budgettrack.model;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;

import com.munger.budgettrack.Main;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by codymunger on 12/31/15.
 */
public class CashFlow implements DatabaseHelper.DatabaseProxyParcelable
{
    public static final Parcelable.Creator<CashFlow> CREATOR = new Parcelable.Creator<CashFlow>()
    {
        public CashFlow createFromParcel(Parcel in)
        {
            return new CashFlow(in);
        }

        public CashFlow[] newArray(int size)
        {
            return new CashFlow[size];
        }
    };

    public static String TABLE_NAME = "cashFlow";

    public long id;
    public String desc;
    public float amount;
    public long startDate;
    public long endDate;

    public CashFlow()
    {
        id = -1;
        startDate = 0;
        endDate = 0;
        desc = "";
        amount = 0.0f;
    }

    public CashFlow(Parcel p)
    {
        id = p.readLong();
        startDate = p.readLong();
        endDate = p.readLong();
        desc = p.readString();
        amount = p.readFloat();
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
        dest.writeLong(startDate);
        dest.writeLong(endDate);
        dest.writeString(desc);
        dest.writeFloat(amount);
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        values.put("startDate", Transaction.dateToKey(startDate));
        values.put("endDate", Transaction.dateToKey(endDate));
        values.put("amount", amount);
        values.put("desc", desc);

        if (id == -1)
            id = DatabaseHelper.getUniqueID();

        values.put("id", id);

        return values;
    }


    public void commit()
    {
        if (id > -1)
            Main.instance.dbHelper.db.update(TABLE_NAME, this);
        else
            Main.instance.dbHelper.db.insert(TABLE_NAME, this);
    }

    public void delete()
    {
        Main.instance.dbHelper.db.delete(TABLE_NAME, this);
        id = -1;
    }



    public static String getCreateTable()
    {
        String ret = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY," +
                "startDate TEXT," +
                "endDate TEXT," +
                "desc TEXT," +
                "amount FLOAT" +
                ")";

        return ret;
    }

    public static String[] getCreateIndices()
    {
        String[] ret = {
                "CREATE INDEX cashFlowStartDateIdx ON " + TABLE_NAME + " (startDate)",
                "CREATE INDEX cashFlowEndDateIdx ON " + TABLE_NAME + "(endDate)"
        };

        return ret;
    }
}
