/**
 * Copyright (C) 2019-2023 Ubiqube.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
module com.ubiqube.etsi.mano.service.rest {
	exports com.ubiqube.etsi.mano.service.rest;
	exports com.ubiqube.etsi.mano.service.rest.model;

	requires jakarta.annotation;
	requires jakarta.persistence;
	requires com.fasterxml.jackson.annotation;
	requires lombok;
	requires org.slf4j;
	requires spring.core;
	requires spring.security.oauth2.client;
	requires spring.security.oauth2.core;
	requires spring.web;
	requires transitive spring.webflux;
	requires org.reactivestreams;
	requires reactor.core;
	requires reactor.netty;
	requires reactor.netty.core;
	requires reactor.netty.http;
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.codec.http;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.resolver;
	requires io.netty.transport;
	requires com.ubiqube.etsi.mano.repository;
}