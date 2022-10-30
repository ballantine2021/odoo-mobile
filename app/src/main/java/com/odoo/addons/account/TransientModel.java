package com.odoo.addons.account;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODate;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

public class TransientModel extends OModel {
    public static final String TAG = TransientModel.class.getSimpleName();

    OColumn name = new OColumn("Name", OVarchar.class);
    OColumn date_from = new OColumn("Date From", ODate.class).setRequired();
    OColumn date_to = new OColumn("Date To", ODate.class).setRequired();


    public TransientModel(Context context, OUser user) {
        super(context, "transient.model", user);
        Context mContext = context;
    }
}
