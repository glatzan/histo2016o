package org.histo.adaptors.patientid;

import java.io.File;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.histo.model.Person.Gender;
import org.histo.model.patient.Patient;
import org.histo.util.TimeUtil;

import com.sun.prism.paint.Paint;

import lombok.Getter;
import lombok.Setter;

/*
 * -<PatientData>
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
public class PatientIDGenerator {

	public long generatePatientID(Patient patient) {
		(new PatientData(patient)).getAsXML();
		return 0;
	}
}
