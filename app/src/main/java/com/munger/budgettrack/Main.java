package com.munger.budgettrack;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.DatabaseHelper;
import com.munger.budgettrack.model.Settings;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.service.CashFlowService;
import com.munger.budgettrack.service.RemoteStorageService;
import com.munger.budgettrack.service.TransactionService;
import com.munger.budgettrack.view.Categories;
import com.munger.budgettrack.view.Chart;
import com.munger.budgettrack.view.Entry;
import com.munger.budgettrack.view.CashFlowBase;
import com.munger.budgettrack.view.ExpenditureEntry;
import com.munger.budgettrack.view.Expenditures;
import com.munger.budgettrack.view.Income;
import com.munger.budgettrack.view.IncomeEntry;
import com.munger.budgettrack.view.Ledger;
import com.munger.budgettrack.view.Overview;

/**
 * Created by codymunger on 12/22/15.
 */
public class Main extends AppCompatActivity
{
    public interface SubView
    {
        public boolean onBack();
    }

    protected FrameLayout root;
    public ActionBar actionBar;
    protected String currentFrag;

    public DatabaseHelper dbHelper;
    public Settings settings;
    public TransactionService transactionService;
    public CashFlowService cashFlowService;
    public RemoteStorageService remoteStorageService;

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
            cashFlowService = new CashFlowService(savedInstanceState.getBundle("cashFlowService"));
            remoteStorageService = new RemoteStorageService(this);

            currentFrag = savedInstanceState.getString("currentFragment");

            FragmentManager fm = getFragmentManager();

            if (fm.getBackStackEntryCount() > 1)
                actionBar.setDisplayHomeAsUpEnabled(true);

        }
        else
        {
            dbHelper = new DatabaseHelper(this);
            dbHelper.loadTransactionCategories();
            dbHelper.loadChangeLog();
            //dbHelper.nuke();
            settings = new Settings();
            transactionService = new TransactionService();
            cashFlowService = new CashFlowService();
            remoteStorageService = new RemoteStorageService(this);
        }


        if (savedInstanceState == null)
            loadOverview();
    }

    @Override
    public void onStart()
    {
        remoteStorageService.connect();
        super.onStart();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putParcelable("dbHelper", dbHelper.getState());
        savedInstanceState.putParcelable("transactionService", transactionService.getState());
        savedInstanceState.putParcelable("cashFlowService", cashFlowService.getState());
        savedInstanceState.putString("currentFragment", currentFrag);
    }

    public final static int RESOLVE_CONNECTION_REQUEST_CODE = 12;

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
    {
        switch (requestCode)
        {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK)
                {
                    remoteStorageService.connect();
                }
                break;
        }
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

    public void loadIncomeEntry() { loadView(new IncomeEntry(), "incomeEntry"); }

    public void loadIncomeEntry(CashFlow tr)
    {
        Bundle b = new Bundle();
        b.putParcelable("data", tr);

        Fragment frag = new IncomeEntry();
        frag.setArguments(b);
        loadView(frag, "incomeEntry");
    }

    public void loadExpenditureEntry() { loadView(new ExpenditureEntry(), "expenditureEntry"); }

    public void loadExpenditureEntry(CashFlow tr)
    {
        Bundle b = new Bundle();
        b.putParcelable("data", tr);

        Fragment frag = new ExpenditureEntry();
        frag.setArguments(b);
        loadView(frag, "expenditureEntry");
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

    public void loadIncome() { loadView(new Income(), "income"); }

    public void loadExpenditures() { loadView(new Expenditures(), "expenditures");}

    public void loadCategories() { loadView(new Categories(), "categories");}

    @Override
    public boolean onSupportNavigateUp()
    {
        goBack();

        return true;
    }

    @Override
    public void onBackPressed()
    {
        boolean value = goBack();

        if (value == false)
            super.onBackPressed();
    }

    public boolean goBack()
    {
        FragmentManager fm = getFragmentManager();
        int sz = fm.getBackStackEntryCount();

        if (sz == 1)
            return false;

        FragmentManager.BackStackEntry bse = fm.getBackStackEntryAt(sz - 2);
        String id = bse.getName();
        fm.popBackStack();
        sz--;

        currentFrag = id;

        if (sz == 1)
            actionBar.setDisplayHomeAsUpEnabled(false);

        if (reloadDepth > 0)
        {
            reloadDepth--;
        }

        return true;
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


        View view = this.getCurrentFocus();
        if (view != null)
        {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public int reloadDepth = 0;

    public void reloadView()
    {
        FragmentManager fm = getFragmentManager();
        int sz = fm.getBackStackEntryCount();

        reloadDepth = sz;
    }
}
