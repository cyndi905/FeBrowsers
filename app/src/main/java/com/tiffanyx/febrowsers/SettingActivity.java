package com.tiffanyx.febrowsers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.tiffanyx.febrowsers.util.UrlUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SettingActivity extends AppCompatActivity {
    private ArrayList<String> content =new ArrayList<>();
    private List<Map<String,Object>> lists=new ArrayList<>();
    private boolean isSettingChange=false;
    private Map<String,String> searchEngines=new HashMap<>();
    private int searchCheckedItem=4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        String[] settingItems = {getString(R.string.settingHomeItem),getString(R.string.settingSearchEngine)};
        setTitle(getString(R.string.setting));
        String[] ses=new String[]{getString(R.string.searchEngGoogle),getString(R.string.searchEngBaidu),getString(R.string.searchEngBing),getString(R.string.searchEngSougou),getString(R.string.searchEngOther)};
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        searchEngines.put("sougou","https://wap.sogou.com/web/searchList.jsp?keyword=");
        searchEngines.put("baidu","https://www.baidu.com/s?wd=");
        searchEngines.put("bing","https://cn.bing.com/search?q=");
        searchEngines.put("google","https://www.google.com/search?q=");
        Iterator it = searchEngines.keySet().iterator();
        String key = null;
        String value = null;
        String searchEngName=getString(R.string.searchEngOther);
        SharedPreferences sharedPreferences=getSharedPreferences("setting",MODE_PRIVATE);
        String hp=sharedPreferences.getString("home","https://m.baidu.com/?tn=simple#");
        String se=sharedPreferences.getString("search",searchEngines.get("baidu"));
        while (it.hasNext()){
            key= (String) it.next();
            Log.e("m",searchEngines.get(key));
            if(searchEngines.get(key).equals(se)){
                switch (key) {
                    case "baidu":
                        searchEngName = getString(R.string.searchEngBaidu);
                        searchCheckedItem = 1;
                        break;
                    case "sougou":
                        searchEngName = getString(R.string.searchEngSougou);
                        searchCheckedItem = 3;
                        break;
                    case "bing":
                        searchEngName = getString(R.string.searchEngBing);
                        searchCheckedItem = 2;
                        break;
                    default:
                        searchEngName = getString(R.string.searchEngGoogle);
                        searchCheckedItem = 0;
                        break;
                }
            }
        }
        content.add(hp);
        content.add(searchEngName);
        ListView lv=findViewById(R.id.settingLv);
        for (int i=0;i<settingItems.length;i++){
            HashMap<String,Object> hashMap=new HashMap<>();
            hashMap.put("itemName",settingItems[i]);
            hashMap.put("itemContent",content.get(i));
            lists.add(hashMap);
        }
        SimpleAdapter simpleAdapter=new SimpleAdapter(this,lists,R.layout.setting_list,new String[]{"itemName","itemContent"},new int[]{R.id.t1,R.id.t2});
        lv.setAdapter(simpleAdapter);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            switch (position){
                case 0:
                    final View layout = getLayoutInflater().inflate(R.layout.setting_hompage, null);
                    final EditText homeEdt = layout.findViewById(R.id.homePageEdt);
                    homeEdt.setText(hp);
                    AlertDialog.Builder builder=new AlertDialog.Builder(SettingActivity.this);
                    builder.setTitle(getString(R.string.settingHp)).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.submit, (dialog, which) -> {
                        String h=homeEdt.getText().toString();
                        if(!h.equals("")){
                            String rh="";
                            if (h.startsWith("http://") || h.startsWith("https://")) {
                                rh=h;
                            }else {
                                StringBuffer buffer=new StringBuffer();
                                buffer.append("http://");
                                buffer.append(h);
                                rh=buffer.toString();
                            }
                            if(UrlUtil.isUrl(rh)){
                                sharedPreferences.edit().putString("home",rh).commit();
                                lists.get(0).put("itemContent",rh);
                                simpleAdapter.notifyDataSetChanged();
                                isSettingChange=true;
                            }else {
                                Toast.makeText(SettingActivity.this,getString(R.string.inputWebsiteErro),Toast.LENGTH_SHORT).show();
                            }

                        }
                    }).setView(layout).show();
                    break;
                case 1:
                    AlertDialog.Builder builder1=new AlertDialog.Builder(this).setTitle(R.string.settingSearchEngine).setSingleChoiceItems(ses, searchCheckedItem, (dialog, which) -> {
                        switch (which){
                            case 0:
                                searchCheckedItem=0;
                                sharedPreferences.edit().putString("search", searchEngines.get("google")).commit();
                                lists.get(1).put("itemContent",getString(R.string.searchEngGoogle));
                                simpleAdapter.notifyDataSetChanged();
                                isSettingChange=true;
                                break;
                            case 1:
                                searchCheckedItem=1;
                                sharedPreferences.edit().putString("search", searchEngines.get("baidu")).commit();
                                lists.get(1).put("itemContent",getString(R.string.searchEngBaidu));
                                simpleAdapter.notifyDataSetChanged();
                                isSettingChange=true;
                                break;
                            case 3:
                                searchCheckedItem=3;
                                sharedPreferences.edit().putString("search", searchEngines.get("sougou")).commit();
                                lists.get(1).put("itemContent",getString(R.string.searchEngSougou));
                                simpleAdapter.notifyDataSetChanged();
                                isSettingChange=true;
                                break;
                            case 2:
                                searchCheckedItem=3;
                                sharedPreferences.edit().putString("search", searchEngines.get("bing")).commit();
                                lists.get(1).put("itemContent",getString(R.string.searchEngBing));
                                simpleAdapter.notifyDataSetChanged();
                                isSettingChange=true;
                                break;
                            case 4:
                                View v=getLayoutInflater().inflate(R.layout.search_engin_edit,null);
                                EditText editText=v.findViewById(R.id.search_engine);
                                if(searchCheckedItem==4){
                                    editText.setText(sharedPreferences.getString("search",""));
                                }
                                AlertDialog.Builder builder2=new AlertDialog.Builder(this).setView(v).setPositiveButton(R.string.submit, (dialog1, which1) -> {
                                    String s=editText.getText().toString();
                                    if(!s.equals("")){
                                        String buffer = "http://" + s;
                                        s = buffer;
                                        if(UrlUtil.isUrl(s.trim())){
                                            sharedPreferences.edit().putString("search",s).commit();
                                            searchCheckedItem=4;
                                            lists.get(1).put("itemContent",getString(R.string.searchEngOther));
                                            simpleAdapter.notifyDataSetChanged();
                                            isSettingChange=true;
                                        }else {
                                            Toast.makeText(SettingActivity.this,R.string.inputWebsiteErro,Toast.LENGTH_SHORT).show();
                                        }
                                    }else {
                                        Toast.makeText(SettingActivity.this,R.string.inputWebsiteErro,Toast.LENGTH_SHORT).show();
                                    }
                                }).setTitle(R.string.searchEngOther);
                                builder2.show();
                                break;
                        }
                    });
                    builder1.show();
                    break;
            }
        });
    }
    public void returnSettingChange(){
        Intent intent = new Intent();
        intent.putExtra("isSettingChange", isSettingChange);
        setResult(RESULT_OK, intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                returnSettingChange();
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        returnSettingChange();
        super.onBackPressed();
    }
}
