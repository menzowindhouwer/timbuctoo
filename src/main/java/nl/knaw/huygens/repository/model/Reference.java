package nl.knaw.huygens.repository.model;

import org.apache.commons.lang.ObjectUtils;

/**
 * A class that represents a link to other objects.
 * @author martijnm
 */
public class Reference {

  private Class<? extends Document> type;
  private String id;
  private String variation;

  // Default constructor for deserializing
  public Reference() {}

  public Reference(Class<? extends Document> type, String id, String variation) {
    this.type = type;
    this.id = id;
    this.variation = variation;
  }

  public Reference(Class<? extends Document> type, String id) {
    this.type = type;
    this.id = id;
  }

  public Class<? extends Document> getType() {
    return type;
  }

  public void setType(Class<? extends Document> objectType) {
    this.type = objectType;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVariation() {
    return variation;
  }

  public void setVariation(String variation) {
    this.variation = variation;
  }

  @Override
  public boolean equals(Object obj) {
    // a null reference will never be an instance of Reference.
    if (obj instanceof Reference) {
      Reference other = (Reference) obj;
      boolean isEqual = ObjectUtils.equals(id, other.id);
      isEqual &= ObjectUtils.equals(variation, other.variation);
      isEqual &= ObjectUtils.equals(type, other.type);
      return isEqual;
    }
    return false;
  }

}
