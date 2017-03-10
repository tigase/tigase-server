package tigase.xmpp.impl;

import static org.junit.Assert.*;
import org.junit.Test;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author andrzej
 */
public class VCardHelperTest {
	private static String VCARD_TEMP_DATA = ("<vCard xmlns=\"vcard-temp\">\n" +
"  <FN>Peter Saint-Andre</FN>\n" +
"  <N>\n" +
"    <FAMILY>Saint-Andre</FAMILY>\n" +
"    <GIVEN>Peter</GIVEN>\n" +
"    <MIDDLE/>\n" +
"  </N>\n" +
"  <NICKNAME>stpeter</NICKNAME>\n" +
"  <NICKNAME>psa</NICKNAME>\n" +
"  <PHOTO><EXTVAL>http://stpeter.im/images/stpeter_oscon.jpg</EXTVAL></PHOTO>\n" +
"  <PHOTO><EXTVAL>http://stpeter.im/images/stpeter_hell.jpg</EXTVAL></PHOTO>\n" +
"  <BDAY>1966-08-06</BDAY>\n" +
"  <ADR>\n" +
"    <WORK/>\n" +
"    <PREF/>\n" +
"    <EXTADD>Suite 600</EXTADD>\n" +
"    <STREET>1899 Wynkoop Street</STREET>\n" +
"    <LOCALITY>Denver</LOCALITY>\n" +
"    <REGION>CO</REGION>\n" +
"    <PCODE>80202</PCODE>\n" +
"    <CTRY>USA</CTRY>\n" +
"  </ADR>\n" +
"  <ADR>\n" +
"    <HOME/>\n" +
"    <EXTADD/>\n" +
"    <STREET/>\n" +
"    <LOCALITY>Parker</LOCALITY>\n" +
"    <REGION>CO</REGION>\n" +
"    <PCODE>80138</PCODE>\n" +
"    <CTRY>USA</CTRY>\n" +
"  </ADR>\n" +
"  <TEL><WORK/><VOICE/><NUMBER>+1-303-308-3282</NUMBER><PREF/></TEL>\n" +
"  <TEL><WORK/><FAX/><NUMBER>+1-303-308-3219</NUMBER></TEL>\n" +
"  <TEL><CELL/><VOICE/><TEXT/><NUMBER>+1-720-256-6756</NUMBER></TEL>\n" +
"  <TEL><HOME/><VOICE/><NUMBER>+1-303-555-1212</NUMBER></TEL>\n" +
"  <EMAIL><INTERNET/><USERID>stpeter@jabber.org</USERID></EMAIL>\n" +
"  <EMAIL><WORK/><USERID>psaintan@cisco.com</USERID></EMAIL>\n" +
"  <JABBERID>stpeter@jabber.org</JABBERID>\n" +
"  <TZ>America/Denver</TZ>\n" +
"  <GEO><LAT>39.59</LAT><LON>-105.01</LON></GEO>\n" +
"  <TITLE>Executive Director</TITLE>\n" +
"  <ROLE>Patron Saint</ROLE>\n" +
"  <LOGO>\n" +
"    <TYPE>image/jpeg</TYPE>\n" +
"    <BINVAL>\n" +
"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\n" +
"0vx/+oqw7D4D8qysrzcv7jro/9k=\n" +
"    </BINVAL>\n" +
"  </LOGO>\n" +
"  <ORG>\n" +
"    <ORGNAME>XMPP Standards Foundation</ORGNAME>\n" +
"    <ORGUNIT/>\n" +
"  </ORG>\n" +
"  <URL>https://stpeter.im/</URL>\n" +
"  <URL>http://www.saint-andre.com/</URL>\n" +
"  <KEY>\n" +
"    <CRED>\n" +
"-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
"Version: GnuPG/MacGPG2 v2.0.18 (Darwin)\n" +
"\n" +
"mQINBFETDzsBEAC0FOv1N3ZJzIIxN6cKD475KVS9CHDPeYpegcOIPnL5eY1DCHeh\n" +
"/IwS1S7RCePtmiybNoV9FsI4PKUknzXQxA6LVEdAR/LUlhgJKjq+gsgp8lqbEILh\n" +
"g13ecH66HwLS9rarbQkC47T7kL8miIPBFC6E3A4Lq1L+eueO6UcLhKgoYkMxOjdi\n" +
"WrMgKTnVpch5ydLkPm/z0Zo8zRgqlPuTLeCrXXZYnjHXLVFN2xy04UzOs7P5u5KV\n" +
"fx5Z7uQisr8pXtyLd6SpTZo6SHgKBv15uz0rqXhsJojiGtOXfWznAjaS5FUOORq9\n" +
"CklG5cMOUAT8TNftv0ktsxaWDL1ELDVQPy1m7mtzo+VREG+0xmU6AjMo/GHblW1U\n" +
"U7MI9yCiuMLsp/HLrFuiosqLVZ85wuLQ2junPe3tK8h15UcxIXAcpQ1VqIaDQFbe\n" +
"uLOXJTF8YHpHdpHYt/ZM1ll7ZBKGAo8yd7uF7wJ9D3gUazwdz9fFjWV7oIk7ATwO\n" +
"lFllzmWDn+M2ygbHOGUGMX5hSaa8eDSieiR2QoLdn27Fip7kMBTJ2+GISrfnJTN/\n" +
"OQvmj0DXXAdxHmu2C4QgmZbkge35n129yzXn9NcqzrGLroV62lL3LgX6cSbiH5i7\n" +
"GgWY6CAPb1pMogV0K475n9FvOSDRiG4QSO5yqKiA3OP5aKrIRp2TNAk4IwARAQAB\n" +
"tCZQZXRlciBTYWludC1BbmRyZSA8c3RwZXRlckBzdHBldGVyLmltPokCOQQTAQIA\n" +
"IwUCURMPOwIbAwcLCQgHAwIBBhUIAgkKCwQWAgMBAh4BAheAAAoJEOoGpJErxa2p\n" +
"6bgQAKpxu07cMDOLc4+EG8H19NWXIVVybOEvfGuHYZaLKkPrhrMZwJiOwBpyISNR\n" +
"t9qzX1eLCVaojaoEVX6kD8MGc5zKFfiJZy3j7lBWl+Ybr7FfXYy2BbAXKx49e1n6\n" +
"ci9LmBrmVfAEaxtDNPITZ9N9oUAb9vS0nrG036EwteEHAveQvlDjO7lhz6+Cv7lZ\n" +
"QgBj9rZ6khfcQ4S3nSCQaKLQ9Iav4fqxI7SfuPKnx6quHX3JNLGnVo3wl+j/foCK\n" +
"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\n" +
"rLuMVIV/\n" +
"=llGw\n" +
"-----END PGP PUBLIC KEY BLOCK-----\n" +
"    </CRED>\n" +
"  </KEY>\n" +
"  <DESC>\n" +
"    More information about me is located on my \n" +
"    personal website: https://stpeter.im/\n" +
"  </DESC>\n" +
"</vCard>").replace("\n", "").replace("  ","");
	
	private static String VCARD4_DATA = ("<vcard xmlns=\"urn:ietf:params:xml:ns:vcard-4.0\">\n" +
"  <fn>\n" +
"    <text>Peter Saint-Andre</text>\n" +
"  </fn>\n" +
"  <n>\n" +
"    <surname>Saint-Andre</surname>\n" +
"    <given>Peter</given>\n" +
"    <additional></additional>\n" +
"  </n>\n" +
"  <nickname>\n" +
"    <text>stpeter</text>\n" +
"  </nickname>\n" +
"  <nickname>\n" +
"    <text>psa</text>\n" +
"  </nickname>\n" +
"  <photo>\n" +
"    <uri>http://stpeter.im/images/stpeter_oscon.jpg</uri>\n" +
"  </photo>\n" +
"  <photo>\n" +
"    <uri>http://stpeter.im/images/stpeter_hell.jpg</uri>\n" +
"  </photo>\n" +
"  <bday>\n" +
"    <date>1966-08-06</date>\n" +
"  </bday>\n" +
"  <adr>\n" +
"    <parameters>\n" +
"      <type><text>work</text></type>\n" +
"      <pref><integer>1</integer></pref>\n" +
"    </parameters>\n" +
"    <ext>Suite 600</ext>\n" +
"    <street>1899 Wynkoop Street</street>\n" +
"    <locality>Denver</locality>\n" +
"    <region>CO</region>\n" +
"    <code>80202</code>\n" +
"    <country>USA</country>\n" +
"  </adr>\n" +
"  <adr>\n" +
"    <parameters><type><text>home</text></type></parameters>\n" +
"    <ext></ext>\n" +
"    <street></street>\n" +
"    <locality>Parker</locality>\n" +
"    <region>CO</region>\n" +
"    <code>80138</code>\n" +
"    <country>USA</country>\n" +
"  </adr>\n" +
"  <tel>\n" +
"    <parameters>\n" +
"      <type><text>work</text><text>voice</text></type>\n" +
"      <pref><integer>1</integer></pref>\n" +
"    </parameters>\n" +
"    <uri>tel:+1-303-308-3282</uri>\n" +
"  </tel>\n" +
"  <tel>\n" +
"    <parameters>\n" +
"      <type><text>work</text><text>fax</text></type>\n" +
"    </parameters>\n" +
"    <uri>tel:+1-303-308-3219</uri>\n" +
"  </tel>\n" +
"  <tel>\n" +
"    <parameters>\n" +
"      <type><text>cell</text><text>voice</text><text>text</text></type>\n" +
"    </parameters>\n" +
"    <uri>tel:+1-720-256-6756</uri>\n" +
"  </tel>\n" +
"  <tel>\n" +
"    <parameters>\n" +
"      <type><text>home</text><text>voice</text></type>\n" +
"    </parameters>\n" +
"    <uri>tel:+1-303-555-1212</uri>\n" +
"  </tel>\n" +
"  <email>\n" +
"    <text>stpeter@jabber.org</text>\n" +
"  </email>\n" +
"  <email>\n" +
"    <parameters>\n" +
"      <type><text>work</text></type>\n" +
"    </parameters>\n" +
"    <text>psaintan@cisco.com</text>\n" +
"  </email>\n" +
"  <impp>\n" +
"    <uri>xmpp:stpeter@jabber.org</uri>\n" +
"  </impp>\n" +
"  <tz>\n" +
"    <text>America/Denver</text>\n" +
"  </tz>\n" +
"  <geo>\n" +
"    <uri>geo:39.59,-105.01</uri>\n" +
"  </geo>\n" +
"  <title>\n" +
"    <text>Executive Director</text>\n" +
"  </title>\n" +
"  <role>\n" +
"    <text>Patron Saint</text>\n" +
"  </role>\n" +
"  <logo>\n" +
"    <uri>data:image/jpeg;base64,\n" +
"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\n" +
"0vx/+oqw7D4D8qysrzcv7jro/9k=\n" +
"      </uri>\n" +
"    </logo>\n" +
"    <org>\n" +
"      <text>XMPP Standards Foundation</text>\n" +
"    </org>\n" +
"    <url>\n" +
"      <uri>https://stpeter.im/</uri>\n" +
"    </url>\n" +
"    <url>\n" +
"      <uri>http://www.saint-andre.com/</uri>\n" +
"    </url>\n" +
"    <key>\n" +
"      <text>\n" +
"-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
"Version: GnuPG/MacGPG2 v2.0.18 (Darwin)\n" +
"\n" +
"mQINBFETDzsBEAC0FOv1N3ZJzIIxN6cKD475KVS9CHDPeYpegcOIPnL5eY1DCHeh\n" +
"/IwS1S7RCePtmiybNoV9FsI4PKUknzXQxA6LVEdAR/LUlhgJKjq+gsgp8lqbEILh\n" +
"g13ecH66HwLS9rarbQkC47T7kL8miIPBFC6E3A4Lq1L+eueO6UcLhKgoYkMxOjdi\n" +
"WrMgKTnVpch5ydLkPm/z0Zo8zRgqlPuTLeCrXXZYnjHXLVFN2xy04UzOs7P5u5KV\n" +
"fx5Z7uQisr8pXtyLd6SpTZo6SHgKBv15uz0rqXhsJojiGtOXfWznAjaS5FUOORq9\n" +
"CklG5cMOUAT8TNftv0ktsxaWDL1ELDVQPy1m7mtzo+VREG+0xmU6AjMo/GHblW1U\n" +
"U7MI9yCiuMLsp/HLrFuiosqLVZ85wuLQ2junPe3tK8h15UcxIXAcpQ1VqIaDQFbe\n" +
"uLOXJTF8YHpHdpHYt/ZM1ll7ZBKGAo8yd7uF7wJ9D3gUazwdz9fFjWV7oIk7ATwO\n" +
"lFllzmWDn+M2ygbHOGUGMX5hSaa8eDSieiR2QoLdn27Fip7kMBTJ2+GISrfnJTN/\n" +
"OQvmj0DXXAdxHmu2C4QgmZbkge35n129yzXn9NcqzrGLroV62lL3LgX6cSbiH5i7\n" +
"GgWY6CAPb1pMogV0K475n9FvOSDRiG4QSO5yqKiA3OP5aKrIRp2TNAk4IwARAQAB\n" +
"tCZQZXRlciBTYWludC1BbmRyZSA8c3RwZXRlckBzdHBldGVyLmltPokCOQQTAQIA\n" +
"IwUCURMPOwIbAwcLCQgHAwIBBhUIAgkKCwQWAgMBAh4BAheAAAoJEOoGpJErxa2p\n" +
"6bgQAKpxu07cMDOLc4+EG8H19NWXIVVybOEvfGuHYZaLKkPrhrMZwJiOwBpyISNR\n" +
"t9qzX1eLCVaojaoEVX6kD8MGc5zKFfiJZy3j7lBWl+Ybr7FfXYy2BbAXKx49e1n6\n" +
"ci9LmBrmVfAEaxtDNPITZ9N9oUAb9vS0nrG036EwteEHAveQvlDjO7lhz6+Cv7lZ\n" +
"QgBj9rZ6khfcQ4S3nSCQaKLQ9Iav4fqxI7SfuPKnx6quHX3JNLGnVo3wl+j/foCK\n" +
"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\n" +
"rLuMVIV/\n" +
"=llGw\n" +
"-----END PGP PUBLIC KEY BLOCK-----\n" +
"      </text>\n" +
"    </key>\n" +
"    <note>\n" +
"      <text>\n" +
"More information about me is located on my \n" +
"personal website: https://stpeter.im/\n" +
"    </text>\n" +
"  </note>\n" +
"</vcard>").replace("\n", "").replace("  ","");

	private static String VCARD2_TEMP_DATA = ("<vCard xmlns=\"vcard-temp\">\n" +
"  <FN>Peter Saint-Andre</FN>\n" +
"  <N>\n" +
"    <FAMILY>Saint-Andre</FAMILY>\n" +
"    <GIVEN>Peter</GIVEN>\n" +
"    <MIDDLE/>\n" +
"  </N>\n" +
"  <NICKNAME>stpeter</NICKNAME>\n" +
"  <NICKNAME>psa</NICKNAME>\n" +
"  <PHOTO><EXTVAL>http://stpeter.im/images/stpeter_oscon.jpg</EXTVAL></PHOTO>\n" +
"  <PHOTO><EXTVAL>http://stpeter.im/images/stpeter_hell.jpg</EXTVAL></PHOTO>\n" +
"  <BDAY>1966-08-06</BDAY>\n" +
"  <ADR>\n" +
"    <WORK/>\n" +
"    <PREF/>\n" +
"    <EXTADD>Suite 600</EXTADD>\n" +
"    <STREET>1899 Wynkoop Street</STREET>\n" +
"    <LOCALITY>Denver</LOCALITY>\n" +
"    <REGION>CO</REGION>\n" +
"    <PCODE>80202</PCODE>\n" +
"    <CTRY>USA</CTRY>\n" +
"  </ADR>\n" +
"  <ADR>\n" +
"    <HOME/>\n" +
"    <EXTADD/>\n" +
"    <STREET/>\n" +
"    <LOCALITY>Parker</LOCALITY>\n" +
"    <REGION>CO</REGION>\n" +
"    <PCODE>80138</PCODE>\n" +
"    <CTRY>USA</CTRY>\n" +
"  </ADR>\n" +
"  <TEL><WORK/><VOICE/><PREF/><NUMBER>+1-303-308-3282</NUMBER></TEL>\n" +
"  <TEL><WORK/><FAX/><NUMBER>+1-303-308-3219</NUMBER></TEL>\n" +
"  <TEL><CELL/><VOICE/><TEXT/><NUMBER>+1-720-256-6756</NUMBER></TEL>\n" +
"  <TEL><HOME/><VOICE/><NUMBER>+1-303-555-1212</NUMBER></TEL>\n" +
"  <EMAIL><USERID>stpeter@jabber.org</USERID></EMAIL>\n" +
"  <EMAIL><WORK/><USERID>psaintan@cisco.com</USERID></EMAIL>\n" +
"  <JABBERID>stpeter@jabber.org</JABBERID>\n" +
"  <TZ>America/Denver</TZ>\n" +
"  <GEO><LAT>39.59</LAT><LON>-105.01</LON></GEO>\n" +
"  <TITLE>Executive Director</TITLE>\n" +
"  <ROLE>Patron Saint</ROLE>\n" +
"  <LOGO>\n" +
"    <TYPE>image/jpeg</TYPE>\n" +
"    <BINVAL>\n" +
"/9j/4AAQSkZJRgABAQEASABIAAD//gAXQ3JlYXRlZCB3aXRoIFRoZSBHSU1Q/9sAQwAIBgYHBgUI\n" +
"BwcHCQkICgwUDQwLCwwZEhMPFB0aHx4dGhwcICQuJyAiLCMcHCg3KSwwMTQ0NB8nOT04MjwuMzQy\n" +
"/9sAQwEJCQkMCwwYDQ0YMiEcITIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIy\n" +
"MjIyMjIyMjIyMjIy/8AAEQgAgQB7AwEiAAIRAQMRAf/EABsAAAMAAwEBAAAAAAAAAAAAAAQFBgAD\n" +
"BwIB/8QAPRAAAgEDAwIDBQQJBAEFAAAAAQIDAAQRBRIhMUEGE1EiYXGBkRQyobEHFSMzNEJScsEk\n" +
"YtHw0jVDc8Lh/8QAGQEAAwEBAQAAAAAAAAAAAAAAAQIEAwAF/8QAJBEAAgICAgICAgMAAAAAAAAA\n" +
"AAECEQMhEjEEQSJREzJCYXH/2gAMAwEAAhEDEQA/AOf2c0KGSY2pDFiinYMsCMfhj8aya6gdvZj9\n" +
"ryg6A8eYuORxQk1rdzSHyZGQYO0GXAPv/MV6luDp8QgntkRyhAfHJ3Y5wOoxxXncbdiA9zp/6xQG\n" +
"KaGFIhK208MwX09amCOeMEdjT3VLi1EaeVGHEi4WRCQVwAD7PY5/CkQYbcfdPUVbjTS2FGAHGele\n" +
"1nmXgOw6/iMV4LnHup/4c8L3/iWfbboIoFPt3D/dH/kfcKd0kOotukL/ALWs8SJNEPZGA4/z2omD\n" +
"T7++dY7azuLlAOPs8Jfb9P8Amu1+Hv0Z+HtPjVp7YX846yXAyM+5elX1tDa2sKxwxRxoBgKigD6U\n" +
"nJGn4a7OB+G/0Y67rlxuuIRptojDdcXS5c/2R9SfoPfXY/DvgzQvCiLJZW/m3uPavbgb5Sf9vZPl\n" +
"9TVCBAw5RfiODQ93Fdi3b9XyxiXt5iE59wNPGSEnja6N5VyCzHYh5LOcUvuNc0y0JDTtO/8ATGP8\n" +
"1Hahc6g87xX0ku9TyhbH4dKEA6AZx1prJ2n7KW58W3L5W1iS3H9XVsfGlMl3NdNvmld8/wBRoMHH\n" +
"NbM5Hw5oAoIjALHPfA59O9KL218NS3byXtjE9w+GdivqBj8MUyk5ikAOPZ61rtreDUbaO5kiTeV2\n" +
"HJ7r7P8AiuGSOXDyzBE1zlInAKFR6k9a+XFtHt8qWczjGVUKSAvPHu+VLftckzPu+62U2E5256Y7\n" +
"1S+Fkj1DxPDaXEe5YYHldc9doGAfdnFQLE+VDdCc2ZXDaQDMTgFDEC6H175zgjimcv6PfFmvXMcy\n" +
"6SttFtB8+4dIEIIz0PPfsKuE1O7tEMVoy2sfPs28Sxdev3Rmh5Jp52LyzSSnqS7E/nVkIcezrFlj\n" +
"+ibSrC3aXWNbF9dAezaad93P+6Qjp8AKsNMt4LO1hgto0ihXhVj6CldrsSLoMt7qNhuiiDJyPSsc\n" +
"k23Rfgi4qyphkCqAMcUQsue9TsGoAkDP1osXOT1oWO0x2JOeua2rPiki3GO/Hxr0L5F/nFGwNB2q\n" +
"2UWq2+MAXCcxv/g/GopkaNijqVdWIYHqDVSt9uDYbt2pRrKrJJDdgcyrh/7h/wDlawlZLnh7QB96\n" +
"E46ivSnlDXyAAyFD/Nx9elZGQ0XHJVsH3VqSntk3W7oDgEYz6ULbWkkURUuxO9ySOMksTRY5iPx5\n" +
"o6KaMxKSygnkjFdQUzhMca3Mau7KXXII28EdvnVv4FtTLq97dOwLR2PlDHUEsD/ioO1uHCg9Og9B\n" +
"mrPwXrENomp3EynYApYoMkYGOfRfaHuqeKansdlHOMTOeOSelaxjml763ayalcBplW22K8bnoCRl\n" +
"gT3x2xW21v4bosoDIRgKHGGbPTjPpzWjkmhUrYbFNhgPSjLYPN7K45oGNRtDnGCCc+6grvxhDZsY\n" +
"rS3LBeN/9VSvbPTi+KK6GwGBucg0T5HljqSPhXMz4/uFmAa1brzk4qisfFS3UIcMR6g9qL12FS5d\n" +
"FasLOOMAV5ktMDIwamLjxVFagmWTC+g60HF+kOw85UzIfftoJphbrsqTKYwynI7VlzMJdP2fzKQw\n" +
"+VDJqFjqsPmW0w8wclfWtYV3kKLzlSaeLakZ5acT6jhZIz0Vhj4HtXqQFLuTjaXGTitJztZSuGjY\n" +
"EgjHuomQ7khlHpzVh5h8Gfs+e5oK6juvtDbCQuAQMj0FGkZgweo3D8DXiUxiTBYZAHYelKdRw85h\n" +
"uPImyG3glT0z61WadpVteeCdQu4DKk8ZKFVwd53DHPUDDduDioieSYsryZIJ4JGSapvB2pG21CWz\n" +
"iLSR3kZR0ZcYI6H48kUvH2zRjLTdOngh+23gSGOFdzPMvKgEg4Pr299btN8Qw3Es8kenwolxJgvs\n" +
"BaMKPZOex+HrQXirWJdQu10m0kLwwkCUr0kk/wDFe3zo/wAP6KqxyW8hO5wWIHfHXFZSjS0beNXO\n" +
"2Uk2bmyaKH75XaAPSpC48P3qysvnJEf5VPUiq/TzsYhSckkD4U4uNLtL61Czp7XZhwR8xWKu7LeK\n" +
"emckk0a8VZGmkBYfcG7qaqvB+izSyMJh7O3INOx4bsrUmTy2kI6F2JApxo0ZWfftCx4xxTSbloEI\n" +
"KPRz7xjp89veMsSkoOmKQR2N9bTKgtzJuI7HH1zXW9XsYrq4kWZCQ3Rh1FJk8LXayfsLobO3mJuo\n" +
"xlx1QJwUttk3pDzwX8REEkTA9G6H1Hvq41Gznv8ASpoLMSCeZQIihwwbIPB7Vvh0j7PEPtDeY3rj\n" +
"AHw9KUa3c3EVmY7aXy3LfeHXHGefnSOTsbhy+Jmm61JLdHS9e/02oqTHHdNgJKf6X9D6HpTpoJYk\n" +
"aF125JIB6qf+4qXk0/8AWFnHJMfM8yMNyevFEaVq11psiWWozNLZniK4ckmP0Uk9vf2quLdbPKmq\n" +
"k0ig3AxZyOWP1IoG5jZp2IYYwPyo+RCigEdwR7/nXxolJ+5TMCOMy6cjloldDclDtjZcBh2KnOAe\n" +
"vFePCysNZkQTpbSG3lAkk6Icfn2oW3t7iBnTIwcYB7ntjPz+lPI41jucxxtPPHCHaRkUjOBk+hx+\n" +
"NYObho1SbDNGh0+3id/MJkAOd45Pfj6U6s7t0j0yWYCMzxud3Ze+KWX7Lc2lr5YjMqIY9yLgMg4y\n" +
"Pfk0PqazRaXpiSlxuQuAw6ZAroNysWMqdlIbhYH3owK9VPrX1vEwgXJcYHqaTpMG0+DJziNRSe9A\n" +
"julMn7vbuHxrDjuj0Yz1ZTxa8sjm9v8AzvsiA+yvUntTSw8b6TtKx7gOwNRL3cU1qIzcqM44z0pQ\n" +
"+lTtlrWWJjnJAcA08YJPsDyy9I68fE2jX0ixy3Pkk9Gz3oLT/FC+e9vM6syMVV+mR2rnFnpNxHNG\n" +
"8rIWzk5YGnht0lkySEkHcd6E1T0Osl9o6C2qpcIVGDxSHxBMsPh+5lI/anEafE9/oKVafLLFNtkP\n" +
"TofWvPiu9zZw2QHL/tSfdyB+dGHykLmdQbQfeuNP0G2uIQS0fkjJ/m4yR+BpmY7a7tY5gQ8cyhlB\n" +
"HUHtSfxJIX0FdoCwxvGqD19k148H3u+Oe1lbLRANCPicED5mrKPLbKDTHNl/o7pZJbM8xsCd0R9P\n" +
"7fT3+6mhuIFJUTK2OMqQB+Nabc+Vew7sja+XI6+8VN+JpLS08R3sC20MgRgC2M87RkfI8fKhdHUR\n" +
"Eqs826JiFJ3GJicNj8q1K7QasY4lUJuwgDHA+Jr5qGoFZFEC7QVwccU80S7tbjUYIjGrsrsqtJgA\n" +
"nBOPxFScZcv6NZP6FzxvFAIyCu1SQAeSCe3zzTXWJpr2y0uNUy8MGXbcAOQMc/KtfiGAx6rHBCQx\n" +
"8sbQPUscCvfimHz9G8uEYFsqkheAdowf+flVeHC6bMWwG3nVQYDIrFeMq2QOeK3TbJgsUqBjnFLv\n" +
"Clmt1aaizrgewFYDocmi/NZHCsAJAcEGpsiuWi/G6jsMHh61juRcxoGV12tG3TPrTP8AVOnMuJNO\n" +
"lRiwO5FJzwO4rxA7m2U7chhjFDy6heWr/swwHcE8Ckv7N0os3X/h+0kikksoJo2z7O4lfzpRHpN5\n" +
"YzxedcGVmzuH9NUFpfvOp3qzMemelBajMzXMa59r1rm/SOcUj2zYdWX04+NKNbnNzePtIOxAg+PJ\n" +
"/wA0RdXQhAVTlvypNLMMMzHJ3At9a2wQp2SeRk1xRaeLNsWi2sCHOJUUn4IRU/o119j1uynJOBLh\n" +
"h6huP8088Uc6Qsz8Ga4Gz3AKT/kVIGTE8RHUOCPqKoSJDqqkhsknJIOflSZ7qzhkZL1Fe53EuxQ8\n" +
"55/Iinb/AL/AHG4/8U0g01Lm2ilJiJZFzkDPAxQCcCuxslL+YoYnbjOeMVQeFjeR7GjtBcYlLKCv\n" +
"IOBk5xxkVXw6L4fsmDRWMMsi8B5zvP0PFbri+mCCKJkRcYAXgYrePhv+TOc/oU6rpkz6p9ufbCuw\n" +
"KsZbLKff9aC3tgq+DgFT3B7UyklySrcE5z76AmAwW2nGOearhj4RpGVtnrTjFBD9liQIgXgD/vNK\n" +
"tbtnBM0XDDHzFbfPMUgdTlaMd0uYd4OfdXleRheOfJdHoY8inCn2A6TrChtrH7uMBj0p3JrFtMVQ\n" +
"RqSSecdTUff2qpMWT2eeooSKS4jcMrZI6ZrHgnsP5GtF7cXsKo7sAm3sOKQrc+bcTTM37GH2nk9M\n" +
"8AUHBHLecXEh2HqF71v19o7Lw+IIgFEsiqR7hz/ihGK5DSk3GwW7Dw3UyOcurHn8vwrVbWzXtzDB\n" +
"082RUJ9Mmmtvo11rumW1/aNGZBGI5InbaxK8ZHr2r5Z2k1rrVjbSxvE5uEB3jbgZ5q9xa/wgu2OP\n" +
"HE4k+xIh/ZoZAMd8bakl5ljb1I/MVTeNCpWw8v7oMqjj4c/WphWwV5xyDSJ6CdYdv9Vnk4c1SWlq\n" +
"4tYwsgC44GKmCf2z9gGz3qxsJozYQFm5KA/WlOOXyXmV3LbuBjqFOKG86VwSu3gdCc0El/Pavtcs\n" +
"wHGCc0aJBcJ5ls6h+pU165mCudxGXYtnOOgFCzXgSJgxCRgd+rGjmkimO2T9nIOMilV/aOmWOGjJ\n" +
"/wCmhK60cgAXOx/bJIY1vWeSBtybnU9QBS2aNIs7nJ9MCt1ndMDsyfQZPU9qmbT+Mh1raDpl+0Dc\n" +
"o+tBeQ6ScJkVkd5JNqYt7OMyqAd2Ty3rj4UwhxOqyRZKn6j3GoMmNReiqMuW2b7RHwCRilviaQyf\n" +
"Y4V5wxOPU04x5ERZuTj6UhvdUtoL3eEFxMowP6F9ce+kxQVhySqNIrtDjks9DSJWG3GWVutM7e4h\n" +
"ljXzHDqP5ZBnHw7ilFrL5yQOmfLZAw+fNN7e3jVi7DrXs0qoha2aNV0uDUokEMqxSIxIDHIbIHHu\n" +
"qQvrK5sJRHPEVcA4PZvgasb28gXIVFDD+YcZoIzG/ga2uYjLG3C46qT3rGeFVaOTKQXDNKwlOT0y\n" +
"F93f51To5ihiQZAWJB0/2ipRgWiaQnLsNicDjA6mqaa1DuG2/wAqjqewAqFpp0zRHKNR/eCtel/x\n" +
"ArKyvVZmEah+9Wvp/gz8Kyspl0cIZ/3bUvi6r/dWVlSZOx4maF/6wvz/ADNOtB/eXf8A8jVlZUcz\n" +
"aPYXq38EfnUbcfxXyFZWUMQch0LTP4SH+2nX/sn4VlZXqx/VEz7EF19/50bpf8SPgPzrKyuAO4P3\n" +
"0vx/+oqw7D4D8qysrzcv7jro/9k=\n" +
"    </BINVAL>\n" +
"  </LOGO>\n" +
"  <ORG>\n" +
"    <ORGNAME>XMPP Standards Foundation</ORGNAME>\n" +
"  </ORG>\n" +
"  <URL>https://stpeter.im/</URL>\n" +
"  <URL>http://www.saint-andre.com/</URL>\n" +
"  <KEY>\n" +
"    <CRED>\n" +
"-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
"Version: GnuPG/MacGPG2 v2.0.18 (Darwin)\n" +
"\n" +
"mQINBFETDzsBEAC0FOv1N3ZJzIIxN6cKD475KVS9CHDPeYpegcOIPnL5eY1DCHeh\n" +
"/IwS1S7RCePtmiybNoV9FsI4PKUknzXQxA6LVEdAR/LUlhgJKjq+gsgp8lqbEILh\n" +
"g13ecH66HwLS9rarbQkC47T7kL8miIPBFC6E3A4Lq1L+eueO6UcLhKgoYkMxOjdi\n" +
"WrMgKTnVpch5ydLkPm/z0Zo8zRgqlPuTLeCrXXZYnjHXLVFN2xy04UzOs7P5u5KV\n" +
"fx5Z7uQisr8pXtyLd6SpTZo6SHgKBv15uz0rqXhsJojiGtOXfWznAjaS5FUOORq9\n" +
"CklG5cMOUAT8TNftv0ktsxaWDL1ELDVQPy1m7mtzo+VREG+0xmU6AjMo/GHblW1U\n" +
"U7MI9yCiuMLsp/HLrFuiosqLVZ85wuLQ2junPe3tK8h15UcxIXAcpQ1VqIaDQFbe\n" +
"uLOXJTF8YHpHdpHYt/ZM1ll7ZBKGAo8yd7uF7wJ9D3gUazwdz9fFjWV7oIk7ATwO\n" +
"lFllzmWDn+M2ygbHOGUGMX5hSaa8eDSieiR2QoLdn27Fip7kMBTJ2+GISrfnJTN/\n" +
"OQvmj0DXXAdxHmu2C4QgmZbkge35n129yzXn9NcqzrGLroV62lL3LgX6cSbiH5i7\n" +
"GgWY6CAPb1pMogV0K475n9FvOSDRiG4QSO5yqKiA3OP5aKrIRp2TNAk4IwARAQAB\n" +
"tCZQZXRlciBTYWludC1BbmRyZSA8c3RwZXRlckBzdHBldGVyLmltPokCOQQTAQIA\n" +
"IwUCURMPOwIbAwcLCQgHAwIBBhUIAgkKCwQWAgMBAh4BAheAAAoJEOoGpJErxa2p\n" +
"6bgQAKpxu07cMDOLc4+EG8H19NWXIVVybOEvfGuHYZaLKkPrhrMZwJiOwBpyISNR\n" +
"t9qzX1eLCVaojaoEVX6kD8MGc5zKFfiJZy3j7lBWl+Ybr7FfXYy2BbAXKx49e1n6\n" +
"ci9LmBrmVfAEaxtDNPITZ9N9oUAb9vS0nrG036EwteEHAveQvlDjO7lhz6+Cv7lZ\n" +
"QgBj9rZ6khfcQ4S3nSCQaKLQ9Iav4fqxI7SfuPKnx6quHX3JNLGnVo3wl+j/foCK\n" +
"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\n" +
"rLuMVIV/\n" +
"=llGw\n" +
"-----END PGP PUBLIC KEY BLOCK-----\n" +
"    </CRED>\n" +
"  </KEY>\n" +
"  <NOTE>\n" +
"    More information about me is located on my \n" +
"    personal website: https://stpeter.im/\n" +
"  </NOTE>\n" +
"</vCard>").replace("\n", "").replace("  ","");
	
	
	protected SimpleParser        parser = SingletonFactory.getParserInstance();
	
	@Test
	public void testVCardTempToVCard4() {
		char[] data = VCARD_TEMP_DATA.toCharArray();
		DomBuilderHandler handler = new DomBuilderHandler();
		parser.parse(handler, data, 0, data.length);
		Element vcardTemp = handler.getParsedElements().poll();
		data = VCARD4_DATA.toCharArray();
		parser.parse(handler, data, 0, data.length);
		Element expResult = handler.getParsedElements().poll();
		
		Element vcard4 = VCardXMPPProcessorAbstract.convertVCardTempToVCard4(vcardTemp);
		assertNotEquals(vcardTemp, vcard4);
		assertEquals(expResult.toString(), vcard4.toString());
	}

	@Test
	public void testVCard4ToVCardTemp() {
		char[] data = VCARD4_DATA.toCharArray();
		DomBuilderHandler handler = new DomBuilderHandler();
		parser.parse(handler, data, 0, data.length);
		Element vcard4 = handler.getParsedElements().poll();
		data = VCARD2_TEMP_DATA.toCharArray();
		parser.parse(handler, data, 0, data.length);
		Element expResult = handler.getParsedElements().poll();
		
		Element vcardTemp = VCardXMPPProcessorAbstract.convertVCard4ToVCardTemp(vcard4);
		assertNotEquals(vcard4, vcardTemp);
		assertEquals(expResult.toString(), vcardTemp.toString());
	}

}
