package com.odoo.addons.stock;

import android.content.Context;

import com.odoo.addons.account.AccountTax;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBlob;
import com.odoo.core.orm.fields.types.ODateTime;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by cracker
 * Created on 10/2/22.
 */

public class ProductProduct extends OModel {

    public static final String TAG = ProductProduct.class.getSimpleName();

    OColumn name = new OColumn("Display Name", OVarchar.class).setSize(64);
    OColumn default_code = new OColumn("Internal Reference", OVarchar.class).setSize(64);
    OColumn list_price = new OColumn("Unit price", OInteger.class);
    OColumn type = new OColumn("Product Type", OVarchar.class);
    OColumn barcode = new OColumn("ean13", OVarchar.class);

    OColumn categ_id = new OColumn("Category ID", ProductCategory.class, OColumn.RelationType.ManyToOne);
    OColumn uom_id = new OColumn("uom_id", UomUom.class, OColumn.RelationType.ManyToOne);
    OColumn taxes_id = new OColumn("Taxes ID", AccountTax.class, OColumn.RelationType.ManyToMany);
    OColumn image_1920 = new OColumn("Avatar", OBlob.class).setDefaultValue(false);
    OColumn write_date = new OColumn("Write Date", ODateTime.class);

    public ProductProduct(Context context, OUser user) {
        super(context, "product.product", user);
        setDefaultNameColumn("name");
    }
}
