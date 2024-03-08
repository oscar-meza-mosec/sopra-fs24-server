package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    @Test
    public void createUser_validInputs_success() {
        // given
        assertNull(userRepository.findByUsername("testUsername"));

        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("123");

        // when
        User createdUser = userService.createUser(testUser);

        // then
        assertEquals(testUser.getId(), createdUser.getId());
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertNotNull(createdUser.getToken());
        assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
    }

    @Test
    public void createUser_duplicateUsername_throwsException() {
        assertNull(userRepository.findByUsername("testUsername"));

        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("123");
        User createdUser = userService.createUser(testUser);

        // attempt to create second user with same username
        User testUser2 = new User();

        // change the name but forget about the username
        testUser2.setUsername("testUsername");
        testUser2.setPassword("157");

        // check that an error is thrown
        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
    }

    @Test
    public void get_user_by_id() {

        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("123");
        User createdUser = userService.createUser(testUser);

        User requestedUser = userService.getUser(testUser.getId());

        assertEquals(requestedUser.getId(), createdUser.getId());
        assertEquals(requestedUser.getUsername(), createdUser.getUsername());
        assertNotNull(requestedUser.getToken());
    }

    @Test
    public void get_user_by_id_not_found() {
        assertThrows(ResponseStatusException.class, () -> userService.getUser(-1L));
    }

    @Test
    public void update_user_profile() {
        assertNull(userRepository.findByUsername("testUsername"));

        Date original = new Date();
        original.setTime(System.currentTimeMillis() - 100000);
        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("123");
        testUser.setBirthday(original);
        User createdUser = userService.createUser(testUser);

        User testUser2 = new User();

        Date changedDate = new Date();
        testUser2.setUsername("newUsername");
        testUser2.setBirthday(changedDate);

        User updatedUser = userService.updateUser(createdUser.getId(), testUser2);

        assertEquals(updatedUser.getUsername(), testUser2.getUsername());
        assertEquals(updatedUser.getBirthday(), testUser2.getBirthday());

        assertNotEquals(updatedUser.getUsername(), createdUser.getUsername());
        assertNotEquals(updatedUser.getBirthday(), createdUser.getBirthday());
    }

    @Test
    public void update_user_profile_not_found() {

        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("123");
        testUser.setBirthday(new Date());

        assertThrows(ResponseStatusException.class, () -> userService.updateUser(-1L, testUser));
    }

    @Test
    public void createUser_with_existing_name_throwsException() {
        assertNull(userRepository.findByUsername("testUsername"));

        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("123");
        userService.createUser(testUser);

        User testUser2 = new User();

        testUser2.setUsername("testUsername2");
        testUser2.setPassword("157");

        User createdUser2 = userService.createUser(testUser2);

        User testUser3 = new User();

        testUser3.setUsername("testUsername"); // Existing first user name
        testUser3.setBirthday(new Date());

        // check that an error is thrown
        assertThrows(ResponseStatusException.class, () -> userService.updateUser(createdUser2.getId(), testUser3));
    }
}
