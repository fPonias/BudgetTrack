package com.munger.budgettrack.view;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.model.TransactionCategory;
import com.munger.budgettrack.service.TransactionService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

public class Chart extends Fragment
{
    public CombinedChart chartMonthly;
    public CombinedChart chartWeekly;
    public PieChart chartSummary;
    public PieChart chartTotalSummary;

    public Chart()
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
        //menuInf.inflate(R.menu.entry_menu, menu);

        super.onCreateOptionsMenu(menu, menuInf);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View ret = inflater.inflate(R.layout.fragment_chart, container, false);

        chartMonthly = (CombinedChart) ret.findViewById(R.id.g_chart_mainChart);
        populateMonthly();

        chartWeekly = (CombinedChart) ret.findViewById(R.id.g_chart_mainChartWeekly);
        populateWeekly();

        chartSummary = (PieChart) ret.findViewById(R.id.g_chart_spendingPie);
        populatePie(chartSummary, false);

        chartTotalSummary = (PieChart) ret.findViewById(R.id.g_chart_totalSpendingPie);
        populatePie(chartTotalSummary, true);


        return ret;
    }

    private void populateMonthly()
    {
        chartMonthly.setDescription("");
        chartMonthly.setBackgroundColor(Color.WHITE);
        chartMonthly.setDrawGridBackground(false);
        chartMonthly.setDrawBarShadow(false);

        // draw bars behind lines
        chartMonthly.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE
        });

        YAxis rightAxis = chartMonthly.getAxisRight();
        rightAxis.setDrawGridLines(false);

        YAxis leftAxis = chartMonthly.getAxisLeft();
        leftAxis.setDrawGridLines(false);

        XAxis xAxis = chartMonthly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.setTimeZone(TimeZone.getDefault());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        cal.set(year, month, 1);
        int max = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        float budget = Main.instance.transactionService.getDailyBudget(year, month);
        float total = Main.instance.transactionService.getMonthlyTotal(year, month);
        float average = total / (day + 1);

        String[] columns = new String[max];
        for (int i = 0; i < max; i++)
        {
            String d = String.valueOf(i + 1);
            columns[i] = d;
        }

        CombinedData data = new CombinedData(columns);

        LineData lineData = new LineData();


        generateGoalData(lineData, budget, max);
        generateAverageData(lineData, average, max);
        generateTrendData(lineData, cal, max);
        data.setData(lineData);
        data.setData(generateBarData(cal, max));

        chartMonthly.setData(data);
        chartMonthly.invalidate();
    }



    private void populateWeekly()
    {
        chartWeekly.setDescription("");
        chartMonthly.setBackgroundColor(Color.WHITE);
        chartMonthly.setDrawGridBackground(false);
        chartMonthly.setDrawBarShadow(false);

        // draw bars behind lines
        chartMonthly.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE
        });

        YAxis rightAxis = chartMonthly.getAxisRight();
        rightAxis.setDrawGridLines(false);

        YAxis leftAxis = chartMonthly.getAxisLeft();
        leftAxis.setDrawGridLines(false);

        XAxis xAxis = chartMonthly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.setTimeZone(TimeZone.getDefault());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int dow = TransactionService.getdow(cal);
        cal.add(Calendar.DAY_OF_MONTH, -(dow));
        int max = 7;

        float budget = Main.instance.transactionService.getWeeklyBudget(year, month, day);
        float total = Main.instance.transactionService.getWeeklyTotal(year, month, day);
        float average = total / (dow + 1);

        String[] columns = new String[max];
        for (int i = 0; i < max; i++)
        {
            String d = String.valueOf(i + 1);
            columns[i] = d;
        }

        CombinedData data = new CombinedData(columns);

        LineData lineData = new LineData();


        generateGoalData(lineData, budget / max, max);
        generateAverageData(lineData, average, max);
        generateTrendData(lineData, cal, max);
        data.setData(lineData);
        data.setData(generateBarData(cal, max));

        chartWeekly.setData(data);
        chartWeekly.invalidate();
    }

    private void generateGoalData(LineData d, float budget, int max)
    {
        ArrayList<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();

        entries.add(new com.github.mikephil.charting.data.Entry(budget, 0));
        entries.add(new com.github.mikephil.charting.data.Entry(budget, max));

        LineDataSet set = new LineDataSet(entries, "budget goal");
        set.setColor(Color.rgb(240, 70, 70));
        set.setLineWidth(2.5f);
        set.setCircleSize(0);
        set.setDrawCubic(true);
        set.setDrawValues(false);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        d.addDataSet(set);
    }

    private void generateTrendData(LineData d, Calendar cal, int max)
    {
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(System.currentTimeMillis());

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(cal.getTimeInMillis());
        ArrayList<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();

        int year, month, day;
        float trend;
        ArrayList<Transaction> vals;

        for (int i = 0; i < max; i++)
        {
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
            trend = Main.instance.transactionService.getTrend(year, month, day);

            entries.add(new com.github.mikephil.charting.data.Entry(trend, i));

            c.add(Calendar.DAY_OF_MONTH, 1);

            if (c.after(today))
                break;
        }

        LineDataSet set = new LineDataSet(entries, "trend");
        set.setColor(Color.rgb(100, 100, 100));
        set.setLineWidth(1.5f);
        set.setCircleSize(0);
        set.setDrawCubic(true);
        set.setDrawValues(false);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        d.addDataSet(set);
    }

    private void generateAverageData(LineData d, float average, int max)
    {
        ArrayList<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();

        entries.add(new com.github.mikephil.charting.data.Entry(average, 0));
        entries.add(new com.github.mikephil.charting.data.Entry(average, max));

        LineDataSet set = new LineDataSet(entries, "average expenses");
        set.setColor(Color.rgb(240, 240, 70));
        set.setLineWidth(2.5f);
        set.setCircleSize(0);
        set.setDrawCubic(true);
        set.setDrawValues(false);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        d.addDataSet(set);
    }


    private BarData generateBarData(Calendar cal, int days) {

        BarData d = new BarData();

        ArrayList<BarEntry> entries = new ArrayList<BarEntry>();

        for(int i = 0; i < days; i++)
        {
            String key = Transaction.dateToKey(cal);

            if (Main.instance.transactionService.sortedTransactions.containsKey(key))
            {
                ArrayList<Transaction> vals = Main.instance.transactionService.sortedTransactions.get(key);
                int sz = vals.size();

                //float[] list = new float[sz];
                float[] list = new float[1];

                list[0] = 0;
                for (int j = 0; j < sz; j++)
                    list[0] = list[0] + vals.get(j).amount;
                //    list[j] = vals.get(j).amount;


                entries.add(new BarEntry(list, i));
            }

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        BarDataSet set = new BarDataSet(entries, "expenditures");
        set.setColor(Color.rgb(60, 220, 78));
        set.setValueTextColor(Color.rgb(0, 60, 0));
        set.setValueTextSize(10f);
        d.addDataSet(set);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        return d;
    }

    private void populatePie(PieChart chart, boolean includeExpenses)
    {
        chart.setUsePercentValues(true);
        chart.setDescription("");

        chart.setDragDecelerationFrictionCoef(0.95f);

        chart.setDrawHoleEnabled(false);
        chart.setHoleColorTransparent(true);

        //chart.setTransparentCircleColor(Color.WHITE);
        //chart.setTransparentCircleAlpha(110);

        //chart.setHoleRadius(58f);
        //chart.setTransparentCircleRadius(61f);

        chart.setDrawCenterText(true);

        chart.setRotationAngle(0);
        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(true);

        setPieData(chart, includeExpenses);

        chart.animateY(1400, Easing.EasingOption.EaseInOutQuad);
        // chartSummary.spin(2000, 0, 360);

        Legend l = chart.getLegend();
        l.setPosition(Legend.LegendPosition.BELOW_CHART_CENTER);
        l.setXEntrySpace(10f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);
    }

    private void setPieData(PieChart chart, boolean includeExpenses)
    {
        ArrayList<com.github.mikephil.charting.data.Entry> yVals1 = new ArrayList<com.github.mikephil.charting.data.Entry>();
        ArrayList<String> xVals = new ArrayList<String>();
        HashMap<String, Float> dataSource;

        if (includeExpenses)
            dataSource = Main.instance.transactionService.totaledCategory;
        else
            dataSource = Main.instance.transactionService.totaledCategorySansCatastrophe;

        // IMPORTANT: In a PieChart, no values (Entry) should have the same
        // xIndex (even if from different DataSets), since no values can be
        // drawn above each other.
        ArrayList<TransactionCategory> transCats = Transaction.transCats;
        if (transCats == null)
            transCats = new ArrayList<>();

        int sz = transCats.size();
        int count = 0;
        for (int i = 0; i < sz; i++)
        {
            TransactionCategory cat = transCats.get(i);
            if (dataSource.containsKey(cat.category))
            {
                xVals.add(count, cat.category);

                float total = dataSource.get(cat.category);
                yVals1.add(new com.github.mikephil.charting.data.Entry(total, count));

                count++;
            }
        }

        PieDataSet dataSet = new PieDataSet(yVals1, "");
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);

        // add a lot of colors

        ArrayList<Integer> colors = new ArrayList<Integer>();

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());

        dataSet.setColors(colors);

        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(8f);
        data.setValueTextColor(Color.BLACK);
        chart.setData(data);

        // undo all highlights
        chart.highlightValues(null);

        chart.invalidate();
    }
}
