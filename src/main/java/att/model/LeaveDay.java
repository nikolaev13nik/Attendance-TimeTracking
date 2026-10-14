package att.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "att_user_leave_days",
        uniqueConstraints = @UniqueConstraint(name = "uq_att_user_leave_days",
                columnNames = {"tenant_id", "user_id", "leave_date"}))
@EqualsAndHashCode(of = "id")
public class LeaveDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @Column(name = "tenant_id", nullable = false)
    Integer tenantId;
    @Column(name = "user_id", nullable = false)
    Integer idUser;
    @Column(name = "leave_date", nullable = false)
    LocalDate leaveDate;
    @Column(name = "vacation_amount")
    Double vacationAmount;
    @Column(name = "sick_amount")
    Double sickAmount;
    @Column(name = "updated_by")
    String updatedBy;
    @Column(name = "sys_update_date")
    OffsetDateTime sysUpdateDate;

}
