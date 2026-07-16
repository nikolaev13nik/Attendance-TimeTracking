package co.il.avivsmile.security;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

	@Bean
	WebExpressionAuthorizationManager.Builder authz() {
		return WebExpressionAuthorizationManager.withDefaults();
	}

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return web -> web.ignoring().requestMatchers(PathRequest.toH2Console());
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, WebExpressionAuthorizationManager.Builder authz) throws Exception {
		http.httpBasic(Customizer.withDefaults());
		http.csrf(csrf -> csrf.disable());
		http.cors(cors -> cors.disable());
		http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		http.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/record/**").hasRole("ADMINISTRATOR")
				.requestMatchers(HttpMethod.PUT, "/record/start/{idUser}").access(authz.expression("@customWebSecurity.checkStartFinishRecords(#idUser, authentication) or hasRole('ADMINISTRATOR')"))
				.requestMatchers(HttpMethod.PUT, "/record/finish/{idUser}").access(authz.expression("@customWebSecurity.checkStartFinishRecords(#idUser, authentication) or hasRole('ADMINISTRATOR')"))
				.requestMatchers(HttpMethod.POST, "/record").hasRole("ADMINISTRATOR")
				.requestMatchers(HttpMethod.DELETE, "/record/remove/{id}").hasRole("ADMINISTRATOR")
//				.requestMatchers(HttpMethod.PUT, "/account/user").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/account/user/{idUser}").hasRole("ADMINISTRATOR")
				.requestMatchers(HttpMethod.POST, "/account/login").authenticated()
				.requestMatchers(HttpMethod.POST, "/account/user/{idUser}/role/{role}").hasRole("ADMINISTRATOR")
				.requestMatchers(HttpMethod.DELETE, "/account/user/{idUser}/role/{role}").hasRole("ADMINISTRATOR")
				.requestMatchers(HttpMethod.PUT, "/account/user/password/{idUser}").access(authz.expression("@customWebSecurity.checkAuthorityChangePassword(#idUser, authentication) or hasRole('ADMINISTRATOR')"))
				.anyRequest().permitAll()
		);

		return http.build();
	}

//	@Bean
//	CorsConfigurationSource corsConfigurationSource() {
//		CorsConfiguration configuration = new CorsConfiguration();
//		configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
//		configuration.setAllowedMethods(Arrays.asList("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH", "OPTIONS"));
//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//		source.registerCorsConfiguration("/**", configuration);
//		return source;
//	}
}