/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
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
package tigase.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Describe class Algorithms here.
 *
 *
 * Created: Wed May  4 13:24:03 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Algorithms {

  /**
   * Creates a new <code>Algorithms</code> instance.
   *
   */
  private Algorithms() { }

  /**
   * This method encodes data using digest algorithm described in
   * <em>JEP-0078</em> documentation.
   * As a result you have <code>String</code> containing digest data which
   * can be compared with data sent by the user to authenticate him.
   *
   * @param id a <code>String</code> value of some ID value like session ID to
   * concatenate with secret word.
   * @param secret a <code>String</code> value of a secret word shared between
   * entites.
   * @param alg a <code>String</code> value of algorithm name to use for
   * generating diffest message.
   * @return a <code>String</code> value digest message as defined.
   * @exception NoSuchAlgorithmException if an error occurs during encoding
   * digest message.
   */
  public static final String digest(final String id, final String secret,
    final String alg) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance(alg);
    String conc = id + secret;
    md.update(conc.getBytes());
    byte[] buff = md.digest();
    StringBuilder enc = new StringBuilder();
    for (byte b : buff) {
      char ch = Character.forDigit((b >> 4) & 0xF, 16);
      enc.append(ch);
      ch = Character.forDigit(b & 0xF, 16);
      enc.append(ch);
    } // end of for (b : digest)
    return enc.toString();
  }

} // Algorithms
