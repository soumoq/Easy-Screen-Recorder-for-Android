package com.example.cameraapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.util.Size;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.hamed.floatinglayout.CallBack;
import io.hamed.floatinglayout.FloatingLayout;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static android.widget.Toast.LENGTH_LONG;


public class MainActivity extends AppCompatActivity implements CallBack, EasyPermissions.PermissionCallbacks {


    private static final int REQUEST_CODE = 1000;
    private static final int REQUEST_PERMISSION = 1001;
    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private ToggleButton mToggleButton;

    private int mScreenDensity;
    private int DISPLAY_WIDTH;
    private int DISPLAY_HEIGHT;

    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private RelativeLayout mRootLayout;
    private String mVideoUrl = "";


    private static final int MY_PERMISSIONS_CAMERA = 1;
    private FloatingLayout floatingLayout;
    private Button button, switchActivity;

    private String quality;
    private RadioGroup radioGroup;
    private ToggleButton toggleButton;
    private ImageView closeBtn;
    private boolean cameraState = true;
    private TextView textMove;
    LogCheck log;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Date currentTime = Calendar.getInstance().getTime();//fetch date
        log = new LogCheck(); //  Create log object
        log.appendLog("\n\n\n\n\n" + currentTime + "\n : Start on create \n");

        grantPermission();
        grantAllPermission();

        button = (Button) findViewById(R.id.btn_run);   //open webcam button
        mRootLayout = findViewById(R.id.rootLayout);


        //Create Floating layout
        floatingLayout = new FloatingLayout(getApplicationContext(), R.layout.floting_layout, MainActivity.this);


        //Open webcam button
        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ShowToast")
            @Override
            public void onClick(View v) {

                if (grantAllPermission()) {
                    if (!floatingLayout.isShow()) {
                        log.appendLog("creating floating layout");
                        floatingLayout.create();        //creating floating layout
                    } else {
                        log.appendLog("Already Running floating layout");
                        Toast.makeText(MainActivity.this, "Already Running", LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please grant all permission", LENGTH_LONG).show();
                }
            }
        });


        //screen recode
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mScreenDensity = displayMetrics.densityDpi;

        mMediaRecorder = new MediaRecorder();
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.check(R.id.radioButton1);
        quality = "high";

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb = (RadioButton) group.findViewById(checkedId);
                if (null != rb && checkedId > -1) {
                    if (rb.getText().equals("High quality")) {
                        quality = "high";
                    } else if (rb.getText().equals("Medium quality")) {
                        quality = "mid";
                    } else if (rb.getText().equals("Low quality")) {
                        quality = "low";
                    }
                }
            }
        });

        log.appendLog("End on create");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean grantPermission() {    //Permission for draw over other app
        if (!Settings.canDrawOverlays(this)) {
            log.appendLog("deny draw over other app permission");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 0);
            return true;
        } else {
            log.appendLog("Draw over other app permission");
            return false;
        }
    }

    @AfterPermissionGranted(123)
    private boolean grantAllPermission() {     //permission for other feature
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Ready to start", LENGTH_LONG).show();
            log.appendLog("Feature permission");
            return true;
        } else {
            log.appendLog("deny feature permission");
            EasyPermissions.requestPermissions(this, "Please grant all permission to use the app",
                    123, perms);
            return false;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void toggleScreenShare(View v) {
        toggleButton = (ToggleButton) v;
        if (toggleButton.isChecked()) {
            Toast.makeText(this, "Recording start", LENGTH_LONG).show();

            if (quality.equals("high")) {
                log.appendLog("Start high quality recording");
                initRecorderHighResolution();
                log.appendLog("End high quality recording");

            } else if (quality.equals("low")) {
                log.appendLog("Start low quality recording");
                initRecorderLowResolution();
                log.appendLog("Start end quality recording");
            } else if (quality.equals("mid")) {
                log.appendLog("Start mid quality recording");
                initRecorderMidResolution();
                log.appendLog("Start mid quality recording");
            }

            closeBtn.setVisibility(View.INVISIBLE);
            recordScreen();
        } else {
//            mMediaRecorder.stop();
            mMediaRecorder.reset();
            stopRecordScreen();

            //mVideoView.setVisibility(View.VISIBLE);
            //mVideoView.setVideoURI(Uri.parse(mVideoUrl));
            //mVideoView.start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void recordScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("Test", DISPLAY_WIDTH, DISPLAY_HEIGHT,
                mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(),
                null, null);
    }

    private void initRecorderHighResolution() {
        try {
            Toast.makeText(this, "Recoding quality high", LENGTH_LONG).show();

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            DISPLAY_WIDTH = metrics.widthPixels;
            DISPLAY_HEIGHT = metrics.heightPixels;

            //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);


            CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            mVideoUrl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) +
                    new StringBuilder("/screen_record-").append(new SimpleDateFormat("dd-MM-yyy-hh_mm_ss")
                            .format(new Date())).append(".mp4").toString();

            mMediaRecorder.setOutputFile(mVideoUrl);
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(cpHigh.videoBitRate);
            mMediaRecorder.setVideoFrameRate(cpHigh.videoFrameRate);


            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);

            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();

        } catch (IOException e) {
            Toast.makeText(this, "Exception: " + e, LENGTH_LONG).show();
            log.appendLog("Exception high quality : " + e);
        }
    }


    private void initRecorderLowResolution() {
        try {
            Toast.makeText(this, "Recoding quality low", LENGTH_LONG).show();

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            DISPLAY_WIDTH = metrics.widthPixels;
            DISPLAY_HEIGHT = metrics.heightPixels;

            //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);


            CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            mVideoUrl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) +
                    new StringBuilder("/screen_record-").append(new SimpleDateFormat("dd-MM-yyy-hh_mm_ss")
                            .format(new Date())).append(".mp4").toString();

            mMediaRecorder.setOutputFile(mVideoUrl);
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(cpHigh.videoBitRate);
            mMediaRecorder.setVideoFrameRate(cpHigh.videoFrameRate);


            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);

            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();

        } catch (IOException e) {
            Toast.makeText(this, "Exception: " + e, LENGTH_LONG).show();
            log.appendLog("Exception low quality : " + e);
        }
    }


    @SuppressLint("SimpleDateFormat")
    private void initRecorderMidResolution() {
        try {
            Toast.makeText(this, "Recoding quality medium", LENGTH_LONG).show();


            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            DISPLAY_WIDTH = metrics.widthPixels;
            DISPLAY_HEIGHT = metrics.heightPixels;

            //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);


            CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            mVideoUrl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) +
                    new StringBuilder("/screen_record-").append(new SimpleDateFormat("dd-MM-yyy-hh_mm_ss")
                            .format(new Date())).append(".mp4").toString();

            mMediaRecorder.setOutputFile(mVideoUrl);
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(cpHigh.videoBitRate);
            mMediaRecorder.setVideoFrameRate(cpHigh.videoFrameRate);


            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);

            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();

        } catch (IOException e) {
            Toast.makeText(this, "Exception: " + e, LENGTH_LONG).show();
            log.appendLog("Exception mid quality : " + e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {

        }

        if (requestCode != REQUEST_CODE) {
            //Toast.makeText(this, "Unk error", Toast.LENGTH_SHORT).show();
            return;
        }

        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            mToggleButton.setChecked(false);
            mMediaRecorder.reset();
            closeBtn.setVisibility(View.VISIBLE);
            return;
        }

        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class MediaProjectionCallback extends MediaProjection.Callback {

        @Override
        public void onStop() {
            super.onStop();

            if (mToggleButton.isChecked()) {
                mToggleButton.setChecked(false);
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            }

            mMediaProjection = null;
            stopRecordScreen();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void stopRecordScreen() {
        if (mVirtualDisplay == null)
            return;

        mVirtualDisplay.release();
        destroyMediaProjection();
        closeBtn.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Recording stop", LENGTH_LONG).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            Toast.makeText(MainActivity.this, "Recoding stop", LENGTH_LONG).show();
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION: {
                if (grantResults.length > 0 && (grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    toggleScreenShare(mToggleButton);
                } else {
                    mToggleButton.setChecked(false);
                    Snackbar.make(mRootLayout, "Permission", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Enable", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                    Manifest.permission.RECORD_AUDIO
                                            },
                                            REQUEST_PERMISSION);
                                }
                            });
                }
                return;
            }
        }
    }


    @SuppressLint("RestrictedApi")
    private void startCamera(TextureView textureView, CameraX.LensFacing face) {

        log.appendLog("Camera is starting....");
        CameraX.unbindAll();
        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen

        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).setLensFacing(face).build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform(textureView);
                    }
                });


        //bind to lifecycle:
        CameraXLifeCycle lifeCycle = new CameraXLifeCycle();
        lifeCycle.doOnResume();

        if (cameraState)
            CameraX.bindToLifecycle(lifeCycle, preview);
        else
            CameraX.unbind(preview);

    }

    private void updateTransform(TextureView textureView) {

        log.appendLog("Camera is into updateTransform method");
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
        log.appendLog("Camera is out of updateTransform method");
    }


    @Override
    public void onClickListener(int resourceId) {
        log.appendLog("Floating layout : onClickListener");
    }

    @SuppressLint({"SetTextI18n", "RestrictedApi"})
    @Override
    public void onCreateListener(@Nullable View view) {
        log.appendLog("Floating layout : start onCreateListener");
        Toast.makeText(this, "Open webcam", Toast.LENGTH_SHORT).show();


        CardView cardView = (CardView) view.findViewById(R.id.root_container);
        cardView.setCardElevation(1);


        mToggleButton = view.findViewById(R.id.toggleButton1);   //recording play pose button
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                log.appendLog("Floating layout : mToggleButton to start recording");
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                        ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                        mToggleButton.setChecked(false);
                        Snackbar.make(mRootLayout, "Permission", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Enable", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{
                                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                        Manifest.permission.RECORD_AUDIO
                                                },
                                                REQUEST_PERMISSION);
                                    }
                                });
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.RECORD_AUDIO
                                },
                                REQUEST_PERMISSION);
                    }
                } else {
                    log.appendLog("Floating layout : mToggleButton to start toggleScreenShare() method");
                    toggleScreenShare(v);
                }
            }
        });


        closeBtn = view.findViewById(R.id.btn_close);    //Close button
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.appendLog("Close Floating layout");
                floatingLayout.close();
            }
        });

        ImageView openBtn = view.findViewById(R.id.open_button);   //Jump main activity button
        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });


        TextureView textureView123 = (TextureView) view.findViewById(R.id.view_finder123);   //camera view
        textMove = view.findViewById(R.id.textMove);
        textMove.setVisibility(View.INVISIBLE);

        ToggleButton switchCamera = view.findViewById(R.id.switchCamera);  //switch camera front back button
        ToggleButton cameraOnOff = view.findViewById(R.id.cameraOnOff);   //camera play pose button

        switchCamera.setVisibility(View.INVISIBLE);
        cameraState = false;
        textureView123.setVisibility(View.INVISIBLE);

        Resources r = getResources();
        int pxw = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, r.getDisplayMetrics());
        int pxh = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, r.getDisplayMetrics());
        cardView.setLayoutParams(new CardView.LayoutParams(pxw, pxh));
        textMove.setVisibility(View.VISIBLE);

        log.appendLog("Start cameraX front");
        startCamera(textureView123, CameraX.LensFacing.FRONT);
        log.appendLog("end cameraX front");
        switchCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    log.appendLog("Start cameraX Back");
                    startCamera(textureView123, CameraX.LensFacing.BACK);
                    log.appendLog("End cameraX Back");
                } else {
                    log.appendLog("Start cameraX Front");
                    startCamera(textureView123, CameraX.LensFacing.FRONT);
                    log.appendLog("End CameraX Front");
                }
            }
        });

        cameraOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    log.appendLog("Camera is invisible");
                    switchCamera.setVisibility(View.INVISIBLE);
                    cameraState = false;
                    textureView123.setVisibility(View.INVISIBLE);
                    Resources r = getResources();
                    int pxw = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, r.getDisplayMetrics());
                    int pxh = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, r.getDisplayMetrics());
                    cardView.setLayoutParams(new CardView.LayoutParams(pxw, pxh));
                    textMove.setVisibility(View.VISIBLE);

                } else {
                    log.appendLog("Camera is visible");
                    textureView123.setVisibility(View.VISIBLE);
                    switchCamera.setVisibility(View.VISIBLE);
                    cameraState = true;
                    startCamera(textureView123, CameraX.LensFacing.FRONT);

                    Resources r = getResources();
                    int pxw = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 105, r.getDisplayMetrics());
                    int pxh = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170, r.getDisplayMetrics());
                    cardView.setLayoutParams(new CardView.LayoutParams(pxw, pxh));
                    textMove.setVisibility(View.INVISIBLE);


                }
            }
        });


    }

    @Override
    public void onCloseListener() {
        Toast.makeText(this, "Close webcam", Toast.LENGTH_SHORT).show();
        log.appendLog("Close floating layout");
    }


}


