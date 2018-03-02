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
package org.microbean.configuration.spi;

import java.io.Serializable;

import java.util.Map;

import org.microbean.configuration.api.ConfigurationValue;

/**
 * An {@link AbstractConfiguration} providing access to {@linkplain
 * System#getenv(String) environment variabes}.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #getValue(Map, String)
 */
public class EnvironmentVariablesConfiguration extends AbstractConfiguration implements Serializable {


  /*
   * Static fields.
   */

  
  /**
   * The version of this class for {@linkplain Serializable
   * serialization} purposes.
   */
  private static final long serialVersionUID = 1L;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link EnvironmentVariablesConfiguration}.
   */
  public EnvironmentVariablesConfiguration() {
    super();
  }


  /*
   * Instance methods.
   */
  

  /**
   * Returns a {@link ConfigurationValue} representing the {@linkplain
   * System#getenv(String) environment variable} identified by the
   * supplied {@code name}, or {@code null}.
   *
   * @param coordinates the configuration coordinates in effect for
   * the current request; may be {@code null}
   *
   * @param name the name of the configuration property for which to
   * return a {@link ConfigurationValue}; may be {@code null}
   *
   * @return a {@link ConfigurationValue}, or {@code null}
   */
  @Override
  public ConfigurationValue getValue(final Map<String, String> coordinates, final String name) {
    ConfigurationValue returnValue = null;
    if (name != null) {
      final String propertyValue = System.getenv(name);
      if (propertyValue != null) {
        returnValue = new ConfigurationValue(this, null /* deliberately null coordinates */, name, propertyValue, false);
      }
    }
    return returnValue;
  }

}
