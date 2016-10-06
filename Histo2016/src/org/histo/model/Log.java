package org.histo.model;

import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.histo.config.HistoSettings;
import org.histo.model.patient.Patient;
import org.histo.model.util.LogListener;
import org.histo.util.TimeUtil;
import org.hibernate.envers.RevisionEntity;

import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.GeneratedValue;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;


@Entity
@SequenceGenerator(name = "log_sequencegenerator", sequenceName = "log_sequence")
@RevisionEntity(LogListener.class)
public class Log {
	
    private int id;
    
    private long timestamp;
    
    private String logString;
    
    private HistoUser histoUser;
    
    private Patient patient;
    
	@Id
	@GeneratedValue(generator = "log_sequencegenerator")
	@Column(unique = true, nullable = false)
    @RevisionNumber
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

    @RevisionTimestamp
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getLogString() {
		return logString;
	}

	public void setLogString(String logString) {
		this.logString = logString;
	}

	@OneToOne
	public HistoUser getUserAcc() {
		return histoUser;
	}

	public void setUserAcc(HistoUser histoUser) {
		this.histoUser = histoUser;
	}

	@OneToOne
	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	
	/**
	 * Returns Date as human readable string.
	 * @return
	 */
	@Transient
	public String getTimestampAsDate(){
		return TimeUtil.formatDate(new Date(getTimestamp()), HistoSettings.STANDARD_DATEFORMAT);
	}
    
}



    
