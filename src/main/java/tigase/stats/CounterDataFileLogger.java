/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.stats;

import tigase.collections.CircularFifoQueue;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class responsible for dumping server statistics to a file
 *
 * @author wojtek
 */
public class CounterDataFileLogger
		implements StatisticsArchivizerIfc, ConfigurationChangedAware, Initializable {

	/* logger instance */
	private static final Logger log = Logger.getLogger(CounterDataFileLogger.class.getName());
	private final static ExecutorService service = Executors.newSingleThreadScheduledExecutor();
	private final AtomicBoolean collectionReady = new AtomicBoolean(false);
	private Charset charset = StandardCharsets.UTF_8;
	/* Field holding datetime format configuration */
	@ConfigField(desc = "Format of a date time", alias = "stats-datetime-format")
	private String dateTimeFormat = "yyyy-MM-dd_HH:mm:ss";
	private DateTimeFormatter dateTimeFormatter;
	/* Field holding directory path configuration */
	@ConfigField(desc = "Directory path", alias = "stats-directory")
	private String directory = "logs/stats";
	/* Field holding file prefix configuration */
	@ConfigField(desc = "Name of a file", alias = "stats-filename")
	private String filename = "stats";
	@ConfigField(desc = "Remaining space in MB resulting in shrinking the collection", alias = "automatically-prune-resize-mb")
	private int freeSpaceMB = 100;
	@ConfigField(desc = "Remaining space in % resulting in shrinking the collection", alias = "automatically-prune-resize-percent")
	private int freeSpacePerCent = 5;
	@ConfigField(desc = "Frequency")
	private long frequency = -1;
	/* Field holding configuration whether include or not datetime timestamp into filename */
	@ConfigField(desc = "Should include date time", alias = "stats-datetime")
	private boolean includeDateTime = true;
	/* Field holding configuration whether include or not unixtime into filename */
	@ConfigField(desc = "Should include unix time", alias = "stats-unixtime")
	private boolean includeUnixTime = true;
	@ConfigField(desc = "Whether old entries should be pruned automatically", alias = "automatically-prune-limit")
	private int limit = 60 * 60 * 24;
	private CircularFifoQueue<Path> pathsQueue = null;
	@ConfigField(desc = "Whether old entries should be pruned automatically", alias = "automatically-prune-old")
	private boolean pruneOldEntries = true;
	@ConfigField(desc = "Factor by which collection will be shrunk", alias = "automatically-prune-resize-factor")
	private double shrinkFactor = 0.75;
	/* Field holding level of the statistics configuration */
	@ConfigField(desc = "Statistics detail level", alias = "stats-level")
	private Level statsLevel = Level.ALL;

	private static void deleteFile(Path file) {
		try {
			if (Files.exists(file)) {
				Files.delete(file);
			}
		} catch (IOException e) {
			log.log(Level.FINEST, "Error deleting file " + file, e);
		}
	}

	@Override
	public void execute(StatisticsProvider sp) {

		if (pruneOldEntries && !collectionReady.get()) {
			return;
		}

		final ZonedDateTime time = ZonedDateTime.now();

		Path path = Paths.get(getPath(time));

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Dumping server statistics to: {0}", path);
		}

		Map<String, String> stats = sp.getAllStats(statsLevel.intValue());
		stats.put("Statistics time", dateTimeFormatter.format(time));
		stats.put("Statistics time (linux)", Long.toString(time.toInstant().toEpochMilli()));

		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			String result = stats.entrySet()
					.stream()
					.map((e) -> e.getKey() + "\t" + e.getValue())
					.collect(Collectors.joining("\n"));
			writer.write(result);
		} catch (IOException ex) {
			log.log(Level.SEVERE, "Error dumping server statistics to file", ex);
		}

		if (pruneOldEntries) {
			pathsQueue.offer(path);

			final File file = path.toFile();
			if (pathsQueue.limit() > 0 && (file.getUsableSpace() / 1024 / 1024 < freeSpaceMB ||
					((file.getUsableSpace() * 100) / file.getTotalSpace() < freeSpacePerCent))) {
				int newLimit = (int) (pathsQueue.limit() * shrinkFactor);

				log.log(Level.FINEST,
						"Shrinking stats file history from {0} to {1} (usable space: {2}, total space: {3}",
						new Object[]{limit, newLimit, file.getUsableSpace(), file.getTotalSpace()});
				limit = newLimit;
				pathsQueue.setLimit(limit);
			}
		}
	}

	@Override
	public void release() {
		// pass
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		Paths.get(directory).toFile().mkdirs();
		dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);

		if (pruneOldEntries) {
			if (pathsQueue == null) {
				pathsQueue = new CircularFifoQueue<>(limit, CounterDataFileLogger::deleteFile);

				final Thread thread = new Thread(() -> {
					final long start = System.currentTimeMillis();
					log.log(Level.FINE, "Started collecting existing statistics files");
					try {
						final File[] files = Paths.get(directory).toFile().listFiles();
						if (files != null && files.length > 0) {
							final List<Path> existingFilesPaths = Arrays.stream(files)
									.sorted(Comparator.comparing(File::lastModified))
									.map(File::toPath)
									.collect(Collectors.toList());
							pathsQueue.addAll(existingFilesPaths);
						}
					} catch (Exception e) {
						log.log(Level.WARNING, "Reading statistics files list", e);
					}
					log.log(Level.CONFIG, "Statistics files collection finished in: {0}s ",
							new Object[]{(System.currentTimeMillis() - start) / 1000});

					collectionReady.set(true);
				});
				thread.setName("stats-files-reader");
				thread.start();

			} else {
				pathsQueue.setLimit(limit);
			}
		}
	}

	@Override
	public long getFrequency() {
		return frequency;
	}

	@Override
	public void initialize() {
		beanConfigurationChanged(Collections.emptyList());
	}

	private String getPath(ZonedDateTime time) {
		return directory + "/" + filename + (includeUnixTime ? "_" + time.toInstant().toEpochMilli() : "") +
				(includeDateTime ? "_" + dateTimeFormatter.format(time) : "") + ".txt";
	}
}
