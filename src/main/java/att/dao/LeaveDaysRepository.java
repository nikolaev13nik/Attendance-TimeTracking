package att.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import att.model.LeaveDay;
import att.model.LeaveDayKey;

@Repository
public interface LeaveDaysRepository extends JpaRepository<LeaveDay, LeaveDayKey> {

    List<LeaveDay> findByLeaveDayKeyTenantIdAndLeaveDayKeyIdUser(Integer tenantId, Integer idUser);

    //todo: why we need native query if cammel case expression is good enouth
    @Query("SELECT DISTINCT l.leaveDayKey.idUser FROM LeaveDay l WHERE l.leaveDayKey.tenantId = :tenantId")
    List<Integer> findDistinctUserIdsByTenantId(@Param("tenantId") Integer tenantId);

    List<LeaveDay> findByLeaveDayKeyTenantIdAndLeaveDayKeyIdUserAndLeaveDayKeyLeaveDateBetween(
            Integer tenantId, Integer idUser, LocalDate start, LocalDate end);
}
