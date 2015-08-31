package frenchtoast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import static android.content.Context.WINDOW_SERVICE;
import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static frenchtoast.ToastInternals.assertMainThread;

public final class FrenchToast {

  /**
   * Make a standard toast that just contains a text view.
   *
   * @param context The context to use.  Usually your {@link android.app.Application}
   * or {@link android.app.Activity} object.
   * @param text The text to show.  Can be formatted text.
   */
  @SuppressLint("ShowToast") public static FrenchToast makeText(Context context,
      CharSequence text) {
    return dip(Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT));
  }

  /**
   * Make a standard toast that just contains a text view with the text from a resource.
   *
   * @param context The context to use.  Usually your {@link android.app.Application}
   * or {@link android.app.Activity} object.
   * @param stringResId The resource id of the string resource to use.  Can be formatted text.
   * @throws Resources.NotFoundException if the resource can't be found.
   */
  @SuppressLint("ShowToast") public static FrenchToast makeText(Context context, int stringResId) {
    return dip(Toast.makeText(context.getApplicationContext(), stringResId, Toast.LENGTH_SHORT));
  }

  public static FrenchToast makeLayout(Context context, int layoutResId) {
    context = context.getApplicationContext();
    View view = LayoutInflater.from(context).inflate(layoutResId, null);
    Toast toast = new Toast(context);
    toast.setView(view);
    return dip(toast);
  }

  /**
   * Creates a {@link FrenchToast} from a {@link Toast}. Never call {@link Toast#show()} or {@link
   * Toast#cancel()} directly on that passed in {@link Toast}.
   */
  public static FrenchToast dip(Toast toast) {
    if (toast.getView() == null) {
      throw new NullPointerException("Toast should have a view already set.");
    }
    return new FrenchToast(toast, toast.getView().getContext());
  }

  public static FrenchToast create(Context context) {
    context = context.getApplicationContext();
    return new FrenchToast(new Toast(context), context);
  }

  private final Toast toast;
  private final WindowManager.LayoutParams params;
  private final Context context;

  private FrenchToast(Toast toast, Context context) {
    assertMainThread();
    this.context = context;
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

  public void show() {
    assertMainThread();
    View view = toast.getView();
    if (view == null) {
      throw new IllegalStateException("Can't show a Toast with no View.");
    }
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

  public void hide() {
    assertMainThread();
    View view = toast.getView();
    if (view == null) {
      return;
    }
    if (view.getParent() != null) {
      WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
      windowManager.removeView(view);
    }
  }

  public Toast getWrappedToast() {
    assertMainThread();
    return toast;
  }

  public boolean isShowing() {
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
