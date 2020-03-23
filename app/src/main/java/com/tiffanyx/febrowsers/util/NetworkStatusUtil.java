package com.tiffanyx.febrowsers.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;

public class NetworkStatusUtil {

    public static boolean isNetworkEnable(Context c) {
        ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork()) != null;
        } else {
            return connectivityManager.getActiveNetworkInfo() != null;
        }

    }
}
