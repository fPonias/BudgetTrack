package com.munger.budgettrack.model;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.munger.budgettrack.Main;

import java.io.Serializable;

/**
 * Created by codymunger on 12/28/15.
 */
public class TransactionCategory implements DatabaseHelper.DatabaseProxyParcelable
{

    public static final Parcelable.Creator<TransactionCategory> CREATOR = new Parcelable.Creator<TransactionCategory>()
    {
        public TransactionCategory createFromParcel(Parcel in)
        {
            return new TransactionCategory(in);
        }

        public TransactionCategory[] newArray(int size)
        {
            return new TransactionCategory[size];
        }
    };

    public static String TABLE_NAME = "transactionCategory";

    public long id;
    public String category;

    public TransactionCategory()
    {
        id = -1;
        category = "";
    }

    public TransactionCategory(Parcel p)
    {
        id = p.readLong();
        category = p.readString();
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
        dest.writeString(category);
    }

    @Override
    public String toString()
    {
        return category;
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();
        values.put("category", category);

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
                "category TEXT" +
                ")";

        return ret;
    }

    public static String[] getDefaultCategories()
    {
        return new String[] {"fun", "eating out", "food", "transport", "medicine", "hardware", "laundry", "clothes", "allowance"};
    }
}
