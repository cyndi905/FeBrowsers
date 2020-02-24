package com.tiffanyx.febrowsers;

import android.content.Intent;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.tiffanyx.febrowsers.adapter.BookmarkAdapter;
import com.tiffanyx.febrowsers.beans.Bookmark;

import org.litepal.LitePal;

import java.util.List;

public class BookmarkActivity extends AppCompatActivity {
    private ListView listView;
    private int item_id;
    private List<Bookmark> bookmarks;
    BookmarkAdapter adapter;
    private boolean isMultiSelect = false;
    Menu mMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);
        setTitle(R.string.bookmark);
        listView = findViewById(R.id.bookmarkList);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setHomeButtonEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
        }
        registerForContextMenu(listView);
        bookmarks = LitePal.findAll(Bookmark.class);
        if (bookmarks.size() == 0) {
            setContentView(R.layout.no_bookmark);
        } else {
            adapter = new BookmarkAdapter(this, bookmarks);
            listView.setAdapter(adapter);
        }
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent();
            intent.putExtra("url", bookmarks.get(position).getUrl());
            setResult(RESULT_OK, intent);
            finish();
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.del:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        item_id = info.position;
        getMenuInflater().inflate(R.menu.context_menu, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_item:
                final View layout = getLayoutInflater().inflate(R.layout.edit_bookmark_layout, null);
                final EditText titleEdt = layout.findViewById(R.id.bookmarkTitle);
                final EditText urlEdt = layout.findViewById(R.id.bookmarkUrl);
                titleEdt.setText(bookmarks.get(item_id).getTitle());
                urlEdt.setText(bookmarks.get(item_id).getUrl());
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.addBookmarkTitle).setView(layout).setPositiveButton(R.string.submit, (dialog, which) -> {
                    String title = titleEdt.getText().toString();
                    String url = urlEdt.getText().toString();
                    if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(url)) {
                        bookmarks.get(item_id).setTitle(title);
                        bookmarks.get(item_id).setUrl(url);
                        if (!bookmarks.get(item_id).save()) {
                            Toast.makeText(BookmarkActivity.this, "修改书签失败", Toast.LENGTH_LONG).show();
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(BookmarkActivity.this, "书签未修改", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton(R.string.cancel, null).show();
                break;
            case R.id.del_item:
                int i = LitePal.deleteAll(Bookmark.class, "id=?", String.valueOf(bookmarks.get(item_id).getId()));
                if (i > 0) {
                    bookmarks.remove(item_id);
                    adapter.notifyDataSetChanged();
                    if (bookmarks.size() == 0) {
                        setContentView(R.layout.no_bookmark);
                    }
                }
                break;
            case R.id.choice_item:
//                isMultiSelect=true;
//                adapter=new BookmarkAdapter(this,bookmarks,true,item_id);
//                listView.setAdapter(adapter);
//                showMenu();
                break;
        }
        return super.onContextItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu=menu;
        getMenuInflater().inflate(R.menu.bookmark_multi_choice_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if(isMultiSelect){
            isMultiSelect=false;
            adapter=new BookmarkAdapter(this,bookmarks);
            listView.setAdapter(adapter);
            hiddenMenu();
        }else {
            finish();
        }
    }
    private void hiddenMenu(){
        for (int i = 0; i < mMenu.size(); i++){
            mMenu.getItem(i).setVisible(false);
        }
    }
    private void showMenu(){
        for (int j = 0; j < mMenu.size(); j++){
            mMenu.getItem(j).setVisible(true);
        }
    }
}
