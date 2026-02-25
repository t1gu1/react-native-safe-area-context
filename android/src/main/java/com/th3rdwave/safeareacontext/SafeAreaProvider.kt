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

  private fun maybeUpdateInsets() {
    val insetsChangeHandler = mInsetsChangeHandler ?: return
    val edgeInsets = getSafeAreaInsets(this) ?: return
    val rootView = rootView as? ViewGroup ?: return
    val frame = getFrame(rootView, this) ?: return
    if (mLastInsets != edgeInsets || mLastFrame != frame) {
      mLastInsets = edgeInsets
      mLastFrame = frame
      // Defer the handler call to the next frame to avoid modifying the view
      // hierarchy during a layout or draw pass.
      post { insetsChangeHandler(this, edgeInsets, frame) }
    }
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
    maybeUpdateInsets()
    return true
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
