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
  public static final String CANONICAL_GANTTPROJECT_CLOUD_URL = "http://webdav.ganttproject.biz";
  private static final Set<String> GANTTPROJECT_CLOUD_SERVERS = ImmutableSet.of(
      CANONICAL_GANTTPROJECT_CLOUD_URL, "https://webdav.ganttproject.biz", "https://webdav.ganttproject.cloud", "http://ganttproject-cloud.appspot.com/webdav"
  );
  //  private static final Set<String> GANTTPROJECT_CLOUD_SERVERS = ImmutableSet.of(
//    "https://webdav.yandex.ru"
//  );
  private final ListOption<WebDavServerDescriptor> myWebdavServerListOption;

  public GPCloudStorageOptions(ListOption<WebDavServerDescriptor> webdavServerListOption) {
    myWebdavServerListOption = webdavServerListOption;
  }

  public Optional<WebDavServerDescriptor> getCloudServer() {
    WebDavServerDescriptor result = findCloudServerDescriptor();
    return result == null ? Optional.empty() : Optional.of(result);
  }

  public void setCloudServer(CloudSettingsDto serverDto) {
    WebDavServerDescriptor cloudServer = findCloudServerDescriptor();
    if (cloudServer == null) {
      cloudServer = new WebDavServerDescriptor("GP Cloud", serverDto.serverUrl, serverDto.username, serverDto.password);
      myWebdavServerListOption.addValue(cloudServer);
    } else {
      cloudServer.setUsername(serverDto.username);
      cloudServer.setPassword(serverDto.password);
    }
  }

  private WebDavServerDescriptor findCloudServerDescriptor() {
    for (WebDavServerDescriptor server : myWebdavServerListOption.getValues()) {
      if (GANTTPROJECT_CLOUD_SERVERS.contains(server.getRootUrl())) {
        return server;
      }
    }
    return null;
  }

}
