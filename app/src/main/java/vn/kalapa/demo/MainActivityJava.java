package vn.kalapa.demo;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import java.util.Locale;

import vn.kalapa.demo.activities.BaseActivity;
import vn.kalapa.demo.activities.ResultActivity;
import vn.kalapa.demo.models.NFCCardData;
import vn.kalapa.demo.models.NFCVerificationData;
import vn.kalapa.demo.utils.Helpers;
import vn.kalapa.demo.utils.LogUtils;
import vn.kalapa.ekyc.KalapaFlowType;
import vn.kalapa.ekyc.KalapaCaptureHandler;
import vn.kalapa.ekyc.KalapaHandler;
import vn.kalapa.ekyc.KalapaNFCHandler;
import vn.kalapa.ekyc.KalapaSDKCallback;
import vn.kalapa.ekyc.KalapaSDKResultCode;
import vn.kalapa.ekyc.KalapaSDK;
import vn.kalapa.ekyc.KalapaSDKConfig;
import vn.kalapa.ekyc.KalapaSDKMediaType;
import vn.kalapa.ekyc.models.KalapaResult;
import vn.kalapa.ekyc.models.PreferencesConfig;
import vn.kalapa.ekyc.networks.KalapaAPI;
import vn.kalapa.ekyc.utils.Common;
import vn.kalapa.ekyc.utils.LocaleHelper;
import vn.kalapa.ekyc.views.ProgressView;

public class MainActivityJava extends BaseActivity {
    private static final String TAG = "MainActivity";
    private Button ekycButton;
    private TextView tvWelcome;
    private TextView tvWelcomeSubtitle;
    private TextView tvVersion;

    private PreferencesConfig preferencesConfig;

    private int livenessVersion = Common.LIVENESS_VERSION.PASSIVE.getVersion();
    private KalapaSDKConfig sdkConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Helpers.Companion.init(this);
        setupBinding();
        getPreferencesValuesAndApply();
        ekycButton.setOnClickListener(v -> {
            if (isAppConfigSet()) {
                if (!Common.Companion.isOnline(MainActivityJava.this)) {
                    Helpers.Companion.showDialog(MainActivityJava.this,
                            getResources().getString(R.string.klp_demo_notice_title),
                            getResources().getString(R.string.klp_demo_error_network), R.drawable.frowning_face);
                    return;
                }
                startEKYC();
//                startCustomEKYC();
            }
        });
    }

    KalapaCaptureHandler ocrHandler = new KalapaCaptureHandler() {

        @Override
        public void process(@NonNull String base64, @NonNull KalapaSDKMediaType kalapaSDKMediaType, @NonNull KalapaSDKCallback kalapaSDKCallback) {
            // If your process is finish as success, you have to use callback.sendDone, otherwise, use callback.sendError to show the error
            boolean assumeSuccessProcess = true;
            if (kalapaSDKMediaType == KalapaSDKMediaType.FRONT) {
                // Process your FRONT side Document image
                if (assumeSuccessProcess)
                    kalapaSDKCallback.sendDone(() -> {
                        return null;
                    });
                else
                    kalapaSDKCallback.sendError("Tell SDK what goes wrong");
            } else if (kalapaSDKMediaType == KalapaSDKMediaType.BACK) { // BACK
                // Process your BACK side Document image
                if (assumeSuccessProcess)
                    kalapaSDKCallback.sendDone(() -> {
                        return null;
                    });
                else
                    kalapaSDKCallback.sendError("Tell SDK what goes wrong");
            } else if (kalapaSDKMediaType == KalapaSDKMediaType.PORTRAIT) { // PORTRAIT
                // Process your PORTRAIT image
                if (assumeSuccessProcess)
                    kalapaSDKCallback.sendDone(() -> {
                        return null;
                    });
                else
                    kalapaSDKCallback.sendError("Tell SDK what goes wrong");
            } else {
                LogUtils.Companion.printLog("Should not go here");
            }
        }


        @Override
        public void onError(@NonNull KalapaSDKResultCode kalapaSDKResultCode) {

        }
    };
    private KalapaNFCHandler nfcHandler = new KalapaNFCHandler("<OPTIONAL_YOUR_MRZ_IF_YOU_HAVE_FROM_PREVIOUS_STEPS>") {

        @Override
        public void process(@NonNull String idCardNumber, @NonNull String nfcData, @NonNull KalapaSDKCallback callback) {
            // SDK will return valid id card number that read from back-side card or your input mrz if it valid and raw nfc data.
            // If your process is finish as success, you have to use callback.sendDone, otherwise, use callback.sendError to show the error
            boolean assumeSuccessProcess = true;
            if (assumeSuccessProcess)
                callback.sendDone(() -> {
                    return null;
                });
            else
                callback.sendError("Tell SDK what goes wrong");
        }

        @Override
        public void onError(@NonNull KalapaSDKResultCode resultCode) {
            // SDK throw error if user can not finish the nfc step.
            // Usual error code is: USER_LEAVE and DEVICE_NOT_SUPPORTED
        }
    };

    private void startCustomEKYC() {
        startFrontCaptureStep();
    }

    private void startFrontCaptureStep() {
        KalapaSDK.Companion.startCaptureForResult(MainActivityJava.this, sdkConfig, ocrHandler);
    }

    private void startBackCaptureStep() {
        KalapaSDK.Companion.startCaptureBackForResult(MainActivityJava.this, sdkConfig, ocrHandler);
    }

    private void startNFCStep() {
        KalapaSDK.Companion.startNFCForResult(MainActivityJava.this, sdkConfig, nfcHandler);
    }

    private void startLivenessStep() {
        KalapaSDK.Companion.startLivenessForResult(MainActivityJava.this, sdkConfig, ocrHandler);
    }

    private void startEKYC() {
        if (Common.Companion.isOnline(MainActivityJava.this)) {
            ProgressView.Companion.showProgress(MainActivityJava.this, ProgressView.ProgressViewType.LOADING, preferencesConfig.getMainColor(), preferencesConfig.getMainTextColor(), getString(R.string.klp_demo_alert_title), getString(R.string.klp_demo_loading));

            KalapaFlowType flowType = !preferencesConfig.getCaptureImage() && !preferencesConfig.getUseNFC() ? KalapaFlowType.NA :
                    preferencesConfig.getCaptureImage() ? preferencesConfig.getUseNFC() ? KalapaFlowType.NFC_EKYC : KalapaFlowType.EKYC : KalapaFlowType.NFC_ONLY;
            KalapaAPI.Companion.doRequestGetSession(
                    preferencesConfig.getEnv(),
                    preferencesConfig.getToken(),
                    flowType,
                    preferencesConfig.getVerifyCheck() + "",
                    preferencesConfig.getFraudCheck() + "",
                    preferencesConfig.getNormalCheckOnly() + "",
                    preferencesConfig.getCardSidesCheck(),
                    preferencesConfig.getAcceptedDocument(),
                    preferencesConfig.getFaceMatchThreshold(),
                    createSessionResult -> {
                        ProgressView.Companion.hideProgress(true);
                        LogUtils.Companion.printLog("doRequestGetSession createSessionResult: ", createSessionResult.getFlow(), createSessionResult.getToken());
                        KalapaSDK.Companion.startFullEKYC(MainActivityJava.this,
                                createSessionResult.getToken(),
                                createSessionResult.component3(), sdkConfig, new KalapaHandler() {
                                    @Override
                                    public void onError(@NonNull KalapaSDKResultCode resultCode) {
                                        Helpers.Companion.showDialog(MainActivityJava.this, getString(R.string.klp_demo_notice_title), getString(R.string.klp_demo_error_happended) + " " + (preferencesConfig.getLanguage().equals("vi") ? resultCode.getVi() : resultCode.getEn()), R.drawable.frowning_face);
                                    }

                                    @Override
                                    public void onComplete(@NonNull KalapaResult kalapaResult) {
                                        LogUtils.Companion.printLog("startFullEKYC onComplete: " + kalapaResult + " \n " + kalapaResult.component1());
                                        ExampleGlobalClass.kalapaResult = kalapaResult;
                                        if (KalapaSDK.Companion.isFaceBitmapInitialized())
                                            ExampleGlobalClass.faceImage = KalapaSDK.faceBitmap;
                                        if (KalapaSDK.Companion.isFrontBitmapInitialized())
                                            ExampleGlobalClass.frontImage = KalapaSDK.frontBitmap;
                                        if (KalapaSDK.Companion.isBackBitmapInitialized())
                                            ExampleGlobalClass.backImage = KalapaSDK.backBitmap;
                                        ExampleGlobalClass.nfcData = new NFCVerificationData(new NFCCardData(kalapaResult.getNfc_data(), true), null, null);
                                        startActivity(new Intent(MainActivityJava.this, ResultActivity.class));
                                    }

                                });
                        return null;
                    }, kalapaError -> {
                        ProgressView.Companion.hideProgress(true);
                        Helpers.Companion.showDialog(MainActivityJava.this,
                                getString(R.string.klp_demo_notice_title),
                                kalapaError.getMessageError(),
                                R.drawable.ic_failed_solid
                        );
                        LogUtils.Companion.printLog("doRequestGetSession kalapaError: " + kalapaError);
                        return null;
                    });
        } else {
            Helpers.Companion.showDialog(MainActivityJava.this,
                    getString(R.string.klp_demo_notice_title),
                    getString(R.string.klp_demo_error_network),
                    R.drawable.ic_failed_solid
            );
        }
    }

    private boolean isAppConfigSet() {
        preferencesConfig = Helpers.Companion.getSharedPreferencesConfig(MainActivityJava.this);
        if (preferencesConfig == null) {
            openSettingUI(() -> {
                getPreferencesValuesAndApply();
                return null;
            });
            return false;
        } else {
            ExampleGlobalClass.preferencesConfig = preferencesConfig;
            Log.d(TAG, "isAppConfigSet Preferences " + preferencesConfig);
            getPreferencesValuesAndApply();
            return true;
        }
    }

    private void getPreferencesValuesAndApply() {
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
            boolean shouldUpdateLanguage = sdkConfig == null || !sdkConfig.getLanguage().equals(lang);
            sdkConfig = new KalapaSDKConfig.KalapaSDKConfigBuilder(MainActivityJava.this)
                    .withBackgroundColor(backgroundColor)
                    .withMainColor(btnColor)
                    .withBtnTextColor(btnTxtColor)
                    .withMainTextColor(txtColor)
                    .withLivenessVersion(livenessVersion)
                    .withBaseURL(preferencesConfig.getEnv())
                    .withLanguage(preferencesConfig.getLanguage())
//                    .withSessionID("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjcwNjNmZTMxOTQwMDRiYzY4YWFkMDgxY2QwZGRmN2ZlIiwidWlkIjoiM2FkODRkMGUxYTIwNGZkYWEyZGUwYWM5NTNmNzA2YTUiLCJjaWQiOiJpbnRlcm5hbF9la3ljIiwiaWF0IjoxNzE5MjIzODE2fQ.mj4vB1V3wv5Bf2d-1zgAlZ1VcfgH17mRoi_VP9FneCQ")
//                    .withFaceData(BitmapUtil.Companion.getTU_BASE64())
//                    .withMRZ("IDVNM0940186406001094018640<<7\\n9408182M3408180VNM<<<<<<<<<<<8\\nNGUYEN<<GIA<TU<<<<<<<<<<<<<<<<")
                    .build();
            LogUtils.Companion.printLog("Pulling language: " + sdkConfig.getLanguage() + " - " + lang);
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
            ekycButton.setText(getResources().getString(R.string.klp_start));
            ekycButton.invalidate();
            tvWelcome.setText(getResources().getString(R.string.klp_name));
            tvWelcome.invalidate();
            tvWelcomeSubtitle.setText(getResources().getString(R.string.klp_demo_name));
            tvWelcomeSubtitle.invalidate();
        } else {
            ekycButton.setText(getResources().getString(R.string.klp_start));
            tvWelcome.setText(getResources().getString(R.string.klp_name));
            tvWelcomeSubtitle.setText(getResources().getString(R.string.klp_demo_name));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.Companion.printLog("onResume");
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
        findViewById(R.id.iv_setting).setOnClickListener(v -> openSettingUI(() -> {
            getPreferencesValuesAndApply();
            return null;
        }));
        ekycButton.setText(getResources().getText(R.string.klp_start));
    }


}
