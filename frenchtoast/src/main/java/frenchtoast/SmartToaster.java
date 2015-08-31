package frenchtoast;

import java.util.concurrent.TimeUnit;

public interface SmartToaster extends Toaster {

  Toaster shortLength();

  Toaster longLength();

  Toaster length(long duration, TimeUnit timeUnit);

  void clear();
}
