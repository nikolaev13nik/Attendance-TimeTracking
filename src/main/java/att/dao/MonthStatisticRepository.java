package att.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import att.model.MonthStatistic;
import att.model.MonthStatisticKey;

@Repository
public interface MonthStatisticRepository extends JpaRepository<MonthStatistic, MonthStatisticKey> {

    List<MonthStatistic> findTop7ByMonthStatisticKeyTenantIdAndMonthStatisticKeyIdUserAndMonthStatisticKeyMonthStartDateLessThanEqualOrderByMonthStatisticKeyMonthStartDateDesc(
            Integer tenantId, Integer idUser, LocalDate monthStartDate);
}
