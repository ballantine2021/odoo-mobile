package com.odoo.addons.account;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by cracker
 * Created on 10/1/22.
 */

public class AccountPaymentTerm extends OModel {
    public static final String TAG = AccountPaymentTerm.class.getSimpleName();

    OColumn name = new OColumn("Payment Term", OVarchar.class);

    public AccountPaymentTerm(Context context, OUser user) {
        super(context, "account.payment.term", user);
    }
}