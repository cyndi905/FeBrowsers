package com.tiffanyx.febrowsers.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardUtil {
    private static ClipboardManager manager = null;

    public static ClipboardManager getClipboardManager(Context context) {
        if (manager != null) {
            return manager;
        } else {
            return manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        }
    }

    public static boolean setClipboard(ClipboardManager manager, String data) {
        try {
            ClipData clipData = ClipData.newPlainText("label", data);
            manager.setPrimaryClip(clipData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
