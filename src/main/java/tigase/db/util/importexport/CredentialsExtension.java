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
package tigase.db.util.importexport;

import tigase.auth.CredentialsDecoderBean;
import tigase.auth.credentials.Credentials;
import tigase.auth.credentials.entries.*;
import tigase.db.AbstractAuthRepositoryWithCredentials;
import tigase.db.AuthRepository;
import tigase.db.UserNotFoundException;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.util.Base64;
import tigase.util.ClassUtil;
import tigase.util.ui.console.CommandlineParameter;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;

import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tigase.db.util.importexport.RepositoryManager.isSet;

public class CredentialsExtension
		extends RepositoryManagerExtensionBase {

	private static final Logger log = Logger.getLogger(CredentialsExtension.class.getSimpleName());

	private final CommandlineParameter EXPORT_PLAIN_CREDENTIALS = new CommandlineParameter.Builder(null,
																								   "plain-credentials").description(
					"Export PLAIN credentials (if any exist)")
			.type(Boolean.class)
			.requireArguments(false)
			.defaultValue("false")
			.build();

	private final CommandlineParameter IMPORT_PLAIN_CREDENTIALS = new CommandlineParameter.Builder(null,
																								   "plain-credentials").description(
			"Import PLAIN credentials").type(Boolean.class).requireArguments(false).defaultValue("false").build();

	@Override
	public void initialize(Kernel kernel, DataSourceHelper dataSourceHelper,
						   RepositoryHolder repositoryHolder, Path rootPath) {
		repositoryHolder.registerPrepFn(AbstractAuthRepositoryWithCredentials.class, this::prepareAuthRepo);
		super.initialize(kernel, dataSourceHelper, repositoryHolder, rootPath);
	}

	@Override
	public Stream<CommandlineParameter> getExportParameters() {
		return Stream.concat(super.getExportParameters(), Stream.of(EXPORT_PLAIN_CREDENTIALS));
	}

	@Override
	public Stream<CommandlineParameter> getImportParameters() {
		return Stream.concat(super.getImportParameters(), Stream.of(IMPORT_PLAIN_CREDENTIALS));
	}

	@Override
	public void exportDomainData(String domain, Writer writer) throws Exception {
	}

	@Override
	public void exportUserData(Path userDirPath, BareJID user, Writer writer) throws Exception {
		AuthRepository authRepository = getRepository(AbstractAuthRepositoryWithCredentials.class, user.getDomain());
		try {
			Credentials credentials = authRepository.getCredentials(user, "default");
			if (credentials instanceof AuthRepository.DefaultCredentials) {
				Field entriesField = AuthRepository.DefaultCredentials.class.getDeclaredField("entries");
				entriesField.setAccessible(true);
				for (AuthRepository.DefaultCredentials.RawEntry entry : (List<AuthRepository.DefaultCredentials.RawEntry>) entriesField.get(
						credentials)) {
					String mechanism = entry.getMechanism();
					Credentials.Entry e1 = credentials.getEntryForMechanism(mechanism);
					if (e1 instanceof ScramCredentialsEntry saslEntry) {
						writeSCRAM(saslEntry, writer);
					} else if (e1 instanceof PlainCredentialsEntry plainEntry) {
						if (!isSet(EXPORT_PLAIN_CREDENTIALS)) {
							writeSCRAM(new ScramSha1CredentialsEntry(plainEntry), writer);
							writeSCRAM(new ScramSha256CredentialsEntry(plainEntry), writer);
							writeSCRAM(new ScramSha512CredentialsEntry(plainEntry), writer);
						} else {
							writer.append("<plain-credentials xmlns='tigase:xep-0227:sasl:0#plain' mechanism='")
									.append(plainEntry.getMechanism())
									.append("'>");
							writer.append("<password>")
									.append(Base64.encode(plainEntry.getPassword().getBytes(StandardCharsets.UTF_8)))
									.append("</password>");
							writer.append("</plain-credentials>");
						}
					}

				}
			}
		} catch (UserNotFoundException ex) {
			log.log(Level.FINEST, "No credentials for user " + user);
		}
	}

	protected void writeSCRAM(ScramCredentialsEntry saslEntry, Writer writer) throws Exception {
		writer.append("<scram-credentials xmlns='urn:xmpp:pie:0#scram' mechanism='")
				.append(saslEntry.getMechanism())
				.append("'>");
		writer.append("<iter-count>").append(String.valueOf(saslEntry.getIterations())).append("</iter-count>");
		writer.append("<salt>").append(Base64.encode(saslEntry.getSalt())).append("</salt>");
		writer.append("<server-key>").append(Base64.encode(saslEntry.getServerKey())).append("</server-key>");
		writer.append("<stored-key>").append(Base64.encode(saslEntry.getStoredKey())).append("</stored-key>");
		writer.append("</scram-credentials>");
	}

	protected AbstractAuthRepositoryWithCredentials prepareAuthRepo(AbstractAuthRepositoryWithCredentials authRepo) {
		CredentialsDecoderBean decoder = new CredentialsDecoderBean();
		try {
			Field f = CredentialsDecoderBean.class.getDeclaredField("decoders");
			f.setAccessible(true);
			List<Credentials.Decoder> decoders = ClassUtil.getClassesImplementing(Credentials.Decoder.class)
					.stream()
					.map(clazz -> {
						try {
							Credentials.Decoder d = clazz.getConstructor().newInstance();
							Bean bean = d.getClass().getAnnotation(Bean.class);
							if (bean != null) {
								Class x = d.getClass();
								while (x != null) {
									try {
										Field f1 = x.getDeclaredField("name");
										f1.setAccessible(true);
										f1.set(d, bean.name());
										x = null;
									} catch (NoSuchFieldException ex) {
										x = x.getSuperclass();
									}
								}
							}
							return d;
						} catch (Throwable ex) {
							log.log(Level.WARNING,
													  "failed to initialize credentials decoder " + clazz, ex);
							return null;
						}
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			System.out.println(decoders);
			f.set(decoder, decoders);
			authRepo.setCredentialsCodecs(null, decoder);
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
		return authRepo;
	}

	@Override
	public ImporterExtension startImportUserData(BareJID userJid, String name,
												 Map<String, String> attrs) throws Exception {
		return switch (name) {
			case "scram-credentials" -> {
				if ("urn:xmpp:pie:0#scram".equals(attrs.get("xmlns"))) {
					yield new SCRAMAuthImportExtension(
							getRepository(AbstractAuthRepositoryWithCredentials.class, userJid.getDomain()), userJid,
							attrs.get("mechanism"));
				}
				yield null;
			}
			case "plain-credentials" -> {
				if ("tigase:xep-0227:sasl:0#plain".equals(attrs.get("xmlns"))) {
					yield new PlainAuthImportExtension(
							getRepository(AbstractAuthRepositoryWithCredentials.class, userJid.getDomain()), userJid,
							attrs.get("mechanism"), isSet(IMPORT_PLAIN_CREDENTIALS));
				}
				yield null;
			}
			default -> null;
		};
	}

	public abstract static class AuthImportExtension
			extends AbstractImporterExtension {

		protected final AuthRepository authRepository;
		private final BareJID user;
		private final String mechanism;

		protected AuthImportExtension(AuthRepository authRepository, BareJID user, String mechanism) {
			this.authRepository = authRepository;
			this.user = user;
			this.mechanism = mechanism;
		}

		protected void save(String data) throws Exception {
			save(mechanism, data);
		}
		protected void save(String mechanism, String data) throws Exception {
			log.finest("importing user " + user + " credentials for " + mechanism + "...");
			authRepository.updateCredential(user, "default", mechanism, data);
		}
	}

	public static class SCRAMAuthImportExtension
			extends AuthImportExtension {

		private byte[] salt;
		private int iterations;
		private byte[] storedKey;
		private byte[] serverKey;

		public SCRAMAuthImportExtension(AuthRepository authRepository, BareJID user, String mechanism) {
			super(authRepository, user, mechanism);
		}

		@Override
		public boolean handleElement(Element element) throws Exception {
			return switch (element.getName()) {
				case "iter-count" -> {
					iterations = Integer.parseInt(element.getCData());
					yield true;
				}
				case "salt" -> {
					salt = Base64.decode(element.getCData());
					yield true;
				}
				case "server-key" -> {
					serverKey = Base64.decode(element.getCData());
					yield true;
				}
				case "stored-key" -> {
					storedKey = Base64.decode(element.getCData());
					yield true;
				}
				default -> false;
			};
		}

		@Override
		public void close() throws Exception {
			if (iterations <= 0) {
				throw new InvalidParameterException("Iterations cannot be less or equal 0!");
			}
			if (salt == null) {
				throw new InvalidParameterException("Salt cannot be null!");
			}
			if (serverKey == null) {
				throw new InvalidParameterException("ServerKey cannot be null!");
			}
			if (storedKey == null) {
				throw new InvalidParameterException("StoredKey cannot be null!");
			}
			save(ScramCredentialsEntry.Encoder.encode(salt, iterations, storedKey, serverKey));
			super.close();
		}
	}

	public static class PlainAuthImportExtension
			extends AuthImportExtension {

		private String password = null;
		private boolean importPLAIN;

		protected PlainAuthImportExtension(AuthRepository authRepository, BareJID user, String mechanism, boolean importPLAIN) {
			super(authRepository, user, mechanism);
		}

		@Override
		public boolean handleElement(Element element) throws Exception {
			if ("password".equals(element.getName())) {
				password = new String(Base64.decode(element.getCData()), StandardCharsets.UTF_8);
				return true;
			}
			return false;
		}

		@Override
		public void close() throws Exception {
			if (importPLAIN) {
				save(password);
			} else {
				PlainCredentialsEntry plainEntry = new PlainCredentialsEntry(password);
				save("SCRAM-SHA-1", ScramCredentialsEntry.Encoder.encode(new ScramSha1CredentialsEntry(plainEntry)));
				save("SCRAM-SHA-256", ScramCredentialsEntry.Encoder.encode(new ScramSha256CredentialsEntry(plainEntry)));
				save("SCRAM-SHA-512", ScramCredentialsEntry.Encoder.encode(new ScramSha512CredentialsEntry(plainEntry)));
			}
			super.close();
		}
	}
}
