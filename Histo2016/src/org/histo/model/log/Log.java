package org.histo.model.log;

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
import org.histo.config.log.LogListener;
import org.histo.model.patient.Patient;
import org.histo.model.user.HistoUser;
import org.histo.util.TimeUtil;

import lombok.Getter;
import lombok.Setter;


@Entity
@SequenceGenerator(name = "log_sequencegenerator", sequenceName = "log_sequence")
@RevisionEntity(LogListener.class)
@Getter
@Setter
public class Log {
	
	@Id
	@GeneratedValue(generator = "log_sequencegenerator")
	@Column(unique = true, nullable = false)
    @RevisionNumber
    private int id;
    
	@RevisionTimestamp
    private long timestamp;
    
	@Column(columnDefinition = "text")
    private String logString;
    
    @OneToOne
    private HistoUser histoUser;
    
	@OneToOne
    private Patient patient;
    
	/**
	 * Returns Date as human readable string.
	 * @return
	 */
	@Transient
	public String getTimestampAsDate(){
		return TimeUtil.formatDate(new Date(getTimestamp()), DateFormat.GERMAN_DATE_TIME.getDateFormat());
	}
    
}



    
