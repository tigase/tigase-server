/**
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
package tigase.xmpp.impl.annotation;

import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;

import java.util.*;

/**
 * This class is extension of {@link tigase.xmpp.XMPPProcessor XMPPProcessor} which provides support for defining
 * processor Id, supported paths and XMLNSs and more using annotations
 *
 * @author andrzej
 */
public abstract class AnnotatedXMPPProcessor
		extends XMPPProcessor {

	private Element[] DISCO_FEATURES;
	private String[][] ELEMENTS;
	private String ID;
	private Set<StanzaType> STANZA_TYPES;
	private Element[] STREAM_FEATURES;
	private String[] XMLNSS;

	private static void processHandle(Handle handle, List<String[]> elems, List<String> xmlnss) {
		if (handle != null) {
			String[] path = null;
			if (handle.path().length > 0) {
				path = handle.path();
			} else {
				path = handle.pathStr().split("/");
			}
			for (int i = 0; i < path.length; i++) {
				path[i] = path[i].intern();
			}
			elems.add(path);
			xmlnss.add(handle.xmlns().intern());
		}
	}

	protected AnnotatedXMPPProcessor() {
		Class cls = this.getClass();

		cmpInfo = null;
		// support for id
		Id id = (Id) cls.getAnnotation(Id.class);
		if (id != null) {
			ID = id.value();
		}

		// support for supported elements paths and xmlnss
		processHandleAnnotation(cls);

		// support for supported type of stanzas
		processHandleStazaTypesAnnotation(cls);

		// support for stream features
		processStreamFeaturesAnnotation(cls);

		// support for disco features
		processDiscoFeaturesAnnotation(cls);

		cmpInfo = null;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	@Override
	public Element[] supDiscoFeatures(XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public Element[] supStreamFeatures(XMPPResourceConnection session) {
		return STREAM_FEATURES;
	}

	@Override
	public Set<StanzaType> supTypes() {
		return STANZA_TYPES;
	}

	private void processHandleAnnotation(Class cls) {
		List<String[]> elems = new ArrayList<String[]>();
		List<String> xmlnss = new ArrayList<String>();

		Handles handles = (Handles) cls.getAnnotation(Handles.class);
		if (handles != null) {
			for (Handle handle : handles.value()) {
				processHandle(handle, elems, xmlnss);
			}
		}
		Handle handle = (Handle) cls.getAnnotation(Handle.class);
		processHandle(handle, elems, xmlnss);

		if (!elems.isEmpty()) {
			ELEMENTS = elems.toArray(new String[elems.size()][]);
			XMLNSS = xmlnss.toArray(new String[xmlnss.size()]);
		}
	}

	private void processStreamFeaturesAnnotation(Class cls) {
		StreamFeatures streamFeatures = (StreamFeatures) cls.getAnnotation(StreamFeatures.class);
		if (streamFeatures != null) {
			List<Element> values = new ArrayList<Element>();
			for (StreamFeature feature : streamFeatures.value()) {
				final Element featureElement = new Element(feature.elem(), new String[]{"xmlns"},
														   new String[]{feature.xmlns()});
				if (feature.children().length > 0) {
					for (String child : feature.children()) {
						featureElement.addChild(new Element(child));
					}
				}
				values.add(featureElement);
			}
			STREAM_FEATURES = values.toArray(new Element[values.size()]);
		}
	}

	private void processDiscoFeaturesAnnotation(Class cls) {
		DiscoFeatures discoFeatures = (DiscoFeatures) cls.getAnnotation(DiscoFeatures.class);
		if (discoFeatures != null) {
			List<Element> values = new ArrayList<Element>();
			for (String feature : discoFeatures.value()) {
				values.add(new Element("feature", new String[]{"var"}, new String[]{feature}));
			}
			DISCO_FEATURES = values.toArray(new Element[values.size()]);
		}
	}

	private void processHandleStazaTypesAnnotation(Class cls) {
		HandleStanzaTypes handleStanzaTypes = (HandleStanzaTypes) cls.getAnnotation(HandleStanzaTypes.class);
		if (handleStanzaTypes != null) {
			StanzaType[] types = handleStanzaTypes.value();
			EnumSet<StanzaType> tmp = EnumSet.noneOf(StanzaType.class);
			tmp.addAll(Arrays.asList(types));
			STANZA_TYPES = tmp;
		}
	}
}
