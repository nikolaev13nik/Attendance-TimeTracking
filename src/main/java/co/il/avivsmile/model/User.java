package co.il.avivsmile.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;

@ToString
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
@EqualsAndHashCode(of = "idUser")
public class User {

	@Id
	Integer idUser;
	String firstName;
	String lastName;
	String password;
	
	
	
	
	
	@OneToMany(mappedBy = "user")
	List<DataTime>records;
	
	
	@Singular
	@ElementCollection(fetch = FetchType.EAGER)
	Set<String> roles;
	
	public boolean addRole(String role) {
		return roles.add(role);
	}

	public boolean removeRole(String role) {
		return roles.remove(role);
	}

	public User(Integer idUser, String firstName, String lastName, String password, Set<String> roles) {
		this.idUser = idUser;
		this.firstName = firstName;
		this.lastName = lastName;
		this.password = password;
		this.roles = roles;
		this.records=new ArrayList<>();
	}
	
}
