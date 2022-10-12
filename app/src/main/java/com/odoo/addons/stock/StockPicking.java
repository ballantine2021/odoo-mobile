package com.odoo.addons.stock;

import android.content.Context;

import com.odoo.R;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODateTime;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by cracker
 * Created on 10/12/22.
 */

public class StockPicking extends OModel {

    OColumn name = new OColumn("", OVarchar.class);
    public StockPicking(Context context, OUser user) {
        super(context, "stock.picking", user);
    }
}
