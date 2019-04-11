package com.codeledger.basicxrsonic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bose.blecore.Logger;
import com.bose.blecore.ScanError;
import com.bose.bosewearableui.DeviceSearchFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    private static final int REQUEST_CODE_SCAN = 1;
    private static final int AUTO_CONNECT_TIMEOUT = 5;
    private static final String PREF_AUTO_CONNECT_ENABLED = "auto-connect-enabled";
    private static final String PREF_LAST_CONNECTED_ADDRESS = "last-connected-address";

    private SharedPreferences mPrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        final Button searchButton = view.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> onSearchClicked());

        final Switch autoConnectSwitch = view.findViewById(R.id.autoConnectSwitch);
        autoConnectSwitch.setChecked(mPrefs.getBoolean(PREF_AUTO_CONNECT_ENABLED, true));
        autoConnectSwitch.setOnCheckedChangeListener((compoundButton, enabled) -> {
            mPrefs.edit()
                    .putBoolean(PREF_AUTO_CONNECT_ENABLED, enabled)
                    .apply();
        });

        final Button simulatedDeviceButton = view.findViewById(R.id.simulatedDeviceButton);
        simulatedDeviceButton.setOnClickListener(v -> onSimulatedDeviceClicked());

        final TextView versionText = view.findViewById(R.id.versionText);
        versionText.setText(getString(R.string.version_name, BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE));
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SCAN:
                if (resultCode == Activity.RESULT_OK) {
                    onDeviceSelected(data.getStringExtra(DeviceSearchFragment.EXTRA_DEVICE_ADDRESS));
                } else if (resultCode == DeviceSearchFragment.RESULT_SCAN_ERROR) {
                    final ScanError error = (ScanError) data.getSerializableExtra(DeviceSearchFragment.EXTRA_ERROR);
                    showScanError(error);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void onSearchClicked() {
        final String autoConnectAddress;
        if (mPrefs.getBoolean(PREF_AUTO_CONNECT_ENABLED, true)) {
            autoConnectAddress = mPrefs.getString(PREF_LAST_CONNECTED_ADDRESS, null);
        } else {
            autoConnectAddress = null;
        }

        final DeviceSearchFragment fragment = DeviceSearchFragment.create(getString(R.string.app_name),
                autoConnectAddress, AUTO_CONNECT_TIMEOUT);
        fragment.setTargetFragment(this, REQUEST_CODE_SCAN);
        fragment.show(getFragmentManager(), "scan");
    }

    private void onSimulatedDeviceClicked() {
        final Bundle args = new Bundle();
        args.putBoolean(MainFragment.ARG_USE_SIMULATED_DEVICE, true);
        navigateToDeviceFragment(args);
    }

    private void onDeviceSelected(@NonNull final String deviceAddress) {
        mPrefs.edit()
                .putString(PREF_LAST_CONNECTED_ADDRESS, deviceAddress)
                .apply();

        final Bundle args = new Bundle();
        args.putString(MainFragment.ARG_DEVICE_ADDRESS, deviceAddress);
        navigateToDeviceFragment(args);
    }

    private void navigateToDeviceFragment(@NonNull final Bundle args) {
        final MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        getFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.content, fragment)
                .commit();
    }

    private void showScanError(@NonNull final ScanError error) {
        final Context context = getContext();
        if (context == null) {
            Logger.e(Logger.Topic.DISCOVERY, "Scan failed with " + error);
            return;
        }

        final String reasonStr;
        switch (error) {
            case ALREADY_STARTED:
                reasonStr = context.getString(R.string.scan_error_already_started);
                break;
            case INTERNAL_ERROR:
                reasonStr = context.getString(R.string.scan_error_internal);
                break;
            case PERMISSION_DENIED:
                reasonStr = context.getString(R.string.scan_error_permission_denied);
                break;
            case BLUETOOTH_DISABLED:
                reasonStr = context.getString(R.string.scan_error_bluetooth_disabled);
                break;
            case FEATURE_UNSUPPORTED:
                reasonStr = context.getString(R.string.scan_error_feature_unsupported);
                break;
            case APPLICATION_REGISTRATION_FAILED:
                reasonStr = context.getString(R.string.scan_error_application_registration_failed);
                break;
            case UNKNOWN:
            default:
                reasonStr = context.getString(R.string.scan_error_unknown);
                break;
        }

        Toast.makeText(context, context.getString(R.string.scan_failed, reasonStr),
                Toast.LENGTH_LONG)
                .show();
    }
}
