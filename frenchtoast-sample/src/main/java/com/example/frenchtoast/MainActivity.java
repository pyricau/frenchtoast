package com.example.frenchtoast;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import frenchtoast.ActivityToasts;
import frenchtoast.FrenchToast;

import static frenchtoast.FrenchToast.makeText;
import static java.util.concurrent.TimeUnit.SECONDS;

public class MainActivity extends Activity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);
    findViewById(R.id.toggle_baguettes).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        toggleBaguetteToast();
      }
    });
    findViewById(R.id.show_long_toast).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        showLongToast();
      }
    });
  }

  private void toggleBaguetteToast() {
    FrenchToast baguetteToast = ExampleApplication.from(this).getBaguetteToast();
    baguetteToast.toggle();
  }

  private void showLongToast() {
    FrenchToast toast = makeText(this, R.string.toast_text);
    ActivityToasts.with(this).enqueue(toast, 5, SECONDS);
  }
}


