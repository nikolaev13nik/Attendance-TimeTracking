package co.il.avivsmile.controller;
import java.util.List;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import co.il.avivsmile.dto.UserEditDto;
import co.il.avivsmile.dto.UserProfileDto;
import co.il.avivsmile.dto.UserRegisterDto;
import co.il.avivsmile.service.UserAccountService;


//@CrossOrigin
//@CrossOrigin(origins = "http://localhost:3000")
//@CrossOrigin(allowedHeaders = "Authorization",origins ="http://localhost:3000" )

@RestController
@RequestMapping("/account")
public class UserAccountController {
	@Autowired
	UserAccountService userAccountService;
	
	@PostMapping("/user")
	public UserProfileDto register(@RequestBody UserRegisterDto userRegisterDto) {
		return userAccountService.register(userRegisterDto);
	}
	
	@PostMapping("/login")
	public UserProfileDto login(Authentication authentication) {
		return userAccountService.login(Integer.parseInt(authentication.getName()));
	}

	@DeleteMapping("/user/{idUser}")
	public UserProfileDto removeUser(@PathVariable Integer idUser) {
		return userAccountService.removeUser(idUser);
	}
	
	@PutMapping("/user/password/{idUser}")
	public UserProfileDto editUser(@RequestBody UserEditDto userEditDto,
									@PathVariable Integer idUser
			) {
		return userAccountService.editUser(idUser, userEditDto);
	}
	
	@PostMapping("/user/{idUser}/role/{role}")
	public Set<String> addRole(@PathVariable Integer idUser, @PathVariable String role){
		return userAccountService.addRole(idUser, role);
	}
	
	@DeleteMapping("/user/{idUser}/role/{role}")
	public Set<String> removeRole(@PathVariable Integer idUser, @PathVariable String role){
		return userAccountService.removeRole(idUser, role);
	}
	
//	@PutMapping("/user/password/{idUser}")
//	public boolean changePassword(@PathVariable Integer idUser, @RequestHeader("X-Password") String password) {
//		return userAccountService.changePassword(idUser, password);
//	}


	@GetMapping("/users")
	public List<UserProfileDto> getAllUsers(){
		return userAccountService.getAllUsers();
	}

}
