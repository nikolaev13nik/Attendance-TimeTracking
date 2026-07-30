package att.service;

import java.util.List;

import att.dto.UserEditDto;
import att.dto.UserProfileDto;
import att.dto.UserRegisterDto;


public interface UserAccountService {
	
	UserProfileDto register(UserRegisterDto userRegisterDto);
	
	UserProfileDto login(Integer idUser);
	
	UserProfileDto editUser(Integer idUser, UserEditDto userEditDto);
	
	UserProfileDto removeUser(Integer idUser);
	
	boolean changePassword(Integer idUser, String password);

	UserProfileDto addRole(Integer idUser, String role);

	UserProfileDto removeRole(Integer idUser, String role);

	List<UserProfileDto>getAllUsers();
}
