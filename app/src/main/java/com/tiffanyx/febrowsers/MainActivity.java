package com.tiffanyx.febrowsers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tiffanyx.febrowsers.beans.Bookmark;
import com.tiffanyx.febrowsers.receiver.DownloadCompleteReceiver;
import com.tiffanyx.febrowsers.util.ClipboardUtil;
import com.tiffanyx.febrowsers.util.Constant;
import com.tiffanyx.febrowsers.util.HtmlUtil;
import com.tiffanyx.febrowsers.util.NetworkStatusUtil;
import com.tiffanyx.febrowsers.util.UrlUtil;
import com.tiffanyx.febrowsers.zxing.activity.CaptureActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.litepal.LitePal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static String videoUrl = "";
    private final int REQUEST_CAMERA = 1;
    private final int REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private final int REQUEST_READ_EXTERNAL_STORAGE = 3;
    private final int REQUEST_LOCATION = 4;
    private String homePage = "";
    private final String DEFAULT_HOME_PAGE = "https://m.baidu.com/?tn=simple#";
    private final String DEFAULT_SEARCH_ENGINE = "https://www.baidu.com/s?wd=";
    private String searchEngine = "";
    private final static String OPEN_HOME_PAGE = "openHomePage";
    private final static String SCAN_QR_CODE = "scanQR";
    private String defaultUserAgent;
    private String version = "";
    private String downloadUrl, downloadContentDisposition, downloadMimeType;
    private ImageButton back;
    private ImageButton forward;
    private ImageButton home;
    private ImageButton more;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private final static int FILE_CHOOSER_REQUEST_CODE = 10000;
    private final static int INPUT_REQUEST_CODE = 233;
    private final static int SETTING_REQUEST_CODE = 12;
    private final static int BOOKMARK_REQUEST_CODE = 2323;
    private ValueCallback<Uri> uploadMessage;
    private ProgressBar progressBar;
    private WebView webView;
    private long pressBackStartTime = 0;
    private ImageButton stopOrRefresh;
    private EditText addressTxv;
    private boolean canRefresh;
    private DownloadCompleteReceiver receiver;
    private boolean isBlockScheme = false;
    private boolean isDownloadFileReqPermission = false;
    private GeolocationPermissions.Callback callback1 = null;
    private String origin1 = null;
    private Timer timer = null;
    private Snackbar snackbar;
    private boolean isRequestPCVersion = false;
    private FrameLayout fVideoLayout;
    private View videoView;
    private LinearLayout head, bottom;
    private boolean isFullScreen = false;//是否手动设置全屏
    private String nightCode;//黑夜css
    private boolean isNightMode = false;
    private PopupMenu popupMenu = null;
    private boolean isEnableWebViewDarkMode = false;
    private int screenOrientation = 0;//记录当前横竖屏情况
    private String tempHtml = null;

    private void downloadBySystem(String url, String contentDisposition, String mimeType) {
        // 指定下载地址
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        // 允许媒体扫描，根据下载的文件类型被加入相册、音乐等媒体库
        request.allowScanningByMediaScanner();
        // 设置通知的显示类型，下载进行时和完成后显示通知
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // 设置通知栏的标题，如果不设置，默认使用文件名
//        request.setTitle("This is title");
        // 设置通知栏的描述
//        request.setDescription("This is description");
        // 允许在计费流量下下载
        request.setAllowedOverMetered(true);
        // 允许该记录在下载管理界面可见
        request.setVisibleInDownloadsUi(true);
        // 允许漫游时下载
        request.setAllowedOverRoaming(true);
        // 允许下载的网路类型
//      request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        // 设置下载文件保存的路径和文件名
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
//        另外可选一下方法，自定义下载路径
//        request.setDestinationUri()
//        request.setDestinationInExternalFilesDir()
        final DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        // 添加一个下载任务
        assert downloadManager != null;
        downloadManager.enqueue(request);
        Toast.makeText(MainActivity.this, R.string.beginDownload, Toast.LENGTH_SHORT).show();
    }

    private void forward() {
        if (webView.canGoForward()) {
            if (tempHtml != null) {
                tempHtml = null;
            }
            webView.goForward();
        } else {
            Toast.makeText(MainActivity.this, R.string.cannotForward, Toast.LENGTH_SHORT).show();
        }
    }

    private void search(String s) {//搜索方法
        webView.loadUrl(searchEngine + s);
    }

    private void goBack() {//返回上一页方法
        if (webView.canGoBack()) {
            if (tempHtml != null) {
                tempHtml = null;
            }
            if (isBlockScheme) {
                webView.goBackOrForward(-2);
                isBlockScheme = false;
            } else {
                webView.goBack();
            }
        } else {
            Toast.makeText(MainActivity.this, R.string.cannotBack, Toast.LENGTH_SHORT).show();
        }
    }

    private void goHome() {
        if (tempHtml != null) {
            tempHtml = null;
        }
        webView.loadUrl(homePage);
    }

    private void changeUserAgent() {
        String insertStr = "(X11;Linux x86_64)";
        String str = webView.getSettings().getUserAgentString();
        Pattern pattern = Pattern.compile("Mozilla/5.0 (.*) A");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            webView.getSettings().setUserAgentString(str.replace(Objects.requireNonNull(matcher.group(1)), insertStr));
        }
        webView.reload();
    }

    private void loadOrSearch(String s) {
        if (s.length() != 0) {
            if (s.startsWith("http://") || s.startsWith("https://")) {
                webView.loadUrl(s);
            } else {
                String ss = "http://" + s;
                boolean isUrl = UrlUtil.isUrl(ss.trim());
                if (isUrl) {
                    webView.loadUrl(ss);
                } else {
                    search(s);
                }
            }
        }
    }

    private void scanQRCode() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
            MainActivity.this.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        } else {
            Intent intent = new Intent(getApplicationContext(), CaptureActivity.class);
            startActivityForResult(intent, 100);
        }
    }

    private void openWithOtherBrowsers() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(webView.getUrl()));
        startActivity(intent);
    }

    private void requestPCVersion(MenuItem item) {
        if (item.isChecked()) {
            item.setChecked(false);
            isRequestPCVersion = false;
        } else {
            isRequestPCVersion = true;
            item.setChecked(true);
        }
        if (item.isChecked()) {
            changeUserAgent();
        } else {
            webView.getSettings().setUserAgentString(defaultUserAgent);
            webView.reload();
        }
    }

    private void refresh() {
        webView.reload();
    }

    private void openImageChooserActivity() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, getString(R.string.choicePic)), FILE_CHOOSER_REQUEST_CODE);
    }

    private void showAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.Dialog_Alert);
        builder.setTitle(R.string.aboutBrowser);
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.setMessage(getString(R.string.app_name) + version + getString(R.string.appInfo));
        builder.setPositiveButton(R.string.getIt, null);
        builder.show();
    }

    private void exit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.Dialog_Alert);
        builder.setTitle(R.string.exitApp);
        builder.setMessage(R.string.exitAppTip);
        builder.setPositiveButton(R.string.exitConfirm, (dialog, which) -> finish());
        builder.setNegativeButton(R.string.exitCancle, null);
        builder.show();
    }

    private void share() {
        View view = createQRCode();
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.Dialog_Alert);
        builder.setView(view);
        builder.setMessage(webView.getTitle());
        builder.setPositiveButton(R.string.submit, null);
        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {//实现2次返回键退出
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isFullScreen) {
                fullScreen();
                isFullScreen = false;
            }
            if (videoView != null) {
                hideVideoView();
                webView.reload();
                return false;
            }
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - pressBackStartTime) >= 1500) {
                    Toast.makeText(MainActivity.this, R.string.pressAgain, Toast.LENGTH_SHORT).show();
                    pressBackStartTime = currentTime;
                } else {
                    finish();
                }
            }
        }
        return false;
    }

    private void showPopupMenu(View v) {
        if (this.popupMenu != null) {
            popupMenu.dismiss();
        }
        PopupMenu popupMenu = new PopupMenu(this, v);
        this.popupMenu = popupMenu;
        popupMenu.inflate(R.menu.menu);
        popupMenu.getMenu().getItem(5).setChecked(isRequestPCVersion);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.about:
                    showAbout();
                    return true;
                case R.id.scanQRCode:
                    scanQRCode();
                    return true;
                case R.id.openByOther:
                    openWithOtherBrowsers();
                    return true;
                case R.id.requestPCVersion:
                    requestPCVersion(menuItem);
                    return true;
                case R.id.exit:
                    exit();
                    return true;
                case R.id.share:
                    share();
                    return true;
                case R.id.addBookmark:
                    addBookmark(v);
                    return true;
                case R.id.bookmark:
                    bookmark();
                    return true;
                case R.id.setting:
                    Intent intent = new Intent(this, SettingActivity.class);
                    startActivityForResult(intent, SETTING_REQUEST_CODE);
                    return true;
            }
            return false;
        });
        popupMenu.show();
        this.popupMenu = null;

    }

    private void bookmark() {
        Intent intent = new Intent(this, BookmarkActivity.class);
        startActivityForResult(intent, BOOKMARK_REQUEST_CODE);
    }

    private void addBookmark(View v) {
        Bookmark bookmark = new Bookmark();
        bookmark.setTitle(webView.getTitle());
        bookmark.setUrl(webView.getUrl());
        if (bookmark.save()) {
            @SuppressLint("InflateParams") final View layout = getLayoutInflater().inflate(R.layout.edit_bookmark_layout, null);
            final EditText titleEdt = layout.findViewById(R.id.bookmarkTitle);
            final EditText urlEdt = layout.findViewById(R.id.bookmarkUrl);
            titleEdt.setText(bookmark.getTitle());
            urlEdt.setText(bookmark.getUrl());
            Snackbar.make(v, R.string.addBookmarkSucc, Snackbar.LENGTH_LONG).setAction(R.string.edit, v1 -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Dialog_Alert);
                builder.setTitle(R.string.addBookmarkTitle).setView(layout).setPositiveButton(R.string.submit, (dialog, which) -> {
                    String title = titleEdt.getText().toString();
                    String url = urlEdt.getText().toString();
                    if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(url)) {
                        bookmark.setTitle(title);
                        bookmark.setUrl(url);
                        if (!bookmark.save()) {
                            Toast.makeText(MainActivity.this, R.string.editBookmarkFailed, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, R.string.bookmarkNoEdit, Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton(R.string.cancel, null).show();
            }).show();
        }
    }

    /**
     * 作为三方浏览器打开传过来的值
     *
     * @return:返回用于判断是否要打开主页
     */
    private boolean getDataFromBrowser(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            try {
                String scheme = data.getScheme();
                String host = data.getHost();
                String path = data.getPath();
                String url = scheme + "://" + host + path;
                webView.loadUrl(url);
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return true;
            }
        } else {
            return true;
        }
    }

    private Bitmap drawable2Bitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof NinePatchDrawable) {
            Bitmap bitmap = Bitmap
                    .createBitmap(
                            drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight(),
                            drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                    : Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        } else {
            return null;
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void fullScreen() {
        isFullScreen = true;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);//释放播放视频强制横屏
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//清除全屏
        }
    }

    //分享二维码创建
    private LinearLayout createQRCode() {
        LinearLayout linearLayout = new LinearLayout(getApplicationContext());
        linearLayout.setGravity(Gravity.CENTER);
        final ImageView img = new ImageView(getApplicationContext());
        img.post(() -> {
            int w, h;
            w = 500;
            h = 500;
            String url = webView.getUrl();
            try {
                if (url == null || "".equals(url) || url.length() < 1) {
                    return;
                }
                Hashtable<EncodeHintType, String> hints = new Hashtable<>();
                hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
                BitMatrix bitMatrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, w, h, hints);

                int[] pixels = new int[w * h];

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        if (bitMatrix.get(x, y)) {
                            pixels[y * w + x] = 0xff000000;
                        } else {
                            if (isNightMode)
                                pixels[y * w + x] = 0x00000000;
                            else
                                pixels[y * w + x] = 0xffffffff;
                        }
                    }
                }
                final Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
                img.setImageBitmap(bitmap);
            } catch (WriterException e) {
                e.printStackTrace();
            }
        });
        linearLayout.addView(img);
        return linearLayout;
    }

    private void save2Album(Bitmap bitmap, String fileName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            runOnUiThread(() -> {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                Toast.makeText(MainActivity.this, R.string.savePicSucc, Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.savePicFailed, Toast.LENGTH_SHORT).show());
            Log.e("myerror", Objects.requireNonNull(e.getMessage()));
        } finally {
            try {
                assert fos != null;
                fos.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void savePic() {
        final WebView.HitTestResult hitTestResult = webView.getHitTestResult();
        // 如果是图片类型或者是带有图片链接的类型
        if (hitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE || hitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            new Thread(() -> {
                FutureTarget<Drawable> target = Glide.with(getApplicationContext()).load(hitTestResult.getExtra()).submit();
                try {
                    String url = hitTestResult.getExtra();
                    Drawable drawable = target.get();
                    if (url != null) {
                        url = url.toUpperCase();
                        String suffix = "png";
                        if (url.contains("JPEG") || url.contains("JPG"))
                            suffix = "jpg";
                        save2Album(drawable2Bitmap(drawable), UUID.randomUUID().toString() + "." + suffix);
                    } else {
                        Toast.makeText(MainActivity.this, R.string.downloadFailed, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "下载出错" + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void initWebView() {
        webView = findViewById(R.id.web);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        WebView.setWebContentsDebuggingEnabled(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
//        webView.setInitialScale(25);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);//允许http和https的混合连接，避免存在图片不加载的情况
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.loadUrl("javascript:window.local_obj.searchVideo('<head>'+" +
                        "document.getElementsByTagName('html')[0].innerHTML+'</head>');" + "try{javascript:document.getElementsByClassName('" + HtmlUtil.getTagByUrl(url) + "')[0].addEventListener('click',function(){local_obj.fullscreen();return false;});}catch(err){}");
                if (isNightMode && isEnableWebViewDarkMode) {
                    changeWebViewMode(true);
                    webView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) {
                    return false;
                }
                try {
                    Uri parsedUri = Uri.parse(url);
                    PackageManager packageManager = getPackageManager();
                    Intent browseIntent = new Intent(Intent.ACTION_VIEW).setData(parsedUri);
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        if (timer != null) {
                            timer.cancel();
                        }
                        if (snackbar != null) {
                            snackbar.dismiss();
                        }
                        if (callback1 != null) {
                            callback1.invoke(origin1, false, false);
                        }
                        callback1 = null;
                        origin1 = null;
                        view.loadUrl(url);
                        return true;
                    }
                    if (browseIntent.resolveActivity(packageManager) != null) {
                        startActivity(browseIntent);
                        return true;
                    }
                    if (url.startsWith("intent:")) {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent.resolveActivity(packageManager) != null) {
                            try {
                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, R.string.unsupportJump, Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        }
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + intent.getPackage()));
                        if (marketIntent.resolveActivity(packageManager) != null) {
                            startActivity(marketIntent);
                            return true;
                        }
                        return true;
                    }
                    return true;
                } catch (Exception e) {
                    isBlockScheme = true;
                    Toast.makeText(MainActivity.this, R.string.unsupportJump, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        webView.getSettings().setDisplayZoomControls(false);
        webView.setOnLongClickListener(view -> {
            final WebView.HitTestResult hitTestResult = webView.getHitTestResult();
            // 如果是图片类型或者是带有图片链接的类型
            if (hitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE || hitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Dialog_Alert);
                builder.setItems(new String[]{getString(R.string.download_pic)}, (dialogInterface, i) -> {
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                                //申请权限
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
                            } else {
                                savePic();
                            }
                        }
                );
                builder.show();
                return true;
            }
            return false;
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback1 = callback;
                origin1 = origin;
                snackbar = Snackbar.make(webView, getString(R.string.allow) + " " + origin + getString(R.string.allowGetLocation), Snackbar.LENGTH_INDEFINITE).setAction(R.string.allow, v -> {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                    } else {
                        callback.invoke(origin, true, true);
                    }
                });
                snackbar.show();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(snackbar::dismiss);
                        callback.invoke(origin, false, false);
                        snackbar = null;
                    }
                }, 5000);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                uploadMessageAboveL = filePathCallback;
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                    //申请权限
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
                } else {
                    openImageChooserActivity();
                }
                return true;
            }

            @SuppressLint("SourceLockedOrientationActivity")
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                videoView = view;
                fVideoLayout.setVisibility(View.VISIBLE);
                fVideoLayout.addView(videoView);
                fVideoLayout.bringToFront();
                head.setVisibility(View.GONE);
                bottom.setVisibility(View.GONE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//设置横屏
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
            }

            @Override
            public void onHideCustomView() {
                hideVideoView();
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (webView != null) {//判断webView是否为空，避免程序退出销毁webView时出现空指针错误
                    if (newProgress != 100) {
                        if (isNightMode && isEnableWebViewDarkMode) {
                            webView.setVisibility(View.INVISIBLE);
                        }
                        canRefresh = false;
                        addressTxv.setHint(R.string.loading);
                        stopOrRefresh.setBackgroundResource(R.drawable.ic_action_stop);
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    } else {
                        canRefresh = true;
                        addressTxv.setHint(webView.getTitle());
                        stopOrRefresh.setBackgroundResource(R.drawable.ic_refresh);
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
        });
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {//开启下载功能
            //显示是否下载的dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.Dialog_Alert);
            String filename = url.substring(url.lastIndexOf('/') + 1);
            builder.setTitle(R.string.fileDownload);
            builder.setMessage(getString(R.string.isFileDownload) + filename + " ？");
            builder.setPositiveButton(R.string.submit, (dialog, which) -> {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                    downloadUrl = url;
                    downloadContentDisposition = contentDisposition;
                    downloadMimeType = mimetype;
                    isDownloadFileReqPermission = true;
                    //申请权限
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    downloadBySystem(url, contentDisposition, mimetype);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.setCancelable(false);
            builder.show();
        });
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setSupportZoom(true);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void hideVideoView() {
        //退出全屏
        if (videoView == null) {
            return;
        }
        //移除全屏视图并隐藏
        fVideoLayout.removeView(videoView);
        fVideoLayout.setVisibility(View.GONE);
        head.setVisibility(View.VISIBLE);
        bottom.setVisibility(View.VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);//释放播放视频强制横屏
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//清除全屏
        videoView = null;
    }

    private void getVersion() {
        PackageManager pm = getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = pm.getPackageInfo(getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        head = findViewById(R.id.head);
        bottom = findViewById(R.id.bottomNav);
        back = findViewById(R.id.back_ico_btn);
        forward = findViewById(R.id.forward_ico_btn);
        home = findViewById(R.id.home_ico_btn);
        more = findViewById(R.id.more_ico_btn);
        fVideoLayout = findViewById(R.id.full_video);
        more.setOnClickListener(this);
        registerForContextMenu(more);
        ImageButton viewBtn = findViewById(R.id.play_ico_btn);
        back.setOnClickListener(this);
        forward.setOnClickListener(this);
        home.setOnClickListener(this);
        viewBtn.setOnClickListener(this);
        stopOrRefresh = findViewById(R.id.stop_or_refresh);
        addressTxv = findViewById(R.id.addressTxv);
        stopOrRefresh.setOnClickListener(this);
        progressBar = findViewById(R.id.progressBar);
        addressTxv.setOnClickListener(this);
    }

    private void initReceiver() {
        receiver = new DownloadCompleteReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(receiver, intentFilter);
    }

    private void load(Bundle status) {
        if (getDataFromBrowser(getIntent())) {
            String action = getIntent().getAction();
            assert action != null;
            switch (action) {
                case OPEN_HOME_PAGE:
                    webView.loadUrl(homePage);
                    break;
                case SCAN_QR_CODE:
                    scanQRCode();
                    break;
                default:
                    if (status == null) {
                        webView.loadUrl(homePage);
                    } else {
                        String u = status.getString("url");
                        assert u != null;
                        if (u.equals("about:blank")) {
                            if (status.getString("webInfo") != null) {
                                webView.loadDataWithBaseURL(null, status.getString("webInfo"), "text/html", "utf-8", null);
                            }
                        } else {
                            webView.loadUrl(u);
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (screenOrientation != newConfig.orientation) {//防止全屏播放视频时重新绘制界面
            screenOrientation = newConfig.orientation;
            return;
        }
        int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                // Night mode is not active, we're using the light theme
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                recreate();
                isNightMode = false;
                changeWebViewMode(false);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                // Night mode is active, we're using dark theme
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                recreate();
                isNightMode = true;
                changeWebViewMode(true);
                break;
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getDataFromBrowser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDayNightMode();
        screenOrientation = getResources().getConfiguration().orientation;
        getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimaryBottomNav, null));
        if (WebViewCompat.getCurrentWebViewPackage(this) == null) {
            Toast.makeText(this, R.string.deviceNotWebview, Toast.LENGTH_LONG).show();
            finish();
        }
        LitePal.initialize(this);//启动数据库
        getSetting();
        getVersion();
        initView();
        initWebView();
        defaultUserAgent = webView.getSettings().getUserAgentString();//取得原始用户代理，用于复原代理
        initReceiver();
        checkNetwork();
        load(savedInstanceState);
    }

    private void checkNetwork() {
        if (!NetworkStatusUtil.isNetworkEnable(getApplicationContext())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Toast.makeText(getApplicationContext(), R.string.networkDisableTip, Toast.LENGTH_LONG).show();
                Intent panelIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
                startActivityForResult(panelIntent, 0);
            }
        }
    }

    private void initDayNightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            isNightMode = true;
        }
    }

    private void getSetting() {
        SharedPreferences sharedPreferences = getSharedPreferences("setting", MODE_PRIVATE);
        homePage = sharedPreferences.getString("home", DEFAULT_HOME_PAGE);
        searchEngine = sharedPreferences.getString("search", DEFAULT_SEARCH_ENGINE);
        try {
            if (isEnableWebViewDarkMode != (isEnableWebViewDarkMode = sharedPreferences.getBoolean("enableWebViewDark", false))) {
                if (isEnableWebViewDarkMode) {
                    changeWebViewMode(true);
                } else {
                    webView.reload();
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("url", webView.getUrl());
        if (tempHtml != null) {
            outState.putString("webInfo", tempHtml);
            tempHtml = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA:
                //判断是否已经授权
                if (grantResults[0] == PERMISSION_GRANTED) {//已授权
                    Intent intent = new Intent(getApplicationContext(), CaptureActivity.class);
                    startActivityForResult(intent, 100);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Dialog_Alert);
                    builder.setTitle(R.string.noPermissions).setMessage(R.string.noCamPersmissions).setPositiveButton(R.string.submitPermissions, (dialogInterface, i) -> requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA)).setNegativeButton(R.string.cancel, null).show();
                }
                break;
            case REQUEST_WRITE_EXTERNAL_STORAGE:
                //判断是否已经授权
                if (grantResults[0] == PERMISSION_GRANTED) {//已授权
                    if (isDownloadFileReqPermission) {
                        downloadBySystem(downloadUrl, downloadContentDisposition, downloadMimeType);
                        isDownloadFileReqPermission = false;
                    } else {
                        savePic();
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Dialog_Alert);
                    builder.setTitle(R.string.noPermissions).setMessage(R.string.noWriteStoragePermissions).setPositiveButton(R.string.submitPermissions, (dialog, which) -> requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE)).setNegativeButton(R.string.cancel, null).show();
                }
                break;
            case REQUEST_READ_EXTERNAL_STORAGE:
                //判断是否已经授权
                if (grantResults[0] == PERMISSION_GRANTED) {//已授权
                    openImageChooserActivity();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Dialog_Alert);
                    builder.setTitle(R.string.noPermissions).setMessage(R.string.noReadStoragePermissions).setPositiveButton(R.string.submitPermissions, (dialogInterface, i) -> requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE)).setNegativeButton(R.string.cancel, null).show();
                }
                break;
            case REQUEST_LOCATION:
                //判断是否已经授权
                if (grantResults[0] == PERMISSION_GRANTED) {//已授权
                    if (origin1 != null && callback1 != null)
                        callback1.invoke(origin1, true, true);
                }
                callback1 = null;
                origin1 = null;
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            if (data != null) {
                String result = data.getStringExtra(Constant.INTENT_EXTRA_KEY_QR_SCAN);
                assert result != null;
                if (result.startsWith("http://") || result.startsWith("https://")) {
                    webView.loadUrl(result);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.Dialog_Alert);
                    builder.setTitle(R.string.QRCodeInfo);
                    builder.setMessage(R.string.QRCodeInfoTip);
                    builder.setPositiveButton(R.string.submit, (dialog, which) -> {
                        String html = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no\"><title>" + getString(R.string.QRCodeResult) + "</title></head><h3>" + getString(R.string.QRCodeResult) + "</h3>" + result + "<br/><br/><button onclick=\"local_obj.copy2Clipboard('" + result + "')\">" + getString(R.string.wCopy) + "</button></html>";
                        tempHtml = html;
                        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
                    });
                    builder.setNegativeButton(R.string.cancel, null);
                    builder.show();
                }
            }
        } else if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (null == uploadMessage && null == uploadMessageAboveL) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (uploadMessage != null) {
                uploadMessage.onReceiveValue(result);
                uploadMessage = null;
            }
        } else if (requestCode == INPUT_REQUEST_CODE && resultCode == RESULT_OK) {
            assert data != null;
            String s = data.getStringExtra("enterUrl");
            if (s != null)
                loadOrSearch(s);
        } else if (requestCode == BOOKMARK_REQUEST_CODE && resultCode == RESULT_OK) {
            assert data != null;
            String s = data.getStringExtra("url");
            if (s != null)
                webView.loadUrl(s);
        } else if (requestCode == SETTING_REQUEST_CODE && resultCode == RESULT_OK) {
            assert data != null;
            boolean b = data.getBooleanExtra("isSettingChange", false);
            if (b) {
                getSetting();
                Snackbar.make(webView, getString(R.string.applySetting), Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void onActivityResultAboveL(int requestCode, int resultCode, Intent data) {
        if (requestCode != FILE_CHOOSER_REQUEST_CODE || uploadMessageAboveL == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String dataString = data.getDataString();
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }

    @Override
    protected void onDestroy() {//程序退出时销毁WebView避免内存泄露
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_ico_btn:
                goBack();
                break;
            case R.id.forward_ico_btn:
                forward();
                break;
            case R.id.home_ico_btn:
                goHome();
                break;
            case R.id.more_ico_btn:
                showPopupMenu(v);
                break;
            case R.id.play_ico_btn:
                playVideo();
                break;
            case R.id.stop_or_refresh:
                if (canRefresh) {
                    refresh();
                } else {
                    webView.stopLoading();
                }
                break;
            case R.id.addressTxv:
                Intent intent = new Intent(this, AddressInputActivity.class);
                intent.putExtra("url", webView.getUrl());
                startActivityForResult(intent, INPUT_REQUEST_CODE);
                break;
        }
    }

    public void changeWebViewMode(boolean isNight) {
        try {
            if (isNight) {
                if (nightCode == null) {
                    InputStream is = getResources().openRawResource(R.raw.night);
                    byte[] buffer = new byte[is.available()];
                    is.read(buffer);
                    is.close();
                    nightCode = Base64.encodeToString(buffer, Base64.NO_WRAP);
                }
                webView.loadUrl("javascript:(function() {" + "var parent = document.getElementsByTagName('head').item(0);" + "var style = document.createElement('style');" + "style.type = 'text/css';" + "style.innerHTML = window.atob('" + nightCode + "');" + "parent.appendChild(style)" + "})();");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playVideo() {
        if (videoUrl != null && !videoUrl.equals("")) {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("videoUrl", videoUrl);
            intent.putExtra("title", webView.getTitle());
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.noFindVedio, Toast.LENGTH_SHORT).show();
        }

    }

    public final class InJavaScriptLocalObj {
        Document doc;


        @JavascriptInterface
        public void copy2Clipboard(String msg) {
            ClipboardManager manager = ClipboardUtil.getClipboardManager(getApplicationContext());
            if (ClipboardUtil.setClipboard(manager, msg)) {
                Toast.makeText(getApplicationContext(), "复制成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "复制失败", Toast.LENGTH_SHORT).show();
            }
        }

        @JavascriptInterface
        public void fullscreen() {
            fullScreen();
        }

        @JavascriptInterface
        public void searchVideo(String html) {
            try {
                doc = Jsoup.parse(html);
                Elements elements = doc.getElementsByTag("video");
                Element element = elements.get(0);
                String s = element.attr("src");
                if (s != null && !s.equals("")) {
                    MainActivity.videoUrl = s;
                } else {
                    MainActivity.videoUrl = "";
                }
            } catch (Exception e) {
                Log.e("myerror", Objects.requireNonNull(e.getMessage()));
                MainActivity.videoUrl = "";
            }
        }
    }
}
