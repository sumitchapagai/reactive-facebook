/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.views.text;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.util.LayoutDirection;
import android.view.Gravity;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.ReactNoCrashSoftException;
import com.facebook.react.bridge.ReactSoftExceptionLogger;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.common.mapbuffer.MapBuffer;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ReactAccessibilityDelegate.AccessibilityRole;
import com.facebook.react.uimanager.ReactAccessibilityDelegate.Role;
import com.facebook.react.views.text.internal.span.CustomLetterSpacingSpan;
import com.facebook.react.views.text.internal.span.CustomLineHeightSpan;
import com.facebook.react.views.text.internal.span.CustomStyleSpan;
import com.facebook.react.views.text.internal.span.ReactAbsoluteSizeSpan;
import com.facebook.react.views.text.internal.span.ReactBackgroundColorSpan;
import com.facebook.react.views.text.internal.span.ReactClickableSpan;
import com.facebook.react.views.text.internal.span.ReactForegroundColorSpan;
import com.facebook.react.views.text.internal.span.ReactStrikethroughSpan;
import com.facebook.react.views.text.internal.span.ReactTagSpan;
import com.facebook.react.views.text.internal.span.ReactUnderlineSpan;
import com.facebook.react.views.text.internal.span.SetSpanOperation;
import com.facebook.react.views.text.internal.span.ShadowStyleSpan;
import com.facebook.react.views.text.internal.span.TextInlineViewPlaceholderSpan;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Class responsible of creating {@link Spanned} object for the JS representation of Text */
public class TextLayoutManager {

  // constants for AttributedString serialization
  public static final short AS_KEY_HASH = 0;
  public static final short AS_KEY_STRING = 1;
  public static final short AS_KEY_FRAGMENTS = 2;
  public static final short AS_KEY_CACHE_ID = 3;

  // constants for Fragment serialization
  public static final short FR_KEY_STRING = 0;
  public static final short FR_KEY_REACT_TAG = 1;
  public static final short FR_KEY_IS_ATTACHMENT = 2;
  public static final short FR_KEY_WIDTH = 3;
  public static final short FR_KEY_HEIGHT = 4;
  public static final short FR_KEY_TEXT_ATTRIBUTES = 5;

  // constants for ParagraphAttributes serialization
  public static final short PA_KEY_MAX_NUMBER_OF_LINES = 0;
  public static final short PA_KEY_ELLIPSIZE_MODE = 1;
  public static final short PA_KEY_TEXT_BREAK_STRATEGY = 2;
  public static final short PA_KEY_ADJUST_FONT_SIZE_TO_FIT = 3;
  public static final short PA_KEY_INCLUDE_FONT_PADDING = 4;
  public static final short PA_KEY_HYPHENATION_FREQUENCY = 5;
  public static final short PA_KEY_MINIMUM_FONT_SIZE = 6;
  public static final short PA_KEY_MAXIMUM_FONT_SIZE = 7;

  private static final boolean ENABLE_MEASURE_LOGGING = ReactBuildConfig.DEBUG && false;

  private static final String TAG = TextLayoutManager.class.getSimpleName();

  // It's important to pass the ANTI_ALIAS_FLAG flag to the constructor rather than setting it
  // later by calling setFlags. This is because the latter approach triggers a bug on Android 4.4.2.
  // The bug is that unicode emoticons aren't measured properly which causes text to be clipped.
  private static final TextPaint sTextPaintInstance = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);

  private static final String INLINE_VIEW_PLACEHOLDER = "0";

  private static final boolean DEFAULT_INCLUDE_FONT_PADDING = true;

  private static final boolean DEFAULT_ADJUST_FONT_SIZE_TO_FIT = false;

  private static final ConcurrentHashMap<Integer, Spannable> sTagToSpannableCache =
      new ConcurrentHashMap<>();

  public static void setCachedSpannableForTag(int reactTag, @NonNull Spannable sp) {
    if (ENABLE_MEASURE_LOGGING) {
      FLog.e(TAG, "Set cached spannable for tag[" + reactTag + "]: " + sp.toString());
    }
    sTagToSpannableCache.put(reactTag, sp);
  }

  public static void deleteCachedSpannableForTag(int reactTag) {
    if (ENABLE_MEASURE_LOGGING) {
      FLog.e(TAG, "Delete cached spannable for tag[" + reactTag + "]");
    }
    sTagToSpannableCache.remove(reactTag);
  }

  public static boolean isRTL(MapBuffer attributedString) {
    // TODO: Don't read AS_KEY_FRAGMENTS, which may be expensive, and is not present when using
    // cached Spannable
    if (!attributedString.contains(AS_KEY_FRAGMENTS)) {
      return false;
    }

    MapBuffer fragments = attributedString.getMapBuffer(AS_KEY_FRAGMENTS);
    if (fragments.getCount() == 0) {
      return false;
    }

    MapBuffer fragment = fragments.getMapBuffer(0);
    MapBuffer textAttributes = fragment.getMapBuffer(FR_KEY_TEXT_ATTRIBUTES);

    if (!textAttributes.contains(TextAttributeProps.TA_KEY_LAYOUT_DIRECTION)) {
      return false;
    }

    return TextAttributeProps.getLayoutDirection(
            textAttributes.getString(TextAttributeProps.TA_KEY_LAYOUT_DIRECTION))
        == LayoutDirection.RTL;
  }

  public static Layout.Alignment getTextAlignment(MapBuffer attributedString, Spannable spanned) {
    // TODO: Don't read AS_KEY_FRAGMENTS, which may be expensive, and is not present when using
    // cached Spannable
    if (!attributedString.contains(AS_KEY_FRAGMENTS)) {
      return Layout.Alignment.ALIGN_NORMAL;
    }

    // Android will align text based on the script, so normal and opposite alignment needs to be
    // swapped when the directions of paragraph and script don't match.
    // I.e. paragraph is LTR but script is RTL, text needs to be aligned to the left, which means
    // ALIGN_OPPOSITE needs to be used to align RTL script to the left
    boolean isParagraphRTL = isRTL(attributedString);
    boolean isScriptRTL =
        TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(spanned, 0, spanned.length());
    boolean swapNormalAndOpposite = isParagraphRTL != isScriptRTL;

    Layout.Alignment alignment =
        swapNormalAndOpposite ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL;

    MapBuffer fragments = attributedString.getMapBuffer(AS_KEY_FRAGMENTS);
    if (fragments.getCount() != 0) {
      MapBuffer fragment = fragments.getMapBuffer(0);
      MapBuffer textAttributes = fragment.getMapBuffer(FR_KEY_TEXT_ATTRIBUTES);

      if (textAttributes.contains(TextAttributeProps.TA_KEY_ALIGNMENT)) {
        String alignmentAttr = textAttributes.getString(TextAttributeProps.TA_KEY_ALIGNMENT);

        if (alignmentAttr.equals("center")) {
          alignment = Layout.Alignment.ALIGN_CENTER;
        } else if (alignmentAttr.equals("right")) {
          alignment =
              swapNormalAndOpposite
                  ? Layout.Alignment.ALIGN_NORMAL
                  : Layout.Alignment.ALIGN_OPPOSITE;
        }
      }
    }

    return alignment;
  }

  public static int getTextGravity(
      MapBuffer attributedString, Spannable spanned, int defaultValue) {
    int gravity = defaultValue;
    Layout.Alignment alignment = getTextAlignment(attributedString, spanned);

    // depending on whether the script is LTR or RTL, ALIGN_NORMAL and ALIGN_OPPOSITE may mean
    // different things
    boolean swapLeftAndRight =
        TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(spanned, 0, spanned.length());

    if (alignment == Layout.Alignment.ALIGN_NORMAL) {
      gravity = swapLeftAndRight ? Gravity.RIGHT : Gravity.LEFT;
    } else if (alignment == Layout.Alignment.ALIGN_OPPOSITE) {
      gravity = swapLeftAndRight ? Gravity.LEFT : Gravity.RIGHT;
    } else if (alignment == Layout.Alignment.ALIGN_CENTER) {
      gravity = Gravity.CENTER_HORIZONTAL;
    }

    return gravity;
  }

  private static void buildSpannableFromFragments(
      Context context, MapBuffer fragments, SpannableStringBuilder sb, List<SetSpanOperation> ops) {

    for (int i = 0, length = fragments.getCount(); i < length; i++) {
      MapBuffer fragment = fragments.getMapBuffer(i);
      int start = sb.length();

      TextAttributeProps textAttributes =
          TextAttributeProps.fromMapBuffer(fragment.getMapBuffer(FR_KEY_TEXT_ATTRIBUTES));

      sb.append(
          TextTransform.apply(fragment.getString(FR_KEY_STRING), textAttributes.mTextTransform));

      int end = sb.length();
      int reactTag =
          fragment.contains(FR_KEY_REACT_TAG) ? fragment.getInt(FR_KEY_REACT_TAG) : View.NO_ID;
      if (fragment.contains(FR_KEY_IS_ATTACHMENT) && fragment.getBoolean(FR_KEY_IS_ATTACHMENT)) {
        float width = PixelUtil.toPixelFromSP(fragment.getDouble(FR_KEY_WIDTH));
        float height = PixelUtil.toPixelFromSP(fragment.getDouble(FR_KEY_HEIGHT));
        ops.add(
            new SetSpanOperation(
                sb.length() - INLINE_VIEW_PLACEHOLDER.length(),
                sb.length(),
                new TextInlineViewPlaceholderSpan(reactTag, (int) width, (int) height)));
      } else if (end >= start) {
        boolean roleIsLink =
            textAttributes.mRole != null
                ? textAttributes.mRole == Role.LINK
                : textAttributes.mAccessibilityRole == AccessibilityRole.LINK;
        if (roleIsLink) {
          ops.add(new SetSpanOperation(start, end, new ReactClickableSpan(reactTag)));
        }
        if (textAttributes.mIsColorSet) {
          ops.add(
              new SetSpanOperation(
                  start, end, new ReactForegroundColorSpan(textAttributes.mColor)));
        }
        if (textAttributes.mIsBackgroundColorSet) {
          ops.add(
              new SetSpanOperation(
                  start, end, new ReactBackgroundColorSpan(textAttributes.mBackgroundColor)));
        }
        if (!Float.isNaN(textAttributes.getLetterSpacing())) {
          ops.add(
              new SetSpanOperation(
                  start, end, new CustomLetterSpacingSpan(textAttributes.getLetterSpacing())));
        }
        ops.add(
            new SetSpanOperation(start, end, new ReactAbsoluteSizeSpan(textAttributes.mFontSize)));
        if (textAttributes.mFontStyle != ReactConstants.UNSET
            || textAttributes.mFontWeight != ReactConstants.UNSET
            || textAttributes.mFontFamily != null) {
          ops.add(
              new SetSpanOperation(
                  start,
                  end,
                  new CustomStyleSpan(
                      textAttributes.mFontStyle,
                      textAttributes.mFontWeight,
                      textAttributes.mFontFeatureSettings,
                      textAttributes.mFontFamily,
                      context.getAssets())));
        }
        if (textAttributes.mIsUnderlineTextDecorationSet) {
          ops.add(new SetSpanOperation(start, end, new ReactUnderlineSpan()));
        }
        if (textAttributes.mIsLineThroughTextDecorationSet) {
          ops.add(new SetSpanOperation(start, end, new ReactStrikethroughSpan()));
        }
        if ((textAttributes.mTextShadowOffsetDx != 0
                || textAttributes.mTextShadowOffsetDy != 0
                || textAttributes.mTextShadowRadius != 0)
            && Color.alpha(textAttributes.mTextShadowColor) != 0) {
          ops.add(
              new SetSpanOperation(
                  start,
                  end,
                  new ShadowStyleSpan(
                      textAttributes.mTextShadowOffsetDx,
                      textAttributes.mTextShadowOffsetDy,
                      textAttributes.mTextShadowRadius,
                      textAttributes.mTextShadowColor)));
        }
        if (!Float.isNaN(textAttributes.getEffectiveLineHeight())) {
          ops.add(
              new SetSpanOperation(
                  start, end, new CustomLineHeightSpan(textAttributes.getEffectiveLineHeight())));
        }

        ops.add(new SetSpanOperation(start, end, new ReactTagSpan(reactTag)));
      }
    }
  }

  // public because both ReactTextViewManager and ReactTextInputManager need to use this
  public static Spannable getOrCreateSpannableForText(
      Context context,
      MapBuffer attributedString,
      @Nullable ReactTextViewManagerCallback reactTextViewManagerCallback) {
    Spannable text = null;
    if (attributedString.contains(AS_KEY_CACHE_ID)) {
      Integer cacheId = attributedString.getInt(AS_KEY_CACHE_ID);
      text = sTagToSpannableCache.get(cacheId);
    } else {
      text =
          createSpannableFromAttributedString(
              context, attributedString, reactTextViewManagerCallback);
    }

    return text;
  }

  private static Spannable createSpannableFromAttributedString(
      Context context,
      MapBuffer attributedString,
      @Nullable ReactTextViewManagerCallback reactTextViewManagerCallback) {

    SpannableStringBuilder sb = new SpannableStringBuilder();

    // The {@link SpannableStringBuilder} implementation require setSpan operation to be called
    // up-to-bottom, otherwise all the spannables that are within the region for which one may set
    // a new spannable will be wiped out
    List<SetSpanOperation> ops = new ArrayList<>();

    buildSpannableFromFragments(context, attributedString.getMapBuffer(AS_KEY_FRAGMENTS), sb, ops);

    // TODO T31905686: add support for inline Images
    // While setting the Spans on the final text, we also check whether any of them are images.
    for (int priorityIndex = 0; priorityIndex < ops.size(); ++priorityIndex) {
      final SetSpanOperation op = ops.get(ops.size() - priorityIndex - 1);

      // Actual order of calling {@code execute} does NOT matter,
      // but the {@code priorityIndex} DOES matter.
      op.execute(sb, priorityIndex);
    }

    if (reactTextViewManagerCallback != null) {
      reactTextViewManagerCallback.onPostProcessSpannable(sb);
    }
    return sb;
  }

  private static Layout createLayout(
      Spannable text,
      BoringLayout.Metrics boring,
      float width,
      YogaMeasureMode widthYogaMeasureMode,
      boolean includeFontPadding,
      int textBreakStrategy,
      int hyphenationFrequency,
      Layout.Alignment alignment) {
    Layout layout;
    int spanLength = text.length();
    boolean unconstrainedWidth = widthYogaMeasureMode == YogaMeasureMode.UNDEFINED || width < 0;
    float desiredWidth =
        boring == null ? Layout.getDesiredWidth(text, sTextPaintInstance) : Float.NaN;
    boolean isScriptRTL = TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(text, 0, spanLength);

    if (boring == null
        && (unconstrainedWidth
            || (!YogaConstants.isUndefined(desiredWidth) && desiredWidth <= width))) {
      // Is used when the width is not known and the text is not boring, ie. if it contains
      // unicode characters.

      if (widthYogaMeasureMode == YogaMeasureMode.EXACTLY) {
        desiredWidth = width;
      }

      int hintWidth = (int) Math.ceil(desiredWidth);
      layout =
          StaticLayout.Builder.obtain(text, 0, spanLength, sTextPaintInstance, hintWidth)
              .setAlignment(alignment)
              .setLineSpacing(0.f, 1.f)
              .setIncludePad(includeFontPadding)
              .setBreakStrategy(textBreakStrategy)
              .setHyphenationFrequency(hyphenationFrequency)
              .setTextDirection(
                  isScriptRTL ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR)
              .build();

    } else if (boring != null && (unconstrainedWidth || boring.width <= width)) {
      int boringLayoutWidth = boring.width;
      if (widthYogaMeasureMode == YogaMeasureMode.EXACTLY) {
        boringLayoutWidth = (int) Math.ceil(width);
      }
      if (boring.width < 0) {
        ReactSoftExceptionLogger.logSoftException(
            TAG, new ReactNoCrashSoftException("Text width is invalid: " + boring.width));
        boringLayoutWidth = 0;
      }
      // Is used for single-line, boring text when the width is either unknown or bigger
      // than the width of the text.
      layout =
          BoringLayout.make(
              text,
              sTextPaintInstance,
              boringLayoutWidth,
              alignment,
              1.f,
              0.f,
              boring,
              includeFontPadding);
    } else {
      // Is used for multiline, boring text and the width is known.
      StaticLayout.Builder builder =
          StaticLayout.Builder.obtain(
                  text, 0, spanLength, sTextPaintInstance, (int) Math.ceil(width))
              .setAlignment(alignment)
              .setLineSpacing(0.f, 1.f)
              .setIncludePad(includeFontPadding)
              .setBreakStrategy(textBreakStrategy)
              .setHyphenationFrequency(hyphenationFrequency)
              .setTextDirection(
                  isScriptRTL ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        builder.setUseLineSpacingFromFallbacks(true);
      }

      layout = builder.build();
    }
    return layout;
  }

  public static Layout createLayout(
      @NonNull Context context,
      MapBuffer attributedString,
      MapBuffer paragraphAttributes,
      float width,
      float height,
      ReactTextViewManagerCallback reactTextViewManagerCallback) {
    Spannable text =
        getOrCreateSpannableForText(context, attributedString, reactTextViewManagerCallback);
    BoringLayout.Metrics boring = BoringLayout.isBoring(text, sTextPaintInstance);

    int textBreakStrategy =
        TextAttributeProps.getTextBreakStrategy(
            paragraphAttributes.getString(PA_KEY_TEXT_BREAK_STRATEGY));
    boolean includeFontPadding =
        paragraphAttributes.contains(PA_KEY_INCLUDE_FONT_PADDING)
            ? paragraphAttributes.getBoolean(PA_KEY_INCLUDE_FONT_PADDING)
            : DEFAULT_INCLUDE_FONT_PADDING;
    int hyphenationFrequency =
        TextAttributeProps.getTextBreakStrategy(
            paragraphAttributes.getString(PA_KEY_HYPHENATION_FREQUENCY));
    boolean adjustFontSizeToFit =
        paragraphAttributes.contains(PA_KEY_ADJUST_FONT_SIZE_TO_FIT)
            ? paragraphAttributes.getBoolean(PA_KEY_ADJUST_FONT_SIZE_TO_FIT)
            : DEFAULT_ADJUST_FONT_SIZE_TO_FIT;
    int maximumNumberOfLines =
        paragraphAttributes.contains(PA_KEY_MAX_NUMBER_OF_LINES)
            ? paragraphAttributes.getInt(PA_KEY_MAX_NUMBER_OF_LINES)
            : ReactConstants.UNSET;

    Layout.Alignment alignment = getTextAlignment(attributedString, text);

    if (adjustFontSizeToFit) {
      double minimumFontSize =
          paragraphAttributes.contains(PA_KEY_MINIMUM_FONT_SIZE)
              ? paragraphAttributes.getDouble(PA_KEY_MINIMUM_FONT_SIZE)
              : Double.NaN;

      adjustSpannableFontToFit(
          text,
          width,
          YogaMeasureMode.EXACTLY,
          height,
          YogaMeasureMode.UNDEFINED,
          minimumFontSize,
          maximumNumberOfLines,
          includeFontPadding,
          textBreakStrategy,
          hyphenationFrequency,
          alignment);
    }

    return createLayout(
        text,
        boring,
        width,
        YogaMeasureMode.EXACTLY,
        includeFontPadding,
        textBreakStrategy,
        hyphenationFrequency,
        alignment);
  }

  public static void adjustSpannableFontToFit(
      Spannable text,
      float width,
      YogaMeasureMode widthYogaMeasureMode,
      float height,
      YogaMeasureMode heightYogaMeasureMode,
      double minimumFontSizeAttr,
      int maximumNumberOfLines,
      boolean includeFontPadding,
      int textBreakStrategy,
      int hyphenationFrequency,
      Layout.Alignment alignment) {
    BoringLayout.Metrics boring = BoringLayout.isBoring(text, sTextPaintInstance);
    Layout layout =
        createLayout(
            text,
            boring,
            width,
            widthYogaMeasureMode,
            includeFontPadding,
            textBreakStrategy,
            hyphenationFrequency,
            alignment);

    // Minimum font size is 4pts to match the iOS implementation.
    int minimumFontSize =
        (int)
            (Double.isNaN(minimumFontSizeAttr) ? PixelUtil.toPixelFromDIP(4) : minimumFontSizeAttr);

    // Find the largest font size used in the spannable to use as a starting point.
    int currentFontSize = minimumFontSize;
    ReactAbsoluteSizeSpan[] spans = text.getSpans(0, text.length(), ReactAbsoluteSizeSpan.class);
    for (ReactAbsoluteSizeSpan span : spans) {
      currentFontSize = Math.max(currentFontSize, span.getSize());
    }

    int initialFontSize = currentFontSize;
    while (currentFontSize > minimumFontSize
        && ((maximumNumberOfLines != ReactConstants.UNSET
                && maximumNumberOfLines != 0
                && layout.getLineCount() > maximumNumberOfLines)
            || (heightYogaMeasureMode != YogaMeasureMode.UNDEFINED
                && layout.getHeight() > height))) {
      // TODO: We could probably use a smarter algorithm here. This will require 0(n)
      // measurements based on the number of points the font size needs to be reduced by.
      currentFontSize -= Math.max(1, (int) PixelUtil.toPixelFromDIP(1));

      float ratio = (float) currentFontSize / (float) initialFontSize;
      ReactAbsoluteSizeSpan[] sizeSpans =
          text.getSpans(0, text.length(), ReactAbsoluteSizeSpan.class);
      for (ReactAbsoluteSizeSpan span : sizeSpans) {
        text.setSpan(
            new ReactAbsoluteSizeSpan((int) Math.max((span.getSize() * ratio), minimumFontSize)),
            text.getSpanStart(span),
            text.getSpanEnd(span),
            text.getSpanFlags(span));
        text.removeSpan(span);
      }
      layout =
          createLayout(
              text,
              boring,
              width,
              widthYogaMeasureMode,
              includeFontPadding,
              textBreakStrategy,
              hyphenationFrequency,
              alignment);
    }
  }

  public static Layout createLayout(
    @NonNull Context context,
    MapBuffer attributedString,
    MapBuffer paragraphAttributes,
    float width,
    float height,
    ReactTextViewManagerCallback reactTextViewManagerCallback) {
    Spannable text = getOrCreateSpannableForText(context, attributedString, reactTextViewManagerCallback);
    BoringLayout.Metrics boring = BoringLayout.isBoring(text, sTextPaintInstance);

    int textBreakStrategy =
      TextAttributeProps.getTextBreakStrategy(
        paragraphAttributes.getString(PA_KEY_TEXT_BREAK_STRATEGY));
    boolean includeFontPadding =
      paragraphAttributes.contains(PA_KEY_INCLUDE_FONT_PADDING)
        ? paragraphAttributes.getBoolean(PA_KEY_INCLUDE_FONT_PADDING)
        : DEFAULT_INCLUDE_FONT_PADDING;
    int hyphenationFrequency =
      TextAttributeProps.getTextBreakStrategy(
        paragraphAttributes.getString(PA_KEY_HYPHENATION_FREQUENCY));
    boolean adjustFontSizeToFit =
      paragraphAttributes.contains(PA_KEY_ADJUST_FONT_SIZE_TO_FIT)
        ? paragraphAttributes.getBoolean(PA_KEY_ADJUST_FONT_SIZE_TO_FIT)
        : DEFAULT_ADJUST_FONT_SIZE_TO_FIT;
    int maximumNumberOfLines =
      paragraphAttributes.contains(PA_KEY_MAX_NUMBER_OF_LINES)
        ? paragraphAttributes.getInt(PA_KEY_MAX_NUMBER_OF_LINES)
        : ReactConstants.UNSET;

    Layout.Alignment alignment = getTextAlignment(attributedString, text);

    if (adjustFontSizeToFit) {
      double minimumFontSize =
        paragraphAttributes.contains(PA_KEY_MINIMUM_FONT_SIZE)
          ? paragraphAttributes.getDouble(PA_KEY_MINIMUM_FONT_SIZE)
          : Double.NaN;

      adjustSpannableFontToFit(
        text,
        width,
        YogaMeasureMode.EXACTLY,
        height,
        YogaMeasureMode.UNDEFINED,
        minimumFontSize,
        maximumNumberOfLines,
        includeFontPadding,
        textBreakStrategy,
        hyphenationFrequency,
        alignment);
    }

    return createLayout(
      text,
      boring,
      width,
      YogaMeasureMode.EXACTLY,
      includeFontPadding,
      textBreakStrategy,
      hyphenationFrequency,
      alignment);
  }

  public static long measureText(
      Context context,
      MapBuffer attributedString,
      MapBuffer paragraphAttributes,
      float width,
      YogaMeasureMode widthYogaMeasureMode,
      float height,
      YogaMeasureMode heightYogaMeasureMode,
      ReactTextViewManagerCallback reactTextViewManagerCallback,
      @Nullable float[] attachmentsPositions) {

    // TODO(5578671): Handle text direction (see View#getTextDirectionHeuristic)
    Layout layout =
        createLayout(
            context,
            attributedString,
            paragraphAttributes,
            width,
            height,
            reactTextViewManagerCallback);
    Spannable text = (Spannable) layout.getText();

    if (text == null) {
      return 0;
    }

    int maximumNumberOfLines =
        paragraphAttributes.contains(PA_KEY_MAX_NUMBER_OF_LINES)
            ? paragraphAttributes.getInt(PA_KEY_MAX_NUMBER_OF_LINES)
            : ReactConstants.UNSET;

    int calculatedLineCount =
        maximumNumberOfLines == ReactConstants.UNSET || maximumNumberOfLines == 0
            ? layout.getLineCount()
            : Math.min(maximumNumberOfLines, layout.getLineCount());

    // Instead of using `layout.getWidth()` (which may yield a significantly larger width for
    // text that is wrapping), compute width using the longest line.
    float calculatedWidth = 0;
    if (widthYogaMeasureMode == YogaMeasureMode.EXACTLY) {
      calculatedWidth = width;
    } else {
      for (int lineIndex = 0; lineIndex < calculatedLineCount; lineIndex++) {
        boolean endsWithNewLine =
            text.length() > 0 && text.charAt(layout.getLineEnd(lineIndex) - 1) == '\n';
        float lineWidth =
            endsWithNewLine ? layout.getLineMax(lineIndex) : layout.getLineWidth(lineIndex);
        if (lineWidth > calculatedWidth) {
          calculatedWidth = lineWidth;
        }
      }
      if (widthYogaMeasureMode == YogaMeasureMode.AT_MOST && calculatedWidth > width) {
        calculatedWidth = width;
      }
    }

    // Android 11+ introduces changes in text width calculation which leads to cases
    // where the container is measured smaller than text. Math.ceil prevents it
    // See T136756103 for investigation
    if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
      calculatedWidth = (float) Math.ceil(calculatedWidth);
    }

    float calculatedHeight = height;
    if (heightYogaMeasureMode != YogaMeasureMode.EXACTLY) {
      calculatedHeight = layout.getLineBottom(calculatedLineCount - 1);
      if (heightYogaMeasureMode == YogaMeasureMode.AT_MOST && calculatedHeight > height) {
        calculatedHeight = height;
      }
    }

    // Calculate the positions of the attachments (views) that will be rendered inside the
    // Spanned Text. The following logic is only executed when a text contains views inside.
    // This follows a similar logic than used in pre-fabric (see ReactTextView.onLayout method).
    int attachmentIndex = 0;
    int lastAttachmentFoundInSpan;
    for (int i = 0; i < text.length(); i = lastAttachmentFoundInSpan) {
      lastAttachmentFoundInSpan =
          text.nextSpanTransition(i, text.length(), TextInlineViewPlaceholderSpan.class);
      TextInlineViewPlaceholderSpan[] placeholders =
          text.getSpans(i, lastAttachmentFoundInSpan, TextInlineViewPlaceholderSpan.class);
      for (TextInlineViewPlaceholderSpan placeholder : placeholders) {
        int start = text.getSpanStart(placeholder);
        int line = layout.getLineForOffset(start);
        boolean isLineTruncated = layout.getEllipsisCount(line) > 0;
        // This truncation check works well on recent versions of Android (tested on 5.1.1 and
        // 6.0.1) but not on Android 4.4.4. The reason is that getEllipsisCount is buggy on
        // Android 4.4.4. Specifically, it incorrectly returns 0 if an inline view is the
        // first thing to be truncated.
        if (!(isLineTruncated && start >= layout.getLineStart(line) + layout.getEllipsisStart(line))
            || start >= layout.getLineEnd(line)) {
          float placeholderWidth = placeholder.getWidth();
          float placeholderHeight = placeholder.getHeight();
          // Calculate if the direction of the placeholder character is Right-To-Left.
          boolean isRtlChar = layout.isRtlCharAt(start);
          boolean isRtlParagraph = layout.getParagraphDirection(line) == Layout.DIR_RIGHT_TO_LEFT;
          float placeholderLeftPosition;
          // There's a bug on Samsung devices where calling getPrimaryHorizontal on
          // the last offset in the layout will result in an endless loop. Work around
          // this bug by avoiding getPrimaryHorizontal in that case.
          if (start == text.length() - 1) {
            boolean endsWithNewLine =
                text.length() > 0 && text.charAt(layout.getLineEnd(line) - 1) == '\n';
            float lineWidth = endsWithNewLine ? layout.getLineMax(line) : layout.getLineWidth(line);
            placeholderLeftPosition =
                isRtlParagraph
                    // Equivalent to `layout.getLineLeft(line)` but `getLineLeft` returns
                    // incorrect
                    // values when the paragraph is RTL and `setSingleLine(true)`.
                    ? calculatedWidth - lineWidth
                    : layout.getLineRight(line) - placeholderWidth;
          } else {
            // The direction of the paragraph may not be exactly the direction the string is
            // heading
            // in at the
            // position of the placeholder. So, if the direction of the character is the same
            // as the
            // paragraph
            // use primary, secondary otherwise.
            boolean characterAndParagraphDirectionMatch = isRtlParagraph == isRtlChar;
            placeholderLeftPosition =
                characterAndParagraphDirectionMatch
                    ? layout.getPrimaryHorizontal(start)
                    : layout.getSecondaryHorizontal(start);
            if (isRtlParagraph && !isRtlChar) {
              // Adjust `placeholderLeftPosition` to work around an Android bug.
              // The bug is when the paragraph is RTL and `setSingleLine(true)`, some layout
              // methods such as `getPrimaryHorizontal`, `getSecondaryHorizontal`, and
              // `getLineRight` return incorrect values. Their return values seem to be off
              // by the same number of pixels so subtracting these values cancels out the
              // error.
              //
              // The result is equivalent to bugless versions of
              // `getPrimaryHorizontal`/`getSecondaryHorizontal`.
              placeholderLeftPosition =
                  calculatedWidth - (layout.getLineRight(line) - placeholderLeftPosition);
            }
            if (isRtlChar) {
              placeholderLeftPosition -= placeholderWidth;
            }
          }
          // Vertically align the inline view to the baseline of the line of text.
          float placeholderTopPosition = layout.getLineBaseline(line) - placeholderHeight;
          int attachmentPosition = attachmentIndex * 2;

          // The attachment array returns the positions of each of the attachments as
          attachmentsPositions[attachmentPosition] =
              PixelUtil.toDIPFromPixel(placeholderTopPosition);
          attachmentsPositions[attachmentPosition + 1] =
              PixelUtil.toDIPFromPixel(placeholderLeftPosition);
          attachmentIndex++;
        }
      }
    }

    float widthInSP = PixelUtil.toDIPFromPixel(calculatedWidth);
    float heightInSP = PixelUtil.toDIPFromPixel(calculatedHeight);

    if (ENABLE_MEASURE_LOGGING) {
      FLog.e(
          TAG,
          "TextMeasure call ('"
              + text
              + "'): w: "
              + calculatedWidth
              + " px - h: "
              + calculatedHeight
              + " px - w : "
              + widthInSP
              + " sp - h: "
              + heightInSP
              + " sp");
    }

    return YogaMeasureOutput.make(widthInSP, heightInSP);
  }

  public static WritableArray measureLines(
      @NonNull Context context,
      MapBuffer attributedString,
      MapBuffer paragraphAttributes,
      float width,
      float height) {

    Layout layout =
        createLayout(context, attributedString, paragraphAttributes, width, height, null);
    return FontMetricsUtil.getFontMetrics(layout.getText(), layout, sTextPaintInstance, context);
  }

  public static float getLastBaseline(
    @NonNull Context context,
    MapBuffer attributedString,
    MapBuffer paragraphAttributes,
    float width,
    float height) {

    Layout layout = doTextStuff(context, attributedString, paragraphAttributes, width, height, null);

    float maxDescent = 0;
    for (int i = 0; i < layout.getLineCount(); i++) {
      if (maxDescent < layout.getLineDescent(i)) {
        maxDescent = layout.getLineDescent(i);
      }
    }

    return PixelUtil.toDIPFromPixel(height - maxDescent);
  }
}
