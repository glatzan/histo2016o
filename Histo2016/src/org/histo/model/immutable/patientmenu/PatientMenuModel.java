package org.histo.model.immutable.patientmenu;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import lombok.Getter;
import lombok.Setter;

@Entity
@Immutable
@Getter
@Setter
public class PatientMenuModel {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(updatable = false, nullable = false)
	private Long id;
	
	@Column(columnDefinition = "VARCHAR")
	private String lastName;

	@Column(columnDefinition = "VARCHAR")
	private String firstName;
		
	@Column(columnDefinition = "VARCHAR")
	private String title = "";	
	
	@Column
	private String piz = "";

	@Column(columnDefinition = "VARCHAR")
	@Type(type = "date")
	private Date birthday;
	
	@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id DESC")
	private List<TaskMenuModel> tasks;
}
