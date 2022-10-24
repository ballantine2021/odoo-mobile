package com.odoo.addons.account;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODate;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

public class AccountBankStatement extends OModel {
    public static final String TAG = AccountBankStatement.class.getSimpleName();

    OColumn name = new OColumn("Name", OVarchar.class);
    OColumn journal_id = new OColumn("Journal",  AccountJournal.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn journal_name = new OColumn("Journal name", OVarchar.class).setLocalColumn().setDefaultValue("");
    OColumn date = new OColumn("Date", ODate.class).setRequired();
    OColumn balance_start = new OColumn("Starting Balance", OFloat.class);
    OColumn balance_end = new OColumn("Ending Balance", OFloat.class);
    OColumn state = new OColumn("State", OSelection.class).addSelection("open","New")
            .addSelection("posted","Processing")
            .addSelection("confirm","Validated");
    OColumn currency_symbol = new OColumn("Currency Symbol", OVarchar.class).setLocalColumn();

    public AccountBankStatement(Context context, OUser user) {
        super(context, "account.bank.statement", user);
        Context mContext = context;
    }
}
