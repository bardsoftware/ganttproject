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

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.update.RestartDialog;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadWorker extends SwingWorker<Void, Integer> {
  private static final int BUFFER_SIZE = 4096;
  private static final String PLUGINS_FOLDER = "/plugins";
  private String myDownloadURL;
  private UIFacade myUiFacade;

  public DownloadWorker(UIFacade uiFacade, String downloadURL) {
    myUiFacade = uiFacade;
    myDownloadURL = downloadURL;
  }

  @Override
  protected void process(List<Integer> chunks) {
    myUiFacade.setDownloadProgress(chunks.get(chunks.size() - 1));
  }

  @Override
  protected Void doInBackground() {
    HttpURLConnection urlConnection = null;
    File updateFile = null;
    try {
      URL url = new URL(myDownloadURL);
      urlConnection = (HttpURLConnection) url.openConnection();
      int responseCode = urlConnection.getResponseCode();

      String saveFilePath = System.getProperty("java.io.tmpdir") + File.separator + "gantt-update";
      updateFile = File.createTempFile(saveFilePath, ".tmp");

      if (responseCode == HttpURLConnection.HTTP_OK) {
        downloadZip(urlConnection, updateFile);
      } else {
        throw new IOException("No file to download. Server replied HTTP code: " + responseCode);
      }
      unzipUpdates(updateFile);
    } catch (IOException ex) {
      ex.printStackTrace();
      cancel(true);
    } finally {
      urlConnection.disconnect();
      updateFile.delete();
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
      ex.printStackTrace();
      cancel(true);
    }
  }

  private void unzipUpdates(File updateFile) {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(updateFile.getAbsolutePath()))) {
      String extractionFolder = getExtractionFolder();

      File folder = new File(extractionFolder);
      if (!folder.exists()) {
        folder.mkdir();
      }

      byte[] buffer = new byte[BUFFER_SIZE];
      ZipEntry zipEntry;
      while ((zipEntry = zis.getNextEntry()) != null) {
        String fileName = zipEntry.getName();

        if (zipEntry.isDirectory()) {
          continue;
        }

        File newFile = new File(extractionFolder + File.separator + fileName);
        new File(newFile.getParent()).mkdirs();

        FileOutputStream fos = new FileOutputStream(newFile);
        int len;
        while ((len = zis.read(buffer)) > 0) {
          fos.write(buffer, 0, len);
        }
        fos.close();
        zis.closeEntry();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private String getExtractionFolder() {
    String projectPath = System.getProperty("user.dir");
    if (new File(projectPath).canWrite()) {
      return projectPath + PLUGINS_FOLDER;
    } else {
      return System.getProperty("user.home") + "/.ganttproject.d" + PLUGINS_FOLDER;
    }
  }

  @Override
  protected void done() {
    myUiFacade.setDownloadProgress(-1);
    if (!isCancelled()) {
      RestartDialog.show(myUiFacade);
    }
  }
}