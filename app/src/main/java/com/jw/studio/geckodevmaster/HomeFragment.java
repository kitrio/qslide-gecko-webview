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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.Locale;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private GeckoViewActivity activity;
    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        activity = (GeckoViewActivity)getActivity();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.home_fragment , container, false);
        Button googleBtn = rootView.findViewById(R.id.button_google);
        Button youtubeBtn = rootView.findViewById(R.id.button_youtube);
        Button localBtn = rootView.findViewById(R.id.button_localurl);

        FragmentManager fragmentManager = activity.getFragmentManager();

        googleBtn.setText("Google");
        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.mTabSessionManager.getCurrentSession().loadUri("https://google.com");
                fragmentManager.beginTransaction().remove(HomeFragment.this).commitAllowingStateLoss();
            }
        });

        youtubeBtn.setText("Youtube");
        youtubeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.mTabSessionManager.getCurrentSession().loadUri("https://m.youtube.com");
                fragmentManager.beginTransaction().remove(HomeFragment.this).commitAllowingStateLoss();
            }
        });

        Drawable topdrawable;
        if (Locale.getDefault().getLanguage().equals("ko")) {
            topdrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.fav_naver, null);
        }else{
            topdrawable = ResourcesCompat.getDrawable(getResources(),R.drawable.fav_duckduckgo,null);
        }
        Objects.requireNonNull(topdrawable).setBounds(0,-5,0,0);
        localBtn.setCompoundDrawablesWithIntrinsicBounds(null,topdrawable,null,null);
        localBtn.setText(getString(R.string.urlname));
        localBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.mTabSessionManager.getCurrentSession().loadUri(getString(R.string.initurl));
                fragmentManager.beginTransaction().remove(HomeFragment.this).commitAllowingStateLoss();
            }
        });
        return rootView;
    }

}
