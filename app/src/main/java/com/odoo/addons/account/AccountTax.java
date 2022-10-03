package com.odoo.addons.account;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by cracker
 * Created on 10/2/22.
 */

public class AccountTax extends OModel {
    public static final String TAG = AccountTax.class.getSimpleName();

    OColumn name = new OColumn("name", OVarchar.class);
    OColumn price_include = new OColumn("price_include", OBoolean.class);
    OColumn amount_type = new OColumn("amount_type", OSelection.class).setSize(40)
            .addSelection("fixed","Fixed")
            .addSelection("group","Taxes Group")
            .addSelection("percent","Price Percent")
            .addSelection("division","Division");
    OColumn amount = new OColumn("amount", OFloat.class);


    public AccountTax(Context context, OUser user) {
        super(context, "account.tax", user);
        Context mContext = context;
    }
}
