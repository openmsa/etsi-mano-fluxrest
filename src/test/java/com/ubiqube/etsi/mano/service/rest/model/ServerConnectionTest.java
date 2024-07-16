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
package com.ubiqube.etsi.mano.service.rest.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;

import org.junit.jupiter.api.Test;

import com.ubiqube.etsi.mano.service.auth.model.ServerConnection;

@SuppressWarnings("static-method")
class ServerConnectionTest {

	@Test
	void test() {
		ServerConnection sc = new ServerConnection();
		sc.getId();
		sc.getName();
		sc.getServerType();
		sc.getTupleVersion();
		sc.getVersion();
		sc = new ServerConnection(null, URI.create("http://localhost/"));
		assertNotNull(sc);
	}

}
