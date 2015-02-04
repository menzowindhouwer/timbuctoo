package nl.knaw.huygens.timbuctoo.rest.resources;

/*
 * #%L
 * Timbuctoo REST api
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

import static nl.knaw.huygens.timbuctoo.config.Paths.SYSTEM_PREFIX;
import static nl.knaw.huygens.timbuctoo.config.Paths.VERSION_PATH_OPTIONAL;

import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import nl.knaw.huygens.timbuctoo.Repository;
import nl.knaw.huygens.timbuctoo.annotations.APIDesc;
import nl.knaw.huygens.timbuctoo.config.TypeNames;
import nl.knaw.huygens.timbuctoo.model.RelationType;
import nl.knaw.huygens.timbuctoo.vre.VRE;
import nl.knaw.huygens.timbuctoo.vre.VRECollection;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

@Path(VERSION_PATH_OPTIONAL + SYSTEM_PREFIX + "/vres")
public class VREResource extends ResourceBase {

  private VRECollection vres;

  @Inject
  public VREResource(Repository repository, VRECollection vres) {
    super(repository, vres);
    this.vres = vres;

  }

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @APIDesc("Lists the available VRE's.")
  public Set<String> getAvailableVREs() {
    Set<String> ids = Sets.newTreeSet();
    for (VRE vre : vres.getAll()) {
      ids.add(vre.getVreId());
    }
    return ids;
  }

  @GET
  @Path("/{id}")
  @Produces({ MediaType.APPLICATION_JSON })
  @APIDesc("Provides info about the specified VRE.")
  public VREInfo getVREInfo(@PathParam("id") String vreId) {
    VRE vre = super.getValidVRE(vreId);

    VREInfo info = new VREInfo();
    info.setName(vre.getVreId());
    info.setDescription(vre.getDescription());

    for (String name : vre.getReceptionNames()) {
      RelationType type = repository.getRelationTypeByName(name, false);
      if (type != null) {
        Reception reception = new Reception();
        reception.typeId = type.getId();
        reception.regularName = type.getRegularName();
        reception.inverseName = type.getInverseName();
        reception.baseSourceType = type.getSourceTypeName();
        reception.baseTargetType = type.getTargetTypeName();
        reception.derivedSourceType = mapTypeName(vre, type.getSourceTypeName());
        reception.derivedTargetType = mapTypeName(vre, type.getTargetTypeName());
        info.addReception(reception);
      }
    }
    return info;
  }

  private String mapTypeName(VRE vre, String iname) {
    return TypeNames.getInternalName(vre.mapTypeName(iname, true));
  }

  // ---------------------------------------------------------------------------

  public static class VREInfo {
    private String name;
    private String description;
    private final List<Reception> receptions = Lists.newArrayList();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public List<Reception> getReceptions() {
      return receptions;
    }

    public void addReception(Reception reception) {
      receptions.add(reception);
    }
  }

  public static class Reception {
    public String typeId;
    public String regularName;
    public String inverseName;
    public String baseSourceType;
    public String baseTargetType;
    public String derivedSourceType;
    public String derivedTargetType;
  }

}
