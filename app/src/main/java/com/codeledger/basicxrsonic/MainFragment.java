package com.codeledger.basicxrsonic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bose.blecore.DeviceException;
import com.bose.wearable.sensordata.Quaternion;
import com.bose.wearable.sensordata.SensorValue;
import com.bose.wearable.sensordata.Vector;
import com.google.android.material.snackbar.Snackbar;
import com.google.vr.sdk.audio.GvrAudioEngine;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class MainFragment extends Fragment {
    public static final String ARG_DEVICE_ADDRESS = "device-address";
    public static final String ARG_USE_SIMULATED_DEVICE = "use-simulated-device";
    private static final String TAG = MainFragment.class.getSimpleName();
    private static final String OBJECT_SOUND_FILE = "Bird_chirp.ogg";


    private String mDeviceAddress;
    private boolean mUseSimulatedDevice;
    @SuppressWarnings("PMD.SingularField") // Need to keep a reference to it so it does not get GC'd
    private MainViewModel mViewModel;
    private View mParentView;
    @Nullable
    private ProgressBar mProgressBar;
    private TextView mPitch;
    private TextView mRoll;
    private TextView mYaw;
    private TextView mX;
    private TextView mY;
    private TextView mZ;

    @Nullable
    private Snackbar mSnackBar;

    private GvrAudioEngine gvrAudioEngine;
    private volatile int sourceId = GvrAudioEngine.INVALID_ID;

    private static final float MIN_TARGET_DISTANCE = -10.0f;


    private float[] targetPosition;
    long localTime;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args != null) {
            mDeviceAddress = args.getString(ARG_DEVICE_ADDRESS);
            mUseSimulatedDevice = args.getBoolean(ARG_USE_SIMULATED_DEVICE, false);
        }

        if (mDeviceAddress == null && !mUseSimulatedDevice) {
            throw new IllegalArgumentException();
        }

        // Initialize 3D audio engine.
        gvrAudioEngine = new GvrAudioEngine(this.getActivity(), GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        targetPosition = new float[] {0.0f, 0.0f, MIN_TARGET_DISTANCE};
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {

        // Avoid any delays during start-up due to decoding of sound files.
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        // Start spatial audio playback of OBJECT_SOUND_FILE at the model position. The
                        // returned sourceId handle is stored and allows for repositioning the sound object
                        // whenever the target position changes.
                        gvrAudioEngine.preloadSoundFile(OBJECT_SOUND_FILE);
                        sourceId = gvrAudioEngine.createSoundObject(OBJECT_SOUND_FILE);
                        gvrAudioEngine.setSoundObjectPosition(
                                sourceId, targetPosition[0], targetPosition[1], targetPosition[2]);
                        gvrAudioEngine.playSound(sourceId, true /* looped playback */);

                    }
                }).start();
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mParentView = view.findViewById(R.id.container);

        mPitch = view.findViewById(R.id.pitch);
        mRoll = view.findViewById(R.id.roll);
        mYaw = view.findViewById(R.id.yaw);

        mX = view.findViewById(R.id.x);
        mY = view.findViewById(R.id.y);
        mZ = view.findViewById(R.id.z);

        localTime = System.nanoTime();
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = requireActivity();
        mProgressBar = activity.findViewById(R.id.progressbar);

        mViewModel = ViewModelProviders.of(this)
                .get(MainViewModel.class);

        mViewModel.busy()
                .observe(this, this::onBusy);

        mViewModel.errors()
                .observe(this, this::onError);

        mViewModel.sensorsSuspended()
                .observe(this, this::onSensorsSuspended);

        mViewModel.accelerometerData()
                .observe(this, this::onAccelerometerData);

        mViewModel.rotationData()
                .observe(this, this::onRotationData);

        if (mDeviceAddress != null) {
            mViewModel.selectDevice(mDeviceAddress);
        } else if (mUseSimulatedDevice) {
            mViewModel.selectSimulatedDevice();
        }
    }

    @Override
    public void onPause() {
        gvrAudioEngine.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        gvrAudioEngine.resume();
    }

    @Override
    public void onDestroy() {
        onBusy(false);

        final Snackbar snackbar = mSnackBar;
        mSnackBar = null;
        if (snackbar != null) {
            snackbar.dismiss();
        }

        super.onDestroy();
    }

    private void onBusy(final boolean isBusy) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(isBusy ? View.VISIBLE : View.INVISIBLE);
        }

        final Activity activity = getActivity();
        final Window window = activity != null ? activity.getWindow() : null;
        if (window != null) {
            if (isBusy) {
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        }
    }

    private void onError(@NonNull final DeviceEvent<DeviceException> event) {
        final DeviceException deviceException = event.get();
        if (deviceException != null) {
            showError(deviceException.getMessage());
            getFragmentManager().popBackStack();
        }
    }

    private void onSensorsSuspended(final boolean isSuspended) {
        final Snackbar snackbar;
        if (isSuspended) {
            snackbar = Snackbar.make(mParentView, R.string.sensors_suspended,
                    Snackbar.LENGTH_INDEFINITE);
        } else if (mSnackBar != null) {
            snackbar = Snackbar.make(mParentView, R.string.sensors_resumed,
                    Snackbar.LENGTH_SHORT);
        } else {
            snackbar = null;
        }

        if (snackbar != null) {
            snackbar.show();
        }

        mSnackBar = snackbar;
    }

    @SuppressWarnings("PMD.ReplaceVectorWithList") // PMD confuses SDK Vector with java.util.Vector
    private void onAccelerometerData(@NonNull final SensorValue sensorValue) {
        final Vector vector = sensorValue.vector();
        mX.setText(formatValue(vector.x()));
        mY.setText(formatValue(vector.y()));
        mZ.setText(formatValue(vector.z()));

        // Update the sound location to match it with the new target position.
        if (sourceId != GvrAudioEngine.INVALID_ID) {
            gvrAudioEngine.setSoundObjectPosition(
                    sourceId, targetPosition[0], targetPosition[1], targetPosition[2]);
        }
    }

    private void onRotationData(@NonNull final SensorValue sensorValue) {
        final Quaternion quaternion = sensorValue.quaternion();
        mPitch.setText(formatAngle(quaternion.pitch()));
        mRoll.setText(formatAngle(quaternion.roll()));
        mYaw.setText(formatAngle(quaternion.yaw()));

        // Update the sound location to match it with the new target position.
        if (sourceId != GvrAudioEngine.INVALID_ID) {
            gvrAudioEngine.setHeadRotation(-(float)sensorValue.quaternion().x(),
                    (float)sensorValue.quaternion().z(),
                    (float)sensorValue.quaternion().y(),
                    -(float)sensorValue.quaternion().w());
            gvrAudioEngine.setSoundObjectPosition(
                    sourceId, targetPosition[0], targetPosition[1], targetPosition[2]);
        }
    }

    private void onHeadsetChange(@NonNull final SensorValue sensorValue) {

    }

    private void showError(final String message) {
        final Context context = getContext();
        if (context != null) {
            final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else {
            Log.e(TAG, "Device error: " + message);
        }
    }

    private static String formatValue(final double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private static String formatAngle(final double radians) {
        final double degrees = radians * 180 / Math.PI;
        return String.format(Locale.US, "%.2f°", degrees);
    }


}
