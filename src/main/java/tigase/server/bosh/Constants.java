/*  Tigase Project
 *  Copyright (C) 2001-2007
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.bosh;

/**
 * Describe class Constants here.
 *
 *
 * Created: Tue Jun  5 22:22:09 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class Constants {

	public static final String BOSH_VERSION = "1.6";

	protected static final String MAX_WAIT_DEF_PROP_KEY = "max-wait";
	protected static final long MAX_WAIT_DEF_PROP_VAL = 60;
	protected static final String MIN_POOLING_PROP_KEY = "min-pooling";
	protected static final long MIN_POOLING_PROP_VAL = 10;
	protected static final String MAX_INACTIVITY_PROP_KEY = "max-inactivity";
	protected static final long MAX_INACTIVITY_PROP_VAL = 60;
	protected static final String CONCURRENT_REQUESTS_PROP_KEY =
		"concurrent-requests";
	protected static final int CONCURRENT_REQUESTS_PROP_VAL = 2;
	protected static final String HOLD_REQUESTS_PROP_KEY = "hold-requests";
	protected static final int HOLD_REQUESTS_PROP_VAL = 1;
	protected static final String MAX_PAUSE_PROP_KEY = "max-inactivity";
	protected static final long MAX_PAUSE_PROP_VAL = 120;

}
