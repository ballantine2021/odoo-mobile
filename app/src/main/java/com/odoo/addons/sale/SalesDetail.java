package com.odoo.addons.sale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.R;
import com.odoo.addons.account.CustomerPayments;
import com.odoo.addons.stock.ProductProduct;
import com.odoo.addons.stock.StockWarehouse;
import com.odoo.addons.stock.UomUom;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.rpc.Odoo;
import com.odoo.core.rpc.handler.OdooVersionException;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.listeners.IOdooConnectionListener;
import com.odoo.core.rpc.listeners.OdooError;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OAlert;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OResource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import odoo.controls.ExpandableListControl;
import odoo.controls.OField;
import odoo.controls.OForm;

public class SalesDetail extends AppCompatActivity implements View.OnClickListener, IOdooConnectionListener {
    private static final String TAG = SalesDetail.class.getSimpleName();
    private static final int REQUEST_ADD_ITEMS = 323;
    private Bundle extra;
    private OForm oForm;
    private ODataRow record = null;
    private SaleOrder so;
    private SaleOrderLine sol;
    private ActionBar actionBar;
    private ExpandableListControl.ExpandableListAdapter mAdapter;
    private final List<Object> objects = new ArrayList<>();
    private final HashMap<String, Float> lineValues = new HashMap<>();
    private final HashMap<String, Integer> lineIds = new HashMap<>();
    private TextView untaxed_Amt;
    private TextView taxes_Amt;
    private TextView total_Amt;
    private double untaxedAmt;
    private double taxesAmt;
    private double totalAmt;
    private ProductProduct pp = null;
    private Sales.Type mType;
    private App app;
    private OField partner_id;
    private OUser user;
    private Context context;
    private UomUom uu;
    private DecimalFormat decimalFormat1;
    private DecimalFormat decimalFormat2;
    private final int row_id = OModel.INVALID_ROW_ID;
    private Menu mMenu;
    private LinearLayout linearLayout;

    private enum SyncType {SaveSync, ProductChange}
    private SyncType sType = SyncType.ProductChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sale_detail);
        OActionBarUtils.setActionBar(this, true);
        actionBar = getSupportActionBar();
        app = (App) getApplicationContext();
        context = this.getApplicationContext();
        user = OUser.current(this);
        so = new SaleOrder(context, user);
        sol = new SaleOrderLine(context, user);
        pp = new ProductProduct(context, user);
        uu = new UomUom(context, user);

        extra = getIntent().getExtras();
        oForm = (OForm) findViewById(R.id.saleForm);
        oForm.setEditable(true);
        mType = Sales.Type.valueOf(extra.getString("type"));
        decimalFormat1 = new DecimalFormat("#,##0.00",new DecimalFormatSymbols(Locale.getDefault()));
        decimalFormat2 = new DecimalFormat("#,##0.0",new DecimalFormatSymbols(Locale.getDefault()));
        init();
        initAdapter();
    }

    private void init() {
        partner_id = (OField) findViewById(R.id.partnerId);

        TextView currency1 = (TextView) findViewById(R.id.currency1);
        TextView currency2 = (TextView) findViewById(R.id.currency2);
        TextView currency3 = (TextView) findViewById(R.id.currency3);
        String currencySymbol = null;
        untaxed_Amt = (TextView) findViewById(R.id.untaxedTotal);
        taxes_Amt = (TextView) findViewById(R.id.taxesTotal);
        total_Amt = (TextView) findViewById(R.id.fTotal);
        untaxed_Amt.setText(decimalFormat1.format(0f));
        taxes_Amt.setText(decimalFormat1.format(0f));
        total_Amt.setText(decimalFormat1.format(0f));
        LinearLayout layoutAddItem = (LinearLayout) findViewById(R.id.layoutAddItem);
        if (extra.containsKey("event_id")){
            partner_id.setValue(extra.getInt("partner_id"));
        }
        layoutAddItem.setOnClickListener(this);
        if (extra == null || !extra.containsKey(OColumn.ROW_ID)) {
            oForm.initForm(null);
            actionBar.setTitle(R.string.label_sales_quotation);
        } else {
            record = so.browse(extra.getInt(OColumn.ROW_ID));
            if(!record.getString("name").equals("false"))
                actionBar.setTitle(record.getString("name"));
            if (record == null) {
                finish();
            }
            if (mType == Sales.Type.Quotation) {
                if (record.getString("state").equals("cancel"))
                    layoutAddItem.setVisibility(View.GONE);
            } else {
                layoutAddItem.setVisibility(View.GONE);
                oForm.setEditable(false);
            }
            currencySymbol = record.getM2ORecord("currency_id").browse().getString("symbol");
            untaxed_Amt.setText(decimalFormat1.format(record.getFloat("amount_untaxed")));
            taxes_Amt.setText(decimalFormat1.format(record.getFloat("amount_tax")));
            total_Amt.setText(decimalFormat1.format(record.getFloat("amount_total")));
            try {
                oForm.initForm(record);
            }catch (Exception e){
                Log.d(TAG, "init: " + e);
            }
        }
        currency1.setText(currencySymbol);
        currency2.setText(currencySymbol);
        currency3.setText(currencySymbol);
        linearLayout = (LinearLayout) findViewById(R.id.payment_details_layout);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CustomerPayments.class);
                intent.putExtra("partner_id", (Integer) partner_id.getValue());
                if(((Integer)partner_id.getValue()) != -1) {
                    startActivityForResult(intent, REQUEST_ADD_ITEMS);
                }
            }
        });
    }

    private void onLongClicked(final Integer selProductID) {
        String title = "";
        final Float quantity = lineValues.get(selProductID.toString());
        List<ODataRow> productInfo = pp.query("SELECT default_code, name FROM product_product WHERE id = ? ", new String[]{selProductID.toString()});
        if(productInfo.size() > 0){
            title = productInfo.get(0).getString("default_code").equals("false") ? "" : "[" + productInfo.get(0).getString("default_code") + "] ";
            title += productInfo.get(0).getString("name").equals("false") ? "" : productInfo.get(0).getString("name");
        }
        if(title.length() == 0)
            title = getString(R.string.label_sale_order_line_quantity);

        OAlert.inputDialog(this, title, new OAlert.OnUserInputListener() {
            @Override
            public void onViewCreated(EditText inputView) {
                inputView.setInputType(InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                inputView.setText(String.valueOf(quantity));
                if (quantity > 0.1f) {
                    inputView.setText(String.valueOf(quantity));
                    inputView.setSelection(String.valueOf(quantity).length());
                }
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }

            @Override
            public void onUserInputted(Object value) {
                float quantity = Float.parseFloat(value.toString());
                if(quantity > 0)
                    lineValues.put(selProductID.toString(), quantity);
                else
                    lineValues.remove(selProductID.toString());
                sType = SyncType.ProductChange;
                checkConnection();
            }
        });
    }

    private void initAdapter() {
        partner_id.setOnValueChangeListener((field, value) -> {
            OnCustomerChangeUpdate onCustomerChangeUpdate = new OnCustomerChangeUpdate();
            onCustomerChangeUpdate.execute(field.getValue().toString());
        });
        final ExpandableListControl mList = (ExpandableListControl) findViewById(R.id.expListOrderLine);
        mList.setVisibility(View.VISIBLE);
        if (extra != null && record != null) {
            List<ODataRow> lines = record.getO2MRecord("order_line").browseEach();
            for (ODataRow line : lines) {
                int product_id = pp.selectServerId(line.getInt("product_id"));
                if (product_id != 0) {
                    lineValues.put(Integer.toString(product_id), line.getFloat("product_uom_qty"));
                    lineIds.put(Integer.toString(product_id), line.getInt("id"));
                }
            }
            objects.addAll(lines);
        }
        mAdapter = mList.getAdapter(R.layout.sale_order_line_item, objects,
                (position, mView, parent) -> {
                    final ODataRow row = (ODataRow) mAdapter.getItem(position);
                    List<ODataRow> productList = pp.query("SELECT _id, id, name, default_code " +
                            "FROM product_product " +
                            "WHERE _id = ?",new String[]{Integer.toString(row.getInt("product_id"))});
                    if(productList.size() > 0){
                        final int productServerID = productList.get(0).getInt("id");
                        OControls.setText(mView, R.id.edtName, (productList.get(0).getString("default_code").equals("false") ? productList.get(0).getString("name"): "[" + productList.get(0).getString("default_code") + "] ") + productList.get(0).getString("name"));
                        if (row.getFloat("virtual_available") != -100)
                            OControls.setText(mView, R.id.edtVirtualAvailable, decimalFormat2.format(row.getFloat("virtual_available")));
                        else
                            OControls.setText(mView, R.id.edtVirtualAvailable, decimalFormat2.format(0.0));

                        OControls.setText(mView, R.id.edtProductQty, decimalFormat2.format(row.getFloat("product_uom_qty")));
                        if (row.getFloat("virtual_available") != -100){
                            if (row.getFloat("virtual_available") < row.getFloat("product_uom_qty"))
                                OControls.setTextColor(mView, R.id.edtProductQty, Color.RED);
                        }

                        OControls.setText(mView, R.id.edtProductPrice, decimalFormat1.format(row.getFloat("price_unit")));
                        OControls.setText(mView, R.id.edtSubTotal,  decimalFormat1.format(row.getFloat("price_total")));

                        mView.setOnLongClickListener(v -> {
                            if (extra == null || !extra.containsKey(OColumn.ROW_ID)) {
                                onLongClicked(productServerID);
                            }
                            else {
                                if(record != null && record.contains("state") && (record.getString("state").equals("draft") || record.getString("state").equals("cancel")))
                                    onLongClicked(productServerID);
                            }
                            return true;
                        });
                    }
                    return mView;
                });
        mAdapter.notifyDataSetChanged(objects);
    }

    private class OnCustomerChangeUpdate extends AsyncTask<String, Void, OValues> {

        ResPartner rp = new ResPartner(context,user);
        StockWarehouse swh = new StockWarehouse(getApplicationContext(), null);
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected OValues doInBackground(String... params) {

            List<ODataRow> rows = rp.query("SELECT id, property_payment_term_id, property_product_pricelist FROM res_partner WHERE _id = ?", new String[]{params[0]});
            OValues oValues = new OValues();

            OArguments args1 = new OArguments();
            args1.add(rows.get(0).getInt("id"));
            args1.add(rows.get(0).getInt("id"));

            if(rows.size() > 0) {
            /* Харилцагчийн дэполт үнийн хүснэгт, төлбөрийн нөхцөлийг сонгох */
                if (!rows.get(0).getString("property_payment_term_id").equals("false"))
                    oValues.put("payment_term_id", rows.get(0).getInt("property_payment_term_id"));
                if (!rows.get(0).getString("property_product_pricelist").equals("false"))
                    oValues.put("pricelist_id", rows.get(0).getInt("property_product_pricelist"));
                List<ODataRow> warehouseList = swh.query("SELECT _id FROM stock_warehouse ORDER BY id DESC");
                if(warehouseList.size() > 0){
                    oValues.put("warehouse_id", warehouseList.get(0).getInt("_id"));
                }
            }
            return oValues;
        }
        @Override
        protected void onPostExecute(OValues v) {
            super.onPostExecute(v);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.layoutAddItem) {
            if (!partner_id.getValue().toString().equals("-1")) {
                Intent intent = new Intent(context, AddProductLineWizard.class);
                Bundle extra = new Bundle();
                for (String key : lineValues.keySet()) {
                    extra.putFloat(key, lineValues.get(key));

                }
                intent.putExtras(extra);
                intent.putExtra("partner_id", (Integer) partner_id.getValue());
                startActivityForResult(intent, REQUEST_ADD_ITEMS);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (oForm.getEditable()) {
            showConfirmCustomer(this, OResource.string(this, R.string.close_activity),
                    type -> {
                        if (type == OAlert.ConfirmType.POSITIVE) {
                            finish();
                        }
                    });
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_ITEMS && resultCode == Activity.RESULT_OK) {
            lineValues.clear();
            for (String key : data.getExtras().keySet()) {
                if (data.getExtras().getFloat(key) > 0)
                    lineValues.put(key, data.getExtras().getFloat(key));
            }
            sType = SyncType.ProductChange;
            checkConnection();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mMenu = menu;
        getMenuInflater().inflate(R.menu.menu_sales_detail, menu);
        menu.findItem(R.id.menu_sales_detail_cancel).setVisible(false);
        menu.findItem(R.id.menu_sales_detail_edit).setVisible(false);
        menu.findItem(R.id.menu_sales_detail_more).setVisible(false);
        if(record != null){
            if(!record.getString("state").equals("draft") && !record.getString("state").equals("cancel")){
                menu.findItem(R.id.menu_sales_detail_save).setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.menu_sales_detail_save: {
                if(!partner_id.getValue().toString().equals("-1")) {
                        mMenu.findItem(R.id.menu_sales_detail_save).setEnabled(false);
                        sType = SyncType.SaveSync;
                        checkConnection();
                }else {
                    if(partner_id.getValue().toString().equals("-1")) {
                        Toast.makeText(context, R.string.sale_customer_required, Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private static void showConfirmCustomer(Context context, String message, final OAlert.OnAlertConfirmListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.label_warning));
        builder.setCancelable(true);
        builder.setMessage(message);
        builder.setPositiveButton(context.getResources().getString(R.string.alert_choose_yes), (dialog, which) -> {
            if (listener != null) {
                listener.onConfirmChoiceSelect(OAlert.ConfirmType.POSITIVE);
            }
        });
        builder.setNegativeButton(context.getResources().getString(R.string.alert_choose_no), (dialog, which) -> {
            if (listener != null) {
                listener.onConfirmChoiceSelect(OAlert.ConfirmType.NEGATIVE);
            }
        });
        builder.setOnCancelListener(dialog -> {
        });
        builder.create().show();
    }

    private void checkConnection(){
        if(app.inNetwork()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Odoo.createInstance(context, user.getHost()).setOnConnect(SalesDetail.this);
                    } catch (OdooVersionException e) {
                        e.printStackTrace();
                    }
                }
            }, 50);

        }else{
            Toast.makeText(context, getText(R.string.toast_network_required), Toast.LENGTH_LONG).show();
            mMenu.findItem(R.id.menu_sales_detail_save).setEnabled(true);
        }
    }

    @Override
    public void onConnect(Odoo odoo) {
        if(sType == SyncType.SaveSync) {
            OnSaleOrderSync onSaleOrderSync = new OnSaleOrderSync();
            onSaleOrderSync.execute(row_id);
        }
        if(sType == SyncType.ProductChange) {
            OnProductChange onProductChange = new OnProductChange();
            onProductChange.execute(lineValues);
        }
    }

    @Override
    public void onError(OdooError error) {
        Toast.makeText(context,getText(R.string.toast_not_connect_server), Toast.LENGTH_SHORT).show();
        mMenu.findItem(R.id.menu_sales_detail_save).setEnabled(true);
    }

    private class  OnSaleOrderSync extends AsyncTask<Integer, Void, Integer> {
        private ProgressDialog progressDialog;
        private final ResPartner rp = new ResPartner(getBaseContext(), null);
        private final ResUsers ru = new ResUsers(getBaseContext(), null);
        private final ResCurrency rc = new ResCurrency(getBaseContext(), null);
        private boolean success = false;
        private String message = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(SalesDetail.this);
            progressDialog.setTitle(R.string.title_please_wait);
            progressDialog.setMessage(OResource.string(SalesDetail.this, R.string.title_sent_to_server));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        @Override
        protected Integer doInBackground(Integer... params) {
            try {
                OValues values = oForm.getValues();
                JSONArray order_line = new JSONArray();

                int partner_server_id = -1;
                if (!values.getString("partner_id").equals("false")) {
                    partner_server_id = rp.selectServerId(values.getInt("partner_id"));
                }

                for (Object line : objects) {
                    JSONArray o_line = new JSONArray();
                    ODataRow row = (ODataRow) line;
                    int product_id = (pp.selectServerId(row.getInt("product_id")));
                    o_line.put((lineIds.containsKey(product_id)) ? 1 : 0);
                    o_line.put((lineIds.containsKey(product_id)) ? lineIds.get(product_id) : false);
                    JSONObject line_data = new JSONObject();
                    if(lineIds.containsKey(product_id)) {
                        line_data.put("product_uom_qty", row.get("product_uom_qty"));
                        o_line.put(line_data);
                    } else {
                        line_data.put("product_id", product_id);
                        line_data.put("product_uom_qty", row.get("product_uom_qty"));
                        o_line.put(line_data);
                    }
                    order_line.put(o_line);
                    lineIds.remove(product_id);
                }
                if (lineIds.size() > 0) {
                    for (String key : lineIds.keySet()) {
                        JSONArray o_line = new JSONArray();
                        o_line.put(2);
                        o_line.put(lineIds.get(key));
                        o_line.put(false);
                        order_line.put(o_line);
                    }
                }

                JSONObject data = new JSONObject();
                data.put("partner_id", partner_server_id > 0 ? partner_server_id:false);
                data.put("date_order", values.getString("date_order"));
                data.put("order_line", order_line);

                int saleOrderId = 0;
                if (record != null) {
                    data.put("id", record.getInt("id"));
                    saleOrderId = record.getInt("id");
                    OArguments args1 = new OArguments();
                    args1.add(record.getInt("id"));
                    args1.add(data);
                    String result = so.getServerDataHelper().callMethodCracker("write", args1);
                    JSONObject jsonObject = new JSONObject(result);
                    if(jsonObject.has("result")) {
                        if(jsonObject.get("result").equals(true)){
                            success = true;
                            so.query("DELETE FROM sale_order WHERE id = ?", new String[]{String.valueOf(saleOrderId)});
                        }
                    }
                }else {
                    OArguments args1 = new OArguments();
                    args1.add(data);
                    String result = so.getServerDataHelper().callMethodCracker("create", args1);
                    JSONObject jsonObject = new JSONObject(result);
                    if(jsonObject.has("result")) {
                        if(isInteger(jsonObject.getString("result")))
                        {
                            success = true;
                            saleOrderId = jsonObject.getInt("result");
                        }
                        else
                            message = jsonObject.getString("result");
                    }
                }
                if(success){
                    OArguments args2 = new OArguments();
                    args2.add(context);
                    args2.add(saleOrderId);
                    String orderResult = so.getServerDataHelper().callMethodCracker("get_sale_order_mobile", args2);

                    JSONObject res_obj = new JSONObject(orderResult);
                    if(res_obj.has("result")) {
                        for (int i = 0; i < res_obj.getJSONArray("result").length(); i++) {
                            JSONObject obj = res_obj.getJSONArray("result").getJSONObject(i);
                            String partner_id = "false";
                            String user_id = "false";
                            String currency_id = "false";

                            List<ODataRow> partnerList; // 1
                            if (!obj.getString("partner_id").equals("null")) {
                                partnerList = rp.query("SELECT _id FROM res_partner WHERE id = ?", new String[]{obj.getString("partner_id")});
                                if (partnerList.size() == 0) {
                                    ODomain oDomain = new ODomain();
                                    oDomain.add("id", "=", obj.getString("partner_id"));
                                    rp.quickSyncRecords(oDomain);
                                    partnerList = rp.query("SELECT _id, name, phone, street FROM res_partner WHERE id = ?", new String[]{obj.getString("partner_id")});
                                    if (partnerList.size() > 0) {
                                        partner_id = partnerList.get(0).getString("_id");

                                        String display_name = partnerList.get(0).getString("name");
                                        if (!partnerList.get(0).getString("phone").equals("false")) {
                                            display_name += " " + partnerList.get(0).getString("phone");
                                        }
                                        if (!partnerList.get(0).getString("street").equals("false")) {
                                            display_name += " " + partnerList.get(0).getString("street");
                                        }
                                        rp.query("UPDATE res_partner SET display_name = ? WHERE _id = ?", new String[]{display_name, partnerList.get(0).getString("_id")});
                                    }
                                } else
                                    partner_id = partnerList.get(0).getString("_id");
                            }

                            if (!obj.getString("user_id").equals("null")) {
                                List<ODataRow> userList = selectRowId2(ru, obj.getInt("user_id"));
                                if (userList.size() == 0) {
                                    quickSyncRecordOne(ru, obj.getInt("user_id"));
                                    userList = selectRowId2(ru, obj.getInt("user_id"));
                                    if (userList.size() > 0)
                                        user_id = userList.get(0).getString("_id");
                                } else
                                    user_id = userList.get(0).getString("_id");
                            }

                            if (!obj.getString("currency_id").equals("null")) {
                                List<ODataRow> currencyList = selectRowId2(rc, obj.getInt("currency_id"));
                                if (currencyList.size() == 0) {
                                    quickSyncRecordOne(rc, obj.getInt("currency_id"));
                                    currencyList = selectRowId2(rc, obj.getInt("currency_id"));
                                    if (currencyList.size() > 0)
                                        currency_id = currencyList.get(0).getString("_id");
                                } else
                                    currency_id = currencyList.get(0).getString("_id");
                            }

                            so.query("INSERT INTO sale_order (id, name, partner_id, partner_name,  date_order, validity_date, state, " +
                                            "user_id, amount_total, amount_untaxed, amount_tax, currency_id, currency_symbol) " +
                                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                    new String[]{obj.getString("id"),
                                            obj.getString("name"),
                                            String.valueOf(partner_id),
                                            obj.getString("partner_name"),
                                            obj.getString("date_order").equals("null") ? "false" : obj.getString("date_order"),
                                            obj.getString("validity_date").equals("null") ? "false" : obj.getString("validity_date"),
                                            obj.getString("state"),

                                            String.valueOf(user_id),
                                            obj.getString("amount_total"),
                                            obj.getString("amount_untaxed"),
                                            obj.getString("amount_tax"),

                                            String.valueOf(currency_id),
                                            obj.getString("currency_symbol").equals("null") ? "" : obj.getString("currency_symbol")});


                            if (obj.getJSONArray("order_line").length() > 0) {
                                int order_id;
                                final int COLUMNS_SIZE = 11;
                                final int MAX_ROW = 999 / COLUMNS_SIZE;
                                List<ODataRow> saleOrderList = so.query("SELECT _id FROM sale_order WHERE id = ?", new String[]{obj.getString("id")});
                                if(saleOrderList.size() > 0){
                                    order_id = saleOrderList.get(0).getInt("_id");
                                    int counter = 0;
                                    JSONArray order_line_sale = obj.getJSONArray("order_line");
                                    List<JSONObject> tempList = new ArrayList<>();
                                    for (int l = 0; l < order_line_sale.length(); l++) {
                                        tempList.add(order_line_sale.getJSONObject(l));
                                        counter++;
                                        if (counter == MAX_ROW || order_line_sale.length() - 1 == l) {
                                            counter = 0;
                                            String sql = "INSERT INTO sale_order_line (id, product_id, name, product_uom_qty, price_unit, " +
                                                    "price_tax, price_subtotal, price_total, order_id, product_uom, virtual_available) " +
                                                    "VALUES ";
                                            String[] arguments = new String[]{};
                                            for (int j = 0; j < tempList.size(); j++) {
                                                if (j == 0) {
                                                    if (tempList.size() == MAX_ROW)
                                                        arguments = new String[MAX_ROW * COLUMNS_SIZE];
                                                    else
                                                        arguments = new String[tempList.size() * COLUMNS_SIZE];
                                                    for (int k = 0; k < COLUMNS_SIZE; k++) {
                                                        sql += k == 0 ? "(?" : ",?";
                                                        if (k == COLUMNS_SIZE - 1)
                                                            sql += ")";
                                                    }
                                                } else {
                                                    for (int k = 0; k < COLUMNS_SIZE; k++) {
                                                        sql += k == 0 ? ",(?" : ",?";
                                                        if (k == COLUMNS_SIZE - 1)
                                                            sql += ")";
                                                    }
                                                }
                                                String product_id = "false";
                                                String product_uom = "false";
                                                List<ODataRow> productList; // 1
                                                if (!tempList.get(j).getString("product_id").equals("null")) {
                                                    productList = pp.query("SELECT _id, id, name, default_code FROM product_product WHERE id = ?", new String[]{tempList.get(j).getString("product_id")});
                                                    if (productList.size() == 0) {
                                                        ODomain oDomain = new ODomain();
                                                        oDomain.add("id", "=", tempList.get(j).getString("product_id"));
                                                        pp.quickSyncRecords(oDomain);
                                                        productList = pp.query("SELECT _id, id, name, default_code FROM product_product WHERE id = ?", new String[]{tempList.get(j).getString("product_id")});
                                                        if (productList.size() > 0)
                                                            product_id = productList.get(0).getString("_id");
                                                    } else
                                                        product_id = productList.get(0).getString("_id");
                                                }

                                                List<ODataRow> productUomList; // 2
                                                if (!tempList.get(j).getString("product_uom").equals("null")) {
                                                    productUomList = uu.query("SELECT _id FROM uom_uom WHERE id = ?", new String[]{tempList.get(j).getString("product_uom")});
                                                    if (productUomList.size() == 0) {
                                                        ODomain oDomain = new ODomain();
                                                        oDomain.add("id", "=", tempList.get(j).getString("product_uom"));
                                                        uu.quickSyncRecords(oDomain);
                                                        productUomList = uu.query("SELECT _id FROM uom_uom WHERE id = ?", new String[]{tempList.get(j).getString("product_uom")});
                                                        if (productUomList.size() > 0)
                                                            product_uom = productUomList.get(0).getString("_id");
                                                    } else
                                                        product_uom = productUomList.get(0).getString("_id");
                                                }

                                                arguments[j * COLUMNS_SIZE] = tempList.get(j).getString("id");
                                                arguments[j * COLUMNS_SIZE + 1] = product_id;
                                                arguments[j * COLUMNS_SIZE + 2] = tempList.get(j).getString("name");
                                                arguments[j * COLUMNS_SIZE + 3] = tempList.get(j).getString("product_uom_qty");
                                                arguments[j * COLUMNS_SIZE + 4] = tempList.get(j).getString("price_unit");
                                                arguments[j * COLUMNS_SIZE + 5] = tempList.get(j).getString("price_tax");
                                                arguments[j * COLUMNS_SIZE + 6] = tempList.get(j).getString("price_subtotal");
                                                arguments[j * COLUMNS_SIZE + 7] = tempList.get(j).getString("price_total");
                                                arguments[j * COLUMNS_SIZE + 8] = String.valueOf(order_id);
                                                arguments[j * COLUMNS_SIZE + 9] = product_uom;
                                                arguments[j * COLUMNS_SIZE + 10] = tempList.get(j).getString("virtual_available");

                                                if (j % MAX_ROW == MAX_ROW - 1 || (j == tempList.size() - 1)) {
                                                    sol.query(sql, arguments);
                                                    tempList.clear();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }catch (Exception e){
                Log.d(TAG, "doInBackground: " + e);
            }
            return params[0];
        }

        @Override
        protected void onPostExecute(Integer orderId) {
            progressDialog.dismiss();
            if(success)
            {
                Toast.makeText(app, "Амжилттай илгээгдлээ", Toast.LENGTH_SHORT).show();
                oForm.setEditable(false);
                finish();
            }
            else{
                Toast.makeText(app, "Өгөгдөл илгээхэд алдаа гарсан!\n "+ message, Toast.LENGTH_LONG).show();
                oForm.setEditable(true);
                mMenu.findItem(R.id.menu_sales_detail_save).setEnabled(true);
            }
        }
    }

    public List<ODataRow> selectRowId2(OModel model, int server_id){
        return model.query("SELECT _id FROM " + model.getModelName().replace(".", "_") + " WHERE id = ?", new String[]{String.valueOf(server_id)});
    }

    public void quickSyncRecordOne(OModel model, int server_id){
        ODomain singleDomain = new ODomain();
        singleDomain.add("id", "=", server_id);
        model.quickSyncRecords(singleDomain);
    }

    private class OnProductChange extends AsyncTask<HashMap<String, Float>, Void, List<ODataRow>> {
        ProgressDialog progressDialog;
        ResPartner rp;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(SalesDetail.this);
            progressDialog.setTitle(R.string.title_please_wait);
            progressDialog.setMessage(OResource.string(SalesDetail.this, R.string.title_working));
            progressDialog.setCancelable(false);
            progressDialog.show();
            rp = new ResPartner(context, null);
        }

        @Override
        protected final List<ODataRow> doInBackground(HashMap<String, Float>... params) {
            List<ODataRow> items = new ArrayList<>();
            try {
                JSONArray lineArray = new JSONArray();
                for (String key : params[0].keySet()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("product_id", Integer.valueOf(key));
                    jsonObject.put("product_uom_qty", params[0].get(key));
                    lineArray.put(jsonObject);
                }

                int partner = rp.selectServerId((Integer) partner_id.getValue());
                OArguments args = new OArguments();
                args.add(partner);
                args.add(lineArray);

                String result = so.getServerDataHelper().callMethodCracker("onchange_product_id_mobile", args);
                final JSONObject resultObj = new JSONObject(result);
                if(resultObj.has("result")) {
                    JSONObject dataObj= new JSONObject(resultObj.getString("result"));
                    untaxedAmt = dataObj.getDouble("amount_untaxed");
                    taxesAmt = dataObj.getDouble("amount_tax");
                    totalAmt = dataObj.getDouble("amount_total");
                    JSONArray arr = dataObj.getJSONArray("order_line");
                    if (arr.length() > 0) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            List<ODataRow> productList = pp.query("SELECT _id, id, name, list_price FROM product_product WHERE id = ?", new String[]{obj.getString("product_id")});
                            if (productList.size() > 0) {
                                ODataRow product = productList.get(0);
                                OValues values = new OValues();
                                values.put("product_id", product.getInt("_id"));
                                values.put("product_uom_qty", obj.getDouble("product_uom_qty"));
                                values.put("price_unit", obj.getDouble("price_unit"));
                                values.put("price_subtotal", obj.getDouble("price_subtotal"));
                                values.put("price_total", obj.getDouble("price_total"));
                                values.put("virtual_available", obj.getDouble("virtual_available"));
                                if (extra != null)
                                    values.put("order_id", extra.getInt("id"));
                                items.add(values.toDataRow());
                            }
                        }
                    }
                }
                else
                {
                    Handler handler1 = new Handler(Looper.getMainLooper());
                    handler1.post(() -> {
                        if(resultObj.has("error")) {
                            try {
                                JSONObject errorObject = new JSONObject(resultObj.getString("error"));
                                if(errorObject.has("data")){
                                    final JSONObject dataObject = new JSONObject(errorObject.getString("data"));
                                    if(dataObject.has("message")) {
                                        runOnUiThread(() -> {
                                            try {
                                                Toast.makeText(getApplicationContext(), "" + dataObject.getString("message"), Toast.LENGTH_LONG).show();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

            } catch (Exception e) {
                SalesDetail.this.runOnUiThread(() -> Toast.makeText(SalesDetail.this, R.string.toast_error_regist_item, Toast.LENGTH_LONG).show());
            }
            return items;
        }

        @Override
        protected void onPostExecute(List<ODataRow> rows) {
            progressDialog.dismiss();
            if (rows != null) {
                objects.clear();
                objects.addAll(rows);
                mAdapter.notifyDataSetChanged(objects);
                untaxed_Amt.setText(decimalFormat1.format(untaxedAmt));
                taxes_Amt.setText(decimalFormat1.format(taxesAmt));
                total_Amt.setText(decimalFormat1.format(totalAmt));
            }
        }
    }

    public static class OActionBarUtils {
        public static void setActionBar(SalesDetail activity, Boolean withHomeButtonEnabled) {
            Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
            if (toolbar != null) {
                activity.setSupportActionBar(toolbar);
                ActionBar actionBar = activity.getSupportActionBar();
                if (withHomeButtonEnabled) {
                    assert actionBar != null;
                    actionBar.setHomeButtonEnabled(true);
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }
            }
        }
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        return true;
    }
}

