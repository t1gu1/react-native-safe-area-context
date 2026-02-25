package com.th3rdwave.safeareacontext

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import com.facebook.react.bridge.Arguments
import com.facebook.react.uimanager.StateWrapper
import com.facebook.react.uimanager.UIManagerModule
import com.facebook.react.views.view.ReactViewGroup
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val MAX_WAIT_TIME_NANO = 500000000L // 500ms

class SafeAreaView(context: Context?) :
    ReactViewGroup(context), ViewTreeObserver.OnPreDrawListener {
  private var mMode = SafeAreaViewMode.PADDING
  private var mInsets: EdgeInsets? = null
  private var mEdges: SafeAreaViewEdges? = null
  private var mProviderView: View? = null
  private var mStateWrapper: StateWrapper? = null

  fun getStateWrapper(): StateWrapper? {
    return mStateWrapper
  }

  fun setStateWrapper(stateWrapper: StateWrapper?) {
    mStateWrapper = stateWrapper
  }

  private fun updateInsets() {
    val insets = mInsets
    if (insets != null) {
      val edges =
          mEdges
              ?: SafeAreaViewEdges(
                  SafeAreaViewEdgeModes.ADDITIVE,
                  SafeAreaViewEdgeModes.ADDITIVE,
                  SafeAreaViewEdgeModes.ADDITIVE,
                  SafeAreaViewEdgeModes.ADDITIVE)
      val stateWrapper = getStateWrapper()
      if (stateWrapper != null) {
        val map = Arguments.createMap()
        map.putMap("insets", edgeInsetsToJsMap(insets))
        stateWrapper.updateState(map)
      } else {
        val localData = SafeAreaViewLocalData(insets = insets, mode = mMode, edges = edges)
        val reactContext = getReactContext(this)
        val uiManager = reactContext.getNativeModule(UIManagerModule::class.java)
        if (uiManager != null) {
          uiManager.setViewLocalData(id, localData)
        }
      }
    }
  }

  fun setMode(mode: SafeAreaViewMode) {
    mMode = mode
    maybeUpdateInsets()
  }

  fun setEdges(edges: SafeAreaViewEdges) {
    mEdges = edges
    maybeUpdateInsets()
  }

  private fun maybeUpdateInsets() {
    if (!isAttachedToWindow) {
      return
    }
    val providerView = mProviderView ?: return
    val edgeInsets = getSafeAreaInsets(providerView) ?: return
    if (mInsets != edgeInsets) {
      mInsets = edgeInsets
      updateInsets()
    }
  }

  private fun findProvider(): View {
    var current = parent
    while (current != null) {
      if (current is SafeAreaProvider) {
        return current
      }
      current = current.parent
    }
    return this
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    mProviderView = findProvider()
    mProviderView?.viewTreeObserver?.addOnPreDrawListener(this)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    mProviderView?.viewTreeObserver?.removeOnPreDrawListener(this)
    mProviderView = null
  }

  override fun onPreDraw(): Boolean {
    maybeUpdateInsets()
    return true
  }

  override fun draw(canvas: Canvas) {
    try {
      super.draw(canvas)
    } catch (e: Exception) {
      // This is a workaround for a React Native bug where the view hierarchy can be inconsistent
      // during draw.
    }
  }

  override fun dispatchDraw(canvas: Canvas) {
    try {
      super.dispatchDraw(canvas)
    } catch (e: Exception) {
      // This is a workaround for a React Native bug where the view hierarchy can be inconsistent
      // during draw.
    }
  }
}
