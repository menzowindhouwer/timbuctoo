package nl.knaw.huygens.timbuctoo.storage;

/*
 * #%L
 * Timbuctoo core
 * =======
 * Copyright (C) 2012 - 2014 Huygens ING
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.regex.Pattern;

import nl.knaw.huygens.timbuctoo.config.TypeNames;
import nl.knaw.huygens.timbuctoo.model.ModelException;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A class that contains all the information about how the class fields are mapped 
 * to the fields in the database.
 */
public class FieldMap extends HashMap<String, Field> {

  private static final long serialVersionUID = 1L;

  /** Separator between parts of a property name, as string. */
  public static final String SEPARATOR = ":";

  /** Separator between parts of a key, as character. */
  public static final char SEPARATOR_CHAR = ':';

  /** Prefix of properties that are not (de)serialzied. */
  public static final char VIRTUAL_PROPERTY_PREFIX = '@';

  /**
   * Constructs an empty field map.
   */
  public FieldMap() {}

  /**
   * Constructs a field map for the specified view type with the specified prefix.
   */
  public FieldMap(Class<?> prefixType, Class<?> viewType) {
    addToFieldMap(prefixType, viewType);
  }

  /**
   * Constructs a composite field map for all types starting with viewType up to and
   * including the stoptype. To get a non-empty map stopType must be a superclass
   * of viewType.
   */
  public FieldMap(Class<?> prefixType, Class<?> viewType, Class<?> stopType) {
    Class<?> type = viewType;
    while (stopType.isAssignableFrom(type)) {
      addToFieldMap(prefixType, type);
      type = type.getSuperclass();
    }
  }

  /**
   * Adds declared fields of the specified view type to the field map,
   * using as keys the corresponding property names.
   */
  public void addToFieldMap(Class<?> prefixType, Class<?> viewType) {
    String prefix = TypeNames.getInternalName(prefixType);
    for (Field field : viewType.getDeclaredFields()) {
      if (isProperty(field)) {
        String fieldName = getFieldName(viewType, field);
        if (!isVirtualProperty(fieldName)) {
          put(propertyName(prefix, fieldName), field);
        }
      }
    }
  }

  // ---------------------------------------------------------------------------

  /** Returns the name of a property from its parts. */
  public static String propertyName(String prefix, String field) {
    checkArgument(field != null && field.length() != 0);

    StringBuilder builder = new StringBuilder();
    if (Character.isLetter(field.charAt(0))) {
      builder.append(prefix).append(SEPARATOR_CHAR);
    }
    builder.append(field);
    return builder.toString();
  }

  /** Returns the name of a property from its parts. */
  public static String propertyName(Class<?> type, String field) {
    return propertyName(TypeNames.getInternalName(type), field);
  }

  /**
   * Validates the property names in the class of the specified type
   * and throws a {@code ModelException} if an invalid name is found.
   * 
   * Allowed names are standard Java identifiers without an underscore
   * character, optionally prefixed with a "_", "^" or "@".
   */
  public static void validatePropertyNames(Class<?> type) throws ModelException {
    Pattern pattern = Pattern.compile("[\\_\\^\\@]?[a-zA-Z][a-zA-Z0-9]*");
    for (Field field : type.getDeclaredFields()) {
      if (isProperty(field)) {
        String name = getFieldName(type, field);
        if (!pattern.matcher(name).matches()) {
          throw new ModelException("Invalid property name %s of %s", name, type);
        }
      }
    }
  }

  /**
   * Gets the name of the specified field in the specified class, without a prefix.
   * It uses the name specified in {@code JsonProperty} annotations on the field
   * itself or the getter corresponding to the field (in that order).
   */
  public static String getFieldName(Class<?> type, Field field) {
    JsonProperty annotation = field.getAnnotation(JsonProperty.class);
    if (annotation != null) {
      return annotation.value();
    }

    Method method = getMethodByName(type, getMethodName(field));
    if (method != null && method.getAnnotation(JsonProperty.class) != null) {
      return method.getAnnotation(JsonProperty.class).value();
    }

    return field.getName();
  }

  // ---------------------------------------------------------------------------

  /**
   * Indicates whether a field qualifies as property.
   */
  private static boolean isProperty(Field field) {
    return (field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) == 0;
  }

  private static boolean isVirtualProperty(String name) {
    return !name.isEmpty() && (name.charAt(0) == VIRTUAL_PROPERTY_PREFIX);
  }

  /**
   * Searches for a public method in the specified class or its superclasses
   * and -interfaces that matches the specified name and has no parameters.
   */
  private static Method getMethodByName(Class<?> type, String methodName) {
    try {
      // TODO decide: use type.getDeclaredMethod(methodName)?
      return type.getMethod(methodName);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private static final String GET_ACCESSOR = "get";
  private static final String IS_ACCESSOR = "is"; // get accesor for booleans.

  private static String getMethodName(Field field) {
    char[] fieldNameChars = field.getName().toCharArray();

    fieldNameChars[0] = Character.toUpperCase(fieldNameChars[0]);

    String accessor = isBoolean(field.getType()) ? IS_ACCESSOR : GET_ACCESSOR;
    return accessor.concat(String.valueOf(fieldNameChars));
  }

  private static boolean isBoolean(Class<?> cls) {
    return cls == boolean.class || cls == Boolean.class;
  }

}
