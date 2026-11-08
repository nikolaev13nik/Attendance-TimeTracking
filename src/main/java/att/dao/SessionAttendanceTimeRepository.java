package att.dao;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import att.model.DataTime;

@Repository
public interface SessionAttendanceTimeRepository extends JpaRepository<DataTime, Integer> {

    List<DataTime> findByTenantIdAndIdUserAndWorkDateOrderByIdDesc(Integer tenantId, Integer idUser,
                                                                   LocalDate workDate);

    Optional<DataTime> findByIdAndTenantId(Integer id, Integer tenantId);

    @Query(value = "SELECT COUNT(DISTINCT work_date) FROM att_work_sessions " +
            "WHERE tenant_id = :tenantId " +
            "AND user_id = :userId " +
            "AND work_date BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    Long countWorkedDaysBetween(@Param("tenantId") Integer tenantId,
                                @Param("userId") Integer userId, @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);


    @Query(value = "SELECT * FROM att_work_sessions WHERE tenant_id = :tenantId AND user_id = :userId " +
            "AND work_date BETWEEN :fromDate AND :toDate AND (open_session_date IS NULL OR close_session_date IS NULL);",
            nativeQuery = true)
    List<DataTime> findIncompleteSessions(@Param("tenantId") Integer tenantId,
                                          @Param("userId") Integer userId,
                                          @Param("fromDate") LocalDate fromDate,
                                          @Param("toDate") LocalDate toDate);

    @Query(value =
            "SELECT COALESCE(SUM(day_minutes - :thresholdMinutes), 0) " +
                    "FROM (SELECT SUM(EXTRACT(EPOCH FROM (close_session_date - open_session_date)) / 60) AS day_minutes " +
                    "FROM att_work_sessions " +
                    "WHERE tenant_id = :tenantId " +
                    "AND user_id = :userId " +
                    "AND work_date BETWEEN :fromDate AND :toDate " +
                    "AND open_session_date IS NOT NULL " +
                    "AND close_session_date IS NOT NULL " +
                    "GROUP BY work_date) daily " +
                    "WHERE day_minutes > :thresholdMinutes",
            nativeQuery = true)
    Long calculateOvertimeMinutes(
            @Param("tenantId") Integer tenantId,
            @Param("userId") Integer userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("thresholdMinutes") Long thresholdMinutes);


    @Query(value =
            "SELECT * " +
                    "FROM att_work_sessions " +
                    "WHERE tenant_id = :tenantId " +
                    "AND user_id = :userId " +
                    "AND work_date BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    List<DataTime> findByUserIdAndWorkDateBetween(@Param("tenantId") Integer tenantId, @Param("userId") Integer userId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query(value =
            "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (close_session_date - open_session_date)) / 60), 0) " +
                    "FROM att_work_sessions " +
                    "WHERE tenant_id = :tenantId " +
                    "AND user_id = :userId " +
                    "AND work_date BETWEEN :startDate AND :endDate " +
                    "AND open_session_date IS NOT NULL " +
                    "AND close_session_date IS NOT NULL",
            nativeQuery = true)
    Long calculateWorkedMinutes(
            @Param("tenantId") Integer tenantId,
            @Param("userId") Integer userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT a.* FROM att_work_sessions a " +
            "WHERE a.tenant_id = :tenantId " +
            "AND a.user_id IN (:userIds) " +
            "AND a.work_date BETWEEN :startDate AND :endDate " +
            "AND EXISTS (SELECT 1 FROM att_work_sessions b " +
            "WHERE b.tenant_id = a.tenant_id " +
            "AND b.user_id = a.user_id " +
            "AND b.work_date = a.work_date " +
            "AND b.id > a.id " +
            "AND a.open_session_date < b.close_session_date " +
            "AND b.open_session_date < a.close_session_date)",
            nativeQuery = true)
    List<DataTime> findOverlappingSessions(@Param("tenantId") Integer tenantId,
                                           @Param("userIds") Collection<Integer> userIds,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT DISTINCT user_id FROM att_work_sessions WHERE tenant_id = :tenantId",
            nativeQuery = true)
    List<Integer> findDistinctUserIdsByTenantId(@Param("tenantId") Integer tenantId);
}
