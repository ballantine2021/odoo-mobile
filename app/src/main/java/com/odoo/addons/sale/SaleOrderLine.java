package com.odoo.addons.sale;

import android.content.Context;

import com.odoo.addons.stock.ProductProduct;
import com.odoo.addons.stock.UomUom;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.support.OUser;

/**
 * Created by cracker
 * Created on 10/8/22.
 */

public class SaleOrderLine extends OModel {
    public static final String TAG = SaleOrderLine.class.getSimpleName();

    OColumn product_id = new OColumn("Product", ProductProduct.class, OColumn.RelationType.ManyToOne);
    OColumn name = new OColumn("Display Name", OText.class);
    OColumn product_uom_qty = new OColumn("Quantity", OFloat.class);
    OColumn price_unit = new OColumn("Unit Price", OFloat.class);
    OColumn price_tax = new OColumn("Tax", OFloat.class);
    OColumn price_subtotal = new OColumn("Sub Total", OFloat.class);
    OColumn price_total = new OColumn("Total", OFloat.class);
    OColumn order_id = new OColumn("ID", SaleOrder.class, OColumn.RelationType.ManyToOne);
    OColumn product_uom = new OColumn("Product Measure", UomUom.class, OColumn.RelationType.ManyToOne);

    public SaleOrderLine(Context context, OUser user) {
        super(context, "sale.order.line", user);
    }
}
