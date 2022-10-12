package com.odoo.addons.sale;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.stock.ProductCategory;
import com.odoo.addons.stock.ProductProduct;
import com.odoo.addons.stock.StockWarehouse;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.support.list.OListAdapter;
import com.odoo.core.utils.BitmapUtils;
import com.odoo.core.utils.OAlert;
import com.odoo.core.utils.OControls;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import odoo.controls.IOnQuickRecordCreateListener;

public class AddProductLineWizard extends ActionBarActivity implements
        AdapterView.OnItemClickListener, TextWatcher, View.OnClickListener,
        OListAdapter.OnSearchChange, IOnQuickRecordCreateListener, AdapterView.OnItemLongClickListener  {

    private static final int selected_position = -1;
    private ProductProduct pp;
    private EditText edt_searchable_input;
    private ListView mList = null;
    private OListAdapter mAdapter;
    private List<Object> objects = new ArrayList<>();
    private List<Object> localItems = new ArrayList<>();
    private HashMap<String, Float> lineValues = new HashMap<>();
    private Boolean mLongClicked = false;
    private int partnerId;
    private int productCategoryId;

    private Spinner spinnerCategory;
    private Bundle extra;
    private ProductCategory pc;
    final List<String> items = new ArrayList<>();
    private int previous_order_id = 0;
    private String previous_order_name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sale_add_item);
        setResult(RESULT_CANCELED);
//        app = (App) getApplicationContext();
        pp = new ProductProduct(this, null);
        spinnerCategory= (Spinner) findViewById(R.id.spinner_product_category);
        edt_searchable_input = (EditText) findViewById(R.id.edt_searchable_input);
        edt_searchable_input.addTextChangedListener(this);
        findViewById(R.id.done).setOnClickListener(this);
        extra = getIntent().getExtras();
        mList = (ListView) findViewById(R.id.searchable_items);
        mList.setOnItemClickListener(this);
        mList.setOnItemLongClickListener(this);


        extra.remove("warehouse_id");
        extra.remove("pricelist_id");
        partnerId = (int) extra.get("partner_id");
        extra.remove("partner_id");

        pc = new ProductCategory(getApplicationContext(), null);
        items.add("Барааны ангилал");
        for (ODataRow oDataRow : pc.query("SELECT id, name FROM product_category")){
            items.add(oDataRow.getString("name"));
        }

        JSONArray jsonArray1 = new JSONArray();
        for (ODataRow product : pp.query("SELECT id FROM product_product")) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", product.get("id"));
                jsonArray1.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, items){
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.WHITE);
                return tv;
            }
        };
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == 0) {
                    productCategoryId = 0;
                }
                else{
                    List<ODataRow> categoryList = pc.query("SELECT _id, name FROM product_category WHERE name = ?", new String[]{items.get(position)});
                    productCategoryId = categoryList.size() > 0 ? categoryList.get(0).getInt("_id") : 0;
                }
                fillExpandableList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });

        fillPreviousOrder();
    }

    private void fillPreviousOrder(){
        SaleOrder so = new SaleOrder(getApplicationContext(), null);
        SaleOrderLine sol = new SaleOrderLine(getApplicationContext(), null);
        Button fillButton =(Button)findViewById(R.id.button_call_order);
        for(ODataRow oDataRow : so.query("SELECT _id, name FROM sale_order where state = 'sale' AND partner_id = ? " +
                "ORDER BY id DESC LIMIT 1", new String[]{String.valueOf((partnerId))})) {
            previous_order_id = oDataRow.getInt("_id");
            previous_order_name = oDataRow.getString("name");
            fillButton.setText(previous_order_name + String.valueOf(" захиалгыг хуулах"));
            fillButton.setVisibility(View.VISIBLE);
            fillButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    spinnerCategory.setSelection(0);
                    lineValues.clear();
                    for(ODataRow oDataRow1 : sol.query("SELECT pp.id AS product_id, sol.product_uom_qty " +
                            "FROM sale_order_line sol " +
                            "LEFT JOIN product_product pp ON pp._id = sol.product_id " +
                            "WHERE sol.order_id = ?", new String[]{String.valueOf((previous_order_id))}))
                    {
                        lineValues.put(oDataRow1.getString("product_id"), oDataRow1.getFloat("product_uom_qty"));
                    }
                    mAdapter.notifiyDataChange(objects);
                }
            });
        }
    }

    public void fillExpandableList(){
        if (extra != null) {
            for (String key : extra.keySet()) {
                lineValues.put(key, extra.getFloat(key));
            }
            localItems.clear();
            String query = "SELECT pp.id, pp.name, pp.default_code, pp.list_price " +
                    "FROM product_product pp ";
            if(productCategoryId > 0){
                query += "WHERE pp.categ_id = " + productCategoryId + " ";
            }
            query += "ORDER BY pp.default_code";

            for (ODataRow product : pp.query(query)) {
                if (lineValues.containsKey(product.getString("id") + "")) {
                    localItems.add(0, product);
                } else {
                    localItems.add(product);
                }
            }
            Log.d("HEHE", "fillExpandableList: " + localItems.size());
            objects.clear();
            objects.addAll(localItems);
            mAdapter = new OListAdapter(this, R.layout.sale_product_line_item, objects) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = LayoutInflater.from(AddProductLineWizard.this).inflate(R.layout.sale_product_line_item
                                , parent, false);
                    }
                    return generateView(getItem(position), position, convertView, parent);
                }
            };
            mList.setAdapter(mAdapter);
        } else {
            finish();
        }
    }

    private View generateView(final Object row, int pos, View view, ViewGroup parent) {
        final ODataRow r = (ODataRow) row;
        Float qty = (lineValues.containsKey(r.getString("id")) &&
                lineValues.get(r.getString("id")) > 0) ? lineValues.get(r.getString("id")) : 0;
        Float list_price = r.getFloat("list_price");

        if (qty <= 0) {
            OControls.setGone(view, R.id.multiply);
            OControls.setGone(view, R.id.productQty);
            OControls.setGone(view, R.id.result);
            OControls.setGone(view, R.id.remove_qty);

        } else {

            view.findViewById(R.id.remove_qty).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Float lineQty = lineValues.get(r.getString("id"));
                    lineValues.put(r.getString("id"), lineQty - 1);
                    mAdapter.notifiyDataChange(objects);
                }
            });

            OControls.setVisible(view, R.id.multiply);
            OControls.setVisible(view, R.id.productQty);
            OControls.setVisible(view, R.id.result);
            OControls.setVisible(view, R.id.remove_qty);
            OControls.setText(view, R.id.productQty, qty + " ");
            OControls.setText(view, R.id.result, " = " + String.format("%.1f",list_price*qty) + '₮');
        }

        OControls.setText(view, R.id.productName,(r.getString("default_code").equals("false") ? "": ("[" + r.getString("default_code")+ "] ")) + r.getString("name"));
        OControls.setText(view, R.id.info, String.format("%.1f", list_price) + '₮');




        if (r.contains(OColumn.ROW_ID)
                && selected_position == r.getInt(OColumn.ROW_ID)) {
            view.setBackgroundColor(getResources().getColor(
                    R.color.control_pressed));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
        return view;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {


        if(s.length() > 0) {
            if(s.length() % 2 == 0)
                mAdapter.getFilter().filter(s);
        }
        else {
            objects.clear();
            objects.addAll(localItems);
            mAdapter.notifiyDataChange(objects);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.done) {
            back();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        back();
        super.onBackPressed();
    }

    private void back(){
        Bundle data = new Bundle();
        for (String key : lineValues.keySet()) {
            data.putFloat(key, lineValues.get(key));
        }
        Intent intent = new Intent();
        intent.putExtras(data);
        setResult(RESULT_OK, intent);
        finish();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ODataRow data = (ODataRow) objects.get(position);
        int row_id = pp.selectRowId(data.getInt("id"));

        if (row_id != -1) {
            data.put(OColumn.ROW_ID, row_id);
        }
        onRecordCreated(data);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ODataRow data = (ODataRow) objects.get(position);
        mLongClicked = true;
        int row_id = pp.selectRowId(data.getInt("id"));
        if (row_id != -1) {
            data.put(OColumn.ROW_ID, row_id);
        }
        onLongClicked(data);
        return true;
    }

    @Override
    public void onRecordCreated(ODataRow row) {
        if (!mLongClicked) {
            Float count = ((lineValues.containsKey(row.getString("id"))) ? lineValues.get(row.getString("id")) : 0);
            lineValues.put(row.getString("id"), ++count);
            mAdapter.notifiyDataChange(objects);
        } else {
            onLongClicked(row);
        }
    }

    private void onLongClicked(final ODataRow row) {
        mLongClicked = false;
        final Float count = ((lineValues.containsKey(row.getString("id")))
                ? lineValues.get(row.getString("id")) : 0);
        String name = "";

        if(pp.selectRowId(row.getInt("id")) != -1 && !pp.browse(pp.selectRowId(row.getInt("id"))).equals(null)) {
            ODataRow oDataRow = pp.browse(pp.selectRowId(row.getInt("id")));
            name = (!oDataRow.getString("default_code").equals("false") ? "["+oDataRow.getString("default_code") + "] " : "") + oDataRow.getString("name");
        }
        OAlert.inputDialog(this, name, new OAlert.OnUserInputListener() {
            @Override
            public void onViewCreated(EditText inputView) {
                inputView.setInputType(InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                inputView.setText(count.toString());
                inputView.setSelection(String.valueOf(count).length());
            }

            @Override
            public void onUserInputted(Object value) {
                float userData = Float.parseFloat(value.toString());
                lineValues.put(row.getString("id"), userData);
                mAdapter.notifiyDataChange(objects);
            }
        });
    }

    @Override
    public void onSearchChange(List<Object> newRecords) {
    }
}