# FrenchToast

*Stale Android Toasts made tasty.*

Android Toasts are amazing, but they have two major drawbacks:

* You cannot control **when they show up** as well as their **duration**. Other apps can enqueue toasts that will delay yours from showing up. 
* They [break context](http://cyrilmottier.com/2012/07/24/the-making-of-prixing-4-activity-tied-notifications/): they remain on screen when the user switches to other activities.

FrenchToast gives you absolute control over your app Toasts. It does so by duplicating the internals of Android Toasts and giving you access.

Unlike other *Toast-like* libraries, FrenchToast doesn't add a view to the root of your activity. Instead, it creates a new Window for each Toast, exactly like the real Android Toasts.

## Setup

In your `build.gradle`:

```gradle
 dependencies {
   compile 'info.piwai.frenchtoast:frenchtoast:TODO'
 }
```

## Crafting Bespoke French Toasts

FrenchToast API is similar to Toast:

```java
FrenchToast toast = FrenchToast.makeText(context, "I love Baguettes!");
// Toast is shown forever, as long as the process lives:
toast.show();
// Or until you call hide:
toast.hide();
```

You can dip a Toast to make a FrenchToast:

```java
Toast toast = Toast.makeText(context, "BREAD ALL THE THINGS!", LENGTH_SHORT);
toast.setGravity(LEFT | TOP, 0, 0);
FrenchToast frenchToast = FrenchToast.dip(toast);
frenchToast.show();
```

You can also make a FrenchToast from a layout:

```java
FrenchToast toast = FrenchToast.makeLayout(context, R.layout.fried_toast);
toast.show();
```

## Serial Toaster

Of course, showing several Toasts at the same time could be confusing. FrenchToast provides a ToastQueue to make sure you only show one Toast at a time.

```java
ToastQueue queue = new LifecycleToastQueue();
// Show toast1 for 3 seconds, starting now:
queue.enqueue(toast1, 3, SECONDS);
// Also provides methods for Android default LENGTH_SHORT and LENGTH_LONG durations.
queue.enqueueShort(toast2);
queue.enqueueLong(toast3);
// You can remove a toast from the queue, hiding it immediately if it is already showing:
queue.cancel(toast1);
// You can also clear the queue from all Toasts, including any Toast currently showing.
queue.clear();
```

The queues can be paused and resumed to scope toasts to subparts of your app.

```java
LifecycleToastQueue queue = new LifecycleToastQueue();
// Immediately start showing a Toast:
queue.enqueue(toast1, 3, SECONDS);
// Pause the queue, toast1 disappears:
queue.pause();
// You can enqueue toasts while the queue is paused:
queue.enqueueShort(toast2);
// toast1 is shown for the entire 3 seconds again, then toast2 is shown.
queue.resume();
```

## ActivityToasts

Managing the lifecycle of a toast queue can be tricky. ActivityToasts associates a LifecycleToastQueue to a live activity, making sure it survives activity configuration changes. That way, you can scope toasts to a given activity. They will hide when the activity is paused, and show up again when the activity is resumed.

You need to setup `ActivityToasts` in your `Application` class.

```java
public class ExampleApplication extends Application {

  @Override public void onCreate() {
    super.onCreate();
    ActivityToasts.install(this);
  }
}
```

Then the `ToastQueue` is available from any context that wraps an activity through `ActivityToasts.with(context)`.

```java
FrenchToast toast = FrenchToast.makeText(context, R.string.toast_text);
ActivityToasts.with(context).enqueue(toast, 3, SECONDS);
```

![logo.png](assets/logo.png)

## License

    Copyright 2015 Pierre-Yves Ricau

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
