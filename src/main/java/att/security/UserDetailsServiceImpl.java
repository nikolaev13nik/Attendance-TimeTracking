package att.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

import att.dao.UserRepository;
import att.model.User;


@Service
public class UserDetailsServiceImpl implements UserDetailsService{
	
	@Autowired
	UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String id)  {
		
		User user = userRepository.findById(Integer.parseInt(id))
								.orElseThrow(()->new UsernameNotFoundException(id));
		String password = user.getPassword();
		Set<String>roles=user.getRoles()
				.stream()
				.map(r->"ROLE_"+r.toUpperCase())
				.collect(Collectors.toSet());
		return new org.springframework.security.core.userdetails.User(id, password, AuthorityUtils.createAuthorityList(roles.toArray(new String[roles.size()])));
	}

}
