package att.model;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthStatisticKey implements Serializable {

    @Column(name = "tenant_id")
    Integer tenantId;
    @Column(name = "user_id")
    Integer idUser;
    @Column(name = "month_start_date")
    LocalDate monthStartDate;

}
