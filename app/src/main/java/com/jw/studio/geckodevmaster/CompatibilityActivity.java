package com.jw.studio.geckodevmaster;

//import android.Manifest;
import android.app.Activity;
import android.content.Intent;
//import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;

public class CompatibilityActivity extends Activity {

    private String MANUFACTURE;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        MANUFACTURE = Build.MANUFACTURER;

        if(MANUFACTURE.contains("LGE")){
//            if (ContextCompat.checkSelfPermission(this,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                        3);
//            }
            Log.d("phone MANUFACTURE="+MANUFACTURE,"intent start");
            Intent intent = new Intent(getApplicationContext(), GeckoViewActivity.class);
            intent.putExtra("com.lge.app.floating.launchAsFloating", true);
            startActivity(intent);
        }else{
            Log.d("phone MANUFACTURE"+MANUFACTURE,"not LG");

        }setContentView(R.layout.compatibility_activity);

    }
}