package edu.ap.gosmartlib.security;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2UserServiceTest {

    @Test
    void givenUserInfoAndGroupInfo_whenLoadUser_thenMergedAttributesContainGroups() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        String baseUrl = "http://localhost:" + server.getAddress().getPort();

        String fullUserInfoJson = """
                {
                  "userID": "u-1",
                  "basisrol": "Bibliotheekbeheerder",
                  "platform": "%s"
                }
                """.formatted(baseUrl);

        String groupInfoJson = """
                {
                  "groups": [{"groupID": "g-1", "name": "2ITSOF2"}],
                  "parentGroups": [{"groupID": "p-1", "name": "2de jaars"}]
                }
                """;

        server.createContext("/userinfo", exchange -> {
            byte[] bytes = fullUserInfoJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/Api/V1/groupinfo", exchange -> {
            byte[] bytes = groupInfoJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        try {
            ClientRegistration registration = ClientRegistration.withRegistrationId("smartschool")
                    .clientId("client-id")
                    .clientSecret("client-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("fulluserinfo", "groupinfo")
                    .authorizationUri(baseUrl + "/oauth/authorize")
                    .tokenUri(baseUrl + "/oauth/token")
                    .userInfoUri(baseUrl + "/userinfo")
                    .userNameAttributeName("userID")
                    .build();

            OAuth2AccessToken token = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "token-123",
                    Instant.now(),
                    Instant.now().plusSeconds(300));

            OAuth2UserRequest userRequest = new OAuth2UserRequest(registration, token);

            OAuth2UserService service = new OAuth2UserService();
            OAuth2User user = service.loadUser(userRequest);

            assertEquals("u-1", user.getAttribute("userID"));
            List<Map<String, Object>> groups = user.getAttribute("groups");
            List<Map<String, Object>> parentGroups = user.getAttribute("parentGroups");
            assertNotNull(groups);
            assertNotNull(parentGroups);
            assertEquals(1, groups.size());
            assertEquals(1, parentGroups.size());
            assertEquals("g-1", groups.get(0).get("groupID"));
            assertEquals("p-1", parentGroups.get(0).get("groupID"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void givenInvalidPlatform_whenFetchGroupInfo_thenReturnsEmptyMap() {
        OAuth2UserService service = new OAuth2UserService();

        @SuppressWarnings("unchecked")
        Map<String, Object> result = ReflectionTestUtils.invokeMethod(
                service,
                "fetchGroupInfo",
                "ht!tp://invalid",
                "dummy-token");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

