package org.particleframework.core.value;

import org.particleframework.core.convert.ArgumentConversionContext;
import org.particleframework.core.convert.ConversionContext;
import org.particleframework.core.convert.ConversionService;

import java.util.Map;
import java.util.Optional;

/**
 * A {@link PropertyResolver} that resolves values from a backing map
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MapPropertyResolver implements PropertyResolver {
    private final Map<String, Object> map;
    private final ConversionService<?> conversionService;

    public MapPropertyResolver(Map<String, Object> map) {
        this.map = map;
        this.conversionService = ConversionService.SHARED;
    }

    public MapPropertyResolver(Map<String, Object> map, ConversionService conversionService) {
        this.map = map;
        this.conversionService = conversionService;
    }

    @Override
    public boolean containsProperty(String name) {
        return map.containsKey(name);
    }

    @Override
    public boolean containsProperties(String name) {
        return map.keySet().stream().anyMatch(k -> k.startsWith(name));
    }

    @Override
    public <T> Optional<T> getProperty(String name, ArgumentConversionContext<T> conversionContext) {
        Object value = map.get(name);
        return conversionService.convert(value, conversionContext);
    }
}