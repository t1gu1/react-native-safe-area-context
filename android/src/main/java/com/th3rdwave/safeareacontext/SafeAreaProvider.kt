package com.th3rdwave.safeareacontext

import android.content.Context
import android.graphics.Canvas
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.facebook.react.views.view.ReactViewGroup

typealias OnInsetsChangeHandler = (view: SafeAreaProvider, insets: EdgeInsets, frame: Rect) -> Unit

class SafeAreaProvider(context: Context?) :
    ReactViewGroup(context), ViewTreeObserver.OnPreDrawListener {
  private var mInsetsChangeHandler: OnInsetsChangeHandler? = null
  private var mLastInsets: EdgeInsets? = null
  private var mLastFrame: Rect? = null

  private fun maybeUpdateInsets(): Boolean {
    val insetsChangeHandler = mInsetsChangeHandler ?: return false
    val edgeInsets = getSafeAreaInsets(this) ?: return false
    val rootView = rootView as? ViewGroup ?: return false
    val frame = getFrame(rootView, this) ?: return false
    if (mLastInsets != edgeInsets || mLastFrame != frame) {
      mLastInsets = edgeInsets
      mLastFrame = frame
      insetsChangeHandler(this, edgeInsets, frame)
      return true
    }
    return false
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    viewTreeObserver.addOnPreDrawListener(this)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    viewTreeObserver.removeOnPreDrawListener(this)
  }

  override fun onPreDraw(): Boolean {
    val didUpdate = maybeUpdateInsets()
    if (didUpdate) {
      requestLayout()
    }
    return !didUpdate
  }

  override fun dispatchDraw(canvas: Canvas) {
    try {
      super.dispatchDraw(canvas)
    } catch (e: IllegalStateException) {
      // This is a workaround for a React Native bug where the view hierarchy can be inconsistent
      // during draw.
    }
  }

  fun setOnInsetsChangeHandler(handler: OnInsetsChangeHandler?) {
    mInsetsChangeHandler = handler
    requestLayout()
  }
}
