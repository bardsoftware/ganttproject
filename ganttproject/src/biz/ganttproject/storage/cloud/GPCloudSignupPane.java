// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.FXUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudSignupPane implements GPCloudStorage.PageUi {
  private final Consumer<Pane> myUpdateUi;
  private final GPCloudLoginPane myLoginPane;
  private GanttLanguage i18n = GanttLanguage.getInstance();

  GPCloudSignupPane(Consumer<Pane> updateUi, GPCloudLoginPane loginPane) {
    myUpdateUi = updateUi;
    myLoginPane = loginPane;
  }
  public CompletableFuture<Pane> createPane() {
    CompletableFuture<Pane> result = new CompletableFuture<>();
    String msg = null;
    try {
      msg = String.format(i18n.getText("cloud.signup.html"),
          IOUtils.toString(getClass().getResourceAsStream("/biz/ganttproject/storage/cloud/GPCloudSignupPane.css"), "UTF-8"),
          "Sign Up to GanttProject Cloud!",
          "Online storage for collaborative work",
          "Integrated with GanttProject Desktop",
          "Free for up to 2 users/month",
          "Scalable pay-as-you-go billing",
          "Click the button or the link below to sign up. It is fast and free.",
          "http://ganttproject.cloud",
          "Sign Up Now!",
          "Already signed up? Click Continue to sign in to GanttProject Cloud.");
      //msg = String.format(i18n.getText("cloud.signup.html1"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    BorderPane pane = new BorderPane();
    FXUtil.WebViewFitContent webView = new FXUtil.WebViewFitContent(msg);
    webView.setOnReady(() -> result.complete(pane));
    FXUtil.setOpenLinksInBrowser(webView.getWebEngine());
    pane.setCenter(webView);
    webView.getWebView().setPrefWidth(400);

    HBox buttonPane = new HBox();
    buttonPane.setAlignment(Pos.CENTER);

    Button btnNext = new Button("Sign in");
    buttonPane.getChildren().add(btnNext);
    pane.setBottom(buttonPane);
    btnNext.addEventHandler(ActionEvent.ACTION, e -> {
      myLoginPane.createPane().thenApply(loginPane -> {
        myUpdateUi.accept(loginPane);
        return loginPane;
      });
    });
    return result;
  }
}



