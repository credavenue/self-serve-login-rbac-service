package com.yubi.selfserveloginrbackservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ErrorResponse {

  @JsonAlias("status")
  private String status;

  @JsonAlias("statusCode")
  private long statusCode;

  @JsonAlias("userId")
  private String userId;

  @JsonAlias("message")
  private String message;

  @JsonAlias("data")
  private List<Object> data;

  @JsonAlias("errors")
  List<ErrorObject> errors;

}
