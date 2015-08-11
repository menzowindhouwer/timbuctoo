package nl.knaw.huygens.timbuctoo.rest.util.search;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import nl.knaw.huygens.timbuctoo.Repository;
import nl.knaw.huygens.timbuctoo.config.TypeNames;
import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.DomainEntityDTO;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.Relation;
import nl.knaw.huygens.timbuctoo.model.RelationDTO;
import nl.knaw.huygens.timbuctoo.model.RelationType;
import nl.knaw.huygens.timbuctoo.model.mapping.FieldNameMap;
import nl.knaw.huygens.timbuctoo.model.mapping.FieldNameMapFactory;
import nl.knaw.huygens.timbuctoo.model.mapping.MappingException;
import nl.knaw.huygens.timbuctoo.vre.NotInScopeException;
import nl.knaw.huygens.timbuctoo.vre.SearchException;
import nl.knaw.huygens.timbuctoo.vre.VRE;

import java.util.List;
import java.util.Map;

public class RelationDTOFactory {
  private final Repository repository;
  private final TypeRegistry typeRegistry;
  private final DomainEntityDTOFactory domainEntityDTOFactory;
  private final FieldNameMapFactory fieldNameMapFactory;

  @Inject
  public RelationDTOFactory(Repository repository, TypeRegistry typeRegistry, DomainEntityDTOFactory domainEntityDTOFactory, FieldNameMapFactory fieldNameMapFactory) {
    this.repository = repository;
    this.typeRegistry = typeRegistry;
    this.domainEntityDTOFactory = domainEntityDTOFactory;
    this.fieldNameMapFactory = fieldNameMapFactory;
  }

  public RelationDTO create(VRE vre, Class<? extends DomainEntity> type, Map<String, Object> dataRow) throws NotInScopeException, SearchException, MappingException {
    RelationDTO dto = new RelationDTO();

    dto.setType(TypeNames.getInternalName(type));
    String id = String.valueOf(dataRow.get(Entity.INDEX_FIELD_ID));
    dto.setId(id);
    dto.createPath(TypeNames.getExternalName(type), id);

    String relTypeId = String.valueOf(dataRow.get(Relation.TYPE_ID_FACET_NAME));
    RelationType relationType = repository.getEntityOrDefaultVariation(RelationType.class, relTypeId);
    dto.setRelationName(relationType.getRegularName());


    DomainEntityDTO sourceDTO = createEntityDTO(vre, dataRow, Relation.SOURCE_ID_FACET_NAME, Relation.INDEX_FIELD_SOURCE_TYPE);
    dto.setSourceName(sourceDTO.getDisplayName());
    dto.setSourceData(sourceDTO.getData());

    DomainEntityDTO targetDTO = createEntityDTO(vre, dataRow, Relation.TARGET_ID_FACET_NAME, Relation.INDEX_FIELD_TARGET_TYPE);
    dto.setTargetName(targetDTO.getDisplayName());
    dto.setTargetData(targetDTO.getData());

    return dto;
  }

  private DomainEntityDTO createEntityDTO(VRE vre, Map<String, Object> dataRow, String idField, String typeField) throws NotInScopeException, SearchException, MappingException {
    String id = String.valueOf(dataRow.get(idField));
    String typeString = String.valueOf(dataRow.get(typeField));

    Class<? extends DomainEntity> type = typeRegistry.getDomainEntityType(typeString);
    List<Map<String, Object>> data = vre.getRawDataFor(type, Lists.newArrayList(id));
    FieldNameMap fieldNameMap = fieldNameMapFactory.create(FieldNameMapFactory.Representation.INDEX, FieldNameMapFactory.Representation.CLIENT, type);

    System.out.println("data: " + data.get(0));
    System.out.println("type: " + type);
    System.out.println("fieldNameMap: " + fieldNameMap);

    return domainEntityDTOFactory.create(type, fieldNameMap, data.get(0));
  }
}