package com.example.frenchtoast;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import frenchtoast.FrenchToast;
import frenchtoast.Mixture;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MainActivity extends Activity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);
    findViewById(R.id.show_baguette).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        showBaguetteToast();
      }
    });
    findViewById(R.id.toggle_infinite_toast).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        toggleInfiniteToast();
      }
    });
  }

  private void showBaguetteToast() {
    FrenchToast.with(this).length(3, SECONDS).showLayout(R.layout.baguette_toast);
  }

  private void toggleInfiniteToast() {
    Mixture mixture = ExampleApplication.from(this).getInfiniteToast();
    mixture.toggle();
  }
}


