package att.dao;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import att.model.DataTime;

@Repository
public interface SessionAttendanceTimeRepository extends JpaRepository<DataTime, Integer> {

    List<DataTime> findByTenantIdAndIdUserAndWorkDateOrderByIdDesc(Integer tenantId, Integer idUser,
                                                                   LocalDate workDate);

    Long countByTenantIdAndIdUserAndWorkDateBetween(
            Integer tenantId,
            Integer userId,
            LocalDate startDate,
            LocalDate endDate);


    @Query(value = "SELECT * FROM att_work_sessions WHERE tenant_id = :tenantId AND user_id = :userId " +
            "AND work_date BETWEEN :fromDate AND :toDate AND (open_session_date IS NULL OR close_session_date IS NULL);",
            nativeQuery = true)
    List<DataTime> findIncompleteSessions(@Param("tenantId") Integer tenantId,
                                          @Param("userId") Integer userId,
                                          @Param("fromDate") LocalDate fromDate,
                                          @Param("toDate") LocalDate toDate);

    @Query(value =
            "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (close_session_date - open_session_date)) / 60 - 480), 0) " +
                    "FROM att_work_sessions " +
                    "WHERE tenant_id = :tenantId " +
                    "AND user_id = :userId " +
                    "AND work_date BETWEEN :fromDate AND :toDate " +
                    "AND open_session_date IS NOT NULL " +
                    "AND close_session_date IS NOT NULL " +
                    "AND EXTRACT(EPOCH FROM (close_session_date - open_session_date)) / 60 > 480",
            nativeQuery = true)
    Long calculateOvertimeMinutes(
            @Param("tenantId") Integer tenantId,
            @Param("userId") Integer userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);


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
}
