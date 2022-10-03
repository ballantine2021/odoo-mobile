package com.odoo.addons.stock;

/**
 * Created by cracker
 * Created on 10/2/22.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.odoo.App;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.support.OdooCompatActivity;
import com.odoo.core.utils.BitmapUtils;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.OForm;

public class ProductDetails extends OdooCompatActivity {
    public static final String TAG = ProductDetails.class.getSimpleName();
    private final String KEY_MODE = "key_edit_mode";
    private Bundle extras;
    private ProductProduct pp;
    private ODataRow record = null;
    private OForm mForm;
    private App app;
    private Context context;
    private Boolean mEditMode = false;
    private Menu mMenu;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private Toolbar toolbar;
    private int rowId;
    private ImageView productImate = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_detail);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.product_collapsing_toolbar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        if (savedInstanceState != null) {
            mEditMode = savedInstanceState.getBoolean(KEY_MODE);
        }

        app = (App) getApplication();
        context = getApplicationContext();
        pp = new ProductProduct(this, null);
        extras = getIntent().getExtras();
        productImate = (ImageView) findViewById(R.id.image_1920);
        if (!hasRecordInExtra())
            mEditMode = true;
        setupToolbar();
//        OControls.setImage(view, R.id.image_small, img);
//        if (record != null && !record.getString("image_small").equals("false")) {
//            userImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
//            String base64 = newImage;
//            if (newImage == null) {
//                if (!record.getString("large_image").equals("false")) {
//                    base64 = record.getString("large_image");
//                } else {
//                    base64 = record.getString("image_small");
//                }
//            }
////            userImage.setImageBitmap(BitmapUtils.getBitmapImage(this, base64));
//        } else {
//            userImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
//            userImage.setColorFilter(Color.WHITE);
//            int color = OStringColorUtil.getStringColor(this, record.getString("name"));
//            userImage.setBackgroundColor(color);
//        }
    }

    private boolean hasRecordInExtra() {
        return extras != null && extras.containsKey(OColumn.ROW_ID);
    }

    private void setMode(Boolean edit) {
        mForm = (OForm) findViewById(R.id.productForm);
        mForm.setEditable(edit);
        findViewById(R.id.product_view_layout).setVisibility(View.VISIBLE);
    }

    private void setupToolbar() {
        if (!hasRecordInExtra()) {
            setMode(mEditMode);
            mForm.setEditable(mEditMode);
            mForm.initForm(null);
        } else {
            rowId = extras.getInt(OColumn.ROW_ID);
            record = pp.browse(rowId);
            Bitmap img;
            if (!record.getString("image_1920").equals("false")) {
                img = BitmapUtils.getBitmapImage(getApplicationContext(), record.getString("image_1920"));
                productImate.setImageBitmap(img);
            }
            setMode(mEditMode);
            mForm.setEditable(mEditMode);
            mForm.initForm(record);
            collapsingToolbarLayout.setTitle(record.getString("name"));
            List<String> taxesArray = new ArrayList<>();
            for(ODataRow oDataRow : record.getM2MRecord("taxes_id").browseEach()) {
                taxesArray.add(oDataRow.getString("name"));
            }
            if(taxesArray.size() > 0){
                LinearLayout taxesLayout = (LinearLayout) findViewById(R.id.taxesLayout);
                taxesLayout.setVisibility(View.VISIBLE);
                ArrayAdapter adapter = new ArrayAdapter<>(context, R.layout.taxes_list_item,taxesArray);
                NonScrollListView listView = (NonScrollListView) findViewById(R.id.taxesListView);
                listView.setAdapter(adapter);
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_detail, menu);
        mMenu = menu;
        setMode(mEditMode);
        return true;
    }
}
