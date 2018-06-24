package com.samourai.ponydirect;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.util.encoders.Hex;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.sms.SMSReceiver;

import com.yanzhenjie.zbar.Symbol;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog progress = null;

    private final static int SCAN_HEX_TX = 2011;

    private IntentFilter isFilter = null;
    private BroadcastReceiver isReceiver = null;

    private TextView tvLog = null;

    private static final int PERMISSIONS_CODE = 0;

    public static final String ACTION_INTENT = "com.samourai.ponydirect.LOG";
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            if(ACTION_INTENT.equals(intent.getAction()) && tvLog != null) {

                String strText = intent.getStringExtra("msg");
                String strLog = tvLog.getText().toString();

                if(strLog.length() > 0) {
                    strLog += "\n";
                }

                strLog += strText;

                tvLog.setText(strLog);

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                doGetHex();
            }
        });

        if(!hasReadSmsPermission() || !hasSendSmsPermission() || !hasCameraPermission()) {
            showRequestPermissionsInfoAlertDialog();
        }

        tvLog = findViewById(R.id.log);

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(receiver, filter);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(isReceiver == null)    {
            isFilter = new IntentFilter();
            isFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
            isFilter.setPriority(2147483647);
            isReceiver = new SMSReceiver();
            MainActivity.this.registerReceiver(isReceiver, isFilter);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            if(isReceiver != null)    {
                MainActivity.this.unregisterReceiver(isReceiver);
            }
        }
        catch(IllegalArgumentException iae) {
            ;
        }

    }

    @Override
    protected void onDestroy() {

        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(receiver);

        try {
            if(isReceiver != null)    {
                MainActivity.this.unregisterReceiver(isReceiver);
            }
        }
        catch(IllegalArgumentException iae) {
            ;
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            doSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == SCAN_HEX_TX)	{

            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT).trim();

                doSendHex(strResult);

            }
        }
        else {
            ;
        }

    }

    private void doSettings()	{
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private void doGetHex()    {

        final EditText edHexTx = new EditText(MainActivity.this);
        edHexTx.setSingleLine(false);
        edHexTx.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edHexTx.setLines(10);
        edHexTx.setHint(R.string.tx_hex);
        edHexTx.setGravity(Gravity.START);
        TextWatcher textWatcher = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                edHexTx.setSelection(0);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edHexTx.addTextChangedListener(textWatcher);

        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.app_name)
                .setView(edHexTx)
                .setMessage(R.string.enter_tx_hex)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        final String strHexTx = edHexTx.getText().toString().trim();

                        Log.d("MainActivity", strHexTx);

                        doSendHex(strHexTx);

                    }
                }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();

                        dialog.dismiss();

                        doScanHexTx();
                    }
                });

        dlg.show();

    }

    private void doSendHex(final String hexTx)    {

        if(!hexTx.matches("^[A-Fa-f0-9]+$")) {
            return;
        }

        final Transaction tx = new Transaction(MainNetParams.get(), Hex.decode(hexTx));
        final String msg = MainActivity.this.getString(R.string.broadcast) + ":" + tx.getHashAsString() + " " + getText(R.string.to) + " " + PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.SMS_RELAY, MainActivity.this.getText(R.string.default_relay).toString()) + " ?";

        final TextView tvHexTx = new TextView(MainActivity.this);
        tvHexTx.setSingleLine(false);
        tvHexTx.setLines(10);
        tvHexTx.setGravity(Gravity.START);
        tvHexTx.setText(hexTx);

        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        TxFactory.getInstance(MainActivity.this).putPayload(hexTx);

                    }

                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }
                });

        dlg.show();

    }

    private void doScanHexTx()   {
        Intent intent = new Intent(MainActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_HEX_TX);
    }

    private void showRequestPermissionsInfoAlertDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_alert_dialog_title);
        builder.setMessage(R.string.permission_dialog_message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                requestPermissions();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();

    }

    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasSendSmsPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.SEND_SMS) &&
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_SMS) &&
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)
                ) {
            Log.d("MainActivity", "shouldShowRequestPermissionRationale(), no permission requested");
            return;
        }

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, Manifest.permission.CAMERA}, PERMISSIONS_CODE);

    }

}
