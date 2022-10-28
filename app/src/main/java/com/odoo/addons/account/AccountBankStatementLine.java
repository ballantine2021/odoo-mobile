package com.odoo.addons.account;

import android.content.Context;

import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODate;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

public class AccountBankStatementLine extends OModel {
    public static final String TAG = AccountBankStatementLine.class.getSimpleName();

    OColumn date = new OColumn("Date", ODate.class).setRequired();
    OColumn payment_ref = new OColumn("", OVarchar.class).setRequired();
    OColumn partner_id = new OColumn("Partner", ResPartner.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn amount = new OColumn("", OFloat.class).setRequired();
    OColumn statement_id = new OColumn("Statement",  AccountBankStatement.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn journal_id = new OColumn("Journal",  AccountJournal.class, OColumn.RelationType.ManyToOne);

    public AccountBankStatementLine(Context context, OUser user) {
        super(context, "account.bank.statement.line", user);
        Context mContext = context;
    }
}
