package com.tiffanyx.febrowsers;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;

public class AddressInputActivity extends AppCompatActivity {
    private EditText editText;
    private ImageButton imageButton;

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_input);
        String url = getIntent().getStringExtra("url");
        ImageButton button = findViewById(R.id.closeBtn);
        button.setOnClickListener(v -> finish());
        editText = findViewById(R.id.addressTxv1);
        editText.setText(url);
        editText.selectAll();
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.requestFocus();
        editText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                returnResult();
                return true;
            }
            return false;
        });
        //显示软键盘
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                returnResult();
            }
            return false;
        });
        imageButton = findViewById(R.id.confirm);
        imageButton.setOnClickListener(v -> returnResult());
    }

    private void returnResult() {
        Intent intent = new Intent();
        intent.putExtra("enterUrl", editText.getText().toString());
        setResult(RESULT_OK, intent);
        finish();
    }
}
