/**
 *     Copyright (C) 2019-2023 Ubiqube.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.ubiqube.etsi.mano.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcContentSignerBuilder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.ubiqube.etsi.mano.service.rest.FluxRest;
import com.ubiqube.etsi.mano.service.rest.model.AuthType;
import com.ubiqube.etsi.mano.service.rest.model.AuthentificationInformations;
import com.ubiqube.etsi.mano.service.rest.model.ServerConnection;

import jakarta.annotation.Nonnull;

@SuppressWarnings("static-method")
class TlsTest {
	private static final String CLIENT_KEYSTORE = "/tmp/test-unit.ks";

	@RegisterExtension
	static WireMockExtension wm1 = WireMockExtension.newInstance()
			.options(wireMockConfig()
					.httpDisabled(true)
					.httpsPort(8443)
					.keystorePath(CLIENT_KEYSTORE)
					.keystorePassword("password"))
			.build();

	private static KeyPair caKey;

	private static X509Certificate cert;
	static {
		try {
			final KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(null);
			createCA(ks);
		} catch (InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | SignatureException | OperatorCreationException | IOException e) {
			e.printStackTrace();
		}
	}

	private static void createCA(final KeyStore ks) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException, NoSuchProviderException, SignatureException, OperatorCreationException, IOException {
		final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(4096);
		caKey = kpg.generateKeyPair();
		cert = generateCert(caKey, "C=FR, O=Ubiqube, OU=TestUnit, CN=localhost");
		ks.setKeyEntry("privatekey", caKey.getPrivate(), "password".toCharArray(),
				new java.security.cert.Certificate[] { cert });
		ks.setCertificateEntry("cert", cert);
		try (final FileOutputStream keystoreOutputStream = new FileOutputStream(CLIENT_KEYSTORE)) {
			ks.store(keystoreOutputStream, "password".toCharArray());
		}
		ks.setCertificateEntry("ca", cert);
	}

	private static X509Certificate generateCert(final KeyPair keyPair, final String issuer) throws IOException, CertificateException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, OperatorCreationException {
		final X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(new X500Name(issuer),
				BigInteger.ONE, new Date(System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 30)),
				new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)), new X500Name(issuer),
				SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

		final GeneralNames subjectAltNames = new GeneralNames(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
		certificateBuilder.addExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName, false,
				subjectAltNames);

		final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder()
				.find("sha256WithRSAEncryption");
		final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
		final BcContentSignerBuilder signerBuilder = new BcRSAContentSignerBuilder(sigAlgId, digAlgId);
		final AsymmetricKeyParameter keyp = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
		final ContentSigner signer = signerBuilder.build(keyp);
		final X509CertificateHolder x509CertificateHolder = certificateBuilder.build(signer);

		final X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(x509CertificateHolder);
		certificate.checkValidity(new Date());
		certificate.verify(keyPair.getPublic());
		return certificate;
	}

	@Test
	void testFail() throws Exception {
		final WireMockRuntimeInfo wmRuntimeInfo = wm1.getRuntimeInfo();
		wm1.stubFor(get(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		srv.setTlsCert(null);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpsBaseUrl() + "/test001";
		final URI fullUri = URI.create(uri);
		assertThrows(WebClientRequestException.class, () -> fr.get(fullUri, String.class, null));
	}

	@Test
	void testOk() throws Exception {
		final WireMockRuntimeInfo wmRuntimeInfo = wm1.getRuntimeInfo();
		wm1.stubFor(get(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200).withBody("{}")));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpsBaseUrl() + "/test001";
		final String str = fr.get(URI.create(uri), String.class, null);
		assertEquals("{}", str);
	}

	@Nonnull
	private ServerConnection createServer(final WireMockRuntimeInfo wmRuntimeInfo) {
		final String certTxt = pemEncode(cert);
		System.out.println(certTxt);
		final AuthentificationInformations auth = AuthentificationInformations.builder()
				.authTlsCert(certTxt)
				.authType(List.of(AuthType.fromValue("TLS_CERT")))
				.build();
		return ServerConnection.serverBuilder()
				.authentification(auth)
				.tlsCert(certTxt)
				.url(wmRuntimeInfo.getHttpsBaseUrl())
				.build();
	}

	private static String pemEncode(final Object key) {
		try (final Writer out = new StringWriter();
				final PemWriter pw = new PemWriter(out);) {
			pw.writeObject(new JcaMiscPEMGenerator(key));
			pw.flush();
			return out.toString();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

}
