package com.example.cameraapp;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class CameraXLifeCycle implements LifecycleOwner {
    private LifecycleRegistry lifecycleRegistry;

    public CameraXLifeCycle() {
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.markState(Lifecycle.State.CREATED);
    }

    public void doOnResume() {
        lifecycleRegistry.markState(Lifecycle.State.RESUMED);
    }

    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }
}