package org.histo.template.documents;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.histo.config.enums.DateFormat;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.util.FileUtil;
import org.histo.util.HistoUtil;

public class TemplateSlideLable extends DocumentTemplate {

	private Slide slide;
	private Date date;

	public void initData(Task task, Slide slide, Date date) {
		this.task = task;
		this.slide = slide;
		this.date = date;

		if (HistoUtil.isNotNullOrEmpty(getContent()))
			setFileContent(FileUtil.getContentOfFile(getContent()));

		if (HistoUtil.isNotNullOrEmpty(getContent2()))
			setFile2Content(FileUtil.getContentOfFile(getContent2()));
	}

	public void fillTemplate() {

//		String result = getFileContent().replaceAll("%slideNumber%",
//				task.getTaskID() + HistoUtil.fitString(slide.getUniqueIDinTask(), 3, '0'));
//		result = result.replaceAll("%slideName%", task.getTaskID() + " " + slide.getSlideID());
//		result = result.replaceAll("%slideText%", slide.getCommentary());
//		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
//		result = result.replaceAll("%date%",
//				ldt.format(DateTimeFormatter.ofPattern(DateFormat.GERMAN_DATE.getDateFormat())));
//
//		setFileContent(result);
	}

}
