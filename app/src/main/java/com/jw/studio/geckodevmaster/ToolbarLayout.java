package com.jw.studio.geckodevmaster;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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

    public ToolbarLayout(Context context, TabSessionManager sessionManager) {
        super(context);
        mSessionManager = sessionManager;
        initView();
    }

    private void initView() {

        setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1.0f));
        setPadding(20,0,20,0);
        setOrientation(LinearLayout.HORIZONTAL);

        mLocationView = new LocationView(getContext());
        mLocationView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f));//notice
        mLocationView.setBackgroundColor(Color.TRANSPARENT);
        mLocationView.setPadding(20,0,10,0);
        mLocationView.setId(R.id.url_bar);
        addView(mLocationView);

        mTabsCountButton = getTabsCountButton();
        mTabsCountButton.setPadding(0,0,0,0);
        addView(mTabsCountButton);

    }

    private Button getTabsCountButton() {
        Button button = new Button(getContext());
        button.setLayoutParams(new LayoutParams(100, LayoutParams.WRAP_CONTENT));
        button.setPadding(10,10,10,10);
        button.setId(R.id.tabs_button);
        button.setOnClickListener(this::onTabButtonClicked);
        button.setBackgroundResource(R.drawable.tab_number_background);
        //button.setBackgroundColor(Color.TRANSPARENT);
        return button;
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
