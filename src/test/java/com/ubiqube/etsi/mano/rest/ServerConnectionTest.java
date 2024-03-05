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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ubiqube.etsi.mano.service.auth.model.ServerConnection;
import com.ubiqube.etsi.mano.service.auth.model.ServerConnection.ServerConnectionBuilder;

@SuppressWarnings("static-method")
class ServerConnectionTest {

	@Test
	void testName() throws Exception {
		final ServerConnectionBuilder sc = ServerConnection.serverBuilder()
				.authentification(null)
				.id(UUID.randomUUID())
				.ignoreSsl(false)
				.name("name")
				.serverType(null)
				.tlsCert("")
				.tupleVersion(123)
				.url(URI.create("http://localhost/"))
				.version("123");
		assertNotNull(sc.toString());
		final ServerConnection scObj = sc.build();
		scObj.setAuthentification(null);
		scObj.setId(null);
		scObj.setIgnoreSsl(false);
		scObj.setName(null);
		scObj.setServerType(null);
		scObj.setTlsCert(null);
		scObj.setTupleVersion(0);
		scObj.setUrl(URI.create("http://localhost/"));
		scObj.setVersion(null);
	}
}
