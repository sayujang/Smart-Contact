package com.smartcontact.scm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.smartcontact.scm.services.implementation.SecurityCustomUserDetailService;

@Configuration //spring runs this automatically when the app starts ie a type of bean
public class SecurityConfig {
    @Autowired
    private SecurityCustomUserDetailService userDetailService;
    @Autowired
    private OauthAuthenticationSuccessHandler oauthAuthenticationSuccessHandler;
    @Bean //tells spring to create bean for the object its returning to be created in application context
    public AuthenticationProvider authenticationProvider()
    {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }
    @Bean
    public PasswordEncoder passwordEncoder()
        {
            return new BCryptPasswordEncoder();
        }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception
    {
        httpSecurity.authorizeHttpRequests(authorize->{
            // authorize.requestMatchers("/home","/register","/services").permitAll();
            authorize.requestMatchers("/user/**").authenticated();
            authorize.anyRequest().permitAll();
        }
        );
        httpSecurity.formLogin(form -> {
        form.loginPage("/login");
        form.loginProcessingUrl("/authenticate");  //any name( ie no need controllers and endpoint for this, spring security will handle it internally)
        form.permitAll();
        form.successForwardUrl("/user/profile");
        form.failureForwardUrl("/login?error=true");//optional: spring security does this by default
        //now spring security will expect email and password as the names of input fields//by default it would expect username and password
    form.usernameParameter("email");
    form.passwordParameter("password");}
        );
        httpSecurity.csrf(AbstractHttpConfigurer::disable);//When CSRF is enabled, Spring only allows POST requests with a CSRF token → your GET request gets blocked (403 Forbidden or Whitelabel error). ahref uses by default get method
        httpSecurity.logout(
            logout->{
                logout.logoutUrl("/do-logout");
                logout.logoutSuccessUrl("/login?logout=true");
            }
        );
        httpSecurity.oauth2Login(login->{
            login.loginPage("/login");
            login.successHandler(oauthAuthenticationSuccessHandler);
        });
        return httpSecurity.build();
        
    }
}

