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

public class WebExtensionManager implements WebExtension.ActionDelegate, WebExtension.SessionTabDelegate,
        WebExtension.TabDelegate,
        WebExtensionController.PromptDelegate,
        WebExtensionController.DebuggerDelegate,
        TabSessionManager.TabObserver {
    public WebExtension extension;

    private LruCache<WebExtension.Icon, Bitmap> bitmapCache = new LruCache<>(5);
    private GeckoRuntime runtime;
    private WebExtension.Action defaultAction;
    private WeakReference<WebExtensionDelegate> extensionDelegate;
    private TabSessionManager tabSessionManager;

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onInstallPrompt(final @NonNull WebExtension extension) {
        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
    }

    @Override
    public void onExtensionListUpdated() {
        refreshExtensionList();
    }

    // We only support either one browserAction or one pageAction
    private void onAction(final WebExtension extension, final GeckoSession session,
                          final WebExtension.Action action) {
        WebExtensionDelegate delegate = extensionDelegate.get();
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
    public GeckoResult<GeckoSession> onNewTab(@NonNull WebExtension webExtension, @NonNull WebExtension.CreateTabDetails createTabDetails) {
        WebExtensionDelegate delegate = extensionDelegate.get();
        if (delegate == null) {
            return GeckoResult.fromValue(null);
        }
        return GeckoResult.fromValue(delegate.openNewTab(createTabDetails));
    }

    @Override
    public GeckoResult<AllowOrDeny> onCloseTab(WebExtension extension, GeckoSession session) {
        final WebExtensionDelegate delegate = extensionDelegate.get();
        if (delegate == null) {
            return GeckoResult.fromValue(AllowOrDeny.DENY);
        }

        final TabSession tabSession = tabSessionManager.getSession(session);
        if (tabSession != null) {
            delegate.closeTab(tabSession);
        }

        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
    }

    @Override
    public GeckoResult<AllowOrDeny> onUpdateTab(WebExtension extension,
                                                GeckoSession session,
                                                WebExtension.UpdateTabDetails updateDetails) {
        final WebExtensionDelegate delegate = extensionDelegate.get();
        if (delegate == null) {
            return GeckoResult.fromValue(AllowOrDeny.DENY);
        }

        final TabSession tabSession = tabSessionManager.getSession(session);
        if (tabSession != null) {
            delegate.updateTab(tabSession, updateDetails);
        }

        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
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
        WebExtensionDelegate extensionDelegate = this.extensionDelegate.get();
        if (extensionDelegate == null) {
            return null;
        }

        GeckoSession session = extensionDelegate.toggleBrowserActionPopup(false);
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
        WebExtensionDelegate extensionDelegate = this.extensionDelegate.get();
        if (extensionDelegate == null) {
            return;
        }

        if (resolved == null || resolved.enabled == null || !resolved.enabled) {
            extensionDelegate.onActionButton(null);
            return;
        }

        if (resolved.icon != null) {
            if (bitmapCache.get(resolved.icon) != null) {
                extensionDelegate.onActionButton(new ActionButton(
                        bitmapCache.get(resolved.icon), resolved.badgeText,
                        resolved.badgeTextColor,
                        resolved.badgeBackgroundColor
                ));
            } else {
                resolved.icon.get(100).accept(bitmap -> {
                    bitmapCache.put(resolved.icon, bitmap);
                    extensionDelegate.onActionButton(new ActionButton(
                            bitmap, resolved.badgeText,
                            resolved.badgeTextColor,
                            resolved.badgeBackgroundColor));
                });
            }
        } else {
            extensionDelegate.onActionButton(null);
        }
    }

    public void onClicked(TabSession session) {
        WebExtension.Action action = actionFor(session);
        if (action != null) {
            action.click();
        }
    }

    public void setExtensionDelegate(WebExtensionDelegate delegate) {
        extensionDelegate = new WeakReference<>(delegate);
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

    public GeckoResult<Void> unregisterExtension() {
        if (extension == null) {
            return GeckoResult.fromValue(null);
        }

        tabSessionManager.unregisterWebExtension();

        return runtime.getWebExtensionController().uninstall(extension).accept((unused) -> {
            extension = null;
            defaultAction = null;
            updateAction(null);
        });
    }

    public void registerExtension(WebExtension extension) {
        extension.setActionDelegate(this);
        extension.setTabDelegate(this);
        tabSessionManager.setWebExtensionActionDelegate(extension, this, this);
        this.extension = extension;
    }

    private void refreshExtensionList() {
        runtime.getWebExtensionController()
                .list().accept(extensions -> {
            for (final WebExtension extension : extensions) {
                registerExtension(extension);
            }
        });
    }

    public WebExtensionManager(GeckoRuntime runtime,
                               TabSessionManager tabManager) {
        tabSessionManager = tabManager;
        this.runtime = runtime;
    }
}
