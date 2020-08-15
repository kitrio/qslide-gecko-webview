package com.jw.studio.geckodevmaster;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.jw.studio.geckodevmaster.session.TabSessionManager;

import androidx.constraintlayout.widget.ConstraintLayout;

public class ToolbarLayout extends ConstraintLayout {

    public interface TabListener {
        void switchToTab(int tabId);
        void onBrowserActionClick();
    }

    private LocationView mLocationView;
    private Button mTabsCountButton;
    private View mBrowserAction;
    private TabListener mTabListener;
    private TabSessionManager mSessionManager;


    public ToolbarLayout(Context context, TabSessionManager sessionManager) {
        super(context);
        mSessionManager = sessionManager;
        initView();
    }

    private void initView() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.toolbar_layout, this, true);
        mLocationView = findViewById(R.id.locationView);
        mBrowserAction = getBrowserAction();
        addView(mBrowserAction);
        mTabsCountButton = findViewById(R.id.tabs_button);
        mTabsCountButton.setOnClickListener(this::onTabButtonClicked);
    }

    public LocationView getLocationView() {
        return mLocationView;
    }

    private View getBrowserAction() {
        View browserAction = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.browser_action, this, false);
        browserAction.setVisibility(GONE);
        return browserAction;
    }

    public void setBrowserActionButton(ActionButton button) {
        if (button == null) {
            mBrowserAction.setVisibility(GONE);
            return;
        }

        BitmapDrawable drawable = new BitmapDrawable(getContext().getResources(), button.icon);
        ImageView view = mBrowserAction.findViewById(R.id.browser_action_icon);
        view.setOnClickListener(this::onBrowserActionButtonClicked);
        view.setBackground(drawable);

        TextView badge = mBrowserAction.findViewById(R.id.browser_action_badge);
        if (button.text != null && !button.text.equals("")) {
            if (button.backgroundColor != null) {
                GradientDrawable backgroundDrawable = ((GradientDrawable) badge.getBackground().mutate());
                backgroundDrawable.setColor(button.backgroundColor);
                backgroundDrawable.invalidateSelf();
            }
            if (button.textColor != null) {
                badge.setTextColor(button.textColor);
            }
            badge.setText(button.text);
            badge.setVisibility(VISIBLE);
        } else {
            badge.setVisibility(GONE);
        }

        mBrowserAction.setVisibility(VISIBLE);
    }

    public void onBrowserActionButtonClicked(View view) {
        mTabListener.onBrowserActionClick();
    }

    public void setTabListener(TabListener listener) {
        this.mTabListener = listener;
    }

    public void updateTabCount() {
        mTabsCountButton.setText(String.valueOf(mSessionManager.sessionCount()));
    }

    public void onTabButtonClicked(View view) {
        PopupMenu tabButtonMenu = new PopupMenu(view.getContext(), mTabsCountButton);
        for (int idx = 0; idx < mSessionManager.sessionCount(); ++idx) {
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
