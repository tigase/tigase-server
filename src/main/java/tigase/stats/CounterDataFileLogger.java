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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
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
		implements StatisticsArchivizerIfc {

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
	private static String directory = "logs/stats";
	/* Field holding file prefix configuration */
	private static String filename = "stats";
	/* Field holding configuration whether include or not unixtime into filename */
	private static boolean includeUnixTime = true;
	/* Field holding configuration whether include or not datetime timestamp into filename */
	private static boolean includeDateTime = true;

	/* Field holding datetime format configuration */
	private static String dateTimeFormat = "yyyy-MM-dd_HH:mm:ss";

	/* Field holding level of the statistics configuration */
	private static Level statsLevel = Level.ALL;

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
	public void init( Map<String, Object> archivizerConf ) {
		if ( null != archivizerConf.get( DIRECTORY_KEY ) ){
			directory = String.valueOf( archivizerConf.get( DIRECTORY_KEY ) );
		}
		new File( directory ).mkdirs();
		log.log( Level.CONFIG, "Setting CounterDataFileLogger directory to: {0}", directory );

		if ( null != archivizerConf.get( FILENAME_KEY ) ){
			filename = (String) archivizerConf.get( FILENAME_KEY );
		}
		log.log( Level.CONFIG, "Setting CounterDataFileLogger filename to: {0}", filename );

		if ( null != archivizerConf.get( UNIXTIME_KEY ) ){
			includeUnixTime = Boolean.valueOf( (String) archivizerConf.get( UNIXTIME_KEY ) );
		}
		log.log( Level.CONFIG, "Setting CounterDataFileLogger includeUnixTime to: {0}", includeUnixTime );

		if ( null != archivizerConf.get( DATETIME_KEY ) ){
			includeDateTime = Boolean.valueOf( (String) archivizerConf.get( DATETIME_KEY ) );
		}
		log.log( Level.CONFIG, "Setting CounterDataFileLogger includeDateTime to: {0}", includeDateTime );

		if ( null != archivizerConf.get( DATETIME_FORMAT_KEY ) ){
			dateTimeFormat = (String) archivizerConf.get( DATETIME_FORMAT_KEY );
		}
		log.log( Level.CONFIG, "Setting CounterDataFileLogger dateTimeFormat to: {0}", dateTimeFormat );
		sdf = new SimpleDateFormat( dateTimeFormat );

		if ( null != archivizerConf.get( STATISTICS_LEVE_KEY ) ){
			statsLevel = Level.parse( (String) archivizerConf.get( STATISTICS_LEVE_KEY ) );
		}
		log.log( Level.CONFIG, "Setting CounterDataFileLogger statsLevel to: {0}", statsLevel );

	}

	@Override
	public void release() {
		// pass
	}
}
