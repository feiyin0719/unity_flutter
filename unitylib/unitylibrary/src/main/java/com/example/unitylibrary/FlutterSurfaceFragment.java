// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.unitylibrary;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import io.flutter.Log;
import io.flutter.embedding.android.FlutterEngineConfigurator;
import io.flutter.embedding.android.FlutterEngineProvider;
import io.flutter.embedding.android.FlutterSurfaceView;
import io.flutter.embedding.android.FlutterTextureView;
import io.flutter.embedding.android.FlutterView;
import io.flutter.embedding.android.RenderMode;
import io.flutter.embedding.android.SplashScreen;
import io.flutter.embedding.android.SplashScreenProvider;
import io.flutter.embedding.android.TransparencyMode;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.embedding.engine.FlutterShellArgs;
import io.flutter.embedding.engine.renderer.FlutterUiDisplayListener;
import io.flutter.plugin.platform.PlatformPlugin;
import io.flutter.view.FlutterMain;


public class FlutterSurfaceFragment extends Fragment implements FlutterDelegate.Host, LifecycleOwner {
    private static final String TAG = "FlutterFragment";

    LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    /**
     * The Dart entrypoint method name that is executed upon initialization.
     */
    protected static final String ARG_DART_ENTRYPOINT = "dart_entrypoint";
    /**
     * Initial Flutter route that is rendered in a Navigator widget.
     */
    protected static final String ARG_INITIAL_ROUTE = "initial_route";
    /**
     * Path to Flutter's Dart code.
     */
    protected static final String ARG_APP_BUNDLE_PATH = "app_bundle_path";
    /**
     * Flutter shell arguments.
     */
    protected static final String ARG_FLUTTER_INITIALIZATION_ARGS = "initialization_args";
    /**
     * {@link RenderMode} to be used for the {@link FlutterView} in this {@code FlutterFragment}
     */
    protected static final String ARG_FLUTTERVIEW_RENDER_MODE = "flutterview_render_mode";
    /**
     * {@link TransparencyMode} to be used for the {@link FlutterView} in this {@code FlutterFragment}
     */
    protected static final String ARG_FLUTTERVIEW_TRANSPARENCY_MODE = "flutterview_transparency_mode";
    /**
     * See {@link #shouldAttachEngineToActivity()}.
     */
    protected static final String ARG_SHOULD_ATTACH_ENGINE_TO_ACTIVITY =
            "should_attach_engine_to_activity";
    /**
     * The ID of a {@link FlutterEngine} cached in {@link FlutterEngineCache} that will be used within
     * the created {@code FlutterFragment}.
     */
    protected static final String ARG_CACHED_ENGINE_ID = "cached_engine_id";
    /**
     * True if the {@link FlutterEngine} in the created {@code FlutterFragment} should be destroyed
     * when the {@code FlutterFragment} is destroyed, false if the {@link FlutterEngine} should
     * outlive the {@code FlutterFragment}.
     */
    protected static final String ARG_DESTROY_ENGINE_WITH_FRAGMENT = "destroy_engine_with_fragment";

    /**
     * Creates a {@code FlutterFragment} with a default configuration.
     *
     * <p>{@code FlutterFragment}'s default configuration creates a new {@link FlutterEngine} within
     * the {@code FlutterFragment} and uses the following settings:
     *
     * <ul>
     *   <li>Dart entrypoint: "main"
     *   <li>Initial route: "/"
     *   <li>Render mode: surface
     *   <li>Transparency mode: transparent
     * </ul>
     *
     * <p>To use a new {@link FlutterEngine} with different settings, use {@link #withNewEngine()}.
     *
     * <p>To use a cached {@link FlutterEngine} instead of creating a new one, use {@link
     * #withCachedEngine(String)}.
     */
    @NonNull
    public static FlutterSurfaceFragment createDefault() {
        return new NewEngineFragmentBuilder().build();
    }

    /**
     * Returns a {@link NewEngineFragmentBuilder} to create a {@code FlutterFragment} with a new
     * {@link FlutterEngine} and a desired engine configuration.
     */
    @NonNull
    public static NewEngineFragmentBuilder withNewEngine() {
        return new NewEngineFragmentBuilder();
    }


    public static class NewEngineFragmentBuilder {
        private final Class<? extends FlutterSurfaceFragment> fragmentClass;
        private String dartEntrypoint = "main";
        private String initialRoute = "/";
        private String appBundlePath = null;
        private FlutterShellArgs shellArgs = null;
        private RenderMode renderMode = RenderMode.surface;
        private TransparencyMode transparencyMode = TransparencyMode.transparent;
        private boolean shouldAttachEngineToActivity = true;

        /**
         * Constructs a {@code NewEngineFragmentBuilder} that is configured to construct an instance of
         * {@code FlutterFragment}.
         */
        public NewEngineFragmentBuilder() {
            fragmentClass = FlutterSurfaceFragment.class;
        }

        /**
         * Constructs a {@code NewEngineFragmentBuilder} that is configured to construct an instance of
         * {@code subclass}, which extends {@code FlutterFragment}.
         */
        public NewEngineFragmentBuilder(@NonNull Class<? extends FlutterSurfaceFragment> subclass) {
            fragmentClass = subclass;
        }

        /**
         * The name of the initial Dart method to invoke, defaults to "main".
         */
        @NonNull
        public NewEngineFragmentBuilder dartEntrypoint(@NonNull String dartEntrypoint) {
            this.dartEntrypoint = dartEntrypoint;
            return this;
        }


        @NonNull
        public NewEngineFragmentBuilder initialRoute(@NonNull String initialRoute) {
            this.initialRoute = initialRoute;
            return this;
        }

        /**
         * The path to the app bundle which contains the Dart app to execute, defaults to {@link
         * FlutterMain#findAppBundlePath()}
         */
        @NonNull
        public NewEngineFragmentBuilder appBundlePath(@NonNull String appBundlePath) {
            this.appBundlePath = appBundlePath;
            return this;
        }

        /**
         * Any special configuration arguments for the Flutter engine
         */
        @NonNull
        public NewEngineFragmentBuilder flutterShellArgs(@NonNull FlutterShellArgs shellArgs) {
            this.shellArgs = shellArgs;
            return this;
        }

        /**
         * Render Flutter either as a {@link RenderMode#surface} or a {@link RenderMode#texture}. You
         * should use {@code surface} unless you have a specific reason to use {@code texture}. {@code
         * texture} comes with a significant performance impact, but {@code texture} can be displayed
         * beneath other Android {@code View}s and animated, whereas {@code surface} cannot.
         */
        @NonNull
        public NewEngineFragmentBuilder renderMode(@NonNull RenderMode renderMode) {
            this.renderMode = renderMode;
            return this;
        }

        /**
         * Support a {@link TransparencyMode#transparent} background within {@link FlutterView}, or
         * force an {@link TransparencyMode#opaque} background.
         *
         * <p>See {@link TransparencyMode} for implications of this selection.
         */
        @NonNull
        public NewEngineFragmentBuilder transparencyMode(@NonNull TransparencyMode transparencyMode) {
            this.transparencyMode = transparencyMode;
            return this;
        }


        @NonNull
        public NewEngineFragmentBuilder shouldAttachEngineToActivity(
                boolean shouldAttachEngineToActivity) {
            this.shouldAttachEngineToActivity = shouldAttachEngineToActivity;
            return this;
        }

        /**
         * Creates a {@link Bundle} of arguments that are assigned to the new {@code FlutterFragment}.
         *
         * <p>Subclasses should override this method to add new properties to the {@link Bundle}.
         * Subclasses must call through to the super method to collect all existing property values.
         */
        @NonNull
        protected Bundle createArgs() {
            Bundle args = new Bundle();
            args.putString(ARG_INITIAL_ROUTE, initialRoute);
            args.putString(ARG_APP_BUNDLE_PATH, appBundlePath);
            args.putString(ARG_DART_ENTRYPOINT, dartEntrypoint);
            // TODO(mattcarroll): determine if we should have an explicit FlutterTestFragment instead of
            // conflating.
            if (null != shellArgs) {
                args.putStringArray(ARG_FLUTTER_INITIALIZATION_ARGS, shellArgs.toArray());
            }
            args.putString(
                    ARG_FLUTTERVIEW_RENDER_MODE,
                    renderMode != null ? renderMode.name() : RenderMode.surface.name());
            args.putString(
                    ARG_FLUTTERVIEW_TRANSPARENCY_MODE,
                    transparencyMode != null ? transparencyMode.name() : TransparencyMode.transparent.name());
            args.putBoolean(ARG_SHOULD_ATTACH_ENGINE_TO_ACTIVITY, shouldAttachEngineToActivity);
            args.putBoolean(ARG_DESTROY_ENGINE_WITH_FRAGMENT, true);
            args.putBoolean("verbose-logging", true);
            return args;
        }

        /**
         * Constructs a new {@code FlutterFragment} (or a subclass) that is configured based on
         * properties set on this {@code Builder}.
         */
        @NonNull
        public <T extends FlutterSurfaceFragment> T build() {
            try {
                @SuppressWarnings("unchecked")
                T frag = (T) fragmentClass.getDeclaredConstructor().newInstance();
                if (frag == null) {
                    throw new RuntimeException(
                            "The FlutterFragment subclass sent in the constructor ("
                                    + fragmentClass.getCanonicalName()
                                    + ") does not match the expected return type.");
                }

                Bundle args = createArgs();
                frag.setArguments(args);

                return frag;
            } catch (Exception e) {
                throw new RuntimeException(
                        "Could not instantiate FlutterFragment subclass (" + fragmentClass.getName() + ")", e);
            }
        }
    }

    /**
     * Returns a {@link CachedEngineFragmentBuilder} to create a {@code FlutterFragment} with a cached
     * {@link FlutterEngine} in {@link FlutterEngineCache}.
     *
     * <p>An {@code IllegalStateException} will be thrown during the lifecycle of the {@code
     * FlutterFragment} if a cached {@link FlutterEngine} is requested but does not exist in the
     * cache.
     *
     * <p>To create a {@code FlutterFragment} that uses a new {@link FlutterEngine}, use {@link
     * #createDefault()} or {@link #withNewEngine()}.
     */
    @NonNull
    public static CachedEngineFragmentBuilder withCachedEngine(@NonNull String engineId) {
        return new CachedEngineFragmentBuilder(engineId);
    }


    public static class CachedEngineFragmentBuilder {
        private final Class<? extends FlutterSurfaceFragment> fragmentClass;
        private final String engineId;
        private boolean destroyEngineWithFragment = false;
        private RenderMode renderMode = RenderMode.surface;
        private TransparencyMode transparencyMode = TransparencyMode.transparent;
        private boolean shouldAttachEngineToActivity = true;

        private CachedEngineFragmentBuilder(@NonNull String engineId) {
            this(FlutterSurfaceFragment.class, engineId);
        }

        protected CachedEngineFragmentBuilder(
                @NonNull Class<? extends FlutterSurfaceFragment> subclass, @NonNull String engineId) {
            this.fragmentClass = subclass;
            this.engineId = engineId;
        }

        /**
         * Pass {@code true} to destroy the cached {@link FlutterEngine} when this {@code
         * FlutterFragment} is destroyed, or {@code false} for the cached {@link FlutterEngine} to
         * outlive this {@code FlutterFragment}.
         */
        @NonNull
        public CachedEngineFragmentBuilder destroyEngineWithFragment(
                boolean destroyEngineWithFragment) {
            this.destroyEngineWithFragment = destroyEngineWithFragment;
            return this;
        }

        /**
         * Render Flutter either as a {@link RenderMode#surface} or a {@link RenderMode#texture}. You
         * should use {@code surface} unless you have a specific reason to use {@code texture}. {@code
         * texture} comes with a significant performance impact, but {@code texture} can be displayed
         * beneath other Android {@code View}s and animated, whereas {@code surface} cannot.
         */
        @NonNull
        public CachedEngineFragmentBuilder renderMode(@NonNull RenderMode renderMode) {
            this.renderMode = renderMode;
            return this;
        }

        /**
         * Support a {@link TransparencyMode#transparent} background within {@link FlutterView}, or
         * force an {@link TransparencyMode#opaque} background.
         *
         * <p>See {@link TransparencyMode} for implications of this selection.
         */
        @NonNull
        public CachedEngineFragmentBuilder transparencyMode(
                @NonNull TransparencyMode transparencyMode) {
            this.transparencyMode = transparencyMode;
            return this;
        }


        @NonNull
        public CachedEngineFragmentBuilder shouldAttachEngineToActivity(
                boolean shouldAttachEngineToActivity) {
            this.shouldAttachEngineToActivity = shouldAttachEngineToActivity;
            return this;
        }

        /**
         * Creates a {@link Bundle} of arguments that are assigned to the new {@code FlutterFragment}.
         *
         * <p>Subclasses should override this method to add new properties to the {@link Bundle}.
         * Subclasses must call through to the super method to collect all existing property values.
         */
        @NonNull
        protected Bundle createArgs() {
            Bundle args = new Bundle();
            args.putString(ARG_CACHED_ENGINE_ID, engineId);
            args.putBoolean(ARG_DESTROY_ENGINE_WITH_FRAGMENT, destroyEngineWithFragment);
            args.putString(
                    ARG_FLUTTERVIEW_RENDER_MODE,
                    renderMode != null ? renderMode.name() : RenderMode.surface.name());
            args.putString(
                    ARG_FLUTTERVIEW_TRANSPARENCY_MODE,
                    transparencyMode != null ? transparencyMode.name() : TransparencyMode.transparent.name());
            args.putBoolean(ARG_SHOULD_ATTACH_ENGINE_TO_ACTIVITY, shouldAttachEngineToActivity);
            return args;
        }

        /**
         * Constructs a new {@code FlutterFragment} (or a subclass) that is configured based on
         * properties set on this {@code CachedEngineFragmentBuilder}.
         */
        @NonNull
        public <T extends FlutterSurfaceFragment> T build() {
            try {
                @SuppressWarnings("unchecked")
                T frag = (T) fragmentClass.getDeclaredConstructor().newInstance();
                if (frag == null) {
                    throw new RuntimeException(
                            "The FlutterFragment subclass sent in the constructor ("
                                    + fragmentClass.getCanonicalName()
                                    + ") does not match the expected return type.");
                }

                Bundle args = createArgs();
                frag.setArguments(args);

                return frag;
            } catch (Exception e) {
                throw new RuntimeException(
                        "Could not instantiate FlutterFragment subclass (" + fragmentClass.getName() + ")", e);
            }
        }
    }

    // Delegate that runs all lifecycle and OS hook logic that is common between
    // FlutterActivity and FlutterFragment. See the FlutterActivityAndFragmentDelegate
    // implementation for details about why it exists.
    @VisibleForTesting /* package */ FlutterDelegate delegate;

    public FlutterSurfaceFragment() {
        // Ensure that we at least have an empty Bundle of arguments so that we don't
        // need to continually check for null arguments before grabbing one.
        setArguments(new Bundle());
    }

    /**
     * This method exists so that JVM tests can ensure that a delegate exists without putting this
     * Fragment through any lifecycle events, because JVM tests cannot handle executing any lifecycle
     * methods, at the time of writing this.
     *
     * <p>The testing infrastructure should be upgraded to make FlutterFragment tests easy to write
     * while exercising real lifecycle methods. At such a time, this method should be removed.
     */
    // TODO(mattcarroll): remove this when tests allow for it
    // (https://github.com/flutter/flutter/issues/43798)
    @VisibleForTesting
    /* package */ void setDelegate(@NonNull FlutterDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        delegate = new FlutterDelegate(this);
        delegate.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i("myyf", "onCreateView");
        delegate.createSurface();
//        return delegate.onCreateView(inflater, container, savedInstanceState);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        delegate.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        delegate.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        delegate.onResume();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                GLTexture.instance.updateTexture();
//                Bitmap bitmap = getFlutterEngine().getRenderer().getBitmap();
//                if (bitmap != null)
//                    GLTexture.instance.saveBitmap(getActivity(),bitmap, "flutter");
//            }
//        }, 2000);

    }

    // TODO(mattcarroll): determine why this can't be in onResume(). Comment reason, or move if
    // possible.
    @ActivityCallThrough
    public void onPostResume() {
        delegate.onPostResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        delegate.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        delegate.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        delegate.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        delegate.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        delegate.onDetach();
        delegate.release();
        delegate = null;
    }

    /**
     * The result of a permission request has been received.
     *
     * <p>See {@link android.app.Activity#onRequestPermissionsResult(int, String[], int[])}
     *
     * <p>
     *
     * @param requestCode  identifier passed with the initial permission request
     * @param permissions  permissions that were requested
     * @param grantResults permission grants or denials
     */
    @ActivityCallThrough
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        delegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @ActivityCallThrough
    public void onNewIntent(@NonNull Intent intent) {
        delegate.onNewIntent(intent);
    }

    /**
     * The hardware back button was pressed.
     *
     * <p>See {@link android.app.Activity#onBackPressed()}
     */
    @ActivityCallThrough
    public void onBackPressed() {
        delegate.onBackPressed();
    }

    /**
     * A result has been returned after an invocation of {@link
     * Fragment#startActivityForResult(Intent, int)}.
     *
     * <p>
     *
     * @param requestCode request code sent with {@link Fragment#startActivityForResult(Intent, int)}
     * @param resultCode  code representing the result of the {@code Activity} that was launched
     * @param data        any corresponding return data, held within an {@code Intent}
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        delegate.onActivityResult(requestCode, resultCode, data);
    }


    @ActivityCallThrough
    public void onUserLeaveHint() {
        delegate.onUserLeaveHint();
    }

    /**
     * Callback invoked when memory is low.
     *
     * <p>This implementation forwards a memory pressure warning to the running Flutter app.
     *
     * <p>
     *
     * @param level level
     */
    @ActivityCallThrough
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        delegate.onTrimMemory(level);
    }

    /**
     * Callback invoked when memory is low.
     *
     * <p>This implementation forwards a memory pressure warning to the running Flutter app.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        delegate.onLowMemory();
    }


    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    @Override
    @NonNull
    public FlutterShellArgs getFlutterShellArgs() {
        String[] flutterShellArgsArray = getArguments().getStringArray(ARG_FLUTTER_INITIALIZATION_ARGS);
        return new FlutterShellArgs(
                flutterShellArgsArray != null ? flutterShellArgsArray : new String[]{});
    }

    /**
     * Returns the ID of a statically cached {@link FlutterEngine} to use within this {@code
     * FlutterFragment}, or {@code null} if this {@code FlutterFragment} does not want to use a cached
     * {@link FlutterEngine}.
     */
    @Nullable
    @Override
    public String getCachedEngineId() {
        return getArguments().getString(ARG_CACHED_ENGINE_ID, null);
    }

    /**
     * Returns false if the {@link FlutterEngine} within this {@code FlutterFragment} should outlive
     * the {@code FlutterFragment}, itself.
     *
     * <p>Defaults to true if no custom {@link FlutterEngine is provided}, false if a custom {@link
     * FlutterEngine} is provided.
     */
    @Override
    public boolean shouldDestroyEngineWithHost() {
        boolean explicitDestructionRequested =
                getArguments().getBoolean(ARG_DESTROY_ENGINE_WITH_FRAGMENT, false);
        if (getCachedEngineId() != null || delegate.isFlutterEngineFromHost()) {
            // Only destroy a cached engine if explicitly requested by app developer.
            return explicitDestructionRequested;
        } else {
            // If this Fragment created the FlutterEngine, destroy it by default unless
            // explicitly requested not to.
            return getArguments().getBoolean(ARG_DESTROY_ENGINE_WITH_FRAGMENT, true);
        }
    }


    @Override
    @NonNull
    public String getDartEntrypointFunctionName() {
        return getArguments().getString(ARG_DART_ENTRYPOINT, "main");
    }


    @Override
    @NonNull
    public String getAppBundlePath() {
        return getArguments().getString(ARG_APP_BUNDLE_PATH, FlutterMain.findAppBundlePath());
    }


    @Override
    @Nullable
    public String getInitialRoute() {
        return getArguments().getString(ARG_INITIAL_ROUTE);
    }


    @Override
    @NonNull
    public RenderMode getRenderMode() {
        String renderModeName =
                getArguments().getString(ARG_FLUTTERVIEW_RENDER_MODE, RenderMode.surface.name());
        return RenderMode.valueOf(renderModeName);
    }


    @Override
    @NonNull
    public TransparencyMode getTransparencyMode() {
        String transparencyModeName =
                getArguments()
                        .getString(ARG_FLUTTERVIEW_TRANSPARENCY_MODE, TransparencyMode.transparent.name());
        return TransparencyMode.valueOf(transparencyModeName);
    }

    @Override
    @Nullable
    public SplashScreen provideSplashScreen() {
        Activity parentActivity = getActivity();
        if (parentActivity instanceof SplashScreenProvider) {
            SplashScreenProvider splashScreenProvider = (SplashScreenProvider) parentActivity;
            return splashScreenProvider.provideSplashScreen();
        }

        return null;
    }


    @Override
    @Nullable
    public FlutterEngine provideFlutterEngine(@NonNull Context context) {
        // Defer to the FragmentActivity that owns us to see if it wants to provide a
        // FlutterEngine.
        FlutterEngine flutterEngine = null;
        Activity attachedActivity = getActivity();
        if (attachedActivity instanceof FlutterEngineProvider) {
            // Defer to the Activity that owns us to provide a FlutterEngine.
            Log.v(TAG, "Deferring to attached Activity to provide a FlutterEngine.");
            FlutterEngineProvider flutterEngineProvider = (FlutterEngineProvider) attachedActivity;
            flutterEngine = flutterEngineProvider.provideFlutterEngine(getContext());
        }

        return flutterEngine;
    }

    /**
     * Hook for subclasses to obtain a reference to the {@link FlutterEngine} that is owned by this
     * {@code FlutterActivity}.
     */
    @Nullable
    public FlutterEngine getFlutterEngine() {
        return delegate.getFlutterEngine();
    }

    @Nullable
    @Override
    public PlatformPlugin providePlatformPlugin(
            @Nullable Activity activity, @NonNull FlutterEngine flutterEngine) {
        if (activity != null) {
            return new PlatformPlugin(getActivity(), flutterEngine.getPlatformChannel());
        } else {
            return null;
        }
    }


    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        Activity attachedActivity = getActivity();
        if (attachedActivity instanceof FlutterEngineConfigurator) {
            ((FlutterEngineConfigurator) attachedActivity).configureFlutterEngine(flutterEngine);
        }
    }

    /**
     * Hook for the host to cleanup references that were established in {@link
     * #configureFlutterEngine(FlutterEngine)} before the host is destroyed or detached.
     *
     * <p>This method is called in {@link #onDetach()}.
     */
    @Override
    public void cleanUpFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        Activity attachedActivity = getActivity();
        if (attachedActivity instanceof FlutterEngineConfigurator) {
            ((FlutterEngineConfigurator) attachedActivity).cleanUpFlutterEngine(flutterEngine);
        }
    }


    @Override
    public boolean shouldAttachEngineToActivity() {
        return getArguments().getBoolean(ARG_SHOULD_ATTACH_ENGINE_TO_ACTIVITY);
    }

    @Override
    public void onFlutterSurfaceViewCreated(@NonNull FlutterSurfaceView flutterSurfaceView) {
        // Hook for subclasses.
    }

    @Override
    public void onFlutterTextureViewCreated(@NonNull FlutterTextureView flutterTextureView) {
        // Hook for subclasses.
    }

    @Override
    public void onFlutterUiDisplayed() {
        Activity attachedActivity = getActivity();
        if (attachedActivity instanceof FlutterUiDisplayListener) {
            ((FlutterUiDisplayListener) attachedActivity).onFlutterUiDisplayed();
        }
    }


    @Override
    public void onFlutterUiNoLongerDisplayed() {
        Activity attachedActivity = getActivity();
        if (attachedActivity instanceof FlutterUiDisplayListener) {
            ((FlutterUiDisplayListener) attachedActivity).onFlutterUiNoLongerDisplayed();
        }
    }

    /**
     * Annotates methods in {@code FlutterFragment} that must be called by the containing {@code
     * Activity}.
     */
    @interface ActivityCallThrough {
    }
}
