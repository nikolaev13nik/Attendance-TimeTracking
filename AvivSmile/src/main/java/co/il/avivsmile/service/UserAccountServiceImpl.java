package co.il.avivsmile.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.il.avivsmile.dao.UserRepository;
import co.il.avivsmile.dto.UserDto;
import co.il.avivsmile.dto.UserEditDto;
import co.il.avivsmile.dto.UserProfileDto;
import co.il.avivsmile.dto.UserRegisterDto;
import co.il.avivsmile.exceptions.UserExistsException;
import co.il.avivsmile.exceptions.UserNotFoundException;
import co.il.avivsmile.model.User;

@Service
public class UserAccountServiceImpl implements UserAccountService {

//	@Autowired
	final UserRepository accountRepository;

//	@Autowired
//	AccountConfiguration accountConfiguration;

//	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	public UserAccountServiceImpl(UserRepository accountRepository, PasswordEncoder passwordEncoder) {
		super();
		this.accountRepository = accountRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public UserProfileDto register(UserRegisterDto userRegisterDto) {
		if (accountRepository.existsById(userRegisterDto.getIdUser())) {
			throw new UserExistsException();
		}
		String hashPassword = passwordEncoder.encode(userRegisterDto.getPassword());
//		String hashPassword = Base64.getEncoder().encodeToString(userRegisterDto.getPassword().getBytes());
		User user = User.builder().idUser(userRegisterDto.getIdUser()).password(hashPassword)
				.firstName(userRegisterDto.getFirstName()).lastName(userRegisterDto.getLastName()).role("User")
				.records(new ArrayList<>()).build();
		accountRepository.save(user);
		return userToUserProfileDto(user);
	}

	private UserProfileDto userToUserProfileDto(User userAccount) {
		return UserProfileDto.builder().idUser(userAccount.getIdUser()).firstName(userAccount.getFirstName())
				.lastName(userAccount.getLastName()).roles(userAccount.getRoles()).build();
	}

	@Override
	public UserProfileDto login(Integer idUser) {
		User user = accountRepository.findById(idUser).get();
		return userToUserProfileDto(user);
	}

	@Transactional
	@Override
	public UserProfileDto editUser(Integer idUser, UserEditDto userEditDto) {
		User user = accountRepository.findById(idUser).get();
		if (userEditDto.getFirstName() != null) {
			user.setFirstName(userEditDto.getFirstName());
		}
		if (userEditDto.getLastName() != null) {
			user.setLastName(userEditDto.getLastName());
		}
		if (userEditDto.getPassword() != null) {
			String hashPassword = passwordEncoder.encode(userEditDto.getPassword());
			user.setPassword(hashPassword);
		}
		accountRepository.save(user);
		return userToUserProfileDto(user);
	}

	@Transactional
	@Override
	public UserProfileDto removeUser(Integer idUser) {
		User userAccount = accountRepository.findById(idUser)
				.orElseThrow(() -> new UserNotFoundException(idUser.toString()));
		accountRepository.deleteById(idUser);
		return userToUserProfileDto(userAccount);
	}

	@Transactional
	@Override
	public boolean changePassword(Integer idUser, String password) {
		User user = accountRepository.findById(idUser).get();
		String hashPassword = passwordEncoder.encode(password);
		user.setPassword(hashPassword);
		return true;

	}

	@Transactional
	@Override
	public Set<String> addRole(Integer idUser, String role) {
		User user = accountRepository.findById(idUser).orElseThrow(() -> new UserNotFoundException(idUser.toString()));
		user.addRole(role);
		accountRepository.save(user);
		return user.getRoles();
	}

	@Transactional
	@Override
	public Set<String> removeRole(Integer idUser, String role) {
		User userAccount = accountRepository.findById(idUser)
				.orElseThrow(() -> new UserNotFoundException(idUser.toString()));
		userAccount.removeRole(role);
		accountRepository.save(userAccount);
		return userAccount.getRoles();
	}

	@Override
	public List<UserProfileDto> getAllUsers() {
		List<User> listUsers = accountRepository.findAll();
		return listUsers.stream().map(dto -> userToUserProfileDto(dto)).collect(Collectors.toList());
	}

}
