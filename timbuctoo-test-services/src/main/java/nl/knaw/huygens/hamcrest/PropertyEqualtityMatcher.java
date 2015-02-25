package nl.knaw.huygens.hamcrest;

/*
 * #%L
 * Test services
 * =======
 * Copyright (C) 2012 - 2015 Huygens ING
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

import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.Description;

public abstract class PropertyEqualtityMatcher<T, V> extends PropertyMatcher<T, V> {
  private final String propertyName;
  private final V propertyValue;

  public PropertyEqualtityMatcher(String propertyName, V propertyValue) {
    super(propertyName, equalTo(propertyValue));
    this.propertyName = propertyName;
    this.propertyValue = propertyValue;
  }

  @Override
  public void describeTo(Description description) {
    describeField(description, propertyName, propertyValue);
  }

  @Override
  protected void describeMismatchSafely(T item, Description mismatchDescription) {
    describeField(mismatchDescription, propertyName, getItemValue(item));
    mismatchDescription.appendText(" instead of: ").appendValue(propertyValue);
  }

  protected void describeField(Description description, String fieldName, Object value) {
    description.appendText(fieldName).appendText(" has value: ").appendValue(value);
  }

}
