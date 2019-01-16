/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.views.scroll;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.PixelUtil;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Helper for view managers to handle commands like 'scrollTo'.
 * Shared by {@link ReactScrollViewManager} and {@link ReactHorizontalScrollViewManager}.
 */
public class ReactScrollViewCommandHelper {

  public static final int COMMAND_SCROLL_TO = 1;
  public static final int COMMAND_SCROLL_TO_END = 2;
  public static final int COMMAND_FLASH_SCROLL_INDICATORS = 3;


  public interface ScrollCommandHandler<T> {
    void scrollTo(T scrollView, ScrollToCommandData data);
    void scrollToEnd(T scrollView, ScrollToEndCommandData data);
    void flashScrollIndicators(T scrollView);
  }

  public static class ScrollToCommandData {

    public final int mDestX, mDestY, mDuration;
    public final boolean mAnimated;

    ScrollToCommandData(int destX, int destY, boolean animated, int duration) {
      mDestX = destX;
      mDestY = destY;
      mAnimated = animated;
      mDuration = duration;
    }
  }

  public static class ScrollToEndCommandData {

    public final int mDuration;
    public final boolean mAnimated;

    ScrollToEndCommandData(boolean animated, int duration) {
      mAnimated = animated;
      mDuration = duration;
    }
  }

  public static Map<String,Integer> getCommandsMap() {
    return MapBuilder.of(
        "scrollTo",
        COMMAND_SCROLL_TO,
        "scrollToEnd",
        COMMAND_SCROLL_TO_END,
        "flashScrollIndicators",
        COMMAND_FLASH_SCROLL_INDICATORS);
  }

  public static <T> void receiveCommand(
      ScrollCommandHandler<T> viewManager,
      T scrollView,
      int commandType,
      @Nullable ReadableArray args) {
    Assertions.assertNotNull(viewManager);
    Assertions.assertNotNull(scrollView);
    Assertions.assertNotNull(args);
    switch (commandType) {
      case COMMAND_SCROLL_TO: {
        int destX = Math.round(PixelUtil.toPixelFromDIP(args.getDouble(0)));
        int destY = Math.round(PixelUtil.toPixelFromDIP(args.getDouble(1)));
        boolean animated = args.getBoolean(2);
        int duration = (int) Math.round(args.getDouble(3));
        viewManager.scrollTo(scrollView, new ScrollToCommandData(destX, destY, animated, duration));
        return;
      }
      case COMMAND_SCROLL_TO_END: {
        boolean animated = args.getBoolean(0);
        int duration = (int) Math.round(args.getDouble(1));
        viewManager.scrollToEnd(scrollView, new ScrollToEndCommandData(animated, duration));
        return;
      }
      case COMMAND_FLASH_SCROLL_INDICATORS:
        viewManager.flashScrollIndicators(scrollView);
        return;

      default:
        throw new IllegalArgumentException(String.format(
            "Unsupported command %d received by %s.",
            commandType,
            viewManager.getClass().getSimpleName()));
    }
  }
}
