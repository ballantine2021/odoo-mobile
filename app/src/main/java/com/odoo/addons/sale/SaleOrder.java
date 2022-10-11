package com.odoo.addons.sale;

import android.content.Context;

import com.odoo.addons.account.AccountPaymentTerm;
import com.odoo.addons.stock.StockWarehouse;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODateTime;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by cracker
 * Created on 10/8/22.
 */

public class SaleOrder extends OModel {

    OColumn name = new OColumn("Name", OVarchar.class).setSize(100);
    OColumn partner_id = new OColumn("Customer", ResPartner.class, OColumn.RelationType.ManyToOne).setRequired().addDomain("id", "!=", 0);
    OColumn partner_name = new OColumn("CustomerName", OVarchar.class).setLocalColumn().setDefaultValue("");
    OColumn date_order = new OColumn("Order Date", ODateTime.class);
    OColumn validity_date = new OColumn("Expiration Date", ODateTime.class);
    OColumn state = new OColumn("status", OVarchar.class).setSize(10).setDefaultValue("draft");
    OColumn payment_term_id= new OColumn("Payment Term", AccountPaymentTerm.class, OColumn.RelationType.ManyToOne);
    OColumn user_id = new OColumn("Salesperson", ResUsers.class, OColumn.RelationType.ManyToOne);
    OColumn amount_total = new OColumn("Total", OFloat.class);
    OColumn amount_untaxed = new OColumn("Untaxed", OInteger.class);
    OColumn amount_tax = new OColumn("Tax", OInteger.class);
    OColumn pricelist_id = new OColumn("Price List", PriceList.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn currency_id = new OColumn("Currency", ResCurrency.class, OColumn.RelationType.ManyToOne);
    OColumn currency_symbol = new OColumn("Currency Symbol", OVarchar.class).setLocalColumn();
    OColumn warehouse_id = new OColumn("Warehouse", StockWarehouse.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn order_line = new OColumn("Order Lines", SaleOrderLine.class, OColumn.RelationType.OneToMany).setRelatedColumn("order_id");

    public SaleOrder(Context context, OUser user) {
        super(context, "sale.order", user);
    }

}
