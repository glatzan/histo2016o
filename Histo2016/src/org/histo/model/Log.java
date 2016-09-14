package org.histo.model;

import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.histo.model.util.LogListener;
import org.hibernate.envers.RevisionEntity;

import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.Entity;


@Entity
@SequenceGenerator(name = "log_sequencegenerator", sequenceName = "log_sequence")
@RevisionEntity(LogListener.class)
public class Log {
	
    private int id;
    
    private long timestamp;
    
    private String logString;
    
    private UserAcc userAcc;
    
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
	public UserAcc getUserAcc() {
		return userAcc;
	}

	public void setUserAcc(UserAcc userAcc) {
		this.userAcc = userAcc;
	}

	@OneToOne
	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	
	
    
}



    
