/*
Copyright 2018 Oleksii Lapinskyi, BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject;

import com.google.common.io.ByteStreams;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.update.RestartDialog;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadWorker extends SwingWorker<Void, Integer> {
  private static final int BUFFER_SIZE = 4096;
  private static final String PLUGINS_FOLDER = File.separator + "plugins";
  private String myDownloadURL;
  private UIFacade myUiFacade;

  public DownloadWorker(UIFacade uiFacade, String downloadURL) {
    myUiFacade = uiFacade;
    myDownloadURL = downloadURL;
  }

  @Override
  protected void process(List<Integer> chunks) {
    String statusText = "";
    int chunk = chunks.get(chunks.size() - 1);
    if (chunk > -1) {
      statusText = GanttLanguage.getInstance().formatText("downloadProgress", chunk);
    }
    myUiFacade.setStatusText(statusText);
  }

  @Override
  protected Void doInBackground() {
    HttpURLConnection urlConnection = null;
    File updateFile = null;
    try {
      URL url = new URL(myDownloadURL);
      urlConnection = (HttpURLConnection) url.openConnection();
      int responseCode = urlConnection.getResponseCode();

      updateFile = File.createTempFile("gantt-update", ".tmp");

      if (responseCode == HttpURLConnection.HTTP_OK) {
        downloadZip(urlConnection, updateFile);
      } else {
        throw new IOException("No file to download. Server replied HTTP code: " + responseCode);
      }
      unzipUpdates(updateFile);
    } catch (IOException ex) {
      GPLogger.log(ex);
      cancel(true);
    } finally {
      if (urlConnection != null) {
        urlConnection.disconnect();
      }
      if (updateFile != null) {
        updateFile.delete();
      }
    }

    return null;
  }

  private void downloadZip(HttpURLConnection urlConnection, File updateFile) {
    try (InputStream inputStream = urlConnection.getInputStream();
         FileOutputStream outputStream = new FileOutputStream(updateFile)) {

      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead = -1;
      long totalBytesRead = 0;
      int percentCompleted = 0;
      long fileSize = urlConnection.getContentLength();

      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
        totalBytesRead += bytesRead;
        percentCompleted = (int) (totalBytesRead * 100 / fileSize);

        publish(percentCompleted);
      }
    } catch (IOException ex) {
      GPLogger.log(ex);
      cancel(true);
    }
  }

  private void unzipUpdates(File updateFile) {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(updateFile.getAbsolutePath()))) {
      String extractionFolder = getExtractionFolder();

      File folder = new File(extractionFolder);
      if (!folder.exists()) {
        if (!folder.mkdirs()) {
          throw new IOException("Cannot create plugins folder");
        }
      }

      ZipEntry zipEntry;
      while ((zipEntry = zis.getNextEntry()) != null) {
        String fileName = zipEntry.getName();

        if (zipEntry.isDirectory()) {
          continue;
        }

        File newFile = new File(extractionFolder + File.separator + fileName);
        File parent = newFile.getParentFile();
        if (!parent.exists()) {
          if (!parent.mkdirs()) {
            throw new IOException("Cannot create plugins folder");
          }
        }

        FileOutputStream fos = new FileOutputStream(newFile);
        ByteStreams.copy(zis, fos);

        fos.close();
        zis.closeEntry();
      }
    } catch (IOException ex) {
      GPLogger.log(ex);
      cancel(true);
    }
  }

  private String getExtractionFolder() {
    URL projectPath = Object.class.getResource(PLUGINS_FOLDER);
    if (projectPath != null && new File(projectPath.getPath()).canWrite()) {
      return projectPath.getPath() + PLUGINS_FOLDER;
    } else {
      return System.getProperty("user.home") + File.separator + ".ganttproject.d" + PLUGINS_FOLDER;
    }
  }

  @Override
  protected void done() {
    myUiFacade.setStatusText("");
    if (!isCancelled()) {
      RestartDialog.show(myUiFacade);
    }
  }
}