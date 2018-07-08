package com.home.vas.RadarUltrasonic;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

public class LicenseActivity extends AppCompatActivity {

    private WebView LicenseWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LicenseWebView = (WebView) findViewById(R.id.LicenseWebView);
        LicenseWebView.getSettings().setTextZoom(100);
        LicenseWebView.setBackgroundColor(Color.TRANSPARENT);
        LicenseWebView.loadUrl("file:///android_asset/open_source_licenses.html");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
