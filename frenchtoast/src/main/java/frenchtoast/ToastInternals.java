package frenchtoast;

import android.os.Handler;
import android.os.Looper;

final class ToastInternals {

  static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  static void assertMainThread() {
    if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
      throw new UnsupportedOperationException(
          "Should be called from main thread, not " + Thread.currentThread());
    }
  }

  /**
   * Returns instance unless it's null.
   *
   * @throws NullPointerException if instance is null
   */
  static <T> T checkNotNull(T instance, String name) {
    if (instance == null) {
      throw new NullPointerException(name + " must not be null");
    }
    return instance;
  }

  private ToastInternals() {
    throw new AssertionError();
  }
}
