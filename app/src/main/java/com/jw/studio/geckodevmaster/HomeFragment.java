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

import com.jw.studio.geckodevmaster.databinding.HomeFragmentBinding;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;

public class HomeFragment extends Fragment {

    private GeckoViewActivity activity;
    private FragmentManager fragmentManager;
    private HomeFragmentBinding homeFragmentBinding;

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
        homeFragmentBinding = DataBindingUtil.inflate(inflater,R.layout.home_fragment, container, false);
        View rootView = homeFragmentBinding.getRoot();
        Button googleBtn = homeFragmentBinding.buttonGoogle;
        Button youtubeBtn = homeFragmentBinding.buttonYoutube;
        Button localBtn = homeFragmentBinding.buttonLocalurl;
        Button facebookBtn = homeFragmentBinding.buttonFacebook;
        Button instagramBtn = homeFragmentBinding.buttonInstagram;
        Button twitterBtn = homeFragmentBinding.buttonTwitter;
        Drawable topDrawable;

        googleBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri("https://www.google.com");
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });

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
        localBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri(getString(R.string.initurl));
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });

        facebookBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri("https://m.facebook.com");
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });

        instagramBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri("https://www.instagram.com");
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });

        twitterBtn.setOnClickListener(v -> {
            activity.tabSessionManager.getCurrentSession().loadUri("https://mobile.twitter.com");
            fragmentManager.beginTransaction().hide(HomeFragment.this).commitAllowingStateLoss();
        });

        return rootView;
    }

}
