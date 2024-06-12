//package vn.kalapa.demo;
//
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.content.res.ColorStateList;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.core.view.ViewCompat;
//
//import java.util.Locale;
//import vn.kalapa.demo.activities.BaseActivity;
//import vn.kalapa.demo.activities.ResultActivity;
//import vn.kalapa.demo.models.NFCCardData;
//import vn.kalapa.demo.models.NFCVerificationData;
//import vn.kalapa.demo.utils.Helpers;
//import vn.kalapa.demo.utils.LogUtils;
//import vn.kalapa.ekyc.KalapaHandler;
//import vn.kalapa.ekyc.KalapaSDK;
//import vn.kalapa.ekyc.KalapaSDKConfig;
//import vn.kalapa.ekyc.KalapaSDKResultCode;
//import vn.kalapa.ekyc.models.KalapaResult;
//import vn.kalapa.ekyc.models.PreferencesConfig;
//import vn.kalapa.ekyc.networks.KalapaAPI;
//import vn.kalapa.ekyc.utils.Common;
//import vn.kalapa.ekyc.utils.LocaleHelper;
//import vn.kalapa.ekyc.views.ProgressView;
//
//public class PaymeDemoJava extends BaseActivity {
//    public static String PACKAGE_NAME;
//    private Button ekycButton;
//    private TextView tvWelcome;
//    private TextView tvWelcomeSubtitle;
//
//    private PreferencesConfig preferencesConfig;
//
//    private int livenessVersion = Common.LIVENESS_VERSION.PASSIVE.getVersion();
//    private KalapaSDKConfig sdkConfig;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        PACKAGE_NAME = getApplicationContext().getPackageName();
//        Helpers.Companion.init(this);
//        setupBinding();
//        ekycButton.setOnClickListener(v -> {
//
//            if (isAppConfigSet()) {
//                runOnUiThread(() -> {
//                    LogUtils.Companion.printLog("Showing ProgressView");
//                    ProgressView.Companion.showProgress(PaymeDemoJava.this, ProgressView.ProgressViewType.LOADING,
//                            preferencesConfig.getMainColor(),
//                            preferencesConfig.getMainTextColor(),
//                            getString(R.string.klp_face_otp_alert_title),
//                            getString(R.string.klp_faceotp_loading));
//                });
//                if (!Common.Companion.isOnline(PaymeDemoJava.this)) {
//                    ProgressView.Companion.hideProgress(true);
//                    Helpers.Companion.showDialog(PaymeDemoJava.this,
//                            getResources().getString(R.string.klp_face_otp_alert_title),
//                            getResources().getString(R.string.klp_face_otp_error_network), R.drawable.frowning_face);
//                    return;
//                }
//                new Handler(Looper.getMainLooper()).post(() -> {
//                    setSdkConfig(preferencesConfig);
//                    startEKYC();
//                });
//            } else
//                ProgressView.Companion.hideProgress(true);
//        });
//    }
//
//    private void startEKYC() {
//        if (Common.Companion.isOnline(PaymeDemoJava.this)) {
//            KalapaAPI.Companion.doRequestGetSession(
//                    preferencesConfig.getEnv(),
//                    preferencesConfig.getToken(),
//                    createSessionResult -> {
//                        ProgressView.Companion.hideProgress(true);
//                        LogUtils.Companion.printLog("doRequestGetSession createSessionResult: " + createSessionResult.component1());
//                        KalapaSDK.Companion.startFullEKYC(PaymeDemoJava.this, createSessionResult.component1(), sdkConfig, new KalapaHandler() {
//                            @Override
//                            public void onError(@NonNull KalapaSDKResultCode resultCode) {
//                                Helpers.Companion.showDialog(PaymeDemoJava.this, getString(R.string.klp_face_otp_alert_title), getString(R.string.klp_face_otp_error_happended) + " " + (preferencesConfig.getLanguage().equals("vi") ? resultCode.getVi() : resultCode.getEn()), R.drawable.frowning_face);
//                            }
//
//                            @Override
//                            public void onComplete(@NonNull KalapaResult kalapaResult) {
//                                LogUtils.Companion.printLog("startFullEKYC onComplete: " + kalapaResult + " \n " + kalapaResult.component1());
//                                ExampleGlobalClass.kalapaResult = kalapaResult;
//                                if (KalapaSDK.Companion.isFaceBitmapInitialized())
//                                    ExampleGlobalClass.faceImage = KalapaSDK.faceBitmap;
//                                if (KalapaSDK.Companion.isFrontBitmapInitialized())
//                                    ExampleGlobalClass.frontImage = KalapaSDK.frontBitmap;
//                                if (KalapaSDK.Companion.isBackBitmapInitialized())
//                                    ExampleGlobalClass.backImage = KalapaSDK.backBitmap;
//                                ExampleGlobalClass.nfcData = new NFCVerificationData(new NFCCardData(kalapaResult.getNfc_data(), true), null, null);
//                                startActivity(new Intent(PaymeDemoJava.this, ResultActivity.class));
//                            }
//
//                        });
//                        return null;
//                    }, kalapaError -> {
//                        ProgressView.Companion.hideProgress(true);
//                        Helpers.Companion.showDialog(PaymeDemoJava.this,
//                                getString(R.string.klp_face_otp_alert_title),
//                                kalapaError.getMessageError(),
//                                R.drawable.ic_failed_solid
//                        );
//                        LogUtils.Companion.printLog("doRequestGetSession kalapaError: " + kalapaError);
//                        return null;
//                    });
//        } else {
//            Helpers.Companion.showDialog(PaymeDemoJava.this,
//                    getString(R.string.klp_face_otp_alert_title),
//                    getString(R.string.klp_face_otp_error_network),
//                    R.drawable.ic_failed_solid
//            );
//        }
//    }
//
//    private boolean isAppConfigSet() {
//        preferencesConfig = Helpers.Companion.getSharedPreferencesConfig(this);
//        if (preferencesConfig == null) {
//            openSettingUI();
//            return false;
//        }
//        return true;
//    }
//
//    private void getPreferencesValuesAndApply() {
//
//        preferencesConfig = Helpers.Companion.getSharedPreferencesConfig(this);
//        if (preferencesConfig != null) {
//            ExampleGlobalClass.preferencesConfig = preferencesConfig;
//            String btnColor = preferencesConfig.getMainColor();
//            String btnTxtColor = preferencesConfig.getBtnTextColor();
//            String lang = preferencesConfig.getLanguage();
//            refreshText(lang);
//            refreshColor(btnColor, btnTxtColor);
//        } else
//            LogUtils.Companion.printLog("Preferences config is null...");
//    }
//
//    private void setSdkConfig(PreferencesConfig preferencesConfig) {
//        sdkConfig = new KalapaSDKConfig.KalapaSDKConfigBuilder(PaymeDemoJava.this)
//                .withBackgroundColor(preferencesConfig.getBackgroundColor())
//                .withMainColor(preferencesConfig.getMainColor())
//                .withBtnTextColor(preferencesConfig.getBtnTextColor())
//                .withMainTextColor(preferencesConfig.getMainTextColor())
//                .withLivenessVersion(preferencesConfig.getLivenessVersion())
//                .withBaseURL(preferencesConfig.getEnv())
//                .withLanguage(preferencesConfig.getLanguage())
//                .build();
//        KalapaSDK.Companion.configure(sdkConfig);
//    }
//
//    private void refreshText(String lang) {
//        Locale locale = new Locale(lang.equals("vi") ? LocaleHelper.VIETNAMESE : lang.equals("ko") ? LocaleHelper.KOREAN : LocaleHelper.ENGLISH);
//        Locale.setDefault(locale);
//        android.content.res.Configuration configuration = this.getResources().getConfiguration();
//        configuration.setLocale(locale);
//        configuration.setLayoutDirection(locale);
//        this.getResources().updateConfiguration(configuration, this.getResources().getDisplayMetrics());
//        LogUtils.Companion.printLog("refreshText ", lang);
//        if (preferencesConfig != null) {
//            ekycButton.setText(getResources().getString(R.string.klp_start));
//            ekycButton.invalidate();
//            tvWelcome.setText(getResources().getString(R.string.klp_name));
//            tvWelcome.invalidate();
//            tvWelcomeSubtitle.setText(getResources().getString(R.string.klp_face_otp_demo));
//            tvWelcomeSubtitle.invalidate();
//        } else {
//            ekycButton.setText(getResources().getString(R.string.klp_start));
//            tvWelcome.setText(getResources().getString(R.string.klp_name));
//            tvWelcomeSubtitle.setText(getResources().getString(R.string.klp_face_otp_demo));
//        }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        long start = System.currentTimeMillis();
//        getPreferencesValuesAndApply();
//    }
//
//    private void refreshColor(String btnColor, String btnTxtColor) {
//        ekycButton.setTextColor(Color.parseColor(btnTxtColor));
//        ViewCompat.setBackgroundTintList(
//                ekycButton,
//                ColorStateList.valueOf(Color.parseColor(btnColor))
//        );
//    }
//
//    @SuppressLint("ResourceType")
//    public void setupBinding() {
//        ekycButton = findViewById(R.id.btn_eykc);
//        tvWelcome = findViewById(R.id.tv_welcome);
//        tvWelcomeSubtitle = findViewById(R.id.tv_welcome_subtitle);
//        TextView tvVersion = findViewById(R.id.tv_version_code);
//
//        try {
//            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
//            String version = pInfo.versionName;
//            tvVersion.setText("version " + version);
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//        View container = findViewById(R.id.container);
//        findViewById(R.id.iv_setting).setOnClickListener(v -> openSettingUI());
//        ekycButton.setText(getResources().getText(R.string.klp_start));
//    }
//
//
//}
