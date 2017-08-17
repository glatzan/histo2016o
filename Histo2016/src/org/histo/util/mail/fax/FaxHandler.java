package org.histo.util.mail.fax;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.histo.action.handler.SettingsHandler;
import org.histo.model.PDFContainer;
import org.histo.util.interfaces.FileHandlerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Lazy;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class FaxHandler {

	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Lazy
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SettingsHandler settingsHandler;

	private FaxSettings settings;

	public void sendFax(String faxNumber, PDFContainer container) {
		sendFax(Arrays.asList(new String[] { faxNumber }), container);
	}

	/**
	 * Sends a fax to the given nubers
	 * 
	 * @param faxNumbers
	 * @param container
	 */
	public void sendFax(List<String> faxNumbers, PDFContainer container) {
		String faxCommand = getSettings().getCommand();

		if (faxCommand != null && faxNumbers.size() > 0) {

			File workingDirectory = new File(
					FileHandlerUtil.getAbsolutePath(settingsHandler.getProgramSettings().getWorkingDirectory()));

			File tempFile = new File(workingDirectory.getAbsolutePath() + File.separator + settings.getFileName());

			FileHandlerUtil.saveContentOfFile(tempFile, container.getData());

			for (String faxNumber : faxNumbers) {
				String tmp = faxCommand.replace("$faxNumber", faxNumber).replace("$file",
						tempFile.getPath().replaceAll("\\\\", "/"));

				String[] tmpArr = tmp.split(settings.getCommandSplitter());

				logger.debug("Using faxCommand: " + tmp);

				try {

					ProcessBuilder builder = new ProcessBuilder(tmpArr);
//					builder.redirectErrorStream(true);
					Process p = builder.start();
//					BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
//					String line;
//					while (true) {
//						line = r.readLine();
//						if (line == null) {
//							break;
//						}
//					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Getter
	@Setter
	public class FaxSettings {
		private String fileName;
		private String windows_command;
		private String linux_command;
		private String commandSplitter;

		public String getCommand() {
			if (SystemUtils.IS_OS_WINDOWS) {
				logger.debug("Sending fax, windows detected!");
				return getWindows_command();
			} else if (SystemUtils.IS_OS_LINUX) {
				logger.debug("Sending fax, windows detected!");
				return getLinux_command();
			} else {
				logger.debug("Sending fax, not supported os detected!");
				return null;
			}
		}
	}
}
