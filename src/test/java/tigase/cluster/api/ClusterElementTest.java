/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.cluster.api;

import junit.framework.TestCase;
import org.junit.Test;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;

/**
 *
 * @author andrzej
 */
public class ClusterElementTest extends TestCase {

        @Test
        public void testGetMethodName() {
             SimpleParser parser = new SimpleParser();
             DomBuilderHandler handler = new DomBuilderHandler();
             char[] data = "<cluster to=\"sess-man@blue\" type=\"set\" id=\"cl-6627\" xmlns=\"tigase:cluster\" from=\"sess-man@green\"><control><visited-nodes><node-id>sess-man@green</node-id></visited-nodes><method-call name=\"packet-forward-sm-cmd\"/><first-node>sess-man@green</first-node></control><data><presence to=\"test2@test\" xmlns=\"jabber:client\" from=\"test1@test/test\"><status/><priority>5</priority></presence></data></cluster>".toCharArray();
             parser.parse(handler, data, 0, data.length);
             Element elem = handler.getParsedElements().poll();
             assertEquals("packet-forward-sm-cmd", elem.findChild("/cluster/control/method-call").getAttribute("name"));
             assertEquals("/cluster/control/method-call", ClusterElement.CLUSTER_METHOD_PATH);
             ClusterElement clElem = new ClusterElement(elem);
             assertEquals("packet-forward-sm-cmd", clElem.getMethodName());
        }
        
}
