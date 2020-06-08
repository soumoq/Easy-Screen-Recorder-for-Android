package com.example.cameraapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.core.app.*;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.*;
import android.util.Rational;
import android.util.Size;
import android.view.*;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_CAMERA=1;
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    TextureView textureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.view_finder);

        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @SuppressLint("RestrictedApi")
    private void startCamera() {

        CameraX.unbindAll();
        Rational aspectRatio = new Rational (textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen

        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).setLensFacing(CameraX.LensFacing.FRONT).build();
        Preview preview = new Preview(pConfig);

        try {
            CameraX.getCameraWithLensFacing(CameraX.LensFacing.FRONT);
        }catch (Exception e)
        {
            Toast.makeText(this,"Exception: "+e,Toast.LENGTH_LONG).show();
        }

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output){
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);


                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });



        //bind to lifecycle:
        CameraX.bindToLifecycle((LifecycleOwner)this, preview);
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();

        switch(rotation){
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

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    private boolean allPermissionsGranted()
    {
        //When permission is not granted by user, show them message why this permission is needed.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            Toast.makeText(this, "Please grant permissions to camera", Toast.LENGTH_LONG).show();

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
}