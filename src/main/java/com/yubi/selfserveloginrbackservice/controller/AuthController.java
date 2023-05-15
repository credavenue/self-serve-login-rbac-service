package com.yubi.selfserveloginrbackservice.controller;

import com.yubi.selfserveloginrbackservice.model.UserInfo;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;

@RestController
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
        @RequestHeader String currentGroup) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();

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

        // Build the request URI with query parameters
        URI uri = new URI("https://auth-qa-api.go-yubi.in/users/" + currentEntityId
            + "/all_permissions_web?product_id=" + productId + "&entity_id=" + entityId + "&group_id=" + groupId);

        // Create the request entity with method, headers, and body
        RequestEntity<Object> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, uri);

        // Send the HTTP GET request
        ResponseEntity<Object> responseEntity = restTemplate.exchange(requestEntity, Object.class);

        // Store the response in Redis
        String caUserId = userInfo.getCaUserId();
        Jedis jedis = new Jedis(redisHostname); // Replace with the Redis server address if needed
        int expiryTimeInSeconds = 3600; // Set the expiry time (e.g., 1 hour)
        jedis.setex(caUserId, expiryTimeInSeconds, responseEntity.getBody().toString());
        jedis.close();
        return responseEntity;
    }

}
