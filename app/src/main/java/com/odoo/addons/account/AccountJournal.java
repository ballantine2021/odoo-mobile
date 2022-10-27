package com.odoo.addons.account;

import android.content.Context;

import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.OM2ORecord;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

public class AccountJournal extends OModel {
    public static final String TAG = AccountJournal.class.getSimpleName();

    OColumn name = new OColumn("Name", OVarchar.class);
    OColumn type = new OColumn("Type", OSelection.class).addSelection("sale","Sales")
            .addSelection("purchase","Purchase")
            .addSelection("cash","Cash")
            .addSelection("bank","Bank")
            .addSelection("general","Miscellaneous");

    public AccountJournal(Context context, OUser user) {
        super(context, "account.journal", user);
        Context mContext = context;
    }
}
