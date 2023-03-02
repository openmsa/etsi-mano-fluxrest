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

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.logging.ByteBufFormat;
import io.netty.handler.logging.LoggingHandler;

/**
 *
 * @author olivier
 *
 */
public class ManoLoggingHandler extends LoggingHandler {

	private static final String READ = "READ";
	private static final String WRITE = "WRITE";
	private static final Logger LOG = LoggerFactory.getLogger(ManoLoggingHandler.class);

	public ManoLoggingHandler() {
		super(ByteBufFormat.SIMPLE);
	}

	@Override
	public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
		LOG.trace("write class {} ", msg.getClass().getName());
		if (logger.isEnabled(internalLevel)) {
			if (msg instanceof final DefaultHttpRequest dhr) {
				dumpHeaders(dhr.headers(), ctx, WRITE);
			}
			if (msg instanceof final DefaultFullHttpRequest dfhr) {
				logger.log(internalLevel, format(ctx, WRITE, dump(ctx, WRITE, dfhr.content())));
			} else {
				logger.log(internalLevel, format(ctx, WRITE, msg));
			}
		}
		ctx.write(msg, promise);
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
		LOG.trace("read class {} ", msg.getClass().getName());
		if (logger.isEnabled(internalLevel)) {
			if (msg instanceof final DefaultHttpResponse dhr) {
				dumpHeaders(dhr.headers(), ctx, READ);
			} else {
				logger.log(internalLevel, format(ctx, READ, msg));
			}
		}
		ctx.fireChannelRead(msg);
	}

	private void dumpHeaders(final HttpHeaders headers, final ChannelHandlerContext ctx, final String op) {
		final StringBuilder sb = new StringBuilder("Headers: \n");
		headers.entries().forEach(x -> {
			sb.append(x.getKey()).append(": ");
			if (isValid(x.getKey())) {
				sb.append(x.getValue());
			} else {
				sb.append("\"******\"");
			}
			sb.append("\n");
		});
		logger.log(internalLevel, format(ctx, op, sb.toString()));
	}

	private static boolean isValid(final String key) {
		return !"Authorization".equalsIgnoreCase(key);
	}

	private static String dump(final ChannelHandlerContext ctx, final String eventName, final ByteBuf content) {
		final String chStr = ctx.channel().toString();
		final int length = content.readableBytes();
		if (length == 0) {
			final StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 4);
			buf.append(chStr).append(' ').append(eventName).append(": 0B");
			return buf.toString();
		}
		final int outputLength = chStr.length() + 1 + eventName.length() + 2 + 10 + 1;
		final StringBuilder buf = new StringBuilder(outputLength);
		buf.append(chStr).append(' ').append(eventName).append(": ").append(length).append('B').append("\n");
		buf.append(content.toString(Charset.defaultCharset()));
		return buf.toString();
	}

}
