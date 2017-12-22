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
package org.particleframework.jackson.bind;

import org.particleframework.core.bind.BeanPropertyBinder;
import org.particleframework.core.convert.ConversionContext;
import org.particleframework.core.convert.TypeConverter;
import org.particleframework.core.reflect.InstantiationUtils;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 * A class that uses the {@link BeanPropertyBinder} to bind maps to {@link Object} instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Singleton
public class MapToObjectConverter implements TypeConverter<Map, Object> {
    private final BeanPropertyBinder beanPropertyBinder;

    public MapToObjectConverter(BeanPropertyBinder beanPropertyBinder) {
        this.beanPropertyBinder = beanPropertyBinder;
    }

    @Override
    public Optional<Object> convert(Map map, Class<Object> targetType, ConversionContext context) {
        return InstantiationUtils.tryInstantiate(targetType)
                                 .map(object -> beanPropertyBinder.bind(object, map));
    }
}
