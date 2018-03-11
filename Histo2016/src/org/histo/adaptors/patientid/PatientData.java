package org.histo.adaptors.patientid;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.histo.model.Person.Gender;
import org.histo.model.patient.Patient;
import org.histo.util.TimeUtil;

import lombok.Setter;

@XmlRootElement(namespace = "PatientData")
@Setter
public class PatientData {

	public PatientData() {
		
	}
	
	public PatientData(Patient patient) {
		// <Sex>M</Sex>
		setSex(patient.getPerson().getGender() == Gender.FEMALE ? 'W' : 'M');
		// <Surname>Hairer</Surname>
		setSurename(patient.getPerson().getFirstName());
		// <Givenname>Martin</Givenname>
		setGivenname(patient.getPerson().getLastName());
		// <BirthDate>1975-11-14</BirthDate>
		setBirthDate(TimeUtil.formatDate(patient.getPerson().getBirthday(), "yyyy-MM-dd"));
		// <PersonalTitle>Prof. Dr.</PersonalTitle>
		setPersonalTitle(patient.getPerson().getTitle());
		// <JobTitle>Mathematiker</JobTitle>
		// TODO implement job title
		// <CountryCodePostal>CH</CountryCodePostal>
		// <PLZ>1202</PLZ>
		setPlz(patient.getPerson().getContact().getPostcode());
		// <City>Geneve</City>
		setCity(patient.getPerson().getContact().getTown());
		// <Street>Avenue de France 40</Street>
		setStreet(patient.getPerson().getContact().getStreet());
		// <TelephoneNumber>+49 761 270 41680</TelephoneNumber>
		setTelephoneNumber(patient.getPerson().getContact().getPhone());
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

	public String getAsXML() {
		
		StringWriter result = new StringWriter();
		
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(PatientData.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			//jaxbMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, true);

			jaxbMarshaller.marshal(this, result);
			jaxbMarshaller.marshal(this, System.out);

		} catch (JAXBException e) {
			e.printStackTrace();
		}

		return result.toString();
	}
}