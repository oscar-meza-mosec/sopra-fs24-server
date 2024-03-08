package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.Cookie;
import java.util.Date;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */

@WebAppConfiguration
@SpringBootTest
public class UserControllerStatusCodesTest {
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setup() throws Exception { // This setup will run before each test
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        userRepository.deleteAll();
    }

    @Test
    public void unauthenticated_requests() throws Exception {

        // All these routes are protected via Spring Security authentication, so without being logged-in we can't access these resources

        MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest).andExpect(status().isUnauthorized()); // Status 401

        MockHttpServletRequestBuilder getUser = get("/users/1").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getUser).andExpect(status().isUnauthorized()); // Status 401

        MockHttpServletRequestBuilder getCurrentUser = get("/current_user").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getCurrentUser).andExpect(status().isUnauthorized()); // Status 401

        UserPostDTO user = new UserPostDTO();
        user.setUsername("test");
        user.setPassword("test");
        user.setBirthday(new Date());

        MockHttpServletRequestBuilder updateUser = put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(user));

        mockMvc.perform(updateUser).andExpect(status().isUnauthorized()); // Status 401
    }

    @Test
    public void login_test_302_status_and_authenticated() throws Exception {

        // First we create a new user
        User user = new User();
        user.setUsername("test");
        user.setPassword("test");

        userService.createUser(user);

        // And then we test the login process and get the response Status 302 "Found", noticing a redirection, with a session authentication test
        String redirectedUrl = mockMvc
                .perform(formLogin()
                        .user("test")
                        .password("test")
                        .loginProcessingUrl("/perform_login")
                )
                .andExpect(status().is(302))
                .andExpect(authenticated())
                .andReturn().getResponse().getRedirectedUrl();

        // We can also check that the login was successful from the redirected URL
        assertEquals(redirectedUrl, "/login_success");
    }

    @Test
    public void get_all_users() throws Exception {

        User user = new User();
        user.setUsername("first");
        user.setPassword("123");

        // First we create one user
        User createdUser = userService.createUser(user);

        User user2 = new User();
        user2.setUsername("second");
        user2.setPassword("139");

        // THen we create another user
        User createdUser2 =userService.createUser(user2);

        // Then we perform the login process using the first created user credentials returning the MvcResult from that request
        MvcResult res = mockMvc
                .perform(formLogin()
                        .user("first")
                        .password("123")
                        .loginProcessingUrl("/perform_login")
                )
                .andExpect(status().is(302))
                .andExpect(authenticated()).andReturn();

        // We get the request authenticated session
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // Then we try to get the users list
        MockHttpServletRequestBuilder getRequest = get("/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON);

        // Finally, since the Status is 200 "OK", we can check that
        mockMvc.perform(getRequest).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // we have 2 items on the response
                .andExpect(jsonPath("$[0].username", is(createdUser.getUsername()))) // the username of the first created user is the same
                .andExpect(jsonPath("$[0].status", is(UserStatus.ONLINE.toString()))) // the status of the first created user is ONLINE, since we are logged-in from that account
                .andExpect(jsonPath("$[1].username", is(createdUser2.getUsername()))) // the username of the second created user is the same
                .andExpect(jsonPath("$[1].status", is(UserStatus.OFFLINE.toString()))); // the status of the second created user is still OFFLINE
    }

    @Test
    public void createUser_valid_201_status() throws Exception {

        // First we prepare the user data
        User user = new User();
        user.setUsername("testUsername");
        user.setPassword("123");

        // Since this registration URL isn't protected via authentication we can call it now
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(user));

        // The response returns a 201 "Created" Status, and the returned data is the same as the request
        mockMvc.perform(postRequest)
                .andExpect(status().is(201))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.status", is(UserStatus.OFFLINE.toString())));
    }

    @Test
    public void createUser_duplicated_409_status() throws Exception {

        User user = new User();
        user.setUsername("duplicated");
        user.setPassword("137");

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("duplicated");
        userPostDTO.setPassword("123");

        // First we create the user on the database
        userService.createUser(user);

        // Then we prepare our request for creating a new user
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        // Finally we execute the request obtaining a "Conflict" 409 status in the response.
        String error = mockMvc.perform(postRequest)
                .andExpect(status().is(409))
                .andReturn().getResponse().getErrorMessage();

        // We also check the error message
        assertEquals(error, "The username provided is not unique. Therefore, the user could not be created!");
    }

    @Test
    public void getUser_exists_200_status() throws Exception {

        User user = new User();
        user.setUsername("first");
        user.setPassword("123");

        // First we create the user
        User createdUser = userService.createUser(user);

        // Then we perform the login process using the previously created user credentials returning the MvcResult from that request
        MvcResult res = mockMvc
                .perform(formLogin()
                        .user("first")
                        .password("123")
                        .loginProcessingUrl("/perform_login")
                )
                .andExpect(status().is(302))
                .andExpect(authenticated()).andReturn();

        // We get the request authenticated session
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // Then we try to get the user profile data
        assert session != null;
        MockHttpServletRequestBuilder postRequest = get("/users/" + createdUser.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON);

        // Since the user with that Id exists, the response has a Status 200 "OK" as expected, as the Status is successful, we can also check the response data
        mockMvc.perform(postRequest)
                .andExpect(authenticated())
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.status", is(UserStatus.ONLINE.toString())));
    }

    @Test
    public void getUser_not_exists_404_status() throws Exception {

        User user = new User();
        user.setUsername("first");
        user.setPassword("123");

        // First we create the user
        User createdUser = userService.createUser(user);

        // Then we perform the login process using the previously created user credentials returning the MvcResult from that request
        MvcResult res = mockMvc
                .perform(formLogin()
                        .user("first")
                        .password("123")
                        .loginProcessingUrl("/perform_login")
                )
                .andExpect(status().is(302))
                .andExpect(authenticated()).andReturn();

        // We get the request authenticated session
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // Then we try to get the user profile data, but using a non-existent user Id
        assert session != null;
        MockHttpServletRequestBuilder postRequest = get("/users/" + -1L)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON);

        // Since the user with that Id doesn't exists, the response has a Status 404 "Not Found" as expected
        String error = mockMvc.perform(postRequest)
                .andExpect(authenticated())
                .andExpect(status().is(404)).andReturn().getResponse().getErrorMessage();

        // We also check the error message
        assertEquals(error, "User with userId -1 was not found");
    }

    @Test
    public void updateUser_valid_input_204_status() throws Exception {

        User user = new User();
        user.setUsername("first");
        user.setPassword("123");

        // First we create the user
        User createdUser = userService.createUser(user);

        // Then we perform the login process using the previously created user credentials returning the MvcResult from that request
        MvcResult res = mockMvc
                .perform(formLogin()
                        .user("first")
                        .password("123")
                        .loginProcessingUrl("/perform_login")
                )
                .andExpect(status().is(302))
                .andExpect(authenticated()).andReturn();

        // We get the request authenticated session
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // We create the user object with the data we want to change
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("changed");
        userPostDTO.setBirthday(new Date());

        // Then we try to update the user with that data
        assert session != null;
        MockHttpServletRequestBuilder postRequest = put("/users/" + createdUser.getId())
                .session(session)
                .content(asJsonString(userPostDTO))
                .contentType(MediaType.APPLICATION_JSON);

        // Since we are trying to update our own user, and that is allowed, the response has a Status 204 "No Content" as expected
        mockMvc.perform(postRequest)
                .andExpect(authenticated())
                .andExpect(status().is(204));
    }

    @Test
    public void updateUser_user_not_found_404_status() throws Exception {

        User user = new User();
        user.setUsername("first");
        user.setPassword("123");

        // First we create the user
        User createdUser = userService.createUser(user);

        // Then we perform the login process using the previously created user credentials returning the MvcResult from that request
        MvcResult res = mockMvc
                .perform(formLogin()
                        .user("first")
                        .password("123")
                        .loginProcessingUrl("/perform_login")
                )
                .andExpect(status().is(302))
                .andExpect(authenticated()).andReturn();

        // We get the request authenticated session
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // We create the user object with the data we want to change
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("changed");
        userPostDTO.setBirthday(new Date());

        // Then we try to update the user with that data, but this time using a non-existent user Id
        assert session != null;
        MockHttpServletRequestBuilder postRequest = put("/users/" + -1L)
                .session(session)
                .content(asJsonString(userPostDTO))
                .contentType(MediaType.APPLICATION_JSON);

        // Since the user with Id -1L doesn't exists the response has a Status 404 "Not Found" as expected
        String error = mockMvc.perform(postRequest)
                .andExpect(authenticated())
                .andExpect(status().is(404))
                .andReturn().getResponse().getErrorMessage();

        // We also check the error message
        assertEquals(error, "User with userId -1 was not found");
    }

    @Test
    public void updateUser_trying_to_update_another_user_409_status() throws Exception {

        User user = new User();
        user.setUsername("first");
        user.setPassword("123");

        // First we create one user
        User createdUser = userService.createUser(user);

        User user2 = new User();
        user2.setUsername("second");
        user2.setPassword("139");

        // THen we create another user
        User createdUser2 = userService.createUser(user2);

        // Then we perform the login process using the first created user credentials returning the MvcResult from that request
        MvcResult res = mockMvc
                .perform(formLogin()
                        .user("first")
                        .password("123")
                        .loginProcessingUrl("/perform_login")
                )
                .andExpect(status().is(302))
                .andExpect(authenticated()).andReturn();

        // We get the request authenticated session
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // We create the user object with the data we want to change
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("changed");
        userPostDTO.setBirthday(new Date());

        // Then we try to update the second user, but logged in from the first user account
        assert session != null;
        MockHttpServletRequestBuilder postRequest = put("/users/" + createdUser2.getId())
                .session(session)
                .content(asJsonString(userPostDTO))
                .contentType(MediaType.APPLICATION_JSON);

        // Since we are trying to update another user account being logged-in from our user account, and that isn't allowed,
        // the response has a Status 409 "Conflict" as expected.
        String error = mockMvc.perform(postRequest)
                .andExpect(authenticated())
                .andExpect(status().is(409))
                .andReturn().getResponse().getErrorMessage();

        // We can also check the error message
        assertEquals(error, "We can only update our own profile");;
    }

    @Test
    public void logout_204_status() throws Exception {

        User user = new User();
        user.setUsername("test");
        user.setPassword("123");

        // First we create one user
        User createdUser = userService.createUser(user);

        // Then we perform the login process using the first created user credentials returning the MvcResult from that request
        MvcResult res = mockMvc
                .perform(formLogin()
                        .user("test")
                        .password("123")
                        .loginProcessingUrl("/perform_login")
                )
                .andExpect(status().is(302))
                .andExpect(authenticated()).andReturn();

        // We get the request authenticated session
        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // Then we try to perfomrm the second logout process
        assert session != null;
        MockHttpServletRequestBuilder logoutRequest = get("/perform_logout")
                .session(session);

        // We can check that the response has a Status 204 "No Content" as expected
        Cookie[] cookies = mockMvc.perform(logoutRequest)
                .andExpect(unauthenticated())
                .andExpect(status().is(204))
                .andReturn().getResponse().getCookies();

        // We iterate through the cookies obtained from the response
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("JSESSIONID")) {
                // We confirm that even the JSESSIONID cookie value has been deleted
                assertNull(cookie.getValue());
            }
        }
    }

    /**
     * Helper Method to convert userPostDTO into a JSON string such that the input
     * can be processed
     * Input will look like this: {"name": "Test User", "username": "testUsername"}
     *
     * @param object
     * @return string
     */
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }

}