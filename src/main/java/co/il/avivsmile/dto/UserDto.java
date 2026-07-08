package co.il.avivsmile.dto;

import java.util.List;

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
public class UserDto {

	Integer idUser;
	String firstName;
	String lastName;
//	String password;
//	List<DataTimeDto>records;
	
	
}
