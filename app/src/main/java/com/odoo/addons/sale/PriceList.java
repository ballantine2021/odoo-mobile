package com.odoo.addons.sale;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by cracker
 * Created on 10/1/22.
 */

public class PriceList extends OModel {
    public static final String TAG = PriceList.class.getSimpleName();
    OColumn name = new OColumn("name", OVarchar.class);

    public PriceList(Context context, OUser user) {
        super(context, "product.pricelist", user);
    }
}
