/*
 * Copyright 2017 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.http.server.netty.types.files;

import io.micronaut.http.*;
import io.micronaut.http.server.netty.types.NettyCustomizableResponseTypeHandler;
import io.micronaut.http.server.netty.types.NettyFileCustomizableResponseType;
import io.micronaut.http.server.types.files.StreamedFile;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.micronaut.http.server.netty.NettyHttpResponse;
import io.micronaut.http.server.netty.async.DefaultCloseHandler;
import io.micronaut.http.server.types.CustomizableResponseTypeException;
import io.micronaut.http.server.types.files.SystemFileCustomizableResponseType;

import javax.inject.Singleton;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;

/**
 * Responsible for writing files out to the response in Netty
 *
 * @author James Kleeh
 * @since 1.0
 */
@Singleton
public class FileTypeHandler implements NettyCustomizableResponseTypeHandler<Object> {

    private final FileTypeHandlerConfiguration configuration;
    private static final Class<?>[] supportedTypes = new Class[] {File.class, SystemFileCustomizableResponseType.class, StreamedFile.class, NettyFileCustomizableResponseType.class};

    public FileTypeHandler(FileTypeHandlerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void handle(Object obj, HttpRequest<?> request, NettyHttpResponse<?> response, ChannelHandlerContext context) {

        NettyFileCustomizableResponseType type;
        if (obj instanceof File) {
            type = new NettySystemFileCustomizableResponseType((File) obj);
        } else if (obj instanceof NettyFileCustomizableResponseType) {
            type = (NettyFileCustomizableResponseType) obj;
        } else if (obj instanceof SystemFileCustomizableResponseType) {
            type = new NettySystemFileCustomizableResponseType((SystemFileCustomizableResponseType) obj);
        } else if (obj instanceof StreamedFile) {
            type = new NettyStreamedFileCustomizableResponseType((StreamedFile) obj);
        } else {
            throw new CustomizableResponseTypeException("FileTypeHandler only supports File or FileCustomizableResponseType types");
        }

        long lastModified = type.getLastModified();

        // Cache Validation
        ZonedDateTime ifModifiedSince = request.getHeaders().getDate(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null) {

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSince.toEpochSecond();
            long fileLastModifiedSeconds = lastModified / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                FullHttpResponse nettyResponse = notModified();
                context.writeAndFlush(nettyResponse)
                        .addListener(new DefaultCloseHandler(context, request, response.code()));
                return;
            }
        }

        response.header(HttpHeaders.CONTENT_TYPE, getMediaType(type.getName()));
        setDateAndCacheHeaders(response, lastModified);
        if (request.getHeaders().isKeepAlive()) {
            response.header(HttpHeaders.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        type.process(response);

        type.write(request, response, context);
    }

    @Override
    public boolean supports(Class<?> type) {
        return Arrays.stream(supportedTypes)
                .anyMatch((aClass -> aClass.isAssignableFrom(type)));
    }

    protected MediaType getMediaType(String filename) {
        String extension = getExtension(filename);
        Optional<MediaType> mediaType = MediaType.forExtension(extension);
        return mediaType.orElse(MediaType.TEXT_PLAIN_TYPE);

    }

    protected void setDateAndCacheHeaders(MutableHttpResponse response, long lastModified) {
        // Date header
        MutableHttpHeaders headers = response.getHeaders();
        LocalDateTime now = LocalDateTime.now();
        headers.date(now);

        // Add cache headers
        LocalDateTime cacheSeconds = now.plus(configuration.getCacheSeconds(), ChronoUnit.SECONDS);
        headers.expires(cacheSeconds);

        response.header(HttpHeaders.CACHE_CONTROL, "private, max-age=" + configuration.getCacheSeconds());
        headers.lastModified(lastModified);
    }

    protected void setDateHeader(MutableHttpResponse response) {
        MutableHttpHeaders headers = response.getHeaders();
        LocalDateTime now = LocalDateTime.now();
        headers.date(now);
    }

    private FullHttpResponse notModified() {
        NettyHttpResponse response = (NettyHttpResponse)HttpResponse.notModified();
        setDateHeader(response);
        return response.getNativeResponse();
    }

    private String getExtension(String filename) {
        int extensionPos = filename.lastIndexOf('.');
        int lastUnixPos = filename.lastIndexOf('/');
        int lastWindowsPos = filename.lastIndexOf('\\');
        int lastSeparator = Math.max(lastUnixPos, lastWindowsPos);

        int index = lastSeparator > extensionPos ? -1 : extensionPos;
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

}
