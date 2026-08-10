package att.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
//@NoArgsConstructor
//@Builder
@Entity
@Table(name = "att_work_sessions")
@EqualsAndHashCode(of = "id")
public class DataTime {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Integer id;
	@Column(name = "user_id")
	Integer idUser;
	@Column(name = "tenant_id")
	Integer tenantId;
	@JsonFormat(pattern="yyyy-MM-dd")
	@Column(name = "work_date")
	LocalDate workDate;
	@Column(name = "open_session_date")
	OffsetDateTime openSessionDate;
	@Column(name = "close_session_date")
	OffsetDateTime closeSessionDate;
	String status;
	@Column(name = "updated_by")
	String updatedBy;
	@Column(name = "sys_update_date")
	OffsetDateTime sysUpdateDate;


}
