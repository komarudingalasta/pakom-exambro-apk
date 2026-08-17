package id.pakkom.exambro;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String EXAM_URL = "https://komarudingalasta.github.io/pakkom-exambro/";
    private static final String TEACHER_PIN = "2468"; // Ganti sebelum penggunaan massal.
    private static final String PREFS = "pakkom_exambro_state";
    private static final String KEY_EXAM_ACTIVE = "exam_active";
    private static final String KEY_LAST_URL = "last_url";

    private ScrollView homeScroll;
    private LinearLayout examPanel, finishPanel, connectionPanel;
    private TextView networkStatus, batteryStatus, readinessSummary, examStatus;
    private WebView webView;
    private Button startButton;

    private SharedPreferences prefs;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private boolean examActive = false;
    private boolean pageLoadedOnce = false;
    private boolean mainFrameError = false;
    private int logoTapCount = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable resetLogoTap = () -> logoTapCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cegah screenshot/rekam layar aplikasi melalui mekanisme standar Android.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        bindViews();
        setupWebView();
        setupNetworkMonitoring();
        setupActions();
        refreshReadiness();

        examActive = prefs.getBoolean(KEY_EXAM_ACTIVE, false);
        if (examActive) {
            restoreExamSession();
        } else {
            showHome();
        }
    }

    private void bindViews() {
        homeScroll = findViewById(R.id.homeScroll);
        examPanel = findViewById(R.id.examPanel);
        finishPanel = findViewById(R.id.finishPanel);
        connectionPanel = findViewById(R.id.connectionPanel);
        networkStatus = findViewById(R.id.networkStatus);
        batteryStatus = findViewById(R.id.batteryStatus);
        readinessSummary = findViewById(R.id.readinessSummary);
        examStatus = findViewById(R.id.examStatus);
        webView = findViewById(R.id.webView);
        startButton = findViewById(R.id.startButton);
    }

    private void setupActions() {
        startButton.setOnClickListener(v -> {
            refreshReadiness();
            if (!hasValidatedInternet()) {
                Toast.makeText(this, "Internet belum siap.", Toast.LENGTH_LONG).show();
                return;
            }
            showStartConfirmation();
        });

        findViewById(R.id.closeButton).setOnClickListener(v -> finishAndRemoveTask());
        findViewById(R.id.retryButton).setOnClickListener(v -> retryWeb());
        findViewById(R.id.studentHelp).setOnClickListener(v -> showStudentHelp());
        findViewById(R.id.teacherAccess).setOnClickListener(v -> showTeacherMenu());
        findViewById(R.id.homeTeacherButton).setOnClickListener(v -> showTeacherMenu());

        findViewById(R.id.examLogo).setOnClickListener(v -> registerEmergencyTap());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (examActive) {
                    Toast.makeText(
                            MainActivity.this,
                            "Mode ujian aktif. Gunakan BANTUAN jika halaman bermasalah.",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    finish();
                }
            }
        });
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUserAgentString(s.getUserAgentString() + " PakKomExambro/5.2");
        webView.setBackgroundColor(Color.WHITE);
        webView.addJavascriptInterface(new ExamBridge(), "PakKomExambro");
        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                mainFrameError = false;
                updateExamStatus();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!mainFrameError) {
                    pageLoadedOnce = true;
                    prefs.edit().putString(KEY_LAST_URL, url).apply();
                    hideConnectionPanel();
                }
                updateExamStatus();
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error
            ) {
                if (request.isForMainFrame()) {
                    mainFrameError = true;
                    showConnectionPanel(
                            hasValidatedInternet()
                                    ? "Halaman Belum Dapat Dimuat"
                                    : "Koneksi Terputus",
                            "Sesi ujian tetap disimpan. Periksa koneksi lalu tekan Coba Lagi."
                    );
                }
            }
        });
    }

    private void setupNetworkMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    refreshReadiness();
                    updateExamStatus();
                    if (examActive && connectionPanel.getVisibility() == View.VISIBLE) {
                        handler.postDelayed(() -> {
                            if (hasValidatedInternet()) retryWeb();
                        }, 900);
                    }
                });
            }

            @Override public void onLost(Network network) {
                runOnUiThread(() -> {
                    refreshReadiness();
                    updateExamStatus();
                    if (examActive) {
                        showConnectionPanel(
                                "Koneksi Terputus",
                                "Sesi ujian tetap dipertahankan. Exambro akan mencoba menyambung kembali."
                        );
                    }
                });
            }

            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                runOnUiThread(() -> {
                    refreshReadiness();
                    updateExamStatus();
                });
            }
        };

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } catch (Exception ignored) { }
    }

    private void showStartConfirmation() {
        String battery = getBatteryPercent() + "%";
        new AlertDialog.Builder(this)
                .setTitle("Siap Memulai Ujian?")
                .setMessage(
                        "Setelah dimulai, navigasi perangkat akan dibatasi sampai ujian selesai.\n\n" +
                        "✓ Internet siap\n" +
                        "✓ Anti-screenshot aktif\n" +
                        "✓ Baterai " + battery + "\n\n" +
                        "Jika terjadi gangguan, guru dapat menggunakan AKSES GURU."
                )
                .setNegativeButton("Kembali", null)
                .setPositiveButton("Mulai Ujian", (d, w) -> startExam())
                .show();
    }

    private void startExam() {
        examActive = true;
        pageLoadedOnce = false;
        prefs.edit()
                .putBoolean(KEY_EXAM_ACTIVE, true)
                .putString(KEY_LAST_URL, EXAM_URL)
                .apply();

        showExam();
        enterImmersiveMode();
        tryStartLockTask();
        webView.loadUrl(EXAM_URL);
    }

    private void restoreExamSession() {
        showExam();
        enterImmersiveMode();
        tryStartLockTask();

        String lastUrl = prefs.getString(KEY_LAST_URL, EXAM_URL);
        if (lastUrl == null || !lastUrl.startsWith("https://")) {
            lastUrl = EXAM_URL;
        }

        if (hasValidatedInternet()) {
            webView.loadUrl(lastUrl);
        } else {
            showConnectionPanel(
                    "Menunggu Koneksi",
                    "Sesi ujian sebelumnya ditemukan. Sambungkan internet untuk melanjutkan."
            );
        }
    }

    private void showHome() {
        homeScroll.setVisibility(View.VISIBLE);
        examPanel.setVisibility(View.GONE);
        finishPanel.setVisibility(View.GONE);
        connectionPanel.setVisibility(View.GONE);
    }

    private void showExam() {
        homeScroll.setVisibility(View.GONE);
        finishPanel.setVisibility(View.GONE);
        examPanel.setVisibility(View.VISIBLE);
        updateExamStatus();
    }

    /**
     * Safe exit:
     * 1) tandai sesi tidak aktif terlebih dahulu,
     * 2) hentikan loading,
     * 3) lepaskan Lock Task,
     * 4) pulihkan system UI,
     * 5) baru tampilkan halaman selesai.
     */
    private void completeExamAndUnlock() {
        prefs.edit()
                .putBoolean(KEY_EXAM_ACTIVE, false)
                .remove(KEY_LAST_URL)
                .apply();
        examActive = false;

        webView.stopLoading();
        stopLockTaskSafely();
        exitImmersiveMode();

        examPanel.setVisibility(View.GONE);
        homeScroll.setVisibility(View.GONE);
        finishPanel.setVisibility(View.VISIBLE);
    }

    private void teacherExitToDevice() {
        prefs.edit()
                .putBoolean(KEY_EXAM_ACTIVE, false)
                .remove(KEY_LAST_URL)
                .apply();
        examActive = false;

        webView.stopLoading();
        stopLockTaskSafely();
        exitImmersiveMode();
        finishAndRemoveTask();
    }

    private void teacherReturnHome() {
        prefs.edit()
                .putBoolean(KEY_EXAM_ACTIVE, false)
                .remove(KEY_LAST_URL)
                .apply();
        examActive = false;

        webView.stopLoading();
        stopLockTaskSafely();
        exitImmersiveMode();
        webView.loadUrl("about:blank");
        showHome();
        refreshReadiness();
    }


    private void showStudentHelp() {
        String internet = hasValidatedInternet() ? "Terhubung" : "Tidak terhubung";
        String[] items = {"Refresh halaman", "Cek koneksi", "Info aplikasi", "Keluar darurat"};
        new AlertDialog.Builder(this)
                .setTitle("Bantuan Ujian")
                .setMessage("Internet: " + internet + "\nMode ujian: " +
                        (examActive ? "Aktif" : "Tidak aktif") + "\nPakKom Exambro V5.2")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) retryWeb();
                    else if (which == 1) Toast.makeText(this,
                            hasValidatedInternet() ? "Internet terhubung dan tervalidasi."
                                    : "Internet belum terhubung/tervalidasi.", Toast.LENGTH_LONG).show();
                    else if (which == 2) new AlertDialog.Builder(this)
                            .setTitle("Info PakKom Exambro")
                            .setMessage("Versi 5.2.0\nAnti-screenshot: Aktif\nMode ujian: " +
                                    (examActive ? "Aktif" : "Tidak aktif"))
                            .setPositiveButton("OK", null).show();
                    else showStudentEmergencyExit();
                })
                .setNegativeButton("Kembali ke Ujian", null).show();
    }

    private void showStudentEmergencyExit() {
        if (!examActive) return;
        final String[] reasons = {
                "Aplikasi/halaman bermasalah", "Koneksi bermasalah",
                "Perangkat bermasalah", "Atas arahan guru", "Lainnya"
        };
        final int[] selected = {-1};
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Keluar Darurat")
                .setMessage("Gunakan hanya jika ujian tidak dapat dilanjutkan pada perangkat ini.")
                .setSingleChoiceItems(reasons, -1, (d, which) -> selected[0] = which)
                .setNegativeButton("Batal", null).setPositiveButton("Lanjutkan", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            if (selected[0] < 0) {
                Toast.makeText(this, "Pilih alasan keluar terlebih dahulu.", Toast.LENGTH_SHORT).show();
                return;
            }
            String reason = reasons[selected[0]];
            dialog.dismiss();
            confirmStudentEmergencyExit(reason);
        }));
        dialog.show();
    }

    private void confirmStudentEmergencyExit(String reason) {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Keluar Darurat")
                .setMessage("Ujian belum dinyatakan selesai.\n\nAlasan: " + reason +
                        "\n\nKeluar hanya jika Anda benar-benar tidak dapat melanjutkan ujian.")
                .setNegativeButton("Kembali", null)
                .setPositiveButton("Keluar Darurat", (d, w) -> studentEmergencyExit(reason)).show();
    }

    private void studentEmergencyExit(String reason) {
        prefs.edit().putString("last_emergency_exit_reason", reason)
                .putLong("last_emergency_exit_time", System.currentTimeMillis())
                .putBoolean(KEY_EXAM_ACTIVE, false).remove(KEY_LAST_URL).apply();
        examActive = false;
        webView.stopLoading();
        stopLockTaskSafely();
        exitImmersiveMode();
        new AlertDialog.Builder(this).setCancelable(false)
                .setTitle("Mode Ujian Telah Dilepas")
                .setMessage("Keluar darurat berhasil. Segera laporkan kepada guru/pengawas.")
                .setPositiveButton("Tutup Exambro", (d, w) -> finishAndRemoveTask()).show();
    }

    private void showTeacherMenu() {
        requestTeacherPin(() -> {
            final String[] items = examActive
                    ? new String[] {
                        "Muat ulang halaman ujian",
                        "Kembali ke beranda Exambro",
                        "Keluar dari Exambro"
                    }
                    : new String[] {
                        "Tutup aplikasi"
                    };

            new AlertDialog.Builder(this)
                    .setTitle("Menu Guru")
                    .setItems(items, (dialog, which) -> {
                        if (!examActive) {
                            finishAndRemoveTask();
                            return;
                        }
                        if (which == 0) {
                            retryWeb();
                        } else if (which == 1) {
                            confirmTeacherAction(
                                    "Kembali ke Beranda?",
                                    "Mode ujian akan dilepas dan sesi Exambro di perangkat ini diakhiri.",
                                    this::teacherReturnHome
                            );
                        } else {
                            confirmTeacherAction(
                                    "Keluar dari Exambro?",
                                    "Mode ujian akan dilepas sebelum aplikasi ditutup.",
                                    this::teacherExitToDevice
                            );
                        }
                    })
                    .show();
        });
    }

    private void requestTeacherPin(Runnable onSuccess) {
        final EditText pin = new EditText(this);
        pin.setHint("PIN guru");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        pin.setPadding(pad, pad, pad, pad);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Akses Guru")
                .setMessage("Masukkan PIN guru.")
                .setView(pin)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Lanjut", null)
                .create();

        dialog.setOnShowListener(x ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (TEACHER_PIN.equals(pin.getText().toString())) {
                        dialog.dismiss();
                        onSuccess.run();
                    } else {
                        pin.setError("PIN salah");
                    }
                })
        );
        dialog.show();
    }

    private void confirmTeacherAction(String title, String message, Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Ya", (d, w) -> action.run())
                .show();
    }

    private void registerEmergencyTap() {
        logoTapCount++;
        handler.removeCallbacks(resetLogoTap);
        handler.postDelayed(resetLogoTap, 2500);

        if (logoTapCount >= 5) {
            logoTapCount = 0;
            handler.removeCallbacks(resetLogoTap);
            showTeacherMenu();
        }
    }

    private void tryStartLockTask() {
        try {
            startLockTask();
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Screen pinning tidak dapat diaktifkan otomatis pada perangkat ini.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void stopLockTaskSafely() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null && am.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE) {
                stopLockTask();
            }
        } catch (Exception ignored) { }
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void exitImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    private void refreshReadiness() {
        boolean internet = hasValidatedInternet();
        int battery = getBatteryPercent();
        boolean batteryOk = battery >= 15 || battery < 0;

        if (internet) {
            networkStatus.setText("✓ Internet terhubung dan tervalidasi");
            networkStatus.setTextColor(getColor(R.color.green));
        } else {
            networkStatus.setText("! Internet belum siap");
            networkStatus.setTextColor(getColor(R.color.amber));
        }

        if (battery < 0) {
            batteryStatus.setText("• Status baterai tidak tersedia");
            batteryStatus.setTextColor(getColor(R.color.muted));
        } else if (batteryOk) {
            batteryStatus.setText("✓ Baterai " + battery + "%");
            batteryStatus.setTextColor(getColor(R.color.green));
        } else {
            batteryStatus.setText("! Baterai rendah: " + battery + "%");
            batteryStatus.setTextColor(getColor(R.color.amber));
        }

        if (internet && batteryOk) {
            readinessSummary.setText("Perangkat siap digunakan.");
            readinessSummary.setTextColor(getColor(R.color.green));
            startButton.setEnabled(true);
            startButton.setAlpha(1f);
        } else {
            readinessSummary.setText(
                    !internet
                            ? "Hubungkan internet sebelum memulai."
                            : "Sebaiknya isi daya perangkat sebelum ujian."
            );
            readinessSummary.setTextColor(getColor(R.color.amber));
            startButton.setEnabled(internet); // baterai rendah memberi peringatan, tidak memblokir darurat.
            startButton.setAlpha(internet ? 1f : 0.55f);
        }
    }

    private boolean hasValidatedInternet() {
        if (connectivityManager == null) {
            connectivityManager =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private int getBatteryPercent() {
        try {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            if (bm == null) return -1;
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception e) {
            return -1;
        }
    }

    private void updateExamStatus() {
        if (examStatus == null) return;
        if (hasValidatedInternet()) {
            examStatus.setText("● MODE UJIAN • ONLINE");
            examStatus.setTextColor(Color.parseColor("#78E09B"));
        } else {
            examStatus.setText("● MODE UJIAN • OFFLINE");
            examStatus.setTextColor(Color.parseColor("#FFC15A"));
        }
    }

    private void showConnectionPanel(String title, String message) {
        findViewById(R.id.connectionTitle);
        ((TextView) findViewById(R.id.connectionTitle)).setText(title);
        ((TextView) findViewById(R.id.connectionMessage)).setText(message);
        connectionPanel.setVisibility(View.VISIBLE);
    }

    private void hideConnectionPanel() {
        connectionPanel.setVisibility(View.GONE);
    }

    private void retryWeb() {
        if (!hasValidatedInternet()) {
            showConnectionPanel(
                    "Belum Ada Internet",
                    "Sambungkan perangkat ke internet. Sesi ujian tidak dihapus."
            );
            return;
        }

        connectionPanel.setVisibility(View.GONE);
        if (pageLoadedOnce && webView.getUrl() != null && !webView.getUrl().equals("about:blank")) {
            webView.reload();
        } else {
            String lastUrl = prefs.getString(KEY_LAST_URL, EXAM_URL);
            webView.loadUrl(lastUrl == null ? EXAM_URL : lastUrl);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshReadiness();

        if (examActive) {
            enterImmersiveMode();
            updateExamStatus();
        }
    }

    @Override
    protected void onDestroy() {
        // Jangan otomatis melepas mode ujian di onDestroy.
        // Jika proses Android dihentikan, SharedPreferences mempertahankan sesi untuk recovery.
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) { }
        }

        handler.removeCallbacksAndMessages(null);

        if (webView != null) {
            webView.removeJavascriptInterface("PakKomExambro");
            webView.destroy();
        }

        super.onDestroy();
    }

    public class ExamBridge {
        /**
         * Web utama dapat memanggil:
         * PakKomExambro.finishExam()
         * HANYA setelah jawaban/status selesai berhasil disimpan.
         */
        @JavascriptInterface
        public void finishExam() {
            runOnUiThread(() -> completeExamAndUnlock());
        }

        @JavascriptInterface
        public String getAppVersion() {
            return "5.2.0";
        }
    }
}
