import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.yubi.selfserveloginrbackservice.constant.Constants;
import com.yubi.selfserveloginrbackservice.controller.AuthController;
import com.yubi.selfserveloginrbackservice.model.Response;
import com.yubi.selfserveloginrbackservice.model.UserInfo;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;

@SpringBootTest(classes=AuthControllerTest.class)
@RunWith(MockitoJUnitRunner.class)
public class AuthControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Jedis jedis;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private AuthController authController;

    private final String authority = "testAuthority";
    private final String origin = "testOrigin";
    private final String uriPrefix = "https://auth-qa-api.go-yubi.in";
    private final String redisHostname = "localhost";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(authController, "authority", authority);
        ReflectionTestUtils.setField(authController, "origin", origin);
        ReflectionTestUtils.setField(authController, "uriPrefix", uriPrefix);
        ReflectionTestUtils.setField(authController, "redisHostname", redisHostname);
    }

    @Test
    void testGetPermissionsCachedDataFound() {

        String cachedData = "cachedData";
        Mockito.when(jedis.get(anyString())).thenReturn(cachedData);
        UserInfo userInfo = new UserInfo();
        userInfo.setCaUserId("646751e78a491a5bfc717387");
        userInfo.setEntityId("6458c067637fb4005af02604");
        userInfo.setGroupId("lender");
        userInfo.setProductId("SPOCTOX");
        userInfo.setLocalUserId("646751e78a491a5bfc717387");
        ResponseEntity<Object> responseEntity = authController.getPermissions(userInfo,
            "authorization",
            "mfaToken", "currentEntityId", "currentGroup");
        // Verify the response
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(cachedData, responseEntity.getBody());
        // Verify Redis cache interaction
        verify(jedis).get(("646751e78a491a5bfc717387"));
        verify(jedis, never()).setex(anyString(), anyInt(), anyString());
        Mockito.verifyNoInteractions(restTemplate);

    }


    @Test
    void testGetPermissionsHttpCallSuccess() throws URISyntaxException {
        // Mock Redis cache miss
        Mockito.when(jedis.get(anyString())).thenReturn(null);

        // Create a UserInfo object for the request
        UserInfo userInfo = new UserInfo();
        userInfo.setCaUserId("testCaUserId");
        userInfo.setProductId("testProductId");
        userInfo.setEntityId("testEntityId");
        userInfo.setGroupId("testGroupId");
        userInfo.setLocalUserId("testLocalUserId");

        // Create the expected URI
        URI expectedUri = new URI(uriPrefix
            + "/users/testLocalUserId/all_permissions_web?product_id=testProductId&entity_id=testEntityId&group_id=testGroupId");

        // Create the expected response entity
        ResponseEntity<Object> expectedResponseEntity = new ResponseEntity<>(HttpStatus.OK);

        // Mock the restTemplate.exchange method
        Mockito.when(
                restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(Object.class)))
            .thenReturn(expectedResponseEntity);

        // Invoke the method
        ResponseEntity<Object> responseEntity = authController.getPermissions(userInfo,
            "authorization",
            "mfaToken", "currentEntityId", "currentGroup");

        // Verify Redis cache interaction
        verify(jedis).get(("testCaUserId"));
    }


    @Test
    public void testGetPermissions_NullResponseEntity() throws URISyntaxException {
        // Mocked data
        UserInfo userInfo = new UserInfo();
        userInfo.setCaUserId("123");
        userInfo.setLocalUserId("LocalUserId");
        userInfo.setProductId("ProductId");
        userInfo.setEntityId("EntityId");
        userInfo.setGroupId("GroupId");

        // Mock Redis get
        Mockito.when(jedis.get(userInfo.getCaUserId())).thenReturn(null);

        // Mock RestTemplate exchange with null response
        Mockito.when(
                restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(Object.class)))
            .thenReturn(null);

        // Invoke the method
        ResponseEntity<Object> response = authController.getPermissions(userInfo, "Authorization",
            "MfaToken",
            "CurrentEntityId", "CurrentGroup");

        // Verify the interactions and response
        verify(jedis).get(userInfo.getCaUserId());
//        verify(restTemplate).exchange(Mockito.any(RequestEntity.class), Mockito.eq(Object.class));

//        Mockito.verifyNoInteractions(jedis); // Redis setex should not be called
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(Constants.FAILURE, ((Response) response.getBody()).getStatus());
    }


    @Test
    public void testLogout_CacheDataDeleted() {
        // Mocked data
        UserInfo userInfo = new UserInfo();
        userInfo.setCaUserId("123");
        // Mock Redis del
        Mockito.when(jedis.del(userInfo.getCaUserId())).thenReturn(1L);
        // Invoke the method
        ResponseEntity<Object> response = authController.logout(userInfo);
        // Verify the interactions and response
        verify(jedis).del(userInfo.getCaUserId());
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("Logout successful", ((Response) response.getBody()).getMessage());
        Assertions.assertEquals(Constants.SUCCESS, ((Response) response.getBody()).getStatus());
    }


}