package com.tiffanyx.febrowsers.util;

public class HtmlUtil {
    public static String getTagByUrl(String url) {
        if (url.contains("qq")) {
            return "tvp_fullscreen_button"; // http://m.v.qq.com
        } else if (url.contains("youku")) {
            return "x-zoomin";              // http://www.youku.com
        } else if (url.contains("bilibili")) {
            return "icon-widescreen";       // http://www.bilibili.com/mobile/index.html
        }
        return "";
    }

}
