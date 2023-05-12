package com.yubi.selfserveloginrbackservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LoginController {

  @PostMapping("/generateToken")
  public ResponseEntity<Object> validateOTP(@RequestParam String userName, String password) {
    return null;
  }

}