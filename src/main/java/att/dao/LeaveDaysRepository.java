package att.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import att.model.LeaveDay;

@Repository
public interface LeaveDaysRepository extends JpaRepository<LeaveDay, Integer> {

    Optional<LeaveDay> findByIdAndTenantId(Integer id, Integer tenantId);

    Optional<LeaveDay> findByTenantIdAndIdUserAndLeaveDate(Integer tenantId, Integer idUser, LocalDate leaveDate);

    List<LeaveDay> findByTenantIdAndIdUserAndLeaveDateIn(Integer tenantId, Integer idUser,
                                                         Collection<LocalDate> leaveDates);

    List<LeaveDay> findByTenantIdAndIdUser(Integer tenantId, Integer idUser);

    @Query("SELECT DISTINCT l.idUser FROM LeaveDay l WHERE l.tenantId = :tenantId")
    List<Integer> findDistinctUserIdsByTenantId(@Param("tenantId") Integer tenantId);

    List<LeaveDay> findByTenantIdAndIdUserAndLeaveDateBetween(
            Integer tenantId, Integer idUser, LocalDate start, LocalDate end);
}
