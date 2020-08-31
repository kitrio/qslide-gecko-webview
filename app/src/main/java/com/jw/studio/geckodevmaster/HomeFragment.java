package com.jw.studio.geckodevmaster;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class HomeFragment extends Fragment {

    private GeckoViewActivity activity;
    private FragmentManager fragmentManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof GeckoViewActivity) {
            activity = (GeckoViewActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = activity.getFragmentManager();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.home_fragment, container, false);
        Button googleBtn = rootView.findViewById(R.id.button_google);
        Button youtubeBtn = rootView.findViewById(R.id.button_youtube);
        Button localBtn = rootView.findViewById(R.id.button_localurl);
        Button facebookBtn = rootView.findViewById(R.id.button_facebook);
        Button instagramBtn =rootView.findViewById(R.id.button_instagram);
        Button twitterBtn =rootView.findViewById(R.id.button_twitter);
        Drawable topDrawable;

        googleBtn.setText("Google");
        googleBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri("https://www.google.com");
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });

        youtubeBtn.setText("Youtube");
        youtubeBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri("https://m.youtube.com");
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });

        if (Locale.getDefault().getLanguage().equals("ko")) {
            topDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.fav_naver, null);
        } else if(Locale.getDefault().getLanguage().equals("ja")){
            topDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.fav_yahoo, null);
        } else {
            topDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.fav_duckduckgo, null);
        }
        topDrawable.setBounds(0, -5, 0, 0);
        localBtn.setCompoundDrawablesWithIntrinsicBounds(null, topDrawable, null, null);
        localBtn.setText(getString(R.string.urlname));
        localBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri(getString(R.string.initurl));
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });

        facebookBtn.setText(getString(R.string.facebook));
        facebookBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri("https://m.facebook.com");
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });

        instagramBtn.setText(getString(R.string.instagram));
        instagramBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri("https://www.instagram.com");
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });

        twitterBtn.setText(getString(R.string.twitter));
        twitterBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri("https://mobile.twitter.com");
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });


        return rootView;
    }

}
