package org.histo.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.histo.config.enums.DateFormat;
import org.histo.model.patient.Patient;
import org.histo.model.user.HistoUser;
import org.histo.model.util.LogListener;
import org.histo.util.TimeUtil;


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
		return TimeUtil.formatDate(new Date(getTimestamp()), DateFormat.GERMAN_DATE_TIME.getDateFormat());
	}
    
}



    
