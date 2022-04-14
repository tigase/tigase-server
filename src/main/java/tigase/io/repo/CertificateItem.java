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

package tigase.io.repo;

import tigase.cert.CertificateEntry;
import tigase.cert.CertificateUtil;
import tigase.db.comp.RepositoryItemAbstract;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;

import java.io.CharArrayReader;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CertificateItem
		extends RepositoryItemAbstract {

	public static final String PEM_CERTIFICATE_KEY = "pem-certificate";
	public static final String FINGERPRINT_KEY = "fingerprint";
	public static final String SERIALNUMBER_KEY = "serial-number";
	public static final String IS_DEFAULT_KEY = "is-default";
	public static final String ALIAS_KEY = "alias";
	public static final String REPO_ITEM_ELEM_NAME = "certificate";

	private static final Logger log = Logger.getLogger(CertificateItem.class.getName());
	private String alias;
	private CertificateEntry entry;
	private String fingerprint;
	private boolean isDefault;
	private String serialNumber;

	public CertificateItem() {
	}

	public CertificateItem(String alias, CertificateEntry entry) {
		Objects.requireNonNull(alias);
		Objects.requireNonNull(entry);
		if ("default".equals(alias)) {
			this.isDefault = true;
		}
		this.alias = CertificateUtil.getCertCName((X509Certificate) entry.getCertificate().get());
		this.entry = entry;
		try {
			if (entry.getCertificate().isPresent()) {
				final Certificate certificate = entry.getCertificate().get();
				fingerprint = CertificateUtil.getCertificateFingerprint(certificate);
				CertificateUtil.getCertificateSerialNumber(certificate)
						.ifPresent(serialNumber -> this.serialNumber = serialNumber.toString(16));
			}
		} catch (CertificateEncodingException | NoSuchAlgorithmException e) {
			log.log(Level.WARNING, "Failing creating Certificate item", e);
		}
	}

	public Optional<String> getSerialNumber() {
		return Optional.ofNullable(serialNumber);
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public Optional<String> getFingerprint() {
		return Optional.ofNullable(fingerprint);
	}

	public void setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
	}

	@Override
	public String getKey() {
		return getAlias();
	}

	@Override
	protected void setKey(String key) {
		setAlias(key);
	}

	@Override
	public String getElemName() {
		return REPO_ITEM_ELEM_NAME;
	}

	public CertificateEntry getCertificateEntry() {
		return entry;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public void initFromCommand(Packet packet) {
		log.log(Level.FINEST, "Initiating item from command");
		super.initFromCommand(packet);
		alias = Command.getFieldValue(packet, ALIAS_KEY);
		fingerprint = Command.getFieldValue(packet, FINGERPRINT_KEY);
		serialNumber = Command.getFieldValue(packet, SERIALNUMBER_KEY);
		isDefault = Boolean.parseBoolean(Command.getFieldValue(packet, IS_DEFAULT_KEY));
		final String pemCertificate = Command.getFieldValue(packet, PEM_CERTIFICATE_KEY);
		try {
			entry = CertificateUtil.parseCertificate(new CharArrayReader(pemCertificate.toCharArray()));
		} catch (Exception e) {
			log.log(Level.WARNING, "Error while loading certificate from PEM format", e);
		}
	}

	@Override
	public void addCommandFields(Packet packet) {
		super.addCommandFields(packet);
		Command.addFieldValue(packet, ALIAS_KEY, alias);
		if (getFingerprint().isPresent()) {
			Command.addFieldValue(packet, FINGERPRINT_KEY, getFingerprint().get());
		}
		if (getSerialNumber().isPresent()) {
			Command.addFieldValue(packet, SERIALNUMBER_KEY, getSerialNumber().get());
		}
		Command.addFieldValue(packet, IS_DEFAULT_KEY, String.valueOf(isDefault));
		try {
			final String pemCertificate = CertificateUtil.exportToPemFormat(entry);
			Command.addFieldValue(packet, PEM_CERTIFICATE_KEY, pemCertificate);
		} catch (CertificateEncodingException e) {
			log.log(Level.WARNING, "Error converting certificate entry to PEM format", e);
		}
	}

	@Override
	public void initFromElement(Element elem) {
		log.log(Level.FINEST, "Initiating item from element: " + elem);
		if (elem.getName() != REPO_ITEM_ELEM_NAME) {
			throw new IllegalArgumentException("Incorrect element name, expected: " + REPO_ITEM_ELEM_NAME);
		}
		super.initFromElement(elem);
		setAlias(elem.getAttributeStaticStr(ALIAS_KEY));
		setFingerprint(elem.getAttributeStaticStr(FINGERPRINT_KEY));
		setDefault(Boolean.parseBoolean(elem.getAttributeStaticStr(IS_DEFAULT_KEY)));
		String pemCertificate = elem.getCData();
		if (pemCertificate == null) {
			// handling of the short-lived case where certificate was stored as an attribute…
			pemCertificate = elem.getAttributeStaticStr(PEM_CERTIFICATE_KEY);
		}
		if (pemCertificate == null) {
			throw new IllegalArgumentException(
					"Certificate is missing - neither as element attribute or CData: " + elem);
		}
		try {
			entry = CertificateUtil.parseCertificate(new CharArrayReader(pemCertificate.toCharArray()));
		} catch (Exception e) {
			log.log(Level.WARNING, "Error while loading certificate from PEM format: " + elem, e);
		}
		String serialNumberVal = elem.getAttributeStaticStr(SERIALNUMBER_KEY);
		if (serialNumberVal != null) {
			setSerialNumber(serialNumberVal);
		} else if (entry.getCertificate().isPresent()) {
			CertificateUtil.getCertificateSerialNumber(entry.getCertificate().get())
					.ifPresent(serialNumber -> setSerialNumber(serialNumber.toString(16)));
		}
	}

	@Override
	public void initFromPropertyString(String propString) {
		throw new UnsupportedOperationException("Configuring via property string is not supported");
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean aDefault) {
		isDefault = aDefault;
	}

	@Override
	public Element toElement() {
		Element elem = super.toElement();

		elem.addAttribute(ALIAS_KEY, alias);
		if (getFingerprint().isPresent()) {
			elem.addAttribute(FINGERPRINT_KEY, getFingerprint().get());
		}
		if (getSerialNumber().isPresent()) {
			elem.addAttribute(SERIALNUMBER_KEY, getSerialNumber().get());
		}
		elem.addAttribute(IS_DEFAULT_KEY, String.valueOf(isDefault()));
		try {
			final String pemCertificate = CertificateUtil.exportToPemFormat(entry);
			elem.setCData(pemCertificate);
		} catch (CertificateEncodingException e) {
			log.log(Level.WARNING, "Error converting certificate entry to PEM format", e);
		}
		return elem;
	}

	@Override
	public String toString() {
		return "CertificateItem{" + "alias='" + alias + '\'' + ", fingerprint='" + fingerprint + '\'' +
				", isDefault=" + isDefault + ", serialNumber='" + serialNumber + '\'' + '}';
	}

	@Override
	public String toPropertyString() {
		throw new UnsupportedOperationException("Configuring via property string is not supported");
	}
}
