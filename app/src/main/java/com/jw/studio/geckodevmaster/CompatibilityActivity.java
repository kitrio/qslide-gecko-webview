package com.jw.studio.geckodevmaster;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class CompatibilityActivity extends Activity {
    public String MANUFACTURE;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        MANUFACTURE = Build.MANUFACTURER;

        if(MANUFACTURE.contains("LGE")){
            Log.d("phone MANUFACTURE="+MANUFACTURE,"intent start");
            Intent intent = new Intent(getApplicationContext(), GeckoViewActivity.class);
            intent.putExtra("com.lge.app.floating.launchAsFloating", true);
            startActivity(intent);
        }else{
            Log.d("phone MANUFACTURE"+MANUFACTURE,"not LG");

        }setContentView(R.layout.compatibility_activity);

    }
}