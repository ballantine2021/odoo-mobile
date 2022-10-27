package com.odoo.addons.account;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import com.odoo.R;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OControls;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import odoo.controls.ExpandableListControl;
import odoo.controls.OForm;

public class BankStatementsDetail extends AppCompatActivity {
    private Bundle extra;
    private OForm oForm;
    private ODataRow record = null;
    private AccountBankStatement abs;
    private ResPartner rp;
    private ActionBar actionBar;
    private ExpandableListControl.ExpandableListAdapter mAdapter;
    private final List<Object> objects = new ArrayList<>();
    private DecimalFormat decimalFormat1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bank_statement_detail);
        OActionBarUtils.setActionBar(this, true);
        actionBar = getSupportActionBar();
        Context context = this.getApplicationContext();
        OUser user = OUser.current(this);
        abs = new AccountBankStatement(context, user);
        rp = new ResPartner(context, user);
        extra = getIntent().getExtras();
        oForm = (OForm) findViewById(R.id.bankStatementForm);
        oForm.setEditable(false);
        decimalFormat1 = new DecimalFormat("#,##0.00",new DecimalFormatSymbols(Locale.getDefault()));
        init();
        initAdapter();
    }

    private void init() {
        record = abs.browse(extra.getInt(OColumn.ROW_ID));
        if(!record.getString("name").equals("false"))
            actionBar.setTitle(record.getString("name"));
        oForm.initForm(record);
        TextView tvTotalAmount = (TextView) findViewById(R.id.totalAmount);
        String currencySymbol = record.getString("currency_symbol");
        float totalAmount = record.getFloat("balance_end") - record.getFloat("balance_start");
        tvTotalAmount.setText(String.format("%s%s", decimalFormat1.format(totalAmount), currencySymbol));
    }

    private void initAdapter() {

        final ExpandableListControl mList = (ExpandableListControl) findViewById(R.id.bankStatementLine);
        mList.setVisibility(View.VISIBLE);
        if (extra != null && record != null) {
            List<ODataRow> lines = record.getO2MRecord("statement_lines").browseEach();
            objects.addAll(lines);
        }
        mAdapter = mList.getAdapter(R.layout.bank_statement_line_item, objects,
                (position, mView, parent) -> {
                    final ODataRow row = (ODataRow) mAdapter.getItem(position);
                    List<ODataRow> partnerList = rp.query("SELECT _id, id, name " +
                            "FROM res_partner " +
                            "WHERE _id = ?",new String[]{Integer.toString(row.getInt("partner_id"))});
                    if(partnerList.size() > 0)
                        OControls.setText(mView, R.id.bankStatementLinePartner, partnerList.get(0).getString("name"));
                    else
                        OControls.setText(mView, R.id.bankStatementLinePartner, R.string.label_bank_statement_line_not_partner);
                    OControls.setText(mView, R.id.bankStatementLineDate, row.getString("date"));
                    OControls.setText(mView, R.id.bankStatementLineRef, row.getString("payment_ref"));
                    OControls.setText(mView, R.id.bankStatementLineAmount,  decimalFormat1.format(row.getFloat("amount")));
                    return mView;
                });
        mAdapter.notifyDataSetChanged(objects);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public static class OActionBarUtils {
        public static void setActionBar(BankStatementsDetail activity, Boolean withHomeButtonEnabled) {
            Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
            if (toolbar != null) {
                activity.setSupportActionBar(toolbar);
                ActionBar actionBar = activity.getSupportActionBar();
                if (withHomeButtonEnabled) {
                    if (actionBar != null) {
                        actionBar.setHomeButtonEnabled(true);
                        actionBar.setDisplayHomeAsUpEnabled(true);
                    }
                }
            }
        }
    }
}

