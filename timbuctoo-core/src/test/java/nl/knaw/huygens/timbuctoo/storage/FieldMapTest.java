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

import static nl.knaw.huygens.timbuctoo.storage.FieldMap.SEPARATOR;
import static nl.knaw.huygens.timbuctoo.storage.FieldMap.propertyName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.ModelException;
import nl.knaw.huygens.timbuctoo.model.Person;
import nl.knaw.huygens.timbuctoo.model.SystemEntity;
import nl.knaw.huygens.timbuctoo.model.User;

import org.junit.Test;

import test.variation.model.MongoObjectMapperEntity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FieldMapTest {

  private static final Class<? extends Entity> TYPE = MongoObjectMapperEntity.class;

  @Test
  public void testPropertyNameForEntity() {
    assertThat(propertyName(Entity.class, "_x"), equalTo("_x"));
    assertThat(propertyName(Entity.class, "^x"), equalTo("^x"));
    assertThat(propertyName(Entity.class, "@x"), equalTo("@x"));
    assertThat(propertyName(Entity.class, "xx"), equalTo("entity" + SEPARATOR + "xx"));
  }

  @Test
  public void testPropertyNameForSystemEntity() {
    assertThat(propertyName(SystemEntity.class, "_x"), equalTo("_x"));
    assertThat(propertyName(SystemEntity.class, "^x"), equalTo("^x"));
    assertThat(propertyName(SystemEntity.class, "@x"), equalTo("@x"));
    assertThat(propertyName(SystemEntity.class, "xx"), equalTo("systementity" + SEPARATOR + "xx"));
  }

  @Test
  public void testPropertyNameForDomainEntity() {
    assertThat(propertyName(DomainEntity.class, "_x"), equalTo("_x"));
    assertThat(propertyName(DomainEntity.class, "^x"), equalTo("^x"));
    assertThat(propertyName(DomainEntity.class, "@x"), equalTo("@x"));
    assertThat(propertyName(DomainEntity.class, "xx"), equalTo("domainentity" + SEPARATOR + "xx"));
  }

  @Test
  public void testPropertyNameForUser() {
    assertThat(propertyName(User.class, "_x"), equalTo("_x"));
    assertThat(propertyName(User.class, "^x"), equalTo("^x"));
    assertThat(propertyName(User.class, "@x"), equalTo("@x"));
    assertThat(propertyName(User.class, "xx"), equalTo("user" + SEPARATOR + "xx"));
  }

  @Test
  public void testPropertyNameForPerson() {
    assertThat(propertyName(Person.class, "_x"), equalTo("_x"));
    assertThat(propertyName(Person.class, "^x"), equalTo("^x"));
    assertThat(propertyName(Person.class, "@x"), equalTo("@x"));
    assertThat(propertyName(Person.class, "xx"), equalTo("person" + SEPARATOR + "xx"));
  }

  @Test
  public void testGetFieldNameSimpleField() throws Exception {
    assertThat(FieldMap.getFieldName(TYPE, TYPE.getDeclaredField("name")), equalTo("name"));
  }

  @Test
  public void testGetFieldNameForFieldWithAnnotation() throws Exception {
    assertThat(FieldMap.getFieldName(TYPE, TYPE.getDeclaredField("annotatedProperty")), equalTo("propAnnotated"));
  }

  @Test
  public void testGetFieldNameFieldForAccessorWithAnnotation() throws Exception {
    assertThat(FieldMap.getFieldName(TYPE, TYPE.getDeclaredField("propWithAnnotatedAccessors")), equalTo("pwaa"));
  }

  @Test
  public void testGetFieldNameForEntity() throws Exception {
    assertThat(FieldMap.getFieldName(Entity.class, Entity.class.getDeclaredField("id")), equalTo("_id"));
  }

  @Test
  public void testGetFieldNameForDomainEntity() throws Exception {
    assertThat(FieldMap.getFieldName(DomainEntity.class, Entity.class.getDeclaredField("id")), equalTo("_id"));
  }

  @Test
  public void testGetFieldNameForSystemEntity() throws Exception {
    assertThat(FieldMap.getFieldName(SystemEntity.class, Entity.class.getDeclaredField("id")), equalTo("_id"));
  }

  // ---------------------------------------------------------------------------

  @Test(expected = ModelException.class)
  public void testInvalidProperty() throws ModelException {
    FieldMap.validatePropertyNames(ClassWithInvalidProperty.class);
  }

  static class ClassWithInvalidProperty {
    private String proper_ty;

    public String getProper_ty() {
      return proper_ty;
    }

    public void setProper_ty(String proper_ty) {
      this.proper_ty = proper_ty;
    }
  }

  @Test
  public void testVariantProperty() throws ModelException {
    FieldMap.validatePropertyNames(ClassVariantProperty.class);
  }

  static class ClassVariantProperty {
    private String property;

    public String getProperty() {
      return property;
    }

    public void setProperty(String property) {
      this.property = property;
    }
  }

  @Test
  public void testInternalProperty() throws ModelException {
    FieldMap.validatePropertyNames(ClassWithInternalProperty.class);
  }

  static class ClassWithInternalProperty {
    private String property;

    @JsonProperty("_property")
    public String getProperty() {
      return property;
    }

    @JsonProperty("_property")
    public void setProperty(String property) {
      this.property = property;
    }
  }

  @Test
  public void testSharedProperty() throws ModelException {
    FieldMap.validatePropertyNames(ClassWithSharedProperty.class);
  }

  static class ClassWithSharedProperty {
    private String property;

    @JsonProperty("^property")
    public String getProperty() {
      return property;
    }

    @JsonProperty("^property")
    public void setProperty(String property) {
      this.property = property;
    }
  }

  @Test
  public void testVirtualProperty() throws ModelException {
    FieldMap.validatePropertyNames(ClassWithVirtualProperty.class);
  }

  static class ClassWithVirtualProperty {
    private String property;

    @JsonProperty("@property")
    public String getProperty() {
      return property;
    }

    @JsonProperty("@property")
    public void setProperty(String property) {
      this.property = property;
    }
  }

}