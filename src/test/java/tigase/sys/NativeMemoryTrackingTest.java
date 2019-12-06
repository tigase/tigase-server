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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class NativeMemoryTrackingTest {

	private static final String summary =
			"\n" + "Native Memory Tracking:\n" + "\n" + "Total: reserved=5733424KB, committed=366756KB\n" +
					"-                 Java Heap (reserved=4194304KB, committed=262144KB)\n" +
					"                            (mmap: reserved=4194304KB, committed=262144KB) \n" + " \n" +
					"-                     Class (reserved=1059085KB, committed=10637KB)\n" +
					"                            (classes #1609)\n" +
					"                            (  instance classes #1450, array classes #159)\n" +
					"                            (malloc=269KB #2481) \n" +
					"                            (mmap: reserved=1058816KB, committed=10368KB) \n" +
					"                            (  Metadata:   )\n" +
					"                            (    reserved=10240KB, committed=9216KB)\n" +
					"                            (    used=9076KB)\n" +
					"                            (    free=140KB)\n" +
					"                            (    waste=0KB =0.00%)\n" +
					"                            (  Class space:)\n" +
					"                            (    reserved=1048576KB, committed=1152KB)\n" +
					"                            (    used=993KB)\n" +
					"                            (    free=159KB)\n" +
					"                            (    waste=0KB =0.00%)\n" + " \n" +
					"-                    Thread (reserved=17486KB, committed=17486KB)\n" +
					"                            (thread #17)\n" +
					"                            (stack: reserved=17408KB, committed=17408KB)\n" +
					"                            (malloc=57KB #104) \n" +
					"                            (arena=21KB #33)\n" + " \n" +
					"-                      Code (reserved=247804KB, committed=7664KB)\n" +
					"                            (malloc=116KB #1038) \n" +
					"                            (mmap: reserved=247688KB, committed=7548KB) \n" + " \n" +
					"-                        GC (reserved=207337KB, committed=61417KB)\n" +
					"                            (malloc=17861KB #2347) \n" +
					"                            (mmap: reserved=189476KB, committed=43556KB) \n" + " \n" +
					"-                  Compiler (reserved=1095KB, committed=1095KB)\n" +
					"                            (malloc=18KB #89) \n" +
					"                            (arena=1076KB #14)\n" + " \n" +
					"-                  Internal (reserved=562KB, committed=562KB)\n" +
					"                            (malloc=530KB #982) \n" +
					"                            (mmap: reserved=32KB, committed=32KB) \n" + " \n" +
					"-                     Other (reserved=2KB, committed=2KB)\n" +
					"                            (malloc=2KB #1) \n" + " \n" +
					"-                    Symbol (reserved=3059KB, committed=3059KB)\n" +
					"                            (malloc=1644KB #4986) \n" +
					"                            (arena=1415KB #1)\n" + " \n" +
					"-    Native Memory Tracking (reserved=227KB, committed=227KB)\n" +
					"                            (malloc=4KB #54) \n" +
					"                            (tracking overhead=223KB)\n" + " \n" +
					"-               Arena Chunk (reserved=2337KB, committed=2337KB)\n" +
					"                            (malloc=2337KB) \n" + " \n" +
					"-                   Logging (reserved=4KB, committed=4KB)\n" +
					"                            (malloc=4KB #182) \n" + " \n" +
					"-                 Arguments (reserved=19KB, committed=19KB)\n" +
					"                            (malloc=19KB #484) \n" + " \n" +
					"-                    Module (reserved=60KB, committed=60KB)\n" +
					"                            (malloc=60KB #1035) \n" + " \n" +
					"-              Synchronizer (reserved=35KB, committed=35KB)\n" +
					"                            (malloc=35KB #291) \n" + " \n" +
					"-                 Safepoint (reserved=8KB, committed=8KB)\n" +
					"                            (mmap: reserved=8KB, committed=8KB) \n" + " \n" + "\n" + "\n" +
					"Process finished with exit code 0\n";
	private static NativeMemoryTracking nmt;
	private Set<String> summaryScopes = new TreeSet<>(
			Arrays.asList("Total", "Java Heap", "Class", "Thread", "Code", "GC", "Compiler", "Internal", "Other",
						  "Symbol", "Native Memory Tracking", "Arena Chunk", "Logging", "Arguments", "Module",
						  "Synchronizer", "Safepoint"));

	@BeforeClass
	public static void setUp() {
//		final Optional<NativeMemoryTracking> nativeMemoryTracking = NativeMemoryTracking.getNativeMemoryTracking();
		final Optional<NativeMemoryTracking> nativeMemoryTracking = NativeMemoryTracking.parse(summary);
//		System.out.println(nativeMemoryTracking);
		nmt = nativeMemoryTracking.orElse(null);
	}

	@Test
	public void getTypes() {
		Assert.assertNotNull(nmt);
		Map<String, NMTScope> scopes = nmt.getScopes();
//		scopes.forEach((s, nmtScope) -> System.out.println(nmtScope));
		final Set<String> scopeTypes = scopes.values().stream().map(NMTScope::getScopeType).collect(Collectors.toSet());
		Assert.assertEquals(17, scopeTypes.size());
		Assert.assertEquals(summaryScopes, scopeTypes);
	}

	@Test
	public void getTotalValues() {
		Assert.assertNotNull(nmt);
		Map<String, NMTScope> scopes = nmt.getScopes();
		final NMTScope totalScope = scopes.get(NMTScope.COMMON_SCOPES.TOTAL.getName());
		//"Total: reserved=5733424KB, committed=366756KB"
		Assert.assertEquals(5733424, totalScope.getReserved().longValue());
		Assert.assertEquals(366756, totalScope.getCommitted().longValue());
		Assert.assertFalse(totalScope.getArena().isPresent());
		Assert.assertFalse(totalScope.getMalloc().isPresent());
		Assert.assertFalse(totalScope.getMmapCommitted().isPresent());
		Assert.assertFalse(totalScope.getMmapReserved().isPresent());
	}

	@Test
	public void getExtendedValuesGC() {
		Assert.assertNotNull(nmt);
		Map<String, NMTScope> scopes = nmt.getScopes();
		final NMTScope gcScope = scopes.get(NMTScope.COMMON_SCOPES.GC.getName());
		// GC (reserved=207337KB, committed=61417KB)
		//    (malloc=17861KB #2347)
		//    (mmap: reserved=189476KB, committed=43556KB)
//		System.out.println(gcScope);
		Assert.assertEquals(207337, gcScope.getReserved().longValue());
		Assert.assertEquals(61417, gcScope.getCommitted().longValue());
		Assert.assertEquals(17861, gcScope.getMalloc().get().longValue());
		Assert.assertEquals(43556, gcScope.getMmapCommitted().get().longValue());
		Assert.assertEquals(189476, gcScope.getMmapReserved().get().longValue());
		Assert.assertFalse(gcScope.getArena().isPresent());
	}
}