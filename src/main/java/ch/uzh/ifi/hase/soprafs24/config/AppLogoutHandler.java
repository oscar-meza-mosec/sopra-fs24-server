package ch.uzh.ifi.hase.soprafs24.config;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Optional;

@Transactional
public class AppLogoutHandler implements LogoutHandler {

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        HttpSession session = request.getSession();
        Object uid = session.getAttribute("uid");
        if (uid == null) {
            return;
        }
        Long id = Long.parseLong(uid.toString());
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()) {
            return;
        }
        System.out.println("uid logout:" + id);
        user.get().setStatus(UserStatus.OFFLINE);
    }
}
