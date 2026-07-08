package co.il.avivsmile.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class DataTimeDto {
	
	Integer id;
	UserDto user;
	
	@JsonFormat(pattern="yyyy-MM-dd")
	LocalDate date;
	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime start;
	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime finish;
	
	
}
