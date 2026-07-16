package co.il.avivsmile.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import co.il.avivsmile.api.AccountApi;
import co.il.avivsmile.dto.UserEditDto;
import co.il.avivsmile.dto.UserProfileDto;
import co.il.avivsmile.dto.UserRegisterDto;
import co.il.avivsmile.service.UserAccountService;

@RestController
public class UserAccountController implements AccountApi {

	@Autowired
	UserAccountService userAccountService;

	@Override
	public ResponseEntity<UserProfileDto> register(UserRegisterDto userRegisterDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userAccountService.register(userRegisterDto));
	}

	@Override
	public ResponseEntity<UserProfileDto> removeUser(Integer idUser) {
		return ResponseEntity.ok(userAccountService.removeUser(idUser));
	}

	@Override
	public ResponseEntity<UserProfileDto> editUser(Integer idUser, UserEditDto userEditDto) {
		return ResponseEntity.ok(userAccountService.editUser(idUser, userEditDto));
	}

	@Override
	public ResponseEntity<UserProfileDto> addRole(Integer idUser, String role) {
		return ResponseEntity.ok(userAccountService.addRole(idUser, role));
	}

	@Override
	public ResponseEntity<UserProfileDto> removeRole(Integer idUser, String role) {
		return ResponseEntity.ok(userAccountService.removeRole(idUser, role));
	}

	@Override
	public ResponseEntity<List<UserProfileDto>> getAllUsers() {
		return ResponseEntity.ok(userAccountService.getAllUsers());
	}

	@PostMapping("/account/login")
	public ResponseEntity<UserProfileDto> login(Authentication authentication) {
		return ResponseEntity.ok(userAccountService.login(Integer.parseInt(authentication.getName())));
	}
}