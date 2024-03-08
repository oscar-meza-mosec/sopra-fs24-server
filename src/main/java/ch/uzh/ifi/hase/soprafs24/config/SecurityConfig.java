package ch.uzh.ifi.hase.soprafs24.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public AppAuthenticationSuccessHandler authenticationSuccessHandler(){
        return new AppAuthenticationSuccessHandler("/login_success");
    }

    @Bean
    public AppLogoutHandler appLogoutHandler(){
        return new AppLogoutHandler();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .failureUrl("/login_error")
                .successHandler(authenticationSuccessHandler()) // Here we set the session attribute for the user Id
                .and()
                .httpBasic()
                .and()
                .authorizeRequests()
                .mvcMatchers(HttpMethod.GET, "/users/{id}").authenticated()
                .mvcMatchers(HttpMethod.GET, "/users").authenticated()
                .mvcMatchers(HttpMethod.PUT, "/users").authenticated()
                .mvcMatchers(HttpMethod.GET, "/overview").authenticated()
                .mvcMatchers(HttpMethod.GET, "/profile/**").authenticated()
                .anyRequest().permitAll()
                .and()
                .headers().frameOptions().disable() // for the H2 console
                .and()
                .csrf().disable()
                .logout()
                .logoutUrl("/perform_logout")
                .addLogoutHandler(appLogoutHandler())  // Here we update the status of the user to OFFLINE using the user Id session attribute
                .logoutSuccessUrl("/login")
                .deleteCookies("JSESSIONID");
    }

}
