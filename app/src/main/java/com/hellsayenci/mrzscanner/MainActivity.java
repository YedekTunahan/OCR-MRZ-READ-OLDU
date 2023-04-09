package com.hellsayenci.mrzscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.vision.text.TextBlock;
import com.google.mlkit.vision.common.InputImage;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;



public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private CameraView camera;
    private FrameOverlay viewFinder;
    private AtomicBoolean processing = new AtomicBoolean(false); // Atomic otomatik güncellenebilen bir değer.
    private Bitmap originalBitmap = null;
    private Bitmap scannable = null;

    private ProcessOCR processOCR;

    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);


    DisplayMetrics displayMetrics = new DisplayMetrics();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera = findViewById(R.id.camera);
        camera.setLifecycleOwner(this); // Yaşam Döngüsü Sahibini Ayarla



        camera.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(@NonNull CameraOptions options) {
                viewFinder = new FrameOverlay(MainActivity.this);
                camera.addView(viewFinder);
                camera.addFrameProcessor(frameProcessor); // çerçeve işlemcisi = frame processor Bununla birlikte Sürekli okuma gerçekleştiriliyor...
            }
        });
    }

    //Kamera ya eklediğimiz Frame
    private FrameProcessor frameProcessor = new FrameProcessor() {
        @Override
        public void process(@NonNull Frame frame) {

            if (frame.getData() != null && !processing.get()) { // Tarafından belirtilen bellek efektleriyle geçerli değeri döndürür,Geçerli değer == beklenen Değer ise değeri atomik olarak newValue olarak ayarlar ve bellek efektleri VarHandle tarafından belirtilir.Karşılaştır ve ayarla.

                processing.set(true);

                YuvImage yuvImage = new YuvImage((byte[]) frame.getData(), ImageFormat.NV21, frame.getSize().getWidth(), frame.getSize().getHeight(), null);

                ByteArrayOutputStream os = new ByteArrayOutputStream(); // verinin bir bayt dizisine yazılması ve çıktı işlemleri için kullanılır.Oluşturulur.

                // HDR GÖRÜNTÜYÜ , jpeg e sıkıştırma işlemi true false döner , 0-100 arası sıkıştırma oranı  100 max kalite için sıkıştırma
                // Rect () -- > Dikdörtken oluşturuyoruz okuma dikdörtgeni
                // stream ( os ) Sıkıştırılmış verileri yazmak için
                yuvImage.compressToJpeg(new Rect(0, 0, frame.getSize().getWidth(), frame.getSize().getHeight()), 100, os);

                byte[] jpegByteArray = os.toByteArray(); // bayt Array'i oluşturma işlemidir.

                Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length); // decoeBaytArray bayt dizisinin kodunu çözer.

                if(bitmap != null) {

                    bitmap = rotateImage(bitmap, frame.getRotation());
                    bitmap = getViewFinderArea(bitmap);  // Bulucu Alanını görüntülediğimiz alan

                    originalBitmap = bitmap;
                    scannable = getScannableArea(bitmap); // Taranacak Alanın Sınırlarını Belirttiğimiz yer Tarama Alanı -- processOCR 'a veriliyor.
                    // TEXT - Sürekli Tarama yapar
                    processOCR = new ProcessOCR();
                    processOCR.setBitmap(scannable);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            processOCR.execute();
                        }
                    });
                }
            }
        }
    };

    private Bitmap getViewFinderArea(Bitmap bitmap) {   //Bunu iptal edince Tüm kartı okuyor

        int sizeInPixel = getResources().getDimensionPixelSize(R.dimen.frame_margin);

        int center = bitmap.getHeight() / 2;

        int left = sizeInPixel; // ok
        int right = bitmap.getWidth() - sizeInPixel;  // ok

        int width = right - left;
        int frameHeight = (int) (width / 1.42f); // Passport's size (ISO/IEC 7810 ID-3) is 125mm × 88mm  ok
        int top = center - (int)(frameHeight / 1.7f); // 1.7f değil de 2 yapınca tüm kartı yine okuyor..

        bitmap = Bitmap.createBitmap(bitmap, left, (int) (top*1.9f),
                width, frameHeight);

      /*  Log.e("START:","getViewFinderArea");
        Log.e("left:",String.valueOf(left));
        Log.e("top:",String.valueOf(top));
        Log.e("right:",String.valueOf(width));
        Log.e("bottom:",String.valueOf(frameHeight));
        Log.e("SONN:","getViewFinderArea");*/
        return bitmap;
    }

    private Bitmap getScannableArea(Bitmap bitmap){   // Taranabilir alan elde etme()
        int top = bitmap.getHeight() * 1 / 10;

        bitmap = Bitmap.createBitmap(bitmap, 0, top,
                bitmap.getWidth(), bitmap.getHeight() - top);

        /*Log.e("START:","getScannableArea");
        Log.e("left:",String.valueOf(0));
        Log.e("top:",String.valueOf(top));
        Log.e("right:",String.valueOf(bitmap.getWidth()));
        Log.e("bottom:",String.valueOf(bitmap.getHeight() - top));
        Log.e("SONN:","getScannableArea");*/

        return bitmap;
    }

    private Bitmap rotateImage(Bitmap bitmap, int rotate){
        Log.v(TAG, "Rotation: " + rotate);

        if (rotate != 0) {

            // Getting width & height of the given image.
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            // Setting pre rotate
            Matrix mtx = new Matrix();
            mtx.preRotate(rotate);

            // Rotating Bitmap
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
        }

        // Convert to ARGB_8888, required by tess
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        return bitmap;
    }


    /// okuma işlemi ,,,
    private class ProcessOCR extends AsyncTask {
        Bitmap bitmap = null;
        @Override
        protected Object doInBackground(Object[] objects) {
            if (bitmap != null) {

                // ML KİT
                Log.e("m","test");

                // Görüntü geliyor
                processImage(bitmap);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {  // yürütme sonrası ()
            processing.set(false);  // işleme

        }

        public void setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }

    //ML KİT İmage işleme
    private void processImage(Bitmap bitmap) {

        Log.e("M","thread ML KİT AKTİF");

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        Task<com.google.mlkit.vision.text.Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<com.google.mlkit.vision.text.Text>() {
                            @Override
                            public void onSuccess(com.google.mlkit.vision.text.Text visionText) {
                                // Task completed successfully
                                // ...
                                //textView.setText((CharSequence) visionText);
                                for(Text.TextBlock block : visionText.getTextBlocks()){
                                    String blockText = block.getText();
                                    Point[] blockCornerPoints = block.getCornerPoints();
                                    Rect blockFrame = block.getBoundingBox();
                                    /*Log.e("symbol",blockText);
                                    Log.e("symbolFrame", String.valueOf(blockFrame));
                                    Log.d("-","---------");*/
                                }

                                String value = visionText.getText().replace(" ","");
                                String result = value.replace("«","<");


                                 if (result.length() == 92) {
                                    Log.e("visionText", result); // Bütün taramayı veriyor...

                                    Log.e("UZUNLUK", String.valueOf(result.length()));
                                }


                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });


    }

}