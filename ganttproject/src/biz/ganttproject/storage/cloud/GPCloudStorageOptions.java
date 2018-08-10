// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPAbstractOption;
import biz.ganttproject.core.option.ListOption;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;

import java.util.*;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCloudStorageOptions extends GPAbstractOption<WebDavServerDescriptor> implements ListOption<WebDavServerDescriptor> {
  public static final String CANONICAL_GANTTPROJECT_CLOUD_URL = "http://webdav.ganttproject.biz";
  private static final Set<String> GANTTPROJECT_CLOUD_SERVERS = ImmutableSet.of(
      CANONICAL_GANTTPROJECT_CLOUD_URL, "https://webdav.ganttproject.biz", "https://webdav.ganttproject.cloud", "http://ganttproject-cloud.appspot.com/webdav"
  );

  private final List<WebDavServerDescriptor> myServers = new ArrayList<>();
  private final ObservableList<WebDavServerDescriptor> myObservableList = FXCollections.observableList(myServers);

  public GPCloudStorageOptions() {
    super("servers");
  }

  public ObservableList<WebDavServerDescriptor> getList() {
    return myObservableList;
  }

  public Optional<WebDavServerDescriptor> getCloudServer() {
    WebDavServerDescriptor result = findCloudServerDescriptor(GANTTPROJECT_CLOUD_SERVERS);
    return result == null ? Optional.empty() : Optional.of(result);
  }

  public void setCloudServer(CloudSettingsDto serverDto) {
    WebDavServerDescriptor cloudServer = findCloudServerDescriptor(GANTTPROJECT_CLOUD_SERVERS);
    if (cloudServer == null) {
      cloudServer = new WebDavServerDescriptor("GP Cloud", serverDto.serverUrl, serverDto.username, serverDto.password);
      addValue(cloudServer);
    } else {
      cloudServer.setUsername(serverDto.username);
      cloudServer.setPassword(serverDto.password);
      myObservableList.set(myObservableList.indexOf(cloudServer), cloudServer);
    }
  }

  public ObservableList<WebDavServerDescriptor> getWebdavServers() {
    return myObservableList.filtered(server -> !GANTTPROJECT_CLOUD_SERVERS.contains(server.getRootUrl()));
  }

  private WebDavServerDescriptor findCloudServerDescriptor(Collection<String> goodUrls) {
    for (WebDavServerDescriptor server : myServers) {
      if (goodUrls.contains(server.getRootUrl())) {
        return server;
      }
    }
    return null;
  }

  @Override
  public void setValues(Iterable<WebDavServerDescriptor> values) {
    values.forEach(myObservableList::add);
  }

  @Override
  public Iterable<WebDavServerDescriptor> getValues() {
    return Collections.unmodifiableList(myServers);
  }

  @Override
  public void setValueIndex(int idx) {
    super.setValue(myServers.get(idx));
  }

  @Override
  public void addValue(WebDavServerDescriptor value) {
    myObservableList.add(value);
  }

  @Override
  public void updateValue(WebDavServerDescriptor oldValue, WebDavServerDescriptor newValue) {
    FXCollections.replaceAll(myObservableList, oldValue,newValue);
  }

  @Override
  public void removeValueIndex(int idx) {
    myObservableList.remove(idx);
  }

  @Override
  public EnumerationOption asEnumerationOption() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getPersistentValue() {
    StringBuilder result = new StringBuilder();
    for (WebDavServerDescriptor server : myServers) {
      result.append("\n").append(server.name).append("\t").append(server.getRootUrl()).append("\t").append(server.username);
      if (server.getSavePassword()) {
        result.append("\t").append(server.getPassword());
      }
    }
    return result.toString();
  }

  @Override
  public void loadPersistentValue(String value) {
    for (String s : value.split("\\n")) {
      if (!Strings.isNullOrEmpty(s)) {
        String[] parts = s.split("\\t");
        WebDavServerDescriptor server = new WebDavServerDescriptor();
        if (parts.length >= 1) {
          server.name = parts[0];
        }
        if (parts.length >= 2) {
          server.setRootUrl(parts[1]);
        }
        if (parts.length >= 3) {
          server.username = parts[2];
        }
        if (parts.length >= 4) {
          server.setPassword(parts[3]);
          server.setSavePassword(true);
        }
        if (!server.getRootUrl().isEmpty()) {
          myObservableList.add(server);
        }
      }
    }
  }

  public void removeValue(WebDavServerDescriptor server) {
    myObservableList.remove(server);
  }
}
