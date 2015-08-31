package frenchtoast;

import java.util.concurrent.TimeUnit;

public interface ToastQueue {
  void enqueueLong(FrenchToast frenchToast);

  void enqueueShort(FrenchToast frenchToast);

  void enqueue(FrenchToast frenchToast, long duration, TimeUnit timeUnit);

  boolean cancel(FrenchToast canceledToast);

  void clear();
}
