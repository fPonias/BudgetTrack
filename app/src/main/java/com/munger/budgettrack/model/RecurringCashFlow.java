package com.munger.budgettrack.model;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.munger.budgettrack.Main;

/**
 * Created by codymunger on 12/23/15.
 */
public class RecurringCashFlow implements DatabaseHelper.DatabaseProxyParcelable
{
    public static final Parcelable.Creator<RecurringCashFlow> CREATOR = new Parcelable.Creator<RecurringCashFlow>()
    {
        public RecurringCashFlow createFromParcel(Parcel in)
        {
            return new RecurringCashFlow(in);
        }

        public RecurringCashFlow[] newArray(int size)
        {
            return new RecurringCashFlow[size];
        }
    };

    public static String TABLE_NAME = "recurringCashFlow";

    public long id;
    public float amount;
    public String start;
    public String end;
    public String desc;

    public RecurringCashFlow()
    {
        id = -1;
        amount = 0;
        start = "";
        end = "";
        desc = "";
    }

    public RecurringCashFlow(Parcel p)
    {
        id = p.readLong();
        amount = p.readFloat();
        start = p.readString();
        end = p.readString();
        desc = p.readString();
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
        dest.writeFloat(amount);
        dest.writeString(start);
        dest.writeString(end);
        dest.writeString(desc);
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        values.put("amount", amount);
        values.put("desc", desc);
        values.put("start", start);
        values.put("end", end);

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

    public static String getCreateTable()
    {
        String ret = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY," +
                "desc TEXT," +
                "start TEXT," +
                "end TEXT," +
                "amount FLOAT" +
                ")";

        return ret;
    }

    public static String[] getCreateIndices()
    {
        String[] ret = {
        };

        return ret;
    }
}
