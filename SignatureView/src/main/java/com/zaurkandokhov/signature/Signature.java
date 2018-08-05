package com.zaurkandokhov.signature;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.zaurkandokhov.signatureview.SignatureView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Signature extends AppCompatActivity {
    private static final int PERMISSIONS_WRITE_EXTERNAL_STORAGE = 0;
    private static final int PERMISSIONS_CAMERA = 1;

    private static final String FILE_SIGNATURE_PREFIX = "signature";
    private static final String FILE_CAMERA_PREFIX = "camera";

    private static final int CAMERA_REQUEST = 1888;

    private SignatureView signatureView;
    //private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);//see xml layout
        signatureView = findViewById(R.id.signature_view);

        int colorPrimary = ContextCompat.getColor(this, R.color.colorAccent);
        signatureView.setPenColor(colorPrimary);
        // or like signatureView.setPenColor(Color.RED);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }

        requestPermission(PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        requestPermission(PERMISSIONS_CAMERA);
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            try {
                Bitmap workingBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
                signatureView.setBitmap(mutableBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //saveBitmapImage(photo, FILE_CAMERA_PREFIX, false, false);
            // Update UI to reflect image being shared
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_signature, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download:
                saveBitmapImage(signatureView.getSignatureBitmap(), FILE_SIGNATURE_PREFIX, false, false);
                return true;
            case R.id.action_camera:
                showCameraIntent();
                return true;
            case R.id.action_clear:
                signatureView.clearCanvas();//Clear SignatureView
                Toast.makeText(getApplicationContext(),
                        R.string.canvas_cleared,
                        Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_info:
                infoDialog();
                return true;
            case R.id.action_has_signature:
                Toast.makeText(getApplicationContext(),
                        "SignatureView is empty: " + signatureView.isBitmapEmpty(),
                        Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class MyMediaScanner implements
            MediaScannerConnection.MediaScannerConnectionClient {

        private MediaScannerConnection mSC;
        private File file;

        MyMediaScanner(Context context, File file) {
            this.file = file;
            mSC = new MediaScannerConnection(context, this);
            mSC.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            mSC.scanFile(file.getAbsolutePath(), null);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            mSC.disconnect();
        }
    }

    public void infoDialog() {
        String infoMessage = "App version : " + BuildConfig.VERSION_NAME;
        infoMessage = infoMessage + "\n\n" + "SignatureView library version : " +
                com.zaurkandokhov.signatureview.BuildConfig.VERSION_NAME;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.info)
                .setMessage(infoMessage)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        Dialog dialog = builder.create();
        dialog.show();
    }

    private void showCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    private void saveBitmapImage(Bitmap bitmap, String fileNamePrefix, boolean useExternalStorage, boolean useUniqueFileName) {
        File directory = useExternalStorage ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) : getFilesDir();
        File file = new File(directory, String.format("%s_%s.png", fileNamePrefix, useUniqueFileName ? System.currentTimeMillis() : "last"));
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } else {
                throw new FileNotFoundException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.flush();
                    out.close();

                    if (bitmap != null) {
                        Toast.makeText(getApplicationContext(),
                                "Image saved successfully at " + file.getPath(),
                                Toast.LENGTH_LONG).show();
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                            new MyMediaScanner(this, file);
                        } else {
                            ArrayList<String> toBeScanned = new ArrayList<String>();
                            toBeScanned.add(file.getAbsolutePath());
                            String[] toBeScannedStr = new String[toBeScanned.size()];
                            toBeScannedStr = toBeScanned.toArray(toBeScannedStr);
                            MediaScannerConnection.scanFile(this, toBeScannedStr, null, null);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case CAMERA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Bitmap photo = (Bitmap) data.getExtras().get("data");
                        saveBitmapImage(photo, FILE_CAMERA_PREFIX, false, false);
                        //imageView.setImageBitmap(photo);
                        //imageView.setImageBitmap(getBitmapFromResources(getResources(), R.drawable.logo));
                    }
                }
                break;
        }
    }

    public void requestPermission(final int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final String permissionString;
            final String alertDialogTitle;
            final String alertDialogMessage;
            switch (requestCode) {
                case PERMISSIONS_WRITE_EXTERNAL_STORAGE:
                    permissionString = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                    alertDialogTitle = "Запись на внешний накопитель";
                    alertDialogMessage = "Мы не можем сохранить файл без Вашего разрешения. Разрешить запись файла?";
                    break;
                case PERMISSIONS_CAMERA:
                    permissionString = Manifest.permission.CAMERA;
                    alertDialogTitle = "Доступ к камере";
                    alertDialogMessage = "Мы не можем использовать камеру без Вашего разрешения. Разрешить использование камеры?";
                    break;
                default:
                    permissionString = "";
                    alertDialogTitle = "";
                    alertDialogMessage = "";
            }

            if (ActivityCompat.checkSelfPermission(this, permissionString) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                if (shouldShowRequestPermissionRationale(permissionString)) {
                    new AlertDialog.Builder(Signature.this)
                            .setTitle(alertDialogTitle)
                            .setMessage(alertDialogMessage)
                            .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{ permissionString }, requestCode);
                                    }
                                }
                            })
                            .setNegativeButton("Нет, спасибо", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(Signature.this, ":(", Toast.LENGTH_SHORT).show();
                                }
                            }).show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{ permissionString }, requestCode);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        String accessGranted;
        String accessDenied;
        switch (requestCode) {
            case PERMISSIONS_WRITE_EXTERNAL_STORAGE:
                accessGranted = "Доступ к записи на внешний накопитель разрешен";
                accessDenied = "Доступ к записи на внешний накопитель запрещен";
                break;
            case PERMISSIONS_CAMERA:
                accessGranted = "Доступ к камере разрешен";
                accessDenied = "Доступ к камере запрещен";
                break;
            default:
                accessGranted = "";
                accessDenied = "";
        }
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        accessGranted,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,
                        accessDenied,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}