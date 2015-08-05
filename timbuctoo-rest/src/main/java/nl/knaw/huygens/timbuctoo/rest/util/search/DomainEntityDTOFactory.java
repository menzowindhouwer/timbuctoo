package nl.knaw.huygens.timbuctoo.rest.util.search;

import nl.knaw.huygens.timbuctoo.config.TypeNames;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.DomainEntityDTO;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.mapping.FieldNameMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DomainEntityDTOFactory {
  private static final Logger LOG = LoggerFactory.getLogger(DomainEntityDTOFactory.class);
  public DomainEntityDTO create(Class<? extends DomainEntity> type, FieldNameMap fieldNameMap, Map<String, Object> data) {
    LOG.debug("fieldNameMap: {}", fieldNameMap);
    LOG.debug("data: {}", data);

    DomainEntityDTO dto = new DomainEntityDTO();

    dto.setId(getAsString(data, Entity.INDEX_FIELD_ID));
    dto.setDisplayName(getAsString(data, Entity.INDEX_FIELD_IDENTIFICATION_NAME));
    dto.setData(fieldNameMap.remap(data));
    dto.setType(type);
    dto.createPath(TypeNames.getExternalName(type));

    return dto;
  }

  private String getAsString(Map<String, Object> data, String key) {
    Object value = data.get(key);
    return value != null ? value.toString() : null;
  }
}