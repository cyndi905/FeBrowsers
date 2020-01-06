package com.tiffanyx.febrowsers.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtil {
    public static boolean isUrl(String s){
        String regex =  "^([hH][tT]{2}[pP]:/*|[hH][tT]{2}[pP][sS]:/*|[fF][tT][pP]:/*)(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+(\\?{0,1}(([A-Za-z0-9-~]+\\={0,1})([A-Za-z0-9-~]*)\\&{0,1})*)$";

        Pattern pat = Pattern.compile(regex.trim());//对比
        Matcher mat = pat.matcher(s.trim());
        return mat.matches();
    }
}
