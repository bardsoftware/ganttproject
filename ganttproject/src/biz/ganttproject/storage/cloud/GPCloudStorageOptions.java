// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.core.option.ListOption;
import com.google.common.collect.ImmutableSet;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;

import java.util.Optional;
import java.util.Set;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudStorageOptions {
  private static final Set<String> GANTTPROJECT_CLOUD_SERVERS = ImmutableSet.of(
      "http://webdav.ganttproject.biz", "https://webdav.ganttproject.biz", "https://webdav.ganttproject.cloud", "http://ganttproject-cloud.appspot.com/webdav"
  );
//  private static final Set<String> GANTTPROJECT_CLOUD_SERVERS = ImmutableSet.of(
//    "https://webdav.yandex.ru"
//  );
  private final ListOption<WebDavServerDescriptor> myWebdavServerListOption;

  public GPCloudStorageOptions(ListOption<WebDavServerDescriptor> webdavServerListOption) {
    myWebdavServerListOption = webdavServerListOption;
  }

  public Optional<WebDavServerDescriptor> getCloudServer() {
    for (WebDavServerDescriptor server : myWebdavServerListOption.getValues()) {
      if (GANTTPROJECT_CLOUD_SERVERS.contains(server.getRootUrl())) {
        return Optional.of(server);
      }
    }
    return Optional.empty();
  }

  public void setCloudServer(CloudSettingsDto cloudServer) {
    myWebdavServerListOption.addValue(new WebDavServerDescriptor("GP Cloud", cloudServer.serverUrl, cloudServer.username, cloudServer.password));
  }
}
