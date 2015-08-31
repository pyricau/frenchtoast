package frenchtoast;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

import static frenchtoast.ToastInternals.MAIN_HANDLER;
import static frenchtoast.ToastInternals.assertMainThread;

/**
 * A queue on which you can enqueue Toasts to show with a specific duration. One Toast
 * is shown at a time. You can tie this queue to a lifecycle (e.g. an activity) so that
 * the toasts are only displayed in the relevant context. If a Toast is displayed when the queue is
 * paused, then it's duration will be reset and it will be shown for its whole duration again when
 * the queue gets resumed. The queue is resumed by default.
 */
public final class LifecycleToastQueue implements ToastQueue {

  static final class EnqueuedToast {
    final FrenchToast frenchToast;
    final long durationMs;

    EnqueuedToast(FrenchToast frenchToast, long durationMs) {
      this.frenchToast = frenchToast;
      this.durationMs = durationMs;
    }

    @Override public boolean equals(Object other) {
      if (!(other instanceof EnqueuedToast)) {
        return false;
      }
      EnqueuedToast that = (EnqueuedToast) other;
      return frenchToast == that.frenchToast;
    }

    @Override public int hashCode() {
      return System.identityHashCode(frenchToast);
    }
  }

  /*
   * Constants based on https://github.com/android/platform_frameworks_base/blob/master/services/
   * core/java/com/android/server/notification/NotificationManagerService.java#L140
   */

  private static final int ANDROID_LONG_DELAY_MS = 3_500;
  private static final int ANDROID_SHORT_DELAY_MS = 2_000;

  private final Deque<EnqueuedToast> toastDeque = new ArrayDeque<>();

  private final Runnable hideToast = new Runnable() {
    @Override public void run() {
      hideToast();
    }
  };

  private boolean paused;

  public LifecycleToastQueue() {
    assertMainThread();
  }

  @Override public void enqueueLong(FrenchToast frenchToast) {
    enqueue(frenchToast, ANDROID_LONG_DELAY_MS);
  }

  @Override public void enqueueShort(FrenchToast frenchToast) {
    enqueue(frenchToast, ANDROID_SHORT_DELAY_MS);
  }

  @Override public void enqueue(FrenchToast frenchToast, long duration, TimeUnit timeUnit) {
    enqueue(frenchToast, timeUnit.toMillis(duration));
  }

  @Override public void clear() {
    assertMainThread();
    if (toastDeque.isEmpty()) {
      return;
    }
    if (!paused) {
      EnqueuedToast firstToast = toastDeque.getFirst();
      firstToast.frenchToast.hide();
      MAIN_HANDLER.removeCallbacks(hideToast);
    }
    toastDeque.clear();
  }

  @Override public boolean cancel(FrenchToast canceledToast) {
    assertMainThread();
    if (toastDeque.isEmpty()) {
      return false;
    }

    boolean showNext = false;

    if (!paused) {
      EnqueuedToast firstToast = toastDeque.getFirst();
      if (firstToast.frenchToast == canceledToast) {
        firstToast.frenchToast.hide();
        MAIN_HANDLER.removeCallbacks(hideToast);
        showNext = true;
      }
    }

    EnqueuedToast toDelete = new EnqueuedToast(canceledToast, 0);
    boolean removed = toastDeque.remove(toDelete);

    if (showNext) {
      showFirstToast();
    }

    return removed;
  }

  public void pause() {
    assertMainThread();
    if (paused) {
      return;
    }
    paused = true;
    if (toastDeque.isEmpty()) {
      return;
    }
    EnqueuedToast firstToast = toastDeque.getFirst();
    firstToast.frenchToast.hide();
    MAIN_HANDLER.removeCallbacks(hideToast);
  }

  public void resume() {
    assertMainThread();
    if (!paused) {
      return;
    }
    paused = false;
    showFirstToast();
  }

  private void enqueue(FrenchToast frenchToast, long durationMs) {
    assertMainThread();
    boolean empty = toastDeque.isEmpty();
    toastDeque.add(new EnqueuedToast(frenchToast, durationMs));
    if (empty) {
      showFirstToast();
    }
  }

  private void showFirstToast() {
    if (toastDeque.isEmpty() || paused) {
      return;
    }
    EnqueuedToast currentToast = toastDeque.getFirst();
    currentToast.frenchToast.show();
    MAIN_HANDLER.postDelayed(hideToast, currentToast.durationMs);
  }

  private void hideToast() {
    EnqueuedToast currentToast = toastDeque.removeFirst();
    currentToast.frenchToast.hide();
    showFirstToast();
  }
}
