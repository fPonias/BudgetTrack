package com.munger.budgettrack.view;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.service.TransactionService;

import java.util.Calendar;

public class Overview extends Fragment
{
    public TextView remainingDaysWeekTxt;
    public TextView remainingBudgetWeekTxt;
    public TextView avgSpendingWeekTxt;
    public TextView remainingDaysMonthTxt;
    public TextView remainingBudgetMonthTxt;
    public TextView avgSpendingMonthTxt;
    public TextView remainingCatastropheTxt;

    private TransactionService.TransactionsChangedListener dataListener;

    public Overview()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View ret = inflater.inflate(R.layout.fragment_overview, container, false);

        remainingDaysWeekTxt = (TextView) ret.findViewById(R.id.g_overview_remainingDaysWeekTxt);
        remainingBudgetWeekTxt = (TextView) ret.findViewById(R.id.g_overview_remainingBudgetWeekTxt);
        avgSpendingWeekTxt = (TextView) ret.findViewById(R.id.g_overview_averageWeekTxt);
        remainingDaysMonthTxt = (TextView) ret.findViewById(R.id.g_overview_remainingDaysMonthTxt);
        remainingBudgetMonthTxt = (TextView) ret.findViewById(R.id.g_overview_remainingBudgetMonthTxt);
        avgSpendingMonthTxt = (TextView) ret.findViewById(R.id.g_overview_averageMonthTxt);
        remainingCatastropheTxt = (TextView) ret.findViewById(R.id.g_overview_remainingCatstropheTxt);

        dataListener = new TransactionService.TransactionsChangedListener() {public void changed()
        {
            update();
        }};
        Main.instance.transactionService.addChangeListener(dataListener);

        return ret;
    }

    @Override
    public void onDestroy()
    {
        Main.instance.transactionService.removeChangeListener(dataListener);
        super.onDestroy();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInf)
    {
        menuInf.inflate(R.menu.overview_menu, menu);

        super.onCreateOptionsMenu(menu, menuInf);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_add:
                Main.instance.loadEntry();
                return true;

            case R.id.action_settings:
                Main.instance.loadSettings();
                return true;

            case R.id.action_ledger:
                Main.instance.loadLedger();
                return true;

            case R.id.action_chart:
                Main.instance.loadChart();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onStart()
    {
        update();
        super.onStart();
    }

    public void update()
    {
        com.munger.budgettrack.model.Settings settings = Main.instance.settings;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        Main.instance.transactionService.loadTransactions(year, month);

        int dayCount = cal.getMaximum(Calendar.DAY_OF_MONTH);
        float monthTotal = Main.instance.transactionService.getMonthlyTotal(year, month);
        float monthaverage = monthTotal / dayCount;
        monthaverage = Math.round(monthaverage * 100) / 100.0f;

        float cataTotal = Main.instance.transactionService.getCatastropheTotal(year, month);
        float remainingCata = Main.instance.settings.emergencyFund - cataTotal;
        float cataaverate = cataTotal / dayCount;
        cataaverate = Math.round(cataaverate * 100) / 100.0f;

        float monthlyBudget = Main.instance.transactionService.getMonthlyBudget();
        float remainingBudgetMonth = monthlyBudget - monthTotal;
        remainingBudgetMonth = Math.round(remainingBudgetMonth * 100) / 100.0f;
        int remainingDaysMonth = dayCount - cal.get(Calendar.DAY_OF_MONTH);

        remainingDaysMonthTxt.setText(String.valueOf(remainingDaysMonth));
        remainingBudgetMonthTxt.setText("$" + String.valueOf(remainingBudgetMonth));
        avgSpendingMonthTxt.setText("$" + String.valueOf(monthaverage));
        remainingCatastropheTxt.setText("$" + String.valueOf(remainingCata));


        float weekTotal = Main.instance.transactionService.getWeeklyTotal(year, month, day);
        float weekaverage = weekTotal / 7.0f;
        weekaverage = Math.round(weekaverage * 100) / 100.0f;

        float weeklyBudget = Main.instance.transactionService.getWeeklyBudget(year, month, day);
        float remainingBudgetWeek = weeklyBudget - weekTotal;
        remainingBudgetWeek = Math.round(remainingBudgetWeek * 100) / 100.0f;
        int remainingDaysWeek = 7 - ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7);

        remainingDaysWeekTxt.setText(String.valueOf(remainingDaysWeek));
        remainingBudgetWeekTxt.setText("$" + String.valueOf(remainingBudgetWeek));
        avgSpendingWeekTxt.setText("$" + String.valueOf(weekaverage));
    }
}
