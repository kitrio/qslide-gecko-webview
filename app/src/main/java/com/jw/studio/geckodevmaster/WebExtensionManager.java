package com.jw.studio.geckodevmaster;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.jw.studio.geckodevmaster.session.TabSession;
import com.jw.studio.geckodevmaster.session.TabSessionManager;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

interface BrowserActionDelegate {
    default GeckoSession toggleBrowserActionPopup(boolean force) {
        return null;
    }
    default void onActionButton(ActionButton button) {}
    default TabSession getSession(GeckoSession session) {
        return null;
    }
    default TabSession getCurrentSession() {
        return null;
    }
}
public class WebExtensionManager implements WebExtension.ActionDelegate, WebExtensionController.PromptDelegate,
        TabSessionManager.TabObserver {
    public WebExtension extension;

    private LruCache<WebExtension.Icon, Bitmap> mBitmapCache = new LruCache<>(5);
    private GeckoRuntime mRuntime;
    private WebExtension.Action mDefaultAction;

    private WeakReference<BrowserActionDelegate> mActionDelegate;

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onInstallPrompt(final @NonNull WebExtension extension) {
        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
    }

    // We only support either one browserAction or one pageAction
    private void onAction(final WebExtension extension, final GeckoSession session,
                          final WebExtension.Action action) {
        BrowserActionDelegate delegate = mActionDelegate.get();
        if (delegate == null) {
            return;
        }

        WebExtension.Action resolved;

        if (session == null) {
            // This is the default action
            mDefaultAction = action;
            resolved = actionFor(delegate.getCurrentSession());
        } else {
            if (delegate.getSession(session) == null) {
                return;
            }
            delegate.getSession(session).action = action;
            if (delegate.getCurrentSession() != session) {
                // This update is not for the session that we are currently displaying,
                // no need to update the UI
                return;
            }
            resolved = action.withDefault(mDefaultAction);
        }

        updateAction(resolved);
    }

    @Override
    public void onPageAction(final WebExtension extension,
                             final GeckoSession session,
                             final WebExtension.Action action) {
        onAction(extension, session, action);
    }

    @Override
    public void onBrowserAction(final WebExtension extension,
                                final GeckoSession session,
                                final WebExtension.Action action) {
        onAction(extension, session, action);
    }

    private GeckoResult<GeckoSession> togglePopup(boolean force) {
        BrowserActionDelegate actionDelegate = mActionDelegate.get();
        if (actionDelegate == null) {
            return null;
        }

        GeckoSession session = actionDelegate.toggleBrowserActionPopup(false);
        if (session == null) {
            return null;
        }

        return GeckoResult.fromValue(session);
    }

    @Override
    public GeckoResult<GeckoSession> onTogglePopup(final @NonNull WebExtension extension,
                                                   final @NonNull WebExtension.Action action) {
        return togglePopup(false);
    }

    @Override
    public GeckoResult<GeckoSession> onOpenPopup(final @NonNull WebExtension extension,
                                                 final @NonNull WebExtension.Action action) {
        return togglePopup(true);
    }

    private WebExtension.Action actionFor(TabSession session) {
        if (session.action == null) {
            return mDefaultAction;
        } else {
            return session.action.withDefault(mDefaultAction);
        }
    }

    private void updateAction(WebExtension.Action resolved) {
        BrowserActionDelegate actionDelegate = mActionDelegate.get();
        if (actionDelegate == null) {
            return;
        }

        if (resolved == null || resolved.enabled == null || !resolved.enabled) {
            actionDelegate.onActionButton(null);
            return;
        }

        if (resolved.icon != null) {
            if (mBitmapCache.get(resolved.icon) != null) {
                actionDelegate.onActionButton(new ActionButton(
                        mBitmapCache.get(resolved.icon), resolved.badgeText,
                        resolved.badgeTextColor,
                        resolved.badgeBackgroundColor
                ));
            } else {
                resolved.icon.get(100).accept(bitmap -> {
                    mBitmapCache.put(resolved.icon, bitmap);
                    actionDelegate.onActionButton(new ActionButton(
                            bitmap, resolved.badgeText,
                            resolved.badgeTextColor,
                            resolved.badgeBackgroundColor));
                });
            }
        } else {
            actionDelegate.onActionButton(null);
        }
    }

    public void onClicked(TabSession session) {
        WebExtension.Action action = actionFor(session);
        if (action != null) {
            action.click();
        }
    }

    public void setActionDelegate(BrowserActionDelegate delegate) {
        mActionDelegate = new WeakReference<>(delegate);
    }

    @Override
    public void onCurrentSession(TabSession session) {
        if (mDefaultAction == null) {
            // No action was ever defined, so nothing to do
            return;
        }

        if (session.action != null) {
            updateAction(session.action.withDefault(mDefaultAction));
        } else {
            updateAction(mDefaultAction);
        }
    }

    public GeckoResult<Void> unregisterExtension(TabSessionManager tabManager) {
        if (extension == null) {
            return GeckoResult.fromValue(null);
        }

        tabManager.unregisterWebExtension();

        return mRuntime.getWebExtensionController().uninstall(extension).accept((unused) -> {
            extension = null;
            mDefaultAction = null;
            updateAction(null);
        });
    }

    public void registerExtension(WebExtension extension,
                                  TabSessionManager tabManager) {
        extension.setActionDelegate(this);
        tabManager.setWebExtensionActionDelegate(extension, this);
        this.extension = extension;
    }

    public WebExtensionManager(GeckoRuntime runtime,
                               TabSessionManager tabManager) {
        runtime.getWebExtensionController()
                .list().accept(extensions -> {
            for (final WebExtension extension : extensions) {
                registerExtension(extension, tabManager);
            }
        });
        mRuntime = runtime;
    }
}
