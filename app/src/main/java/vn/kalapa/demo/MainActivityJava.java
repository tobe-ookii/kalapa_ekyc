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
import vn.kalapa.ekyc.DialogListener;
import vn.kalapa.ekyc.KalapaFlowType;
import vn.kalapa.ekyc.KalapaHandler;
import vn.kalapa.ekyc.KalapaSDKResultCode;
import vn.kalapa.ekyc.KalapaSDK;
import vn.kalapa.ekyc.KalapaSDKConfig;
import vn.kalapa.ekyc.models.KalapaResult;
import vn.kalapa.ekyc.models.PreferencesConfig;
import vn.kalapa.ekyc.networks.KalapaAPI;
import vn.kalapa.ekyc.utils.Common;
import vn.kalapa.ekyc.utils.LocaleHelper;
import vn.kalapa.ekyc.views.ProgressView;

public class MainActivityJava extends BaseActivity {
    private static final String TAG = "MainActivity";
    public static String faceDataBase64 = "";
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
            }
        });
    }

    private boolean isUpgraded = false;

    private KalapaHandler klpHandler = new KalapaHandler() {
        @Override
        public void onExpired() {
            startEKYC();
        }

        @Override
        public void onError(@NonNull KalapaSDKResultCode resultCode) {
            Helpers.Companion.showDialog(MainActivityJava.this, getString(R.string.klp_demo_notice_title), getString(R.string.klp_demo_error_happended) + " " + (preferencesConfig.getLanguage().equals("vi") ? resultCode.getVi() : resultCode.getEn()), getResources().getString(R.string.klp_demo_confirm), R.drawable.frowning_face);
        }

        @Override
        public void onComplete(@NonNull KalapaResult kalapaResult) {
            Common.SCENARIO scenario = preferencesConfig.getScenario();
            LogUtils.Companion.printLog("startFullEKYC onComplete: \n " + scenario + preferencesConfig.getUseNFC() + kalapaResult.toMap() + " \n " + kalapaResult.getSession());
            ExampleGlobalClass.kalapaResult = kalapaResult;
            if (KalapaSDK.Companion.getFaceBitmap() != null)
                ExampleGlobalClass.faceImage = KalapaSDK.Companion.getFaceBitmap();
            if (KalapaSDK.Companion.getFrontBitmap() != null)
                ExampleGlobalClass.frontImage = KalapaSDK.Companion.getFrontBitmap();
            if (KalapaSDK.Companion.getBackBitmap() != null)
                ExampleGlobalClass.backImage = KalapaSDK.Companion.getBackBitmap();
            if (kalapaResult.getNfc_data() != null && kalapaResult.getNfc_data().getId_number() != null)
                ExampleGlobalClass.nfcData = new NFCVerificationData(new NFCCardData(kalapaResult.getNfc_data(), true), null, null);
            if (!isUpgraded && scenario == Common.SCENARIO.REGISTER && (!preferencesConfig.getUseNFC()) &&
                    (kalapaResult.getDecision() != null && (kalapaResult.getDecision().equals("APPROVE") || kalapaResult.getDecision().equals("MANUAL")))) {
                Helpers.Companion.showDialog(MainActivityJava.this,
                        getString(R.string.klp_demo_upgrade_title), getString(R.string.klp_demo_upgrade_body),
                        getString(R.string.klp_demo_btn_upgrade), getString(R.string.klp_demo_btn_later), R.drawable.klp_demo_nfc, new DialogListener() {
                            @Override
                            public void onYes() {
                                isUpgraded = true;
                                startUpgradeFlow(kalapaResult.getSession());
                            }

                            @Override
                            public void onNo() {
                                isUpgraded = false;
                                startActivity(new Intent(MainActivityJava.this, ResultActivity.class));
                            }
                        });
            } else {
                isUpgraded = false;
                startActivity(new Intent(MainActivityJava.this, ResultActivity.class));
            }
        }

    };

    private void startRegisterFlow(String session, String flow) {
        new KalapaSDK.KalapaSDKBuilder(MainActivityJava.this, sdkConfig).build().start(session, flow, klpHandler);
    }

    private void startUpgradeFlow(String leftoverSession) {
        new KalapaSDK.KalapaSDKBuilder(MainActivityJava.this, sdkConfig).withLeftoverSession(leftoverSession).build().start(leftoverSession, KalapaFlowType.NFC_ONLY.getFlow(), klpHandler);
    }

    private void startUpgradeFlow(String session, String mrz, String faceData) {
        new KalapaSDK.KalapaSDKBuilder(MainActivityJava.this, sdkConfig).withMrz(mrz).withFaceData(faceData).build().start(session, KalapaFlowType.NFC_ONLY.getFlow(), klpHandler);

    }

    private void startCustomFlow(boolean hasCaptureScreen, boolean hasLivenessScreen, boolean hasNFCScreen, String withMrzData, String faceDataBase64) {
        LogUtils.Companion.printLog("hasCaptureScreen", hasCaptureScreen, "hasLivenessScreen", hasLivenessScreen, "hasNFCScreen", hasNFCScreen, "withMrzData", withMrzData);
        new KalapaSDK.KalapaSDKBuilder(MainActivityJava.this, sdkConfig).withMrz(withMrzData).withFaceData(faceDataBase64).build().startCustomFlow(hasCaptureScreen, hasLivenessScreen, hasNFCScreen, klpHandler);
    }

    private void startEKYC() {
        if (Common.Companion.isOnline(MainActivityJava.this)) {
            Common.SCENARIO scenario = preferencesConfig.getScenario();
            ProgressView.Companion.showProgress(MainActivityJava.this, ProgressView.ProgressViewType.LOADING, preferencesConfig.getMainColor(), preferencesConfig.getMainTextColor(), getString(R.string.klp_demo_alert_title), getString(R.string.klp_demo_loading));
            if (scenario == Common.SCENARIO.CUSTOM) {
                startCustomFlow(preferencesConfig.getHasCustomCaptureScreen(), preferencesConfig.getHasCustomLivenessScreen(), preferencesConfig.getHasCustomNFCScreen(), preferencesConfig.getMrz(), faceDataBase64);
                ProgressView.Companion.hideProgress(false);
            } else if (scenario == Common.SCENARIO.UPGRADE && preferencesConfig.getScenarioPlan() == Common.SCENARIO_PLAN.FROM_SESSION_ID) {
                startUpgradeFlow(preferencesConfig.getLeftoverSession());
            } else {
                // UPGRADE from Data.
                KalapaFlowType flowType = scenario == Common.SCENARIO.UPGRADE ? KalapaFlowType.NFC_ONLY :
                        !preferencesConfig.getCaptureImage() && !preferencesConfig.getUseNFC() ? KalapaFlowType.NA : preferencesConfig.getCaptureImage() ? preferencesConfig.getUseNFC() ? KalapaFlowType.NFC_EKYC : KalapaFlowType.EKYC : KalapaFlowType.NFC_ONLY;
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
                            KalapaSDK.KalapaSDKBuilder builder = getKalapaSDKBuilder(flowType);
                            builder.build().start(createSessionResult.getToken(), createSessionResult.getFlow(), klpHandler);
                            return null;
                        }, kalapaError -> {
                            ProgressView.Companion.hideProgress(true);
                            Helpers.Companion.showDialog(MainActivityJava.this, getString(R.string.klp_demo_notice_title), kalapaError.getMessageError(), R.drawable.ic_failed_solid);
                            LogUtils.Companion.printLog("doRequestGetSession kalapaError: " + kalapaError);
                            return null;
                        });
            }


        } else
            Helpers.Companion.showDialog(MainActivityJava.this, getString(R.string.klp_demo_notice_title), getString(R.string.klp_demo_error_network), R.drawable.ic_failed_solid);
    }

    @NonNull
    private KalapaSDK.KalapaSDKBuilder getKalapaSDKBuilder(KalapaFlowType flowType) {
        KalapaSDK.KalapaSDKBuilder builder = new KalapaSDK.KalapaSDKBuilder(MainActivityJava.this, sdkConfig);
        LogUtils.Companion.printLog("Builder: ", flowType, preferencesConfig.getMrz(), preferencesConfig.getScenarioPlan());
        if (flowType == KalapaFlowType.NFC_ONLY) {
            if (preferencesConfig.getScenarioPlan() == Common.SCENARIO_PLAN.FROM_SESSION_ID) {
                builder.withLeftoverSession(preferencesConfig.getLeftoverSession());
            } else if (preferencesConfig.getScenarioPlan() == Common.SCENARIO_PLAN.FROM_PROVIDED_DATA) {
                builder.withFaceData(faceDataBase64);
                builder.withMrz(preferencesConfig.getMrz());
                if (faceDataBase64 != null) builder.withFaceData(faceDataBase64);
            }
        }
        return builder;
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
            refreshText(lang);
            refreshColor(btnColor, btnTxtColor);
            sdkConfig = new KalapaSDKConfig.KalapaSDKConfigBuilder(MainActivityJava.this)
                    .withBackgroundColor(backgroundColor)
                    .withMainColor(btnColor)
                    .withBtnTextColor(btnTxtColor)
                    .withMainTextColor(txtColor)
                    .withLivenessVersion(livenessVersion)
                    .withBaseURL(preferencesConfig.getEnv())
                    .withLanguage(preferencesConfig.getLanguage())
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
