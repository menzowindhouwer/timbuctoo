package nl.knaw.huygens.timbuctoo.storage.neo4j;

import nl.knaw.huygens.timbuctoo.model.Entity;

import org.neo4j.graphdb.PropertyContainer;

public class NoOpPropertyContainerConverter<U extends PropertyContainer, T extends Entity> implements PropertyContainerConverter<U, T> {

  @Override
  public void addValuesToPropertyContainer(U propertyContainer, T entity) throws ConversionException {
    // TODO Auto-generated method stub

  }

  @Override
  public void addValuesToEntity(T entity, U propertyContainer) throws ConversionException {
    // TODO Auto-generated method stub

  }

  @Override
  public void addFieldConverter(FieldConverter fieldWrapper) {
    // TODO Auto-generated method stub

  }

  @Override
  public void updatePropertyContainer(U propertyContainer, Entity entity) throws ConversionException {
    // TODO Auto-generated method stub

  }

  @Override
  public void updateModifiedAndRev(U propertyContainer, Entity entity) throws ConversionException {
    // TODO Auto-generated method stub

  }

}