/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017-2018 microBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.configuration.spi.converter;

import java.time.Duration;

import org.microbean.configuration.spi.Converter;

public final class StringToDurationConverter extends Converter<Duration> {

  private static final long serialVersionUID = 1L;

  @Override
  public final Duration convert(final String value) {
    Duration returnValue = null;
    if (value != null) {
      returnValue = Duration.parse(value);
    }
    return returnValue;
  }
  
}
