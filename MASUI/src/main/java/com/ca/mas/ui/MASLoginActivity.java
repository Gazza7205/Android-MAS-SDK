package com.ca.mas.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASUser;
import com.ca.mas.foundation.auth.MASAuthenticationProvider;
import com.ca.mas.foundation.auth.MASAuthenticationProviders;
import com.ca.mas.foundation.auth.MASProximityLogin;
import com.ca.mas.foundation.auth.MASProximityLoginBLE;
import com.ca.mas.foundation.auth.MASProximityLoginBLECentralListener;
import com.ca.mas.foundation.auth.MASProximityLoginNFC;
import com.ca.mas.foundation.auth.MASProximityLoginQRCode;

import java.util.List;

import static com.ca.mas.core.MAG.DEBUG;
import static com.ca.mas.core.MAG.TAG;

public class MASLoginActivity extends AppCompatActivity {
    public static final String REQUEST_ID = "requestID";
    public static final String PROVIDERS = "providers";
    private long requestId;

    private Context mContext;
    private EditText mEditTextUsername;
    private EditText mEditTextPassword;
    private GridLayout mGridLayout;
    private MASAuthenticationProviders providers;
    private MASProximityLogin qrCode;
    private MASProximityLogin nfc;
    private MASProximityLogin ble;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mas_login);
        mContext = this;

        Intent intent = getIntent();
        if (intent != null) {
            Parcelable parcel = intent.getParcelableExtra(PROVIDERS);
            if (parcel != null && parcel instanceof MASAuthenticationProviders) {
                providers = (MASAuthenticationProviders) parcel;
                requestId = intent.getLongExtra(REQUEST_ID, -1);
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mEditTextUsername = (EditText) findViewById(R.id.activity_mas_login_edit_text_username);
        mEditTextPassword = (EditText) findViewById(R.id.activity_mas_login_edit_text_password);

        // Proximity Login
        qrCode = getQrCode();
        nfc = getNfc();
        ble = getBle();
        initProximity(qrCode);
        initProximity(nfc);
        initProximity(ble);

        // Social Login
        mGridLayout = (GridLayout) findViewById(R.id.activity_mas_login_grid_layout);
        List<MASAuthenticationProvider> providerList = providers.getProviders();
        for (final MASAuthenticationProvider p : providerList) {
            String identifier = p.getIdentifier();
            Integer id = null;
            switch (identifier) {
                case "enterprise":
                    id = R.id.activity_mas_login_enterprise;
                    break;
                case "facebook":
                    id = R.id.activity_mas_login_facebook;
                    break;
                case "google":
                    id = R.id.activity_mas_login_google;
                    break;
                case "linkedin":
                    id = R.id.activity_mas_login_linked_in;
                    break;
                case "salesforce":
                    id = R.id.activity_mas_login_salesforce;
                    break;
                case "qrcode":
                    id = R.id.activity_mas_login_qr_code;
                    break;
            }

            if (id != null) {
                View button = findViewById(id);
                if (!p.isProximityLogin()) {
                    if (providers.getIdp().equals("all") || providers.getIdp().equalsIgnoreCase(identifier)) {
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CustomTabs.socialLogin(mContext, p);
                                finish();
                            }
                        });
                    }
                } else if (identifier.equalsIgnoreCase("qrcode")) {
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.qr_code_dialog, null);
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(500, 500);
                            ImageView imageView = (ImageView) qrCode.render();
                            imageView.setLayoutParams(layoutParams);
                            linearLayout.addView(imageView);
                            new AlertDialog.Builder(MASLoginActivity.this)
                                    .setView(linearLayout)
                                    .setNegativeButton("Done", null)
                                    .show();
                        }
                    });
                } else {
                    mGridLayout.removeView(button);
                }
            }
        }

        int numButtons = providerList.size(); // Social buttons + QR button
        updateGridLayoutNumRowsColumns(numButtons);
    }

    private void updateGridLayoutNumRowsColumns(int numButtons) {
        int numRows = (int) Math.ceil((double) numButtons / 3);
        int numColumns = 0;
        switch (numButtons) {
            case 0:
                numColumns = 0;
                break;
            case 1:
                numColumns = 1;
                break;
            case 2:
            case 4:
                numColumns = 2;
                break;
            default:
                numColumns = 3;
                break;
        }

        mGridLayout.setRowCount(numRows);
        mGridLayout.setColumnCount(numColumns);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_login_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.menu_bluetooth) {
            new AlertDialog.Builder(this)
                    .setView(R.layout.proximity_dialog)
                    .setPositiveButton("OKAY", null)
                    .show();
            return true;
        } else if (id == R.id.menu_nfc) {
            new AlertDialog.Builder(this)
                    .setView(R.layout.proximity_dialog)
                    .setPositiveButton("OKAY", null)
                    .show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopProximity(qrCode);
        stopProximity(nfc);
        stopProximity(ble);
    }

    public void onClickLogin(View view) {
        int id = view.getId();
        if (id == R.id.activity_mas_login_button_login) {
            final ProgressDialog progress = new ProgressDialog(this);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setCancelable(false);
            progress.show();
            String username = mEditTextUsername.getText().toString();
            char[] password = mEditTextPassword.getText().toString().toCharArray();

            MASUser.login(username, password, new MASCallback<MASUser>() {
                @Override
                public Handler getHandler() {
                    return new Handler(Looper.getMainLooper());
                }

                @Override
                public void onSuccess(MASUser result) {
                    progress.dismiss();
                    finish();
                }

                @Override
                public void onError(Throwable e) {
                    progress.dismiss();
                    Toast.makeText(MASLoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private MASProximityLoginQRCode getQrCode() {
        return new MASProximityLoginQRCode() {
            @Override
            public void onError(int errorCode, final String m, Exception e) {
                // Hide QR Code option
                View qrButton = findViewById(R.id.activity_mas_login_qr_code);
                mGridLayout.removeView(qrButton);
            }

            @Override
            protected void onAuthCodeReceived(String code) {
                super.onAuthCodeReceived(code);
                onProximityAuthenticated();
            }
        };
    }

    private MASProximityLoginNFC getNfc() {
        return new MASProximityLoginNFC() {
            @Override
            public void onError(int errorCode, final String m, Exception e) {
                if (DEBUG) Log.i(TAG, m);
            }

            @Override
            protected void onAuthCodeReceived(String code) {
                super.onAuthCodeReceived(code);
                onProximityAuthenticated();
            }
        };
    }

    private MASProximityLoginBLE getBle() {
        // Prepare callback to receive status update
        MASProximityLoginBLECentralListener callback = new MASProximityLoginBLECentralListener() {
            @Override
            public void onStatusUpdate(int state) {
                switch (state) {
                    case MASProximityLoginBLECentralListener.BLE_STATE_SCAN_STARTED:
                        if (DEBUG) Log.i(TAG, "Scan Started");
                        break;
                    case MASProximityLoginBLECentralListener.BLE_STATE_SCAN_STOPPED:
                        if (DEBUG) Log.i(TAG, "Scan Stopped");
                        break;
                    case MASProximityLoginBLECentralListener.BLE_STATE_DEVICE_DETECTED:
                        if (DEBUG) Log.i(TAG, "Device detected");
                        break;
                    case MASProximityLoginBLECentralListener.BLE_STATE_CONNECTED:
                        if (DEBUG) Log.i(TAG, "Connected to Gatt Server");
                        break;
                    case MASProximityLoginBLECentralListener.BLE_STATE_DISCONNECTED:
                        if (DEBUG) Log.i(TAG, "Disconnected from Gatt Server");
                        break;
                    case MASProximityLoginBLECentralListener.BLE_STATE_SERVICE_DISCOVERED:
                        if (DEBUG) Log.i(TAG, "Service Discovered");
                        break;
                    case MASProximityLoginBLECentralListener.BLE_STATE_CHARACTERISTIC_FOUND:
                        if (DEBUG) Log.i(TAG, "Characteristic Found");
                        break;
                    case MASProximityLoginBLECentralListener.BLE_STATE_CHARACTERISTIC_WRITTEN:
                        if (DEBUG) Log.i(TAG, "Writing data to Characteristic... ");
                        break;
                    case MASProximityLoginBLECentralListener.BLE_STATE_AUTH_SUCCEEDED:
                        if (DEBUG) Log.i(TAG, "Auth Succeeded");
                        break;
                    case MASProximityLoginBLECentralListener.BLE_STATE_AUTH_FAILED:
                        if (DEBUG) Log.i(TAG, "Auth Failed");
                        break;
                }
            }
        };

        return new MASProximityLoginBLE(callback) {
            @Override
            public void onError(int errorCode, final String m, Exception e) {
                if (DEBUG) Log.i(TAG, m);
            }

            @Override
            protected void onAuthCodeReceived(String code) {
                super.onAuthCodeReceived(code);
                onProximityAuthenticated();
            }
        };
    }

    private void initProximity(MASProximityLogin masProximityLogin) {
        boolean init = masProximityLogin.init(this, requestId, providers);
        if (init) {
            masProximityLogin.start();
        }
    }

    private void stopProximity(MASProximityLogin masProximityLogin) {
        if (masProximityLogin != null) {
            masProximityLogin.stop();
        }
    }

    private void onProximityAuthenticated() {
        //Fetch the user profile
        MASUser.login(null);
        finish();
    }
}
