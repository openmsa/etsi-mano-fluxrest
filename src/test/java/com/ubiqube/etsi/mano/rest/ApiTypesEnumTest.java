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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.ubiqube.etsi.mano.service.rest.model.ApiTypesEnum;

@SuppressWarnings("static-method")
class ApiTypesEnumTest {

	@Test
	void testName() throws Exception {
		final ApiTypesEnum obj = ApiTypesEnum.fromValue("SOL003");
		assertNotNull(obj);
		assertNotNull(obj.toString());
		assertNotNull(obj.value());
	}

	@Test
	void testNull() throws Exception {
		final ApiTypesEnum obj = ApiTypesEnum.fromValue("SOL");
		assertNull(obj);
	}

	@Test
	void testNullInput() throws Exception {
		final ApiTypesEnum obj = ApiTypesEnum.fromValue(null);
		assertNull(obj);
	}

}
