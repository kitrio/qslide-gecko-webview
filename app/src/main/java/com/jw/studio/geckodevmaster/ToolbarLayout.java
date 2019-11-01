package com.jw.studio.geckodevmaster;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

public class ToolbarLayout extends LinearLayout {

    public interface TabListener {
        void switchToTab(int tabId);
    }

    private LocationView mLocationView;
    private Button mTabsCountButton;
    private TabListener mTabListener;
    private TabSessionManager mSessionManager;
    private ImageButton mBackButtoon;

    public ToolbarLayout(Context context, TabSessionManager sessionManager) {
        super(context);
        mSessionManager = sessionManager;
        initView();
    }

    private void initView() {

        setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
        setOrientation(LinearLayout.HORIZONTAL);

        mBackButtoon = new ImageButton(getContext());
        mBackButtoon.setId(R.id.back_button);
        mBackButtoon.setImageResource(R.drawable.ic_toolbar);
        mBackButtoon.setBackgroundColor(Color.TRANSPARENT);
        mBackButtoon.setPadding(0,0,14,0);
        mBackButtoon.setLayoutParams(new LayoutParams(100, LayoutParams.MATCH_PARENT));
        addView(mBackButtoon);

        mLocationView = new LocationView(getContext());
        mLocationView.setId(R.id.url_bar);
        mLocationView.setBackgroundColor(Color.TRANSPARENT);
        mLocationView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,1));
        addView(mLocationView);

        mTabsCountButton = new Button(getContext());
        mTabsCountButton.setLayoutParams(new LayoutParams(100, LayoutParams.MATCH_PARENT));
        mTabsCountButton.setId(R.id.tabs_button);
        mTabsCountButton.setOnClickListener(this::onTabButtonClicked);
        mTabsCountButton.setBackgroundResource(R.drawable.tab_number_background);
        addView(mTabsCountButton);

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
