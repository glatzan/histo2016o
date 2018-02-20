package org.histo.adaptors.patientid;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.histo.model.patient.Patient;

import lombok.Getter;
import lombok.Setter;

public class PatientIDGenerator {

	public long generatePatientID(String patientData) {
		return 0;
	}

	@XmlRootElement(namespace = "PatientData")
	@Getter
	@Setter
	public class PatientData {

		public void PatientData(Patient patient) {
			setSex(patient.getPerson().getGender());
		}
		
		@XmlElement(name = "Sex")
		private char sex;
		@XmlElement(name = "Surename")
		private String surename;
		@XmlElement(name = "Givenname")
		private String givenname;
		@XmlElement(name = "BirthDate")
		private String birthDate;
		@XmlElement(name = "PersonalTitle")
		private String personalTitle;
		@XmlElement(name = "JobTitle")
		private String jobTitle;
		@XmlElement(name = "CountryCodePostal")
		private String countryCodePostal;
		@XmlElement(name = "PLZ")
		private String plz;
		@XmlElement(name = "City")
		private String city;
		@XmlElement(name = "Street")
		private String street;
		@XmlElement(name = "TelephoneNumber")
		private String telephoneNumber;
		
	}
}

/*
 * -<PatientData>
 * 
 * <Sex>M</Sex>
 * 
 * <Surname>Hairer</Surname>
 * 
 * <Givenname>Martin</Givenname>
 * 
 * <Birthname>Hairer</Birthname>
 * 
 * <BirthDate>1975-11-14</BirthDate>
 * 
 * <PersonalTitle>Prof. Dr.</PersonalTitle>
 * 
 * <JobTitle>Mathematiker</JobTitle>
 * 
 * <CountryCodePostal>CH</CountryCodePostal>
 * 
 * <PLZ>1202</PLZ>
 * 
 * <City>Geneve</City>
 * 
 * <Street>Avenue de France 40</Street>
 * 
 * <TelephoneNumber>+49 761 270 41680</TelephoneNumber>
 * 
 * </PatientData>
 */