package att.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import att.model.LeaveDay;
import att.model.LeaveDayKey;

@Repository
public interface LeaveDaysRepository extends JpaRepository<LeaveDay, LeaveDayKey> {

    List<LeaveDay> findByLeaveDayKeyTenantIdAndLeaveDayKeyIdUser(Integer tenantId, Integer idUser);
}
