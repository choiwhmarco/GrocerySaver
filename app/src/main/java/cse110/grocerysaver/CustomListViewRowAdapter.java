package cse110.grocerysaver;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

class CustomListViewRowAdapter extends ArrayAdapter<FoodItem> {

    //ViewHolder holder;

    public CustomListViewRowAdapter(Context context, List<FoodItem> foodItems) {
        //super(context,R.layout.custom_list_view_row, foodItems);
        super(context,R.layout.custom_listview_layout, foodItems);
    }
    @NonNull
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        View v = convertView;

            LayoutInflater myInflater = LayoutInflater.from(getContext());
            v = myInflater.inflate(R.layout.custom_listview_layout, null);

        FoodItem foodItem = getItem(position);

        TextView customRowNameText = (TextView) v.findViewById(R.id.foodItemName);
        customRowNameText.setText(foodItem.getFoodName());

        ImageView customRowImage = (ImageView) v.findViewById(R.id.foodImage);



        final CheckBox check = (CheckBox) v.findViewById(R.id.selected);

        // set the selected food item to checked when
        check.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ListView lv = (ListView) parent;
                if (lv != null) {
                    lv.setItemChecked(position, !lv.isItemChecked(position));
                }
            }
        });

        return v;
    }

}