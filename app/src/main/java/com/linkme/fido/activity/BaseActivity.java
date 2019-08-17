package com.linkme.fido.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.linkme.fido.LinkMeApp;

/**
 * BaseActivity
 * 공통처리 로직 구현
 * 아직 없음...
 */
public class BaseActivity extends AppCompatActivity {

    private long pressedMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if(System.currentTimeMillis() > pressedMillis + 2000) {
            pressedMillis = System.currentTimeMillis();
            showExitMessage();
        } else if(System.currentTimeMillis() <= pressedMillis + 2000) {
            finish();
        }
    }

    private void showExitMessage() {
        LinkMeApp app = (LinkMeApp) getApplication();
        app.showToast("한번더 누르면 종료띠", Toast.LENGTH_SHORT);
    }
}
