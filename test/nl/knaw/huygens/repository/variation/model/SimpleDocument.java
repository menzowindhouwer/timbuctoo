package nl.knaw.huygens.repository.variation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.knaw.huygens.repository.indexdata.IndexAnnotation;
import nl.knaw.huygens.repository.model.Document;

public class SimpleDocument extends Document {

  @Override
  @JsonProperty("!currentVariation")
  public String getCurrentVariation() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @JsonProperty("!currentVariation")
  public void setCurrentVariation(String currentVariation) {
    // TODO Auto-generated method stub

  }

  @Override
  @JsonIgnore
  @IndexAnnotation(fieldName = "desc")
  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

}
