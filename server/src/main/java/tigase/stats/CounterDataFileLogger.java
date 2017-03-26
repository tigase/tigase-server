/*
 * CounterDataLogger.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */
package tigase.stats;

//~--- non-JDK imports --------------------------------------------------------
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for dumping server statistics to a file
 *
 * @author wojtek
 */
public class CounterDataFileLogger
		implements StatisticsArchivizerIfc, ConfigurationChangedAware, Initializable {

	/* logger instance */
	private static final Logger log = Logger.getLogger( CounterDataFileLogger.class.getName() );

	/* Field denoting directory configuration */
	private static final String DIRECTORY_KEY = "stats-directory";
	/* Field denoting file prefix configuration */
	private static final String FILENAME_KEY = "stats-filename";
	/* Field denoting whether include or not unixtime into filename configuration */
	private static final String UNIXTIME_KEY = "stats-unixtime";
	/* Field denoting whether include or not datetime timestamp into filename configuration */
	private static final String DATETIME_KEY = "stats-datetime";
	/* Field denoting datetime format configuration */
	private static final String DATETIME_FORMAT_KEY = "stats-datetime-format";
	/* Field denoting level of the statistics configuration */
	private static final String STATISTICS_LEVE_KEY = "stats-level";

	/* Field holding directory path configuration */
	@ConfigField(desc = "Directory path", alias = DIRECTORY_KEY)
	private static String directory = "logs/stats";
	/* Field holding file prefix configuration */
	@ConfigField(desc = "Name of a file", alias = FILENAME_KEY)
	private static String filename = "stats";
	/* Field holding configuration whether include or not unixtime into filename */
	@ConfigField(desc = "Should include unix time", alias = UNIXTIME_KEY)
	private static boolean includeUnixTime = true;
	/* Field holding configuration whether include or not datetime timestamp into filename */
	@ConfigField(desc = "Should include date time", alias = DATETIME_KEY)
	private static boolean includeDateTime = true;

	/* Field holding datetime format configuration */
	@ConfigField(desc = "Format of a date time", alias = DATETIME_FORMAT_KEY)
	private static String dateTimeFormat = "yyyy-MM-dd_HH:mm:ss";

	/* Field holding level of the statistics configuration */
	@ConfigField(desc = "Statistics detail level", alias = STATISTICS_LEVE_KEY)
	private static Level statsLevel = Level.ALL;

	@ConfigField(desc = "Frequency")
	private long frequency = -1;

	/* Variable for SimpleDateFormat */
	private static SimpleDateFormat sdf;

	@Override
	public void execute( StatisticsProvider sp ) {

		Date currTime = new Date();
		String path = directory + "/"
									+ filename
									+ ( includeUnixTime ? "_" + currTime.getTime() : "" )
									+ ( includeDateTime ? "_" + sdf.format( currTime ) : "" )
									+ ".txt";

		log.log( Level.FINEST, "Dumping server statistics to: {0}", path );
		Path p = Paths.get( path);
		
		Map<String, String> stats = sp.getAllStats( statsLevel.intValue() );

		try (BufferedWriter writer = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> entry : stats.entrySet()) {
				sb.append(entry.getKey()).append("\t");
				sb.append(entry.getValue()).append("\n");
			}
			sb.append("Statistics time: ").append(sdf.format(currTime)).append("\n");
			sb.append("Statistics time (linux): ").append(currTime.getTime()).append("\n");

			writer.write(sb.toString());
		} catch ( IOException ex ) {
			log.log( Level.SEVERE, "Error dumping server statistics to file", ex );
		}
	}

	@Override
	public void release() {
		// pass
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		new File( directory ).mkdirs();
		sdf = new SimpleDateFormat( dateTimeFormat );
	}

	@Override
	public long getFrequency() {
		return frequency;
	}

	@Override
	public void initialize() {
		beanConfigurationChanged(Collections.emptyList());
	}
}
