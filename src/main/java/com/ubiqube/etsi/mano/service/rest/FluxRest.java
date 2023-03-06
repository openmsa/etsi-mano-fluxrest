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
package com.ubiqube.etsi.mano.service.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.UriComponentsBuilder;

import com.ubiqube.etsi.mano.repository.ManoResource;
import com.ubiqube.etsi.mano.service.rest.model.AuthentificationInformations;
import com.ubiqube.etsi.mano.service.rest.model.ServerConnection;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.annotation.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.netty.http.client.HttpClient;

/**
 *
 * @author Olivier Vignaud <ovi@ubiqube.com>
 *
 */
public class FluxRest {
	private static final String VERSION = "Version";
	private static final Logger LOG = LoggerFactory.getLogger(FluxRest.class);

	private final String rootUrl;
	private final String id = UUID.randomUUID().toString();
	protected HttpClient httpClient;
	protected Builder webBuilder;

	public FluxRest(final ServerConnection server) {
		this.rootUrl = server.getUrl();
		this.httpClient = getHttpClient(server.getUrl(), server.isIgnoreSsl(), server.getTlsCert());
		this.webBuilder = applyBasicWebClientBuilder(WebClient.builder(), server);
	}

	private Builder applyBasicWebClientBuilder(final Builder wcb, final ServerConnection server) {
		final ClientHttpConnector conn = new ReactorClientHttpConnector(httpClient);
		wcb.clientConnector(conn);
		createAuthPart(wcb, server.getAuthentification());
		wcb.baseUrl(rootUrl)
				.filter(errorHandler());
		return wcb;
	}

	private static @Nullable SslContext buildSslContext(final String url, @Nullable final Boolean ignoreSsl, final @Nullable String tlsCert) {
		if (url.startsWith("http:")) {
			return null;
		}
		if ((ignoreSsl != null) && ignoreSsl) {
			return bypassAllSsl();
		}
		if (tlsCert != null) {
			return allowSslOneCert(tlsCert);
		}
		return defaultSslContext();
	}

	private static @Nullable SslContext defaultSslContext() {
		try {
			return SslContextBuilder.forClient().build();
		} catch (final SSLException e) {
			throw new RestException(e);
		}
	}

	private void createAuthPart(final Builder wcb, final @Nullable AuthentificationInformations auth) {
		if (auth == null) {
			return;
		}
		Optional.ofNullable(auth.getAuthParamBasic()).ifPresent(x -> wcb.filter(ExchangeFilterFunctions.basicAuthentication(x.getUserName(), x.getPassword())));
		Optional.ofNullable(auth.getAuthParamOauth2()).ifPresent(x -> {
			final HttpClient oAuth2httpClient = getHttpClient(x.getTokenEndpoint(), x.getO2IgnoreSsl(), x.getO2AuthTlsCert());
			final AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
					getRegistration(x.getTokenEndpoint(), x.getClientId(), x.getClientSecret(), "openid"),
					new InMemoryReactiveOAuth2AuthorizedClientService(getRegistration(x.getTokenEndpoint(), x.getClientId(), x.getClientSecret(), "openid")));
			Optional.ofNullable(x.getO2IgnoreSsl()).filter(Boolean::booleanValue).ifPresent(y -> authorizedClientManager.setAuthorizedClientProvider(getAuthorizedClientProvider(oAuth2httpClient)));
			final ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
			oauth2.setDefaultClientRegistrationId(id);
			wcb.filter(oauth2);
		});
	}

	private static ReactiveOAuth2AuthorizedClientProvider getAuthorizedClientProvider(final HttpClient httpClient) {
		final ClientHttpConnector httpConnector = new ReactorClientHttpConnector(httpClient);
		final WebClientReactiveClientCredentialsTokenResponseClient accessTokenResponseClient = new WebClientReactiveClientCredentialsTokenResponseClient();
		accessTokenResponseClient.setWebClient(WebClient.builder().clientConnector(httpConnector).build());
		return ReactiveOAuth2AuthorizedClientProviderBuilder
				.builder()
				.clientCredentials(c -> c.accessTokenResponseClient(accessTokenResponseClient)).build();
	}

	private static SslContext allowSslOneCert(final String tlsCert) {
		try {
			final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);
			trustStore.setCertificateEntry(UUID.randomUUID().toString(),
					CertificateFactory.getInstance("X.509")
							.generateCertificate(new ByteArrayInputStream(tlsCert.getBytes())));

			final TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(trustStore);

			return SslContextBuilder.forClient()
					.trustManager(trustManagerFactory)
					.build();
		} catch (final IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
			throw new RestException(e);
		}
	}

	private static SslContext bypassAllSsl() {
		try {
			return SslContextBuilder.forClient()
					.trustManager(InsecureTrustManagerFactory.INSTANCE)
					.build();
		} catch (final SSLException e) {
			throw new RestException(e);
		}
	}

	private static HttpClient getHttpClient(final String url, @Nullable final Boolean ignoreSsl, @Nullable final String tlsCert) {
		final SslContext sslContext = buildSslContext(url, ignoreSsl, tlsCert);
		HttpClient client = HttpClient.create()
				.doOnRequest((h, c) -> c.addHandlerFirst(new ManoLoggingHandler()));
		if (null != sslContext) {
			// The copy is needed, because secure return another instance of client.
			client = client.secure(t -> t.sslContext(sslContext));
		}
		return client;
	}

	private ReactiveClientRegistrationRepository getRegistration(final String tokenUri, final String clientId, final String clientSecret, final String scope) {
		final ClientRegistration registration = ClientRegistration
				.withRegistrationId(id)
				.tokenUri(tokenUri)
				.clientId(clientId)
				.clientSecret(clientSecret)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope(scope)
				.build();
		return new InMemoryReactiveClientRegistrationRepository(registration);
	}

	public final @Nullable <T> ResponseEntity<T> getWithReturn(final URI uri, final Class<T> clazz, @Nullable final String version) {
		final Map<String, String> map = Optional.ofNullable(version).map(x -> Map.of(VERSION, x)).orElseGet(Map::of);
		final Mono<ResponseEntity<T>> resp = makeBaseQuery(uri, HttpMethod.GET, null, map)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntity(clazz);
		return resp.block();
	}

	public final @Nullable <T> T get(final URI uri, final Class<T> clazz, @Nullable final String version) {
		return call(uri, HttpMethod.GET, clazz, version);
	}

	public final @Nullable <T> ResponseEntity<T> postWithReturn(final URI uri, final Object body, final Class<T> clazz, @Nullable final String version) {
		final Map<String, String> map = Optional.ofNullable(version).map(x -> Map.of(VERSION, x)).orElseGet(Map::of);
		final Mono<ResponseEntity<T>> resp = makeBaseQuery(uri, HttpMethod.POST, body, map)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntity(clazz);
		return resp.block();
	}

	public final @Nullable <T> ResponseEntity<T> deleteWithReturn(final URI uri, final Object body, @Nullable final String version) {
		final Map<String, String> map = Optional.ofNullable(version).map(x -> Map.of(VERSION, x)).orElseGet(Map::of);
		final ResponseSpec resp = makeBaseQuery(uri, HttpMethod.DELETE, body, map)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();
		return (ResponseEntity<T>) resp.toBodilessEntity().block();
	}

	public final @Nullable <T> T post(final URI uri, final Class<T> clazz, @Nullable final String version) {
		return call(uri, HttpMethod.POST, clazz, version);
	}

	public final @Nullable <T> T post(final URI uri, final Object body, final Class<T> clazz, @Nullable final String version) {
		return call(uri, HttpMethod.POST, body, clazz, version);
	}

	/**
	 *
	 * @param <T>     Return class.
	 * @param uri     URI to call.
	 * @param body    The body as an {@link InputStream}.
	 * @param clazz   Return Class.
	 * @param version MANO Version null other wise.
	 * @return
	 */
	public final @Nullable <T> T put(final URI uri, final InputStream body, final Class<T> clazz, final String contentType) {
		return innerCall(uri, HttpMethod.PUT, body, clazz, Map.of("Content-Type", contentType));
	}

	public final @Nullable <T> T delete(final URI uri, final Class<T> clazz, @Nullable final String version) {
		return call(uri, HttpMethod.DELETE, clazz, version);
	}

	public final @Nullable <T> T call(final URI uri, final HttpMethod method, final Class<T> clazz, @Nullable final String version) {
		final Map<String, String> map = Optional.ofNullable(version).map(x -> Map.of(VERSION, x)).orElseGet(Map::of);
		return innerCall(uri, method, null, clazz, map);
	}

	public final @Nullable <T> T call(final URI uri, final HttpMethod method, final Object body, final Class<T> clazz, @Nullable final String version) {
		final Map<String, String> map = Optional.ofNullable(version).map(x -> Map.of(VERSION, x)).orElseGet(Map::of);
		return innerCall(uri, method, body, clazz, map);
	}

	public @Nullable <T> T get(final URI uri, final ParameterizedTypeReference<T> myBean, final @Nullable String version) {
		final Map<String, String> map = Optional.ofNullable(version).map(x -> Map.of(VERSION, x)).orElseGet(Map::of);
		final Mono<ResponseEntity<T>> resp = makeBaseQuery(uri, HttpMethod.GET, null, map)
				.retrieve()
				.toEntity(myBean);
		if (version != null) {
			return getBlockingResult(resp, null, Map.of(VERSION, version));
		}
		return getBlockingResult(resp, null, Map.of());
	}

	public UriComponentsBuilder uriBuilder() {
		return UriComponentsBuilder.fromHttpUrl(rootUrl);
	}

	/**
	 *
	 * @param uri     URI to get the content/
	 * @param path    Path to store the temporary file.
	 * @param version Version header to add if needed, null otherwise.
	 */
	public void download(final URI uri, final Path path, final @Nullable String version) {
		final RequestHeadersSpec<?> wc = webBuilder
				.build()
				.get()
				.uri(uri)
				.accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);
		if (null != version) {
			wc.header(VERSION, version);
		}
		final Publisher<DataBuffer> dataBufferFlux = wc.retrieve()
				.bodyToFlux(DataBuffer.class);
		DataBufferUtils.write(dataBufferFlux, path, StandardOpenOption.CREATE).block();
	}

	private RequestHeadersSpec<?> makeBaseQuery(final URI uri, final HttpMethod method, final @Nullable Object requestObject, final Map<String, String> headers) {
		final RequestHeadersSpec<?> wc = webBuilder
				.build()
				.method(method)
				.uri(uri)
				.contentType(Optional.ofNullable(headers.get("Content-Type")).map(MediaType::parseMediaType).orElse(MediaType.APPLICATION_JSON));
		if (null != requestObject) {
			if (requestObject instanceof final InputStream is) {
				((RequestBodySpec) wc).body(BodyInserters.fromResource(new InputStreamResource(is)));
			} else {
				((RequestBodySpec) wc).bodyValue(requestObject);
			}
		}
		Optional.ofNullable(headers.get(VERSION)).ifPresent(x -> wc.header(VERSION, x));
		return wc;
	}

	private static ExchangeFilterFunction errorHandler() {
		return ExchangeFilterFunction.ofResponseProcessor(cr -> {
			final HttpStatusCode status = cr.statusCode();
			if (status.is5xxServerError() || status.is4xxClientError()) {
				return cr.bodyToMono(String.class).flatMap(x -> Mono.error(() -> new RestException(x)));
			}
			return Mono.just(cr);
		});
	}

	private final @Nullable <T> T innerCall(final URI uri, final HttpMethod method, final @Nullable Object requestObject, final Class<T> clazz, final Map<String, String> headers) {
		final Mono<ResponseEntity<T>> resp = makeBaseQuery(uri, method, requestObject, headers)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntity(clazz);
		return getBlockingResult(resp, clazz, headers);
	}

	private @Nullable <T> T getBlockingResult(final Mono<ResponseEntity<T>> resp, final Class<T> clazz, final Map<String, String> headers) {
		final ResponseEntity<T> resp2 = resp.block();
		if (null == resp2) {
			return null;
		}
		final Optional<URI> uri = Optional.ofNullable(resp2.getHeaders().getLocation()).filter(x -> !x.toString().isEmpty());
		if (uri.isPresent()) {
			LOG.info("Location: {}", uri);
			return innerCall(uri.get(), HttpMethod.GET, null, clazz, headers);
		}
		return resp2.getBody();
	}

	public void upload(final URI uri, final Path path, final String accept, @Nullable final String version) {
		final MultiValueMap<String, ?> multipart = fromPath(path, accept);
		upload(uri, multipart, version);
	}

	public void upload(final URI uri, final InputStream is, final String accept, final @Nullable String version) {
		final MultiValueMap<String, ?> multipart = fromInputStream(is, accept);
		upload(uri, multipart, version);
	}

	public void upload(final URI uri, final ManoResource mr, final String accept, final @Nullable String version) {
		try (InputStream is = mr.getInputStream()) {
			upload(uri, is, accept, version);
		} catch (final IOException e) {
			throw new RestException(e);
		}
	}

	public void upload(final URI uri, final MultiValueMap<String, ?> multipartData, final @Nullable String version) {
		final RequestHeadersSpec<?> wc = webBuilder
				.build()
				.put()
				.uri(uri)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData));
		if (null != version) {
			wc.header(VERSION, version);
		}
		wc.retrieve()
				.bodyToMono(String.class)
				.block();
	}

	private static MultiValueMap<String, ?> fromPath(final Path path, final String accept) {
		final MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("file", new FileSystemResource(path), MediaType.valueOf(accept));
		return builder.build();
	}

	private static MultiValueMap<String, ?> fromInputStream(final InputStream is, final String accept) {
		final MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("file", new InputStreamResource(is), MediaType.valueOf(accept));
		return builder.build();
	}

	public <T> T patch(final URI uri, final Class<T> clazz, final @Nullable String ifMatch, final Map<String, Object> patch, @Nullable final String version) {
		final Map<String, String> map = Optional.ofNullable(version).map(x -> Map.of(VERSION, x)).orElseGet(Map::of);
		final RequestHeadersSpec<?> base = makeBaseQuery(uri, HttpMethod.PATCH, patch, map);
		if (ifMatch != null) {
			base.header(HttpHeaders.IF_MATCH, ifMatch);
		}
		final Mono<ResponseEntity<T>> resp = base
				.retrieve()
				.toEntity(clazz);
		return getBlockingResult(resp, null, Map.of(VERSION, version));
	}

	public void doDownload(final String url, final Consumer<InputStream> target, @Nullable final String version) {
		final ExceptionHandler eh = new ExceptionHandler();
		try (final PipedOutputStream osPipe = new PipedOutputStream();
				final PipedInputStream isPipe = new PipedInputStream(osPipe)) {
			final Flux<DataBuffer> wc = webBuilder
					.build()
					.get()
					.uri(url)
					.accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
					.header(VERSION, version)
					.retrieve()
					.onRawStatus(i -> i != 200, exepctionFunction(osPipe))
					.bodyToFlux(DataBuffer.class);

			DataBufferUtils.write(wc, osPipe)
					.doFinally(onComplete(osPipe))
					.onErrorResume(Throwable.class, e -> {
						eh.setE(e);
						eh.setMessage(e.getMessage());
						return Mono.error(e);
					})
					.subscribe(DataBufferUtils.releaseConsumer());
			target.accept(isPipe);
			wc.blockLast();
		} catch (final IOException e) {
			LOG.error("", e);
		}
	}

	private static Consumer<SignalType> onComplete(final PipedOutputStream osPipe) {
		return s -> closePipe(osPipe);
	}

	private static Function<ClientResponse, Mono<? extends Throwable>> exepctionFunction(final PipedOutputStream osPipe) {
		return response -> {
			closePipe(osPipe);
			throw new RestException("An error occured." + response.statusCode());
		};
	}

	private static void closePipe(final OutputStream osPipe) {
		try (osPipe) {
			//
		} catch (final IOException e) {
			throw new RestException(e);
		}
	}

}
