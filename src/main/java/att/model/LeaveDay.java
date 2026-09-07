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
@Table(name = "att_user_leave_days")
@EqualsAndHashCode(of = "leaveDayKey")
public class LeaveDay {

    @EmbeddedId
    LeaveDayKey leaveDayKey;

    double amount;
    @Column(name = "updated_by")
    String updatedBy;
    @Column(name = "sys_update_date")
    OffsetDateTime sysUpdateDate;

}
