/*
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
package tigase.sys;

import tigase.annotations.TigaseDeprecated;

import javax.management.JMException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NativeMemoryTracking {

	private static final Logger log = Logger.getLogger(NativeMemoryTracking.class.getName());
	private static final Pattern subScopeHeaderPattern = Pattern.compile(
			"^-\\s+([\\w\\s]+)\\(reserved=(\\d+)[GKM]B, committed=(\\d+)[GKM]B\\).*");
	private static final Pattern mmapPattern = Pattern.compile(
			"^\\s+\\(mmap: reserved=(\\d+)[GKM]B, committed=(\\d+)[GKM]B\\).*");
	private static final Pattern mallocArenaPattern = Pattern.compile("^\\s*\\((malloc|arena)=(\\d+)[GKM]B.*\\).*");
	private final SCALE scale;
	private final Map<String, NMTScope> scopes;

	enum SCALE {
		KB,
		MB,
		GB
	}

	private static String executeMBeanCommand(String command, String... args) throws JMException {
		return (String) ManagementFactory.getPlatformMBeanServer()
				.invoke(new ObjectName("com.sun.management:type=DiagnosticCommand"), command, new Object[]{args},
						new String[]{"[Ljava.lang.String;"});
	}

	private static Map<String, NMTScope> getNMTScopesFrom(String summary) {
		Map<String, NMTScope> scopes = new ConcurrentHashMap<>();
		final String[] summaryLines = summary.split("\\r?\\n");
		for (int currentLine = 0; currentLine < summaryLines.length; currentLine++) {
			String summaryLine = summaryLines[currentLine];
			if (summaryLine.startsWith("Total")) {
				processTotalScope(scopes, summaryLine);
			} else if (summaryLine.startsWith("-")) {
				currentLine = processSubScope(scopes, summaryLines, currentLine);
			}
		}
		return scopes;
	}

	static Optional<NativeMemoryTracking> getNativeMemoryTracking() {
		return getNativeMemoryTracking(SCALE.MB);
	}

	static Optional<NativeMemoryTracking> getNativeMemoryTracking(SCALE scale) {
		final Optional<String> nativeMemoryTrackingSummary = getNativeMemoryTrackingTextSummary(scale);
		return nativeMemoryTrackingSummary.flatMap((String summary) -> parse(summary, scale));
	}

	private static Optional<String> getNativeMemoryTrackingTextSummary(SCALE scale) {
		String result = null;
		try {
			result = executeMBeanCommand("vmNativeMemory", "summary", "scale=" + scale);
		} catch (Exception e) {
			log.log(Level.FINER, e, () -> "There was a problem obtaining NMT summary");
		}
		return Optional.ofNullable(result);
	}

	public static void main(String[] args) {
		System.out.println(NativeMemoryTracking.getNativeMemoryTracking(SCALE.MB));
	}

	static Optional<NativeMemoryTracking> parse(String summary) {
		return parse(summary, SCALE.KB);
	}

	static Optional<NativeMemoryTracking> parse(String summary, SCALE scale) {
		Map<String, NMTScope> scopes = getNMTScopesFrom(summary);
		return scopes != null && !scopes.isEmpty()
			   ? Optional.of(new NativeMemoryTracking(scopes, scale))
			   : Optional.empty();
	}

	private static void processMallocAndArena(NMTScope.NMTScopeBuilder scopeBuilder, String subSummaryLine,
											  Matcher mallocArenaMatcher) {
		String type = mallocArenaMatcher.group(1);
		switch (type) {
			case "malloc":
				tryParsingMatched(mallocArenaMatcher.group(2), subSummaryLine).ifPresent(scopeBuilder::withMalloc);
				break;
			case "arena":
				tryParsingMatched(mallocArenaMatcher.group(2), subSummaryLine).ifPresent(scopeBuilder::withArena);
				break;
		}
	}

	private static void processMmap(NMTScope.NMTScopeBuilder scopeBuilder, String subSummaryLine, Matcher mmapMatcher) {
		tryParsingMatched(mmapMatcher.group(1), subSummaryLine).ifPresent(scopeBuilder::withMmapReserved);
		tryParsingMatched(mmapMatcher.group(2), subSummaryLine).ifPresent(scopeBuilder::withMmapCommitted);
	}

	private static int processSubScope(Map<String, NMTScope> scopes, String[] summaryLines, int currentLine) {
		//- GC (reserved=207337KB, committed=61417KB)
		//     (malloc=17861KB #2347)
		//     (mmap: reserved=189476KB, committed=43556KB)
		final Matcher subScopeHeaderMatcher = subScopeHeaderPattern.matcher(summaryLines[currentLine]);
		if (subScopeHeaderMatcher.matches()) {
			String scope = subScopeHeaderMatcher.group(1).trim();
			Long reserverd = Long.valueOf(subScopeHeaderMatcher.group(2));
			Long commited = Long.valueOf(subScopeHeaderMatcher.group(3));
			final NMTScope.NMTScopeBuilder scopeBuilder = NMTScope.NMTScopeBuilder.aNMTScope(scope, reserverd,
																							 commited);
			while (currentLine + 1 < summaryLines.length && !summaryLines[currentLine + 1].startsWith("-")) {
				final String subSummaryLine = summaryLines[currentLine++];
				final Matcher mmapMatcher = mmapPattern.matcher(subSummaryLine);
				if (mmapMatcher.matches()) {
					processMmap(scopeBuilder, subSummaryLine, mmapMatcher);
				}
				final Matcher mallocArenaMatcher = mallocArenaPattern.matcher(subSummaryLine);
				if (mallocArenaMatcher.matches()) {
					processMallocAndArena(scopeBuilder, subSummaryLine, mallocArenaMatcher);
				}
			}

			final NMTScope tmpScope = scopeBuilder.build();
			scopes.put(tmpScope.getScopeType(), tmpScope);
		}
		return currentLine;
	}

	private static void processTotalScope(Map<String, NMTScope> scopes, String summaryLine) {
		final Pattern compile = Pattern.compile("Total: reserved=(\\d+)[GKM]B, committed=(\\d+)[GKM]B");
		final Matcher matcher = compile.matcher(summaryLine);
		if (matcher.matches()) {
			final NMTScope total = new NMTScope("Total", Long.valueOf(matcher.group(1)),
												Long.valueOf(matcher.group(2)));
			scopes.put(total.getScopeType(), total);
		}
	}

	private static Optional<Long> tryParsingMatched(String matched, String line) {
		try {
			return Optional.of(Long.valueOf(matched));
		} catch (NumberFormatException e) {
			log.log(Level.WARNING, e, () -> "Can''t parse string: " + matched + " from line: " + line);
		}
		return Optional.empty();
	}

	public NativeMemoryTracking(Map<String, NMTScope> scopes) {
		this(scopes, SCALE.MB);
	}

	public NativeMemoryTracking(Map<String, NMTScope> scopes, SCALE scale) {
		Objects.nonNull(scopes);
		this.scopes = scopes;
		this.scale = scale;
	}

	public Map<String, NMTScope> getScopes() {
		return Collections.unmodifiableMap(scopes);
	}

	public SCALE getScale() {
		return scale;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("scopes: ").append(scopes.size());
		final NMTScope totalScope = scopes.get(NMTScope.COMMON_SCOPES.TOTAL.name);
		if (totalScope != null) {
			sb.append(", total reserved: ")
					.append(totalScope.getReserved())
					.append(", total commited: ")
					.append(totalScope.getCommitted())
					.append(", scale: ")
					.append(scale)
			;
		}
		return sb.toString();
	}
}
