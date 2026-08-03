package att.dao;



import org.springframework.data.jpa.repository.JpaRepository;

import att.model.DataTime;

public interface UserAttendanceTimeRepository extends JpaRepository<DataTime, Integer> {
	

}
