package org.histo.service;

import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class FileService {

	public void addFileToPatient(String piz, PDFContainer container) {
		
	}
	
	public void addFileToPatient(Patient patient, PDFContainer container) {
		
	}
}
