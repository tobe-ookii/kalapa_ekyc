package vn.kalapa.demo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.RequestBody;
import vn.kalapa.demo.activities.BaseActivity;
import vn.kalapa.demo.activities.ResultActivity;
import vn.kalapa.demo.models.NFCCardData;
import vn.kalapa.demo.models.NFCVerificationData;
import vn.kalapa.demo.network.Client;
import vn.kalapa.demo.utils.Helpers;
import vn.kalapa.demo.utils.LogUtils;
import vn.kalapa.ekyc.FaceOTPFlowType;
import vn.kalapa.ekyc.KalapaCaptureHandler;
import vn.kalapa.ekyc.KalapaHandler;
import vn.kalapa.ekyc.KalapaNFCHandler;
import vn.kalapa.ekyc.KalapaSDKResultCode;
import vn.kalapa.ekyc.KalapaSDK;
import vn.kalapa.ekyc.KalapaSDKCallback;
import vn.kalapa.ekyc.KalapaSDKConfig;
import vn.kalapa.ekyc.KalapaSDKMediaType;
import vn.kalapa.ekyc.managers.AESCryptor;
import vn.kalapa.ekyc.models.KalapaResult;
import vn.kalapa.ekyc.models.PreferencesConfig;
import vn.kalapa.ekyc.networks.KalapaAPI;
import vn.kalapa.ekyc.utils.BitmapUtil;
import vn.kalapa.ekyc.utils.Common;
import vn.kalapa.ekyc.utils.LocaleHelper;
import vn.kalapa.ekyc.views.ProgressView;

public class MainActivityJava extends BaseActivity {
    private static final String TAG = "MainActivity";
    private Button ekycButton;
    private TextView tvWelcome;
    private TextView tvWelcomeSubtitle;
    private View container;
    private TextView tvVersion;

    private PreferencesConfig preferencesConfig;

    private int livenessVersion = Common.LIVENESS_VERSION.PASSIVE.getVersion();
    private String scenario = FaceOTPFlowType.VERIFY.name();
    private KalapaSDKConfig sdkConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Helpers.Companion.init(this);
        setupBinding();
        ekycButton.setOnClickListener(v -> {
            if (isAppConfigSet()) {
                if (!Common.Companion.isOnline(MainActivityJava.this)) {
                    Helpers.Companion.showDialog(MainActivityJava.this,
                            getResources().getString(R.string.klp_face_otp_demo),
                            getResources().getString(R.string.klp_face_otp_error_network), R.drawable.frowning_face);
                    return;
                }
//                startLivenessAndNFC();
//                String message = "Something!!!";
//                LogUtils.Companion.printLog("Try Encrypt " + message);
//                LogUtils.Companion.printLog(AESCryptor.encryptText(message));
//                startFaceOTP();
//                startCapture();
                startEKYC();
//                KalapaSDK.Companion.startConfirmForResult(MainActivityJava.this, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImY1NGM2ZWM4YmNlYzQ4OTk5NDM1OTJkZTA4YjZkMmE0IiwidWlkIjoiM2FkODRkMGUxYTIwNGZkYWEyZGUwYWM5NTNmNzA2YTUiLCJjaWQiOiJpbnRlcm5hbF9la3ljIiwiaWF0IjoxNzE3NjY4OTk4fQ.KPPuHUk_gNnuDhB8evoU2_VekxY77x96U1s6L6eZYAY"
//                , sdkConfig, new KalapaHandler() {
//                    @Override
//                    public void onError(@NonNull KalapaSDKResultCode resultCode) {
//
//                    }
//
//                    @Override
//                    public void onComplete(@NonNull KalapaResult kalapaResult) {
//                        super.onComplete(kalapaResult);
//                        ExampleGlobalClass.kalapaResult = kalapaResult;
//                        if (KalapaSDK.Companion.isFaceBitmapInitialized()) ExampleGlobalClass.faceImage = KalapaSDK.faceBitmap;
//                        if (KalapaSDK.Companion.isFrontBitmapInitialized()) ExampleGlobalClass.frontImage = KalapaSDK.frontBitmap;
//                        if (KalapaSDK.Companion.isBackBitmapInitialized()) ExampleGlobalClass.backImage = KalapaSDK.backBitmap;
//                        ExampleGlobalClass.nfcData = new NFCVerificationData(new NFCCardData(kalapaResult.getNfc_data(), true), null, null);
//                        startActivity(new Intent(MainActivityJava.this, ResultActivity.class));
//                    }
//                });

            }
        });
    }

    private void startCapture() {
        KalapaSDK.Companion.startCaptureForResult(MainActivityJava.this, sdkConfig, new KalapaCaptureHandler() {
            @Override
            public void process(@NonNull String base64, @NonNull KalapaSDKMediaType mediaType, @NonNull KalapaSDKCallback callback) {
                callback.sendDone(() -> {
                    KalapaSDK.Companion.startCaptureBackForResult(MainActivityJava.this, sdkConfig, new KalapaCaptureHandler() {
                        @Override
                        public void process(@NonNull String base64, @NonNull KalapaSDKMediaType mediaType, @NonNull KalapaSDKCallback callback) {
                            KalapaSDK.Companion.startLivenessForResult(MainActivityJava.this, sdkConfig, new KalapaCaptureHandler() {
                                @Override
                                public void process(@NonNull String base64, @NonNull KalapaSDKMediaType mediaType, @NonNull KalapaSDKCallback callback) {
                                    callback.sendDone(() -> {
                                        return null;
                                    });
                                }

                                @Override
                                public void onError(@NonNull KalapaSDKResultCode resultCode) {

                                }
                            });
                        }

                        @Override
                        public void onError(@NonNull KalapaSDKResultCode resultCode) {

                        }
                    });
                    return null;
                });
            }

            @Override
            public void onError(@NonNull KalapaSDKResultCode resultCode) {

            }
        });
    }

    public void startFaceOTP() {
//        if (preferencesConfig.getScenario().equals(FaceOTPFlowType.PASSPORT.name()))
//            startPassport();
//        else startLiveness();
    }

    private void startEKYC() {
        if (Common.Companion.isOnline(MainActivityJava.this)) {
            String[] acceptedDocument = {"CCCD", "CMND", "CMND12"};
            ProgressView.Companion.showProgress(MainActivityJava.this, ProgressView.ProgressViewType.LOADING, null, null);
            KalapaAPI.Companion.doRequestGetSession(preferencesConfig.getEnv(), preferencesConfig.getToken(), "true", "true", "true", true, acceptedDocument, 50,
                    createSessionResult -> {
                        ProgressView.Companion.hideProgress(true);
                        LogUtils.Companion.printLog("doRequestGetSession createSessionResult: " + createSessionResult.component1());
                        KalapaSDK.Companion.startFullEKYC(MainActivityJava.this, createSessionResult.component1(), sdkConfig, new KalapaHandler() {

                            @Override
                            public void onError(@NonNull KalapaSDKResultCode resultCode) {
                                Helpers.Companion.showDialog(MainActivityJava.this,
                                        getString(R.string.klp_face_otp_alert_title),
                                        "Error happened due to " + resultCode.name(),
                                        R.drawable.frowning_face
                                );
                            }

                            @Override
                            public void onComplete(@NonNull KalapaResult kalapaResult) {
                                LogUtils.Companion.printLog("startFullEKYC onComplete: " + kalapaResult + " \n " + kalapaResult.component1());
                                ExampleGlobalClass.kalapaResult = kalapaResult;
                                if (KalapaSDK.Companion.isFaceBitmapInitialized()) ExampleGlobalClass.faceImage = KalapaSDK.faceBitmap;
                                if (KalapaSDK.Companion.isFrontBitmapInitialized()) ExampleGlobalClass.frontImage = KalapaSDK.frontBitmap;
                                if (KalapaSDK.Companion.isBackBitmapInitialized()) ExampleGlobalClass.backImage = KalapaSDK.backBitmap;
                                ExampleGlobalClass.nfcData = new NFCVerificationData(new NFCCardData(kalapaResult.getNfc_data(), true), null, null);
                                startActivity(new Intent(MainActivityJava.this, ResultActivity.class));
                            }

                        });
                        return null;
                    }, kalapaError -> {
                        ProgressView.Companion.hideProgress(true);
                        Helpers.Companion.showDialog(MainActivityJava.this,
                                getString(R.string.klp_face_otp_alert_title),
                                kalapaError.getMessageError(),
                                R.drawable.ic_failed_solid
                        );
                        LogUtils.Companion.printLog("doRequestGetSession kalapaError: " + kalapaError);
                        return null;
                    });
        } else {
            Helpers.Companion.showDialog(MainActivityJava.this,
                    getString(R.string.klp_face_otp_alert_title),
                    getString(R.string.klp_face_otp_error_network),
                    R.drawable.ic_failed_solid
            );
        }
    }

    public void startLivenessAndNFC() {
        KalapaSDK.Companion.startLivenessForResult(MainActivityJava.this, sdkConfig, new KalapaCaptureHandler() {
            String selfieFace = null;
            String NFC_SELFIE_PATH = preferencesConfig.getEnv() + "/verify?lang=" + preferencesConfig.getLanguage();

            @Override
            public void process(@NonNull String base64, @NonNull KalapaSDKMediaType mediaType, @NonNull KalapaSDKCallback callback) {
                selfieFace = base64;
                callback.sendDone(() -> {
                    KalapaSDK.Companion.startNFCForResult(MainActivityJava.this, sdkConfig, new KalapaNFCHandler("") {
                        @Override
                        public void process(@NonNull String idCardNumber, @NonNull String nfcData, @NonNull KalapaSDKCallback callback) {
                            /*  Check NFC + Face
                             *  What if Face not valid (At Selfie Step?)
                             * */
                            byte[] imageInBytes = BitmapUtil.Companion.convertBase64ToBytes(base64);

                            RequestBody requestFile = RequestBody.create(imageInBytes);
                            new Client(preferencesConfig.getEnv()).postFormData(NFC_SELFIE_PATH, new HashMap<String, String>() {{
                                        put("Authorization",
//                                                preferencesConfig.getToken());
                                                "5bb42ea331ee010001a0b7d76py45b3907n6vki4on4a087778352090");
                                    }},
                                    new HashMap<String, String>() {{
                                        put("user_id", idCardNumber);
//                                        put("nfc_data", nfcData);
                                        put("signature", AESCryptor.encryptText(AESCryptor.hash(imageInBytes)));
                                        put("nfc_data", AESCryptor.encryptText(nfcData));
                                    }},
                                    requestFile, "face_image", new Client.RequestListener() {

                                        @Override
                                        public void timeout() {
                                            LogUtils.Companion.printLog("timeout");
                                        }

                                        @Override
                                        public void fail(@NonNull String error) {
                                            LogUtils.Companion.printLog("fail" + error);
                                            callback.sendError(error);
                                        }

                                        @Override
                                        public void success(@NonNull JSONObject jsonObject) {
                                            LogUtils.Companion.printLog("success" + jsonObject);
                                            // Done!
                                            callback.sendDone(() -> {
                                                LogUtils.Companion.printLog("startNFCOnly ", jsonObject.toString());
                                                NFCVerificationData nfcData = NFCVerificationData.Companion.fromJson(jsonObject.toString());
                                                ExampleGlobalClass.nfcData = nfcData;
                                                ExampleGlobalClass.faceImage = BitmapUtil.Companion.base64ToBitmap(base64);
                                                LogUtils.Companion.printLog("nfcData ", nfcData);

                                                startActivity(new Intent(MainActivityJava.this, ResultActivity.class));
//                                                    Helpers.Companion.showDialog(MainActivityJava.this, getString(R.string.klp_faceOTP_register_successful), getString(R.string.klp_faceOTP_register_successful_desc), R.drawable.image_checkmark);
                                                return null;
                                            });
                                        }
                                    });
                        }

                        @Override
                        public void onError(@NonNull KalapaSDKResultCode resultCode) {
                            Helpers.Companion.showDialog(MainActivityJava.this, "Error happened due to ", resultCode.name(), R.drawable.ic_failed_solid);
                        }
                    });
                    return null;
                });
            }

            @Override
            public void onError(@NonNull KalapaSDKResultCode resultCode) {
                Helpers.Companion.showDialog(MainActivityJava.this, "Error happened due to ", resultCode.name(), R.drawable.ic_failed_solid);
            }
        });
    }

    private void startPassport() {
        KalapaSDK.Companion.startCapturingPassportForResult(MainActivityJava.this, sdkConfig, new KalapaCaptureHandler() {
            String PASSPORT_PATH = "/passport/check";

            @Override
            public void onError(@NonNull KalapaSDKResultCode resultCode) {
                Helpers.Companion.showDialog(MainActivityJava.this, "Error happened due to ", resultCode.name(), R.drawable.ic_failed_solid);
            }

            @Override
            public void process(@NonNull String base64, @NonNull KalapaSDKMediaType mediaType, @NonNull KalapaSDKCallback callback) {
                RequestBody requestFile = RequestBody.create(BitmapUtil.Companion.convertBase64ToBytes(base64));
                String fullUrl = sdkConfig.getBaseURL() + PASSPORT_PATH + "?lang=" + sdkConfig.getLanguage();
//                         RequestBody.create(MediaType.get("multipart/form-data"));
//                        new Client(BASE_URL).postFormData(SELFIE_PATH, new HashMap<>(), requestFile, "face_image", new Client.RequestListener() {
                new Client(sdkConfig.getBaseURL()).postFormData(fullUrl, new HashMap<>(), requestFile, null, new Client.RequestListener() {
                    //                new Client(sdkConfig.getBaseURL()).postFormData(fullUrl, new HashMap<>(), null, requestFile, null, new Client.RequestListener() {
                    @Override
                    public void timeout() {

                    }

                    @Override
                    public void fail(@NonNull String error) {
                        callback.sendError(error);
                    }

                    @Override
                    public void success(@NonNull JSONObject jsonObject) {
                        callback.sendDone(() -> {
                            LogUtils.Companion.printLog("Scenario? ", scenario);
                            try {
                                Helpers.Companion.showDialog(MainActivityJava.this, "Congratulation", jsonObject.get("data").toString(), R.drawable.image_checkmark);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Helpers.Companion.showDialog(MainActivityJava.this, "Congratulation, but", e.getLocalizedMessage(), R.drawable.image_checkmark);
                            }
                            return null; // Never use this return.
                        });
                    }
                });
            }
        });
    }

    private void startLiveness() {
        KalapaSDK.Companion.startLivenessForResult(
                MainActivityJava.this,
                sdkConfig,
                new KalapaCaptureHandler() {
                    String SELFIE_PATH = "/bank-svc/ekyc/check/face/base64";

                    @Override
                    public void process(String base64, KalapaSDKMediaType mediaType, KalapaSDKCallback callback) {
//                        LogUtils.Companion.printLog("Base64: ", base64);
                        RequestBody requestFile = RequestBody.create(BitmapUtil.Companion.convertBase64ToBytes(base64));
                        Map<String, String> body = new HashMap<>();
                        body.put("face_data", base64);
//                         RequestBody.create(MediaType.get("multipart/form-data"));
//                        new Client(BASE_URL).postFormData(SELFIE_PATH, new HashMap<>(), requestFile, "face_image", new Client.RequestListener() {
                        new Client(preferencesConfig.getEnv()).postFormData(SELFIE_PATH, new HashMap<>(), body, null, null, new Client.RequestListener() {
                            @Override
                            public void success(JSONObject jsonObject) {
                                // Your liveness / check selfie response here. Save it to use
                                callback.sendDone(() -> {
                                    LogUtils.Companion.printLog("Scenario? ", scenario);
                                    if (scenario.equals(FaceOTPFlowType.VERIFY.name()))
                                        Helpers.Companion.showDialog(MainActivityJava.this, getString(R.string.klp_faceOTP_successful_transaction), getString(R.string.klp_faceOTP_verify_transaction_desc), R.drawable.image_checkmark);
                                    else {
//                                        Helpers.Companion.showDialog(MainActivityJava.this, getString(R.string.klp_title_congratulation), getString(R.string.klp_message_device_activated_successfully), R.drawable.image_checkmark);
                                        startNFC();
                                    }
                                    return null; // Never use this return.
                                });
                            }

                            @Override
                            public void fail(String error) {
                                callback.sendError(error);
                            }

                            @Override
                            public void timeout() {
                                callback.sendError(getString(R.string.klp_error_happen));
                            }
                        });
                    }

                    @Override
                    public void onError(KalapaSDKResultCode resultCode) {
                        // Capture not finished return an occurred
                        Helpers.Companion.showDialog(MainActivityJava.this, "Error happened due to ", resultCode.name(), R.drawable.ic_failed_solid);
                    }
                }
        );
    }

    public void startNFC() {
        KalapaSDK.Companion.startNFCForResult(this, sdkConfig, new KalapaNFCHandler("") {
            @Override
            public void process(String idCardNumber, String nfcData, KalapaSDKCallback callback) {
                final String NFC_PATH = "/bank-svc/ekyc/check/nfc";
//                LogUtils.Companion.printLog("nfcData: " + nfcData);
                new Client(preferencesConfig.getEnv()).postXWWWFormData(NFC_PATH, new HashMap<>(), new HashMap<String, String>() {{
                    put("nfc_data", nfcData);
                }}, new Client.RequestListener() {
                    @Override
                    public void success(JSONObject jsonObject) {
                        callback.sendDone(() -> {
                            Helpers.Companion.showDialog(MainActivityJava.this, getString(R.string.klp_faceOTP_register_successful), getString(R.string.klp_faceOTP_register_successful_desc), R.drawable.image_checkmark);
                            return null;
                        });
                    }

                    @Override
                    public void fail(String error) {
                        callback.sendError(error);
                    }

                    @Override
                    public void timeout() {
                        callback.sendError(getString(R.string.klp_error_happen));
                    }
                });
                LogUtils.Companion.printLog("callback.onDone startNFCForResult");
            }

            @Override
            public void onError(KalapaSDKResultCode resultCode) {
                // MRZ not finished return an occurred
                Helpers.Companion.showDialog(MainActivityJava.this, "Error happened due to ", resultCode.name(), R.drawable.ic_failed_solid);

            }
        });
    }

    public void startNFCOnly(String token) {
        KalapaSDK.Companion.startNFCOnly(this, token, sdkConfig, new KalapaNFCHandler("") {
            @Override
            public void process(String idCardNumber, String nfcData, KalapaSDKCallback callback) {
                final String NFC_VERIFY_PATH = "https://ekyc-api.kalapa.vn/c06/verify/id";
//                LogUtils.Companion.printLog("nfcData: " + nfcData);
                new Client(preferencesConfig.getEnv()).postXWWWFormData(NFC_VERIFY_PATH, new HashMap<String, String>() {{
                    put("Authorization", token);
                }}, new HashMap<String, String>() {{
                    try {
                        put("nfc_data", URLEncoder.encode(nfcData, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        put("nfc_data", nfcData);
                        throw new RuntimeException(e);
                    }
                    put("id_number", idCardNumber);
                }}, new Client.RequestListener() {
                    @Override
                    public void success(JSONObject jsonObject) {
                        callback.sendDone(() -> {
                            LogUtils.Companion.printLog("startNFCOnly", jsonObject.toString());
                            try {
                                NFCVerificationData nfcData = NFCVerificationData.Companion.fromJson(jsonObject.getString("data"));
                                ExampleGlobalClass.nfcData = nfcData;
                                startActivity(new Intent(MainActivityJava.this, ResultActivity.class));
                            } catch (JSONException e) {
                                Helpers.Companion.showDialog(MainActivityJava.this, getString(R.string.klp_faceOTP_register_successful), getString(R.string.klp_faceOTP_register_successful_desc), R.drawable.image_checkmark);
                            }
                            return null;
                        });
                    }

                    @Override
                    public void fail(String error) {
                        callback.sendError(error);
                    }

                    @Override
                    public void timeout() {
                        callback.sendError(getString(R.string.klp_error_happen));
                    }
                });
                LogUtils.Companion.printLog("callback.onDone startNFCForResult");
            }

            @Override
            public void onError(KalapaSDKResultCode resultCode) {
                // MRZ not finished return an occurred
                Helpers.Companion.showDialog(MainActivityJava.this, "Error happened due to ", resultCode.name(), R.drawable.ic_failed_solid);
            }
        });
    }

    private boolean isAppConfigSet() {
        preferencesConfig = Helpers.Companion.getSharedPreferencesConfig(MainActivityJava.this);
        if (preferencesConfig == null) {
            openSettingUI();
            return false;
        } else {
            ExampleGlobalClass.preferencesConfig = preferencesConfig;
            Log.d(TAG, "isAppConfigSet Preferences " + preferencesConfig);
            initConfig();
        }
        return true;
    }

    private void initConfig() {
        if (preferencesConfig != null) {
            LogUtils.Companion.printLog("Setting Config " + preferencesConfig);
            KalapaSDK.config = new KalapaSDKConfig(
                    MainActivityJava.this,
                    preferencesConfig.getBackgroundColor(),
                    preferencesConfig.getMainColor(),
                    preferencesConfig.getMainTextColor(),
                    preferencesConfig.getBtnTextColor(),
                    preferencesConfig.getLivenessVersion(),
                    preferencesConfig.getLanguage(),
                    3,
                    preferencesConfig.getEnv()
            );
        } else {
            LogUtils.Companion.printLog("Setting preferencesConfig is null");
        }
    }

    private void getPreferencesValuesAndApply(boolean updateLanguage) {
        preferencesConfig = Helpers.Companion.getSharedPreferencesConfig(this);
        if (preferencesConfig != null) {
            String backgroundColor = preferencesConfig.getBackgroundColor();
            String txtColor = preferencesConfig.getMainTextColor();
            String btnColor = preferencesConfig.getMainColor();
            String btnTxtColor = preferencesConfig.getBtnTextColor();
            String lang = preferencesConfig.getLanguage();
            livenessVersion = preferencesConfig.getLivenessVersion();
//            scenario = preferencesConfig.getScenario();
            refreshText(lang);
            refreshColor(btnColor, btnTxtColor);
            if (updateLanguage) {
                sdkConfig = new KalapaSDKConfig(
                        MainActivityJava.this,
                        backgroundColor,
                        btnColor,
                        txtColor,
                        btnTxtColor,
                        livenessVersion,
                        lang,
                        3,
                        preferencesConfig.getEnv()
                );
            }
        }
    }


    private void refreshText(String lang) {
        Locale locale = new Locale(lang.equals("vi") ? LocaleHelper.VIETNAMESE : lang.equals("ko") ? LocaleHelper.KOREAN : LocaleHelper.ENGLISH);
        Locale.setDefault(locale);
        android.content.res.Configuration configuration = this.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        this.getResources().updateConfiguration(configuration, this.getResources().getDisplayMetrics());
        if (preferencesConfig != null) {
//            if (preferencesConfig.getLanguage().contains("vi")) {
//                ekycButton.setText("Bắt đầu");
//                ekycButton.invalidate();
//                tvWelcome.setText("Mời trải nghiệm");
//                tvWelcome.invalidate();
//                tvWelcomeSubtitle.setText("Xác thực khuôn mặt");
//                tvWelcomeSubtitle.invalidate();
//            } else {
//                ekycButton.setText("Start");
//                ekycButton.invalidate();
//                tvWelcome.setText("Welcome to");
//                tvWelcome.invalidate();
//                tvWelcomeSubtitle.setText("Face OTP Demo");
//                tvWelcomeSubtitle.invalidate();
//            }
            ekycButton.setText(getResources().getString(R.string.klp_start));
            ekycButton.invalidate();
            tvWelcome.setText(getResources().getString(R.string.klp_name));
            tvWelcome.invalidate();
            tvWelcomeSubtitle.setText(getResources().getString(R.string.klp_face_otp_demo));
            tvWelcomeSubtitle.invalidate();
        } else {
            ekycButton.setText(getResources().getString(R.string.klp_start));
            tvWelcome.setText(getResources().getString(R.string.klp_name));
            tvWelcomeSubtitle.setText(getResources().getString(R.string.klp_face_otp_demo));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.Companion.printLog("onResume");
        getPreferencesValuesAndApply(true);
    }

    private void refreshColor(String btnColor, String btnTxtColor) {
        ekycButton.setTextColor(Color.parseColor(btnTxtColor));
        ViewCompat.setBackgroundTintList(
                ekycButton,
                ColorStateList.valueOf(Color.parseColor(btnColor))
        );
    }

    @SuppressLint("ResourceType")
    public void setupBinding() {
        ekycButton = findViewById(R.id.btn_eykc);
        tvWelcome = findViewById(R.id.tv_welcome);
        tvWelcomeSubtitle = findViewById(R.id.tv_welcome_subtitle);
        tvVersion = findViewById(R.id.tv_version_code);

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            String version = pInfo.versionName;
            tvVersion.setText("version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        container = findViewById(R.id.container);
        findViewById(R.id.iv_setting).setOnClickListener(v -> openSettingUI());
        ekycButton.setText(getResources().getText(R.string.klp_start));
    }


}
