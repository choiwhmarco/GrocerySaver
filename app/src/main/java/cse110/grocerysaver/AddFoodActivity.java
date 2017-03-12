package cse110.grocerysaver;

import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import cse110.grocerysaver.database.Favorite;
import cse110.grocerysaver.database.FridgeItem;
import cse110.grocerysaver.database.InventoryItem;
import cse110.grocerysaver.database.Persistable;
import cse110.grocerysaver.database.PersistableManager;

public class AddFoodActivity extends AppCompatActivity {

    public final static String EXTRA_FRIDGE_ITEM_ID = "EXTRA_FRIDGE_ITEM_ID";

    private PersistableManager persistableManager;
    private FridgeItem fridgeItem = new FridgeItem();
    private InventoryItem inventoryItem = new InventoryItem();

    private AutoCompleteTextView nameFld;
    private EditText expDateFld;
    private EditText notesFld;

    SimpleDateFormat format;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);

        persistableManager = new PersistableManager(this);
        ArrayList<InventoryItem> autoCompleteList = persistableManager.populateInventory(InventoryItem.class);

        nameFld = (AutoCompleteTextView) findViewById(R.id.nameField);
        expDateFld = (EditText) findViewById(R.id.expDateField);
        notesFld = (EditText) findViewById(R.id.notesField);

        format = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);

        ArrayAdapter<InventoryItem> autoCompleteAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, autoCompleteList);
        nameFld.setAdapter(autoCompleteAdapter);

        nameFld.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InventoryItem selected = (InventoryItem) parent.getAdapter().getItem(position);
                nameFld.setText(selected.getName());
                expDateFld.setText(format.format(selected.getExpirationDate().getTime()));
            }
        });

        Long id = getIntent().getLongExtra(EXTRA_FRIDGE_ITEM_ID, Persistable.NEW_RECORD);
        if (id != Persistable.NEW_RECORD) {
            setTitle("Edit fridge item");

            fridgeItem = (FridgeItem) persistableManager.findByID(FridgeItem.class, id);

            nameFld.setText(fridgeItem.getName());
            expDateFld.setText(format.format(fridgeItem.getExpirationDate().getTime()));
            notesFld.setText(fridgeItem.getNotes());
        } else {
            View buttonPanel = findViewById(R.id.buttonPanel);
            View fieldsContainer = findViewById(R.id.fieldsContainer);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) fieldsContainer.getLayoutParams();

            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            fieldsContainer.setLayoutParams(params);
            buttonPanel.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_food, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_food_done:
                if (!isDataValid()) {
                    return false;
                }

                Calendar expiration = Calendar.getInstance();
                try {
                    expiration.setTime(format.parse(expDateFld.getText().toString()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                fridgeItem.setName(nameFld.getText().toString());
                fridgeItem.setDateAdded(Calendar.getInstance());
                fridgeItem.setExpirationDate(expiration);
                fridgeItem.setNotes(notesFld.getText().toString());

                persistableManager.save(fridgeItem);

                // check if item exists in inventory table, add it if not add it
                boolean inInventory =
                        persistableManager.isFoodItemInInventoryDb(InventoryItem.class, nameFld.getText().toString());

                if (!inInventory) {
                    inventoryItem = new InventoryItem();
                    inventoryItem.setName(nameFld.getText().toString());

                    long shelfLife = expiration.getTimeInMillis() - System.currentTimeMillis();
                    inventoryItem.setShelfLife(shelfLife);

                    persistableManager.save(inventoryItem);
                }

                finish();
        }
        return false;
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void removeFridgeItem(View v) {
        Long id = getIntent().getLongExtra(EXTRA_FRIDGE_ITEM_ID, Persistable.NEW_RECORD);
        FridgeItem f = (FridgeItem) persistableManager.findByID(FridgeItem.class, id);

        persistableManager.delete(f);

        Toast.makeText(this, "Fridge item removed. " + Emoji.e(0x1f636), Toast.LENGTH_SHORT).show();

        finish();
    }

    public void addToFavorites(View v) {
        Long id = getIntent().getLongExtra(EXTRA_FRIDGE_ITEM_ID, Persistable.NEW_RECORD);
        FridgeItem fridgeItem = (FridgeItem) persistableManager.findByID(FridgeItem.class, id);
        Favorite favorite  = new Favorite();

        favorite.setName(fridgeItem.getName());
        favorite.setShelfLife(fridgeItem.getShelfLife());
        favorite.setNotes(fridgeItem.getNotes());

        persistableManager.save(favorite);

        Toast.makeText(this, "Fridge item added to Favorites. " + Emoji.e(0x1f60b), Toast.LENGTH_SHORT).show();

        finish();
    }

    private boolean isDataValid() {
        if (nameFld.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please enter food name. " + Emoji.e(0x1f613), Toast.LENGTH_SHORT).show();

            return false;
        }

        if (expDateFld.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please enter an expiration date. " + Emoji.e(0x1f613), Toast.LENGTH_SHORT).show();

            return false;
        }

        return true;
    }
}
