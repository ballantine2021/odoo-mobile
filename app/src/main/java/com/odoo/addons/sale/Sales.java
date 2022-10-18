package com.odoo.addons.sale;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
//import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.stock.ProductProduct;
import com.odoo.addons.stock.StockPicking;
import com.odoo.addons.stock.UomUom;
import com.odoo.base.addons.ir.IrModuleModule;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.Odoo;
import com.odoo.core.rpc.handler.OdooVersionException;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.listeners.IOdooConnectionListener;
import com.odoo.core.rpc.listeners.OdooError;
import com.odoo.core.support.OUser;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.addons.fragment.IOnSearchViewChangeListener;
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.support.list.IOnItemClickListener;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.utils.IntentUtils;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OCursorUtils;
import com.odoo.core.utils.ODateUtils;
import com.odoo.core.utils.OPreferenceManager;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.sys.IOnBackPressListener;
import com.odoo.libs.calendar.SysCal;
import com.odoo.libs.calendar.view.OdooCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Sales extends BaseFragment implements OCursorListAdapter.OnViewBindListener,
        ISyncStatusObserverListener,
        LoaderManager.LoaderCallbacks<Cursor>, IOnItemClickListener, View.OnClickListener,
        IOnBackPressListener,IOnSearchViewChangeListener,
        BottomSheetListenersNew.OnSheetItemClickListener,BottomSheetListenersNew.OnSheetActionClickListener,
        OdooCalendar.OdooCalendarDateSelectListener, IOdooConnectionListener {

    private static final String TAG = Sales.class.getSimpleName();
    private static final String EXTRA_KEY_TYPE = "extra_key_type";
    private View mView;
    private OCursorListAdapter mAdapter;
    private String mFilter = null;
    private Type mType = Type.Quotation;
    private SaleOrder so;
    private SaleOrderLine sol;
    private View calendarView = null;
    private ListView quotationList;
    private OdooCalendar odooCalendar;
    private final Date date = new Date();
    private String mFilterDate = date.toString();
    private List<ODataRow> sales ;
    private ProgressDialog pd;
    private DecimalFormat decimalFormat;
    private BottomSheetNew bottomSheet;
    private enum SyncType {MultiSync, ConfirmSync, CancelSync, DraftSync, PickingSync}
    private SyncType sType = SyncType.MultiSync;
    private ODataRow clickedRow = null;

    public enum Type {
        Quotation,
        SaleOrder
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        calendarView = LayoutInflater.from(getActivity()).inflate(R.layout.sale_order_dashboard_items,
                container, false);
        return inflater.inflate(R.layout.sale_order_dashboard, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pd = new ProgressDialog(getActivity());
        pd.setTitle(R.string.title_please_wait);
        pd.setMessage(OResource.string(getActivity(), R.string.title_working));
        pd.setCancelable(false);

        parent().setOnBackPressListener(this);
        mView = view;
        mType = Type.valueOf(getArguments().getString(EXTRA_KEY_TYPE));
        if(mType == Type.SaleOrder)
            view.findViewById(R.id.fabButton).setVisibility(View.GONE);
        getLoaderManager().initLoader(0, null, this);
        so = new SaleOrder(getContext(), null);
        sol= new SaleOrderLine(getContext(), null);
        odooCalendar = (OdooCalendar) view.findViewById(R.id.dashboard_sale_order);
        odooCalendar.setOdooCalendarDateSelectListener(this);
    }

    @Override
    public Class<SaleOrder> database() {
        return SaleOrder.class;
    }

    @Override
    public void onSheetActionClick(BottomSheetNew bottomSheet, Object extras) {
        bottomSheet.dismiss();
        ODataRow row = OCursorUtils.toDatarow((Cursor) extras);
        loadActivity(row);
    }

    @Override
    public void onItemClick(BottomSheetNew var1, MenuItem menu, Object extras) {

        ODataRow row = OCursorUtils.toDatarow((Cursor) extras);
        int i = menu.getItemId();
        switch (i){
            case R.id.menu_so_quotation_cancel:
                sType = SyncType.CancelSync;
                clickedRow = row;
                checkConnection();
                break;
            case R.id.menu_so_confirm_sale:
                sType = SyncType.ConfirmSync;
                clickedRow = row;
                checkConnection();
                break;
            case R.id.menu_to_draft:
                sType = SyncType.DraftSync;
                clickedRow = row;
                checkConnection();
                break;
            case R.id.menu_so_picking_confirm:
                sType = SyncType.PickingSync;
                clickedRow = row;
                checkConnection();
                break;
        }
        var1.dismiss();
    }

    @Override
    public List<OdooCalendar.DateDataObject> weekDataInfo(List<SysCal.DateInfo> week_dates) {
        List<OdooCalendar.DateDataObject> items = new ArrayList<>();
        for (SysCal.DateInfo date : week_dates) {
            String date_str = date.getDateString();
            items.add(new OdooCalendar.DateDataObject(date_str, false));
        }
        return items;
    }

    @Override
    public View getEventsView(ViewGroup viewGroup, SysCal.DateInfo dateInfo) {
        calendarView = LayoutInflater.from(getActivity()).inflate(R.layout.sale_order_dashboard_items,
                viewGroup, false);
        quotationList = (ListView) calendarView.findViewById(R.id.items_container);
        mFilterDate = dateInfo.getDateString();
        setHasFloatingButton(mView, R.id.fabButton, quotationList, this);
        mAdapter = new OCursorListAdapter(getActivity(), null, R.layout.sale_order_item);
        mAdapter.setOnViewBindListener(this);
        quotationList.setAdapter(mAdapter);
        quotationList.setFastScrollAlwaysVisible(true);
        mAdapter.handleItemClickListener(quotationList, this);
        setHasSyncStatusObserver(Sales.class.getSimpleName(), this, db());
        if (getActivity() != null)
            getLoaderManager().restartLoader(0, null, Sales.this);
        return calendarView;
    }

    @Override
    public void onConnect(Odoo odoo) {
        if(sType.equals(SyncType.MultiSync)){
            OnSaleOrderDownload onSaleOrderDownload = new OnSaleOrderDownload();
            onSaleOrderDownload.execute();
        }
        else{
            if(sType.equals(SyncType.PickingSync)){
                PickingConfirm pickingConfirm = new PickingConfirm();
                pickingConfirm.execute();
            }
            else {
                OnOperation onOperation = new OnOperation();
                onOperation.execute();
            }
        }
    }

    @Override
    public void onError(OdooError error) {
        hideRefreshingProgress();
        Toast.makeText(getContext(),getText(R.string.toast_not_connect_server),Toast.LENGTH_SHORT).show();
    }

    private class OnOperation extends AsyncTask<ODataRow, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd.show();
        }

        protected String doInBackground(ODataRow... params) {
            String response;
            String action = "";
            OArguments args = new OArguments();
            args.add(new JSONArray().put(clickedRow.getInt("id")));

            if(sType.equals(SyncType.ConfirmSync)){
                action = "action_confirm";
            }
            if(sType.equals(SyncType.DraftSync)){
                action = "action_draft";
            }
            if(sType.equals(SyncType.CancelSync)){
                action ="action_cancel";
            }

            response = so.getServerDataHelper().callMethodCracker(action, args);

            if(sType.equals(SyncType.ConfirmSync)){
                action = "get_picking_ids_mobile";
                String result = so.getServerDataHelper().callMethodCracker(action, args);
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    if(jsonObject.has("result")){
                        OValues oValues = new OValues();
                        oValues.put("picking_ids", jsonObject.getString("result"));
                        so.update(clickedRow.getInt("_id"), oValues);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return response;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(String.valueOf(result));
            OValues oValues = new OValues();
            try {
                JSONObject jsonObject = new JSONObject(result);
                if(jsonObject.has("result")){
                    if(jsonObject.getString("result").equals("true")){
                        if(sType.equals(SyncType.ConfirmSync))
                            oValues.put("state", "sale");
                        if(sType.equals(SyncType.CancelSync))
                            oValues.put("state", "cancel");
                        if(sType.equals(SyncType.DraftSync))
                            oValues.put("state", "draft");
                        so.update(clickedRow.getInt("_id"), oValues);
                        getLoaderManager().restartLoader(0,  null, Sales.this);
                    }
                }else
                {
                    if(sType.equals(SyncType.ConfirmSync) && jsonObject.has("error")) {
                        JSONObject errorObject = new JSONObject(jsonObject.getString("error"));
                        if(errorObject.has("data")){
                            JSONObject dataObject = new JSONObject(errorObject.getString("data"));
                            if(dataObject.has("message")) {
                                Toast.makeText(getContext(), "" + dataObject.getString("message"), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pd.dismiss();
        }
    }

    private class PickingConfirm extends AsyncTask<ODataRow, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd.show();
        }

        protected String doInBackground(ODataRow... params) {
            String response ="";
            String action = "do_new_transfer_sale_mobile";
            OArguments args = new OArguments();
            StockPicking sp = new StockPicking(getContext(), null);
            try {
                JSONArray arr = new JSONArray(clickedRow.getString("picking_ids"));
                JSONObject picking_obj = arr.getJSONObject(0);
                args.add(new JSONArray().put(picking_obj.getInt("id")));
                response = sp.getServerDataHelper().callMethodCracker(action, args);


            } catch (JSONException e) {
                e.printStackTrace();
            }

            return response;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(String.valueOf(result));
            OValues oValues = new OValues();
            try {
                JSONObject jsonObject = new JSONObject(result);
                if(jsonObject.has("result")){
                    if(jsonObject.getString("result").equals("true")){
                        OValues oValues1 = new OValues();
                        oValues1.put("picking_ids", "[]");
                        so.update(clickedRow.getInt("_id"), oValues);
                        getLoaderManager().restartLoader(0,  null, Sales.this);
                        Toast.makeText(getContext(), "Хүргэлт амжилттай баталгаажлаа", Toast.LENGTH_LONG).show();
                    }
                    if(jsonObject.getString("result").equals("done")){
                        Toast.makeText(getContext(), "Хүргэлт хэдийн баталгаажсан байна", Toast.LENGTH_LONG).show();
                    }
                    if(jsonObject.getString("result").equals("different")){
                        Toast.makeText(getContext(), "Хүргэлтийг хийх боломжгүй", Toast.LENGTH_LONG).show();
                    }
                }else
                {
                    JSONObject errorObject = new JSONObject(jsonObject.getString("error"));
                    if(errorObject.has("data")){
                        JSONObject dataObject = new JSONObject(errorObject.getString("data"));
                        if(dataObject.has("message")) {
                            Toast.makeText(getContext(), "" + dataObject.getString("message"), Toast.LENGTH_LONG).show();
                        }
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pd.dismiss();
        }
    }

    private class OnSaleOrderDownload extends AsyncTask<ODataRow, Void, Void> {
        ResPartner rp = new ResPartner(getContext(), null);

        ResUsers ru = new ResUsers(getContext(), null);
        ResCurrency rc = new ResCurrency(getContext(), null);
        ProductProduct pp = new ProductProduct(getContext(), null);
        UomUom uu = new UomUom(getContext(), null);

        protected void onPreExecute() {
            super.onPreExecute();
            pd.show();
        }

        @Override
        protected Void doInBackground(ODataRow... params) {
            OPreferenceManager preferenceManager = new OPreferenceManager(getContext());
            int date_limit = preferenceManager.getInt("sync_data_limit", 7);
            sol.query("DELETE FROM sale_order_line");
            so.query("DELETE FROM sale_order");
            OArguments args = new OArguments();
            args.add(getContext());
            args.add(ODateUtils.getDateBefore(date_limit));

            try {
                String response = so.getServerDataHelper().callMethodCracker("get_sale_order_list_mobile", args);
                if(response != null) {
                    JSONObject jsonObject = new JSONObject(response);
                    if(jsonObject.has("result")) {
                        JSONArray arr = jsonObject.getJSONArray("result");
                        if (arr.length() > 0) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
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

                                so.query("INSERT INTO sale_order (id, name, partner_id, partner_name, date_order, validity_date, state, " +
                                                "user_id, amount_total, amount_untaxed, amount_tax, currency_id, currency_symbol, picking_ids) " +
                                                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
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
                                                obj.getString("currency_symbol").equals("null") ? "" : obj.getString("currency_symbol"),
                                                obj.getString("picking_ids")});

                                if (obj.getJSONArray("order_line").length() > 0) {
                                    int order_id;
                                    final int COLUMNS_SIZE = 10;
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
                                                        "price_tax, price_subtotal, price_total, order_id, product_uom) " +
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

                                                    if (j % MAX_ROW == MAX_ROW - 1 || (j == tempList.size() - 1)) {
                                                        sol.query(sql, arguments);
                                                        tempList.clear();
                                                    }
                                                }
                                            }
                                        }
                                        HashMap<String, Integer> newLineIds = new HashMap<>();
                                        for(ODataRow oDataRow : sol.query("SELECT id, product_id FROM sale_order_line WHERE order_id = ?", new String[]{String.valueOf(order_id)})){
                                            newLineIds.put(String.valueOf(oDataRow.getInt("product_id")),oDataRow.getInt("id"));
                                        }
                                        OValues updateValue = new OValues();
                                        updateValue.put("line_products", newLineIds.toString());
                                        so.update(order_id, updateValue);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if ( pd!=null && pd.isShowing() ){
                pd.cancel();
            }
            hideRefreshingProgress();
            getLoaderManager().restartLoader(0, null, Sales.this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_sales, menu);
        setHasSearchView(this, menu, R.id.menu_sales_search);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_sales_refresh) {
            sType = SyncType.MultiSync;
            checkConnection();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchViewTextChange(String newFilter) {
        mFilter = newFilter;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public void onSearchViewClose() {

    }

    @Override
    public void onStatusChange(Boolean refreshing) {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {
        OControls.setText(view, R.id.name, row.get("name").equals("false") ? "" : row.get("name") );
        OControls.setText(view, R.id.date_order, ODateUtils.convertToDefault(row.getString("date_order"), ODateUtils.DEFAULT_FORMAT, "MMMM, dd"));
        OControls.setText(view, R.id.amount_total, decimalFormat.format(row.get("amount_total")));
        switch (row.getString("state")){
            case "sale":
            case "done":
                OControls.setText(view, R.id.state, R.string.selection_state_sale_order);
                break;
            case "draft":
                OControls.setText(view, R.id.state, R.string.selection_state_draft);
                break;
            case "cancel":
                OControls.setText(view, R.id.state, R.string.selection_state_cancel);
                break;
        }

        if(!row.getString("partner_name").equals("false")) {
            OControls.setVisible(view, R.id.partner_name);
            OControls.setText(view, R.id.partner_name, row.getString("partner_name"));
        }
        if (row.getString("currency_symbol").equals("false")) {
            OControls.setGone(view, (R.id.currency_symbol));
        } else {
            OControls.setText(view, R.id.currency_symbol, row.get("currency_symbol"));
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

    void checkConnection(){
        if (inNetwork()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Odoo.createInstance(getContext(), user().getHost()).setOnConnect(Sales.this);
                    } catch (OdooVersionException e) {
                        e.printStackTrace();
                    }
                }
            }, 500);
        } else {
            hideRefreshingProgress();
            Toast.makeText(getActivity(), _s(R.string.toast_network_required), Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String where = "";
        List<String> arguments = new ArrayList<>();
        arguments.add(mFilterDate);
        switch (mType) {
            case Quotation:
                where = "date(date_order) >= ? AND date(date_order) <= ? AND (state = ? or state = ?)";
                arguments.add(mFilterDate);
                arguments.add("draft");
                arguments.add("cancel");
                break;
            case SaleOrder:
                where = "date(date_order) >= ? AND date(date_order) <= ? AND (state = ? or state = ? or state = ?)";
                arguments.add(mFilterDate);
                arguments.add("sale");
                arguments.add("sent");
                arguments.add("done");
                break;
        }
        if (mFilter != null) {
            where += " AND name LIKE ? ";
            arguments.add("%" + mFilter + "%");
        }
        return new CursorLoader(getActivity(), db().uri(),null,where,arguments.toArray(new String[arguments.size()]),"name desc");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        try {
            if (data.getCount() > 0) {
                mAdapter.changeCursor(data);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        OControls.setGone(mView, R.id.loadingProgress);
                        OControls.setVisible(calendarView, R.id.items_container);
                    }
                }, 500);
            } else {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        OControls.setGone(mView, R.id.loadingProgress);
                        OControls.setGone(calendarView, R.id.items_container);
                    }
                }, 500);
            }

            decimalFormat = new DecimalFormat("#,##0.0",new DecimalFormatSymbols(Locale.getDefault()));
            TextView salesCount = (TextView) calendarView.findViewById(R.id.salesCount);
            TextView salesBalance = (TextView) calendarView.findViewById(R.id.DayBalance);
            float sales_day_balance = 0.f;
            String where = "";
            if (mFilter != null) {
                switch (mType) {
                    case Quotation:
                        where += " AND (state = ? OR state = ?) AND (partner_name LIKE ? OR ref LIKE ?)";
                        sales = so.query( "SELECT amount_total FROM sale_order WHERE date(date_order) >=  ? AND date(date_order) <= ?" + where, new String[]{mFilterDate, mFilterDate, "draft", "cancel", "%" + mFilter + "%", "%" + mFilter + "%"});
                        break;
                    case SaleOrder:
                        where += " AND (state = ? OR state = ? OR state = ?) AND (partner_name LIKE ? OR ref LIKE ?)";
                        sales = so.query( "SELECT amount_total FROM sale_order WHERE date(date_order) >=  ? AND date(date_order) <= ?" + where, new String[]{mFilterDate, mFilterDate, "sale", "sent", "done", "%" + mFilter + "%", "%" + mFilter + "%"});
                        break;
                }
            }else {
                switch (mType) {
                    case Quotation:
                        sales = so.query( "SELECT amount_total FROM sale_order WHERE date(date_order) >=  ? AND date(date_order) <= ? AND (state = ? OR state = ?)", new String[]{mFilterDate, mFilterDate, "draft", "cancel"});
                        break;
                    case SaleOrder:
                        sales = so.query( "SELECT amount_total FROM sale_order WHERE date(date_order) >=  ? AND date(date_order) <= ? AND (state = ? OR state = ? OR state = ?)", new String[]{mFilterDate, mFilterDate, "sale", "sent", "done"});
                        break;
                }
            }
            if (sales.size() > 0){
                for (ODataRow row : sales) {
                    sales_day_balance += row.getFloat("amount_total");
                }
            }
            sales.clear();
            salesBalance.setText(decimalFormat.format(sales_day_balance));
            salesCount.setText(decimalFormat.format(data.getCount()));
        }   catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    @Override
    public void onItemDoubleClick(View view, int position) {
        ODataRow row = OCursorUtils.toDatarow((Cursor) mAdapter.getItem(position));
        Bundle data = row.getPrimaryBundleData();
        data.putString("type", mType.toString());
        IntentUtils.startActivity(getActivity(), SalesDetail.class, data);
    }

    @Override
    public void onItemClick(View view, int position) {
        if(bottomSheet!=null && bottomSheet.isShowing()){
            return;
        }
        else {
            Cursor data = (Cursor) mAdapter.getItem(position);
            BottomSheetNew.Builder builder = new BottomSheetNew.Builder(getContext());
            builder.listener(this);
            builder.setIconColor(_c(R.color.colorAccent));
            builder.setTextColor(Color.parseColor("#414141"));
            builder.actionListener(this);
            builder.setActionIcon(R.drawable.ic_action_edit);
            String title = data.getString(data.getColumnIndex("name"));

            if (data.getString(data.getColumnIndex("picking_ids")).length()!=2) {
                title += " -> Хүргэлт: ";
                try {
                    JSONArray arr = new JSONArray(data.getString(data.getColumnIndex("picking_ids")));
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject picking_obj = arr.getJSONObject(i);
                        title += picking_obj.getString("name") + " ";
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            builder.title(title);
            builder.setData(data);

            if (data.getString(data.getColumnIndex("state")).equals("sale")){
                if (data.getString(data.getColumnIndex("picking_ids")).length()!=2)
                    builder.menu(R.menu.menu_order_sheet_picking);
                else
                    builder.menu(R.menu.menu_order_sheet);
            }
            if (data.getString(data.getColumnIndex("state")).equals("done"))
                builder.menu(R.menu.menu_order_sheet);
            if (data.getString(data.getColumnIndex("state")).equals("draft"))
                builder.menu(R.menu.menu_quotation_sheet);
            if (data.getString(data.getColumnIndex("state")).equals("sent"))
                builder.menu(R.menu.menu_quotation_sheet);
            if (data.getString(data.getColumnIndex("state")).equals("cancel"))
                builder.menu(R.menu.menu_cancel_sheet);
            bottomSheet = builder.create();
            bottomSheet.show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fabButton:
                loadActivity(null);
                break;
        }
    }

    @Override
    public boolean onBackPressed()  {
        if (bottomSheet != null && bottomSheet.isShowing()) {
            bottomSheet.dismiss();
            return false;
        }
        return true;
    }

    private void loadActivity(ODataRow row) {
        Bundle data;
        if(row!=null)
            data = row.getPrimaryBundleData();
        else
            data = new Bundle();
        data.putString("type", mType.toString());
        IntentUtils.startActivity(getActivity(), SalesDetail.class, data);
    }

    public List<ODrawerItem> drawerMenus(Context context) {
        OUser user = OUser.current(context);

        IrModuleModule irModule = new IrModuleModule(context, null);
        if(irModule.select(null, "name = ?", new String[]{"mobile-backend"}).get(0).getString("state").equals("installed")) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("SALE_USER_ROLE", Context.MODE_PRIVATE);
            String role = sharedPreferences.getString("userId-"+user.getUserId(), "false");
            if (role.equals("sale_sales_man")) {
                List<ODrawerItem> items = new ArrayList<>();
                items.add(new ODrawerItem(TAG).setTitle(context.getString(R.string.title_activity_quotation))
                        .setIcon(R.drawable.ic_action_quotation)
                        .setExtra(extra(Type.Quotation))
                        .setInstance(new Sales()));
                items.add(new ODrawerItem(TAG).setTitle(context.getString(R.string.title_activity_sale_order))
                        .setIcon(R.drawable.ic_action_sale_order)
                        .setExtra(extra(Type.SaleOrder))
                        .setInstance(new Sales()));
                return items;
            }
        }

        return null;
    }

    private Bundle extra(Type quotation) {
        Bundle extra = new Bundle();
        extra.putString(EXTRA_KEY_TYPE, quotation.toString());
        return extra;
    }
}
