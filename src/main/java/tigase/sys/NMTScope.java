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

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public class NMTScope {

	Long arena;
	Long committed;
	Long malloc;
	Long mmapCommitted;
	Long mmapReserved;
	Long reserved;
	String scopeType;

	NMTScope(String scopeType, Long reserved, Long committed) {
		this(scopeType, reserved, committed, null, null, null, null);
	}

	NMTScope(String scopeType, Long reserved, Long committed, Long malloc, Long mmapReserved, Long mmapCommitted,
			 Long arena) {
		Objects.nonNull(scopeType);
		this.scopeType = scopeType;
		Objects.nonNull(reserved);
		this.reserved = reserved;
		Objects.nonNull(committed);
		this.committed = committed;
		this.malloc = malloc;
		this.mmapReserved = mmapReserved;
		this.mmapCommitted = mmapCommitted;
		this.arena = arena;
	}

	public String getScopeType() {
		return scopeType;
	}

	public Long getReserved() {
		return reserved;
	}

	public Long getCommitted() {
		return committed;
	}

	public Optional<Long> getMalloc() {
		return Optional.ofNullable(malloc);
	}

	public Optional<Long> getMmapReserved() {
		return Optional.ofNullable(mmapReserved);
	}

	public Optional<Long> getMmapCommitted() {
		return Optional.ofNullable(mmapCommitted);
	}

	public Optional<Long> getArena() {
		return Optional.ofNullable(arena);
	}

	@Override
	public String toString() {
		final StringJoiner joiner = new StringJoiner(", ", scopeType + " (KB): ", "").add("reserved=" + reserved)
				.add("committed=" + committed);
		getMalloc().ifPresent(val -> joiner.add("malloc=" + val));
		getMmapReserved().ifPresent(val -> joiner.add("mmapReserved=" + val));
		getMmapCommitted().ifPresent(val -> joiner.add("mmapCommitted=" + val));
		getArena().ifPresent(val -> joiner.add("arena=" + val));
		return joiner.toString();
	}

	enum COMMON_SCOPES {
		TOTAL("Total"),
		JAVA_HEAP("Java Heap"),
		CLASS("Class"),
		THREAD("Thread"),
		CODE("Code"),
		GC("GC"),
		COMPILER("Compiler"),
		INTERNAL("Internal"),
		OTHER("Other"),
		SYMBOL("Symbol"),
		NATIVE_MEMORY_TRACKING("Native Memory Tracking"),
		ARENA_CHUNK("Arena Chunk"),
		LOGGING("Logging"),
		ARGUMENTS("Arguments"),
		MODULE("Module"),
		SYNCHRONIZER("Synchronizer"),
		SAFEPOINT("Safepoint");

		String name;

		COMMON_SCOPES(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	static final class NMTScopeBuilder {

		Long arena;
		Long committed;
		Long malloc;
		Long mmapCommitted;
		Long mmapReserved;
		Long reserved;
		String scopeType;

		static NMTScopeBuilder aNMTScope(String scopeType, Long reserved, Long committed) {
			return new NMTScopeBuilder(scopeType, reserved, committed);
		}

		private NMTScopeBuilder(String scopeType, Long reserved, Long committed) {
			Objects.nonNull(scopeType);
			this.scopeType = scopeType;
			Objects.nonNull(reserved);
			this.reserved = reserved;
			Objects.nonNull(committed);
			this.committed = committed;

		}

		NMTScopeBuilder withMalloc(Long malloc) {
			this.malloc = malloc;
			return this;
		}

		NMTScopeBuilder withMmapReserved(Long mmapReserved) {
			this.mmapReserved = mmapReserved;
			return this;
		}

		NMTScopeBuilder withMmapCommitted(Long mmapCommitted) {
			this.mmapCommitted = mmapCommitted;
			return this;
		}

		NMTScopeBuilder withArena(Long arena) {
			this.arena = arena;
			return this;
		}

		NMTScope build() {
			return new NMTScope(scopeType, reserved, committed, malloc, mmapReserved, mmapCommitted, arena);
		}
	}
}
