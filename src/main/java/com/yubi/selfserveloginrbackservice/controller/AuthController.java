package com.yubi.selfserveloginrbackservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubi.selfserveloginrbackservice.constant.Constants;
import com.yubi.selfserveloginrbackservice.model.Response;
import com.yubi.selfserveloginrbackservice.model.UserInfo;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
import redis.clients.jedis.exceptions.JedisConnectionException;

@RestController
@Slf4j
public class AuthController {

    @Value("${authority}")
    private String authority;

    @Value("${origin}")
    private String origin;

    @Value("${url.prefix}")
    private String uriPrefix;

    @Value("${spring.redis.host}")
    private String redisHostname;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Jedis jedis;

    private RestTemplate restTemplate;

    @PostConstruct
    public void initializeJedis() {
        jedis = new Jedis(redisHostname);
    }


    @PostMapping("/getUserPermissions")
    public ResponseEntity<Object> getPermissions(@RequestBody UserInfo userInfo,
        @RequestHeader String authorization,
        @RequestHeader String mfaToken,
        @RequestHeader String currentEntityId,
        @RequestHeader String currentGroup) {
        String caUserId = userInfo.getCaUserId();
        Response response = new Response();


        ObjectMapper objectMapper = new ObjectMapper();
        // Check if the data exists in Redis
        String cachedData = jedis.get(caUserId);
        if (cachedData != null) {
            log.info("Cached data found for caUserId: {}", caUserId);

            // Deserialize the cachedData string into a JSON object
            try {
                Object jsonPayload = objectMapper.readValue(cachedData, Object.class);
                return ResponseEntity.ok().body(jsonPayload);
            } catch (Exception e) {
                log.error("Failed to deserialize JSON payload from Redis: {}", e.getMessage());
                response.setMessage("Failed to deserialize JSON payload from Redis");
                response.setStatus(Constants.FAILURE);
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
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

        URI uri;
        ResponseEntity<Object> responseEntity;

        try {
            uri = new URI(uriPrefix + "/users/" + userInfo.getLocalUserId()
                + "/all_permissions_web?product_id=" + productId + "&entity_id=" + entityId
                + "&group_id=" + groupId);
            log.info("Sending request to: {}", uri);
            RequestEntity<Object> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, uri);
            // Send the HTTP GET request
            responseEntity = restTemplate.exchange(requestEntity, Object.class);

            if (responseEntity != null && responseEntity.getBody() != null) {
                // Store the response in Redis as a serialized JSON string
                String serializedPayload = objectMapper.writeValueAsString(responseEntity.getBody());
                int expiryTimeInSeconds = 3600; // TODO: Make it dynamic
                jedis.setex(caUserId, expiryTimeInSeconds, serializedPayload);
                log.info("Response stored in Redis for caUserId: {}", caUserId);
            } else {
                log.warn("Response entity or its body is null");
                response.setMessage("Response entity or its body is null");
                response.setStatus(Constants.FAILURE);
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (URISyntaxException ex) {
            log.error("Invalid URI syntax: {}", ex.getMessage());
            response.setMessage("Invalid URI syntax: " + ex.getMessage());
            response.setStatus(Constants.FAILURE);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        return responseEntity;
    }


    @PostMapping("/logout")
    public ResponseEntity<Object> logout(@RequestBody UserInfo userInfo) {
        Response response = new Response();
        String caUserId = userInfo.getCaUserId();
        try {
            Long deletedCount = jedis.del(caUserId);
            if (deletedCount != null && deletedCount > 0) {
                // TODO: Perform other logout-related tasks

                log.info("Cache data deleted for caUserId: {}", caUserId);
                response.setMessage("Logout successful");
                response.setStatus(Constants.SUCCESS);
                response.setStatusCode(HttpStatus.OK.value());
                return ResponseEntity.ok().body(response);
            } else {
                log.warn("No cache data found for caUserId: {}", caUserId);
                response.setMessage("No cache data found for the user");
                response.setStatus(Constants.SUCCESS);
                response.setStatusCode(HttpStatus.OK.value());
                return ResponseEntity.ok().body(response);
            }
        } catch (JedisConnectionException ex) {
            log.error("Exception occurred while connecting to Redis: {}", ex.getMessage());
            response.setMessage("Error connecting to Redis");
        } catch (Exception ex) {
            log.error("Exception occurred while deleting cache data for caUserId: {}", caUserId);
            response.setMessage("Error occurred during logout");
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        response.setStatus(Constants.FAILURE);
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }





}
