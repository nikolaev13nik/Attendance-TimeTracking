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

    @Query(value = "SELECT l.* FROM att_user_leave_days l " +
            "WHERE l.tenant_id = :tenantId " +
            "AND l.leave_date BETWEEN :startDate AND :endDate " +
            "AND COALESCE(l.vacation_amount, 0) + COALESCE(l.sick_amount, 0) >= :fullDay " +
            "AND EXISTS (SELECT 1 FROM att_work_sessions s " +
            "WHERE s.tenant_id = l.tenant_id " +
            "AND s.user_id = l.user_id " +
            "AND s.work_date = l.leave_date) " +
            "ORDER BY l.user_id, l.leave_date",
            nativeQuery = true)
    List<LeaveDay> findFullLeaveDaysWithWorkSessions(@Param("tenantId") Integer tenantId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("fullDay") Double fullDay);
}
