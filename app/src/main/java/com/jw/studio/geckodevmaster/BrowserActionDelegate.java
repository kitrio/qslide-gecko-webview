package com.jw.studio.geckodevmaster;

import com.jw.studio.geckodevmaster.session.TabSession;

import org.mozilla.geckoview.GeckoSession;

public interface BrowserActionDelegate {
    default GeckoSession toggleBrowserActionPopup(boolean force) {
        return null;
    }

    default void onActionButton(ActionButton button) {
    }

    default TabSession getSession(GeckoSession session) {
        return null;
    }

    default TabSession getCurrentSession() {
        return null;
    }
}
