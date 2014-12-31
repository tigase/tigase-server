/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.annotations;

import java.lang.annotation.ElementType; // NOPMD
import java.lang.annotation.Target; // NOPMD
import java.lang.annotation.Documented; // NOPMD
import java.lang.annotation.Retention; // NOPMD
import java.lang.annotation.RetentionPolicy; // NOPMD

/**
 * <code>TODO</code> this is information for developers that there is still
 * something to do with annotated code. Additional parameters can provide
 * detailed information what exatcly is suposed to correct in code, how
 * important it is for project the time when it should be done and name of
 * developer to which correction is assigned.
 *
 * <code>TODO</code> annotation has a few properties which can be set to better
 * describe code to be changed like <code>note</code> - allows you to add some
 * description, <code>severity</code> - allows you to set severity level for
 * this code change, <code>timeLine</code> - allows you to set expected time
 * when code change should be ready to use and <code>assignedTo</code> - allows
 * you to set name of developer who should make the change to code. All this
 * properties has some default values so it is not necessary to set them all
 * every time you use <code>TODO</code> annotation.<br>
 * Below you can find a few samples how to use <code>TODO</code> annotation:
 * <p>Sample of use all annotation with all possible properties:</p>
 * <pre>  @TODO(
 *  severity=TODO.Severity.CRITICAL,
 *  note="This empty method which should calculate data checksum, needs implementation.",
 *  timeLine="30/11/2004",
 *  assignedTo="Artur Hefczyc"
 * )
 * public long checksum(char[] buff) { return -1; }</pre>
 * <p>A few samples using selected set of <code>TODO</code> properties:</p>
 * <pre>  @TODO(
 *  severity=TODO.Severity.DOCUMENTATION,
 *  note="This method needs better inline documentation, I can't udnerstan how it works",
 *  assignedTo="Artur Hefczyc"
 * )
 * public String calculateWeather(byte[][][] buff) { ... }</pre>
 * <pre>  @TODO(note="SSL socket functionality not implemented yet.")
 * protected void init() { ... }</pre>
 * <p>
 * Created: Wed Sep 29 18:58:21 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Retention(RetentionPolicy.SOURCE)
@Documented
@Target({ElementType.TYPE,
      ElementType.METHOD,
      ElementType.CONSTRUCTOR,
      ElementType.ANNOTATION_TYPE})
public @interface TODO {

  /**
   * This enumeration defines importance levels for code change which is
   * expected to be made for annotated element.
   */
  public enum Severity {
    /**
     * If change severity is set to <code>CRITICAL</code> it means that wihtout
     * this change some progress is not possible. Probably it blocks some
     * important functionality like <em>SSL</em> activation for server port.
     */
    CRITICAL,
    /**
     * <code>IMPORTANT</code> severity means that this code does not block
     * implementation of any functionality but might be inefficient, insecure
     * or contain some temporary solution.<br>
     * <code>IMPORTANT</code> severity can be also assigned to code which needs
     * some medium or major refactoring.
     */
    IMPORTANT,
    /**
     * <code>TRIVIAL</code> severity means that this code works correctly and
     * is implemented according to design but there is still some minor
     * improvement that can be done or just cleaning the code.<br>
     * <code>TRIVIAL</code> severity can be assigned also to code which needs
     * some minor refactoring.
     */
    TRIVIAL,
    /**
     * <code>DOCUMENTATION</code> severity refers to code which should be
     * documented. It does not refer to API documentation. It refers to in-line
     * documentation which should be added due to complicity of some code or
     * unusual algorithm used.<br>
     * Usually I try to avoid "smart" code but in certain cases it is required
     * to use code which might be difficult to understand. In all such cases
     * code should be detaily documented. This annotation can help to remind
     * what parts of code needs more documentation.<br>
     * This annotation should be also added by other developer who is not owner
     * of some part of code but tried to read it and found it difficult to
     * understand. In such case it is recommended that this developers should
     * leave such annotation to bring attention to owner that some code needs
     * better documentation.
     */
    DOCUMENTATION;
  };

  /**
   * <code>severity</code> property allows you to set and retrieve severity of
   * expected code change described by this <code>TODO</code> annotation.
	 *
	 * @return priority of code change
   */
  Severity severity() default Severity.IMPORTANT;
  /**
   * <code>note</code> property allows you to set and retrieve description
   * text for expected code change.
	 *
	 * @return description text for code change
   */
  String note() default "Functionality not fully implemented.";
  /**
   * <code>timeLine</code> property allows you to set and retrieve expected
   * time by when the change should be done to this code.
	 *
	 * @return due date of the change
   */
  String timeLine() default "2004/12/31";
  /**
   * <code>assignedTo</code> property allows you to assign developer for the code
   * change described by this annotation.
	 *
	 * @return Name of the developer
   */
  String assignedTo() default "Artur Hefczyc";

}// TODO
