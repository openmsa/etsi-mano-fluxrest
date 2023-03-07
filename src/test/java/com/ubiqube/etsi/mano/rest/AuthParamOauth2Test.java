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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.ubiqube.etsi.mano.service.rest.model.AuthParamOauth2;
import com.ubiqube.etsi.mano.service.rest.model.AuthParamOauth2.AuthParamOauth2Builder;

@SuppressWarnings("static-method")
class AuthParamOauth2Test {

	@Test
	void testName() throws Exception {
		final AuthParamOauth2Builder apob = AuthParamOauth2.builder()
				.clientId(null)
				.clientSecret(null)
				.grantType(null)
				.o2AuthTlsCert(null)
				.o2IgnoreSsl(null)
				.o2Password(null)
				.o2Username(null)
				.tokenEndpoint(null);
		assertNotNull(apob.toString());
		final AuthParamOauth2 apo = apob.build();
		apo.setClientId(null);
		apo.setClientSecret(null);
		apo.setGrantType(null);
		apo.setO2AuthTlsCert(null);
		apo.setO2IgnoreSsl(null);
		apo.setO2Password(null);
		apo.setO2Username(null);
		apo.setTokenEndpoint(null);
	}
}
