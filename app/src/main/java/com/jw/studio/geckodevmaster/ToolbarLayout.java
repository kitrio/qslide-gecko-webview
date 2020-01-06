package com.jw.studio.geckodevmaster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

import androidx.constraintlayout.widget.ConstraintLayout;

public class ToolbarLayout extends ConstraintLayout {

    public interface TabListener {
        void switchToTab(int tabId);
    }

    private LocationView mLocationView;
    private Button mTabsCountButton;
    private TabListener mTabListener;
    private TabSessionManager mSessionManager;


    public ToolbarLayout(Context context, TabSessionManager sessionManager) {
        super(context);
        mSessionManager = sessionManager;
        initView();
    }

    private void initView() {
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.toolbar_layout,this,true);
        mLocationView = findViewById(R.id.locationView);
        mTabsCountButton = findViewById(R.id.tabs_button);
        mTabsCountButton.setOnClickListener(this::onTabButtonClicked);
    }

    public LocationView getLocationView() {
        return mLocationView;
    }

    public void setTabListener(TabListener listener) {
        this.mTabListener = listener;
    }

    public void updateTabCount() {
        mTabsCountButton.setText(String.valueOf(mSessionManager.sessionCount()));
    }

    public void onTabButtonClicked(View view) {
        PopupMenu tabButtonMenu = new PopupMenu(view.getContext(), mTabsCountButton);
        for(int idx = 0; idx < mSessionManager.sessionCount(); ++idx) {
            tabButtonMenu.getMenu().add(0, idx, idx,
                    mSessionManager.getSession(idx).getTitle());
        }
        tabButtonMenu.setOnMenuItemClickListener(item -> {
            mTabListener.switchToTab(item.getItemId());
            return true;
        });
        tabButtonMenu.show();
    }

}
