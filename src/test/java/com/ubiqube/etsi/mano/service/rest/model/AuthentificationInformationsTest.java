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
package com.ubiqube.etsi.mano.service.rest.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ubiqube.etsi.mano.service.rest.model.AuthentificationInformations.AuthentificationInformationsBuilder;

@SuppressWarnings("static-method")
class AuthentificationInformationsTest {

	@Test
	void test() {
		final AuthentificationInformations ai = new AuthentificationInformations();
		ai.setAuthParamBasic(null);
		ai.setAuthParamOauth2(null);
		ai.setAuthTlsCert(null);
		ai.setAuthType(List.of());
		ai.getAuthTlsCert();
		ai.getAuthType();
		final AuthentificationInformationsBuilder aib = AuthentificationInformations.builder();
		aib.toString();
		assertTrue(true);
	}

}
