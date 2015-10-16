package tigase.component.modules;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.component.PacketWriter;
import tigase.component.exceptions.ComponentException;
import tigase.component.responses.ResponseManager;
import tigase.criteria.Criteria;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.StanzaType;

@Bean(name = "stanzaProcessor")
public class StanzaProcessor {

	private Logger log = Logger.getLogger(this.getClass().getName());

	@Inject(type = Module.class)
	private List<Module> modules;

	@Inject
	private ResponseManager responseManager;

	@Inject
	private PacketWriter writer;

	public List<Module> getModules() {
		return modules;
	}

	public ResponseManager getResponseManager() {
		return responseManager;
	}

	public PacketWriter getWriter() {
		return writer;
	}

	private boolean process(final Packet packet) throws ComponentException, TigaseStringprepException {
		boolean handled = false;
		if (log.isLoggable(Level.FINER)) {
			log.finest("Processing packet: " + packet.toString());
		}

		for (Module module : this.modules) {
			Criteria criteria = module.getModuleCriteria();
			if (criteria != null && criteria.match(packet.getElement())) {
				handled = true;
				if (log.isLoggable(Level.FINER)) {
					log.finer("Handled by module " + module.getClass());
				}
				module.process(packet);
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Finished " + module.getClass());
				}
				break;
			}
		}
		return handled;
	}

	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Received: " + packet.getElement());
		}
		try {
			Runnable responseHandler = responseManager.getResponseHandler(packet);

			boolean handled;
			if (responseHandler != null) {
				handled = true;
				responseHandler.run();
			} else {
				handled = this.process(packet);
			}

			if (!handled) {
				final String t = packet.getElement().getAttributeStaticStr(Packet.TYPE_ATT);
				final StanzaType type = (t == null) ? null : StanzaType.valueof(t);

				if (type != StanzaType.error) {
					throw new ComponentException(Authorization.FEATURE_NOT_IMPLEMENTED);
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.finer(packet.getElemName() + " stanza with type='error' ignored");
					}
				}
			}
		} catch (TigaseStringprepException e) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, e.getMessage() + " when processing " + packet.toString());
			}
			sendException(packet, new ComponentException(Authorization.JID_MALFORMED));
		} catch (ComponentException e) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, e.getMessageWithPosition() + " when processing " + packet.toString());
			}
			sendException(packet, e);
		} catch (Exception e) {
			if (log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, e.getMessage() + " when processing " + packet.toString(), e);
			}
			sendException(packet, new ComponentException(Authorization.INTERNAL_SERVER_ERROR));
		}
	}

	/**
	 * Converts {@link ComponentException} to XMPP error stanza and sends it to
	 * sender of packet.
	 *
	 *
	 * @param packet
	 *            packet what caused exception.
	 * @param e
	 *            exception.
	 */
	public void sendException(final Packet packet, final ComponentException e) {
		try {
			final String t = packet.getElement().getAttributeStaticStr(Packet.TYPE_ATT);

			if ((t != null) && (t == "error")) {
				if (log.isLoggable(Level.FINER)) {
					log.finer(packet.getElemName() + " stanza already with type='error' ignored");
				}

				return;
			}

			Packet result = e.makeElement(packet, true);
			Element el = result.getElement();

			el.setAttribute("from", BareJID.bareJIDInstance(el.getAttributeStaticStr(Packet.FROM_ATT)).toString());
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Sending back: " + result.toString());
			}
			writer.write(result);
		} catch (Exception e1) {
			if (log.isLoggable(Level.WARNING)) {
				log.log(Level.WARNING, "Problem during generate error response", e1);
			}
		}
	}

	public void setModules(List<Module> modules) {
		this.modules = modules;
	}

	public void setResponseManager(ResponseManager responseManager) {
		this.responseManager = responseManager;
	}

	public void setWriter(PacketWriter writer) {
		this.writer = writer;
	}

}
