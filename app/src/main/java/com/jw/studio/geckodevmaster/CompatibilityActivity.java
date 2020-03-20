package com.jw.studio.geckodevmaster;

//import android.Manifest;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

//import android.content.pm.PackageManager;

//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;

public class CompatibilityActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String MANUFACTURE = Build.MANUFACTURER;
        int OSVERSION = Build.VERSION.SDK_INT;
        if (MANUFACTURE.contains("LGE") && !(Build.VERSION_CODES.P < OSVERSION)) {
//            if (ContextCompat.checkSelfPermission(this,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                        3);
//            }
            Log.d("phone MANUFACTURE=" + MANUFACTURE, "intent start");
            Intent intent = new Intent(getApplicationContext(), GeckoViewActivity.class);
            intent.putExtra("com.lge.app.floating.launchAsFloating", true);
            startActivity(intent);
        } else {
            Log.d("phone MANUFACTURE" + MANUFACTURE, "not LG");
        }
        setContentView(R.layout.compatibility_activity);
    }
}