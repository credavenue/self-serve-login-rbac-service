package com.yubi.selfserveloginrbackservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yubi.selfserveloginrbackservice.model.UserInfo;

@RestController
public class AuthController {
    
    @PostMapping("/getUserPermissions")
    public ResponseEntity<Object> login(@RequestParam(required = true) UserInfo userInfo) {

        return null;

    }
    
}
