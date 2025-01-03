/**
 * Copyright (C) 2019-2025 Ubiqube.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.ubiqube.etsi.mano.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.ubiqube.etsi.mano.service.auth.model.AuthParamBasic;
import com.ubiqube.etsi.mano.service.auth.model.AuthType;
import com.ubiqube.etsi.mano.service.auth.model.AuthentificationInformations;
import com.ubiqube.etsi.mano.service.auth.model.ServerConnection;
import com.ubiqube.etsi.mano.service.rest.FluxRest;

@SuppressWarnings("static-method")
@WireMockTest
class BasicAuthTest {
	/** Logger. */
	private static final Logger LOG = LoggerFactory.getLogger(BasicAuthTest.class);

	@Test
	void testName(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse()
				.withStatus(200)
				.withBody("{}")));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		LOG.info("{}", uri);
		final String res = fr.get(URI.create(uri), String.class, "1.2.3");
		assertEquals("{}", res);
	}

	private static ServerConnection createServer(final WireMockRuntimeInfo wmRuntimeInfo) {
		final AuthentificationInformations auth = AuthentificationInformations.builder()
				.authParamBasic(AuthParamBasic.builder()
						.userName("user")
						.password("password")
						.build())
				.authType(List.of(AuthType.BASIC))
				.build();
		return ServerConnection.serverBuilder()
				.authentification(auth)
				.url(URI.create(wmRuntimeInfo.getHttpBaseUrl()))
				.build();
	}

}
