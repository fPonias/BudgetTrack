package com.munger.budgettrack.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.munger.budgettrack.Main;

/**
 * Created by codymunger on 12/23/15.
 */
public class Settings
{
    public float monthlyIncome;
    public float monthlyExpenses;
    public float emergencyFund;

    public static String fileKey = "com.munger.budgettrack.Preferences";

    private SharedPreferences prefFile;

    public Settings()
    {
        Context context = Main.instance;
        prefFile = context.getSharedPreferences(fileKey, Context.MODE_PRIVATE);

        monthlyIncome = prefFile.getFloat("monthlyIncome", 0);
        monthlyExpenses = prefFile.getFloat("monthlyExpenses", 0);
        emergencyFund = prefFile.getFloat("emergencyFund", 0);

    }

    public void save()
    {
        SharedPreferences.Editor editor = prefFile.edit();
        editor.putFloat("monthlyIncome", monthlyIncome);
        editor.putFloat("monthlyExpenses", monthlyExpenses);
        editor.putFloat("emergencyFund", emergencyFund);
        editor.commit();
    }
}
