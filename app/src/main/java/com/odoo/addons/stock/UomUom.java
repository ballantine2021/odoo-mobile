package com.odoo.addons.stock;

/**
 * Created by cracker
 * Created on 10/2/22.
 */

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

public class UomUom extends OModel {

    OColumn display_name = new OColumn("Display Name", OVarchar.class);
    OColumn factor = new OColumn("Ratio", OFloat.class);
    OColumn factor_inv = new OColumn("Bigger Ratio", OFloat.class);
    OColumn name = new OColumn("Unit of Measure", OVarchar.class);
    OColumn rounding = new OColumn("Rounding Precision", OFloat.class);
    OColumn uom_type = new OColumn("Type", OVarchar.class);

    public UomUom(Context context, OUser user) {
        super(context, "uom.uom", user);
    }
}