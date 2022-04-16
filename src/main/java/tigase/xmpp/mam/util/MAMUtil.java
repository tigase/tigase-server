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
package tigase.xmpp.mam.util;

import tigase.xmpp.rsm.RSM;

public class MAMUtil {

	public static Range rangeFromPositions(Integer afterPos, Integer beforePos) {
		return new Range(afterPos != null ? afterPos + 1 : 0,
						 beforePos != null ? beforePos : Integer.MAX_VALUE);
	}

	public static void calculateOffsetAndPosition(RSM rsm, int count, Integer before, Integer after, Range idRange) {
		int index = rsm.getIndex() == null ? 0 : rsm.getIndex();
		int newCount = idRange.isUpperUnbound() ? count - idRange.getLowerBound() : idRange.size();

		if (after != null) {
			// it is ok, if we go out of range we will return empty result
			index = Math.max((after - idRange.getLowerBound()) + 1, 0);
		} else if (before != null) {
			index = Math.max((Math.min(before, idRange.getUpperBound()) - rsm.getMax()) - idRange.getLowerBound(), 0);
		} else if (rsm.hasBefore()) {
			index = Math.max(newCount - rsm.getMax(), 0);
		}

		int limit = Math.min(rsm.getMax(), Math.min(newCount, before != null ? before : Integer.MAX_VALUE) - index);

		rsm.setIndex(index);
		rsm.setMax(limit);
		rsm.setCount(newCount);
	}
}
