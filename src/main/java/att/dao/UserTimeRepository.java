package att.dao;



import org.springframework.data.jpa.repository.JpaRepository;

import att.model.DataTime;

public interface UserTimeRepository extends JpaRepository<DataTime, Integer>{
	

}
