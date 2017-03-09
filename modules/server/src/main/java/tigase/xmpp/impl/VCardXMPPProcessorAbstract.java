/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.xmpp.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tigase.db.TigaseDBException;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;

/**
 *
 * @author andrzej
 */
public abstract class VCardXMPPProcessorAbstract extends XMPPProcessorAbstract {
	
	private static final Pattern DATA_URI_PATTERN = Pattern.compile("data:(.+);base64,(.+)");
	private static final Pattern TEL_URI_PATTERN = Pattern.compile("tel:(.+)");
	private static final Pattern XMPP_URI_PATTERN = Pattern.compile("xmpp:(.+)");
	private static final Pattern GEO_URI_PATTERN = Pattern.compile("geo:([\\-0-9\\.]+),([\\-0-9\\.]+)");
	
	private static Map<String, VCardXMPPProcessorAbstract> PROCESSORS = new ConcurrentHashMap<>();
	private static Map<String, Converter> CONVERTERS = new ConcurrentHashMap<>();
	
	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		super.init(settings);
		PROCESSORS.put(this.id(), this);
	}
	
	protected abstract String getVCardXMLNS();
	
	protected void setVCard(XMPPResourceConnection session, Element vcard) throws TigaseDBException, NotAuthorizedException {
		for (VCardXMPPProcessorAbstract processor : PROCESSORS.values()) {
			Converter conv = CONVERTERS.get(getVCardXMLNS() + "|" + processor.getVCardXMLNS());
			Element vcardEl = conv == null ? vcard : conv.convert(vcard);
			processor.storeVCard(session, vcardEl);
		}
	}
	
	protected abstract void storeVCard(XMPPResourceConnection session, Element vcard) throws TigaseDBException, NotAuthorizedException;
	
	private interface Converter {
		
		Element convert(Element vcard);
		
	}
	
	static {
		CONVERTERS.put(VCardTemp.XMLNS + "|" + VCard4.XMLNS, (Element vcardTemp) -> {
		if (vcardTemp == null)
			return null;
		
		Element vcard4= new Element(VCard4.VCARD_EL, new String[] { "xmlns" }, new String[] { VCard4.XMLNS });
		
		vcardTemp.forEachChild((Element c) -> {
			Element r = null;
			List<Element> list = null;
			Element parameters = null;
			Element type = null;

			switch (c.getName()) {
				case "FN":
				case "NICKNAME":
				case "TZ":
				case "TITLE":
				case "ROLE":
				case "CATEGORIES":
				case "NOTE":
				case "PRODID":
					r = new Element(c.getName().toLowerCase());
					r.addChild(new Element("text", c.getCData()));
					break;
				case "N":
					r = new Element("n");
					list = c.mapChildren((Element c1) -> {
						switch (c1.getName()) {
							case "GIVEN":
								return new Element("given", c1.getCData());
							case "FAMILY":
								return new Element("surname", c1.getCData());
							case "MIDDLE":
								return new Element("additional", c1.getCData());
							case "PREFIX":
								return new Element("prefix", c1.getCData());
							case "SUFFIX":
								return new Element("suffix", c1.getCData());
							default:
								return null;
						}
					});
					if (list != null) {
						list.removeIf((Element c1) -> c1 == null);
						r.addChildren(list);
					}
					break;
				case "PHOTO":
				case "LOGO":
					r = new Element(c.getName().toLowerCase());
					Element extVal = c.findChild((Element c1) -> c1.getName() == "EXTVAL");
					if (extVal != null) {
						r.addChild(new Element("uri", extVal.getCData()));
					} else {
						r.addChild(new Element("uri", "data:" + c.getChildCData((Element c1) -> c1.getName() == "TYPE")  + ";base64," + c.getChildCData((Element c1) -> c1.getName() == "BINVAL")));
					}
					break;
				case "BDAY":
					r = new Element(c.getName().toLowerCase());
					r.addChild(new Element("date", c.getCData()));
					break;
				case "ADR":
					r = new Element("adr");
					for (Element c1 : c.getChildren()) {
						switch (c1.getName()) {
							case "POBOX":
							case "STREET":
							case "LOCALITY":
							case "REGION":
								r.addChild(new Element(c1.getName().toLowerCase(), c1.getCData()));
								break;
							case "EXTADD":
								r.addChild(new Element("ext", c1.getCData()));
								break;								
							case "PCODE":
								r.addChild(new Element("code", c1.getCData()));
								break;						
							case "CTRY":
								r.addChild(new Element("country", c1.getCData()));
								break;
							case "HOME":
							case "WORK":
								if (parameters == null) {
									parameters = new Element("parameters");
									r.addChild(parameters);
								}
								if (type == null) {
									type = new Element("type");
									parameters.addChild(type);
								}
								type.addChild(new Element("text", c1.getName().toLowerCase()));
								break;
							case "PREF":
								if (parameters == null) {
									parameters = new Element("parameters");
									r.addChild(parameters);
								}
								Element pref = new Element("pref");
								pref.addChild(new Element("integer", "1"));
								parameters.addChild(pref);
								break;
							default:
								break;
						}
					}
					break;
				case "TEL":
					r = new Element("tel");
					for (Element c1 : c.getChildren()) {
						switch (c1.getName()) {
							case "HOME":
							case "WORK":
							case "TEXT":
							case "FAX":
							case "CELL":
							case "VOICE":
							case "VIDEO":
							case "PAGER":
							case "TEXTPHONE":
								if (parameters == null) {
									parameters = new Element("parameters");
									r.addChild(parameters);
								}
								if (type == null) {
									type = new Element("type");
									parameters.addChild(type);
								}
								type.addChild(new Element("text", c1.getName().toLowerCase()));
								break;
							case "PREF":
								if (parameters == null) {
									parameters = new Element("parameters");
									r.addChild(parameters);
								}
								Element pref = new Element("pref");
								pref.addChild(new Element("integer", "1"));
								parameters.addChild(pref);
								break;
							case "NUMBER":
								Element uri = new Element("uri", "tel:" + c1.getCData());
								r.addChild(uri);
							default:
								break;
						}
					}
					break;
				case "EMAIL":
					r = new Element("email");
					for (Element c1 : c.getChildren()) {
						switch (c1.getName()) {
							case "HOME":
							case "WORK":
								if (parameters == null) {
									parameters = new Element("parameters");
									r.addChild(parameters);
								}
								if (type == null) {
									type = new Element("type");
									parameters.addChild(type);
								}
								type.addChild(new Element("text", c1.getName().toLowerCase()));
								break;
							case "PREF":
								if (parameters == null) {
									parameters = new Element("parameters");
									r.addChild(parameters);
								}
								Element pref = new Element("pref");
								pref.addChild(new Element("integer", "1"));
								parameters.addChild(pref);
								break;
							case "USERID":
								r.addChild(new Element("text", c1.getCData()));
							default:
								break;
						}
					}
					break;
				case "JABBERID":
					r = new Element("impp");
					r.addChild(new Element("uri", "xmpp:" + c.getCData()));
					break;
				case "GEO":
					r = new Element(c.getName().toLowerCase());
					r.addChild(new Element("uri", "geo:" + c.getChildCData((Element c1) -> c1.getName() == "LAT") + "," + c.getChildCData((Element c1) -> c1.getName() == "LON")));
					break;
				case "AGENT":
					r = new Element(c.getName().toLowerCase());	
					r.addChild(new Element("uri", c.getChildCData((Element c1) -> c1.getName() == "EXTVAL")));
					break;
				case "ORG":
					c = c.findChild((Element c1) -> c1.getName() == "ORGNAME");
					if (c != null) {
						r = new Element("org");
						r.addChild(new Element("text", c.getCData()));
					}
					break;
				case "REV":
					r = new Element("rev");
					r.addChild(new Element("timestamp", c.getCData()));
					break;
				case "SORT-STRING":
					r = new Element("sort-as", c.getCData());
					break;
				case "SOUND":
					r = new Element("sound");
					extVal = c.findChildStaticStr(new String[] { "EXTVAL" });
					if (extVal != null) {
						r.addChild(new Element("uri", extVal.getCData()));
					} else {
						r.addChild(new Element("uri", "data:audio/basic;base64," + c.getChildCData((Element c1) -> c1.getName() == "BINVAL")));
					}
					break;
				case "UID":
				case "URL":
					r = new Element(c.getName().toLowerCase());
					r.addChild(new Element("uri", c.getCData()));
					break;
				case "KEY":
					r = new Element(c.getName().toLowerCase());
					r.addChild(new Element("text", c.getChildCData((Element c1) -> c1.getName() == "CRED")));
					break;
				case "DESC":
					r = new Element("note");
					r.addChild(new Element("text", c.getCData()));
					break;
				default:
					break;
			}
			if (r != null)
				vcard4.addChild(r);
		});
		
		return vcard4;
		});
	
		CONVERTERS.put(VCard4.XMLNS + "|" + VCardTemp.XMLNS, (Element vcard4) -> {
		Element vcardTemp = new Element(VCardTemp.vCard, new String[] { "xmlns" }, new String[] { VCardTemp.XMLNS });
		
		vcard4.forEachChild((Element c) -> {
			Element r = null;
			Element text = null;
			Element uri = null;	
			List<Element> list = null;
			
			Element parameters = null;
			Element type = null;

			switch (c.getName()) {
				case "fn":
				case "nickname":
				case "tz":
				case "title":
				case "role":
				case "categories":
				case "note":
				case "prodid":
					text = c.findChild((Element c1) -> c1.getName() == "text");
					if (text != null) {
						r = new Element(c.getName().toUpperCase(), text.getCData());
					}
					break;
				case "n":
					r = new Element("N");
					list = c.mapChildren((Element c1) -> {
						switch (c1.getName()) {
							case "given":
								return new Element("GIVEN", c1.getCData());
							case "surname":
								return new Element("FAMILY", c1.getCData());
							case "additional":
								return new Element("MIDDLE", c1.getCData());
							case "prefix":
								return new Element("PREFIX", c1.getCData());
							case "suffix":
								return new Element("SUFFIX", c1.getCData());
							default:
								return null;
						}
					});
					if (list != null) {
						list.removeIf((Element c1) -> c1 == null);
						r.addChildren(list);
					}
					break;
				case "photo":
				case "logo":
					uri = c.findChild((Element c1) -> c1.getName() == "uri");
					if (uri != null) {
						r = new Element(c.getName().toUpperCase());
						String uriStr = uri.getCData();
						Matcher matcher = DATA_URI_PATTERN.matcher(uriStr);
						if (matcher.matches()) {
							r.addChild(new Element("TYPE", matcher.group(1)));
							r.addChild(new Element("BINVAL", matcher.group(2)));
						} else {
							r.addChild(new Element("EXTVAL", uriStr));
						}
					}
					break;
				case "bday":
					Element date = c.findChild((Element c1) -> c1.getName() == "date");
					if (date != null) {
						r = new Element(c.getName().toUpperCase(), date.getCData());					
					}
					break;
				case "adr":
					r = new Element("ADR");
					for (Element c1 : c.getChildren()) {
						switch (c1.getName()) {
							case "pobox":
							case "street":
							case "locality":
							case "region":
								r.addChild(new Element(c1.getName().toUpperCase(), c1.getCData()));
								break;
							case "ext":
								r.addChild(new Element("EXTADD", c1.getCData()));
								break;								
							case "code":
								r.addChild(new Element("PCODE", c1.getCData()));
								break;						
							case "country":
								r.addChild(new Element("CTRY", c1.getCData()));
								break;
							case "parameters":
								for (Element c2 : c1.getChildren()) {
									switch (c2.getName()) {
										case "type":
											list = c2.findChildren((Element c3) -> c3.getName() == "text");
											if (list != null) {
												for (Element c3 : list) {
													r.addChild(new Element(c3.getCData().toUpperCase()));
												}
											}
										case "pref":
											text = c2.findChild((Element c3) -> c3.getName() == "integer");
											if (text != null) {
												r.addChild(new Element("PREF"));
											}
											break;
										default:
											break;
									}
								}
								break;
							default:
								break;
						}
					}
					break;
				case "tel":
					r = new Element("TEL");
					for (Element c1 : c.getChildren()) {
						switch (c1.getName()) {
							case "parameters":
								for (Element c2 : c1.getChildren()) {
									switch (c2.getName()) {
										case "type":
											list = c2.findChildren((Element c3) -> c3.getName() == "text");
											if (list != null) {
												for (Element c3 : list) {
													r.addChild(new Element(c3.getCData().toUpperCase()));
												}
											}
										case "pref":
											text = c2.findChild((Element c3) -> c3.getName() == "integer");
											if (text != null) {
												r.addChild(new Element("PREF"));
											}
											break;
										default:
											break;
									}
								}								
								break;
							case "uri":
								String uriStr = c1.getCData();
								Matcher matcher = TEL_URI_PATTERN.matcher(uriStr);
								if (matcher.matches()) {
									r.addChild(new Element("NUMBER", matcher.group(1)));							
								}
							default:
								break;
						}
					}
					break;
				case "email":
					r = new Element("EMAIL");
					for (Element c1 : c.getChildren()) {
						switch (c1.getName()) {
							case "parameters":
								for (Element c2 : c1.getChildren()) {
									switch (c2.getName()) {
										case "type":
											list = c2.findChildren((Element c3) -> c3.getName() == "text");
											if (list != null) {
												for (Element c3 : list) {
													r.addChild(new Element(c3.getCData().toUpperCase()));
												}
											}
										case "pref":
											text = c2.findChild((Element c3) -> c3.getName() == "integer");
											if (text != null) {
												r.addChild(new Element("PREF"));
											}
											break;
										default:
											break;
									}
								}								
								break;
							case "text":
								r.addChild(new Element("USERID", c1.getCData()));
							default:
								break;
						}
					}
					break;
				case "impp":
					uri = c.findChild((Element c1) -> c1.getName() == "uri");
					if (uri != null) {
						Matcher matcher = XMPP_URI_PATTERN.matcher(uri.getCData());
						if (matcher.matches()) {
							r = new Element("JABBERID", matcher.group(1));
						}
					}
				case "geo":
					uri = c.findChild((Element c1) -> c1.getName() == "uri");
					if (uri != null) {
						Matcher matcher = GEO_URI_PATTERN.matcher(uri.getCData());
						if (matcher.matches()) {
							r = new Element("GEO");
							r.addChild(new Element("LAT", matcher.group(1)));
							r.addChild(new Element("LON", matcher.group(2)));
						}
					}					
					break;
				case "agent":
					r = new Element(c.getName().toUpperCase());	
					uri = c.findChild((Element c1) -> c1.getName() == "uri");
					r.addChild(new Element("EXTVAL", uri.getCData()));
					break;
				case "org":
					text = c.findChild((Element c1) -> c1.getName() == "text");
					if (text != null) {
						r = new Element("ORG");
						r.addChild(new Element("ORGNAME", text.getCData()));
					}
					break;
				case "rev":
					text = c.findChild((Element c2) -> c2.getName() == "timestamp");
					if (text != null) {
						r = new Element("REV", text.getCData());
					}
					break;
				case "sort-as":
					r = new Element("SORT-STRING", c.getCData());
					break;
				case "sound":
					r = new Element("SOUND");
					uri =  c.findChild((Element c1) -> c1.getName() == "uri");
					if (uri != null) {
						Matcher matcher = DATA_URI_PATTERN.matcher(uri.getCData());
						if (matcher.matches()) {
							r.addChild(new Element("BINVAL", matcher.group(2)));
						} else {
							r.addChild(new Element("EXTVAL", uri.getCData()));
						}
					}
					break;
				case "uid":
				case "url":
					uri =  c.findChild((Element c1) -> c1.getName() == "uri");
					if (uri != null) {
						r = new Element(c.getName().toUpperCase(), uri.getCData());
					}
					break;
				case "key":
					text = c.findChild((Element c1) -> c1.getName() == "text");
					if (text != null) {
						r = new Element(c.getName().toUpperCase());
						r.addChild(new Element("CRED", text.getCData()));
					}
					break;
				default:
					break;
			}
			if (r != null)
				vcardTemp.addChild(r);
		});		
		
		return vcardTemp;
		});
	}

	public static Element convertVCardTempToVCard4(Element vcardTemp) {
		return CONVERTERS.get(VCardTemp.XMLNS + "|" + VCard4.XMLNS).convert(vcardTemp);
	}
	
	public static Element convertVCard4ToVCardTemp(Element vcard4) {
		return CONVERTERS.get(VCard4.XMLNS + "|" + VCardTemp.XMLNS).convert(vcard4);
	}
	
}
