package co.il.avivsmile.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class UserProfileDto {
	
	Integer idUser;
	String firstName;
	String lastName;
	Set<String> roles;

}