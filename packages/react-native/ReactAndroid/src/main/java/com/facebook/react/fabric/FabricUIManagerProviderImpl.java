/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.fabric;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.react.bridge.CatalystInstance;
import com.facebook.react.bridge.JSIModuleProvider;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.UIManager;
import com.facebook.react.bridge.UIManagerProvider;
import com.facebook.react.fabric.events.EventBeatManager;
import com.facebook.react.uimanager.ViewManagerRegistry;
import com.facebook.systrace.Systrace;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class FabricUIManagerProviderImpl
    implements JSIModuleProvider<UIManager>, UIManagerProvider {

  private final @Nullable ReactApplicationContext mReactApplicationContext;
  private final ComponentFactory mComponentFactory;
  private final ReactNativeConfig mConfig;
  private final ViewManagerRegistry mViewManagerRegistry;

  public FabricUIManagerProviderImpl(
      ReactApplicationContext reactApplicationContext,
      ComponentFactory componentFactory,
      ReactNativeConfig config,
      ViewManagerRegistry viewManagerRegistry) {
    mReactApplicationContext = reactApplicationContext;
    mComponentFactory = componentFactory;
    mConfig = config;
    mViewManagerRegistry = viewManagerRegistry;
  }

  public FabricUIManagerProviderImpl(
      ComponentFactory componentFactory,
      ReactNativeConfig config,
      ViewManagerRegistry viewManagerRegistry) {
    mReactApplicationContext = null;
    mComponentFactory = componentFactory;
    mConfig = config;
    mViewManagerRegistry = viewManagerRegistry;
  }

  @Override
  public UIManager get() {
    if (mReactApplicationContext != null) {
      return createUIManager(mReactApplicationContext);
    }
    throw new IllegalStateException(
        "This method shoulndn't be called without ReactContext initialized");
  }

  @Override
  public UIManager createUIManager(ReactApplicationContext reactApplicationContext) {
    Systrace.beginSection(
        Systrace.TRACE_TAG_REACT_JAVA_BRIDGE, "FabricUIManagerProviderImpl.create");
    EventBeatManager eventBeatManager = new EventBeatManager();
    Systrace.beginSection(
        Systrace.TRACE_TAG_REACT_JAVA_BRIDGE, "FabricUIManagerProviderImpl.createUIManager");

    FabricUIManager fabricUIManager =
        new FabricUIManager(reactApplicationContext, mViewManagerRegistry, eventBeatManager);
    Systrace.endSection(Systrace.TRACE_TAG_REACT_JAVA_BRIDGE);

    Systrace.beginSection(
        Systrace.TRACE_TAG_REACT_JAVA_BRIDGE, "FabricUIManagerProviderImpl.registerBinding");
    final Binding binding = new BindingImpl();

    CatalystInstance catalystInstance = reactApplicationContext.getCatalystInstance();

    binding.register(
        catalystInstance.getRuntimeExecutor(),
        catalystInstance.getRuntimeScheduler(),
        fabricUIManager,
        eventBeatManager,
        mComponentFactory,
        mConfig);

    Systrace.endSection(Systrace.TRACE_TAG_REACT_JAVA_BRIDGE);
    Systrace.endSection(Systrace.TRACE_TAG_REACT_JAVA_BRIDGE);

    return fabricUIManager;
  }
}
