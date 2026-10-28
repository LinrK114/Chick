package com.example.customclickapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;

public class AboutActivity extends AppCompatActivity {

    private static final String VERSION_URL = "https://ota.novaray.ccwu.cc/version.txt";
    private static final String NOTE_URL = "https://ota.novaray.ccwu.cc/note.md";
    private static final String SHA_URL = "https://ota.novaray.ccwu.cc/sha.txt";
    private static final String APK_URL = "https://ota.novaray.ccwu.cc/chick_latest.apk";
    private static final String GITHUB_URL = "https://github.com/LinrK114/";

    private static final String PREFS = "app_prefs";
    private static final String KEY_COUNT = "click_count";

    private TextView tvUpdateStatus;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private File pendingApk = null;
    private String pendingSha = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvUpdateStatus = findViewById(R.id.tvUpdateStatus);

        findViewById(R.id.rowDeveloper).setOnClickListener(v -> showDeveloperDialog());
        findViewById(R.id.rowLicense).setOnClickListener(v ->
                startActivity(new Intent(this, LicenseActivity.class)));
        findViewById(R.id.rowUpdate).setOnClickListener(v -> checkForUpdate());
        findViewById(R.id.rowClearCount).setOnClickListener(v -> showClearCountDialog());

        loadAboutImage();
    }

    private void showClearCountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_count_title)
                .setMessage(R.string.clear_count_message)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                    prefs.edit().putInt(KEY_COUNT, 0).apply();
                    Toast.makeText(this, R.string.count_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadAboutImage() {
        try (InputStream is = getAssets().open("about_image.png")) {
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
            if (bmp != null) {
                ((ImageView) findViewById(R.id.aboutImage)).setImageBitmap(bmp);
            }
        } catch (Exception ignored) {
        }
    }

    private void showDeveloperDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_jump_title)
                .setMessage(R.string.dialog_jump_message)
                .setPositiveButton(R.string.dialog_yes, (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    private void checkForUpdate() {
        tvUpdateStatus.setVisibility(View.VISIBLE);
        tvUpdateStatus.setText(R.string.checking_update);

        executor.execute(() -> {
            String serverVersion;
            try {
                long start = System.currentTimeMillis();
                serverVersion = httpGet(VERSION_URL)
                        .replace("\uFEFF", "")
                        .trim();
                long cost = System.currentTimeMillis() - start;
                android.util.Log.d("AboutActivity",
                        "VERSION ok, cost=" + cost + "ms, value=" + serverVersion);
            } catch (Exception e) {
                android.util.Log.e("AboutActivity", "VERSION failed: " + VERSION_URL, e);
                String msg = "version.txt 失败\n" + describeException(e);
                runOnUiThread(() -> tvUpdateStatus.setText(
                        getString(R.string.check_update_failed) + "\n" + msg));
                return;
            }

            String localVersion;
            try {
                localVersion = getPackageManager()
                        .getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                localVersion = "0.0.0";
            }

            android.util.Log.d("AboutActivity",
                    "server=" + serverVersion + " local=" + localVersion);

            if (!isNewerVersion(serverVersion, localVersion)) {
                final String sv = serverVersion;
                final String lv = localVersion;
                runOnUiThread(() -> tvUpdateStatus.setText(
                        getString(R.string.already_latest)
                                + "（服务器 " + sv + " / 本地 " + lv + "）"));
                return;
            }

            String note;
            try {
                long start = System.currentTimeMillis();
                note = httpGet(NOTE_URL);
                long cost = System.currentTimeMillis() - start;
                android.util.Log.d("AboutActivity",
                        "NOTE ok, cost=" + cost + "ms, length=" + note.length());
            } catch (Exception e) {
                android.util.Log.e("AboutActivity", "NOTE failed: " + NOTE_URL, e);
                String msg = "note.md 失败\n" + describeException(e);
                runOnUiThread(() -> tvUpdateStatus.setText(
                        getString(R.string.check_update_failed) + "\n" + msg));
                return;
            }

            String sha;
            try {
                sha = extractSha(httpGet(SHA_URL));
                android.util.Log.d("AboutActivity", "SHA ok, value=" + sha);
                if (sha.length() != 64) {
                    throw new IOException("sha.txt 格式不正确，长度=" + sha.length());
                }
            } catch (Exception e) {
                android.util.Log.e("AboutActivity", "SHA failed: " + SHA_URL, e);
                String msg = "sha.txt 失败\n" + describeException(e);
                runOnUiThread(() -> tvUpdateStatus.setText(
                        getString(R.string.check_update_failed) + "\n" + msg));
                return;
            }

            final String finalSha = sha;
            runOnUiThread(() -> {
                tvUpdateStatus.setVisibility(View.GONE);
                pendingSha = finalSha;
                showUpdateDialog(note);
            });
        });
    }

    private String describeException(Exception e) {
        String name = e.getClass().getSimpleName();
        String msg = e.getMessage() == null ? "" : e.getMessage();
        return name + ": " + msg;
    }

    private String extractSha(String raw) {
        if (raw == null) return "";
        String s = raw.replace("\uFEFF", "").trim();
        if (s.matches("[0-9a-fA-F]{64}")) {
            return s.toLowerCase();
        }
        Matcher m = Pattern.compile("[0-9a-fA-F]{64}").matcher(s);
        if (m.find()) {
            return m.group().toLowerCase();
        }
        return "";
    }

    private void showUpdateDialog(String noteMd) {
        TextView content = new TextView(this);
        content.setPadding(48, 24, 48, 24);
        Markwon markwon = Markwon.create(this);
        markwon.setMarkdown(content, noteMd);

        ScrollView sv = new ScrollView(this);
        sv.addView(content);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.update_found_title)
                .setView(sv)
                .setPositiveButton(R.string.update_start, (d, w) -> startDownloadApk())
                .setNegativeButton(R.string.update_cancel, null)
                .show();
    }

    private void startDownloadApk() {
        FrameLayout container = new FrameLayout(this);
        int size = (int) (getResources().getDisplayMetrics().density * 120);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
        lp.gravity = Gravity.CENTER;
        int pad = (int) (getResources().getDisplayMetrics().density * 16);
        container.setPadding(pad, pad, pad, pad);

        CircularProgressIndicator progress = new CircularProgressIndicator(this);
        progress.setIndeterminate(false);
        progress.setMax(100);
        progress.setProgress(0);
        progress.setIndicatorSize((int) (getResources().getDisplayMetrics().density * 72));
        progress.setTrackThickness((int) (getResources().getDisplayMetrics().density * 8));
        container.addView(progress, lp);

        TextView percent = new TextView(this);
        percent.setText("0%");
        percent.setTextSize(16);
        FrameLayout.LayoutParams lpText = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lpText.gravity = Gravity.CENTER;
        container.addView(percent, lpText);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.downloading)
                .setView(container)
                .setCancelable(false)
                .create();
        dialog.show();

        File dir = getExternalFilesDir(null);
        if (dir == null) dir = getCacheDir();
        final File apkFile = new File(dir, "chick_latest.apk");
        android.util.Log.d("AboutActivity", "apk dest=" + apkFile.getAbsolutePath());

        if (apkFile.exists()) {
            apkFile.delete();
        }

        executor.execute(() -> {
            try {
                downloadFile(APK_URL, apkFile, p -> runOnUiThread(() -> {
                    if (p < 0) {
                        percent.setText("下载中…");
                    } else {
                        progress.setProgress(p);
                        percent.setText(p + "%");
                    }
                }));

                runOnUiThread(() -> {
                    progress.setIndeterminate(true);
                    percent.setText("校验中…");
                });

                String actualSha;
                try {
                    actualSha = sha256(apkFile);
                } catch (Exception e) {
                    android.util.Log.e("AboutActivity", "sha256 compute failed", e);
                    String err = e.getMessage() == null ? e.toString() : e.getMessage();
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("校验失败")
                                .setMessage("无法计算文件哈希：\n" + err)
                                .setPositiveButton("是", (d, w) -> startDownloadApk())
                                .setNegativeButton("否", null)
                                .show();
                    });
                    return;
                }

                boolean ok = pendingSha != null
                        && pendingSha.length() == 64
                        && actualSha.equalsIgnoreCase(pendingSha);

                if (!ok) {
                    android.util.Log.e("AboutActivity",
                            "SHA mismatch: expected=" + pendingSha + " actual=" + actualSha);
                    final String expected = pendingSha == null ? "" : pendingSha;
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("校验失败")
                                .setMessage("下载的安装包与服务器哈希不一致。\n\n"
                                        + "期望: " + expected + "\n"
                                        + "实际: " + actualSha + "\n\n"
                                        + "是否重新下载？")
                                .setPositiveButton("是", (d, w) -> startDownloadApk())
                                .setNegativeButton("否", null)
                                .show();
                    });
                    return;
                }

                android.util.Log.d("AboutActivity", "SHA verified: " + actualSha);
                runOnUiThread(() -> {
                    dialog.dismiss();
                    pendingApk = apkFile;
                    tryInstallApk();
                });

            } catch (Exception e) {
                android.util.Log.e("AboutActivity", "download failed", e);
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                runOnUiThread(() -> {
                    dialog.dismiss();
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.download_failed)
                            .setMessage(msg)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            }
        });
    }

    private String sha256(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private void tryInstallApk() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.install_permission_title)
                        .setMessage("请在打开的设置页面中找到本应用，并允许“安装未知应用”")
                        .setPositiveButton("去设置", (d, w) -> {
                            Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        })
                        .setNegativeButton(R.string.update_cancel, null)
                        .show();
                return;
            }
        }
        installApk(pendingApk);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingApk != null && pendingApk.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                    || getPackageManager().canRequestPackageInstalls()) {
                File f = pendingApk;
                pendingApk = null;
                installApk(f);
            }
        }
    }

    private void installApk(File file) {
        Uri uri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    interface ProgressCallback {
        void onProgress(int percent);
    }

    private void downloadFile(String urlStr, File dest, ProgressCallback cb) throws Exception {
        int redirects = 0;
        String currentUrl = urlStr;
        HttpURLConnection conn = null;

        while (redirects <= 5) {
            try {
                conn = (HttpURLConnection) new URL(currentUrl).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Linux; Android 13) CustomClickApp");
                conn.setRequestProperty("Accept", "*/*");

                int code = conn.getResponseCode();
                android.util.Log.d("AboutActivity",
                        "download code=" + code + " url=" + currentUrl);

                if (code >= 300 && code < 400) {
                    String loc = conn.getHeaderField("Location");
                    if (loc == null) {
                        throw new IOException("重定向缺少 Location, code=" + code);
                    }
                    if (!loc.startsWith("http")) {
                        loc = new URL(new URL(currentUrl), loc).toString();
                    }
                    currentUrl = loc;
                    redirects++;
                    conn.disconnect();
                    conn = null;
                    continue;
                }

                if (code != 200) {
                    throw new IOException("HTTP " + code);
                }

                int total = conn.getContentLength();
                android.util.Log.d("AboutActivity", "download total=" + total);

                File parent = dest.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int len;
                    long done = 0;
                    int lastPercent = -1;
                    long lastLog = 0;

                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                        done += len;

                        long now = System.currentTimeMillis();
                        if (now - lastLog > 1000) {
                            android.util.Log.d("AboutActivity",
                                    "downloaded=" + done + " total=" + total);
                            lastLog = now;
                        }

                        if (total > 0) {
                            int p = (int) (done * 100 / total);
                            if (p != lastPercent) {
                                lastPercent = p;
                                final int fp = p;
                                cb.onProgress(fp);
                            }
                        } else {
                            cb.onProgress(-1);
                        }
                    }

                    android.util.Log.d("AboutActivity",
                            "download done, size=" + dest.length());
                    if (dest.length() == 0) {
                        throw new IOException("下载文件为 0 字节");
                    }
                }
                return;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        throw new IOException("重定向次数过多");
    }

    private String httpGet(String urlStr) throws Exception {
        int redirects = 0;
        String currentUrl = urlStr;

        while (redirects <= 5) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(currentUrl).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Linux; Android 13) CustomClickApp/" +
                                getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
                conn.setRequestProperty("Accept", "text/plain, text/markdown, */*");
                conn.setRequestProperty("Cache-Control", "no-cache");

                int code = conn.getResponseCode();

                if (code >= 300 && code < 400) {
                    String loc = conn.getHeaderField("Location");
                    if (loc == null) {
                        throw new IOException("重定向缺少 Location, code=" + code);
                    }
                    if (!loc.startsWith("http")) {
                        loc = new URL(new URL(currentUrl), loc).toString();
                    }
                    currentUrl = loc;
                    redirects++;
                    continue;
                }

                if (code != 200) {
                    throw new IOException("HTTP " + code);
                }

                String charset = "UTF-8";
                String contentType = conn.getContentType();
                if (contentType != null) {
                    Matcher m = Pattern
                            .compile("charset\\s*=\\s*([^\";\\s]+)", Pattern.CASE_INSENSITIVE)
                            .matcher(contentType);
                    if (m.find()) {
                        String cs = m.group(1).trim();
                        if (cs.startsWith("\"") && cs.endsWith("\"")) {
                            cs = cs.substring(1, cs.length() - 1);
                        }
                        if (Charset.isSupported(cs)) {
                            charset = cs;
                        }
                    }
                }

                byte[] data;
                try (InputStream in = conn.getInputStream()) {
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        bo.write(buf, 0, len);
                    }
                    data = bo.toByteArray();
                }

                if (data.length >= 3 &&
                        (data[0] & 0xFF) == 0xEF &&
                        (data[1] & 0xFF) == 0xBB &&
                        (data[2] & 0xFF) == 0xBF) {
                    byte[] stripped = new byte[data.length - 3];
                    System.arraycopy(data, 3, stripped, 0, stripped.length);
                    data = stripped;
                }

                String result = new String(data, charset);
                android.util.Log.d("AboutActivity",
                        "httpGet ok, url=" + currentUrl + " charset=" + charset +
                                " bytes=" + data.length + " chars=" + result.length());
                return result;

            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        throw new IOException("重定向次数过多");
    }

    private boolean isNewerVersion(String server, String local) {
        String[] s = server.split("\\.");
        String[] l = local.split("\\.");
        int n = Math.max(s.length, l.length);
        for (int i = 0; i < n; i++) {
            int sv = i < s.length ? parseIntSafe(s[i]) : 0;
            int lv = i < l.length ? parseIntSafe(l[i]) : 0;
            if (sv > lv) return true;
            if (sv < lv) return false;
        }
        return false;
    }

    private int parseIntSafe(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}