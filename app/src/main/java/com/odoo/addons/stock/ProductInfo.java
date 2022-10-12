package com.odoo.addons.stock;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.account.AccountPaymentTerm;
import com.odoo.addons.account.AccountTax;
import com.odoo.addons.sale.SaleCategory;
import com.odoo.base.addons.ir.IrModuleModule;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.rpc.Odoo;
import com.odoo.core.rpc.handler.OdooVersionException;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.listeners.IOdooConnectionListener;
import com.odoo.core.rpc.listeners.OdooError;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.addons.fragment.IOnSearchViewChangeListener;
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.utils.IntentUtils;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OCursorUtils;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.sys.IOnBackPressListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductInfo extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
        ISyncStatusObserverListener, OCursorListAdapter.OnViewBindListener, IOnBackPressListener,
        LoaderManager.LoaderCallbacks<Cursor>, IOnSearchViewChangeListener,IOdooConnectionListener,
        AdapterView.OnItemClickListener{

    private static final String TAG = ProductInfo.class.getSimpleName();
    private View mView;
    private OCursorListAdapter listAdapter;
    private ProgressDialog progressDialog;
    private String mCurFilter = null;
    private ProductProduct pp;
    private AccountTax at;
    private ProductCategory pc;
    private UomUom uu;
    private AccountPaymentTerm apt;
    private SaleCategory sc;
    private ResUsers ru;
    private StockWarehouse swh;
    private TextView footerCount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.product_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        ListView listView = (ListView) mView.findViewById(R.id.product_list_view);
        footerCount = (TextView) mView.findViewById(R.id.product_count);

        listAdapter = new OCursorListAdapter(getActivity(), null, R.layout.product_row_item);
        listAdapter.setOnViewBindListener(this);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(this);
        listView.setFastScrollEnabled(true);
        listView.setFastScrollAlwaysVisible(true);
        parent().setOnBackPressListener(this);

        pp = new ProductProduct(getContext(), null);
        pc = new ProductCategory(getContext(),null);
        uu = new UomUom(getContext(),null);
        at = new AccountTax(getContext(), null);
        sc = new SaleCategory(getContext(), null);
        ru = new ResUsers(getContext(), null);


        getLoaderManager().initLoader(0, null, this);

        for(ODataRow oDataRow : pp.query("SELECT * FROM product_product")){
            Log.d(TAG, "product_product: " + oDataRow.getAll());
        }

        for(ODataRow oDataRow : pc.query("SELECT * FROM product_category")){
            Log.d(TAG, "product_category: " + oDataRow.getAll());
        }

        for(ODataRow oDataRow : at.query("SELECT * FROM product_product_account_tax_rel")){
            Log.d(TAG, "product_product_account_tax_rel: " + oDataRow.getAll());
        }

        for(ODataRow oDataRow : ru.query("SELECT * FROM res_users")){
            Log.d(TAG, "res_users: " + oDataRow.getAll());
        }

        ResCurrency rc = new ResCurrency(getContext(), null);
        for(ODataRow oDataRow : rc.query("SELECT * FROM res_currency")){
            Log.d(TAG, "res_currency: " + oDataRow.getAll());
        }

        IrModuleModule imm =  new IrModuleModule(getContext(), null);
        for(ODataRow oDataRow : imm.query("SELECT * FROM ir_module_module")){
            Log.d(TAG, "ir_module_module: " + oDataRow.getAll() );
        }
    }

    @Override
    public Class<ProductProduct> database() {
        return ProductProduct.class;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        String [] projection = new String []{"name","default_code","barcode"};
        String where = "";
        String[] whereArgs ;
        List<String> args = new ArrayList<>();
        if (mCurFilter != null) {
            where += "(default_code like ? OR name like ? OR barcode like ?)";
            args.add("%" + mCurFilter + "%");
            args.add("%" + mCurFilter + "%");
            args.add("%" + mCurFilter + "%");
        }
        whereArgs = args.toArray(new String[args.size()]);
        return new CursorLoader(getActivity(), db().uri(), projection, where,whereArgs, "default_code");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        listAdapter.changeCursor(data);
        refreshFooter();
        if (data.getCount() > 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setVisible(mView, R.id.swipe_container);
                    OControls.setGone(mView, R.id.data_list_no_item);

                    OControls.setGone(mView, R.id.icon);
                    OControls.setGone(mView, R.id.title);
                    OControls.setGone(mView, R.id.subTitle);

                    setHasSwipeRefreshView(mView, R.id.swipe_container, ProductInfo.this);
                }
            }, 500);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setGone(mView, R.id.swipe_container);
                    OControls.setVisible(mView, R.id.data_list_no_item);
                    OControls.setImage(mView, R.id.icon, R.drawable.ic_action_product);
                    OControls.setText(mView, R.id.title, _s(R.string.label_no_product_found));
                    OControls.setText(mView, R.id.subTitle, _s(R.string.swipe_to_update_list));
                    setHasSwipeRefreshView(mView, R.id.data_list_no_item, ProductInfo.this);
                }
            }, 500);
        }
    }


    private void refreshFooter(){
        String query = "SELECT COUNT(_id) AS total FROM product_product";
        footerCount.setText(String.format("%s%s %s", getString(R.string.label_footer_count), pp.query(query).get(0).getString("total"), getString(R.string.title_activity_product_info)));
    }

    public class TaxObject{
        int product_server_id;
        int tax_row_id;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        listAdapter.changeCursor(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_product_list, menu);
        setHasSearchView(this, menu, R.id.menu_search);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_dashboard_goto_today) {
            checkConnection();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        List<ODrawerItem> items = new ArrayList<>();
        items.add(new ODrawerItem("Seller")
                .setTitle(context.getString(R.string.title_directory))
                .setGroupTitle());
        items.add(new ODrawerItem(TAG).setTitle(context.getString(R.string.title_activity_product_info))
                .setIcon(R.drawable.ic_action_product)
                .setInstance(new ProductInfo()));
        return items;
    }

    @Override
    public void onStatusChange(Boolean refreshing) {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public boolean onBackPressed() {
        return true;
    }

    @Override
    public boolean onSearchViewTextChange(String newFilter) {
        mCurFilter = newFilter;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public void onSearchViewClose() {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ODataRow row = OCursorUtils.toDatarow((Cursor) listAdapter.getItem(position));
        Bundle data = new Bundle();
        if (row != null) {
            data = row.getPrimaryBundleData();
        }
        IntentUtils.startActivity(getActivity(), ProductDetails.class, data);
    }

    private class ProductDownload extends AsyncTask<ODataRow, Void, Void> {

        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setTitle(R.string.title_please_wait);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(OResource.string(getContext(), R.string.title_working));
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(ODataRow... params) {
            ODomain oDomain = new ODomain();
//            apt.quickSyncRecords(oDomain);
            sc.quickSyncRecords(oDomain);
            oDomain.add("company_id", "=", user().getCompanyId());
//            swh.quickSyncRecords(oDomain);

            //Product Template
            JSONArray jsonArray1 = new JSONArray();
            for(ODataRow dataRow : pp.query("SELECT id, write_date FROM product_product")){
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("id", dataRow.get("id"));
                    jsonObject.put("write_date", dataRow.get("write_date"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray1.put(jsonObject);
            }
            OArguments argsTemplate = new OArguments();
            argsTemplate.add("product.product");
            argsTemplate.add(jsonArray1);
            try {
                String templateResponse = pp.getServerDataHelper().callMethodCracker("get_product_list_sale_mobile", argsTemplate);
                OArguments oArguments = new OArguments();
                oArguments.add("product.product");
                if(templateResponse != null) {
                    JSONObject jsonObject = new JSONObject(templateResponse);
                    if (jsonObject.has("result")) {
                        JSONArray productListResult = jsonObject.getJSONArray("result");
                        JSONObject updateObject = new JSONObject(productListResult.get(0).toString());
                        JSONObject insertObject = new JSONObject(productListResult.get(1).toString());
                        JSONArray updateList = updateObject.getJSONArray("update");
                        JSONArray insertList = insertObject.getJSONArray("insert");
                        if (updateList.length() > 0) {
                            for (int i = 0; i < updateList.length(); i++) {
                                JSONObject templateObj = updateList.getJSONObject(i);

                                int categ_id = -1;
                                int uom_id = -1;

                                if(!templateObj.getString("categ_id").equals("false")) {
                                    categ_id = pc.quickSelectRowId(templateObj.getInt("categ_id"));
                                    if(categ_id < 0){
                                        quickSyncRecordOne(pc, templateObj.getInt("categ_id"));
                                        categ_id = pc.quickSelectRowId(templateObj.getInt("categ_id"));
                                    }
                                }

                                if(!templateObj.getString("uom_id").equals("false")) {
                                    uom_id = uu.quickSelectRowId(templateObj.getInt("uom_id"));
                                    if(uom_id < 0){
                                        quickSyncRecordOne(uu, templateObj.getInt("uom_id"));
                                        uom_id = uu.quickSelectRowId(templateObj.getInt("uom_id"));
                                    }
                                }

                                pp.query("UPDATE product_product " +
                                                "SET write_date = ?, name = ?, default_code = ?, list_price = ?, barcode = ?, categ_id = ?, uom_id = ? " +
                                                "WHERE id = ?",
                                        new String[]{templateObj.getString("write_date"),
                                                templateObj.getString("name"),
                                                templateObj.getString("default_code"),
                                                templateObj.getString("list_price"),
                                                templateObj.getString("barcode"),
                                                String.valueOf(categ_id),
                                                String.valueOf(uom_id),
                                                templateObj.getString("id")});

                                List<ODataRow> productList = selectRowId2(pp,templateObj.getInt("id"));
                                if(productList.size() > 0) {
                                    JSONArray taxes_ids = new JSONArray(templateObj.getString("taxes_id"));
                                    at.query("DELETE FROM product_product_account_tax_rel WHERE product_product_id = ?", new String[]{productList.get(0).getString("_id")});
                                    for (int k = 0; k < taxes_ids.length(); k++) {
                                        int product_id = productList.get(0).getInt("_id");
                                        int tax_id = 0;
                                        List<ODataRow> accountTaxList = selectRowId2(at, taxes_ids.getInt(k));
                                        if (accountTaxList.size() > 0) {
                                            tax_id = accountTaxList.get(0).getInt("_id");
                                        } else {
                                            quickSyncRecordOne(at, taxes_ids.getInt(k));
                                            accountTaxList = selectRowId2(at, taxes_ids.getInt(k));
                                            if (accountTaxList.size() > 0)
                                                tax_id = accountTaxList.get(0).getInt("_id");
                                        }
                                        if (product_id > 0 && tax_id > 0)
                                            at.query("INSERT INTO product_product_account_tax_rel (product_product_id, account_tax_id) " +
                                                    "VALUES (?,?)", new String[]{String.valueOf(product_id), String.valueOf(tax_id)});
                                    }
                                }
                            }
                        }

                        if (insertList.length() > 0) {
                            final int COLUMNS_SIZE = 10;
                            final int MAX_ROW = 999 / COLUMNS_SIZE;
                            int counter = 0;
                            List<JSONObject> tempList = new ArrayList<>();
                            List<TaxObject> productTaxList = new ArrayList<>();
                            for (int i = 0; i < insertList.length(); i++) {
                                tempList.add(insertList.getJSONObject(i));
                                counter++;
                                if (counter == MAX_ROW || insertList.length() - 1 == i) {
                                    counter = 0;
                                    String sql = "INSERT INTO product_product (id, write_date, name, default_code, list_price, type, barcode, categ_id, uom_id, image_1920) VALUES ";

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

                                        int categ_id = 0;
                                        int uom_id = 0;

                                        if(!tempList.get(j).getString("categ_id").equals("false")) {
                                            List<ODataRow> productCategoryList = selectRowId2(pc, tempList.get(j).getInt("categ_id"));
                                            if(productCategoryList.size() > 0) {
                                                categ_id = productCategoryList.get(0).getInt("_id");
                                            }
                                            else{
                                                quickSyncRecordOne(pc, tempList.get(j).getInt("categ_id"));
                                                productCategoryList = selectRowId2(pc, tempList.get(j).getInt("categ_id"));
                                                if (productCategoryList.size() > 0)
                                                    categ_id = productCategoryList.get(0).getInt("_id");
                                            }
                                        }

                                        if(!tempList.get(j).getString("uom_id").equals("false")) {
                                            List<ODataRow> uomList = selectRowId2(uu, tempList.get(j).getInt("uom_id"));
                                            if(uomList.size() > 0) {
                                                uom_id = uomList.get(0).getInt("_id");
                                            }
                                            else{
                                                quickSyncRecordOne(uu, tempList.get(j).getInt("uom_id"));
                                                uomList = selectRowId2(uu, tempList.get(j).getInt("uom_id"));
                                                if (uomList.size() > 0)
                                                    uom_id = uomList.get(0).getInt("_id");
                                            }
                                        }

                                        if(!tempList.get(j).getString("taxes_id").equals("[null]")) {
                                            List<Integer> newIds = new ArrayList<>();
                                            JSONArray taxes_ids = new JSONArray(tempList.get(j).getString("taxes_id"));
                                            for (int k = 0; k < taxes_ids.length(); k++) {
                                                List<ODataRow> accountTaxList = selectRowId2(at,taxes_ids.getInt(k));
                                                if (accountTaxList.size() > 0) {
                                                    newIds.add(accountTaxList.get(0).getInt("_id"));
                                                } else {
                                                    quickSyncRecordOne(at, taxes_ids.getInt(k));
                                                    accountTaxList = selectRowId2(at,taxes_ids.getInt(k));
                                                    if (accountTaxList.size() > 0)
                                                        newIds.add(accountTaxList.get(0).getInt("_id"));
                                                }
                                            }

                                            if (newIds.size() > 0) {
                                                for (int q = 0; q < newIds.size(); q++) {
                                                    TaxObject taxObject = new TaxObject();
                                                    taxObject.product_server_id = tempList.get(j).getInt("id");
                                                    taxObject.tax_row_id = newIds.get(q);
                                                    productTaxList.add(taxObject);
                                                }
                                            }
                                        }

                                        arguments[j * COLUMNS_SIZE] = tempList.get(j).getString("id");
                                        arguments[j * COLUMNS_SIZE + 1] = tempList.get(j).getString("write_date");
                                        arguments[j * COLUMNS_SIZE + 2] = tempList.get(j).getString("name");
                                        arguments[j * COLUMNS_SIZE + 3] = tempList.get(j).getString("default_code");
                                        arguments[j * COLUMNS_SIZE + 4] = tempList.get(j).getString("list_price");
                                        arguments[j * COLUMNS_SIZE + 5] = tempList.get(j).getString("type");
                                        arguments[j * COLUMNS_SIZE + 6] = tempList.get(j).getString("barcode");
                                        arguments[j * COLUMNS_SIZE + 7] = String.valueOf(categ_id);
                                        arguments[j * COLUMNS_SIZE + 8] = String.valueOf(uom_id);
                                        arguments[j * COLUMNS_SIZE + 9] = tempList.get(j).getString("image_1920");

                                        if (j % MAX_ROW == MAX_ROW - 1 || (j == tempList.size() - 1)) {
                                            pp.query(sql, arguments);
                                            StringBuilder taxRelSQL = new StringBuilder("INSERT INTO product_product_account_tax_rel (account_tax_id, product_product_id) VALUES ");
                                            String[] taxesArguments = new String[]{};
                                            final int COLUMNS_SIZE1 = 2;
                                            for (int l = 0; l < productTaxList.size(); l++) {
                                                if (l == 0) {
                                                    taxesArguments = new String[productTaxList.size() * COLUMNS_SIZE1];
                                                    for (int m = 0; m < COLUMNS_SIZE1; m++) {
                                                        taxRelSQL.append(m == 0 ? "(?" : ",?");
                                                        if (m == COLUMNS_SIZE1 - 1)
                                                            taxRelSQL.append(")");
                                                    }
                                                } else {
                                                    for (int m = 0; m < COLUMNS_SIZE1; m++) {
                                                        taxRelSQL.append(m == 0 ? ",(?" : ",?");
                                                        if (m == COLUMNS_SIZE1 - 1)
                                                            taxRelSQL.append(")");
                                                    }
                                                }

                                                int product_row_id = 0;
                                                List<ODataRow> productList = selectRowId2(pp, productTaxList.get(l).product_server_id);
                                                if(productList.size() > 0)
                                                    product_row_id = productList.get(0).getInt("_id");

                                                taxesArguments[l * COLUMNS_SIZE1] = String.valueOf(productTaxList.get(l).tax_row_id);
                                                taxesArguments[l * COLUMNS_SIZE1 + 1] = String.valueOf(product_row_id);

                                                if (l == productTaxList.size() - 1) {

                                                    pp.query(taxRelSQL.toString(), taxesArguments);
                                                }
                                            }
                                            productTaxList.clear();
                                            tempList.clear();
                                        }
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
            progressDialog.dismiss();
            hideRefreshingProgress();
            getLoaderManager().restartLoader(0, null, ProductInfo.this);
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

    @Override
    public void onRefresh() {
        checkConnection();
    }

    private void checkConnection(){
        if(inNetwork()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Odoo.createInstance(getContext(), user().getHost()).setOnConnect(ProductInfo.this);
                    } catch (OdooVersionException e) {
                        e.printStackTrace();
                    }
                }
            }, 500);

        }else{
            Toast.makeText(getActivity(), _s(R.string.toast_network_required), Toast.LENGTH_LONG).show();
            hideRefreshingProgress();
        }
    }

    @Override
    public void onConnect(Odoo odoo) {
        ProductDownload productDownload = new ProductDownload();
        productDownload.execute();
    }

    @Override
    public void onError(OdooError error) {
        hideRefreshingProgress();
        Toast.makeText(getContext(),getText(R.string.toast_not_connect_server),Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {
        OControls.setText(view, R.id.name, row.getString("default_code").equals("false") ? row.get("name") : "[" + row.get("default_code") + "] "+ row.get("name"));
    }
}
