package com.tiffanyx.febrowsers.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import com.tiffanyx.febrowsers.R;
import com.tiffanyx.febrowsers.beans.Bookmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BookmarkAdapter extends BaseAdapter {
    List<Bookmark> data;
    Context context;
    boolean isMultiSelect=false;
    int selectItemId;
    public ArrayList<Integer> selectedItemsId=new ArrayList<>();

    public BookmarkAdapter(Context context, List<Bookmark> data) {
        this.context = context;
        this.data = data;
    }
    public BookmarkAdapter(Context context, List<Bookmark> data,boolean isMultiSelect,int selectItemId) {
        this.context = context;
        this.data = data;
        this.isMultiSelect = isMultiSelect;
        this.selectItemId=selectItemId;
        selectedItemsId.add(selectItemId);
    }

    @Override
    public int getCount() {
        return data == null ? 0 : data.size();
    }

    @Override
    public Object getItem(int position) {
        return data == null ? null : data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.bookmark_item, null);
        TextView titleTxv = view.findViewById(R.id.itemTitle);
        TextView urlTxv = view.findViewById(R.id.itemUrl);
        Bookmark bookmark = data.get(position);
        titleTxv.setText(bookmark.getTitle());
        urlTxv.setText(bookmark.getUrl());
        CheckBox checkBox=view.findViewById(R.id.cb);
        checkBox.setOnClickListener(v -> {
            if(checkBox.isChecked())
                selectedItemsId.add(position);
            else{
                selectedItemsId.remove(Integer.valueOf(position));
            }
            Collections.sort(selectedItemsId);
        });
        if(isMultiSelect){
            checkBox.setVisibility(View.VISIBLE);
            if(position==selectItemId){
                checkBox.setChecked(true);
            }
        }
        return view;
    }
}
