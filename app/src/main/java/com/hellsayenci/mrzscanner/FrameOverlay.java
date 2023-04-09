package com.hellsayenci.mrzscanner;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;


//Çerçeve
public class FrameOverlay extends LinearLayout {
    private Bitmap windowFrame;
    private Context context;

    public FrameOverlay(Context context) {
        super(context);
        this.context = context;
    }

    public FrameOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public FrameOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FrameOverlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (windowFrame == null) {
            createWindowFrame();
        }
        canvas.drawBitmap(windowFrame, 0, 0, null);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isClickable() {
        return false;
    }



    // ÇERÇEVE OLUŞTURMA
    protected void createWindowFrame() {
        int viewHeight = getHeight(); // Çember yüksekliği

        windowFrame = Bitmap.createBitmap(getWidth(), viewHeight, Bitmap.Config.ARGB_8888);
        Canvas osCanvas = new Canvas(windowFrame);

        RectF outerRectangle = new RectF(0, 0, getWidth(), viewHeight); // Siyah arka plan için   -- Normal top = 50 olunca aşağıdan başlıyor...

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); // ANTI_ALIAS_FLAG kenarları yumuşatmak için kullanılır.
        paint.setColor(Color.argb(250, 0, 0, 0)); // Arka plan siyah ekran
        osCanvas.drawRect(outerRectangle, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));  //Çerçeve içerisine Görüntüyü aktarıyor.Burası perdelemeyi kaldıran kısım, Belirtilen alandaki Filtreyi kaldırıyor..

        // Çerçeve İçi
        int sizeInPixel = context.getResources().getDimensionPixelSize(R.dimen.frame_margin); // Çerçevenin Width Genişliğini belirtiyor ( left )

        int center = viewHeight / 2;

        int left = sizeInPixel;
        int right = getWidth() - sizeInPixel; // left ile ters ilişki

        int width = right - left;
        int frameHeight = (int) (width / 1.42f); // Passport's size (ISO/IEC 7810 ID-3) is 125mm × 88mm

        int top = center - (frameHeight / 2);
        int bottom = center + (frameHeight / 2);

        RectF innerRectangle = new RectF(left, top, right, bottom);
        osCanvas.drawRect(innerRectangle, paint); // Belirilen ölçüdeki alanın içini boşaltıyoruz.

        // Beyaz Çerçeve Çizimi
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(5);
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.STROKE);
        osCanvas.drawRect(innerRectangle, paint);

        Log.e("START:","ÇERÇEVE");
        Log.e("left:",String.valueOf(left));
        Log.e("top:",String.valueOf(top));
        Log.e("right:",String.valueOf(right));
        Log.e("bottom:",String.valueOf(bottom));
        Log.e("SONN:","ÇERÇEVE");


        //////////// 2........

        int sizeInPixel2 = context.getResources().getDimensionPixelSize(R.dimen.frame_margin); // Çerçevenin Width Genişliğini belirtiyor ( left )

        int center2 = viewHeight / 2 ;

        int left2 = sizeInPixel;
        int right2 = getWidth() - sizeInPixel; // left ile ters ilişki

        int width2 = right - left;
        int frameHeight2 = (int) (width / 1.42f) / 4; // Passport's size (ISO/IEC 7810 ID-3) is 125mm × 88mm

        int top2 = center  - (frameHeight / 2);
        int bottom2 = center + (frameHeight / 2);

        RectF innerRectangle2 = new RectF(left2, (int) (top2*2f), right2, bottom2);


        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(10);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);

        osCanvas.drawRect(innerRectangle2, paint);
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        windowFrame = null;
    }
}