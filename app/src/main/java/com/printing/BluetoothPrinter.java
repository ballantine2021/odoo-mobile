package com.printing;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Base64;
import android.util.Log;

import com.odoo.R;
import com.odoo.core.orm.ODataRow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;


public class BluetoothPrinter {
    private static final String TAG = BluetoothPrinter.class.getSimpleName();

//    private static final String FILENAME = "receipt.png";
    private static final int NUM_COL = 3;
    private static final int PRODUCT_COL = 30;
    private static final int PRICE_COL = 300;
    private static final int QTY_COL = 350;
    private static final int SUBTOTAL_COL = 450;
    private static final int CANVAS_WIDTH = 460;
    private static final int HEIGHT_OFFSET = 950;
    Context context;

    public BluetoothPrinter(Context context) {
        this.context = context;

    }

    public String PrintSOTitle(){
        Resources resources = this.context.getResources();
        Bitmap bitmap = Bitmap.createBitmap(CANVAS_WIDTH, 160, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable logo = ResourcesCompat.getDrawable(resources, R.drawable.torlon_logo, null);

        int y = 30;
        Typeface boldFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize((int) (26));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(boldFont);
        canvas.drawText(resources.getString(R.string.title_receipt), CANVAS_WIDTH/2, y, titlePaint);

        y += 5;
        logo.setBounds(30, y, CANVAS_WIDTH-30,y+120);
        logo.draw(canvas);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream); // bm is the bitmap object
        String strBytes = "<IMAGE>1#"+Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
        return strBytes;

    }

    public String PrintSaleOrder(ODataRow so, List<ODataRow> order_lines, int salesman_id){
        Resources resources = this.context.getResources();
//        float scale = resources.getDisplayMetrics().density;
        int bmp_height = HEIGHT_OFFSET + order_lines.size() * 35;
        Bitmap bitmap = Bitmap.createBitmap(CANVAS_WIDTH, bmp_height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
//        Drawable logo = ResourcesCompat.getDrawable(resources, R.drawable.torlon_logo, null);
        Typeface boldFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        Typeface normalFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.BLACK);
        // text size in pixels
        paint.setTextSize((int) (23));

        Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numPaint.setColor(Color.BLACK);
        numPaint.setTextSize((int) (23));
        numPaint.setTextAlign(Paint.Align.RIGHT);

/*        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize((int) (26));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(boldFont);*/


        String salesman = so.getM2ORecord("user_id").browse().getString("name");

        int y = 30;
//        canvas.drawText(resources.getString(R.string.title_receipt), CANVAS_WIDTH/2, y, titlePaint);
//        y += 10;
//        canvas.drawText(resources.getString(R.string.company_name), 5, y, paint);
 /*       logo.setBounds(30, y, CANVAS_WIDTH-30,y+120);
        logo.draw(canvas);
        y += 150;*/
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
        paint.setTypeface(boldFont);
        canvas.drawText("Код: " + String.valueOf(salesman_id), 350, y, paint);
        paint.setTypeface(normalFont);
        y += 40;
        canvas.drawText(resources.getString(R.string.label_receipt_address), 5, y, paint);
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
        canvas.drawText(resources.getString(R.string.label_sale_detail_number), NUM_COL, y, paint);
        canvas.drawText(resources.getString(R.string.label_receipt_product), PRODUCT_COL, y, paint);
        canvas.drawText(resources.getString(R.string.label_receipt_price_unit), PRICE_COL, y, numPaint);
        canvas.drawText(resources.getString(R.string.label_receipt_qty), QTY_COL, y, numPaint);
        canvas.drawText(resources.getString(R.string.label_receipt_subtotal), SUBTOTAL_COL, y, numPaint);


        y += 8;
        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);
        int num = 1;

        for (ODataRow row : order_lines){
            y += 35;
            String productName = row.getString("name");
            if (productName.length() > 15) {
                productName = productName.substring(0,12);
            }
            canvas.drawText(String.valueOf(num), NUM_COL, y, paint);
            canvas.drawText(productName, PRODUCT_COL, y, paint);
            int priceUnit = row.getFloat("price_unit").intValue();
            int qty = row.getFloat("product_uom_qty").intValue();
            int subtotal = row.getFloat("price_total").intValue();
            canvas.drawText(String.format("%,d", priceUnit), PRICE_COL, y, numPaint);
            canvas.drawText(String.valueOf(qty), QTY_COL, y, numPaint);
            canvas.drawText(String.format("%,d", subtotal), SUBTOTAL_COL, y, numPaint);
            num += 1;
        }
        y += 5;
        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);
        y += 30;
        canvas.drawText(resources.getString(R.string.label_total_amount), 200, y, paint);
        int total = so.getFloat("amount_total").intValue();
        canvas.drawText(String.format("%,d",total), SUBTOTAL_COL, y, numPaint);
        y += 20;
        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);

        y += 35;
        canvas.drawText(resources.getString(R.string.footer_receipt_delivered), 5, y, paint);
        canvas.drawText(salesman, 200, y, paint);
        y += 35;
        canvas.drawText(resources.getString(R.string.footer_receipt_received), 5, y, paint);
        y += 50;
        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);

        y += 25;
        canvas.drawText(resources.getString(R.string.label_text_bank_account), 5, y, paint);
        y += 30;
        canvas.drawText("-Хаан банк: 5246113939", 20, y, paint);
        y += 30;
        canvas.drawText("-Хас банк: 5000153327", 20, y, paint);

        y += 35;
        canvas.drawText("Дансаар төлбөр шилжүүлэх тохиолдолд", 5, y, paint);
        y += 30;
        canvas.drawText("гүйлгээний утга хэсэгт зарлагын", 5, y, paint);
        y += 30;
        canvas.drawText("баримт дээрхи борлуулагчийн кодыг", 5, y, paint);
        y += 30;
        canvas.drawText("заавал бичиж илгээнэ үү!", 5, y, paint);
        y += 50;
        canvas.drawText("ХАМТРАН АЖИЛЛАСАН", 100, y, paint);
        y += 30;
        canvas.drawText("ТАНАЙ ХАМТ ОЛОНД БАЯРЛАЛАА!", 30, y, paint);
        y += 10;
        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);


/*        File path = context.getExternalFilesDir(null);
        File file = new File(path, FILENAME);
        try (FileOutputStream out = new FileOutputStream(file)) {
            boolean res = bitmap.compress(Bitmap.CompressFormat.PNG, 50, out);

        } catch(Exception e){
            e.printStackTrace();
        }*/

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream); // bm is the bitmap object
        String strBytes = "<IMAGE>1#"+Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
        return strBytes;

    }



}
