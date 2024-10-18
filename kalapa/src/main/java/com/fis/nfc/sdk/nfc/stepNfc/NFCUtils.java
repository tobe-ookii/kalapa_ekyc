package com.fis.nfc.sdk.nfc.stepNfc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.fis.ekyc.nfc.build_in.IDCardReader;
import com.fis.ekyc.nfc.build_in.model.CardResult;
import com.fis.ekyc.nfc.build_in.model.ResultCode;
import com.fis.nfc.sdk.nfc.stepNfc.model.DataVerifyObject;
import com.fis.nfc.sdk.nfc.util.CustomSdk;
import com.fis.nfc.util.Cache;
import com.google.gson.GsonBuilder;

import java.util.Arrays;

import vn.kalapa.ekyc.utils.Helpers;

public class NFCUtils {
    public static final int DEVICE_NOT_SUPPORT = -1;
    public static final int DEVICE_SUPPORT = 1;
    public static final int NFC_IS_OFF = 0;
    public static final String TAG = NFCUtils.class.getCanonicalName();
    private BroadcastReceiver mReceiver;
    CardResult mCardResult;
    private Activity activity;
    private NFCListener listener;
    String idCardNumber;

    public NFCUtils() {
    }

    public NFCUtils setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
        return this;
    }

    public NFCUtils setListener(NFCListener listener) {
        this.listener = listener;
        this.checkNFCStatus();
        return this;
    }

    public NFCUtils init(@NonNull Activity activity) {
        this.activity = activity;
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.nfc.action.ADAPTER_STATE_CHANGED")) {
                    int state = intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", 1);
                    switch (state) {
                        case 1:
                            if (NFCUtils.this.listener != null) {
                                NFCUtils.this.listener.CheckNFCAvailable(0);
                            }
                            break;
                        case 2:
                            if (NFCUtils.this.listener != null) {
                                NFCUtils.this.listener.CheckNFCAvailable(1);
                            }

                            NFCUtils.this.callOnResume();
                        case 3:
                        case 4:
                    }
                }

            }
        };
        IntentFilter filter = new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED");
        activity.registerReceiver(this.mReceiver, filter);
        return this;
    }

    public void deinit() {
        Helpers.Companion.printLog("NFCUtils deInit");
        this.activity.unregisterReceiver(this.mReceiver);
    }

    private void checkNFCStatus() {
        byte status;
        if (this.checkNFCInDevice()) {
            NfcManager manager = (NfcManager) this.activity.getSystemService(Context.NFC_SERVICE);
            if (manager.getDefaultAdapter() != null && manager.getDefaultAdapter().isEnabled()) {
                status = 1;
            } else {
                status = 0;
            }
        } else {
            status = -1;
        }

        if (this.listener != null) {
            this.listener.CheckNFCAvailable(status);
        }

    }

    private boolean checkNFCInDevice() {
        return this.activity.getPackageManager().hasSystemFeature("android.hardware.nfc");
    }

    public void callOnResume() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this.activity);
        if (adapter != null) {
            Intent intent = new Intent(this.activity.getApplicationContext(), this.activity.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this.activity, 0, intent, PendingIntent.FLAG_MUTABLE);
            String[][] filter = new String[][]{{"android.nfc.tech.IsoDep"}};
            adapter.enableForegroundDispatch(this.activity, pendingIntent, (IntentFilter[]) null, filter);
        }

    }

    public void callOnPause() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this.activity);
        if (adapter != null) {
            adapter.disableForegroundDispatch(this.activity);
        }

    }

    public void callOnNewIntent(Intent intent) {
        Tag tag = (Tag) intent.getExtras().getParcelable("android.nfc.extra.TAG");
        (new ReadTask(IsoDep.get(tag), this.idCardNumber)).execute(new Void[0]);
    }

    private class ReadTask extends AsyncTask<Void, String, String> {
        private IsoDep isoDep;
        private String idcard;

        private ReadTask(IsoDep isoDep, String idcard) {
            this.isoDep = isoDep;
            this.idcard = idcard;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            if (NFCUtils.this.listener != null) {
                NFCUtils.this.listener.OnStartProcess();
            }

        }

        @SuppressLint({"NewApi"})
        protected String doInBackground(Void... params) {
            String response = "";

            try {
                this.publishProgress(new String[]{"Đang giao tiếp NFC"});
                IDCardReader rd = new IDCardReader();
                String cccdId = this.idcard;

                try {
                    NFCUtils.this.mCardResult = rd.readData(this.isoDep, cccdId, true, true, true);
                } catch (Exception var41) {
                    Exception ex = var41;
                    System.out.println(Arrays.toString(ex.getStackTrace()));
                    CustomSdk.Companion.getErrorCodeCallback().invoke(NFCUtils.this.mCardResult.getCode());
                }

                byte[] sod = NFCUtils.this.mCardResult.getSOD();
                byte[] data1 = NFCUtils.this.mCardResult.getDG(1);
                byte[] data2 = NFCUtils.this.mCardResult.getDG(2);
                byte[] data3 = NFCUtils.this.mCardResult.getDG(3);
                byte[] data4 = NFCUtils.this.mCardResult.getDG(4);
                byte[] data5 = NFCUtils.this.mCardResult.getDG(5);
                byte[] data6 = NFCUtils.this.mCardResult.getDG(6);
                byte[] data7 = NFCUtils.this.mCardResult.getDG(7);
                byte[] data8 = NFCUtils.this.mCardResult.getDG(8);
                byte[] data9 = NFCUtils.this.mCardResult.getDG(9);
                byte[] data10 = NFCUtils.this.mCardResult.getDG(10);
                byte[] data11 = NFCUtils.this.mCardResult.getDG(11);
                byte[] data12 = NFCUtils.this.mCardResult.getDG(12);
                byte[] data13 = NFCUtils.this.mCardResult.getDG(13);
                byte[] data14 = NFCUtils.this.mCardResult.getDG(14);
                byte[] data15 = NFCUtils.this.mCardResult.getDG(15);
                byte[] data16 = NFCUtils.this.mCardResult.getDG(16);
                String dg1 = data1 == null ? "" : Base64.encodeToString(data1, 0).replaceAll("\n", "");
                String dg2 = data2 == null ? "" : Base64.encodeToString(data2, 0).replaceAll("\n", "");
                String dg3 = data3 == null ? "" : Base64.encodeToString(data3, 0).replaceAll("\n", "");
                String dg4 = data4 == null ? "" : Base64.encodeToString(data4, 0).replaceAll("\n", "");
                String dg5 = data5 == null ? "" : Base64.encodeToString(data5, 0).replaceAll("\n", "");
                String dg6 = data6 == null ? "" : Base64.encodeToString(data6, 0).replaceAll("\n", "");
                String dg7 = data7 == null ? "" : Base64.encodeToString(data7, 0).replaceAll("\n", "");
                String dg8 = data8 == null ? "" : Base64.encodeToString(data8, 0).replaceAll("\n", "");
                String dg9 = data9 == null ? "" : Base64.encodeToString(data9, 0).replaceAll("\n", "");
                String dg10 = data10 == null ? "" : Base64.encodeToString(data10, 0).replaceAll("\n", "");
                String dg11 = data11 == null ? "" : Base64.encodeToString(data11, 0).replaceAll("\n", "");
                String dg12 = data12 == null ? "" : Base64.encodeToString(data12, 0).replaceAll("\n", "");
                String dg13 = data13 == null ? "" : Base64.encodeToString(data13, 0).replaceAll("\n", "");
                String dg14 = data14 == null ? "" : Base64.encodeToString(data14, 0).replaceAll("\n", "");
                String dg15 = data15 == null ? "" : Base64.encodeToString(data15, 0).replaceAll("\n", "");
                String dg16 = data16 == null ? "" : Base64.encodeToString(data16, 0).replaceAll("\n", "");
                String sodData = sod == null ? "" : Base64.encodeToString(sod, 0).replaceAll("\n", "");
                DataVerifyObject cardObject = new DataVerifyObject(sodData, dg1, dg2, dg3, dg4, dg5, dg6, dg7, dg8, dg9, dg10, dg11, dg12, dg13, dg14, dg15, dg16);
                Cache.Companion.setJsonNfcObject(cardObject);
                CardResult result = NFCUtils.this.mCardResult;
                if (result != null && (result.getCode() == ResultCode.SUCCESS || result.getCode() == ResultCode.SUCCESS_WITH_WARNING)) {
                    Cache.Companion.setJsonNfc((new GsonBuilder()).serializeNulls().create().toJson(cardObject));
                    if (NFCUtils.this.listener != null) {
                        NFCUtils.this.listener.OnSuccess(Cache.Companion.getJsonNfc());
                    }
                } else if (NFCUtils.this.listener != null) {
                    NFCUtils.this.listener.OnError(NFCUtils.this.mCardResult.getCode());
                }

                response = Cache.Companion.getJsonNfc();
                return response;
            } catch (Exception var42) {
                Exception e = var42;
                e.printStackTrace();
                if (NFCUtils.this.listener != null) {
                    NFCUtils.this.listener.OnError(NFCUtils.this.mCardResult.getCode());
                }

                return "";
            }
        }

        protected void onPostExecute(String result) {
            Log.d(NFCUtils.TAG, result);
            if (NFCUtils.this.listener != null) {
                NFCUtils.this.listener.OnProcessFinished();
            }

        }
    }

    public interface NFCListener {
        void OnSuccess(String var1);

        void OnFail(String var1);

        void OnError(ResultCode var1);

        void OnStartProcess();

        void OnProcessFinished();

        void CheckNFCAvailable(int var1);
    }
}
