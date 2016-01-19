package com.munger.budgettrack.view;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;

import java.io.File;
import java.io.IOException;

/**
 * Created by codymunger on 12/23/15.
 */
public class Settings extends Fragment
{
    public EditText emergencyFundsTxt;
    public Button backupDBBtn;
    public Button restoreDBBtn;

    public Settings()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInf)
    {
        menuInf.inflate(R.menu.settings_menu, menu);

        super.onCreateOptionsMenu(menu, menuInf);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View ret = inflater.inflate(R.layout.fragment_settings, container, false);
        emergencyFundsTxt = (EditText) ret.findViewById(R.id.g_settings_emergencyFundTxt);
        backupDBBtn = (Button) ret.findViewById(R.id.g_settings_backupDB);
        restoreDBBtn = (Button) ret.findViewById(R.id.g_settings_restoreDB);

        backupDBBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
        {
            backupDatabase();
        }});

        restoreDBBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
        {
            restoreDatabase();
        }});

        load();

        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_cancel:
                Main.instance.goBack();

                return true;

            case R.id.action_save:
                boolean okay = validate();

                if (okay)
                {
                    save();
                    Main.instance.goBack();
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private boolean validate()
    {
        return true;
    }

    private void load()
    {
        String emerg = String.valueOf(Main.instance.settings.emergencyFund);
        emergencyFundsTxt.setText(emerg);

        File backupFileLocation = Main.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (backupFileLocation.exists())
            restoreDBBtn.setEnabled(true);
        else
            restoreDBBtn.setEnabled(false);
    }

    private void save()
    {
        String emerg = emergencyFundsTxt.getText().toString();
        Main.instance.settings.emergencyFund = Float.parseFloat(emerg);

        Main.instance.settings.save();

    }

    private void backupDatabase()
    {
        File fileDir = Main.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        String path = fileDir.getAbsolutePath() + "/BudgetManagerBackupDatabase.db";

        try
        {
            Main.instance.dbHelper.backupToStorage(path);
        }
        catch(IOException e){
            AlertDialog.Builder builder = new AlertDialog.Builder(Main.instance);

            builder.setMessage("Failed to backup database to file: " + path );
            builder.setTitle("Database backup failed");

            builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void restoreDatabase()
    {
        File fileDir = Main.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        String path = fileDir.getAbsolutePath() + "/BudgetManagerBackupDatabase.db";

        try
        {
            Main.instance.dbHelper.restoreFromStorage(path);
        }
        catch(IOException e){
            AlertDialog.Builder builder = new AlertDialog.Builder(Main.instance);

            builder.setMessage("Failed to restore database from file: " + path );
            builder.setTitle("Database restore failed");

            builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int id)
            {
            }});

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}
