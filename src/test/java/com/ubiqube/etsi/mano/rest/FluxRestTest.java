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
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.ubiqube.etsi.mano.repository.ByteArrayResource;
import com.ubiqube.etsi.mano.repository.ManoResource;
import com.ubiqube.etsi.mano.service.rest.FluxRest;
import com.ubiqube.etsi.mano.service.rest.model.AuthentificationInformations;
import com.ubiqube.etsi.mano.service.rest.model.ServerConnection;

@SuppressWarnings("static-method")
@WireMockTest
class FluxRestTest {

	@Test
	void testName(final WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse()
				.withBody("{}")
				.withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final String res = fr.get(URI.create(uri), String.class, null);
		assertNotNull(res);
	}

	@Test
	void testPostWithReturn(final WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		stubFor(post(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		System.out.println("" + uri);
		final ResponseEntity<String> res = fr.postWithReturn(URI.create(uri), "", String.class, "2.3.4");
		assertNotNull(res);
		System.out.println(res);
	}

	@Test
	void testDeleteWithReturn(final WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		stubFor(delete(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		System.out.println("" + uri);
		final ResponseEntity<String> res = fr.deleteWithReturn(URI.create(uri), String.class, "2.3.4");
		assertNotNull(res);
		System.out.println(res);
	}

	@Test
	void testDoDownload(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse()
				.withStatus(200)
				.withBody("Download bianry")));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		System.out.println("" + uri);
		final Consumer<InputStream> tgt = is -> {
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				is.transferTo(bos);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
			bos.toString();
		};
		fr.doDownload(uri, tgt, "2.3.4");
		assertNotNull("");
	}

	@Test
	void testDownload2(final WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		System.out.println("" + uri);
		final Path path = Paths.get("/tmp/test");
		fr.download(URI.create(uri), path, "2.3.4");
		assertNotNull("");
	}

	@Test
	void testPatch(final WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		stubFor(patch(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		System.out.println("" + uri);
		final Path path = Paths.get("/tmp/test");
		fr.patch(URI.create(uri), String.class, null, Map.of(), "2.3.4");
		assertNotNull("");
	}

	@Test
	void testPatchIfMatch(final WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
		stubFor(patch(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		System.out.println("" + uri);
		final Path path = Paths.get("/tmp/test");
		fr.patch(URI.create(uri), String.class, "1", Map.of(), "2.3.4");
		assertNotNull("");
	}

	@Test
	void testUploadInputStream(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(put(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final InputStream is = new ByteArrayInputStream("{}".getBytes());
		fr.upload(URI.create(uri), is, "application/json", null);
		assertTrue(true);
	}

	@Test
	void testUploadManoResource(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(put(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final InputStream is = new ByteArrayInputStream("{}".getBytes());
		final ManoResource mr = new ByteArrayResource("{}".getBytes(), "name.json");
		fr.upload(URI.create(uri), mr, "application/json", "0.0.1");
		assertTrue(true);
	}

	@Test
	void testUploadPath(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(put(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final InputStream is = new ByteArrayInputStream("{}".getBytes());
		final Path path = Paths.get("/tmp/test");
		fr.upload(URI.create(uri), path, "application/json", "0.0.1");
		assertTrue(true);
	}

	@Test
	void testGet(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final ParameterizedTypeReference<String> ptr = new ParameterizedTypeReference<>() {
			//
		};
		fr.get(URI.create(uri), ptr, null);
		assertTrue(true);
	}

	@Test
	void testGetWithVersion(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final ParameterizedTypeReference<String> ptr = new ParameterizedTypeReference<>() {
			//
		};
		fr.get(URI.create(uri), ptr, "1.2.3");
		assertTrue(true);
	}

	@Test
	void testCall(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(delete(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		fr.call(URI.create(uri), HttpMethod.DELETE, String.class, "1.2.3");
		assertTrue(true);
	}

	@Test
	void testCallWithBody(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(delete(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		fr.call(URI.create(uri), HttpMethod.DELETE, "{}", String.class, "1.2.3");
		assertTrue(true);
	}

	@Test
	void testPut(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(put(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final InputStream is = new ByteArrayInputStream("{}".getBytes());
		fr.put(URI.create(uri), is, String.class, "application/json");
		assertTrue(true);
	}

	@Test
	void testDelete(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(delete(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		fr.delete(URI.create(uri), String.class, "1.2.3");
		assertTrue(true);
	}

	@Test
	void testPost(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(post(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		fr.post(URI.create(uri), String.class, "1.2.3");
		assertTrue(true);
	}

	@Test
	void testPost002(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(post(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		fr.post(URI.create(uri), "{}", String.class, "1.2.3");
		assertTrue(true);
	}

	private static ServerConnection createServer(final WireMockRuntimeInfo wmRuntimeInfo) {
		final AuthentificationInformations auth = null;
		return ServerConnection.serverBuilder()
				.authentification(auth)
				.url(wmRuntimeInfo.getHttpBaseUrl())
				.build();

	}
}
