package com.odoo.addons.sale;

import android.view.Menu;
import android.view.MenuItem;


/**
 * Created by cracker
 * Created on 10/8/22.
 */

public class BottomSheetListenersNew {
    public BottomSheetListenersNew() {
    }

    public interface OnSheetMenuCreateListener {
        void onSheetMenuCreate(Menu var1, Object var2);
    }

    public interface OnSheetActionClickListener {
        void onSheetActionClick(BottomSheetNew var1, Object var2);
    }

    public interface OnSheetItemClickListener {
        void onItemClick(BottomSheetNew var1, MenuItem var2, Object var3);
    }
}