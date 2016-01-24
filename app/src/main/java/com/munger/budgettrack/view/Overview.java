package com.munger.budgettrack.view;

import android.app.Fragment;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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

import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.TimeZone;

public class Overview extends Fragment
{
    public TextView remainingDaysWeekTxt;
    public TextView totalDaysWeekTxt;
    public TextView remainingBudgetWeekTxt;
    public TextView avgSpendingWeekTxt;
    public TextView totalBudgetWeekTxt;
    public TextView totalAvgWeekTxt;

    public TextView remainingDaysMonthTxt;
    public TextView totalDaysMonthTxt;
    public TextView remainingBudgetMonthTxt;
    public TextView avgSpendingMonthTxt;
    public TextView totalBudgetMonthTxt;
    public TextView totalAvgMonthTxt;

    public TextView trendTxt;
    public TextView trendDeltaTxt;
    public TextView projectedSurplusTxt;

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
        totalDaysWeekTxt = (TextView) ret.findViewById(R.id.g_overview_remainingDaysWeekTotalTxt);
        remainingBudgetWeekTxt = (TextView) ret.findViewById(R.id.g_overview_remainingBudgetWeekTxt);
        avgSpendingWeekTxt = (TextView) ret.findViewById(R.id.g_overview_averageWeekTxt);
        totalBudgetWeekTxt = (TextView) ret.findViewById(R.id.g_overview_maxBudgetWeek);
        totalAvgWeekTxt = (TextView) ret.findViewById(R.id.g_overview_goalAverageWeek);

        remainingDaysMonthTxt = (TextView) ret.findViewById(R.id.g_overview_remainingDaysMonthTxt);
        totalDaysMonthTxt = (TextView) ret.findViewById(R.id.g_overview_remainingDaysMonthTotalTxt);
        remainingBudgetMonthTxt = (TextView) ret.findViewById(R.id.g_overview_remainingBudgetMonthTxt);
        avgSpendingMonthTxt = (TextView) ret.findViewById(R.id.g_overview_averageMonthTxt);
        totalBudgetMonthTxt = (TextView) ret.findViewById(R.id.g_overview_maxBudgetMonth);
        totalAvgMonthTxt = (TextView) ret.findViewById(R.id.g_overview_goalAverageMonth);

        trendTxt = (TextView) ret.findViewById(R.id.g_overview_trendTxt);
        trendDeltaTxt = (TextView) ret.findViewById(R.id.g_overview_trendChangeTxt);
        projectedSurplusTxt = (TextView) ret.findViewById(R.id.g_overview_projectedSurplus);

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

            case R.id.action_income:
                Main.instance.loadIncome();
                return true;

            case R.id.action_expenditures:
                Main.instance.loadExpenditures();
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

    private String getFormattedAmount(float amount)
    {
        int hundo = Math.round(amount * 100);
        float val = hundo / 100.0f;

        int extraZeros = 0;
        if (Math.abs(hundo) % 10 == 0)
            extraZeros++;

        StringBuilder builder = new StringBuilder();
        builder.append('$');
        builder.append(val);

        for (int i = 0; i < extraZeros; i++)
            builder.append('0');

        return builder.toString();
    }

    private void setColor(TextView view, float warnValue, float minValue, float actualValue, float maxValue, int warnColor, int negColor, int defaultColor)
    {
        if (actualValue < minValue || actualValue > maxValue)
            view.setTextColor(negColor);
        else if (actualValue <= warnValue && actualValue >= minValue)
            view.setTextColor(warnColor);
        else
            view.setTextColor(defaultColor);
    }

    public void update()
    {
        int defaultTextColor = remainingDaysMonthTxt.getCurrentTextColor();
        int warningTextColor = ContextCompat.getColor(Main.instance, R.color.colorWarning);
        int negativeTextColor = ContextCompat.getColor(Main.instance, R.color.colorNegative);
        com.munger.budgettrack.model.Settings settings = Main.instance.settings;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.setTimeZone(TimeZone.getDefault());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        Main.instance.transactionService.loadTransactions(year, month);
        Main.instance.cashFlowService.loadData();

        int dayCount = cal.getMaximum(Calendar.DAY_OF_MONTH);
        float monthTotal = Main.instance.transactionService.getMonthlyTotal(year, month);
        float monthaverage = monthTotal / (day + 1.0f);

        float cataTotal = Main.instance.transactionService.getCatastropheTotal(year, month);
        float remainingCata = Main.instance.settings.emergencyFund - cataTotal;
        float cataaverate = cataTotal / dayCount;


        float monthlyBudget = Main.instance.transactionService.getMonthlyBudget();
        float monthlyBudgetGoal = (float) (monthlyBudget / (double) dayCount);
        float remainingBudgetMonth = monthlyBudget - monthTotal;

        float trend = Main.instance.transactionService.getTrend(year, month, day);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        float yesterTrend = Main.instance.transactionService.getTrend(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        float trendDelta = trend - yesterTrend;
        trendTxt.setText(getFormattedAmount(trend));
        String delta = (trendDelta >= 0) ? "+" : "";
        delta += getFormattedAmount(trendDelta).substring(1);
        trendDeltaTxt.setText(delta);

        float trendGuess = (yesterTrend + monthaverage) * 0.5f;
        float surplusRemainder = trendGuess * (dayCount - day);
        float surplus = remainingBudgetMonth - surplusRemainder;
        if (remainingCata < 0)
            surplus += remainingCata;


        remainingDaysMonthTxt.setText(String.valueOf(day) + "/");
        totalDaysMonthTxt.setText(String.valueOf(dayCount));
        remainingBudgetMonthTxt.setText(getFormattedAmount(remainingBudgetMonth));
        setColor(remainingBudgetMonthTxt, 0, 0, remainingBudgetMonth, Float.MAX_VALUE, warningTextColor, negativeTextColor, defaultTextColor);
        avgSpendingMonthTxt.setText(getFormattedAmount(monthaverage));
        setColor(avgSpendingMonthTxt, 0, 0, monthaverage, monthlyBudgetGoal, warningTextColor, negativeTextColor, defaultTextColor);
        totalBudgetMonthTxt.setText(getFormattedAmount(monthlyBudget));
        totalAvgMonthTxt.setText(getFormattedAmount(monthlyBudgetGoal));
        projectedSurplusTxt.setText(getFormattedAmount(surplus));
        setColor(projectedSurplusTxt, 0, 0, surplus, Float.MAX_VALUE, warningTextColor, negativeTextColor, defaultTextColor);

        remainingCatastropheTxt.setText(getFormattedAmount(remainingCata));
        setColor(remainingCatastropheTxt, 0, 0, remainingCata, Float.MAX_VALUE, warningTextColor, negativeTextColor, defaultTextColor);


        float weekTotal = Main.instance.transactionService.getWeeklyTotal(year, month, day);
        int dow = TransactionService.getdow(cal) + 1;
        float weekaverage = weekTotal / (dow);

        float weeklyBudget = Main.instance.transactionService.getWeeklyBudget(year, month, day);
        float weeklyAverageGoal = weeklyBudget / 7.0f;
        float remainingBudgetWeek = weeklyBudget - weekTotal;

        remainingDaysWeekTxt.setText(String.valueOf(dow) + "/");
        remainingBudgetWeekTxt.setText(getFormattedAmount(remainingBudgetWeek));
        setColor(remainingBudgetWeekTxt, 0, 0, remainingBudgetWeek, Float.MAX_VALUE, warningTextColor, negativeTextColor, defaultTextColor);
        avgSpendingWeekTxt.setText(getFormattedAmount(weekaverage));
        setColor(avgSpendingWeekTxt, 0, 0, weekaverage, weeklyAverageGoal, warningTextColor, negativeTextColor, defaultTextColor);
        totalBudgetWeekTxt.setText(getFormattedAmount(weeklyBudget));
        totalAvgWeekTxt.setText(getFormattedAmount(weeklyAverageGoal));
    }
}
