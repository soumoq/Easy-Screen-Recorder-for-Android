package com.example.cameraapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Rational;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import io.hamed.floatinglayout.CallBack;
import io.hamed.floatinglayout.FloatingLayout;

import static android.widget.Toast.LENGTH_LONG;


public class MainActivity extends AppCompatActivity implements CallBack {

    private static final int MY_PERMISSIONS_CAMERA = 1;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    //private TextureView textureView;
    private FloatingLayout floatingLayout;
    private Button button, switchActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //textureView = findViewById(R.id.view_finder);
        button = (Button) findViewById(R.id.btn_run);
        switchActivity = (Button) findViewById(R.id.switchActivity);


        if (allPermissionsGranted()) {
            //startCamera(textureView); //start camera if permission has been granted by user
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                );
                startActivityForResult(intent, 25);
            }
        }


        floatingLayout = new FloatingLayout(getApplicationContext(), R.layout.floting_layout, MainActivity.this);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //btn_run.setOnClickListener { if (!floatingLayout!!.isShow()) floatingLayout!!.create() }
                if (!floatingLayout.isShow()) {
                    floatingLayout.create();
                } else {
                    Toast.makeText(MainActivity.this, "Not running", LENGTH_LONG).show();
                }
            }
        });

        switchActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, Test.class);
                startActivity(intent);
            }
        });

    }


    @SuppressLint("RestrictedApi")
    private void startCamera(TextureView textureView) {

        CameraX.unbindAll();
        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen

        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).setLensFacing(CameraX.LensFacing.FRONT).build();
        Preview preview = new Preview(pConfig);

        try {
            CameraX.getCameraWithLensFacing(CameraX.LensFacing.FRONT);
        } catch (Exception e) {
            Toast.makeText(this, "Exception: " + e, LENGTH_LONG).show();
        }

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        //parent.removeView(textureView);
                        //parent.addView(textureView, 0);


                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform(textureView);
                    }
                });


        //bind to lifecycle:
        CameraXLifeCycle lifeCycle = new CameraXLifeCycle();
        lifeCycle.doOnResume();
        CameraX.bindToLifecycle(lifeCycle, preview);

    }

    private void updateTransform(TextureView textureView) {
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int) textureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    private boolean allPermissionsGranted() {
        //When permission is not granted by user, show them message why this permission is needed.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            Toast.makeText(this, "Please grant permissions to camera", LENGTH_LONG).show();

            //Give user option to still opt-in the permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_CAMERA);
        } else {
            // Show user dialog to grant permission to record audio
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_CAMERA);
        }

        return true;
    }


    @Override
    public void onClickListener(int resourceId) {
        if (resourceId == R.id.btn_close)
            floatingLayout.close();


    }

    @SuppressLint({"SetTextI18n", "RestrictedApi"})
    @Override
    public void onCreateListener(@Nullable View view) {
        Toast.makeText(this, "On Create", Toast.LENGTH_SHORT).show();
        TextureView textureView123 = (TextureView) view.findViewById(R.id.view_finder123);

        if (allPermissionsGranted()) {
            startCamera(textureView123);

        }

        TextView textView = (TextView) view.findViewById(R.id.txtv);
        textView.setText("How is it");
    }

    @Override
    public void onCloseListener() {
        Toast.makeText(this, "On Destroy", Toast.LENGTH_SHORT).show();
    }

}


