package att.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import att.model.MonthStatistic;
import att.model.MonthStatisticKey;

@Repository
public interface MonthStatisticRepository extends JpaRepository<MonthStatistic, MonthStatisticKey> {
}
