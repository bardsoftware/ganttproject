// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.FXUtil;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudSignupPane {
  private GanttLanguage i18n = GanttLanguage.getInstance();

  Pane createPane() {
    String msg = null;
    try {
      msg = String.format(i18n.getText("cloud.signup.html"),
          IOUtils.toString(getClass().getResourceAsStream("/biz/ganttproject/storage/cloud/GPCloudSignupPane.css"), "UTF-8"),
          "Join GanttProject Cloud!",
          "GanttProject Cloud is an online storage for collaborative work on your projects.",
          "Integrated with GanttProject Desktop",
          "Free for up to 2 users/month",
          "Scalable pay-as-you-go billing",
          "http://ganttproject.cloud",
          "Sign Up Now!",
          "Sign up is free. No credit card required");
      //msg = String.format(i18n.getText("cloud.signup.html1"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    BorderPane pane = new BorderPane();
    FXUtil.WebViewFitContent webView = new FXUtil.WebViewFitContent(msg);
    webView.setOnReady(() -> {});
    FXUtil.setOpenLinksInBrowser(webView.getWebEngine());
    pane.setCenter(webView);
    webView.getWebView().setPrefWidth(400);
    return pane;
  }
}



