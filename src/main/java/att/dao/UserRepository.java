package att.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import att.model.User;

public interface UserRepository extends JpaRepository<User, Integer>{

}
