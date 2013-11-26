/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tigase.xmpp.impl.roster;

import tigase.xmpp.XMPPException;

/**
 *
 * @author kobit
 */
public class RepositoryAccessException extends XMPPException {

 private static final long serialVersionUID = 1L;

  /**
   * Creates a new <code>PacketErrorTypeException</code> instance.
   *
	 * @param message
   */
  public RepositoryAccessException(String message) { super(message); }

  public RepositoryAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}
