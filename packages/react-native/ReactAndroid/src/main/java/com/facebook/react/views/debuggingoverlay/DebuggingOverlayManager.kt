/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.views.debuggingoverlay

import android.graphics.RectF
import com.facebook.react.bridge.NoSuchKeyException
import com.facebook.react.bridge.ReactNoCrashSoftException
import com.facebook.react.bridge.ReactSoftExceptionLogger
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.UnexpectedNativeTypeException
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.PixelUtil
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext

@ReactModule(name = DebuggingOverlayManager.REACT_CLASS)
public class DebuggingOverlayManager : SimpleViewManager<DebuggingOverlay>() {
  override fun receiveCommand(view: DebuggingOverlay, commandId: String, args: ReadableArray?) {
    when (commandId) {
      "highlightTraceUpdates" -> {
        if (args == null) {
          return
        }

        val providedTraceUpdates = args.getArray(0)!!
        val formattedTraceUpdates = mutableListOf<TraceUpdate>()

        var successfullyParsedPayload = true
        for (i in 0 until providedTraceUpdates.size()) {
          val traceUpdate = providedTraceUpdates.getMap(i)!!
          val serializedRectangle = traceUpdate.getMap("rectangle")
          if (serializedRectangle == null) {
            ReactSoftExceptionLogger.logSoftException(
                REACT_CLASS,
                ReactNoCrashSoftException(
                    "Unexpected payload for highlighting trace updates: rectangle field is" +
                        " null"))
            successfullyParsedPayload = false
            break
          }

          val id = traceUpdate.getInt("id")
          val color = traceUpdate.getInt("color")

          try {
            val left = serializedRectangle.getDouble("x").toFloat()
            val top = serializedRectangle.getDouble("y").toFloat()
            val right = (left + serializedRectangle.getDouble("width")).toFloat()
            val bottom = (top + serializedRectangle.getDouble("height")).toFloat()

            val rectangle =
                RectF(
                    PixelUtil.toPixelFromDIP(left),
                    PixelUtil.toPixelFromDIP(top),
                    PixelUtil.toPixelFromDIP(right),
                    PixelUtil.toPixelFromDIP(bottom))

            formattedTraceUpdates.add(TraceUpdate(id, rectangle, color))
          } catch (ex: Exception) {
            when (ex) {
              is NoSuchKeyException,
              is UnexpectedNativeTypeException -> {
                ReactSoftExceptionLogger.logSoftException(
                    REACT_CLASS,
                    ReactNoCrashSoftException(
                        "Unexpected payload for highlighting trace updates: rectangle field should" +
                            " have x, y, width, height fields"))
                successfullyParsedPayload = false
              }
              else -> throw ex
            }
          }
        }

        if (successfullyParsedPayload) {
          view.setTraceUpdates(formattedTraceUpdates)
        }
      }
      "highlightElements" -> {
        if (args == null) {
          return
        }

        val providedElements = args.getArray(0)!!
        val elementsRectangles = mutableListOf<RectF>()

        var successfullyParsedPayload = true
        for (i in 0 until providedElements.size()) {
          val element = providedElements.getMap(i)!!

          try {
            val left = element.getDouble("x").toFloat()
            val top = element.getDouble("y").toFloat()
            val right = (left + element.getDouble("width")).toFloat()
            val bottom = (top + element.getDouble("height")).toFloat()
            val rect =
                RectF(
                    PixelUtil.toPixelFromDIP(left),
                    PixelUtil.toPixelFromDIP(top),
                    PixelUtil.toPixelFromDIP(right),
                    PixelUtil.toPixelFromDIP(bottom))

            elementsRectangles.add(rect)
          } catch (ex: Exception) {
            when (ex) {
              is NoSuchKeyException,
              is UnexpectedNativeTypeException -> {
                ReactSoftExceptionLogger.logSoftException(
                    REACT_CLASS,
                    ReactNoCrashSoftException(
                        "Unexpected payload for highlighting elements: every element should have x," +
                            " y, width, height fields"))
                successfullyParsedPayload = false
              }
              else -> throw ex
            }
          }
        }

        if (successfullyParsedPayload) {
          view.setHighlightedElementsRectangles(elementsRectangles)
        }
      }
      "clearElementsHighlights" -> {
        view.clearElementsHighlights()
      }
      else -> {
        ReactSoftExceptionLogger.logSoftException(
            REACT_CLASS,
            ReactNoCrashSoftException("Received unexpected command in DebuggingOverlayManager"))
      }
    }
  }

  public override fun createViewInstance(context: ThemedReactContext): DebuggingOverlay {
    return DebuggingOverlay(context)
  }

  public override fun getName(): String {
    return REACT_CLASS
  }

  public companion object {
    public const val REACT_CLASS: String = "DebuggingOverlay"
  }
}
