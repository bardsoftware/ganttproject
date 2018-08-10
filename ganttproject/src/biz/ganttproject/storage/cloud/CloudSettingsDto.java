// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author dbarashev@bardsoftware.com
 */
public class CloudSettingsDto {
  public final String serverUrl;
  public final String username;
  public final String password;

  @JsonCreator
  public CloudSettingsDto(
      @JsonProperty("serverUrl") String serverUrl, @JsonProperty("username") String username, @JsonProperty("password") String password) {
    this.serverUrl = serverUrl;
    this.username = username;
    this.password = password;
  }
}
