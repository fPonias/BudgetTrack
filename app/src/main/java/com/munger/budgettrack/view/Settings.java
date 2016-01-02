package com.munger.budgettrack.view;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.Transaction;

/**
 * Created by codymunger on 12/23/15.
 */
public class Settings extends Fragment
{
    public EditText monthlyExpensesTxt;
    public EditText monthlyIncomeTxt;
    public EditText emergencyFundsTxt;

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
        monthlyExpensesTxt = (EditText) ret.findViewById(R.id.g_settings_monthlyExpensesTxt);
        monthlyIncomeTxt = (EditText) ret.findViewById(R.id.g_settings_monthlyIncomeTxt);
        emergencyFundsTxt = (EditText) ret.findViewById(R.id.g_settings_emergencyFundTxt);

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
    }

    private void save()
    {
        String emerg = emergencyFundsTxt.getText().toString();
        Main.instance.settings.emergencyFund = Float.parseFloat(emerg);

        Main.instance.settings.save();

    }
}
