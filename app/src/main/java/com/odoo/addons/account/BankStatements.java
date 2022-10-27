package com.odoo.addons.account;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
import com.odoo.base.addons.ir.IrModuleModule;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
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
import com.odoo.libs.calendar.SysCal;
import com.odoo.libs.calendar.view.OdooCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BankStatements extends BaseFragment implements OCursorListAdapter.OnViewBindListener,
        ISyncStatusObserverListener,
        LoaderManager.LoaderCallbacks<Cursor>, IOnItemClickListener, IOnSearchViewChangeListener,
        OdooCalendar.OdooCalendarDateSelectListener, IOdooConnectionListener {

    private static final String TAG = BankStatements.class.getSimpleName();
    private View mView;
    private OCursorListAdapter mAdapter;
    private String mFilter = null;

    private AccountBankStatement abs;
    private AccountBankStatementLine absl;
    private AccountJournal aj;
    private View calendarView = null;
    private final Date date = new Date();
    private String mFilterDate = date.toString();
    private ProgressDialog pd;
    private DecimalFormat decimalFormat;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        calendarView = LayoutInflater.from(getActivity()).inflate(R.layout.bank_statement_list,
                container, false);
        return inflater.inflate(R.layout.bank_statement_dashboard, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pd = new ProgressDialog(getActivity());
        pd.setTitle(R.string.title_please_wait);
        pd.setMessage(OResource.string(getActivity(), R.string.title_working));
        pd.setCancelable(false);

        mView = view;
        getLoaderManager().initLoader(0, null, this);
        abs = new AccountBankStatement(getContext(), null);
        absl = new AccountBankStatementLine(getContext(), null);
        aj = new AccountJournal(getContext(), null);

        OdooCalendar odooCalendar = (OdooCalendar) view.findViewById(R.id.dashboard_bank_statement);
        odooCalendar.setOdooCalendarDateSelectListener(this);
    }

    @Override
    public Class<AccountBankStatement> database() {
        return AccountBankStatement.class;
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
        calendarView = LayoutInflater.from(getActivity()).inflate(R.layout.bank_statement_list,
                viewGroup, false);
        ListView statementList = (ListView) calendarView.findViewById(R.id.items_container);
        mFilterDate = dateInfo.getDateString();
        mAdapter = new OCursorListAdapter(getActivity(), null, R.layout.bank_statement_list_item);
        mAdapter.setOnViewBindListener(this);
        statementList.setAdapter(mAdapter);
        statementList.setFastScrollAlwaysVisible(true);
        mAdapter.handleItemClickListener(statementList, this);
        setHasSyncStatusObserver(AccountBankStatement.class.getSimpleName(), this, db());
        if (getActivity() != null)
            getLoaderManager().restartLoader(0, null, BankStatements.this);
        return calendarView;
    }

    @Override
    public void onConnect(Odoo odoo) {
        OnBankStatementDownload onBankStatementDownload = new OnBankStatementDownload();
        onBankStatementDownload.execute();
    }

    @Override
    public void onError(OdooError error) {
        hideRefreshingProgress();
        Toast.makeText(getContext(),getText(R.string.toast_not_connect_server),Toast.LENGTH_SHORT).show();
    }

    private class OnBankStatementDownload extends AsyncTask<ODataRow, Void, Void> {
        ResPartner rp = new ResPartner(getContext(), null);

        protected void onPreExecute() {
            super.onPreExecute();
            pd.show();
        }

        @Override
        protected Void doInBackground(ODataRow... params) {
            ODomain statementDomain = new ODomain();
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add("bank");
            arrayList.add("cash");
            statementDomain.add("allowed_user_id", "=", user().getUserId());
            statementDomain.add("type", "in", arrayList);
            aj.quickSyncRecords(statementDomain);

            OPreferenceManager preferenceManager = new OPreferenceManager(getContext());
            int date_limit = preferenceManager.getInt("sync_data_limit", 7);
            absl.query("DELETE FROM account_bank_statement_line");
            abs.query("DELETE FROM account_bank_statement");
            OArguments args = new OArguments();
            args.add(getContext());
            args.add(ODateUtils.getDateBefore(date_limit));

            try {
                String response = abs.getServerDataHelper().callMethodCracker("get_bank_statement_list_mobile", args);
                if(response != null) {
                    JSONObject jsonObject = new JSONObject(response);
                    if(jsonObject.has("result")) {
                        JSONArray arr = jsonObject.getJSONArray("result");
                        if (arr.length() > 0) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                String journal_id = "false";

                                if (!obj.getString("journal_id").equals("null")) {
                                    List<ODataRow> journalList = selectRowId2(aj, obj.getInt("journal_id"));
                                    if (journalList.size() == 0) {
                                        quickSyncRecordOne(aj, obj.getInt("journal_id"));
                                        journalList = selectRowId2(aj, obj.getInt("journal_id"));
                                        if (journalList.size() > 0)
                                            journal_id = journalList.get(0).getString("_id");
                                    } else
                                        journal_id = journalList.get(0).getString("_id");
                                }

                                abs.query("INSERT INTO account_bank_statement (id, name, journal_id, journal_name, date, balance_start, balance_end, state, currency_symbol) " +
                                                "VALUES(?,?,?,?,?,?,?,?,?)",
                                        new String[]{
                                                obj.getString("id"),
                                                obj.getString("name"),
                                                String.valueOf(journal_id),
                                                obj.getString("journal_name"),
                                                obj.getString("date"),
                                                obj.getString("balance_start"),
                                                obj.getString("balance_end"),
                                                obj.getString("state"),
                                                obj.getString("currency_symbol")});

                                if (obj.getJSONArray("statement_lines").length() > 0) {
                                    int statement_id;
                                    final int COLUMNS_SIZE = 6;
                                    final int MAX_ROW = 999 / COLUMNS_SIZE;
                                    List<ODataRow> statementList = abs.query("SELECT _id FROM account_bank_statement WHERE id = ?", new String[]{obj.getString("id")});
                                    if(statementList.size() > 0){
                                        statement_id = statementList.get(0).getInt("_id");
                                        int counter = 0;
                                        JSONArray statement_line = obj.getJSONArray("statement_lines");
                                        List<JSONObject> tempList = new ArrayList<>();
                                        for (int l = 0; l < statement_line.length(); l++) {
                                            tempList.add(statement_line.getJSONObject(l));
                                            counter++;
                                            if (counter == MAX_ROW || statement_line.length() - 1 == l) {
                                                counter = 0;
                                                String sql = "INSERT INTO account_bank_statement_line (id, date, payment_ref, partner_id, amount, statement_id) " +
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
                                                    String partner_id = "false";

                                                    List<ODataRow> partnerList; // 1

                                                    if (!tempList.get(j).getString("partner_id").equals("null")) {
                                                        partnerList = rp.query("SELECT _id, id, name FROM res_partner WHERE id = ?", new String[]{tempList.get(j).getString("partner_id")});
                                                        if (partnerList.size() == 0) {
                                                            quickSyncRecordOne(rp, tempList.get(j).getInt("partner_id"));
                                                            partnerList = rp.query("SELECT _id, id, name FROM res_partner WHERE id = ?", new String[]{tempList.get(j).getString("partner_id")});
                                                            if (partnerList.size() > 0)
                                                                partner_id = partnerList.get(0).getString("_id");
                                                        } else
                                                            partner_id = partnerList.get(0).getString("_id");
                                                    }

                                                    arguments[j * COLUMNS_SIZE] = tempList.get(j).getString("id");
                                                    arguments[j * COLUMNS_SIZE + 1] = tempList.get(j).getString("date");
                                                    arguments[j * COLUMNS_SIZE + 2] = tempList.get(j).getString("payment_ref");
                                                    arguments[j * COLUMNS_SIZE + 3] = String.valueOf(partner_id);
                                                    arguments[j * COLUMNS_SIZE + 4] = tempList.get(j).getString("amount");
                                                    arguments[j * COLUMNS_SIZE + 5] = String.valueOf(statement_id);

                                                    if (j % MAX_ROW == MAX_ROW - 1 || (j == tempList.size() - 1)) {
                                                        absl.query(sql, arguments);
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
            getLoaderManager().restartLoader(0, null, BankStatements.this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_statements, menu);
        setHasSearchView(this, menu, R.id.menu_statement_search);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_statement_refresh) {
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
        OControls.setText(view, R.id.date, ODateUtils.convertToDefault(row.getString("date"), ODateUtils.DEFAULT_DATE_FORMAT, "MMMM, dd"));
        OControls.setText(view, R.id.amount_total, decimalFormat.format((row.getFloat("balance_end")-row.getFloat("balance_start"))));
        switch (row.getString("state")){
            case "open":
                OControls.setText(view, R.id.state, R.string.statement_state_open);
                break;
            case "posted":
                OControls.setText(view, R.id.state, R.string.statement_state_posted);
                break;
            case "confirm":
                OControls.setText(view, R.id.state, R.string.statement_state_confirm);
                break;
        }
        if(!row.getString("journal_name").equals("false")) {
            OControls.setVisible(view, R.id.journal_name);
            OControls.setText(view, R.id.journal_name, row.getString("journal_name"));
        }
        if(row.getString("currency_symbol").equals("false"))
            OControls.setGone(view, (R.id.currency_symbol));
        else
            OControls.setText(view, R.id.currency_symbol, row.get("currency_symbol"));
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
        if (inNetwork()) new Handler().postDelayed(() -> {
            try {
                Odoo.createInstance(getContext(), user().getHost()).setOnConnect(BankStatements.this);
            } catch (OdooVersionException e) {
                e.printStackTrace();
            }
        }, 500);
        else {
            hideRefreshingProgress();
            Toast.makeText(getActivity(), _s(R.string.toast_network_required), Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String where = "date(date) >= ? AND date(date) <= ?";
        List<String> arguments = new ArrayList<>();
        arguments.add(mFilterDate);
        arguments.add(mFilterDate);

        if (mFilter != null) {
            where += " AND (name LIKE ? or journal_name LIKE ?)";
            arguments.add("%" + mFilter + "%");
            arguments.add("%" + mFilter + "%");
        }
        return new CursorLoader(getActivity(), db().uri(),null,where,arguments.toArray(new String[0]),"name desc");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        try {
            if (data.getCount() > 0) {
                mAdapter.changeCursor(data);
                new Handler().postDelayed(() -> {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setVisible(calendarView, R.id.items_container);
                }, 500);
            } else {
                new Handler().postDelayed(() -> {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setGone(calendarView, R.id.items_container);
                }, 500);
            }

            decimalFormat = new DecimalFormat("#,##0.0",new DecimalFormatSymbols(Locale.getDefault()));
            TextView statementBalance = (TextView) calendarView.findViewById(R.id.DayBalance);
            float statementDayBalance = 0.f;
            String where = "";
            List<ODataRow> statements;
            if (mFilter != null) {
                where += " AND ((name like ?) OR (journal_name like ?))";
                statements = abs.query( "SELECT balance_end - balance_start as amount_total FROM account_bank_statement WHERE date(date) >=  ? AND date(date) <= ?" + where, new String[]{mFilterDate, mFilterDate, "%" + mFilter + "%", "%" + mFilter + "%"});
            }else {
                statements = abs.query( "SELECT balance_end - balance_start as amount_total FROM account_bank_statement WHERE date(date) >=  ? AND date(date) <= ?", new String[]{mFilterDate, mFilterDate});
            }
            if (statements.size() > 0){
                for (ODataRow row : statements) {
                    statementDayBalance += row.getFloat("amount_total");
                }
            }
            statements.clear();
            statementBalance.setText(decimalFormat.format(statementDayBalance));
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
        IntentUtils.startActivity(getActivity(), BankStatementsDetail.class, data);
    }

    @Override
    public void onItemClick(View view, int position) {
        ODataRow row = OCursorUtils.toDatarow((Cursor) mAdapter.getItem(position));
        Bundle data = row.getPrimaryBundleData();
        IntentUtils.startActivity(getActivity(), BankStatementsDetail.class, data);
    }


    public List<ODrawerItem> drawerMenus(Context context) {
        OUser user = OUser.current(context);

        IrModuleModule irModule = new IrModuleModule(context, null);
        if(irModule.select(null, "name = ?", new String[]{"mobile-backend"}).get(0).getString("state").equals("installed")) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("ACCOUNT_USER_ROLE", Context.MODE_PRIVATE);
            String role = sharedPreferences.getString("account-userId-"+user.getUserId(), "false");
            if (role.equals("true")) {
                List<ODrawerItem> items = new ArrayList<>();
                items.add(new ODrawerItem(TAG).setTitle(context.getString(R.string.title_activity_bank_statement))
                        .setIcon(R.drawable.ic_action_payment_list2)
                        .setInstance(new BankStatements()));
                return items;
            }
        }
        return null;
    }
}
