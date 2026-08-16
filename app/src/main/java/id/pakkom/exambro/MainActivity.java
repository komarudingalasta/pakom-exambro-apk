package id.pakkom.exambro;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Ganti URL ini jika alamat web PakKom Exambro berubah.
    private static final String EXAM_URL = "https://komarudingalasta.github.io/pakkom-exambro/";

    // PIN darurat awal. WAJIB ganti sebelum dipakai massal di sekolah.
    private static final String TEACHER_PIN = "2468";

    private LinearLayout homePanel, examPanel, finishPanel;
    private TextView networkStatus, examStatus;
    private WebView webView;
    private boolean examActive = false;
    private int logoTapCount = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable resetLogoTap = () -> logoTapCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        homePanel = findViewById(R.id.homePanel);
        examPanel = findViewById(R.id.examPanel);
        finishPanel = findViewById(R.id.finishPanel);
        networkStatus = findViewById(R.id.networkStatus);
        examStatus = findViewById(R.id.examStatus);
        webView = findViewById(R.id.webView);
        Button startButton = findViewById(R.id.startButton);
        Button closeButton = findViewById(R.id.closeButton);

        setupWebView();
        refreshReadiness();

        startButton.setOnClickListener(v -> {
            refreshReadiness();
            if (!hasInternet()) {
                Toast.makeText(this, "Internet belum tersedia.", Toast.LENGTH_LONG).show();
                return;
            }
            showStartConfirmation();
        });

        closeButton.setOnClickListener(v -> finishAndRemoveTask());

        View.OnClickListener emergencyTapListener = v -> registerEmergencyTap();
        findViewById(R.id.examLogo).setOnClickListener(emergencyTapListener);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (examActive) {
                    Toast.makeText(MainActivity.this, "Mode ujian aktif. Gunakan Emergency Exit guru bila diperlukan.", Toast.LENGTH_SHORT).show();
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
        s.setUserAgentString(s.getUserAgentString() + " PakKomExambro/5.0");
        webView.setBackgroundColor(Color.WHITE);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new ExamBridge(), "PakKomExambro");
    }

    private void showStartConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Siap Memulai?")
                .setMessage("Setelah mode ujian dimulai, navigasi perangkat akan dibatasi sampai ujian selesai.\n\nInternet: Siap ✓\nMode aman: Siap ✓\nWeb ujian: Siap ✓")
                .setNegativeButton("Kembali", null)
                .setPositiveButton("Mulai Ujian", (d, w) -> startExam())
                .show();
    }

    private void startExam() {
        examActive = true;
        homePanel.setVisibility(View.GONE);
        finishPanel.setVisibility(View.GONE);
        examPanel.setVisibility(View.VISIBLE);
        enterImmersiveMode();
        tryStartLockTask();
        webView.loadUrl(EXAM_URL);
    }

    private void tryStartLockTask() {
        try {
            startLockTask();
        } catch (Exception ignored) {
            Toast.makeText(this, "Screen pinning tidak dapat diaktifkan otomatis pada perangkat ini.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopExamMode() {
        examActive = false;
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null && am.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE) {
                stopLockTask();
            }
        } catch (Exception ignored) { }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    private void showFinished() {
        stopExamMode();
        webView.stopLoading();
        examPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.GONE);
        finishPanel.setVisibility(View.VISIBLE);
    }

    private void registerEmergencyTap() {
        logoTapCount++;
        handler.removeCallbacks(resetLogoTap);
        handler.postDelayed(resetLogoTap, 2500);
        if (logoTapCount >= 5) {
            logoTapCount = 0;
            handler.removeCallbacks(resetLogoTap);
            showTeacherExitDialog();
        }
    }

    private void showTeacherExitDialog() {
        EditText pin = new EditText(this);
        pin.setHint("PIN guru");
        pin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        pin.setPadding(pad, pad, pad, pad);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Emergency Exit")
                .setMessage("Masukkan PIN guru untuk melepaskan mode ujian.")
                .setView(pin)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Keluar", null)
                .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (TEACHER_PIN.equals(pin.getText().toString())) {
                dialog.dismiss();
                showFinished();
            } else {
                pin.setError("PIN salah");
            }
        }));
        dialog.show();
    }

    private void refreshReadiness() {
        if (hasInternet()) {
            networkStatus.setText("✓ Koneksi internet tersedia");
            networkStatus.setTextColor(getColor(R.color.green));
        } else {
            networkStatus.setText("! Internet belum tersedia");
            networkStatus.setTextColor(getColor(R.color.amber));
        }
    }

    private boolean hasInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
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

    @Override
    protected void onResume() {
        super.onResume();
        refreshReadiness();
        if (examActive) enterImmersiveMode();
    }

    @Override
    protected void onDestroy() {
        if (examActive) stopExamMode();
        if (webView != null) {
            webView.removeJavascriptInterface("PakKomExambro");
            webView.destroy();
        }
        super.onDestroy();
    }

    public class ExamBridge {
        @JavascriptInterface
        public void finishExam() {
            runOnUiThread(() -> showFinished());
        }
    }
}
