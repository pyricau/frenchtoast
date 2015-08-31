package frenchtoast;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static frenchtoast.ToastInternals.MAIN_HANDLER;
import static frenchtoast.ToastInternals.assertMainThread;
import static frenchtoast.ToastInternals.checkNotNull;

public final class ActivityToasts {

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

  public static ToastQueue with(Context context) {
    assertMainThread();
    checkNotNull(context, "context");
    Activity activity = unwrapActivity(context);
    return queueHolder.getOrCreateActivityToastQueue(activity);
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

    ToastQueue getOrCreateActivityToastQueue(Activity activity) {
      Holder holder = createdActivities.get(activity);
      if (holder == null) {
        throw new NullPointerException("Unknown activity "
            + activity
            + ", make sure it's not destroyed "
            + " and that you did not forget to call ActivityToasts.install() "
            + "from Application.onCreate()");
      }

      if (holder.queueOrNull == null) {
        LifecycleToastQueue toastQueue = new LifecycleToastQueue();
        if (holder.paused) {
          toastQueue.pause();
        }
        holder.queueOrNull = toastQueue;
      }

      return holder.queueOrNull;
    }
  }

  private ActivityToasts() {
    throw new AssertionError();
  }
}
