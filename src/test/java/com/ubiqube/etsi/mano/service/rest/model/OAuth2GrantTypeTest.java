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
package com.ubiqube.etsi.mano.service.rest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.ubiqube.etsi.mano.service.auth.model.OAuth2GrantType;

@SuppressWarnings("static-method")
class OAuth2GrantTypeTest {

	@Test
	void test() {
		final OAuth2GrantType ogt = OAuth2GrantType.fromValue("client_credentials");
		assertEquals(OAuth2GrantType.CLIENT_CREDENTIAL, ogt);
		assertNotNull(ogt.toString());
		final OAuth2GrantType ogt2 = OAuth2GrantType.fromValue(null);
		assertNull(ogt2);
	}

}
