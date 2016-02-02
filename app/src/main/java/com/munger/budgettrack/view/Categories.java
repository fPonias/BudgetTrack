package com.munger.budgettrack.view;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.munger.budgettrack.Main;
import com.munger.budgettrack.R;
import com.munger.budgettrack.model.CashFlow;
import com.munger.budgettrack.model.Transaction;
import com.munger.budgettrack.model.TransactionCategory;

import java.lang.reflect.Array;
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
            editPanel.setVisibility(View.GONE);
            nameLabel.setVisibility(View.GONE);

            if (editable)
            {
                editPanel.setVisibility(View.VISIBLE);
            }
            else
            {
                nameLabel.setVisibility(View.VISIBLE);
            }

            this.editable = editable;
            rowView.invalidate();
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

    public static class CategoryAdapter extends ArrayAdapter<TransactionCategory>
    {
        public Categories parent;
        public ArrayList<TransactionCategory> data;
        public HashMap<TransactionCategory, CategoryEntryStruct> structIndex;

        public CategoryAdapter(Categories parent, ArrayList<TransactionCategory> data)
        {
            super(Main.instance, R.layout.income_item, data);
            this.data = data;
            this.parent = parent;
            structIndex = new HashMap<>();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            TransactionCategory tr = data.get(position);
            if (structIndex.containsKey(tr))
            {
                CategoryEntryStruct str = structIndex.get(tr);
                return str.rowView;
            }

            LayoutInflater inflater = (LayoutInflater) Main.instance.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final CategoryEntryStruct str = new CategoryEntryStruct();
            str.rowView = (ViewGroup) inflater.inflate(R.layout.category_item, parent, false);

            str.deleteBtn = (ImageButton) str.rowView.findViewById(R.id.g_categories_listitem_deleteBtn);
            str.nameInput = (EditText) str.rowView.findViewById(R.id.g_categories_listitem_nameEdit);
            str.nameLabel = (TextView) str.rowView.findViewById(R.id.g_categories_listitem_nameLbl);
            str.editPanel = (View) str.rowView.findViewById(R.id.g_categories_listitem_editPanel);

            str.rowView.removeViewAt(1);

            str.data = tr;

            str.nameLabel.setText(tr.category);
            str.nameInput.setText(tr.category);

            str.setEditable(this.parent.editable);

            structIndex.put(tr, str);
            this.parent.indexView(tr, str);


            if (this.parent.editable && tr.category.isEmpty())
            {
                InputMethodManager imm = (InputMethodManager) Main.instance.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(str.nameInput, InputMethodManager.SHOW_IMPLICIT);
            }


            return str.rowView;
        }
    }

    public ListView categoryList;
    private CategoryAdapter listAdapter;
    public boolean editable = true;
    public Menu menu;
    private HashMap<Long, CategoryEntryStruct> widgetIndex;
    private ArrayList<CategoryEntryStruct> newWidgets;
    private ArrayList<CategoryEntryStruct> deletedWidgets;
    private ArrayList<CategoryEntryStruct> changedWidgets;
    public ArrayList<TransactionCategory> data;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View ret = inflater.inflate(R.layout.fragment_category, container, false);

        categoryList = (ListView) ret.findViewById(R.id.g_categories_list);

        reset();

        return ret;
    }

    public void reset()
    {
        newWidgets = new ArrayList<>();
        deletedWidgets = new ArrayList<>();
        changedWidgets = new ArrayList<>();
        widgetIndex = new HashMap<>();

        editable = false;

        data = new ArrayList<>();

        ArrayList<TransactionCategory> cats = Main.instance.dbHelper.transactionCategories;
        for (TransactionCategory cat : cats)
        {
            TransactionCategory clone = new TransactionCategory();
            clone.id = cat.id;
            clone.category = cat.category;
            data.add(clone);
        }

        listAdapter = new CategoryAdapter(this, data);

        categoryList.setAdapter(listAdapter);
        categoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {

        }});
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
        deletedWidgets.add(str);
        listAdapter.notifyDataSetChanged();
    }

    public void entryChanged(CategoryEntryStruct str)
    {
        changedWidgets.add(str);
    }


    public void indexView(TransactionCategory tr, CategoryEntryStruct str)
    {
        if (tr.id > -1)
            widgetIndex.put(tr.id, str);
        else
            newWidgets.add(str);
    }

    public void toggleEdit()
    {
        editable = !editable;

        MenuItem item = (MenuItem) menu.findItem(R.id.action_add);
        item.setVisible(editable);

        if (data == null)
            return;

        for (TransactionCategory tr : data)
        {
            CategoryEntryStruct view = widgetIndex.get(tr.id);
            view.setEditable(editable);
        }

        categoryList.invalidate();
    }

    public void addSelected()
    {
        TransactionCategory cat = new TransactionCategory();
        data.add(cat);

        listAdapter.notifyDataSetChanged();
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
