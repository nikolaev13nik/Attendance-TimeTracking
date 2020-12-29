package co.il.avivsmile.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor

public class EditDataTimeUserDto {
	
	Integer id;
	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime start;
	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime finish;

}
