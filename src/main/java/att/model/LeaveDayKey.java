package att.model;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveDayKey implements Serializable {

    @Column(name = "tenant_id")
    Integer tenantId;
    @Column(name = "user_id")
    Integer idUser;
    @Column(name = "leave_date")
    LocalDate leaveDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", length = 10)
    LeaveType leaveType;

}
