package com.odoo.base.addons.ir;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by cracker
 * Created on 10/6/22.
 */

public class IrModuleModule extends OModel {
    public static final String TAG = IrModuleModule.class.getSimpleName();
    OColumn name = new OColumn("Model Description", OVarchar.class).setSize(50);
    OColumn state = new OColumn("State", OVarchar.class).setSize(20);

    public IrModuleModule(Context context, OUser user) {
        super(context, "ir.module.module", user);
    }

    @Override
    public boolean checkForCreateDate() {
        return false;
    }

    @Override
    public boolean checkForWriteDate() {
        return false;
    }
}
