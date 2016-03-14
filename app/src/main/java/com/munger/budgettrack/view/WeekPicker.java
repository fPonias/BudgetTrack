package com.munger.budgettrack.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.service.TransactionService;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by codymunger on 3/2/16.
 */
public class WeekPicker extends LinearLayout
{
    public Button monthBackBtn;
    public Button weekBackBtn;
    public TextView weekLbl;
    public Button monthSkipBtn;
    public Button weekSkipBtn;
    public TextView dateStartLbl;
    public TextView dateEndLbl;

    public WeekPicker(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WeekPicker,
                0, 0);


        inflate(getContext(), R.layout.week_picker, this);
        monthBackBtn = (Button) findViewById(R.id.g_overview_skipback_month_btn);
        weekBackBtn = (Button) findViewById(R.id.g_overview_skipback_week_btn);
        weekLbl = (TextView) findViewById(R.id.g_overview_skip_dateWeeklbl);
        weekSkipBtn = (Button) findViewById(R.id.g_overview_skip_week_btn);
        monthSkipBtn = (Button) findViewById(R.id.g_overview_skip_month_btn);
        dateStartLbl = (TextView) findViewById(R.id.g_overview_startDateLbl);
        dateEndLbl = (TextView) findViewById(R.id.g_overview_endDateLbl);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        setDate(cal);

        monthBackBtn.setOnClickListener(new OnClickListener() {public void onClick(View v)
        {
            currentDateMonth.add(Calendar.MONTH, -1);
            setDate(currentDateMonth);
        }});

        weekBackBtn.setOnClickListener(new OnClickListener() {public void onClick(View v)
        {
            currentDateWeek.add(Calendar.WEEK_OF_YEAR, -1);
            setDate(currentDateWeek);
        }});

        weekSkipBtn.setOnClickListener(new OnClickListener() {public void onClick(View v)
        {
            currentDateWeek.add(Calendar.WEEK_OF_YEAR, 1);
            setDate(currentDateWeek);
        }});

        monthSkipBtn.setOnClickListener(new OnClickListener() {public void onClick(View v)
        {
            currentDateMonth.add(Calendar.MONTH, 1);
            setDate(currentDateMonth);
        }});
    }

    private Calendar currentDateWeek;
    private Calendar currentDateMonth;

    public void setDate(Calendar cal)
    {
        currentDateMonth = Calendar.getInstance();
        currentDateMonth.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;


        int dowStart = TransactionService.getdow(currentDateMonth) - 1;
        int negativeWeekEnd = 0;

        if (dowStart > 0)
            negativeWeekEnd = 7 - dowStart;

        int dow = TransactionService.getdow(cal);
        cal.add(Calendar.DAY_OF_WEEK, -(dow - 1));
        int day = cal.get(Calendar.DAY_OF_MONTH);
        currentDateWeek = Calendar.getInstance();
        currentDateWeek.setTimeInMillis(cal.getTimeInMillis());

        int week = (day - negativeWeekEnd) / 7 + 1;

        weekLbl.setText("Week " + week);
        dateStartLbl.setText(month + "/" + day + "/" + year);


        cal.add(Calendar.DAY_OF_MONTH, 6);
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH) + 1;
        day = cal.get(Calendar.DAY_OF_MONTH);

        dateEndLbl.setText(month + "/" + day + "/" + year);


        notifyListeners();
    }

    public Calendar getDateWeek()
    {
        return currentDateWeek;
    }

    public Calendar getDateMonth()
    {
        return currentDateMonth;
    }

    public interface ChangeListener
    {
        void onDateChange(WeekPicker picker);
    }

    private ArrayList<ChangeListener> listeners = new ArrayList<ChangeListener>();

    public void addListener(ChangeListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners()
    {
        for(ChangeListener listener : listeners)
            listener.onDateChange(this);
    }
}
