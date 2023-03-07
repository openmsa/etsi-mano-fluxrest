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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.ubiqube.etsi.mano.service.rest.FluxRest;
import com.ubiqube.etsi.mano.service.rest.model.AuthParamOauth2;
import com.ubiqube.etsi.mano.service.rest.model.AuthType;
import com.ubiqube.etsi.mano.service.rest.model.AuthentificationInformations;
import com.ubiqube.etsi.mano.service.rest.model.OAuth2GrantType;
import com.ubiqube.etsi.mano.service.rest.model.ServerConnection;

@SuppressWarnings("static-method")
@WireMockTest
class OAuth2Test {

	@Test
	void testName(final WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse()
				.withStatus(200)
				.withBody("{}")));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		System.out.println("" + uri);
		final ResponseEntity<String> res = fr.getWithReturn(URI.create(uri), String.class, null);
		assertNotNull(res);
		assertEquals("{}", res.getBody());
	}

	private static ServerConnection createServer(final WireMockRuntimeInfo wmRuntimeInfo) {
		final AuthentificationInformations auth = AuthentificationInformations.builder()
				.authParamOauth2(AuthParamOauth2.builder()
						.clientId("mano-vnfm")
						.clientSecret("50e312ab-9f12-481e-b6a8-c23e0de21628")
						.grantType(OAuth2GrantType.CLIENT_CREDENTIAL)
						.o2IgnoreSsl(true)
						.o2Username("user")
						.o2Password("pass")
						.tokenEndpoint("http://mano-auth/auth/realms/mano-realm/protocol/openid-connect/token")
						.build())
				.authType(List.of(AuthType.BASIC))
				.build();
		return ServerConnection.serverBuilder()
				.authentification(auth)
				.url(wmRuntimeInfo.getHttpBaseUrl())
				.build();
	}

}
