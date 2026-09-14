package att.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "att_month_statistic")
@EqualsAndHashCode(of = "monthStatisticKey")
public class MonthStatistic {

    @EmbeddedId
    MonthStatisticKey monthStatisticKey;

    @Column(name = "work_days")
    Integer workDays;
    @Column(name = "overtime_hours")
    Double overtimeHours;
    @Column(name = "total_work_hours")
    Double totalWorkHours;
    @Column(name = "vacation_days")
    Double vacationDays;
    @Column(name = "sick_days")
    Double sickDays;
    @Column(name = "updated_by")
    String updatedBy;
    @Column(name = "sys_update_date")
    OffsetDateTime sysUpdateDate;

}
