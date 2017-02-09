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
package org.microbean.configuration.spi;

public class StringToCharacterArrayConverter extends Converter<Character[]> {

  private static final long serialVersionUID = 1L;

  private static final Character[] EMPTY_CHARACTER_ARRAY = new Character[0];
  
  public StringToCharacterArrayConverter() {
    super();
    assert Character[].class.equals(this.getType());
  }

  @Override
  public Character[] convert(final String value) {
    Character[] returnValue = null;
    if (value != null) {
      if (value.isEmpty()) {
        returnValue = EMPTY_CHARACTER_ARRAY;
      } else {
        returnValue = value.chars().mapToObj(c -> (char)c).toArray(Character[]::new);
      }
    }
    return returnValue;
  }
  
}
