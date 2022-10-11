package com.odoo.addons.sale;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

public class CrmTeam extends OModel {
    public static final String TAG = CrmTeam.class.getSimpleName();

    OColumn name = new OColumn("name", OVarchar.class);

    public CrmTeam(Context context, OUser user) {
        super(context, "crm.team", user);
    }
}
