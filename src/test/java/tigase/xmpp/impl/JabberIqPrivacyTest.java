package tigase.xmpp.impl;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.junit.Test;
import tigase.xml.Element;
import tigase.xmpp.Authorization;

/**
 * Test class for JabberIqPrivacyTest
 * 
 * Currently class tests validateList method checking validation of type, 
 * subscription and action. Other cases are not tested due to missing instance 
 * of XMPPResourceConnection
 */
public class JabberIqPrivacyTest extends TestCase {

        @Test
        public void testValidateListGood() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
                         
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "subscription", "both", "allow", "10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "15" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(null, result);
        }

        @Test
        public void testValidateListBadAction() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
             
             
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "subscription", "both", "ignore", "10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "15" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(Authorization.BAD_REQUEST, result);
        }
        
        @Test
        public void testValidateListBadSubscription() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
                          
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "subscription", "or", "allow", "10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "15" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(Authorization.BAD_REQUEST, result);
        }
        
        @Test
        public void testValidateListBadType() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
                          
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "other", "both", "allow", "10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "15" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(Authorization.BAD_REQUEST, result);
        }
        
        @Test
        public void testValidateListOrderUnsignedInt() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
                          
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "subscription", "both", "allow", "-10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "15" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(Authorization.BAD_REQUEST, result);
             
        }

        @Test
        public void testValidateListOrderAttributeDuplicate() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
                          
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "subscription", "both", "allow", "10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "10" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(Authorization.BAD_REQUEST, result);
             
        }
        
}
