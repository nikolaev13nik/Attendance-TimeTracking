package att.dao;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import att.model.MonthStatistic;
import att.model.MonthStatisticKey;

@Repository
public interface MonthStatisticRepository extends JpaRepository<MonthStatistic, MonthStatisticKey> {

    @Query("SELECT m FROM MonthStatistic m WHERE m.monthStatisticKey.tenantId = :tenantId "
            + "AND m.monthStatisticKey.idUser = :idUser AND m.monthStatisticKey.monthStartDate <= :upTo "
            + "ORDER BY m.monthStatisticKey.monthStartDate DESC")
    List<MonthStatistic> findRecentMonths(@Param("tenantId") Integer tenantId, @Param("idUser") Integer idUser,
                                          @Param("upTo") LocalDate upTo, Limit limit);
}
