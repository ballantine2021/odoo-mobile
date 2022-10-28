/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * <p>
 * Created on 9/1/15 11:54 AM
 */
package com.odoo.addons.account;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.R;
import com.odoo.addons.sale.SalesDetail;
import com.odoo.addons.stock.StockWarehouse;
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
import com.odoo.core.utils.OResource;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import odoo.controls.OField;
import odoo.controls.OForm;

public class PaymentRegister extends AppCompatActivity implements View.OnClickListener, IOdooConnectionListener,OField.IOnFieldValueChangeListener {
    public static final String TAG = PaymentRegister.class.getSimpleName();
    private Button paymentValidate;
    private OForm oForm;
    private OField journal_id;
    private OField partner_id;
    private OField payment_ref;
    private EditText amountText;
    private App app;
    private OUser user;
    private AccountBankStatementLine absl;
    private AccountBankStatement abs;
    private AccountJournal aj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_register);
        OAppBarUtils.setAppBar(this, true);
        app = (App) getApplicationContext();
        user = OUser.current(this);
        abs = new AccountBankStatement(getBaseContext(), null);
        absl = new AccountBankStatementLine(getBaseContext(), null);
        aj = new AccountJournal(getBaseContext(), null);
        oForm = (OForm) findViewById(R.id.paymentForm);
        oForm.initForm(null);
        oForm.setEditable(true);

        setTitle(getBaseContext().getString(R.string.title_activity_bank_statement_regist));

        journal_id = (OField) findViewById(R.id.journalId);
        partner_id = (OField) findViewById(R.id.partnerId);
        payment_ref = (OField) findViewById(R.id.paymentRef);
        amountText = (EditText) findViewById(R.id.account_payment_amount);
        paymentValidate = (Button) findViewById(R.id.paymentValidate);
        paymentValidate.setOnClickListener(this);

        partner_id.setVisibility(View.GONE);
        payment_ref.setVisibility(View.GONE);
        amountText.setVisibility(View.GONE);
        paymentValidate.setVisibility(View.GONE);

        journal_id.setOnValueChangeListener((field, value) -> {
            if(Integer.valueOf((Integer) journal_id.getValue()) != 0){
                partner_id.setVisibility(View.VISIBLE);
                payment_ref.setVisibility(View.VISIBLE);
                amountText.setVisibility(View.VISIBLE);
                paymentValidate.setVisibility(View.VISIBLE);
                paymentValidate.setEnabled(true);
            }
            else{
                paymentValidate.setEnabled(false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.paymentValidate:
                if(0 != Integer.valueOf((Integer) journal_id.getValue())) {
                    paymentValidate.setEnabled(false);
                    try{
                        Float.parseFloat(amountText.getText().toString());
                        checkConnection();
                    }catch(NumberFormatException e){
                        Toast.makeText(getBaseContext(),"Төлбөрийн дүн буруу утга оруулсан байна.", Toast.LENGTH_LONG).show();
                        paymentValidate.setEnabled(true);
                    }
                }
                else
                {
                    Toast.makeText(getBaseContext(),"Та борлуулалтын журнал тохируулж өгнө үү!", Toast.LENGTH_LONG).show();
                    paymentValidate.setEnabled(true);
                }
                break;
        }
    }

    private void checkConnection(){
        if(app.inNetwork()) {
            new Handler().postDelayed(() -> {
                try {
                    Odoo.createInstance(getApplicationContext(), user.getHost()).setOnConnect(PaymentRegister.this);
                } catch (OdooVersionException e) {
                    e.printStackTrace();
                }
            }, 50);

        }else{
            Toast.makeText(getBaseContext(), getText(R.string.toast_network_required), Toast.LENGTH_LONG).show();
            paymentValidate.setEnabled(true);
        }
    }

    private void clearScreen(){
        journal_id.setValue(0);
        partner_id.setVisibility(View.GONE);
        partner_id.setValue(0);
        payment_ref.setVisibility(View.GONE);
        payment_ref.setValue("");
        amountText.setVisibility(View.GONE);
        amountText.setText("");
        paymentValidate.setVisibility(View.GONE);
        paymentValidate.setEnabled(false);
    }

    @Override
    public void onConnect(Odoo odoo) {
        OnPaymentSync onPaymentSync = new OnPaymentSync();
        onPaymentSync.execute(oForm.getValues());
    }

    @Override
    public void onError(OdooError error) {
        Toast.makeText(getBaseContext(),getText(R.string.toast_not_connect_server), Toast.LENGTH_SHORT).show();
        paymentValidate.setEnabled(true);
    }

    @Override
    public void onFieldValueChange(OField field, Object value) {

    }

    private class OnPaymentSync extends AsyncTask<OValues, Integer, String> {
        private ProgressDialog progressDialog;
        private ResPartner rp = new ResPartner(getBaseContext(),null);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(PaymentRegister.this);
            progressDialog.setTitle(R.string.title_please_wait);
            progressDialog.setMessage(OResource.string(PaymentRegister.this, R.string.title_sent_to_server));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @SuppressLint("WrongThread")
        @Override
        protected String doInBackground(OValues... params) {
            OValues values1 = params[0];
            String result = null;
            try {
                int journal_server_id = -1;
                if (!values1.getString("journal_id").equals("false"))
                    journal_server_id = aj.selectServerId(values1.getInt("journal_id"));

                int partner_server_id = -1;
                if (!values1.getString("partner_id").equals("false"))
                    partner_server_id = rp.selectServerId(values1.getInt("partner_id"));

                JSONObject data = new JSONObject();
                data.put("date", values1.getString("date"));
                data.put("journal_id", journal_server_id > 0 ? journal_server_id:false);
                data.put("partner_id", partner_server_id > 0 ? partner_server_id:false);
                data.put("payment_ref", values1.getString("payment_ref"));
                data.put("amount", Float.parseFloat(amountText.getText().toString()));

                OArguments args1 = new OArguments();
                args1.add(journal_server_id);
                args1.add(data);
                result = abs.getServerDataHelper().callMethodCracker("bank_statement_create_mobile", args1);

            }catch (Exception e){
                Log.d(TAG, "doInBackground e: " + e.toString());
            }

            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            try {
                if(result != null) {
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.has("result")) {
                        JSONObject response = new JSONObject(jsonObject.getString("result"));
                        if (response.has("success")) {
                            if (response.getString("success").equals("true")) {
                                clearScreen();
                            }
                            Toast.makeText(getApplicationContext(), response.getString("message"), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onPostExecute(result);
        }
    }
}

