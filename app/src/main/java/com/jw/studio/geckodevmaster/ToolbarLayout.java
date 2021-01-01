package com.jw.studio.geckodevmaster;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
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

    private LocationView locationView;
    private Button tabsCountButton;
    private View browserAction;
    private TabListener tabListener;
    private TabSessionManager sessionManager;


    public ToolbarLayout(Context context, TabSessionManager sessionManager) {
        super(context);
        this.sessionManager = sessionManager;
        initView();
    }

    public ToolbarLayout(Context context, AttributeSet attributes) {
        super(context, attributes);
        initView();
    }

    private void initView() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.toolbar_layout, this, true);
        locationView = findViewById(R.id.locationView);
        browserAction = getBrowserAction();
        addView(browserAction);
        tabsCountButton = findViewById(R.id.tabs_button);
        tabsCountButton.setOnClickListener(this::onTabButtonClicked);
    }

    public LocationView getLocationView() {
        return locationView;
    }

    private View getBrowserAction() {
        View browserAction = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.browser_action, this, false);
        browserAction.setVisibility(GONE);
        return browserAction;
    }

    public void setBrowserActionButton(ActionButton button) {
        if (button == null) {
            browserAction.setVisibility(GONE);
            return;
        }

        BitmapDrawable drawable = new BitmapDrawable(getContext().getResources(), button.icon);
        ImageView view = browserAction.findViewById(R.id.browser_action_icon);
        view.setOnClickListener(this::onBrowserActionButtonClicked);
        view.setBackground(drawable);

        TextView badge = browserAction.findViewById(R.id.browser_action_badge);
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

        browserAction.setVisibility(VISIBLE);
    }

    public void onBrowserActionButtonClicked(View view) {
        tabListener.onBrowserActionClick();
    }

    public void setTabListener(TabListener listener) {
        this.tabListener = listener;
    }

    public void updateTabCount() {
        tabsCountButton.setText(String.valueOf(sessionManager.sessionCount()));
    }

    public void onTabButtonClicked(View view) {
        PopupMenu tabButtonMenu = new PopupMenu(view.getContext(), view);
        for (int idx = 0; idx < sessionManager.sessionCount(); ++idx) {
            tabButtonMenu.getMenu().add(0, idx, idx,
                    sessionManager.getSession(idx).getTitle());
        }
        tabButtonMenu.setOnMenuItemClickListener(item -> {
            tabListener.switchToTab(item.getItemId());
            return true;
        });
        tabButtonMenu.show();
    }

}
