package com.yubi.selfserveloginrbackservice.controller;

import com.yubi.selfserveloginrbackservice.model.UserInfo;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;

@RestController
@Slf4j
public class AuthController {

    @Value("${authority}")
    private String authority;

    @Value("${origin}")
    private String origin;

    @Value("${spring.redis.host}")
    private String redisHostname;

    @PostMapping("/getUserPermissions")
    public ResponseEntity<Object> getPermissions(@RequestBody UserInfo userInfo,
        @RequestHeader String authorization,
        @RequestHeader String mfaToken,
        @RequestHeader String currentEntityId,
        @RequestHeader String currentGroup) {
        RestTemplate restTemplate = new RestTemplate();
        String caUserId = userInfo.getCaUserId();

        // Check if the data exists in Redis
        String cachedData;
        try (Jedis jedis = new Jedis(redisHostname)) {
            cachedData = jedis.get(caUserId);
        }

        if (cachedData != null) {
            log.info("Cached data found for caUserId: {}", caUserId);
            return ResponseEntity.ok().body(cachedData);
        }

        // Set query parameters
        String productId = userInfo.getProductId();
        String entityId = userInfo.getEntityId();
        String groupId = userInfo.getGroupId();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("authority", authority);
        headers.add("authorization", authorization);
        headers.add("content-type", MediaType.APPLICATION_JSON_VALUE);
        headers.add("current-entity-id", currentEntityId);
        headers.add("current-group", currentGroup);
        headers.add("mfa-token", mfaToken);
        headers.add("origin", origin);

        URI uri = null;
        ResponseEntity<Object> responseEntity = null;

        try {
            uri = new URI("https://auth-qa-api.go-yubi.in/users/" + "6360f976d964090043b64dde"
                + "/all_permissions_web?product_id=" + productId + "&entity_id=" + entityId
                + "&group_id=" + groupId);
            log.info("Sending request to: {}", uri);
            RequestEntity<Object> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, uri);
            // Send the HTTP GET request
            responseEntity = restTemplate.exchange(requestEntity, Object.class);

            if (responseEntity != null && responseEntity.getBody() != null) {
                // Store the response in Redis
                try (Jedis jedis = new Jedis(redisHostname)) {
                    int expiryTimeInSeconds = 3600;
                    jedis.setex(caUserId, expiryTimeInSeconds, responseEntity.getBody().toString());
                }
                log.info("Response stored in Redis for caUserId: {}", caUserId);
            } else {
                log.warn("Response entity or its body is null");
            }

        } catch (URISyntaxException ex) {
            log.error("Invalid URI syntax: {}", ex.getMessage());
        } catch (HttpStatusCodeException ex) {
            log.error("HTTP request failed with status code {}: {}", ex.getStatusCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Exception occurred while sending request to platform: {}", ex.getMessage());
        }

        return responseEntity;
    }


}
