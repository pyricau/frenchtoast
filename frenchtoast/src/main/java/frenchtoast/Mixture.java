package frenchtoast;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.support.annotation.MainThread;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import static android.content.Context.WINDOW_SERVICE;
import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static frenchtoast.ToastInternals.assertMainThread;

public final class Mixture {

  /**
   * Creates a {@link Mixture} from a {@link Toast}. Never call {@link Toast#show()} or {@link
   * Toast#cancel()} directly on that passed in {@link Toast}.
   */
  public static Mixture dip(Toast toast) {
    if (toast.getView() == null) {
      throw new NullPointerException("Toast should have a view already set.");
    }
    return new Mixture(toast);
  }

  private final Toast toast;
  private final WindowManager.LayoutParams params;

  private Mixture(Toast toast) {
    assertMainThread();
    this.toast = toast;
    this.params = buildLayoutParams();
  }

  public void toggle() {
    if (isShowing()) {
      hide();
    } else {
      show();
    }
  }

  @MainThread public void show() {
    assertMainThread();
    View view = toast.getView();
    if (view == null) {
      throw new IllegalStateException("Can't show a Toast with no View.");
    }

    Context context = toast.getView().getContext();

    WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);

    // We can resolve the Gravity here by using the Locale for getting
    // the layout direction
    Configuration config = view.getContext().getResources().getConfiguration();
    int gravity = toast.getGravity();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      gravity = Gravity.getAbsoluteGravity(gravity, config.getLayoutDirection());
    }
    params.gravity = gravity;
    if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL) {
      params.horizontalWeight = 1.0f;
    }
    if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL) {
      params.verticalWeight = 1.0f;
    }
    params.x = toast.getXOffset();
    params.y = toast.getYOffset();
    params.verticalMargin = toast.getVerticalMargin();
    params.horizontalMargin = toast.getHorizontalMargin();
    params.packageName = context.getPackageName();
    if (view.getParent() != null) {
      windowManager.removeView(view);
    }
    windowManager.addView(view, params);
    trySendAccessibilityEvent(view);
  }

  @MainThread public void hide() {
    assertMainThread();
    View view = toast.getView();
    if (view == null) {
      return;
    }
    if (view.getParent() != null) {
      Context context = toast.getView().getContext();
      WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
      windowManager.removeView(view);
    }
  }

  @MainThread public boolean isShowing() {
    assertMainThread();
    View view = toast.getView();
    return view != null && view.getParent() != null;
  }

  private WindowManager.LayoutParams buildLayoutParams() {
    WindowManager.LayoutParams params = new WindowManager.LayoutParams();
    params.height = WindowManager.LayoutParams.WRAP_CONTENT;
    params.width = WindowManager.LayoutParams.WRAP_CONTENT;
    params.format = PixelFormat.TRANSLUCENT;
    params.windowAnimations = android.R.style.Animation_Toast;
    params.type = WindowManager.LayoutParams.TYPE_TOAST;
    params.setTitle("Toast");
    params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
    return params;
  }

  private void trySendAccessibilityEvent(View view) {
    Context context = view.getContext();
    AccessibilityManager accessibilityManager =
        (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
    if (!accessibilityManager.isEnabled()) {
      return;
    }
    // treat toasts as notifications since they are used to
    // announce a transient piece of information to the user
    AccessibilityEvent event = AccessibilityEvent.obtain(TYPE_NOTIFICATION_STATE_CHANGED);
    event.setClassName(getClass().getName());
    event.setPackageName(context.getPackageName());
    view.dispatchPopulateAccessibilityEvent(event);
    accessibilityManager.sendAccessibilityEvent(event);
  }
}
