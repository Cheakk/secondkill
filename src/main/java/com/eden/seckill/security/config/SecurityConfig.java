package com.eden.seckill.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;


@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable().authorizeRequests((authorize) -> authorize
				.antMatchers("/css/**").permitAll()
				.antMatchers("/user/**").hasRole("USER")
				.antMatchers("/index.shtml").hasRole("USER")
				.antMatchers("/second").hasRole("USER")
				.antMatchers("/seckillPage/**").hasRole("USER"))
				.formLogin((formLogin) -> formLogin.loginPage("/login").failureUrl("/login-error"));
	}

	@Bean
	public UserDetailsService userDetailsService() {
		UserBuilder users = User.withDefaultPasswordEncoder();
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(users.username("user").password("password").roles("USER").build());
        manager.createUser(users.username("admin").password("password").roles("USER","ADMIN").build());
		return manager;
	}
}
