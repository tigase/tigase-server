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
package tigase.server.bosh;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class BoshIOServiceTest extends TestCase {
    private BoshIOService boshIOService;

    @Before
    public void setUp() throws Exception {
        this.boshIOService = new BoshIOService(null);

        //System.setProperty("file.encoding", "UTF-8");
        /*
        // to mod the test jvm default encoding
        System.setProperty("file.encoding", "GBK");
        Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true);
        charset.set(null, null);
        */
    }

    @Test
    public void testGetCharset() {
        String c1 = "text/xml; charset=utf-8";
        assertEquals("utf-8", this.boshIOService.getCharset(c1));
        String c2 = "text/xml; charset=";
        assertEquals("", this.boshIOService.getCharset(c2));
        String c3 = "text/xml; charset=badEncoding";
        assertEquals("badEncoding", this.boshIOService.getCharset(c3));
        String c4 = "text/xml; xxx=yy";
        assertEquals(null, this.boshIOService.getCharset(c4));
        String c5 = null;
        assertEquals(null, this.boshIOService.getCharset(c5));
        String c6 = "  ";
        assertEquals(null, this.boshIOService.getCharset(c6));
        String c7 = " ; ";
        assertEquals(null, this.boshIOService.getCharset(c7));

    }

    @Test
    public void testGetDataLength() {
        String data = "Information:javac 11.0.5";
        assertEquals(this.boshIOService.getDataLength(data, "text/xml; charset=utf-8"), 24);
        assertEquals(this.boshIOService.getDataLength(data, "text/xml; charset=GBK"), 24);
        assertEquals(this.boshIOService.getDataLength(data, "text/xml; charset=badEncoding"), 24);
        assertEquals(this.boshIOService.getDataLength(data, "text/xml; charset="), 24);
        assertEquals(this.boshIOService.getDataLength(data, "text/xml; xxx=yy"), 24);

        String chineseData = data + "中文字符";

        int charLength = 24 + 4;
        assertTrue(this.boshIOService.getDataLength(chineseData, "text/xml; charset=utf-8") != charLength);
        assertTrue(this.boshIOService.getDataLength(chineseData, "text/xml; charset=utf-8") == (charLength + 8));

        assertTrue(this.boshIOService.getDataLength(chineseData, "text/xml; charset=GBK") != (charLength + 8));
        assertTrue(this.boshIOService.getDataLength(chineseData, "text/xml; charset=GBK") == (charLength + 4));

        assertTrue(this.boshIOService.getDataLength(chineseData, "text/xml; charset=ISO-8859-1") == charLength);

        int dataLength = this.boshIOService.getDataLength(chineseData, "text/xml;");

        assertTrue(Charset.defaultCharset().equals(StandardCharsets.UTF_8) ?
                dataLength == (charLength + 8) :
                (Charset.defaultCharset().equals(Charset.forName("GBK")) ?
                        dataLength == (charLength + 4) :
                        dataLength == charLength));

    }
}