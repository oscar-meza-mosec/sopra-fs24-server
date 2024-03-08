package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

    private final UserService userService;
//    private UserInfo uInfo;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetDTO> getAllUsers() {
        // fetch all users in the internal representation
        List<User> users = userService.getUsers();
        List<UserGetDTO> userGetDTOs = new ArrayList<>();

        // convert each user to the API representation
        for (User user : users) {
            userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
        }
        return userGetDTOs;
    }

    @RequestMapping(value = "/login_error", produces = MediaType.APPLICATION_JSON_VALUE)
    public String loginError() {
        return "{ \"error\":\"The credentials are incorrect\" }";
    }

    @RequestMapping(value = "/login_success", produces = MediaType.APPLICATION_JSON_VALUE)
    public String loginSuccess() {
        return "{ \"message\":\"Login successful\" }";
    }

    @GetMapping("/users/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO getUser(@PathVariable Long id) {
        // fetch user from the id in the internal representation
        User user = userService.getUser(id);
        // convert user to the API representation and return
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED) // 201
    @ResponseBody
    public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
        // convert API user to internal representation
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        // create user
        User createdUser = userService.createUser(userInput);
        // convert internal representation of user back to API
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
    }

    @PutMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    @ResponseBody
    public void updateUser(@RequestBody UserPostDTO userPostDTO, @PathVariable Long id, HttpSession session) {
        // convert API user to internal representation
        Object uid = session.getAttribute("uid");
        if(uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        User userById = userService.getUser(id);
        String baseErrorMessage = "User with userId %d was not found";

        if (userById == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(baseErrorMessage, id));
        }

        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
        if(id != Long.parseLong(uid.toString())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "We can only update our own profile");
        }
        // update user
        userService.updateUser(id, userInput);
    }
    @GetMapping(value = "/current_user", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String getCurrentUser(HttpSession session) {
        // convert API user to internal representation
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if(auth == null || auth.getName().equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        System.out.println("auth.getName():" + auth.getName());
        // convert internal representation of user back to API
        if (session.getAttribute("uid") == null) {
            User user = userService.getUser(auth.getName());
            session.setAttribute("uid", user.getId());
        }
        Long id = Long.parseLong(session.getAttribute("uid").toString());
        User user = userService.getUser(id);
        return String.format("{\"id\":%s, \"name\":\"%s\"}", id, user.getUsername());
    }
}
