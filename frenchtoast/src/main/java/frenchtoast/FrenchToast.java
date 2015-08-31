package frenchtoast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static android.widget.Toast.LENGTH_SHORT;
import static frenchtoast.ToastInternals.MAIN_HANDLER;
import static frenchtoast.ToastInternals.assertMainThread;
import static frenchtoast.ToastInternals.checkNotNull;

public final class FrenchToast {

  private static QueueHolder queueHolder;

  public static void install(Application application) {
    assertMainThread();
    checkNotNull(application, "application");
    if (queueHolder != null) {
      throw new IllegalStateException("Already installed.");
    }
    queueHolder = new QueueHolder();
    application.registerActivityLifecycleCallbacks(queueHolder);
  }

  public static SmartToaster with(Context context) {
    assertMainThread();
    checkNotNull(context, "context");
    Activity activity = unwrapActivity(context);
    return new ActivityToaster(activity);
  }

  private static Activity unwrapActivity(Context context) {
    Context appContext = context.getApplicationContext();
    Context unwrapped = context;
    for (; ; ) {
      if (unwrapped instanceof Activity) {
        return (Activity) unwrapped;
      }
      if (unwrapped == null || unwrapped == appContext || !(unwrapped instanceof ContextWrapper)) {
        throw new IllegalArgumentException(
            "Could not find Activity in the chain of wrapped contexts from " + context);
      }
      Context baseContext = ((ContextWrapper) unwrapped).getBaseContext();
      if (baseContext == unwrapped) {
        throw new IllegalArgumentException(
            "Could not find Activity in the chain of wrapped contexts from " + context);
      }
      unwrapped = baseContext;
    }
  }

  static final class Holder {
    boolean paused;
    LifecycleToastQueue queueOrNull;
    String savedUniqueId;
  }

  static final class ActivityToaster implements SmartToaster {

  /*
   * Constants based on https://github.com/android/platform_frameworks_base/blob/master/services/
   * core/java/com/android/server/notification/NotificationManagerService.java#L140
   */

    private static final int ANDROID_LONG_DELAY_MS = 3_500;
    private static final int ANDROID_SHORT_DELAY_MS = 2_000;
    private static final int IGNORED = LENGTH_SHORT;

    private final Activity activity;
    private long durationMs = ANDROID_LONG_DELAY_MS;

    ActivityToaster(Activity activity) {
      this.activity = activity;
    }

    @Override public Toaster shortLength() {
      assertMainThread();
      durationMs = ANDROID_SHORT_DELAY_MS;
      return this;
    }

    @Override public Toaster longLength() {
      assertMainThread();
      durationMs = ANDROID_LONG_DELAY_MS;
      return this;
    }

    @Override public Toaster length(long duration, TimeUnit timeUnit) {
      assertMainThread();
      durationMs = timeUnit.toMillis(duration);
      return this;
    }

    @Override public void clear() {
      assertMainThread();
      queueHolder.clear(activity);
    }

    @Override public Toasted showText(CharSequence text) {
      @SuppressLint("ShowToast") Toast toast =
          Toast.makeText(activity.getApplicationContext(), text, IGNORED);
      return showDipped(toast);
    }

    @Override public Toasted showText(int stringResId) {
      @SuppressLint("ShowToast") Toast toast =
          Toast.makeText(activity.getApplicationContext(), stringResId, IGNORED);
      return showDipped(toast);
    }

    @Override public Toasted showLayout(int layoutResId) {
      Context context = activity.getApplicationContext();
      View view = LayoutInflater.from(context).inflate(layoutResId, null);
      Toast toast = new Toast(context);
      toast.setView(view);
      return showDipped(toast);
    }

    @Override public Toasted showDipped(Toast toast) {
      assertMainThread();
      Mixture mixture = Mixture.dip(toast);
      ToastQueue queue = queueHolder.getOrCreateActivityToastQueue(activity);
      queue.enqueue(mixture, durationMs);
      return new Toasted(queue, mixture);
    }
  }

  static final class QueueHolder extends ActivityLifecycleCallbacksAdapter {

    private static final String FRENCH_TOAST_ACTIVITY_UNIQUE_ID = "FRENCH_TOAST_ACTIVITY_UNIQUE_ID";
    final Map<Activity, Holder> createdActivities = new LinkedHashMap<>();
    final Map<String, LifecycleToastQueue> retainedQueues = new LinkedHashMap<>();

    final Runnable clearRetainedQueues = new Runnable() {
      @Override public void run() {
        clearRetainedQueues();
      }
    };

    @Override public void onActivityPaused(Activity activity) {
      Holder holder = createdActivities.get(activity);
      holder.paused = true;
      if (holder.queueOrNull != null) {
        holder.queueOrNull.pause();
      }
    }

    @Override public void onActivityResumed(Activity activity) {
      Holder holder = createdActivities.get(activity);
      holder.paused = false;
      if (holder.queueOrNull != null) {
        holder.queueOrNull.resume();
      }
      holder.savedUniqueId = null;
    }

    @Override public void onActivityDestroyed(Activity activity) {
      Holder holder = createdActivities.remove(activity);
      if (holder.queueOrNull == null) {
        return;
      }
      if (activity.isChangingConfigurations() && holder.savedUniqueId != null) {
        retainedQueues.put(holder.savedUniqueId, holder.queueOrNull);
        // onCreate() is always called from the same message as the previous onDestroy().
        MAIN_HANDLER.post(clearRetainedQueues);
      } else {
        holder.queueOrNull.clear();
      }
    }

    private void clearRetainedQueues() {
      for (LifecycleToastQueue queue : retainedQueues.values()) {
        queue.clear();
      }
      retainedQueues.clear();
    }

    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
      Holder holder = createdActivities.get(activity);
      String activityUniqueId = UUID.randomUUID().toString();
      outState.putString(FRENCH_TOAST_ACTIVITY_UNIQUE_ID, activityUniqueId);
      holder.savedUniqueId = activityUniqueId;
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      Holder holder = new Holder();
      createdActivities.put(activity, holder);
      if (!retainedQueues.isEmpty()) {
        String uniqueId = savedInstanceState.getString(FRENCH_TOAST_ACTIVITY_UNIQUE_ID);
        if (uniqueId != null) {
          holder.queueOrNull = retainedQueues.remove(uniqueId);
        }
      }
    }

    void clear(Activity activity) {
      Holder holder = getHolderOrThrow(activity);
      if (holder.queueOrNull != null) {
        holder.queueOrNull.clear();
      }
    }

    ToastQueue getOrCreateActivityToastQueue(Activity activity) {
      Holder holder = getHolderOrThrow(activity);
      if (holder.queueOrNull == null) {
        LifecycleToastQueue toastQueue = new LifecycleToastQueue();
        if (holder.paused) {
          toastQueue.pause();
        }
        holder.queueOrNull = toastQueue;
      }

      return holder.queueOrNull;
    }

    private Holder getHolderOrThrow(Activity activity) {
      Holder holder = createdActivities.get(activity);
      if (holder == null) {
        throw new NullPointerException("Unknown activity "
            + activity
            + ", make sure it's not destroyed "
            + " and that you did not forget to call ActivityToasts.install() "
            + "from Application.onCreate()");
      }
      return holder;
    }
  }

  private FrenchToast() {
    throw new AssertionError();
  }
}
