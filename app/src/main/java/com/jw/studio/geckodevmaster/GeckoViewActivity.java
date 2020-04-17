/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.jw.studio.geckodevmaster;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.jw.studio.geckodevmaster.databinding.AppmenuPopupBinding;
import com.lge.app.floating.FloatableActivity;
import com.lge.app.floating.FloatingWindow;

import org.json.JSONObject;
import org.mozilla.gecko.util.ActivityUtils;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.GeckoWebExecutor;
import org.mozilla.geckoview.SlowScriptResponse;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;
import org.mozilla.geckoview.WebNotification;
import org.mozilla.geckoview.WebNotificationDelegate;
import org.mozilla.geckoview.WebRequest;
import org.mozilla.geckoview.WebRequestError;
import org.mozilla.geckoview.WebResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

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

class WebExtensionManager implements WebExtension.ActionDelegate, WebExtensionController.PromptDelegate,
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

public class GeckoViewActivity extends FloatableActivity implements ToolbarLayout.TabListener, BrowserActionDelegate {
    private static final String LOGTAG = "GeckoViewActivity";
    private static final String USE_MULTIPROCESS_EXTRA = "use_multiprocess";
    private static final String SEARCH_URI_BASE = "https://www.google.com/search?q=";
    private static final String CHANNEL_ID = "QwebviewChannel";
    private static final int REQUEST_FILE_PICKER = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    private static WebExtensionManager sExtensionManager;

    public TabSessionManager mTabSessionManager;
    private static GeckoRuntime sGeckoRuntime;
    private GeckoView mGeckoView;
    private boolean mUseTrackingProtection = true;
    private boolean mUsePrivateBrowsing;
    private boolean mKillProcessOnDestroy;
    private boolean mDesktopMode;
    private TabSession mPopupSession;
    private View mPopupView;

    private boolean mShowNotificationsRejected;
    private ArrayList<String> mAcceptedPersistentStorage = new ArrayList<>();

    private PopupWindow popupWindow;
    private ToolbarLayout mToolbarView;
    private String mCurrentUri;
    private boolean mCanGoBack;
    private static boolean mFullScreen;
    private HashMap<String, Integer> mNotificationIDMap = new HashMap<>();
    private HashMap<Integer, WebNotification> mNotificationMap = new HashMap<>();
    private int mLastID = 100;
    private Fragment homeFragment;
    private FragmentManager fragmentManager;

    private LinkedList<GeckoSession.WebResponseInfo> mPendingDownloads = new LinkedList<>();

    private final LocationView.CommitListener mCommitListener = new LocationView.CommitListener() {
        @Override
        public void onCommit(String text) {
            if ((text.contains(".") || text.contains(":")) && !text.contains(" ")) {
                mTabSessionManager.getCurrentSession().loadUri(text);
            } else {
                mTabSessionManager.getCurrentSession().loadUri(SEARCH_URI_BASE + text);
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            EditText urlEdit = findViewById(mToolbarView.getLocationView().getId());
            imm.hideSoftInputFromWindow(urlEdit.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
//            mGeckoView.requestFocus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOGTAG, "zerdatime " + SystemClock.elapsedRealtime() +
                " - application start");
        createNotificationChannel();
        setContentView(R.layout.geckoview_activity);
        mGeckoView = findViewById(R.id.gecko_view);
        ImageButton toolbar = findViewById(R.id.toolbar);
        ConstraintLayout appLayout = findViewById(R.id.main);

        mTabSessionManager = new TabSessionManager();
        mToolbarView = new ToolbarLayout(this, mTabSessionManager);
        mToolbarView.setId(R.id.toolbar_layout);
        mToolbarView.setTabListener(this);
        mToolbarView.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.WRAP_CONTENT));

        ConstraintSet set = new ConstraintSet();
        appLayout.addView(mToolbarView);
        set.clone(appLayout);
        set.connect(R.id.gecko_view, ConstraintSet.TOP, R.id.main, ConstraintSet.TOP);
        set.connect(R.id.gecko_view, ConstraintSet.BOTTOM, R.id.toolbar_layout, ConstraintSet.TOP);

        set.connect(R.id.toolbar, ConstraintSet.LEFT, R.id.toolbar_layout, ConstraintSet.RIGHT);
        set.connect(R.id.toolbar, ConstraintSet.RIGHT, R.id.main, ConstraintSet.RIGHT, 60);
        set.connect(R.id.toolbar, ConstraintSet.TOP, R.id.gecko_view, ConstraintSet.BOTTOM);
        set.connect(R.id.toolbar, ConstraintSet.BOTTOM, R.id.toolbar_layout, ConstraintSet.BOTTOM);

        set.connect(R.id.toolbar_layout, ConstraintSet.LEFT, R.id.main, ConstraintSet.LEFT);
        set.connect(R.id.toolbar_layout, ConstraintSet.RIGHT, R.id.toolbar, ConstraintSet.LEFT);
        set.connect(R.id.toolbar_layout, ConstraintSet.TOP, R.id.gecko_view, ConstraintSet.BOTTOM);
        set.connect(R.id.toolbar_layout, ConstraintSet.BOTTOM, R.id.main, ConstraintSet.BOTTOM);
        set.applyTo(appLayout);

        final boolean useMultiprocess = getIntent().getBooleanExtra(USE_MULTIPROCESS_EXTRA, true);
        
        if (sGeckoRuntime == null) {
            final GeckoRuntimeSettings.Builder runtimeSettingsBuilder = new GeckoRuntimeSettings.Builder();
            final Bundle extras = getIntent().getExtras();
            if (extras != null) {
                runtimeSettingsBuilder.extras(extras);
            }
            runtimeSettingsBuilder
                .useContentProcessHint(useMultiprocess)
                .remoteDebuggingEnabled(false)
                .consoleOutput(false)
                .debugLogging(false)
                .contentBlocking(new ContentBlocking.Settings.Builder()
                    .antiTracking(ContentBlocking.AntiTracking.DEFAULT |
                            ContentBlocking.AntiTracking.STP)
                    .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                    .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.DEFAULT)
                    .build())
                .aboutConfigEnabled(true);

            sGeckoRuntime = GeckoRuntime.create(this, runtimeSettingsBuilder.build());

            sGeckoRuntime.getWebExtensionController().setTabDelegate(new WebExtensionController.TabDelegate() {
                @Override
                public GeckoResult<GeckoSession> onNewTab(WebExtension source, String uri) {
                    final TabSession newSession = createSession();
                    mToolbarView.updateTabCount();
                    setGeckoViewSession(newSession);
                    return GeckoResult.fromValue(newSession);
                }

                @Override
                public GeckoResult<AllowOrDeny> onCloseTab(WebExtension source, GeckoSession session) {
                    TabSession tabSession = mTabSessionManager.getSession(session);
                    if (tabSession != null) {
                        closeTab(tabSession);
                    }
                    return GeckoResult.fromValue(AllowOrDeny.ALLOW);
                }
            });

            sExtensionManager = new WebExtensionManager(sGeckoRuntime, mTabSessionManager);
            mTabSessionManager.setTabObserver(sExtensionManager);

            sGeckoRuntime.setWebNotificationDelegate(new WebNotificationDelegate() {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);

                @Override
                public void onShowNotification(@NonNull WebNotification notification) {
                    Intent clickIntent = new Intent(GeckoViewActivity.this, GeckoViewActivity.class);
                    clickIntent.putExtra("onClick", notification.tag);
                    PendingIntent dismissIntent = PendingIntent.getActivity(GeckoViewActivity.this, mLastID, clickIntent, 0);

                    Notification.Builder builder = new Notification.Builder(GeckoViewActivity.this)
                            .setContentTitle(notification.title)
                            .setContentText(notification.text)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentIntent(dismissIntent)
                            .setAutoCancel(true);

                    mNotificationIDMap.put(notification.tag, mLastID);
                    mNotificationMap.put(mLastID, notification);

                    if (notification.imageUrl != null && notification.imageUrl.length() > 0) {
                        final GeckoWebExecutor executor = new GeckoWebExecutor(sGeckoRuntime);

                        GeckoResult<WebResponse> response = executor.fetch(
                                new WebRequest.Builder(notification.imageUrl)
                                        .addHeader("Accept", "image")
                                        .build());
                        response.accept(value -> {
                            Bitmap bitmap = BitmapFactory.decodeStream(value.body);
                            builder.setLargeIcon(Icon.createWithBitmap(bitmap));
                            notificationManager.notify(mLastID++, builder.build());
                        });
                    } else {
                        notificationManager.notify(mLastID++, builder.build());
                    }

                }

                @Override
                public void onCloseNotification(@NonNull WebNotification notification) {
                    if (mNotificationIDMap.containsKey(notification.tag)) {
                        int id = mNotificationIDMap.get(notification.tag);
                        notificationManager.cancel(id);
                        mNotificationMap.remove(id);
                        mNotificationIDMap.remove(notification.tag);
                    }
                }
            });

            sGeckoRuntime.setDelegate(() -> {
                mKillProcessOnDestroy = true;
                finish();
            });
        }
        sExtensionManager.setActionDelegate(this);

        if (savedInstanceState == null) {
            TabSession session = getIntent().getParcelableExtra("session");
            if (session != null) {
                connectSession(session);

                if (!session.isOpen()) {
                    session.open(sGeckoRuntime);
                }
                mTabSessionManager.addSession(session);
                session.open(sGeckoRuntime);
                setGeckoViewSession(session);
            } else {
                showHome();
                session = createSession();
                session.open(sGeckoRuntime);
                mTabSessionManager.setCurrentSession(session);
                mGeckoView.setSession(session);
                sGeckoRuntime.getWebExtensionController().setTabActive(session, true);
            }
            loadFromIntent(getIntent());
        }
        mToolbarView.updateTabCount();
        mToolbarView.getLocationView().setCommitListener(mCommitListener);

        toolbar.setOnClickListener((view) -> {
            AppmenuPopupBinding menu = DataBindingUtil.inflate(getLayoutInflater(), R.layout.appmenu_popup, null, false);
            GeckoSession session = mTabSessionManager.getCurrentSession();
            popupWindow = new PopupWindow(menu.getRoot(), ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.setFocusable(true);
            int menu_height = dpToPx(260);

            menu.newtabButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popupWindow.dismiss();
                    createNewTab();
                    showHome();
                }
            });
            menu.forwardButton.setOnClickListener((v -> {
                session.goForward();
            }));
            menu.refreshButton.setOnClickListener((v) -> {
                session.reload();
            });
            menu.closetabButton.setOnClickListener(v -> {
                popupWindow.dismiss();
                closeTab((TabSession) session);
                hideHome();
            });
            if (this.isSwitchingToFloatingMode()) {
                menu.buttonQslide.setVisibility(View.GONE);
            } else {
                menu.buttonQslide.setVisibility(View.VISIBLE);
                menu_height = dpToPx(288);
                menu.buttonQslide.setOnClickListener(v -> {
                    switchToFloatingMode();
                });
            }
            menu.switchDesktop.setChecked(mDesktopMode);
            menu.switchDesktop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mDesktopMode = !mDesktopMode;
                    updateDesktopMode(session);
                    session.reload();
                }
            });
            menu.switchPrivate.setChecked(mUsePrivateBrowsing);
            menu.switchPrivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mUsePrivateBrowsing = !mUsePrivateBrowsing;
                    recreateSession();
                }
            });
            menu.switchTracking.setChecked(mUseTrackingProtection);
            menu.switchTracking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mUseTrackingProtection = !mUseTrackingProtection;
                    updateTrackingProtection(session);
                    session.reload();
                }
            });
//            menu.extendsInstallButton.setOnClickListener(v -> {
//                installAddon() ;
//            });
            popupWindow.showAsDropDown(toolbar, toolbar.getHeight(), -menu_height, mGeckoView.getPaddingBottom());

        });
        ImageButton mBackButton = findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if (ContextCompat.checkSelfPermission(GeckoViewActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(GeckoViewActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        }

    }

    private void showHome() {
        homeFragment = new HomeFragment();
        fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        if (homeFragment.isAdded()) {
            fragmentTransaction.show(homeFragment);
        } else {
            fragmentTransaction.replace(R.id.gecko_view, homeFragment, "home_fragment").commitAllowingStateLoss();
        }
    }

    private void hideHome() {
        fragmentManager = getFragmentManager();
        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }
        if (homeFragment.isVisible()) {
            fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag("home_fragment")).commitAllowingStateLoss();
        }
    }

    @Override
    public TabSession getSession(GeckoSession session) {
        return  mTabSessionManager.getSession(session);
    }

    @Override
    public TabSession getCurrentSession() {
        return mTabSessionManager.getCurrentSession();
    }

    @Override
    public void onActionButton(ActionButton button) {
        mToolbarView.setBrowserActionButton(button);
    }

    @Override
    public GeckoSession toggleBrowserActionPopup(boolean force) {
        if (mPopupSession == null) {
            openPopupSession();
        }

        ViewGroup.LayoutParams params = mPopupView.getLayoutParams();
        boolean shouldShow = force || params.width == 0;
        setPopupVisibility(shouldShow);

        return shouldShow ? mPopupSession : null;
    }
    private void setPopupVisibility(boolean visible) {
        if (mPopupView == null) {
            return;
        }
        ViewGroup.LayoutParams params = mPopupView.getLayoutParams();

        if (visible) {
            params.height = 1100;
            params.width = 1200;
        } else {
            params.height = 0;
            params.width = 0;
        }
        mPopupView.setLayoutParams(params);
    }

    private void openPopupSession() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.browser_action_popup, null);
        GeckoView geckoView = mPopupView.findViewById(R.id.gecko_view_popup);
        geckoView.setViewBackend(GeckoView.BACKEND_TEXTURE_VIEW);
        mPopupSession = new TabSession();
        mPopupSession.open(sGeckoRuntime);
        geckoView.setSession(mPopupSession);

        mPopupView.setOnFocusChangeListener(this::hideBrowserAction);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0, 0);
        params.addRule(RelativeLayout.ABOVE, R.id.toolbar);
        mPopupView.setLayoutParams(params);
        mPopupView.setFocusable(true);
        ((ViewGroup) findViewById(R.id.main)).addView(mPopupView);
    }

    private void hideBrowserAction(View view, boolean hasFocus) {
        if (!hasFocus) {
            ViewGroup.LayoutParams params = mPopupView.getLayoutParams();
            params.height = 0;
            params.width = 0;
            mPopupView.setLayoutParams(params);
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.activity_label);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private TabSession createSession() {
        TabSession session = mTabSessionManager.newSession(new GeckoSessionSettings.Builder()
                .suspendMediaWhenInactive(true)
                .usePrivateMode(mUsePrivateBrowsing)
                .useTrackingProtection(mUseTrackingProtection)
                .viewportMode(mDesktopMode
                        ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                        : GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                .userAgentMode(mDesktopMode
                        ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                        : GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                .build());
        connectSession(session);

        return session;
    }

    private void connectSession(GeckoSession session) {
        session.setContentDelegate(new ContentDelegate());
        session.setHistoryDelegate(new HistoryDelegate());
        final ContentBlockingDelegate cb = new ContentBlockingDelegate();
        session.setContentBlockingDelegate(cb);
        session.setProgressDelegate(new ProgressDelegate(cb));
        session.setNavigationDelegate(new NavigationDelegate());
        final BasicGeckoViewPrompt prompt = new BasicGeckoViewPrompt(this);
        prompt.filePickerRequestCode = REQUEST_FILE_PICKER;
        session.setPromptDelegate(prompt);

        final PermissionDelegate permission = new PermissionDelegate();
        permission.androidPermissionRequestCode = REQUEST_PERMISSIONS;
        session.setPermissionDelegate(permission);
        session.setMediaDelegate(new MediaDelegate(this));
		
        if(sExtensionManager.extension != null) {
            session.setWebExtensionActionDelegate(sExtensionManager.extension, sExtensionManager);
        }
        updateTrackingProtection(session);
        updateDesktopMode(session);
    }

    private void recreateSession() {
        recreateSession(mTabSessionManager.getCurrentSession());
    }

    private void recreateSession(TabSession session) {
        if (session != null) {
            mTabSessionManager.closeSession(session);
        }
        session = createSession();
        session.open(sGeckoRuntime);
        mTabSessionManager.setCurrentSession(session);
        mGeckoView.setSession(session);
        sGeckoRuntime.getWebExtensionController().setTabActive(session, true);
        if (mCurrentUri != null) {
            session.loadUri(mCurrentUri);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && mGeckoView.getSession() !=  null) {
            mTabSessionManager.setCurrentSession((TabSession) mGeckoView.getSession());
            sGeckoRuntime.getWebExtensionController().setTabActive((mGeckoView.getSession()), true);
        } else {
            recreateSession();
        }
    }

    private void updateDesktopMode(GeckoSession session) {
        session.getSettings().setViewportMode(mDesktopMode
                ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                : GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        session.getSettings().setUserAgentMode(mDesktopMode
                ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                : GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
    }

    private void updateTrackingProtection(GeckoSession session) {
        session.getSettings().setUseTrackingProtection(mUseTrackingProtection);
        sGeckoRuntime.getSettings().getContentBlocking()
                .setStrictSocialTrackingProtection(mUseTrackingProtection);
    }

    @Override
    public void onBackPressed() {
        GeckoSession session = mTabSessionManager.getCurrentSession();
        if (mFullScreen && session != null) {
            session.exitFullScreen();
            return;
        }

        if (mCanGoBack && session != null) {
            session.goBack();
        }
    }

    public void createNewTab(String url) {
        TabSession newSession = createSession();
        newSession.open(sGeckoRuntime);
        setGeckoViewSession(newSession);
        newSession.loadUri(url);
        mToolbarView.updateTabCount();
    }

    private void createNewTab() {
        TabSession newSession = createSession();
        newSession.open(sGeckoRuntime);
        setGeckoViewSession(newSession);
        mToolbarView.updateTabCount();
    }

    public void closeTab(TabSession session) {
        if (mTabSessionManager.sessionCount() > 1) {
            mTabSessionManager.closeSession(session);
            TabSession tabSession = mTabSessionManager.getCurrentSession();
            setGeckoViewSession(tabSession);
            if (tabSession.getTitle().equals("about:blank")) {
                showHome();
            }
            mToolbarView.getLocationView().setText(tabSession.getUri());
            mToolbarView.updateTabCount();
        } else {
            recreateSession();
            showHome();
        }
    }

    public void onBrowserActionClick() {
        sExtensionManager.onClicked(mTabSessionManager.getCurrentSession());
    }
    public void switchToTab(int index) {
        hideHome();
        if (mTabSessionManager.sessionCount() <= 0) {
            createNewTab("about:blank");
        }
        TabSession currentSession = mTabSessionManager.getCurrentSession();
        TabSession nextSession = mTabSessionManager.getSession(index);

        if (nextSession != currentSession) {
            setGeckoViewSession(nextSession);
            mCurrentUri = nextSession.getUri();
            if (nextSession.getTitle().equals("about:blank")) {
                mCurrentUri = "about:blank";
                Log.d("geckoview location=", mCurrentUri);
                showHome();
            }
            mToolbarView.getLocationView().setText(nextSession.getUri());
        }
    }

    private void setGeckoViewSession(TabSession session) {
        final WebExtensionController controller = sGeckoRuntime.getWebExtensionController();
        final GeckoSession previousSession = mGeckoView.releaseSession();
        if (previousSession != null){
            controller.setTabActive(previousSession, false);
        }
        mGeckoView.setSession(session);
        controller.setTabActive(session, true);
        mTabSessionManager.setCurrentSession(session);
    }

//    private void isUrlIntent(String url){
//        if (url.startsWith("tel:")){
//            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
//        }
//        if (url.startsWith("mailto:")) {
//            startActivity(new Intent(Intent.EXTRA_EMAIL, Uri.parse(url)));
//        }
//        if (url.startsWith("intent://www.google.com/maps")) {
//            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//            mapIntent.setClassName("com.google.android.apps.maps","com.google.android.maps.MapsActivity");
//            if (mapIntent.resolveActivity(getPackageManager()) != null) {
//                startActivity(mapIntent);
//            }
//        }
//    }

    @Override
    public void onDestroy() {
        if (mKillProcessOnDestroy) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        super.onDestroy();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        if (intent.getBooleanExtra("com.lge.app.floating.returnFromFloating", false)) {
            Log.d("returnFromFloating", "false");
            return;
        }
        super.onNewIntent(intent);

        if (intent.hasExtra("onClick")) {
            int key = intent.getExtras().getInt("onClick");
            WebNotification notification = mNotificationMap.get(key);
            if (notification != null) {
                notification.click();
                mNotificationMap.remove(key);
            }
        }

        setIntent(intent);
        if (intent.getData() != null) {
            loadFromIntent(intent);
        }
    }


    private void loadFromIntent(final Intent intent) {
        final Uri uri = intent.getData();
        if (uri != null) {
            mTabSessionManager.getCurrentSession().loadUri(uri.toString());
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        if (requestCode == REQUEST_FILE_PICKER) {
            final BasicGeckoViewPrompt prompt = (BasicGeckoViewPrompt)
                    mTabSessionManager.getCurrentSession().getPromptDelegate();
            prompt.onFileCallbackResult(resultCode, data);
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String[] permissions,
                                           final int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            final PermissionDelegate permission = (PermissionDelegate)
                    mTabSessionManager.getCurrentSession().getPermissionDelegate();
            permission.onRequestPermissionsResult(permissions, grantResults);
        } else if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            continueDownloads();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void continueDownloads() {
        LinkedList<GeckoSession.WebResponseInfo> downloads = mPendingDownloads;
        mPendingDownloads = new LinkedList<>();

        for (GeckoSession.WebResponseInfo response : downloads) {
            downloadFile(response);
        }
    }

    private void downloadFile(GeckoSession.WebResponseInfo response) {
        mTabSessionManager.getCurrentSession()
                .getUserAgent()
                .accept(userAgent -> downloadFile(response, userAgent),
                        exception -> {
                            throw new IllegalStateException("Could not get UserAgent string.");
                        });
    }

    private void downloadFile(GeckoSession.WebResponseInfo response, String userAgent) {
        if (ContextCompat.checkSelfPermission(GeckoViewActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mPendingDownloads.add(response);
            ActivityCompat.requestPermissions(GeckoViewActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
            return;
        }

        final Uri uri = Uri.parse(response.uri);
        final String filename = response.filename != null ? response.filename : uri.getLastPathSegment();

        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request req = new DownloadManager.Request(uri);
        req.setMimeType(response.contentType);
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        req.addRequestHeader("User-Agent", userAgent);
        manager.enqueue(req);
    }


    private static class HistoryDelegate implements GeckoSession.HistoryDelegate {
        private final HashSet<String> mVisitedURLs;

        private HistoryDelegate() {
            mVisitedURLs = new HashSet<>();
        }

        @Override
        public GeckoResult<Boolean> onVisited(GeckoSession session, String url,
                                              String lastVisitedURL, int flags) {
//            Log.i(LOGTAG, "Visited URL: " + url);

            mVisitedURLs.add(url);
            return GeckoResult.fromValue(true);
        }

        @Override
        public GeckoResult<boolean[]> getVisited(GeckoSession session, String[] urls) {
            boolean[] visited = new boolean[urls.length];
            for (int i = 0; i < urls.length; i++) {
                visited[i] = mVisitedURLs.contains(urls[i]);
            }
            return GeckoResult.fromValue(visited);
        }

        @Override
        public void onHistoryStateChange(final GeckoSession session,
                                         final GeckoSession.HistoryDelegate.HistoryList state) {
            //Log.i(LOGTAG, "History state updated");
        }
    }

    private class ContentDelegate implements GeckoSession.ContentDelegate {
        @Override
        public void onTitleChange(GeckoSession session, String title) {
//            Log.i(LOGTAG, "Content title changed to " + title);
            TabSession tabSession = mTabSessionManager.getSession(session);
            if (tabSession != null) {
                tabSession.setTitle(title);
            }
        }

        @Override
        public void onFullScreen(final GeckoSession session, final boolean fullScreen) {
//            getWindow().setFlags(fullScreen ? WindowManager.LayoutParams.FLAG_FULLSCREEN : 0,
//                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mFullScreen = fullScreen;
            if (fullScreen) {
                ActivityUtils.setFullScreen(GeckoViewActivity.this, true);
                mToolbarView.setVisibility(ConstraintLayout.GONE);
//                Log.d("Geckoview", "Fullscreen in");
            } else {
                mToolbarView.setVisibility(ConstraintLayout.VISIBLE);
//                Log.d("Geckoview", "Fullscreen out");
            }
        }

        @Override
        public void onFocusRequest(final GeckoSession session) {
            Log.i(LOGTAG, "Content requesting focus");
        }

        @Override
        public void onCloseRequest(final GeckoSession session) {
            if (session == mTabSessionManager.getCurrentSession()) {
                finish();
            }
        }

        @Override
        public void onContextMenu(final GeckoSession session,
                                  int screenX, int screenY,
                                  final ContextElement element) {
//            Log.d(LOGTAG, "onContextMenu screenX=" + screenX +
//                    " screenY=" + screenY +
//                    " type=" + element.type +
//                    " linkUri=" + element.linkUri +
//                    " title=" + element.title +
//                    " alt=" + element.altText +
//                    " srcUri=" + element.srcUri);
            BasicGeckoViewPrompt.contextMenuPrompt(GeckoViewActivity.this, element);
        }

        @Override
        public void onExternalResponse(GeckoSession session, GeckoSession.WebResponseInfo response) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndTypeAndNormalize(Uri.parse(response.uri), response.contentType);
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                downloadFile(response);
            }
        }

        @Override
        public void onCrash(GeckoSession session) {
            Log.e(LOGTAG, "Crashed, reopening session");
            session.open(sGeckoRuntime);
        }

        @Override
        public void onFirstComposite(final GeckoSession session) {
//            Log.d(LOGTAG, "onFirstComposite");
        }

        @Override
        public void onWebAppManifest(final GeckoSession session, JSONObject manifest) {
//            Log.d(LOGTAG, "onWebAppManifest: " + manifest);
        }

        private boolean activeAlert = false;

        @Override
        public GeckoResult<SlowScriptResponse> onSlowScript(final GeckoSession geckoSession,
                                                            final String scriptFileName) {
            BasicGeckoViewPrompt prompt = (BasicGeckoViewPrompt) mTabSessionManager.getCurrentSession().getPromptDelegate();
            if (prompt != null) {
                GeckoResult<SlowScriptResponse> result = new GeckoResult<>();
                if (!activeAlert) {
                    activeAlert = true;
                    prompt.onSlowScriptPrompt(geckoSession, getString(R.string.slow_script), result);
                }
                return result.then(value -> {
                    activeAlert = false;
                    return GeckoResult.fromValue(value);
                });
            }
            return null;
        }
    }

    private class ProgressDelegate implements GeckoSession.ProgressDelegate {
        private ContentBlockingDelegate mCb;

        private ProgressDelegate(final ContentBlockingDelegate cb) {
            mCb = cb;
        }

        @Override
        public void onPageStart(GeckoSession session, String url) {
//            Log.i(LOGTAG, "Starting to load page at " + url);
//            Log.i(LOGTAG, "zerdatime " + SystemClock.elapsedRealtime() +
//                    " - page load start");
            mCb.clearCounters();
            if (!url.trim().equals("about:blank")) {
                hideHome();
            }
        }

        @Override
        public void onPageStop(GeckoSession session, boolean success) {
//            Log.i(LOGTAG, "Stopping page load " + (success ? "successfully" : "unsuccessfully"));
//            Log.i(LOGTAG, "zerdatime " + SystemClock.elapsedRealtime() +
//                    " - page load stop");
//            mCb.logCounters();
        }
    }

    private class PermissionDelegate implements GeckoSession.PermissionDelegate {

        public int androidPermissionRequestCode = 1;
        private Callback mCallback;

        class NotificationCallback implements GeckoSession.PermissionDelegate.Callback {
            private final GeckoSession.PermissionDelegate.Callback mCallback;

            NotificationCallback(final GeckoSession.PermissionDelegate.Callback callback) {
                mCallback = callback;
            }

            @Override
            public void reject() {
                mShowNotificationsRejected = true;
                mCallback.reject();
            }

            @Override
            public void grant() {
                mShowNotificationsRejected = false;
                mCallback.grant();
            }
        }

        class PersistentStorageCallback implements GeckoSession.PermissionDelegate.Callback {
            private final GeckoSession.PermissionDelegate.Callback mCallback;
            private final String mUri;

            PersistentStorageCallback(final GeckoSession.PermissionDelegate.Callback callback, String uri) {
                mCallback = callback;
                mUri = uri;
            }

            @Override
            public void reject() {
                mCallback.reject();
            }

            @Override
            public void grant() {
                mAcceptedPersistentStorage.add(mUri);
                mCallback.grant();
            }
        }

        public void onRequestPermissionsResult(final String[] permissions,
                                               final int[] grantResults) {
            if (mCallback == null) {
                return;
            }

            final Callback cb = mCallback;
            mCallback = null;
            for (final int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // At least one permission was not granted.
                    cb.reject();
                    return;
                }
            }
            cb.grant();
        }

        @Override
        public void onAndroidPermissionsRequest(final GeckoSession session, final String[] permissions,
                                                final Callback callback) {
            // requestPermissions was introduced in API 23.
            mCallback = callback;
            requestPermissions(permissions, androidPermissionRequestCode);
        }

        @Override
        public void onContentPermissionRequest(final GeckoSession session, final String uri,
                                               final int type, final Callback callback) {
            final int resId;
            Callback contentPermissionCallback = callback;
            if (PERMISSION_GEOLOCATION == type) {
                resId = R.string.request_geolocation;
            } else if (PERMISSION_DESKTOP_NOTIFICATION == type) {
                if (mShowNotificationsRejected) {
                    Log.w(LOGTAG, "Desktop notifications already denied by user.");
                    callback.reject();
                    return;
                }
                resId = R.string.request_notification;
                contentPermissionCallback = new NotificationCallback(callback);
            } else if (PERMISSION_PERSISTENT_STORAGE == type) {
                if (mAcceptedPersistentStorage.contains(uri)) {
                    Log.w(LOGTAG, "Persistent Storage for " + uri + " already granted by user.");
                    callback.grant();
                    return;
                }
                resId = R.string.request_storage;
                contentPermissionCallback = new PersistentStorageCallback(callback, uri);
            } else {
                Log.w(LOGTAG, "Unknown permission: " + type);
                callback.reject();
                return;
            }

            final String title = getString(resId, Uri.parse(uri).getAuthority());
            final BasicGeckoViewPrompt prompt = (BasicGeckoViewPrompt)
                    mTabSessionManager.getCurrentSession().getPromptDelegate();
            prompt.onPermissionPrompt(session, title, contentPermissionCallback);
        }

        private String[] normalizeMediaName(final MediaSource[] sources) {
            if (sources == null) {
                return null;
            }

            String[] res = new String[sources.length];
            for (int i = 0; i < sources.length; i++) {
                final int mediaSource = sources[i].source;
                final String name = sources[i].name;
                if (MediaSource.SOURCE_CAMERA == mediaSource) {
                    if (name.toLowerCase(Locale.ENGLISH).contains("front")) {
                        res[i] = getString(R.string.media_front_camera);
                    } else {
                        res[i] = getString(R.string.media_back_camera);
                    }
                } else if (!name.isEmpty()) {
                    res[i] = name;
                } else if (MediaSource.SOURCE_MICROPHONE == mediaSource) {
                    res[i] = getString(R.string.media_microphone);
                } else {
                    res[i] = getString(R.string.media_other);
                }
            }

            return res;
        }

        @Override
        public void onMediaPermissionRequest(final GeckoSession session, final String uri,
                                             final MediaSource[] video, final MediaSource[] audio,
                                             final MediaCallback callback) {
            // If we don't have device permissions at this point, just automatically reject the request
            // as we will have already have requested device permissions before getting to this point
            // and if we've reached here and we don't have permissions then that means that the user
            // denied them.
            if ((audio != null
                    && ContextCompat.checkSelfPermission(GeckoViewActivity.this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                    || (video != null
                    && ContextCompat.checkSelfPermission(GeckoViewActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
                callback.reject();
                return;
            }

            final String host = Uri.parse(uri).getAuthority();
            final String title;
            if (audio == null) {
                title = getString(R.string.request_video, host);
            } else if (video == null) {
                title = getString(R.string.request_audio, host);
            } else {
                title = getString(R.string.request_media, host);
            }

            String[] videoNames = normalizeMediaName(video);
            String[] audioNames = normalizeMediaName(audio);

            final BasicGeckoViewPrompt prompt = (BasicGeckoViewPrompt)
                    mTabSessionManager.getCurrentSession().getPromptDelegate();
            prompt.onMediaPrompt(session, title, video, audio, videoNames, audioNames, callback);
        }
    }

    private class NavigationDelegate implements GeckoSession.NavigationDelegate {
        @Override
        public void onLocationChange(GeckoSession session, final String url) {
            TabSession tabSession = mTabSessionManager.getSession(session);
            if (tabSession != null ) {
                tabSession.onLocationChange(url);
            }
            mCurrentUri = url;
            mToolbarView.getLocationView().setText(mCurrentUri);
        }

        @Override
        public void onCanGoBack(GeckoSession session, boolean canGoBack) {
            mCanGoBack = canGoBack;
        }

        @Override
        public void onCanGoForward(GeckoSession session, boolean canGoForward) {
        }

        @Override
        public GeckoResult<AllowOrDeny> onLoadRequest(final GeckoSession session,
                                                      final LoadRequest request) {
//            Log.d(LOGTAG, "onLoadRequest=" + request.uri +
//                    " triggerUri=" + request.triggerUri +
//                    " where=" + request.target +
//                    " isRedirect=" + request.isRedirect);

            return GeckoResult.fromValue(AllowOrDeny.ALLOW);
        }

        @Override
        public GeckoResult<GeckoSession> onNewSession(final GeckoSession session, final String uri) {
            final TabSession newSession = createSession();
            mToolbarView.updateTabCount();
            setGeckoViewSession(newSession);
            // A reference to newSession is stored by mTabSessionManager,
            // which prevents the session from being garbage-collected.
            return GeckoResult.fromValue(newSession);
        }

        private String createErrorPage() {
            String errorPageTemplate;
            InputStream stream = null;
            BufferedReader reader = null;
            StringBuilder builder = new StringBuilder();
            try {
                if (Locale.getDefault().getLanguage().equals("ko")) {
                    stream = getResources().getAssets().open("error_ko.html");
                } else {
                    stream = getResources().getAssets().open("error_en.html");
                }
                reader = new BufferedReader(new InputStreamReader(stream));

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append("\n");
                }

                errorPageTemplate = builder.toString();
            } catch (IOException e) {
                Log.d(LOGTAG, "Failed to open error page template", e);
                return null;
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        Log.e(LOGTAG, "Failed to close error page template stream", e);
                    }
                }

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(LOGTAG, "Failed to close error page template reader", e);
                    }
                }
            }

            return errorPageTemplate;
        }

        @Override
        public GeckoResult<String> onLoadError(final GeckoSession session, final String uri,
                                               final WebRequestError error) {
//            Log.d(LOGTAG, "onLoadError=" + uri +
//                    " error category=" + error.category +
//                    " error=" + error.code);

            return GeckoResult.fromValue("data:text/html," + createErrorPage());
        }
    }

    private static class ContentBlockingDelegate
            implements ContentBlocking.Delegate {
        private int mBlockedAds = 0;
        private int mBlockedAnalytics = 0;
        private int mBlockedSocial = 0;
        private int mBlockedContent = 0;
        private int mBlockedTest = 0;
        private int mBlockedStp = 0;

        private void clearCounters() {
            mBlockedAds = 0;
            mBlockedAnalytics = 0;
            mBlockedSocial = 0;
            mBlockedContent = 0;
            mBlockedTest = 0;
            mBlockedStp = 0;
        }

        private void logCounters() {
//            Log.d(LOGTAG, "Trackers blocked: " + mBlockedAds + " ads, " +
//                    mBlockedAnalytics + " analytics, " +
//                    mBlockedSocial + " social, " +
//                    mBlockedContent + " content, " +
//                    mBlockedTest + " test, " +
//                    mBlockedStp + "stp");
        }

        @Override
        public void onContentBlocked(final GeckoSession session,
                                     final ContentBlocking.BlockEvent event) {
//            Log.d(LOGTAG, "onContentBlocked" +
//                    " AT: " + event.getAntiTrackingCategory() +
//                    " SB: " + event.getSafeBrowsingCategory() +
//                    " CB: " + event.getCookieBehaviorCategory() +
//                    " URI: " + event.uri);
//            if ((event.getAntiTrackingCategory() &
//                    ContentBlocking.AntiTracking.TEST) != 0) {
//                mBlockedTest++;
//            }
//            if ((event.getAntiTrackingCategory() &
//                    ContentBlocking.AntiTracking.AD) != 0) {
//                mBlockedAds++;
//            }
//            if ((event.getAntiTrackingCategory() &
//                    ContentBlocking.AntiTracking.ANALYTIC) != 0) {
//                mBlockedAnalytics++;
//            }
//            if ((event.getAntiTrackingCategory() &
//                    ContentBlocking.AntiTracking.SOCIAL) != 0) {
//                mBlockedSocial++;
//            }
//            if ((event.getAntiTrackingCategory() &
//                    ContentBlocking.AntiTracking.CONTENT) != 0) {
//                mBlockedContent++;
//            }
//            if ((event.getAntiTrackingCategory() &
//                    ContentBlocking.AntiTracking.STP) != 0) {
//                mBlockedStp++;
//            }
        }

        @Override
        public void onContentLoaded(final GeckoSession session,
                                    final ContentBlocking.BlockEvent event) {
//            Log.d(LOGTAG, "onContentLoaded" +
//                    " AT: " + event.getAntiTrackingCategory() +
//                    " SB: " + event.getSafeBrowsingCategory() +
//                    " CB: " + event.getCookieBehaviorCategory() +
//                    " URI: " + event.uri);
        }
    }

    private class MediaDelegate
            implements GeckoSession.MediaDelegate {
        private Integer mLastNotificationId = 100;
        private Integer mNotificationId;
        final private Activity mActivity;

        public MediaDelegate(Activity activity) {
            mActivity = activity;
        }

        @Override
        public void onRecordingStatusChanged(@NonNull GeckoSession session, RecordingDevice[] devices) {
            String message;
            int icon;
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mActivity);
            RecordingDevice camera = null;
            RecordingDevice microphone = null;

            for (RecordingDevice device : devices) {
                if (device.type == RecordingDevice.Type.CAMERA) {
                    camera = device;
                } else if (device.type == RecordingDevice.Type.MICROPHONE) {
                    microphone = device;
                }
            }
            if (camera != null && microphone != null) {
                Log.d(LOGTAG, "DeviceDelegate:onRecordingDeviceEvent display alert_mic_camera");
                message = getResources().getString(R.string.device_sharing_camera_and_mic);
                icon = R.drawable.alert_mic_camera;
            } else if (camera != null) {
                Log.d(LOGTAG, "DeviceDelegate:onRecordingDeviceEvent display alert_camera");
                message = getResources().getString(R.string.device_sharing_camera);
                icon = R.drawable.alert_camera;
            } else if (microphone != null) {
                Log.d(LOGTAG, "DeviceDelegate:onRecordingDeviceEvent display alert_mic");
                message = getResources().getString(R.string.device_sharing_microphone);
                icon = R.drawable.alert_mic;
            } else {
                Log.d(LOGTAG, "DeviceDelegate:onRecordingDeviceEvent dismiss any notifications");
                if (mNotificationId != null) {
                    notificationManager.cancel(mNotificationId);
                    mNotificationId = null;
                }
                return;
            }
            if (mNotificationId == null) {
                mNotificationId = ++mLastNotificationId;
            }

            Intent intent = new Intent(mActivity, GeckoViewActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(mActivity.getApplicationContext(), 0, intent, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(mActivity.getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(icon)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE);

            notificationManager.notify(mNotificationId, builder.build());
        }
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    //Qslide feature
    @Override
    public void onAttachedToFloatingWindow(FloatingWindow floatingWindow) {
//        Log.d("WindowFlow", "onAttachedToFloatingWindow.");
        /* all resources should be reinitialized once again
         * if you set new layout for the floating mode setContentViewForFloatingMode()*/
        // and also listeners a should be added once again to the buttons in floating mode
        int width = dpToPx(324);
        int height = dpToPx(360);
        floatingWindow.setSize(width, height);
    }

    @Override
    public boolean onDetachedFromFloatingWindow(FloatingWindow w, boolean isReturningToFullScreen) {
//        Log.d("WindowFlow", "onDetachedFromFloatingWindow. Returning to Fullscreen: " + isReturningToFullScreen);

        return true;
    }

    @Override
    public void switchToFloatingMode() {
        if (onStartedAsFloatingMode()) {
            setDontFinishOnFloatingMode(true);
        }
        super.switchToFloatingMode();
    }
}
