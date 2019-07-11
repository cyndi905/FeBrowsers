package com.tiffanyx.febrowsers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tiffanyx.febrowsers.beans.Bookmark;
import com.tiffanyx.febrowsers.js.InJavaScriptLocalObj;
import com.tiffanyx.febrowsers.receiver.DownloadCompleteReceiver;
import com.tiffanyx.febrowsers.util.Constant;
import com.tiffanyx.febrowsers.zxing.activity.CaptureActivity;

import org.litepal.LitePal;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static String videoUrl="";
    private final int REQUEST_CAMERA = 1;
    private final int REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private final int REQUEST_READ_EXTERNAL_STORAGE = 3;
    private final String DEFAULT_HOME_PAGE = "https://m.baidu.com/?tn=simple#";
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
    private final static int BOOKMARK_REQUEST_CODE = 2323;
    private ValueCallback<Uri> uploadMessage;
    private ProgressBar progressBar;
    private WebView webView;
    private long pressBackStartTime = 0;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageButton stopOrRefresh;
    private EditText addressTxv;
    private boolean canRefresh;
    private DownloadCompleteReceiver receiver;
    private boolean isBlockScheme = false;
    private boolean isDownloadFileReqPermission = false;

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
        downloadManager.enqueue(request);
        Toast.makeText(MainActivity.this, "开始下载", Toast.LENGTH_SHORT).show();
    }

    private void forward() {
        if (webView.canGoForward()) {
            webView.goForward();
        } else {
            Toast.makeText(MainActivity.this, "已经不能前进了！", Toast.LENGTH_SHORT).show();
        }
    }

    private void search(String s) {//搜索方法
        webView.loadUrl("https://www.baidu.com/s?wd=" + s);
    }

    private void goBack() {//返回上一页方法
        if (webView.canGoBack()) {
            if (isBlockScheme) {
                webView.goBackOrForward(-2);
                isBlockScheme = false;
            } else {
                webView.goBack();
            }
        } else {
            Toast.makeText(MainActivity.this, "已经不能后退了！", Toast.LENGTH_SHORT).show();
        }
    }

    private void goHome() {
        webView.loadUrl(DEFAULT_HOME_PAGE);
    }

    private void changeUserAgent() {
        String insertStr = "(X11;Linux x86_64)";
        String str = webView.getSettings().getUserAgentString();
        Pattern pattern = Pattern.compile("Mozilla/5.0 (.*) A");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            webView.getSettings().setUserAgentString(str.replace(matcher.group(1), insertStr));
        }
        webView.reload();
    }

    private void loadOrSearch(String s) {
        if (s.length() != 0) {
            if (s.startsWith("http://") || s.startsWith("https://")) {
                webView.loadUrl(s);
            } else {
                String regex = "(((https|http)?://)?([a-z0-9]+[.])|(www.))"
                        + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";
                Pattern pat = Pattern.compile(regex.trim());//对比
                Matcher mat = pat.matcher(s.trim());
                boolean isUrl = mat.matches();
                if (isUrl) {
                    s = "http://" + s;
                    webView.loadUrl(s);
                } else {
                    search(s);
                }
            }
        }
    }

    private void scanQRCode() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
        } else {
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
        swipeRefreshLayout.setRefreshing(true);
        webView.reload();
    }

    private void openImageChooserActivity() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "选择图片"), FILE_CHOOSER_REQUEST_CODE);
    }

    private void showAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("关于浏览器");
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.setMessage("Fe浏览器" + version + "，一款纯净的无痕浏览器。本浏览器基于系统内置的WebView内核。");
        builder.setPositiveButton("知道了", null);
        builder.show();
    }

    private void exit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("退出浏览器");
        builder.setMessage("真的要退出浏览器吗？");
        builder.setPositiveButton("再见", (dialog, which) -> finish());
        builder.setNegativeButton("再看一下", null);
        builder.show();
    }

    private void share() {
        View view = createQRCode();
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);
        builder.setMessage(webView.getTitle());
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {//实现2次返回键退出
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - pressBackStartTime) >= 1500) {
                    Toast.makeText(MainActivity.this, "再按一次退出", Toast.LENGTH_SHORT).show();
                    pressBackStartTime = currentTime;
                } else {
                    finish();
                }
            }
        }
        return false;
    }

    private void showPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.menu);
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
            }
            return false;
        });
        popupMenu.show();
    }

    private void bookmark() {
        Intent intent=new Intent(this,BookmarkActivity.class);
        startActivityForResult(intent,BOOKMARK_REQUEST_CODE);
    }

    private void addBookmark(View v) {
        Bookmark bookmark=new Bookmark();
        bookmark.setTitle(webView.getTitle());
        bookmark.setUrl(webView.getUrl());
        if(bookmark.save()){
            final View layout = getLayoutInflater().inflate(R.layout.edit_bookmark_layout, null);
            final EditText titleEdt =layout.findViewById(R.id.bookmarkTitle);
            final EditText urlEdt=layout.findViewById(R.id.bookmarkUrl);
            titleEdt.setText(bookmark.getTitle());
            urlEdt.setText(bookmark.getUrl());
            Snackbar.make(v,"已添加到书签",Snackbar.LENGTH_LONG).setAction("编辑", v1 -> {
                AlertDialog.Builder builder=new AlertDialog.Builder(this);
                builder.setTitle(R.string.addBookmarkTitle).setView(layout).setPositiveButton(R.string.submit, (dialog, which) -> {
                    String title=titleEdt.getText().toString();
                    String url=urlEdt.getText().toString();
                    if(!TextUtils.isEmpty(title)&&!TextUtils.isEmpty(url)){
                        bookmark.setTitle(title);
                        bookmark.setUrl(url);
                        if(!bookmark.save()){
                            Toast.makeText(MainActivity.this,"修改书签失败",Toast.LENGTH_LONG).show();
                        }
                    }else {
                        Toast.makeText(MainActivity.this,"书签未修改",Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton(R.string.cancel,null).show();
            }).show();
        }
    }

    /**
     * 作为三方浏览器打开传过来的值
     * return:返回用于判断是否要打开主页
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

    //分享二维码创建
    public LinearLayout createQRCode() {
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
                Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "保存失败", Toast.LENGTH_SHORT).show());
            Log.e("myerror", e.getMessage());
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
                        int i = url.lastIndexOf(".");
                        save2Album(drawable2Bitmap(drawable), UUID.randomUUID().toString() + "." + url.substring((i + 1)));
                    } else {
                        Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_LONG).show();
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
        webView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);//允许http和https的混合连接，避免存在图片不加载的情况
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:window.local_obj.showSource('<head>'+" +
                        "document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) {
                    return false;
                }
                try {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        view.loadUrl(url);
                        return true;
                    } else {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        intent.addCategory(Intent.CATEGORY_BROWSABLE);
                        intent.setComponent(null);
                        intent.setSelector(null);
                        startActivity(intent);
                        return true;
                    }
                } catch (Exception e) {
                    isBlockScheme = true;
                    Toast.makeText(MainActivity.this, "不支持此应用转跳，已拦截", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        });
        webView.getSettings().setDisplayZoomControls(false);
        webView.setOnLongClickListener(view -> {
            final WebView.HitTestResult hitTestResult = webView.getHitTestResult();
            // 如果是图片类型或者是带有图片链接的类型
            if (hitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE || hitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setItems(new String[]{getString(R.string.download_pic)}, (dialogInterface, i) -> {
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                uploadMessageAboveL = filePathCallback;
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
                } else {
                    openImageChooserActivity();
                }
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (webView != null) {//判断webview是否为空，避免程序退出销毁webview时出现空指针错误
                    if (newProgress != 100) {
                        canRefresh = false;
                        addressTxv.setHint("正在加载...");
                        stopOrRefresh.setBackgroundResource(R.drawable.ic_action_stop);
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    } else {
                        canRefresh = true;
                        addressTxv.setHint(webView.getTitle());
                        stopOrRefresh.setBackgroundResource(R.drawable.ic_refresh);
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            }
        });
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {//开启下载功能
            //显示是否下载的dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            String filename = url.substring(url.lastIndexOf('/') + 1);
            builder.setTitle("文件下载");
            builder.setMessage("是否要下载 " + filename + " ？");
            builder.setPositiveButton("好的", (dialog, which) -> {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
            builder.setNegativeButton("不要下载", null);
            builder.setCancelable(false);
            builder.show();
        });
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setSupportZoom(true);
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
        back = findViewById(R.id.back_ico_btn);
        forward = findViewById(R.id.forward_ico_btn);
        home = findViewById(R.id.home_ico_btn);
        more = findViewById(R.id.more_ico_btn);
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
        swipeRefreshLayout = findViewById(R.id.srl);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            String url = webView.getUrl();
            webView.loadUrl(url);
        });
    }

    private void initReceiver() {
        receiver = new DownloadCompleteReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(receiver, intentFilter);
    }

    private void load() {
        if (getDataFromBrowser(getIntent())) {
            String action = getIntent().getAction();
            assert action != null;
            switch (action) {
                case OPEN_HOME_PAGE:
                    webView.loadUrl("https://www.baidu.com");
                    break;
                case SCAN_QR_CODE:
                    scanQRCode();
                    break;
                default:
                    webView.loadUrl(DEFAULT_HOME_PAGE);
                    break;
            }
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
        getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary, null));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (WebView.getCurrentWebViewPackage() == null) {
                Toast.makeText(this, "你的设备不支持WebView", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        LitePal.getDatabase();
        getVersion();
        initView();
        initWebView();
        defaultUserAgent = webView.getSettings().getUserAgentString();
        initReceiver();
        load();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA:
                //判断是否已经授权
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {//已授权
                    Intent intent = new Intent(getApplicationContext(), CaptureActivity.class);
                    startActivityForResult(intent, 100);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("缺少必要的权限").setMessage("请授予相机权限，否则无法进行扫码！").setPositiveButton("授予权限", (dialogInterface, i) -> requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA)).setNegativeButton("不了", null).show();
                }
                break;
            case REQUEST_WRITE_EXTERNAL_STORAGE:
                //判断是否已经授权
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {//已授权
                    if (isDownloadFileReqPermission) {
                        downloadBySystem(downloadUrl, downloadContentDisposition, downloadMimeType);
                        isDownloadFileReqPermission = false;
                    } else {
                        savePic();
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("缺少必要的权限").setMessage("请授予存储权限，否则无法进行下载！").setPositiveButton("授予权限", (dialog, which) -> requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE)).setNegativeButton("不了", null).show();
                }
                break;
            case REQUEST_READ_EXTERNAL_STORAGE:
                //判断是否已经授权
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {//已授权
                    openImageChooserActivity();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("缺少必要的权限").setMessage("请授予读取存储权限，否则无法读取文件！").setPositiveButton("授予权限", (dialogInterface, i) -> requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE)).setNegativeButton("不了", null).show();
                }
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
                if (result.startsWith("http://") || result.startsWith("https://")) {
                    webView.loadUrl(result);
                } else {
                    Toast.makeText(getApplicationContext(), "这貌似不是网址。", Toast.LENGTH_LONG).show();
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
        }else if (requestCode==BOOKMARK_REQUEST_CODE && resultCode == RESULT_OK){
            assert data != null;
            String s = data.getStringExtra("url");
            if (s != null)
                webView.loadUrl(s);
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

    private void playVideo() {
        if(videoUrl!=null&&!videoUrl.equals("")){
            Intent intent=new Intent(this,PlayerActivity.class);
            intent.putExtra("videoUrl",videoUrl);
            intent.putExtra("title",webView.getTitle());
            startActivity(intent);
        }else {
            Toast.makeText(this,"没有嗅探到视频",Toast.LENGTH_SHORT).show();
        }

    }
}
