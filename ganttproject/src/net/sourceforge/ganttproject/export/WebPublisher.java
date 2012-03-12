/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.export;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.language.GanttLanguage;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

public class WebPublisher {

  public static class Ftp {
    private final FTPClient ftpClient = new FTPClient();
    private boolean isLoggedIn;
    private boolean isConnected;

    public IStatus loginAndChangedir(DocumentManager.FTPOptions options) throws IOException {
      ftpClient.connect(options.getServerName().getValue());
      int reply = ftpClient.getReplyCode();
      if (!FTPReply.isPositiveCompletion(reply)) {
        ftpClient.disconnect();
        return new Status(IStatus.ERROR, "net.sourceforge.ganttproject", GanttLanguage.getInstance().getText(
            "errorFTPConnection")
            + " Connection failed: " + ftpClient.getReplyString());
      }
      isConnected = true;
      if (!ftpClient.login(options.getUserName().getValue(), options.getPassword().getValue())) {
        ftpClient.logout();
        ftpClient.disconnect();
        return new Status(IStatus.ERROR, "net.sourceforge.ganttproject", GanttLanguage.getInstance().getText(
            "errorFTPConnection")
            + " Login failed: " + ftpClient.getReplyString());
      }
      isLoggedIn = true;
      ftpClient.enterLocalPassiveMode();
      if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
        GPLogger.getLogger(WebPublisher.class).warning(
            "Failed to enter passive mode on FTP server=" + options.getServerName() + " Reply message:"
                + ftpClient.getReplyString());
        ftpClient.enterLocalActiveMode();
        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
          return new Status(IStatus.ERROR, "net.sourceforge.ganttproject", GanttLanguage.getInstance().getText(
              "errorFTPConnection")
              + " Passive and active mode failed: " + ftpClient.getReplyString());
        }
      }
      String dirName = options.getDirectoryName().getValue();
      if (dirName == null) {
        dirName = "";
      }
      if (!dirName.isEmpty() && !ftpClient.changeWorkingDirectory(dirName)) {
        ftpClient.logout();
        ftpClient.disconnect();
        return new Status(IStatus.ERROR, "net.sourceforge.ganttproject", GanttLanguage.getInstance().getText(
            "errorFTPConnection")
            + MessageFormat.format(" Change directory to {0} failed: ", dirName, ftpClient.getReplyString()));
      }
      ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      return Status.OK_STATUS;
    }

    IStatus put(File file) throws IOException {
      if (!ftpClient.storeFile(file.getName(), new BufferedInputStream(new FileInputStream(file)))) {
        return new Status(IStatus.ERROR, "net.sourceforge.ganttproject", "Failed to write file=" + file.getName()
            + " server response=" + ftpClient.getReplyString());
      }
      return Status.OK_STATUS;
    }

    public void detach() throws IOException {
      if (isLoggedIn) {
        ftpClient.logout();
      }
      if (isConnected) {
        ftpClient.disconnect();
      }
    }
  }

  WebPublisher() {
  }

  public void run(final File[] exportFiles, final DocumentManager.FTPOptions options) {
    IJobManager jobManager = Job.getJobManager();
    IProgressMonitor monitor = jobManager.createProgressGroup();
    Job startingJob = new Job("starting") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Publishing files on FTP", exportFiles.length);
        try {
          final Ftp ftp = new Ftp();
          IStatus status = ftp.loginAndChangedir(options);
          if (!status.isOK()) {
            GPLogger.log(status.getMessage());
            return status;
          }
          for (int i = 0; i < exportFiles.length; i++) {
            Job nextJob = createTransferJob(ftp, exportFiles[i]);
            nextJob.setProgressGroup(monitor, 1);
            nextJob.schedule();
            nextJob.join();
          }
          Job finishingJob = new Job("finishing") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
              monitor.done();
              try {
                ftp.detach();
                return Status.OK_STATUS;
              } catch (IOException e) {
                GPLogger.log(e);
                return Status.CANCEL_STATUS;
              }
            }
          };
          finishingJob.setProgressGroup(monitor, 0);
          finishingJob.schedule();
          finishingJob.join();
        } catch (IOException e) {
          if (!GPLogger.log(e)) {
            e.printStackTrace(System.err);
          }
        } catch (InterruptedException e) {
          if (!GPLogger.log(e)) {
            e.printStackTrace(System.err);
          }
        }
        return Status.OK_STATUS;
      }
    };
    startingJob.setProgressGroup(monitor, 0);
    startingJob.schedule();
  }

  private Job createTransferJob(final Ftp ftp, final File file) {
    Job result = new Job("transfer file " + file.getName()) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          IStatus ftpStatus = ftp.put(file);
          if (!ftpStatus.isOK()) {
            GPLogger.getLogger(WebPublisher.class).warning(ftpStatus.getMessage());
            return ftpStatus;
          }
          monitor.worked(1);
          return Status.OK_STATUS;
        } catch (IOException e) {
          if (!GPLogger.log(e)) {
            e.printStackTrace(System.err);
          }
          return Status.CANCEL_STATUS;
        } finally {
        }
      }
    };
    return result;
  }

}
