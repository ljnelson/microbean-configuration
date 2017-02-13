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
package org.microbean.configuration;

import java.lang.reflect.Type;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.Set;

import java.util.function.Function;

import java.util.stream.Collectors;

import org.microbean.configuration.spi.Arbiter;
import org.microbean.configuration.spi.Configuration;
import org.microbean.configuration.spi.ConfigurationValue;
import org.microbean.configuration.spi.Converter;
import org.microbean.configuration.spi.TypeLiteral;

/**
 * A single source for configuration values suitable for an
 * application.
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see Configuration
 */
public class Configurations {


  /*
   * Static fields.
   */

  
  /**
   * The name of the configuration property whose value is a {@link
   * Map} of <em>configuration coordinates</em> for the current
   * application.
   *
   * <p>This field is never {@code null}.</p>
   *
   * <p>A request is made via the {@link #getValue(Map, String, Type)}
   * method with {@code null} as the value of its first parameter and
   * the value of this field as its second parameter and {@link Map
   * Map.class} as the value of its third parameter.  The returned
   * {@link Map} is cached for the lifetime of this {@link
   * Configurations} object and is returned by the {@link
   * #getConfigurationCoordinates()} method.</p>
   *
   * @see #getValue(Map, String, Type)
   *
   * @see #getConfigurationCoordinates()
   */
  public static final String CONFIGURATION_COORDINATES = "configurationCoordinates";

  private final ThreadLocal<Set<Configuration>> currentlyActiveConfigurations;
  

  /*
   * Instance fields.
   */

  private final boolean initialized;

  private final Collection<Configuration> configurations;

  private final Collection<Arbiter> arbiters;

  private final Map<Type, Converter<?>> converters;

  private final Map<String, String> configurationCoordinates;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link Configurations}.
   *
   * <p>The {@link #loadConfigurations()}, {@link #loadConverters()}
   * and {@link #loadArbiters()} methods will be invoked during
   * construction.</p>
   *
   * @see #loadConfigurations()
   *
   * @see #loadConverters()
   *
   * @see #loadArbiters()
   *
   * @see #Configurations(Collection, Collection, Collection)
   */
  public Configurations() {
    this(null, null, null);
  }

  /**
   * Creates a new {@link Configurations}.
   *
   * <p>The {@link #loadConverters()} and {@link #loadArbiters()}
   * methods will be invoked during construction.  IF the supplied
   * {@code configurations} is {@code null}, then the {@link
   * #loadConfigurations()} method will be invoked during
   * construction.</p>
   *
   * @param configurations a {@link Collection} of {@link
   * Configuration} instances; if {@code null} then the return value
   * of the {@link #loadConfigurations()} method will be used instead
   *
   * @see #loadConfigurations()
   *
   * @see #loadConverters()
   *
   * @see #loadArbiters()
   *
   * @see #Configurations(Collection, Collection, Collection)
   */
  public Configurations(final Collection<? extends Configuration> configurations) {
    this(configurations, null, null);
  }

  /**
   * Creates a new {@link Configurations}.
   *
   * <p>The {@link #loadConverters()} and {@link #loadArbiters()}
   * methods will be invoked during construction.  IF the supplied
   * {@code configurations} is {@code null}, then the {@link
   * #loadConfigurations()} method will be invoked during
   * construction.</p>
   *
   * @param configurations a {@link Collection} of {@link
   * Configuration} instances; if {@code null} then the return value
   * of the {@link #loadConfigurations()} method will be used instead
   *
   * @param converters a {@link Collection} of {@link Converter}
   * instances; if {@code null} then the return value of the {@link
   * #loadConverters()} method will be used instead
   *
   * @param arbiters a {@link Collection} of {@link Arbiter}
   * instances; if {@code null} then the return value of the {@link
   * #loadArbiters()} method will be used instead
   *
   * @see #loadConfigurations()
   *
   * @see #loadConverters()
   *
   * @see #loadArbiters()
   */
  public Configurations(Collection<? extends Configuration> configurations,
                        Collection<? extends Converter<?>> converters,
                        Collection<? extends Arbiter> arbiters) {
    super();
    this.currentlyActiveConfigurations = ThreadLocal.withInitial(() -> new HashSet<>());
    if (configurations == null) {
      configurations = this.loadConfigurations();
    }
    if (configurations == null || configurations.isEmpty()) {
      this.configurations = Collections.emptySet();
    } else {
      this.configurations = Collections.unmodifiableCollection(new LinkedList<>(configurations));
    }
    for (final Configuration configuration : configurations) {
      if (configuration != null) {
        configuration.setConfigurations(this);
      }
    }

    if (converters == null) {
      converters = this.loadConverters();
    }
    if (converters == null || converters.isEmpty()) {
      converters = Collections.emptySet();
    } else {
      converters = Collections.unmodifiableCollection(new LinkedList<>(converters));
    }
    assert converters != null;
    this.converters = Collections.unmodifiableMap(converters.stream().collect(Collectors.toMap(c -> c.getType(), Function.identity())));

    if (arbiters == null) {
      arbiters = this.loadArbiters();
    }
    if (arbiters == null || arbiters.isEmpty()) {
      this.arbiters = Collections.emptySet();
    } else {
      this.arbiters = Collections.unmodifiableCollection(new LinkedList<>(arbiters));
    }

    this.initialized = true;
    
    final Map<String, String> coordinates = this.getValue(null, CONFIGURATION_COORDINATES, new TypeLiteral<Map<String, String>>() {
        private static final long serialVersionUID = 1L; }.getType());
    if (coordinates == null || coordinates.isEmpty()) {
      this.configurationCoordinates = Collections.emptyMap();
    } else {
      this.configurationCoordinates = Collections.unmodifiableMap(coordinates);
    }

  }


  /*
   * Instance methods.
   */
  

  /**
   * Loads a {@link Collection} of {@link Configuration} objects and
   * returns it.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must not return {@code null}.</p>
   *
   * <p>The default implementation of this method uses the {@link
   * ServiceLoader} mechanism to load {@link Configuration}
   * instances.</p>
   *
   * @return a non-{@code null}, {@link Collection} of {@link
   * Configuration} instances
   *
   * @see ServiceLoader#load(Class)
   */
  protected Collection<? extends Configuration> loadConfigurations() {
    final Collection<Configuration> returnValue = new LinkedList<>();
    final ServiceLoader<Configuration> configurationLoader = ServiceLoader.load(Configuration.class);
    assert configurationLoader != null;
    final Iterator<Configuration> configurationIterator = configurationLoader.iterator();
    assert configurationIterator != null;
    while (configurationIterator.hasNext()) {
      final Configuration configuration = configurationIterator.next();
      assert configuration != null;
      returnValue.add(configuration);
    }
    return returnValue;
  }

  /**
   * Loads a {@link Collection} of {@link Converter} objects and
   * returns it.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must not return {@code null}.</p>
   *
   * <p>The default implementation of this method uses the {@link
   * ServiceLoader} mechanism to load {@link Converter} instances.</p>
   *
   * @return a non-{@code null}, {@link Collection} of {@link
   * Converter} instances
   *
   * @see ServiceLoader#load(Class)
   */
  protected Collection<? extends Converter<?>> loadConverters() {
    final Collection<Converter<?>> returnValue = new LinkedList<>();
    @SuppressWarnings("rawtypes")
    final ServiceLoader<Converter> converterLoader = ServiceLoader.load(Converter.class);
    assert converterLoader != null;
    @SuppressWarnings("rawtypes")
    final Iterator<Converter> converterIterator = converterLoader.iterator();
    assert converterIterator != null;
    while (converterIterator.hasNext()) {
      final Converter<?> converter = converterIterator.next();
      assert converter != null;
      returnValue.add(converter);
    }
    return returnValue;
  }

  /**
   * Loads a {@link Collection} of {@link Arbiter} objects and returns
   * it.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must not return {@code null}.</p>
   *
   * <p>The default implementation of this method uses the {@link
   * ServiceLoader} mechanism to load {@link Arbiter} instances.</p>
   *
   * @return a non-{@code null}, {@link Collection} of {@link Arbiter}
   * instances
   *
   * @see ServiceLoader#load(Class)
   */
  protected Collection<? extends Arbiter> loadArbiters() {
    final Collection<Arbiter> returnValue = new LinkedList<>();
    final ServiceLoader<Arbiter> arbiterLoader = ServiceLoader.load(Arbiter.class);
    assert arbiterLoader != null;
    final Iterator<Arbiter> arbiterIterator = arbiterLoader.iterator();
    assert arbiterIterator != null;
    while (arbiterIterator.hasNext()) {
      final Arbiter arbiter = arbiterIterator.next();
      assert arbiter != null;
      returnValue.add(arbiter);
    }
    return returnValue;
  }

  /**
   * Returns a {@link Map} of <em>configuration
   * coordinates</em>&mdash;aspects and their values that define a
   * location within which requests for configuration values may take
   * place.
   *
   * <p>This method may return {@code null}.</p>
   *
   * <p>Overrides of this method may return {@code null}.</p>
   *
   * <p>The default implementation of this method returns
   * configuration coordinates that are discovered at {@linkplain
   * #Configurations() construction time} and cached for the lifetime
   * of this {@link Configurations} object.</p>
   *
   * @return a {@link Map} of configuration coordinates; may be {@code
   * null}
   */
  public Map<String, String> getConfigurationCoordinates() {
    return this.configurationCoordinates;
  }

  /**
   * Returns a non-{@code null}, {@linkplain
   * Collections#unmodifiableSet(Set) immutable} {@link Set} of {@link
   * Type}s representing all the types to which {@link String}
   * configuration values may be converted by the {@linkplain
   * #loadConverters() <code>Converter</code>s loaded} by this {@link
   * Configurations} object.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return a non-{@code null}, {@linkplain
   * Collections#unmodifiableSet(Set) immutable} {@link Set} of {@link
   * Type}s
   */
  public final Set<Type> getConversionTypes() {
    this.checkState();
    return this.converters.keySet();
  }
  
  public final String getValue(final String name) {
    return this.getValue(this.getConfigurationCoordinates(), name, String.class, null);
  }

  public final String getValue(final String name, final String defaultValue) {
    return this.getValue(this.getConfigurationCoordinates(), name, String.class, defaultValue);
  }
  
  public final String getValue(Map<String, String> callerCoordinates, final String name) {
    return this.getValue(callerCoordinates, name, String.class, null);
  }

  public final String getValue(Map<String, String> callerCoordinates, final String name, final String defaultValue) {
    return this.getValue(callerCoordinates, name, String.class, defaultValue);
  }

  public final <T> T getValue(final String name, final Class<T> type) {
    return this.getValue(this.getConfigurationCoordinates(), name, type, null);
  }

  public final <T> T getValue(final String name, final Class<T> type, final String defaultValue) {
    return this.getValue(this.getConfigurationCoordinates(), name, type, defaultValue);
  }
  
  public final <T> T getValue(Map<String, String> callerCoordinates, final String name, final Class<T> type) {
    return this.getValue(callerCoordinates, name, (Type)type, null);
  }

  public final <T> T getValue(Map<String, String> callerCoordinates, final String name, final Class<T> type, final String defaultValue) {
    return this.getValue(callerCoordinates, name, (Type)type, defaultValue);
  }

  public final <T> T getValue(final String name, final TypeLiteral<T> typeLiteral) {
    return this.getValue(this.getConfigurationCoordinates(), name, typeLiteral, null);
  }

  public final <T> T getValue(final String name, final TypeLiteral<T> typeLiteral, final String defaultValue) {
    return this.getValue(this.getConfigurationCoordinates(), name, typeLiteral, defaultValue);
  }
  
  public final <T> T getValue(Map<String, String> callerCoordinates, final String name, final TypeLiteral<T> typeLiteral) {
    return this.getValue(callerCoordinates, name, typeLiteral == null ? (Type)null : typeLiteral.getType(), null);
  }

  public final <T> T getValue(Map<String, String> callerCoordinates, final String name, final TypeLiteral<T> typeLiteral, final String defaultValue) {
    return this.getValue(callerCoordinates, name, typeLiteral == null ? (Type)null : typeLiteral.getType(), defaultValue);
  }

  public final <T> T getValue(final String name, final Type type) {
    return this.getValue(this.getConfigurationCoordinates(), name, type, null);
  }

  public final <T> T getValue(final String name, final Type type, final String defaultValue) {
    return this.getValue(this.getConfigurationCoordinates(), name, type, defaultValue);
  }
  
  public final <T> T getValue(Map<String, String> callerCoordinates, final String name, final Type type) {
    return this.getValue(callerCoordinates, name, type, null);
  }

  public final <T> T getValue(Map<String, String> callerCoordinates, final String name, final Type type, final String defaultValue) {
    @SuppressWarnings("unchecked")
    final Converter<T> converter = (Converter<T>)this.converters.get(type);
    if (converter == null) {
      throw new NoSuchConverterException(null, null, type);
    }
    return this.getValue(callerCoordinates, name, converter, defaultValue);
  }

  public final <T> T getValue(final String name, final Converter<T> converter) {
    return this.getValue(this.getConfigurationCoordinates(), name, converter, null);
  }
  
  public final <T> T getValue(Map<String, String> callerCoordinates, final String name, final Converter<T> converter) {
    return this.getValue(callerCoordinates, name, converter, null);
  }

  /**
   * Returns an object that is the value for the configuration request
   * represented by the supplied {@code callerCoordinates}, {@code
   * name} and {@code defaultValue} parameters, as converted by the
   * supplied {@link Converter}.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @param <T> the type of the object to be returned
   *
   * @param callerCoordinates the configuration coordinates for which
   * a value should be selected; may be {@code null}
   *
   * @param name the name of the configuration property within the
   * world defined by the supplied {@code configurationCoordinates}
   * whose value is to be selected; must not be {@code null}
   *
   * @param converter a {@link Converter} instance that will convert
   * any {@link String} configuration value into the type of object
   * that this method will return; must not be {@code null}
   *
   * @param defaultValue the fallback default value to use as an
   * absolute last resort; may be {@code null}; will also be converted
   * by the supplied {@link Converter}
   *
   * @return the value for the implied configuration property, or {@code null}
   *
   * @exception NullPointerException if either {@code name} or {@code
   * converter} is {@code null}
   *
   * @exception AmbiguousConfigurationValuesException if two or more
   * values were found that could be suitable and arbitration
   * {@linkplain #performArbitration(Map, String, Collection) was
   * performed} but could not resolve the dispute
   *
   * @see Converter#convert(String)
   *
   * @see Configuration#getValue(Map, String)
   *
   * @see #performArbitration(Map, String, Collection)
   *
   * @see #handleMalformedConfigurationValues(Collection)
   */
  public <T> T getValue(Map<String, String> callerCoordinates, final String name, final Converter<T> converter, final String defaultValue) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(converter);
    this.checkState();
    if (callerCoordinates == null) {
      callerCoordinates = Collections.emptyMap();
    }
    T returnValue = null;

    ConfigurationValue selectedValue = null;    

    // We use a PriorityQueue of ConfigurationValues sorted by their
    // specificity (most specific first) to keep track of the most
    // specific ConfigurationValue found so far.  We create it only
    // when necessary.
    final Comparator<ConfigurationValue> comparator = Comparator.<ConfigurationValue>comparingInt(v -> v.specificity()).reversed();
    PriorityQueue<ConfigurationValue> values = null;

    Collection<ConfigurationValue> badValues = null;
    
    for (final Configuration configuration : this.configurations) {
      assert configuration != null;

      final ConfigurationValue value;
      try {
        if (isActive(configuration)) {
          value = null;
        } else {
          this.activate(configuration);
          value = configuration.getValue(callerCoordinates, name);
        }
      } finally {
        this.deactivate(configuration);
      }

      if (value != null) {

        if (!name.equals(value.getName())) {
          badValues.add(value);
          continue;
        }

        Map<String, String> valueCoordinates = value.getCoordinates();
        if (valueCoordinates == null) {
          valueCoordinates = Collections.emptyMap();
        }

        final int callerCoordinatesSize = callerCoordinates.size();
        final int valueCoordinatesSize = valueCoordinates.size();

        if (callerCoordinatesSize < valueCoordinatesSize) {
          // Bad value!
          if (badValues == null) {
            badValues = new LinkedList<>();
          }
          badValues.add(value);
          
        } else if (callerCoordinates.equals(valueCoordinates)) {
          // We have an exact match.  We hope it's going to be the
          // only one.
          
          if (selectedValue == null) {
            
            if (values == null || values.isEmpty()) {
              // There aren't any conflicts yet; this is good.  This
              // value will be our candidate.
              selectedValue = value;

            } else {
              // We got a match, but we already *had* a match, so we
              // don't have a candidate--instead, add it to the bucket
              // of values that will be arbitrated later.
              values.add(value);
              
            }
            
          } else {
            assert selectedValue != null;
            // We have an exact match, but we already identified a
            // candidate, so oops, we have to treat our prior match
            // and this one as non-candidates.
            
            if (values == null) {
              values = new PriorityQueue<>(comparator);
            }
            values.add(selectedValue);
            selectedValue = null;
            values.add(value);
          }

        } else if (callerCoordinatesSize == valueCoordinatesSize) {
          // Bad value!  The configuration subsystem handed back a
          // value containing coordinates not drawn from the
          // callerCoordinatesSet.  We know this because we already
          // tested for Set equality, which failed, so this test means
          // disparate entries.
          if (badValues == null) {
            badValues = new LinkedList<>();
          }
          badValues.add(value);
          
        } else if (selectedValue != null) {
          // Nothing to do; we've already got our candidate.  We don't
          // break here because we're going to ensure there aren't any
          // duplicates.
          
        } else if (callerCoordinates.entrySet().containsAll(valueCoordinates.entrySet())) {
          // We specified, e.g., {a=b, c=d, e=f} and they have, say,
          // {c=d, e=f} or {a=b, c=d} etc. but not, say, {q=r}.
          if (values == null) {
            values = new PriorityQueue<>(comparator);
          }
          values.add(value);
          
        } else {
          // Bad value!
          if (badValues == null) {
            badValues = new LinkedList<>();
          }
          badValues.add(value);
          
        }
      }
    }
    assert this.allConfigurationsInactive();

    if (badValues != null && !badValues.isEmpty()) {
      this.handleMalformedConfigurationValues(badValues);
    }
    
    if (selectedValue == null && values != null && !values.isEmpty()) {
      final Collection<ConfigurationValue> valuesToArbitrate = new LinkedList<>();
      int highestSpecificitySoFarEncountered = -1;
      while (!values.isEmpty()) {

        final ConfigurationValue value = values.poll();
        assert value != null;

        // The values are sorted by their specificity, most specific
        // first.
        final int valueSpecificity = Math.max(0, value.specificity());
        assert highestSpecificitySoFarEncountered < 0 || valueSpecificity <= highestSpecificitySoFarEncountered;

        if (highestSpecificitySoFarEncountered < 0 || valueSpecificity < highestSpecificitySoFarEncountered) {
          if (selectedValue == null) {
            assert valuesToArbitrate.isEmpty();
            selectedValue = value;
            highestSpecificitySoFarEncountered = valueSpecificity;
          } else if (valuesToArbitrate.isEmpty()) {
            break;
          } else {
            valuesToArbitrate.add(value);
          }
        } else if (valueSpecificity == highestSpecificitySoFarEncountered) {
          assert selectedValue != null;
          if (value.isAuthoritative()) {
            if (selectedValue.isAuthoritative()) {
              // Both say they're authoritative; arbitration required
              valuesToArbitrate.add(selectedValue);
              selectedValue = null;
              valuesToArbitrate.add(value);
            } else {
              // value is authoritative; selectedValue is not; so swap
              // them
              selectedValue = value;
            }
          } else if (selectedValue.isAuthoritative()) {
            // value is not authoritative; selected value is; so just
            // drop value on the floor; it's not authoritative.
          } else {
            // Neither is authoritative; arbitration required.
            valuesToArbitrate.add(selectedValue);
            selectedValue = null;
            valuesToArbitrate.add(value);
          }
        } else {
          assert false : "valueSpecificity > highestSpecificitySoFarEncountered: " + valueSpecificity + " > " + highestSpecificitySoFarEncountered;
        }
        
      }
      
      if (!valuesToArbitrate.isEmpty()) {
        selectedValue = this.performArbitration(callerCoordinates, name, Collections.unmodifiableCollection(valuesToArbitrate));
      }
    }
    
    if (selectedValue == null) {
      returnValue = converter.convert(defaultValue);
    } else {
      returnValue = converter.convert(selectedValue.getValue());
    }
    return returnValue;
  }

  /**
   * Handles any badly formed {@link ConfigurationValue} instances
   * received from {@link Configuration} instances during the
   * execution of a configuration value request.
   *
   * <p>The default implementation of this method does nothing.
   * Malformed values are thus effectively discarded.</p>
   *
   * @param badValues a {@link Collection} of {@link
   * ConfigurationValue} instances that were deemed to be malformed in
   * some way; may be {@code null}
   */
  protected void handleMalformedConfigurationValues(final Collection<ConfigurationValue> badValues) {
    
  }

  /**
   * Given a logical request for a configuration value, represented by
   * the {@code callerCoordinates} and {@code name} parameter values,
   * and a {@link Collection} of {@link ConfigurationValue} instances
   * that represents the ambiguous response from several {@link
   * Configuration} instances, attempts to resolve the ambiguity by
   * returning a single {@link ConfigurationValue} instead.
   *
   * <p>This method may return {@code null}.</p>
   *
   * <p>Overrides of this method may return {@code null}.</p>
   *
   * <p>The default implementation of this method asks all registered
   * {@link Arbiter}s in turn to perform the arbitration and returns
   * the first non-{@code null} response received.</p>
   *
   * @param callerCoordinates the ({@linkplain
   * Collections#unmodifiableMap(Map) immutable}) configuration
   * coordinates in effect for the request; may be {@code null}
   *
   * @param name the name of the configuration value; may be {@code
   * null}
   *
   * @param values an {@linkplain
   * Collections#unmodifiableCollection(Collection) immutable} {@link
   * Collection} of definitionally ambiguous {@link
   * ConfigurationValue}s that resulted from the request; may be
   * {@code null}
   *
   * @return the result of arbitration, or {@code null}
   *
   * @exception AmbiguousConfigurationValuesException if successful
   * arbitration did not happen for any reason
   *
   * @see Arbiter
   */
  protected ConfigurationValue performArbitration(final Map<? extends String, ? extends String> callerCoordinates,
                                                  final String name,
                                                  final Collection<? extends ConfigurationValue> values) {
    if (this.arbiters != null && !this.arbiters.isEmpty()) {
      for (final Arbiter arbiter : arbiters) {
        if (arbiter != null) {
          final ConfigurationValue arbitrationResult = arbiter.arbitrate(callerCoordinates, name, values);
          if (arbitrationResult != null) {
            return arbitrationResult;
          }
        }
      }
    }
    throw new AmbiguousConfigurationValuesException(null, null, callerCoordinates, name, values);
  }

  /**
   * If this {@link Configurations} has not yet finished {@linkplain
   * #Configurations() constructing}, then this method will throw an
   * {@link IllegalStateException}.
   *
   * @exception IllegalStateException if this {@link Configurations}
   * has not yet finished {@linkplain #Configurations() constructing}
   *
   * @see #Configurations()
   */
  private final void checkState() {
    if (!this.initialized) {
      throw new IllegalStateException();
    }
  }

  /**
   * Returns {@code true} if the supplied {@link Configuration} is
   * currently in the middle of executing its {@link
   * Configuration#getValue(Map, String)} method on the current {@link
   * Thread}.
   *
   * @param configuration the {@link Configuration} to test; may be
   * {@code null} in which case {@code false} will be returned
   *
   * @return {@code true} if the supplied {@link Configuration} is
   * active; {@code false} otherwise
   *
   * @see #activate(Configuration)
   *
   * @see #deactivate(Configuration)
   */
  private final boolean isActive(final Configuration configuration) {
    return configuration != null && this.currentlyActiveConfigurations.get().contains(configuration);
  }

  /**
   * Records that the supplied {@link Configuration} is in the middle
   * of executing its {@link Configuration#getValue(Map, String)}
   * method on the current {@link Thread}.
   *
   * <p>This method is idempotent.</p>
   *
   * @param configuration the {@link Configuration} in question; may
   * be {@code null} in which case no action is taken
   *
   * @see #isActive(Configuration)
   *
   * @see #deactivate(Configuration)
   */
  private final void activate(final Configuration configuration) {
    if (configuration != null) {
      this.currentlyActiveConfigurations.get().add(configuration);
      assert this.isActive(configuration);
    }
  }

  /**
   * Records that the supplied {@link Configuration} is no longer in
   * the middle of executing its {@link Configuration#getValue(Map,
   * String)} method on the current {@link Thread}.
   *
   * <p>This method is idempotent.</p>
   *
   * @param configuration the {@link Configuration} in question; may
   * be {@code null} in which case no action is taken
   *
   * @see #isActive(Configuration)
   *
   * @see #activate(Configuration)
   */
  private final void deactivate(final Configuration configuration) {
    if (configuration != null) {
      this.currentlyActiveConfigurations.get().remove(configuration);
    }
    assert !this.isActive(configuration);
  }

  /**
   * Returns {@code true} if all {@link Configuration} instances have
   * been {@linkplain #deactivate(Configuration) deactivated}.
   *
   * @return {@code true} if all {@link Configuration} instances have
   * been {@linkplain #deactivate(Configuration) deactivated}; {@code
   * false} otherwise
   *
   * @see #isActive(Configuration)
   *
   * @see #deactivate(Configuration)
   */
  private final boolean allConfigurationsInactive() {
    return this.currentlyActiveConfigurations.get().isEmpty();
  }
  
}
