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

public class WebExtensionManager implements WebExtension.ActionDelegate, WebExtensionController.PromptDelegate,
        TabSessionManager.TabObserver {
    public WebExtension extension;

    private LruCache<WebExtension.Icon, Bitmap> bitmapCache = new LruCache<>(5);
    private GeckoRuntime runtime;
    private WebExtension.Action defaultAction;
    private WeakReference<BrowserActionDelegate> actionDelegate;

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onInstallPrompt(final @NonNull WebExtension extension) {
        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
    }

    // We only support either one browserAction or one pageAction
    private void onAction(final WebExtension extension, final GeckoSession session,
                          final WebExtension.Action action) {
        BrowserActionDelegate delegate = actionDelegate.get();
        if (delegate == null) {
            return;
        }

        WebExtension.Action resolved;

        if (session == null) {
            // This is the default action
            defaultAction = action;
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
            resolved = action.withDefault(defaultAction);
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
        BrowserActionDelegate actionDelegate = this.actionDelegate.get();
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
            return defaultAction;
        } else {
            return session.action.withDefault(defaultAction);
        }
    }

    private void updateAction(WebExtension.Action resolved) {
        BrowserActionDelegate actionDelegate = this.actionDelegate.get();
        if (actionDelegate == null) {
            return;
        }

        if (resolved == null || resolved.enabled == null || !resolved.enabled) {
            actionDelegate.onActionButton(null);
            return;
        }

        if (resolved.icon != null) {
            if (bitmapCache.get(resolved.icon) != null) {
                actionDelegate.onActionButton(new ActionButton(
                        bitmapCache.get(resolved.icon), resolved.badgeText,
                        resolved.badgeTextColor,
                        resolved.badgeBackgroundColor
                ));
            } else {
                resolved.icon.get(100).accept(bitmap -> {
                    bitmapCache.put(resolved.icon, bitmap);
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
        actionDelegate = new WeakReference<>(delegate);
    }

    @Override
    public void onCurrentSession(TabSession session) {
        if (defaultAction == null) {
            // No action was ever defined, so nothing to do
            return;
        }

        if (session.action != null) {
            updateAction(session.action.withDefault(defaultAction));
        } else {
            updateAction(defaultAction);
        }
    }

    public GeckoResult<Void> unregisterExtension(TabSessionManager tabManager) {
        if (extension == null) {
            return GeckoResult.fromValue(null);
        }

        tabManager.unregisterWebExtension();

        return runtime.getWebExtensionController().uninstall(extension).accept((unused) -> {
            extension = null;
            defaultAction = null;
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
        this.runtime = runtime;
    }
}
