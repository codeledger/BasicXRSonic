package com.codeledger.basicxrsonic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class DeviceEvent<T> {
    @NonNull
    private final T mContent;
    private boolean mHandled;

    public DeviceEvent(@NonNull final T content) {
        mContent = content;
    }

    @NonNull
    public T peek() {
        return mContent;
    }

    @Nullable
    public T get() {
        if (mHandled) {
            return null;
        }

        mHandled = true;
        return mContent;
    }

}

