import com.yubi.selfserveloginrbackservice.constant.Constants;
import com.yubi.selfserveloginrbackservice.controller.AuthController;
import com.yubi.selfserveloginrbackservice.model.Response;
import com.yubi.selfserveloginrbackservice.model.UserInfo;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;

public class AuthControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Jedis jedis;

    @Mock
    RedisTemplate redisTemplate;

    @InjectMocks
    private AuthController authController;

    @Captor
    private ArgumentCaptor<RequestEntity<Object>> requestEntityCaptor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetPermissions_CachedDataExistsInRedis() {
        // Mocked data
        UserInfo userInfo = new UserInfo();
        userInfo.setCaUserId("123");
        String cachedData = "Cached Data";

        // Mock Redis get
        Mockito.when(jedis.get(userInfo.getCaUserId())).thenReturn(cachedData);

        // Invoke the method
        ResponseEntity<Object> response = authController.getPermissions(userInfo, "Authorization",
            "MfaToken",
            "CurrentEntityId", "CurrentGroup");

        // Verify the response
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(cachedData, response.getBody());
        Mockito.verifyNoInteractions(restTemplate);
    }

    @Test
    public void testGetPermissions_SuccessfulRequest() throws URISyntaxException {
        // Mocked data
        UserInfo userInfo = new UserInfo();
        userInfo.setCaUserId("123");
        userInfo.setLocalUserId("LocalUserId");
        userInfo.setProductId("ProductId");
        userInfo.setEntityId("EntityId");
        userInfo.setGroupId("GroupId");
        String expectedResponse = "Response Data";

        // Mock Redis get
        Mockito.when(jedis.get(userInfo.getCaUserId())).thenReturn(null);

        // Mock RestTemplate exchange
        Mockito.when(
                restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.eq(Object.class)))
            .thenReturn(ResponseEntity.ok(expectedResponse));

        // Mock Redis setex
        Mockito.doNothing().when(jedis).setex(userInfo.getCaUserId(), 3600, expectedResponse);

        // Invoke the method
        ResponseEntity<Object> response = authController.getPermissions(userInfo, "Authorization",
            "MfaToken",
            "CurrentEntityId", "CurrentGroup");

        // Verify the interactions and response
        Mockito.verify(jedis).get(userInfo.getCaUserId());
        Mockito.verify(restTemplate)
            .exchange(Mockito.any(RequestEntity.class), Mockito.eq(Object.class));
        Mockito.verify(jedis).setex(userInfo.getCaUserId(), 3600, expectedResponse);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(expectedResponse, response.getBody());
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
        Mockito.verify(jedis).get(userInfo.getCaUserId());
        Mockito.verify(restTemplate)
            .exchange(Mockito.any(RequestEntity.class), Mockito.eq(Object.class));
        Mockito.verifyNoInteractions(jedis); // Redis setex should not be called
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Assertions.assertNull(response.getBody());
        Assertions.assertEquals(Constants.FAILURE, ((Response) response.getBody()).getStatus());
    }

    // ... Add more test cases for different scenarios

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
        Mockito.verify(jedis).del(userInfo.getCaUserId());
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("Logout successful", ((Response) response.getBody()).getMessage());
        Assertions.assertEquals(Constants.SUCCESS, ((Response) response.getBody()).getStatus());
    }


}