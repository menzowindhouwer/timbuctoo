package nl.knaw.huygens.timbuctoo.model.ckcc;

/*
 * #%L
 * Timbuctoo model
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

import java.util.Map;

import nl.knaw.huygens.timbuctoo.model.Person;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;

public class CKCCPerson extends Person {

  /** Unique identifier for concordances */
  private String urn;
  /** Either a Pica PPN or a name */
  private String cenId;
  private String notes;

  public String getUrn() {
    return urn;
  }

  public void setUrn(String urn) {
    this.urn = urn;
  }

  public String getCenId() {
    return cenId;
  }

  public void setCenId(String cenId) {
    this.cenId = cenId;
  }

  @JsonIgnore
  public String getCenUrn() {
    if (cenId == null || cenId.isEmpty()) {
      return "CEN::";
    } else if (Character.isDigit(cenId.charAt(0))) {
      // ppn
      return String.format("CEN:%s:",  cenId);
    } else {
      // name
      return String.format("CEN::%s",  cenId);
    }
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  @Override
  public Map<String, String> getClientRepresentation() {
    Map<String, String> data = Maps.newTreeMap();
    addItemToRepresentation(data, "urn", getUrn());
    addItemToRepresentation(data, "cen", getCenUrn());
    addItemToRepresentation(data, "notes", getNotes());
    return data;
  }

}
