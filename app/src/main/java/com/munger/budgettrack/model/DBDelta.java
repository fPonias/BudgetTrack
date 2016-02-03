package com.munger.budgettrack.model;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.munger.budgettrack.Main;

import java.io.Serializable;

/**
 * Created by codymunger on 1/12/16.
 */
public class DBDelta implements Parcelable, Serializable
{
    public static final Parcelable.Creator<DBDelta> CREATOR = new Parcelable.Creator<DBDelta>()
    {
        public DBDelta createFromParcel(Parcel in)
        {
            return new DBDelta(in);
        }

        public DBDelta[] newArray(int size)
        {
            return new DBDelta[size];
        }
    };

    public static String TABLE_NAME = "delta";

    public long id;
    public String tableName;
    public String action;
    public long targetId;
    public Parcelable obj;
    public long date;

    public DBDelta()
    {
        id = -1;
        tableName = "";
        action = "";
        targetId = -1;
        obj =  null;
        date = System.currentTimeMillis();
    }

    public DBDelta(Parcel p)
    {
        id = p.readLong();
        tableName = p.readString();
        action = p.readString();
        targetId = p.readLong();

        int sz = p.readInt();
        byte[] bytes = new byte[sz];
        p.readByteArray(bytes);
        Object o = DatabaseHelper.unmarshall(bytes, DatabaseHelper.typeList.get(tableName));
        obj = (Parcelable) o;

        date = p.readLong();
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
        dest.writeString(tableName);
        dest.writeString(action);
        dest.writeLong(targetId);

        byte[] bytes = DatabaseHelper.marshall(obj);
        dest.writeInt(bytes.length);
        dest.writeByteArray(bytes);
        dest.writeLong(date);
    }

    public void commit()
    {
        ContentValues values = new ContentValues();
        values.put("tableName", tableName);
        values.put("action", action);
        values.put("targetId", targetId);

        byte[] bytes = DatabaseHelper.marshall(obj);
        values.put("size", bytes.length);
        values.put("obj", bytes);
        values.put("date", date);

        if (id > -1)
            Main.instance.dbHelper.db.getDb().update(TABLE_NAME, values, "id=?", new String[]{String.valueOf(id)});
        else
        {
            id = DatabaseHelper.getUniqueID();
            values.put("id", id);
            Main.instance.dbHelper.db.getDb().insert(TABLE_NAME, "", values);
        }
    }

    public static String getCreateTable()
    {
        String ret = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY," +
                "tableName TEXT," +
                "action TEXT," +
                "targetId INTEGER," +
                "size INTEGER," +
                "obj BLOB," +
                "date INTEGER" +
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
                "CREATE INDEX deltaDateIdx ON " + TABLE_NAME + " (date)"
        };

        return ret;
    }

    public boolean equals(DBDelta item)
    {
        if (id != item.id)
            return false;

        return true;
    }
}
