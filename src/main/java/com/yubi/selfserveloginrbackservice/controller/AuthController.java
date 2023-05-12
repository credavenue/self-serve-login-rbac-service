package com.yubi.selfserveloginrbackservice.controller;

import com.yubi.selfserveloginrbackservice.model.UserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    
    @PostMapping("/getUserPermissions")
    public ResponseEntity<Object> getPermissions(@RequestBody UserInfo userInfo,
        @RequestHeader String authorization,
        @RequestHeader String mfa_token,
        @RequestHeader String current_entity_id,
        @RequestHeader String current_group) {
        //validate auth token
        return null;

    }
    
}
