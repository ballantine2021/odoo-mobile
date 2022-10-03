package com.odoo.addons.sale;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by cracker
 * Created on 10/2/22.
 */
public class SaleCategory extends OModel {
    public static final String TAG = SaleCategory.class.getSimpleName();

    OColumn name = new OColumn("name", OVarchar.class);
    OColumn description = new OColumn("Job Description", OText.class);

    public SaleCategory(Context context, OUser user) {
        super(context, "sale.category", user);
    }
}
