package com.jw.studio.geckodevmaster.session;

import android.util.Log;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.WebExtension;

import java.util.LinkedList;

import androidx.annotation.Nullable;

public class TabSessionManager {
    private static LinkedList<TabSession> tabSessions = new LinkedList<>();
    private int currentSessionIndex = 0;
    private TabObserver tabObserver;

    public interface TabObserver {
        void onCurrentSession(TabSession session);
    }

    public TabSessionManager() {
    }

    public void unregisterWebExtension() {
        for (final TabSession session : tabSessions) {
            session.action = null;
        }
    }

    public void setWebExtensionActionDelegate(WebExtension extension,
                                              WebExtension.ActionDelegate delegate) {
        for (final TabSession session : tabSessions) {
            session.setWebExtensionActionDelegate(extension, delegate);
        }
    }

    public void setTabObserver(TabObserver observer) {
        tabObserver = observer;
    }

    public void addSession(TabSession session) {
        tabSessions.add(session);
    }

    public TabSession getSession(int index) {
        Log.d("tabsize geckoview","size:"+ tabSessions.size() + " index:"+ index);
        if((tabSessions.size()) <= index){
            return  tabSessions.get(--index);
        }
        return tabSessions.get(index);
    }

    public TabSession getCurrentSession() {
        return getSession(currentSessionIndex);
    }

    public TabSession getSession(GeckoSession session) {
        int index = tabSessions.indexOf(session);
        if (index == -1) {
            return null;
        }
        return getSession(index);
    }

    public void setCurrentSession(TabSession session) {
        int index = tabSessions.indexOf(session);
        if (index == -1) {
            tabSessions.add(session);
            index = tabSessions.size() - 1;
        }
        currentSessionIndex = index;

        if(tabObserver != null) {
            tabObserver.onCurrentSession(session);
        }
    }

    private boolean isCurrentSession(TabSession session) {
        return session == getCurrentSession();
    }

    public void closeSession(@Nullable TabSession session) {
        if (session == null) { return; }
        if (isCurrentSession(session)
                && currentSessionIndex == tabSessions.size() - 1) {
            --currentSessionIndex;
        }
        session.close();
        tabSessions.remove(session);
    }

    public TabSession newSession(GeckoSessionSettings settings) {
        TabSession tabSession = new TabSession(settings);
        tabSessions.add(tabSession);
        return tabSession;
    }

    public int sessionCount() {
        return tabSessions.size();
    }
}