package co.il.avivsmile.security;


import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;




@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true )
public class SecurityConfiguration extends WebSecurityConfigurerAdapter{
	
	
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/h2-console/**")
						.antMatchers(HttpMethod.POST,"/account/user");
	}
	
	@Override
	protected void configure(HttpSecurity http)throws Exception{
		
		http.httpBasic();
		http.csrf().disable();
		http.cors().disable();
		
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS );

		http.authorizeRequests()
								.antMatchers(HttpMethod.POST,"/actuator/shutdown").hasRole("ADMINISTRATOR")		

								.antMatchers(HttpMethod.GET,"/record/**").hasRole("ADMINISTRATOR")
								.antMatchers(HttpMethod.PUT,"/record/start/{idUser}").access("@customWebSecurity.checkStartFinishRecords(#idUser,authentication) or hasRole('ADMINISTRATOR')")
								.antMatchers(HttpMethod.PUT,"/record/finish/{idUser}").access("@customWebSecurity.checkStartFinishRecords(#idUser,authentication) or hasRole('ADMINISTRATOR')")
								.antMatchers(HttpMethod.POST,"/record").hasRole("ADMINISTRATOR")
								.antMatchers(HttpMethod.DELETE,"/record/remove/{id}").hasRole("ADMINISTRATOR")						
		
								
//								.antMatchers(HttpMethod.PUT,"/account/user").authenticated()
								.antMatchers(HttpMethod.DELETE,"/account/user/{idUser}").hasRole("ADMINISTRATOR")
								.antMatchers(HttpMethod.POST,"/account/login").authenticated()
								.antMatchers(HttpMethod.POST,"/account/user/{idUser}/role/{role}").hasRole("ADMINISTRATOR")
								.antMatchers(HttpMethod.DELETE,"/account/user/{idUser}/role/{role}").hasRole("ADMINISTRATOR")
								.antMatchers(HttpMethod.PUT,"/account/user/password/{idUser}").access("@customWebSecurity.checkAuthorityChangePassword(#idUser,authentication) or hasRole('ADMINISTRATOR')");
								
	}
	
//	@Bean
//	CorsConfigurationSource corsConfigurationSource() {
//		CorsConfiguration configuration = new CorsConfiguration();
//		configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
//		configuration.setAllowedMethods(Arrays.asList("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH","OPTIONS"));
//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//		source.registerCorsConfiguration("/**", configuration);
//		return source;
//	}

}
