package biz.ganttproject.lib.fx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
public class Toast {

  private static int TOAST_TIMEOUT = 1400;

  private static Popup createPopup(final String message) {
    final Popup popup = new Popup();
    popup.setAutoFix(true);
    Label label = new Label(message);
//    label.getStylesheets().add("/css/mainStyles.css");
//    label.getStyleClass().add("popup");
    popup.getContent().add(label);
    return popup;
  }

  public static void show(Stage stage, final String message) {
    final Popup popup = createPopup(message);
    popup.setOnShown(e -> {
      popup.setX(stage.getX() + stage.getWidth() / 2 - popup.getWidth() / 2);
      popup.setY(stage.getY() + stage.getHeight() / 1.2 - popup.getHeight() / 2);
    });
    popup.show(stage);

    new Timeline(new KeyFrame(
      Duration.millis(TOAST_TIMEOUT),
      ae -> popup.hide())).play();
  }

}