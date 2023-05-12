package com.yubi.selfserveloginrbackservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    @JsonAlias("ca_user_id")
    private String caUserId;
    @JsonAlias("entity_id")
    private String entityId;
    @JsonAlias("group_id")
    private String groupId;
}
