package com.example.frenchtoast;

import android.app.Application;
import android.content.Context;
import frenchtoast.ActivityToasts;
import frenchtoast.FrenchToast;

public class ExampleApplication extends Application {

  public static ExampleApplication from(Context context) {
    return (ExampleApplication) context.getApplicationContext();
  }

  private FrenchToast baguetteToast;

  @Override public void onCreate() {
    super.onCreate();
    ActivityToasts.install(this);
  }

  public FrenchToast getBaguetteToast() {
    if (baguetteToast == null) {
      baguetteToast = FrenchToast.makeLayout(this, R.layout.baguette_toast);
    }
    return baguetteToast;
  }
}
