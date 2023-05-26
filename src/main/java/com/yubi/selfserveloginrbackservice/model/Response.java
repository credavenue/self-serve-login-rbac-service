package com.yubi.selfserveloginrbackservice.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated
public class Response {

  @JsonProperty("status")
  private String status;

  @JsonProperty("statusCode")
  private long statusCode;

  @JsonProperty("userId")
  private String userId;

  @JsonProperty("message")
  private String message;

  @JsonProperty("data")
  private List<Object> data;

  @JsonProperty("errors")
  List<ErrorObject> errors;

}
