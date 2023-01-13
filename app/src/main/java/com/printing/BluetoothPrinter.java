package com.printing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.zj.btsdk.BluetoothService;
import com.zj.btsdk.PrintPic;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Set;

public class BluetoothPrinter {
    private static final String TAG = BluetoothPrinter.class.getSimpleName();
    private static final String FILENAME = "receipt.png";
    private static final int PRODUCT_COL = 5;
    private static final int PRICE_COL = 350;
    private static final int QTY_COL = 450;
    private static final int SUBTOTAL_COL = 550;
    private static final int CANVAS_WIDTH = 560;
    private static final int HEIGHT_OFFSET = 470;
    private int connectionStatus;
    BluetoothService mService = null;
    Context context;

    public BluetoothPrinter(Context context) {
        mService = new BluetoothService(context, mHandler);
        this.context = context;

        Set<BluetoothDevice> pairedDevices = mService.getPairedDev();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG,"no permission");
//                                   return;
                }
                Log.i(TAG, device.getName() + " " + device.getAddress());
            }
        }
        BluetoothDevice dev = mService.getDevByMac("0F:03:E0:A0:9B:83");
        mService.connect(dev);
    }

    public void PrintSaleOrder(ODataRow so, List<ODataRow> order_lines){
        Resources resources = this.context.getResources();
        float scale = resources.getDisplayMetrics().density;
        int bmp_height = HEIGHT_OFFSET + order_lines.size() * 25;
        Bitmap bitmap = Bitmap.createBitmap(CANVAS_WIDTH, bmp_height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.BLACK);
        // text size in pixels
        paint.setTextSize((int) (8 * scale));

        Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numPaint.setColor(Color.BLACK);
        numPaint.setTextSize((int) (8 * scale));
        numPaint.setTextAlign(Paint.Align.RIGHT);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize((int) (12 * scale));
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
                       + so.getString("name"), 350, y, paint);
        y += 40;
        canvas.drawText(resources.getString(R.string.label_sale_detail_customer) + ": "
                        + so.getString("partner_name"), 5, y, paint);
        canvas.drawText(resources.getString(R.string.label_sale_detail_salesman) + ": "
                        + salesman, 350, y, paint);
        y += 30;
        // text shadow
//        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
        Paint dashedPaint = new Paint();
        dashedPaint.setARGB(255, 0, 0, 0);
        dashedPaint.setStyle(Paint.Style.STROKE);
        dashedPaint.setPathEffect(new DashPathEffect(new float[]{10f, 20f}, 0f));

        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);

        y += 22;
//        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(resources.getString(R.string.label_receipt_product), PRODUCT_COL, y, paint);
        canvas.drawText(resources.getString(R.string.label_receipt_price_unit), PRICE_COL, y, numPaint);
        canvas.drawText(resources.getString(R.string.label_receipt_qty), QTY_COL, y, numPaint);
        canvas.drawText(resources.getString(R.string.label_receipt_subtotal), SUBTOTAL_COL, y, numPaint);

        y += 5;
        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);

        for (ODataRow row : order_lines){
            y += 25;
            Log.d(TAG, row.values().toString());
            canvas.drawText(row.getString("name"), PRODUCT_COL, y, paint);
            canvas.drawText(row.getString("price_unit"), PRICE_COL, y, numPaint);
            canvas.drawText(row.getString("product_uom_qty"), QTY_COL, y, numPaint);
            canvas.drawText(row.getString("price_total"), SUBTOTAL_COL, y, numPaint);
        }
        y += 5;
        canvas.drawLine(2, y, CANVAS_WIDTH-10, y, dashedPaint);
        y += 25;
        canvas.drawText(resources.getString(R.string.label_total_amount), 350, y, paint);
        canvas.drawText(so.getString("amount_total"), SUBTOTAL_COL, y, numPaint);
        y += 40;
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(resources.getString(R.string.footer_receipt_received), CANVAS_WIDTH/2, y, paint);
        y += 50;
        canvas.drawText(resources.getString(R.string.footer_receipt_delivered), CANVAS_WIDTH/2, y, paint);


        File path = context.getExternalFilesDir(null);
        File file = new File(path, FILENAME);
        try (FileOutputStream out = new FileOutputStream(file)) {
            boolean res = bitmap.compress(Bitmap.CompressFormat.PNG, 50, out);
            if (res) {
                printImage();
            }

        } catch(Exception e){
            e.printStackTrace();
        }
    }


    @SuppressLint("SdCardPath")
    private void printImage() {
        String path = context.getExternalFilesDir(null).getAbsolutePath() + "/" + FILENAME;
        byte[] sendData = null;
        PrintPic pg = new PrintPic();
        pg.initCanvas(800);
        pg.initPaint();
        pg.drawImage(0, 0, path);
        sendData = pg.printDraw();
        mService.write(sendData);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:   //������
                            connectionStatus = BluetoothService.STATE_CONNECTED;
                            Log.i(TAG,"Connect successful");
                            break;
                        case BluetoothService.STATE_CONNECTING:  //��������
                            Log.d(TAG,"connecting");
                            break;
                        case BluetoothService.STATE_LISTEN:     //�������ӵĵ���
                        case BluetoothService.STATE_NONE:
                            Log.d(TAG,"state none");
                            break;
                    }
                    break;
                case BluetoothService.MESSAGE_CONNECTION_LOST:    //�����ѶϿ�����
                    Log.d(TAG,"Device connection was lost");
                    break;
                case BluetoothService.MESSAGE_UNABLE_CONNECT:     //�޷������豸
                    Log.d(TAG,"Unable to connect device");
                    break;
            }
        }

    };

    public boolean isConnected(){
        return this.connectionStatus == BluetoothService.STATE_CONNECTED;
    }

    public void stopService() {
        mService.stop();
    }

}
