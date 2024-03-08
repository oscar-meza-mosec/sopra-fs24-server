package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(@Qualifier("userRepository") UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers() {
        return this.userRepository.findAll();
    }

    public User getUser(Long id) {
        Optional<User> userById = userRepository.findById(id);
        String baseErrorMessage = "User with userId %d was not found";

        if (userById.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(baseErrorMessage, id));
        }
        return userById.get();
    }

    public User getUser(String username) {
        User user = userRepository.findByUsername(username);
        String baseErrorMessage = "username %s was not found";

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(baseErrorMessage, username));
        }
        return user;
    }

    public User createUser(User newUser) {

        Long max = 0L;
        List<User> users = getUsers();
        for(int x = 0; x < users.size(); x++) {
            User user = users.get(x);
            if(user.getId() > max) {
                max = user.getId();
            }
        }
        users.forEach(user -> {

        });

        if (newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The username can't be empty");
        }

        if (newUser.getPassword() == null || newUser.getPassword().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The password can't be empty");
        }

        newUser.setId(max + 1);
        newUser.setToken(UUID.randomUUID().toString());
        newUser.setStatus(UserStatus.OFFLINE);
        newUser.setCreationDate(new Date());
        checkIfUserExists(newUser);
        // saves the given entity but data is only persisted in the database once
        // flush() is called
        newUser = userRepository.save(newUser);
        userRepository.flush();

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    public User updateUser(Long id, User newUser) {
        User user = getUser(id);
        if(!newUser.getUsername().equals(user.getUsername())) {
            checkIfUserExists(newUser);
        }
        user.setBirthday(newUser.getBirthday());
        user.setUsername(newUser.getUsername());
        log.debug("Updated Information for User: {}", user);
        return user;
    }

    /**
     * This is a helper method that will check the uniqueness criteria of the
     * username and the name
     * defined in the User entity. The method will do nothing if the input is unique
     * and throw an error otherwise.
     *
     * @param userToBeCreated
     * @throws org.springframework.web.server.ResponseStatusException
     * @see User
     */
    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

        String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";

        if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "username", "is"));
        }
    }

}
