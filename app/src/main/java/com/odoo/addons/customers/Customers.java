/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * <p/>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * <p/>
 * Created on 30/12/14 3:28 PM
 */
package com.odoo.addons.customers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
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


import com.odoo.addons.account.AccountPaymentTerm;
import com.odoo.addons.sale.PriceList;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.listeners.IOdooConnectionListener;
import com.odoo.core.rpc.listeners.OdooError;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.utils.BitmapUtils;
import com.odoo.core.utils.OResource;
import com.odoo.R;
import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCountry;
import com.odoo.base.addons.res.ResCountryState;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.rpc.Odoo;
import com.odoo.core.rpc.handler.OdooVersionException;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.addons.fragment.IOnSearchViewChangeListener;
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.utils.IntentUtils;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OCursorUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Customers extends BaseFragment implements ISyncStatusObserverListener,
        LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener,
        OCursorListAdapter.OnViewBindListener, IOnSearchViewChangeListener, View.OnClickListener,
        AdapterView.OnItemClickListener, IOdooConnectionListener {

    public static final String KEY = Customers.class.getSimpleName();
    public static final String EXTRA_KEY_TYPE = "extra_key_type";
    private View mView;
    private String mCurFilter = null;
    private OCursorListAdapter mAdapter = null;
    private ProgressDialog progressDialog;
    private boolean syncRequested = false;
    private DecimalFormat decimalFormat;
    private TextView footerCount;
    private ResPartner resPartner;


    @Override
    public void onConnect(Odoo odoo) {
        GetResPartnerTeam getResPartnerTeam = new GetResPartnerTeam();
        getResPartnerTeam.execute();
        setSwipeRefreshing(true);
    }

    @Override
    public void onError(OdooError error) {
        Toast.makeText(getContext(),getText(R.string.toast_not_connect_server),Toast.LENGTH_SHORT).show();
        progressDialog.dismiss();
        hideRefreshingProgress();
    }

    public enum Type {
        Customer, Supplier, Company
    }

    private Type mType = Type.Customer;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        setHasSyncStatusObserver(KEY, this, db());
        return inflater.inflate(R.layout.common_listview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        mType = Type.valueOf(getArguments().getString(EXTRA_KEY_TYPE));
        ListView mPartnersList = (ListView) view.findViewById(R.id.listview);
        mAdapter = new OCursorListAdapter(getActivity(), null, R.layout.customer_row_item);
        mAdapter.setOnViewBindListener(this);
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle(com.odoo.R.string.title_please_wait);
        progressDialog.setMessage(OResource.string(getActivity(), com.odoo.R.string.title_working));
        progressDialog.setCancelable(false);
        decimalFormat = new DecimalFormat("#,##0.0",new DecimalFormatSymbols(Locale.getDefault()));
        footerCount = (TextView) view.findViewById(R.id.customer_count);
        resPartner = new ResPartner(getContext(), null);

        mPartnersList.setAdapter(mAdapter);
        mPartnersList.setFastScrollAlwaysVisible(true);
        mPartnersList.setOnItemClickListener(this);
        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {
        Bitmap img;
//        if (row.getString("image_small").equals("false")) {
        img = BitmapUtils.getAlphabetImage(getActivity(), row.getString("name"));
//        } else {
//            img = BitmapUtils.getBitmapImage(getActivity(), row.getString("image_small"));
//        }
        OControls.setImage(view, R.id.image_128, img);
        OControls.setText(view, R.id.name, row.getString("name"));
        OControls.setText(view, R.id.company_name, (row.getString("company_name").equals("false"))
                ? "" : row.getString("company_name"));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        String where = "";
        List<String> args = new ArrayList<>();

        where = "id > ?";
        args.add("0");
        if (mCurFilter != null) {
            where += " and name like ? ";
            args.add(mCurFilter + "%");
        }
        String selection = (args.size() > 0) ? where : null;
        String[] selectionArgs = (args.size() > 0) ? args.toArray(new String[args.size()]) : null;
        return new CursorLoader(getActivity(), db().uri(),null, selection, selectionArgs, "name");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
        footerCount.setText(getString(R.string.label_footer_count) + data.getCount() + " " + getString(R.string.title_activity_customer));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                OControls.setGone(mView, R.id.loadingProgress);
            }
        }, 500);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    @Override
    public Class<ResPartner> database() {
        return ResPartner.class;
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        List<ODrawerItem> items = new ArrayList<>();

        items.add(new ODrawerItem(KEY).setTitle("Харилцагчид")
                .setIcon(R.drawable.ic_action_customers)
                .setExtra(extra(Type.Customer))
                .setInstance(new Customers()));
        return items;
    }

    public Bundle extra(Type type) {
        Bundle extra = new Bundle();
        extra.putString(EXTRA_KEY_TYPE, type.toString());
        return extra;
    }


    @Override
    public void onStatusChange(Boolean refreshing) {
        // Sync Status
        getLoaderManager().restartLoader(0, null, this);
    }


    @Override
    public void onRefresh() {
        checkConnection();
    }

    private void checkConnection(){
        if (inNetwork()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Odoo.createInstance(getContext(), user().getHost()).setOnConnect(Customers.this);
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

    private class GetResPartnerTeam extends AsyncTask<Void, Void, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }


        @Override
        protected String doInBackground(Void... params) {
            ResUsers resUsers = new ResUsers(getContext(),null);
            ResPartner resPartner = new ResPartner(getContext(),null);
            ResCompany resCompany = new ResCompany(getContext(),null);
            ResCountryState resCountryState = new ResCountryState(getContext(),null);
            ResCountry resCountry = new ResCountry(getContext(),null);
            AccountPaymentTerm accountPaymentTerm = new AccountPaymentTerm(getContext(),null);
            PriceList priceList = new PriceList(getContext(), null);

            final int COLUMNS_SIZE = 20;
            final int MAX_ROW = 999 / COLUMNS_SIZE;
            String display_name = "";

            OArguments argsPartner = new OArguments();
            JSONArray jsonArray = new JSONArray();
            for(ODataRow dataRow : resPartner.query("SELECT id, write_date FROM res_partner")){
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("id", dataRow.get("id"));
                    jsonObject.put("write_date", dataRow.get("write_date"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray.put(jsonObject);

            }
            argsPartner.add("res.partner");
            argsPartner.add(jsonArray);
            String partnerResponse = resPartner.getServerDataHelper().callMethodCracker("get_res_partner_list", argsPartner);
            if(partnerResponse != null){
                try {
                    JSONObject jsonObject = new JSONObject(partnerResponse);
                    if(jsonObject.has("result"))
                    {
                        JSONArray partnerListResult = jsonObject.getJSONArray("result");

                        JSONObject updateObject = new JSONObject(partnerListResult.get(0).toString());
                        JSONObject insertObject = new JSONObject(partnerListResult.get(1).toString());
                        JSONArray updateList = updateObject.getJSONArray("update");
                        JSONArray insertList = insertObject.getJSONArray("insert");

                        if (updateList.length() > 0) {
                            for (int i = 0; i < updateList.length(); i++) {
                                JSONObject partnerObj = updateList.getJSONObject(i);

                                int accPaymentTermRowId = -1;
                                int priceListRowId = -1;

                                if(!partnerObj.getString("property_payment_term_id").equals("false")) {
                                    List<ODataRow> accPaymentTermList = accountPaymentTerm.query("SELECT _id FROM account_payment_term WHERE id = ?", new String[]{partnerObj.getString("property_payment_term_id")});
                                    if (accPaymentTermList.size() > 0) {
                                        accPaymentTermRowId = accPaymentTermList.get(0).getInt("_id");
                                    } else {
                                        ODomain oDomain = new ODomain();
                                        oDomain.add("id", "=", partnerObj.getString("property_payment_term_id"));
                                        accountPaymentTerm.quickSyncRecords(oDomain);
                                        accPaymentTermList = accountPaymentTerm.query("SELECT _id FROM account_payment_term WHERE id = ?", new String[]{partnerObj.getString("property_payment_term_id")});
                                        if (accPaymentTermList.size() > 0)
                                            accPaymentTermRowId = accPaymentTermList.get(0).getInt("_id");
                                    }
                                }
                                if(!partnerObj.getString("property_product_pricelist").equals("false")) {
                                    List<ODataRow> pricelistList = priceList.query("SELECT _id FROM product_pricelist  WHERE id = ?", new String[]{partnerObj.getString("property_product_pricelist")});
                                    if (pricelistList.size() > 0) {
                                        priceListRowId = pricelistList.get(0).getInt("_id");
                                    } else {
                                        ODomain oDomain = new ODomain();
                                        oDomain.add("id", "=", partnerObj.getString("property_product_pricelist"));
                                        priceList.quickSyncRecords(oDomain);
                                        pricelistList = priceList.query("SELECT _id FROM product_pricelist WHERE id = ?", new String[]{partnerObj.getString("property_product_pricelist")});
                                        if (pricelistList.size() > 0)
                                            priceListRowId = pricelistList.get(0).getInt("_id");
                                    }
                                }

                                display_name = partnerObj.getString("name");
                                if(!partnerObj.getString("phone").equals("false")){
                                    display_name += " " + partnerObj.getString("phone");
                                }

                                resPartner.query("UPDATE res_partner " +
                                        "SET name = ?, write_date = ?, is_company = ?, street = ?, street2 = ?," +
                                        "mobile = ?," +
                                        "company_name = ?, property_product_pricelist = ?, property_payment_term_id = ?, " +
                                        "phone = ?, display_name = ? " +
                                        "WHERE id = ? ", new String[]{partnerObj.getString("name"), partnerObj.getString("write_date"), partnerObj.getString("is_company"),
                                        partnerObj.getString("street"), partnerObj.getString("street2"),
                                        partnerObj.getString("mobile"),
                                        partnerObj.getString("company_name"),
                                        String.valueOf(priceListRowId),
                                        String.valueOf(accPaymentTermRowId),
                                        partnerObj.getString("mobile"),
                                        display_name,
                                        partnerObj.getString("id")});
                            }
                        }
                        if (insertList.length() > 0) {
                            int counter = 0;
                            List<JSONObject> tempList = new ArrayList<>();
                            for (int i = 0; i < insertList.length(); i++) {
                                tempList.add(insertList.getJSONObject(i));
                                counter++;
                                int parentPartnerRowId = -1;
                                int companyRowId = -1;
                                int countryStateRowId = -1;
                                int countryRowId = -1;
                                int accPaymentTermRowId = -1;
                                int priceListRowId = -1;
                                if (counter == MAX_ROW || insertList.length() - 1 == i) {
                                    counter = 0;
                                    String sql = "INSERT INTO res_partner (id, name, write_date, is_company, street, street2, city, zip, website, mobile, email, company_id, " +
                                            "state_id, country_id, company_name, parent_id, phone, display_name, property_payment_term_id, property_product_pricelist) VALUES ";
                                    String[] arguments = new String[]{};
                                    for (int j = 0; j < tempList.size(); j++) {
                                        if (j == 0) {
                                            if (tempList.size() == MAX_ROW)
                                                arguments = new String[MAX_ROW * COLUMNS_SIZE];
                                            else
                                                arguments = new String[tempList.size() * COLUMNS_SIZE];
                                            for(int k = 0; k < COLUMNS_SIZE; k++){
                                                sql += k==0 ? "(?":",?";
                                                if(k == COLUMNS_SIZE - 1)
                                                    sql += ")";
                                            }
                                        } else {
                                            for(int k = 0; k < COLUMNS_SIZE; k++){
                                                sql += k==0 ? ",(?":",?";
                                                if(k == COLUMNS_SIZE - 1)
                                                    sql += ")";
                                            }
                                        }
                                        List<ODataRow> partnerList = resPartner.query("SELECT _id FROM res_partner WHERE id = ?", new String[] {tempList.get(j).getString("id")});

                                        if(partnerList.size() > 0) {
                                            parentPartnerRowId = partnerList.get(0).getInt("_id");
                                        }
                                        if(!tempList.get(j).getString("company_id").equals("false")){
                                            List<ODataRow> companyList = resCompany.query("SELECT _id FROM res_company WHERE id = ?", new String[] {tempList.get(j).getString("company_id")});
                                            if(companyList.size() > 0){
                                                companyRowId = companyList.get(0).getInt("_id");
                                            }
                                            else {
                                                ODomain oDomain = new ODomain();
                                                oDomain.add("id", "=", tempList.get(j).getString("company_id"));
                                                resCompany.quickSyncRecords(oDomain);
                                                companyList = resCompany.query("SELECT _id FROM res_company WHERE id = ?", new String[]{tempList.get(j).getString("company_id")});
                                                if (companyList.size() > 0)
                                                    companyRowId = companyList.get(0).getInt("_id");
                                            }
                                        }
                                        if(!tempList.get(j).getString("state_id").equals("false")) {
                                            List<ODataRow> countryStateList = resCountryState.query("SELECT _id FROM res_country_state WHERE id = ?", new String[] {tempList.get(j).getString("state_id")});
                                            if (countryStateList.size() > 0) {
                                                countryStateRowId = countryStateList.get(0).getInt("_id");
                                            } else {
                                                ODomain oDomain = new ODomain();
                                                oDomain.add("id", "=", tempList.get(j).getString("state_id"));
                                                resCountryState.quickSyncRecords(oDomain);
                                                countryStateList = resCountryState.query("SELECT _id FROM res_country_state WHERE id = ?", new String[]{tempList.get(j).getString("state_id")});
                                                if (countryStateList.size() > 0)
                                                    countryStateRowId = countryStateList.get(0).getInt("_id");
                                            }
                                        }
                                        if(!tempList.get(j).getString("country_id").equals("false")) {
                                            List<ODataRow> countryList = resCountry.query("SELECT _id FROM res_country WHERE id = ?", new String[] {tempList.get(j).getString("country_id")});
                                            if (countryList.size() > 0) {
                                                countryRowId = countryList.get(0).getInt("_id");
                                            } else {
                                                ODomain oDomain = new ODomain();
                                                oDomain.add("id", "=", tempList.get(j).getString("country_id"));
                                                resCountry.quickSyncRecords(oDomain);
                                                countryList = resCountry.query("SELECT _id FROM res_country WHERE id = ?", new String[]{tempList.get(j).getString("country_id")});
                                                if (countryList.size() > 0)
                                                    countryRowId = countryList.get(0).getInt("_id");
                                            }
                                        }
                                        if(!tempList.get(j).getString("property_payment_term_id").equals("false")) {
                                            List<ODataRow> accPaymentTermList = accountPaymentTerm.query("SELECT _id FROM account_payment_term WHERE id = ?", new String[] {tempList.get(j).getString("property_payment_term_id")});

                                            if (accPaymentTermList.size() > 0) {
                                                accPaymentTermRowId = accPaymentTermList.get(0).getInt("_id");
                                            } else {
                                                ODomain oDomain = new ODomain();
                                                oDomain.add("id", "=", tempList.get(j).getString("property_payment_term_id"));
                                                accountPaymentTerm.quickSyncRecords(oDomain);
                                                accPaymentTermList = accountPaymentTerm.query("SELECT _id FROM account_payment_term WHERE id = ?", new String[]{tempList.get(j).getString("property_payment_term_id")});
                                                if (accPaymentTermList.size() > 0)
                                                    accPaymentTermRowId = accPaymentTermList.get(0).getInt("_id");
                                            }
                                        }

                                        if(!tempList.get(j).getString("property_product_pricelist").equals("false")) {
                                            List<ODataRow> pricelistList = priceList.query("SELECT _id FROM product_pricelist  WHERE id = ?", new String[] {tempList.get(j).getString("property_product_pricelist")});

                                            if (pricelistList.size() > 0) {
                                                priceListRowId = pricelistList.get(0).getInt("_id");
                                            } else {
                                                ODomain oDomain = new ODomain();
                                                oDomain.add("id", "=", tempList.get(j).getString("property_product_pricelist"));
                                                priceList.quickSyncRecords(oDomain);
                                                pricelistList = priceList.query("SELECT _id FROM product_pricelist WHERE id = ?", new String[]{tempList.get(j).getString("property_product_pricelist")});
                                                if (pricelistList.size() > 0)
                                                    priceListRowId = pricelistList.get(0).getInt("_id");
                                            }
                                        }

                                        display_name = tempList.get(j).getString("name");
                                        if(!tempList.get(j).getString("phone").equals("false")){
                                            display_name += " " + tempList.get(j).getString("phone");
                                        }
                                        if(!tempList.get(j).getString("street").equals("false")){
                                            display_name += " " + tempList.get(j).getString("street");
                                        }

                                        arguments[j * COLUMNS_SIZE + 0] = tempList.get(j).getString("id");
                                        arguments[j * COLUMNS_SIZE + 1] = tempList.get(j).getString("name");
                                        arguments[j * COLUMNS_SIZE + 2] = tempList.get(j).getString("write_date");
                                        arguments[j * COLUMNS_SIZE + 3] = tempList.get(j).getString("is_company");
                                        arguments[j * COLUMNS_SIZE + 4] = tempList.get(j).getString("street");
                                        arguments[j * COLUMNS_SIZE + 5] = tempList.get(j).getString("street2");
                                        arguments[j * COLUMNS_SIZE + 6] = tempList.get(j).getString("city");
                                        arguments[j * COLUMNS_SIZE + 7] = tempList.get(j).getString("zip");
                                        arguments[j * COLUMNS_SIZE + 8] = tempList.get(j).getString("website");
                                        arguments[j * COLUMNS_SIZE + 9] = tempList.get(j).getString("mobile");
                                        arguments[j * COLUMNS_SIZE + 10] = tempList.get(j).getString("email");
                                        arguments[j * COLUMNS_SIZE + 11] = String.valueOf(companyRowId);
                                        arguments[j * COLUMNS_SIZE + 12] = String.valueOf(countryStateRowId);
                                        arguments[j * COLUMNS_SIZE + 13] = String.valueOf(countryRowId);
                                        arguments[j * COLUMNS_SIZE + 14] = tempList.get(j).getString("company_name");
                                        arguments[j * COLUMNS_SIZE + 15] = String.valueOf(parentPartnerRowId);
                                        arguments[j * COLUMNS_SIZE + 16] = tempList.get(j).getString("phone");
                                        arguments[j * COLUMNS_SIZE + 17] = display_name;
                                        arguments[j * COLUMNS_SIZE + 18] = String.valueOf(accPaymentTermRowId);
                                        arguments[j * COLUMNS_SIZE + 19] = String.valueOf(priceListRowId);

                                        if (j % MAX_ROW == MAX_ROW - 1 || (j == tempList.size() - 1)) {
                                            resPartner.query(sql, arguments);
                                            tempList.clear();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        if(jsonObject.has("error")) {
                            JSONObject errorObject = new JSONObject(jsonObject.getString("error"));
                            if(errorObject.has("data")){
                                final JSONObject dataObject = new JSONObject(errorObject.getString("data"));
                                if(dataObject.has("message")) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Toast.makeText(getContext(), "" + dataObject.getString("message"), Toast.LENGTH_LONG).show();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
        protected void onPostExecute(String result) {
            if ( progressDialog!=null && progressDialog.isShowing() ){
                progressDialog.cancel();
            }
            getLoaderManager().restartLoader(0, null, Customers.this);
            hideRefreshingProgress();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_partners, menu);
        setHasSearchView(this, menu, R.id.menu_partner_search);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_partner_refresh:
                checkConnection();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchViewTextChange(String newFilter) {
        mCurFilter = newFilter;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public void onSearchViewClose() {
        // nothing to do
    }

    @Override
    public void onClick(View v) {
    }

    private void loadActivity(ODataRow row) {
        Bundle data = new Bundle();
        if (row != null) {
            data = row.getPrimaryBundleData();
        }
        data.putString(CustomerDetails.KEY_PARTNER_TYPE, mType.toString());
        IntentUtils.startActivity(getActivity(), CustomerDetails.class, data);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ODataRow row = OCursorUtils.toDatarow((Cursor) mAdapter.getItem(position));
        loadActivity(row);
    }
}