/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.uimanager

import android.annotation.TargetApi
import android.graphics.BlendMode

@TargetApi(29)
internal object MixBlendModeHelper {

  // https://www.w3.org/TR/compositing-1/#mix-blend-mode
  @JvmStatic
  public fun parseMixBlendMode(mixBlendMode: String?): BlendMode? {
    mixBlendMode ?: return null

    val blendMode: BlendMode?
    when (mixBlendMode) {
      "normal" -> blendMode = null
      "multiply" -> blendMode = BlendMode.MULTIPLY
      "screen" -> blendMode = BlendMode.SCREEN
      "overlay" -> blendMode = BlendMode.OVERLAY
      "darken" -> blendMode = BlendMode.DARKEN
      "lighten" -> blendMode = BlendMode.LIGHTEN
      "color-dodge" -> blendMode = BlendMode.COLOR_DODGE
      "color-burn" -> blendMode = BlendMode.COLOR_BURN
      "hard-light" -> blendMode = BlendMode.HARD_LIGHT
      "soft-light" -> blendMode = BlendMode.SOFT_LIGHT
      "difference" -> blendMode = BlendMode.DIFFERENCE
      "exclusion" -> blendMode = BlendMode.EXCLUSION
      "hue" -> blendMode = BlendMode.HUE
      "saturation" -> blendMode = BlendMode.SATURATION
      "color" -> blendMode = BlendMode.COLOR
      "luminosity" -> blendMode = BlendMode.LUMINOSITY
      else -> throw IllegalArgumentException("Invalid mix-blend-mode name: $mixBlendMode")
    }

    return blendMode
  }
}
