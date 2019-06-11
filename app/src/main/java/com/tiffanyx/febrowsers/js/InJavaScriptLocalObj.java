package com.tiffanyx.febrowsers.js;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.tiffanyx.febrowsers.MainActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class InJavaScriptLocalObj {
    Document doc;

    @JavascriptInterface
    public void showSource(String html) {
        try {
            doc = Jsoup.parse(html);
            Elements elements=doc.getElementsByTag("video");
            Element element=elements.get(0);
            String s=element.attr("src");
            if(s!=null&&!s.equals("")) {
                MainActivity.videoUrl=s;
            }else {
                MainActivity.videoUrl="";
            }
        }catch (Exception e){
            Log.e("myerror",e.getMessage());
            MainActivity.videoUrl="";
        }

    }

}
