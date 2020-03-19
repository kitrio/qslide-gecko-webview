package com.jw.studio.geckodevmaster;

import android.util.Log;

import androidx.annotation.Nullable;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import java.util.LinkedList;

public class TabSessionManager {
    private static LinkedList<TabSession> mTabSessions = new LinkedList<>();
    private int mCurrentSessionIndex = 0;
    private TabObserver mTabObserver;

    public interface TabObserver {
        void onCurrentSession(TabSession session);
    }

    public TabSessionManager() {
    }

    public void setTabObserver(TabObserver observer) {
        mTabObserver = observer;
    }

    public void addSession(TabSession session) {
        mTabSessions.add(session);
    }

    public TabSession getSession(int index) {
        Log.d("tabsize geckoview","size:"+mTabSessions.size() + " index:"+ index);
        if((mTabSessions.size()) <= index){
            return  mTabSessions.get(--index);
        }
        return mTabSessions.get(index);
    }

    public TabSession getCurrentSession() {
        return getSession(mCurrentSessionIndex);
    }

    public TabSession getSession(GeckoSession session) {
        int index = mTabSessions.indexOf(session);
        if (index == -1) {
            return null;
        }
        return getSession(index);
    }

    public void setCurrentSession(TabSession session) {
        int index = mTabSessions.indexOf(session);
        if (index == -1) {
            mTabSessions.add(session);
            index = mTabSessions.size() - 1;
        }
        mCurrentSessionIndex = index;

        if(mTabObserver != null) {
            mTabObserver.onCurrentSession(session);
        }
    }

    private boolean isCurrentSession(TabSession session) {
        return session == getCurrentSession();
    }

    public void closeSession(@Nullable TabSession session) {
        if (session == null) { return; }
        if (isCurrentSession(session)
                && mCurrentSessionIndex == mTabSessions.size() - 1) {
            --mCurrentSessionIndex;
        }
        session.close();
        mTabSessions.remove(session);
    }

    public TabSession newSession(GeckoSessionSettings settings) {
        TabSession tabSession = new TabSession(settings);
        mTabSessions.add(tabSession);
        return tabSession;
    }

    public int sessionCount() {
        return mTabSessions.size();
    }
}