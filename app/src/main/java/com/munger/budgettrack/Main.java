package com.munger.budgettrack;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.munger.budgettrack.model.DatabaseHelper;
import com.munger.budgettrack.model.Settings;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.service.TransactionService;
import com.munger.budgettrack.view.Chart;
import com.munger.budgettrack.view.Entry;
import com.munger.budgettrack.view.Ledger;
import com.munger.budgettrack.view.Overview;

/**
 * Created by codymunger on 12/22/15.
 */
public class Main extends AppCompatActivity
{
    protected FrameLayout root;
    public ActionBar actionBar;
    protected String currentFrag;

    public DatabaseHelper dbHelper;
    public Settings settings;
    public TransactionService transactionService;

    public static Main instance;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        instance = this;

        super.onCreate(savedInstanceState);

        root = new FrameLayout(this);
        root.setId(R.id.appId);
        setContentView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        actionBar = getSupportActionBar();


        if (savedInstanceState != null)
        {
            dbHelper = new DatabaseHelper(this, savedInstanceState.getBundle("dbHelper"));
            settings = new Settings();
            transactionService = new TransactionService(savedInstanceState.getBundle("transactionService"));

            currentFrag = savedInstanceState.getString("currentFragment");

            FragmentManager fm = getFragmentManager();

            if (fm.getBackStackEntryCount() > 1)
                actionBar.setDisplayHomeAsUpEnabled(true);

        }
        else
        {
            dbHelper = new DatabaseHelper(this);
            settings = new Settings();
            transactionService = new TransactionService();
        }


        if (savedInstanceState == null)
            loadOverview();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelable("dbHelper", dbHelper.getState());
        savedInstanceState.putParcelable("transactionService", transactionService.getState());
        savedInstanceState.putString("currentFragment", currentFrag);
    }

    public void loadOverview()
    {
        loadView(new Overview(), "overview");
    }

    public void loadEntry(Transaction tr)
    {
        Bundle b = new Bundle();
        b.putParcelable("data", tr);

        Fragment frag = new Entry();
        frag.setArguments(b);
        loadView(frag, "entry");
    }

    public void loadEntry()
    {
        loadView(new Entry(), "entry");
    }

    public void loadSettings()
    {
        loadView(new com.munger.budgettrack.view.Settings(), "settings");
    }

    public void loadLedger()
    {
        loadView(new Ledger(), "ledger");
    }

    public void loadChart()
    {
        loadView(new Chart(), "chart");
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        goBack();

        return true;
    }

    public void goBack()
    {
        FragmentManager fm = getFragmentManager();
        int sz = fm.getBackStackEntryCount();

        if (sz == 1)
            return;

        FragmentManager.BackStackEntry bse = fm.getBackStackEntryAt(sz - 2);
        String id = bse.getName();
        fm.popBackStack();
        sz--;

        currentFrag = id;

        if (sz == 1)
            actionBar.setDisplayHomeAsUpEnabled(false);
    }

    private void loadView(Fragment frag, String id)
    {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        if (fm.getBackStackEntryCount() > 0)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
            Fragment fragment = fm.findFragmentByTag(currentFrag);

            if (fragment != null)
                ft.remove(fragment);
        }

        currentFrag = id;
        ft.add(R.id.appId, frag, id);
        ft.addToBackStack(id);
        ft.commit();
    }

}
