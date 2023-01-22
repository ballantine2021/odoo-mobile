package com.printing;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.Base64;
import android.util.Log;

import com.odoo.R;
import com.odoo.core.orm.ODataRow;

import java.io.ByteArrayOutputStream;
import java.util.List;


public class BluetoothPrinter {
    private static final String TAG = BluetoothPrinter.class.getSimpleName();
    private static final int PRODUCT_COL = 5;
    private static final int PRICE_COL = 270;
    private static final int QTY_COL = 340;
    private static final int SUBTOTAL_COL = 450;
    private static final int CANVAS_WIDTH = 460;
    private static final int HEIGHT_OFFSET = 480;
    Context context;

    public BluetoothPrinter(Context context) {
        this.context = context;

    }

    public String PrintSaleOrder(ODataRow so, List<ODataRow> order_lines){
        Resources resources = this.context.getResources();
//        mService.sendMessage("Шалгаж байна! өглөө өглүү?","UTF-16");
        float scale = resources.getDisplayMetrics().density;
        int bmp_height = HEIGHT_OFFSET + order_lines.size() * 35;
        Bitmap bitmap = Bitmap.createBitmap(CANVAS_WIDTH, bmp_height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.BLACK);
        // text size in pixels
        paint.setTextSize((int) (23));

        Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numPaint.setColor(Color.BLACK);
        numPaint.setTextSize((int) (23));
        numPaint.setTextAlign(Paint.Align.RIGHT);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize((int) (26));
        titlePaint.setTextAlign(Paint.Align.CENTER);


        String salesman = so.getM2ORecord("user_id").browse().getString("name");

        int y = 80;
        canvas.drawText(resources.getString(R.string.title_receipt), CANVAS_WIDTH/2, y, titlePaint);
        y += 50;
        canvas.drawText(resources.getString(R.string.company_name), 5, y, paint);
        y += 40;
        canvas.drawText(resources.getString(R.string.label_sale_detail_date) + ": "
                        + so.getString("date_order"),5, y, paint);
        canvas.drawText(resources.getString(R.string.label_sale_detail_number) + ": "
                       + so.getString("name"), 300, y, paint);
        y += 40;
        canvas.drawText(resources.getString(R.string.label_sale_detail_customer) + ": "
                        + so.getString("partner_name"), 5, y, paint);
        y += 40;
        canvas.drawText(resources.getString(R.string.label_sale_detail_salesman) + ": "
                        + salesman, 5, y, paint);
        y += 30;
        // text shadow
//        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
        Paint dashedPaint = new Paint();
        dashedPaint.setARGB(255, 0, 0, 0);
        dashedPaint.setStyle(Paint.Style.STROKE);
        dashedPaint.setPathEffect(new DashPathEffect(new float[]{10f, 20f}, 0f));

        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);

        y += 25;
//        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(resources.getString(R.string.label_receipt_product), PRODUCT_COL, y, paint);
        canvas.drawText(resources.getString(R.string.label_receipt_price_unit), PRICE_COL, y, numPaint);
        canvas.drawText(resources.getString(R.string.label_receipt_qty), QTY_COL, y, numPaint);
        canvas.drawText(resources.getString(R.string.label_receipt_subtotal), SUBTOTAL_COL, y, numPaint);


        y += 8;
        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);

        for (ODataRow row : order_lines){
            y += 35;
            String productName = row.getString("name");
            if (productName.length() > 15) {
                productName = productName.substring(0,12);
            }
            canvas.drawText(productName, PRODUCT_COL, y, paint);
            int priceUnit = row.getFloat("price_unit").intValue();
            int qty = row.getFloat("product_uom_qty").intValue();
            int subtotal = row.getFloat("price_total").intValue();
            canvas.drawText(String.format("%,d", priceUnit), PRICE_COL, y, numPaint);
            canvas.drawText(String.valueOf(qty), QTY_COL, y, numPaint);
            canvas.drawText(String.format("%,d", subtotal), SUBTOTAL_COL, y, numPaint);
        }
        y += 5;
        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);
        y += 30;
        canvas.drawText(resources.getString(R.string.label_total_amount), 200, y, paint);
        int total = so.getFloat("amount_total").intValue();
        canvas.drawText(String.format("%,d",total), SUBTOTAL_COL, y, numPaint);
        y += 60;
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(resources.getString(R.string.footer_receipt_received), CANVAS_WIDTH/2, y, paint);
        y += 50;
        canvas.drawText(resources.getString(R.string.footer_receipt_delivered), CANVAS_WIDTH/2, y, paint);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream); // bm is the bitmap object
        String strBytes = "<IMAGE>1#"+Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
        return strBytes;

    }



}
