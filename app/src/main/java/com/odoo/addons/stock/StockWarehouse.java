package com.odoo.addons.stock;

import android.content.Context;

import com.odoo.addons.account.AccountPaymentTerm;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

public class StockWarehouse extends OModel {
    public static final String TAG = StockWarehouse.class.getSimpleName();
    OColumn name = new OColumn("WareHouse Name", OVarchar.class);
    public StockWarehouse(Context context, OUser user) {
        super(context, "stock.warehouse", user);
    }
}
