package com.odoo.addons.stock;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by cracker
 * Created on 10/2/22.
 */

public class ProductCategory extends OModel {

    public static final String TAG = ProductCategory.class.getSimpleName();
    OColumn name = new OColumn("name", OVarchar.class);
    OColumn complete_name = new OColumn("complete name", OVarchar.class);
    OColumn display_name = new OColumn("display name", OVarchar.class);

    public ProductCategory(Context context, OUser user) {
        super(context, "product.category", user);
        setDefaultNameColumn("display_name");
    }
}
