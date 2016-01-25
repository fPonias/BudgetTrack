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
        public View rowView;

        public ImageButton deleteBtn;
        public TextView nameLabel;
        public EditText nameInput;

        public TransactionCategory data;

        public boolean editable = true;
        public void setEditable(boolean editable)
        {
            if (editable == this.editable)
                return;

            if (editable)
            {
                deleteBtn.setVisibility(View.VISIBLE);
                nameLabel.setVisibility(View.GONE);
                nameInput.setVisibility(View.VISIBLE);
            }
            else
            {
                deleteBtn.setVisibility(View.GONE);
                nameLabel.setVisibility(View.VISIBLE);
                nameInput.setVisibility(View.GONE);
            }
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

        public CategoryAdapter(Categories parent, ArrayList<TransactionCategory> data)
        {
            super(Main.instance, R.layout.income_item, data);
            this.data = data;
            this.parent = parent;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater = (LayoutInflater) Main.instance.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final CategoryEntryStruct str = new CategoryEntryStruct();
            str.rowView = inflater.inflate(R.layout.category_item, parent, false);

            str.deleteBtn = (ImageButton) str.rowView.findViewById(R.id.g_categories_listitem_deleteBtn);
            str.nameInput = (EditText) str.rowView.findViewById(R.id.g_categories_listitem_nameEdit);
            str.nameLabel = (TextView) str.rowView.findViewById(R.id.g_categories_listitem_nameLbl);

            TransactionCategory tr = data.get(position);
            str.data = tr;

            str.nameLabel.setText(tr.category);
            str.nameInput.setText(tr.category);


            str.setEditable(this.parent.editable);


            final CategoryAdapter that = this;
            str.deleteBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
            {
                that.parent.deleteClicked(str);
            }});


            this.parent.indexView(tr, str);

            return str.rowView;
        }
    }

    public ListView categoryList;
    public boolean editable = true;
    public Menu menu;
    private HashMap<Long, CategoryEntryStruct> widgetIndex = new HashMap<>();
    private ArrayList<CategoryEntryStruct> newWidgets = new ArrayList<>();
    private ArrayList<CategoryEntryStruct> deletedWidgets = new ArrayList<>();
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

        ArrayList<TransactionCategory> cats = Main.instance.dbHelper.transactionCategories;
        for (TransactionCategory cat : cats)
        {
            TransactionCategory clone = new TransactionCategory();
            clone.id = cat.id;
            clone.category = cat.category;
            data.add(clone);
        }

        CategoryAdapter adapter = new CategoryAdapter(this, data);
        categoryList.setAdapter(adapter);

        return ret;
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
        if (str.data.id > -1)
        {
            final TransactionCategory tr = str.data;
            AlertDialog.Builder builder = new AlertDialog.Builder(Main.instance);

            builder.setMessage(getDeleteMessage(tr));
            builder.setTitle(getDeleteTitle());

            builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Main.instance.cashFlowService.deleteCashFlow(tr);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        data.remove(str.data);
        categoryList.invalidate();


    }


    public void indexView(TransactionCategory tr, CategoryEntryStruct str)
    {
        widgetIndex.put(tr.id, str);
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

        categoryList.invalidate();
    }

    @Override
    public boolean onBack()
    {
        if (editable)
            return checkSave();
        else
            return true;
    }

    private boolean checkSave()
    {
        return true;
    }
}
