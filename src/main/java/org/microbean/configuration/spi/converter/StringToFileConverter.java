/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017 MicroBean.
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

import java.io.File;

import org.microbean.configuration.spi.Converter;

public final class StringToFileConverter extends Converter<File> {

  private static final long serialVersionUID = 1L;

  @Override
  public final File convert(final String value) {
    final File returnValue;
    if (value == null) {
      returnValue = null;
    } else {
      returnValue = new File(value);
    }
    return returnValue;
  }
  
}
