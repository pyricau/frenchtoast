package com.example.frenchtoast;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.widget.Toast;
import frenchtoast.FrenchToast;
import frenchtoast.Mixture;

import static android.widget.Toast.LENGTH_SHORT;

@SuppressLint("ShowToast") public class ExampleApplication extends Application {

  public static ExampleApplication from(Context context) {
    return (ExampleApplication) context.getApplicationContext();
  }

  private Mixture infiniteToast;

  @Override public void onCreate() {
    super.onCreate();
    FrenchToast.install(this);
  }

  public Mixture getInfiniteToast() {
    if (infiniteToast == null) {
      infiniteToast = Mixture.dip(Toast.makeText(this, R.string.toast_text, LENGTH_SHORT));
    }
    return infiniteToast;
  }
}
