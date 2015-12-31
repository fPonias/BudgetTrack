package com.munger.budgettrack.model;

import android.content.ContentValues;

import com.munger.budgettrack.Main;

/**
 * Created by codymunger on 12/23/15.
 */
public class RecurringCashFlow
{
    public static String TABLE_NAME = "recurringCashFlow";

    public long id;
    public float amount;
    public String desc;

    public RecurringCashFlow()
    {
        id = -1;
        amount = 0;
        desc = "";
    }

    public void commit()
    {
        ContentValues values = new ContentValues();
        values.put("amount", amount);
        values.put("desc", desc);

        if (id > -1)
            Main.instance.dbHelper.db.update(TABLE_NAME, values, "id=?", new String[] {String.valueOf(id)});
        else
            id = Main.instance.dbHelper.db.insert(TABLE_NAME, "", values);
    }

    public static String getCreateTable()
    {
        String ret = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY," +
                "desc TEXT," +
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
