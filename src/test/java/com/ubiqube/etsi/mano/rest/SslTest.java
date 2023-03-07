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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.ubiqube.etsi.mano.service.rest.FluxRest;
import com.ubiqube.etsi.mano.service.rest.RestException;
import com.ubiqube.etsi.mano.service.rest.model.AuthentificationInformations;
import com.ubiqube.etsi.mano.service.rest.model.ServerConnection;

import jakarta.annotation.Nonnull;

@SuppressWarnings("static-method")
@WireMockTest(httpsEnabled = true)
class SslTest {
	@RegisterExtension
	static WireMockExtension wm1 = WireMockExtension.newInstance()
			.options(wireMockConfig().httpDisabled(true).httpsPort(8443))
			.build();

	@Test
	void testFail() throws Exception {
		final WireMockRuntimeInfo wmRuntimeInfo = wm1.getRuntimeInfo();
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpsBaseUrl() + "/test001";
		final URI fullUri = URI.create(uri);
		assertThrows(RestException.class, () -> fr.get(fullUri, String.class, null));
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
	private static ServerConnection createServer(final WireMockRuntimeInfo wmRuntimeInfo) {
		final AuthentificationInformations auth = null;
		return ServerConnection.serverBuilder()
				.authentification(auth)
				.ignoreSsl(true)
				.url(wmRuntimeInfo.getHttpsBaseUrl())
				.build();
	}

}
