/**
 *     Copyright (C) 2019-2024 Ubiqube.
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
 *     along with this program.  If not, see https://www.gnu.org/licenses/.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.ubiqube.etsi.mano.repository.ByteArrayResource;
import com.ubiqube.etsi.mano.repository.ManoResource;
import com.ubiqube.etsi.mano.service.auth.model.AuthentificationInformations;
import com.ubiqube.etsi.mano.service.auth.model.ServerConnection;
import com.ubiqube.etsi.mano.service.rest.FluxRest;
import com.ubiqube.etsi.mano.service.rest.ProblemDetailException;
import com.ubiqube.etsi.mano.service.rest.RestException;

@SuppressWarnings("static-method")
@WireMockTest
class FluxRestTest {
	/** Logger. */
	private static final Logger LOG = LoggerFactory.getLogger(FluxRestTest.class);

	@Test
	void testGetRequestReturnsExpectedJsonResponse(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse()
				.withBody("{\"key\": \"value\", \"array\": [1, 2, 3], \"nested\": {\"innerKey\": \"innerValue\"}}")
				.withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final String res = fr.get(URI.create(uri), String.class, null);
		assertNotNull(res);
		assertEquals("{\"key\": \"value\", \"array\": [1, 2, 3], \"nested\": {\"innerKey\": \"innerValue\"}}", res);
	}

	@Test
	void testPostWithReturn(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(post(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		LOG.info("Sending POST request to URI: {}", uri);
		final ResponseEntity<String> res = fr.postWithReturn(URI.create(uri), "", String.class, "2.3.4");
		assertNotNull(res);
		LOG.info("Received response body: {}", res.getBody());
	}

	@Test
	void testDeleteWithReturn(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(delete(urlPathMatching("/api/v1/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		assertFalse(srv.isIgnoreSsl(), "SSL verification should not be ignored for security reasons.");
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/api/v1/test001";
		LOG.info("Request URI: {}", uri);
		final ResponseEntity<String> res = fr.deleteWithReturn(URI.create(uri), String.class, "2.3.4");
		assertNotNull(res);
	}

	@Test
	void testDoDownload(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse()
				.withStatus(200)
				.withBody("Download bianry")));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		LOG.info("{}", uri);
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
	void testDownload2(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final Path path = Paths.get("/tmp/test");
		fr.download(URI.create(uri), path, "2.3.4");
		assertNotNull("");
	}

	@Test
	void testPatch(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(patch(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		LOG.info("{}", uri);
		fr.patch(URI.create(uri), String.class, null, Map.of(), "2.3.4");
		assertNotNull("");
	}

	@Test
	void testPatchIfMatch(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(patch(urlPathMatching("/test001")).willReturn(aResponse().withStatus(200)));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		LOG.info("{}", uri);
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
		final Path path = Paths.get("/tmp/test");
		createFile(path);
		final MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add("Version", "0.0.1");
		fr.upload(URI.create(uri), path, "application/json", headers);
		assertTrue(true);
	}

	@Test
	void testGetRequestWithValidResponse(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get(urlPathMatching("/test001"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("Expected response content")));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final ParameterizedTypeReference<String> ptr = new ParameterizedTypeReference<>() {
			//
		};
		final String response = fr.get(URI.create(uri), ptr, null);
		// Assert the response body
		assertEquals("Expected response content", response);

		// Additional assertions
		// Check if the response is not null
		assertNotNull(response, "Response should not be null");

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

	@Test
	void testPostError(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(post(urlPathMatching("/test001")).willReturn(aResponse()
				.withStatus(404)
				.withBody("Not a json body")));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final URI ur = URI.create(uri);
		final RestException e = assertThrows(RestException.class, () -> fr.post(ur, "{}", String.class, "1.2.3"));
		assertNotNull(e);
		assertEquals("Not a json body", e.getMessage());
		assertTrue(true);
	}

	@Test
	void testPostErrorJson(final WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(post(urlPathMatching("/test001")).willReturn(aResponse()
				.withStatus(404)
				.withHeader("Content-Type", "application/problem+json;charset=UTF-8")
				.withBody("""
						{
						  "type": "about:blank",
						  "status": 401,
						  "detail": "Unauthorized.",
						  "instance": "http://6be517c93979"
						}
						""")));
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final String uri = wmRuntimeInfo.getHttpBaseUrl() + "/test001";
		final URI ur = URI.create(uri);
		final ProblemDetailException e = assertThrows(ProblemDetailException.class,
				() -> fr.post(ur, "{}", String.class, "1.2.3"));
		assertEquals("Unauthorized.", e.getMessage());
		final ProblemDetail pd = e.getProblemDetail();
		assertNotNull(pd);
		assertEquals(401, pd.getStatus(), "Status should be 401");
		assertEquals(URI.create("about:blank"), pd.getType(), "Type should be 'about:blank'");
		assertEquals("Unauthorized.", pd.getDetail(), "Detail should be 'Unauthorized.'");
		assertEquals(URI.create("http://6be517c93979"), pd.getInstance(), "Instance should match the expected URI");

	}

	private static ServerConnection createServer(final WireMockRuntimeInfo wmRuntimeInfo) {
		final AuthentificationInformations auth = null;
		return ServerConnection.serverBuilder()
				.authentification(auth)
				.url(URI.create(wmRuntimeInfo.getHttpBaseUrl()))
				.build();
	}

	private void createFile(final Path path) {
		try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
			fos.write("test".getBytes());
		} catch (final IOException e) {
			throw new RestException(e);
		}
	}

	@Test
	void TestUriBuilder(final WireMockRuntimeInfo wmRuntimeInfo) {
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final UriComponentsBuilder ub = fr.uriBuilder();
		assertNotNull(ub);
		assertEquals(wmRuntimeInfo.getHttpBaseUrl(), ub.toUriString());
	}

	@Test
	void testGetWebClientBuilder(final WireMockRuntimeInfo wmRuntimeInfo) {
		final ServerConnection srv = createServer(wmRuntimeInfo);
		final FluxRest fr = new FluxRest(srv);
		final WebClient res = fr.getWebClient();
		assertNotNull(res);
		final Builder builder = fr.getWebClientBuilder();
		assertNotNull(builder);
	}
}
