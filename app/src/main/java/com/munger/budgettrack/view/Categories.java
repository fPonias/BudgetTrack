package com.munger.budgettrack.view;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.model.TransactionCategory;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by codymunger on 1/23/16.
 */
public class Categories extends Fragment implements Main.SubView
{
    public static class CategoryEntryStruct
    {
        public ViewGroup rowView;

        public ImageButton deleteBtn;
        public TextView nameLabel;
        public EditText nameInput;
        public View editPanel;

        public TransactionCategory data;

        public boolean editable = false;
        public void setEditable(boolean editable)
        {
            if (editable)
            {
                editPanel.setVisibility(View.VISIBLE);
                nameLabel.setVisibility(View.GONE);
            }
            else
            {
                editPanel.setVisibility(View.GONE);
                nameLabel.setVisibility(View.VISIBLE);
            }

            this.editable = editable;

            this.rowView.invalidate();
        }

        public boolean isEdited()
        {
            String old = nameLabel.getText().toString();
            String newTxt = nameInput.getText().toString();

            if (old != newTxt.trim())
                return true;
            else
                return false;
        }
    }

    public LinearLayout categoryList;
    public boolean editable = true;
    public Menu menu;
    private ArrayList<CategoryEntryStruct> newWidgets;
    private ArrayList<CategoryEntryStruct> deletedWidgets;
    private ArrayList<CategoryEntryStruct> changedWidgets;
    public ArrayList<TransactionCategory> data;
    private HashMap<View, CategoryEntryStruct> structIndex;
    private View parentView;
    private ViewGroup container;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        this.container = container;
        parentView = inflater.inflate(R.layout.fragment_category, container, false);

        categoryList = (LinearLayout) parentView.findViewById(R.id.g_categories_list);

        reset();

        return parentView;
    }


    public void reset()
    {
        newWidgets = new ArrayList<>();
        deletedWidgets = new ArrayList<>();
        changedWidgets = new ArrayList<>();

        editable = false;

        data = new ArrayList<>();
        structIndex = new HashMap<>();

        ArrayList<TransactionCategory> cats = Main.instance.dbHelper.transactionCategories;
        int sz = cats.size();
        for (int i = 0; i < sz; i++)
        {
            TransactionCategory item = cats.get(i);
            TransactionCategory cln = new TransactionCategory();
            cln.category = item.category;
            cln.id = item.id;
            data.add(cln);
        }

        update();
    }

    public void update()
    {
        categoryList.removeAllViews();

        int sz = data.size();
        for (int i = 0; i < sz; i++)
        {
            TransactionCategory cat = data.get(i);
            View v = getStruct(cat);
            categoryList.addView(v);
        }
    }

    public View getStruct(TransactionCategory tr)
    {
        LayoutInflater inflater = (LayoutInflater) Main.instance.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final CategoryEntryStruct str = new CategoryEntryStruct();
        str.rowView = (ViewGroup) inflater.inflate(R.layout.category_item, container, false);

        str.deleteBtn = (ImageButton) str.rowView.findViewById(R.id.g_categories_listitem_deleteBtn);
        str.nameInput = (EditText) str.rowView.findViewById(R.id.g_categories_listitem_nameEdit);
        str.nameLabel = (TextView) str.rowView.findViewById(R.id.g_categories_listitem_nameLbl);
        str.editPanel = (View) str.rowView.findViewById(R.id.g_categories_listitem_editPanel);

        str.data = tr;

        str.nameLabel.setText(tr.category);
        str.nameInput.setText(tr.category);

        str.setEditable(editable);
        structIndex.put(str.rowView, str);

        return str.rowView;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInf)
    {
        menuInf.inflate(R.menu.ledger_menu, menu);
        this.menu = menu;

        MenuItem item = (MenuItem) this.menu.findItem(R.id.action_add);
        item.setVisible(editable);

        super.onCreateOptionsMenu(menu, menuInf);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_edit:
                toggleEdit();
                return true;

            case R.id.action_add:
                addSelected();

                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    public void deleteClicked(CategoryEntryStruct str)
    {
        data.remove(str.data);
        categoryList.removeView(str.rowView);
        deletedWidgets.add(str);
    }

    public void entryChanged(CategoryEntryStruct str)
    {
        changedWidgets.add(str);
    }

    public void toggleEdit()
    {
        editable = !editable;

        MenuItem item = (MenuItem) menu.findItem(R.id.action_add);
        item.setVisible(editable);

        if (data == null)
            return;

        int sz = categoryList.getChildCount();
        for (int i = 0; i < sz; i++)
        {
            View v = categoryList.getChildAt(i);
            CategoryEntryStruct str = structIndex.get(v);
            if (str != null)
                str.setEditable(editable);
        }

        if (!editable)
            Main.instance.hideKeyboard();

        categoryList.invalidate();
    }

    public void addSelected()
    {
        TransactionCategory cat = new TransactionCategory();
        data.add(cat);
        View v = getStruct(cat);
        categoryList.addView(v);
    }

    public interface ChangeResult
    {
        void result(boolean choice);
    }

    @Override
    public boolean onBack()
    {
        if (editable)
        {
            checkSave(new ChangeResult() {public void result(boolean choice)
            {
                if (choice == true)
                {
                    if (editable)
                        toggleEdit();

                    Main.instance.onBackPressed();
                }
            }});

            return false;
        }
        else
            return true;
    }

    private void checkSave(final ChangeResult callback)
    {
        if (newWidgets.size() == 0 && changedWidgets.size() == 0 && deletedWidgets.size() == 0)
        {
            callback.result(true);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(Main.instance);

        builder.setMessage("Save changes to the categories?");
        builder.setTitle("Save changes?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                saveChanges();
                callback.result(true);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                reset();
                callback.result(true);
            }
        });

        builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                callback.result(false);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveChanges()
    {
        for(CategoryEntryStruct str : newWidgets)
        {
            str.data.commit();
        }

        for(CategoryEntryStruct str : deletedWidgets)
        {
            str.data.delete();
        }

        for(CategoryEntryStruct str : changedWidgets)
        {
            str.data.commit();
        }

        Main.instance.dbHelper.reloadTransactionCategories();
    }
}
