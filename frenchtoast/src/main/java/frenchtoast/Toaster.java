package frenchtoast;

import android.content.res.Resources;
import android.widget.Toast;

public interface Toaster {

  /**
   * Shows a standard toast that just contains a text view.
   *
   * @param text The text to show.  Can be formatted text.
   */
  Toasted showText(CharSequence text);

  /**
   * Shows a standard toast that just contains a text view with the text from a resource.
   *
   * @param stringResId The resource id of the string resource to use.  Can be formatted text.
   * @throws Resources.NotFoundException if the resource can't be found.
   */
  Toasted showText(int stringResId);

  /**
   * Shows a toast that contains the inflated layout from a resource.
   *
   * @param layoutResId The resource id of the layout resource to use.
   * @throws Resources.NotFoundException if the resource can't be found.
   */
  Toasted showLayout(int layoutResId);

  /**
   * Shows the provided Toast, dipped as a FrenchToast.
   *
   * @param toast A Toast to dip that must not have been shown previously.
   */
  Toasted showDipped(Toast toast);
}
