package com.odoo.addons.account;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.odoo.App;
import com.odoo.R;
import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.Odoo;
import com.odoo.core.rpc.handler.OdooVersionException;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.listeners.IOdooConnectionListener;
import com.odoo.core.rpc.listeners.OdooError;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OAppBarUtils;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OPreferenceManager;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import odoo.controls.ExpandableListControl;
import odoo.controls.OField;
import odoo.controls.OForm;

public class CustomerPayments extends AppCompatActivity implements View.OnClickListener, IOdooConnectionListener,OField.IOnFieldValueChangeListener {
    public static final String TAG = CustomerPayments.class.getSimpleName();
    private OForm oForm;
    private App app;
    private OUser user;
    private AccountBankStatementLine absl;
    private AccountBankStatement abs;
    private AccountJournal aj;
    private ResPartner rp;
    private Bundle extra;
    private int partnerId;
    private OField date_from;
    private OField date_to;
    private ExpandableListControl.ExpandableListAdapter mAdapter;
    private final List<Object> objects = new ArrayList<>();
    private DecimalFormat decimalFormat1;
    private float paid_amount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customer_payments);
        OAppBarUtils.setAppBar(this, true);
        app = (App) getApplicationContext();
        user = OUser.current(this);
        abs = new AccountBankStatement(getBaseContext(), null);
        absl = new AccountBankStatementLine(getBaseContext(), null);
        aj = new AccountJournal(getBaseContext(), null);
        rp = new ResPartner(getBaseContext(), null);

        oForm = (OForm) findViewById(R.id.customerPaymentForm);
        oForm.initForm(null);
        oForm.setEditable(true);

        extra = getIntent().getExtras();
        partnerId = (int) extra.get("partner_id");
        setTitle(rp.browse(partnerId).getString("name"));

        date_from = (OField)findViewById(R.id.dateFrom);
        date_to = (OField) findViewById(R.id.dateTo);
        OPreferenceManager preferenceManager = new OPreferenceManager(getBaseContext());
        int date_limit = preferenceManager.getInt("sync_data_limit", 7);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -date_limit);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        decimalFormat1 = new DecimalFormat("#,##0.00",new DecimalFormatSymbols(Locale.getDefault()));
        date_from.setValue(dateFormat.format(cal.getTime()));

        date_from.setOnValueChangeListener((field, value) -> {
            initAdapter();
        });

        date_to.setOnValueChangeListener((field, value) -> {
            initAdapter();
        });
        initAdapter();
    }

    private void initAdapter() {

        final ExpandableListControl mList = (ExpandableListControl) findViewById(R.id.bankStatementLineDetail);
        mList.setVisibility(View.VISIBLE);
        List<ODataRow> lines = absl.query("SELECT absl.partner_id, absl.date, absl.journal_id, aj.name AS journal_name, absl.payment_ref, absl.amount " +
                "FROM account_bank_statement_line absl " +
                "LEFT JOIN account_journal aj ON aj._id = absl.journal_id " +
                "WHERE absl.partner_id = ? AND absl.date BETWEEN ? AND ? ORDER BY absl.date DESC",
                new String[]{Integer.toString(partnerId),
                String.valueOf(date_from.getValue()),
                String.valueOf(date_to.getValue())});

        objects.clear();
        objects.addAll(lines);
        mAdapter = mList.getAdapter(R.layout.bank_statement_line_item2, objects,
                (position, mView, parent) -> {
                    final ODataRow row = (ODataRow) mAdapter.getItem(position);
                    OControls.setText(mView, R.id.bankStatementLineJournalName, row.getString("journal_name"));
                    OControls.setText(mView, R.id.bankStatementLineDate, row.getString("date"));
                    OControls.setText(mView, R.id.bankStatementLineRef, row.getString("payment_ref"));
                    OControls.setText(mView, R.id.bankStatementLineAmount,  decimalFormat1.format(row.getFloat("amount")));
                    return mView;
                });
        mAdapter.notifyDataSetChanged(objects);

        paid_amount = 0;
        for(ODataRow oDataRow : lines){
            paid_amount += oDataRow.getFloat("amount");
        }
        TextView tvTotalAmount = (TextView) findViewById(R.id.totalAmount);
        String currencySymbol = "₮";
        ResCurrency resCurrency = new ResCurrency(getBaseContext(), user);
        ResCompany resCompany = new ResCompany(getBaseContext(), user);
        for(ODataRow company : resCompany.query("SELECT * FROM res_company")){
            for(ODataRow currency : resCurrency.query("SELECT * FROM res_currency WHERE _id = ?",
                    new String[]{company.getString("currency_id")})){
                currencySymbol = currency.getString("symbol");
            }
        }
        tvTotalAmount.setText(String.format("%s%s", decimalFormat1.format(paid_amount), currencySymbol));
        checkConnection();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
    }

    private void checkConnection(){
        if(app.inNetwork()) {
            new Handler().postDelayed(() -> {
                try {
                    Odoo.createInstance(getApplicationContext(), user.getHost()).setOnConnect(CustomerPayments.this);
                } catch (OdooVersionException e) {
                    e.printStackTrace();
                }
            }, 50);
        }
    }

    @Override
    public void onConnect(Odoo odoo) {
        OnPaymentBalanceSync onPaymentBalanceSync = new OnPaymentBalanceSync();
        onPaymentBalanceSync.execute(oForm.getValues());
    }

    @Override
    public void onError(OdooError error) {
    }

    @Override
    public void onFieldValueChange(OField field, Object value) {

    }

    private class OnPaymentBalanceSync extends AsyncTask<OValues, Integer, String> {
        private ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(CustomerPayments.this);
            progressDialog.setTitle(R.string.title_please_wait);
            progressDialog.setMessage(getString(R.string.title_downloading_data));
            progressDialog.setCancelable(false);
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(OValues... params) {
            OValues values1 = params[0];
            String result = null;
            try {
                int partner_server_id = -1;
                partner_server_id = rp.selectServerId(partnerId);
                JSONObject data = new JSONObject();
                data.put("date_from", values1.getString("date_from"));
                data.put("date_to", values1.getString("date_to"));
                data.put("partner_id", partner_server_id);
                OArguments args1 = new OArguments();
                args1.add(partner_server_id);
                args1.add(data);
                result = abs.getServerDataHelper().callMethodCracker("confirmed_sales_amount_sync_mobile", args1);
            }catch (Exception e){
                Log.d(TAG, "doInBackground e: " + e.toString());
            }

            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            try {
                float sales_amount = 0;
                float balance_amount = 0;
                if(result != null) {
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.has("result")) {
                        JSONObject response = new JSONObject(jsonObject.getString("result"));
                        if (response.has("success")) {
                            if (response.getString("success").equals("true")) {
                                sales_amount = (float) response.getDouble("sales_amount");
                                balance_amount = sales_amount - paid_amount;
                            }
                        }
                    }
                }
                TextView tvBalanceAmount = (TextView) findViewById(R.id.totalBalance);
                String currencySymbol = "₮";
                ResCurrency resCurrency = new ResCurrency(getBaseContext(), user);
                ResCompany resCompany = new ResCompany(getBaseContext(), user);
                for(ODataRow company : resCompany.query("SELECT * FROM res_company")){
                    for(ODataRow currency : resCurrency.query("SELECT * FROM res_currency WHERE _id = ?",
                            new String[]{company.getString("currency_id")})){
                        currencySymbol = currency.getString("symbol");
                    }
                }
                tvBalanceAmount.setText(String.format("%s%s", decimalFormat1.format(balance_amount), currencySymbol));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onPostExecute(result);
        }
    }
}

