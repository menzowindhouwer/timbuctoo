package nl.knaw.huygens.timbuctoo.storage;

import static org.junit.Assert.assertEquals;

import java.util.List;

import nl.knaw.huygens.timbuctoo.annotations.IDPrefix;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.Reference;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

public class StorageUtilsTest {

  @Test
  public void testGetIdPrefix() {
    assertEquals(StorageUtils.UNKNOWN_ID_PREFIX, StorageUtils.getIDPrefix(null));
    assertEquals(StorageUtils.UNKNOWN_ID_PREFIX, StorageUtils.getIDPrefix(String.class));
    assertEquals(StorageUtils.UNKNOWN_ID_PREFIX, StorageUtils.getIDPrefix(Entity.class));
    assertEquals("PERS", StorageUtils.getIDPrefix(Person.class));
    assertEquals("PERS", StorageUtils.getIDPrefix(XPerson.class));
  }

  @Test
  public void testFormatEntityId() {
    assertEquals("PERS000000000001", StorageUtils.formatEntityId(Person.class, 1));
    assertEquals("PERS000000001001", StorageUtils.formatEntityId(Person.class, 1001));
    assertEquals("PERS002147483647", StorageUtils.formatEntityId(Person.class, Integer.MAX_VALUE));
    assertEquals("PERS002147483648", StorageUtils.formatEntityId(Person.class, Integer.MAX_VALUE + 1L));
  }

  // -------------------------------------------------------------------

  @IDPrefix("PERS")
  private static class Person extends DomainEntity {
    protected List<Reference> variations = Lists.newArrayList();
    protected String currentVariation;

    @Override
    public String getDisplayName() {
      return null;
    }

    @Override
    @JsonProperty("@variations")
    public List<Reference> getVariations() {
      return variations;
    }

    @Override
    @JsonProperty("@variations")
    public void setVariations(List<Reference> variations) {
      this.variations = variations;
    }

    @Override
    public void addVariation(Class<? extends Entity> refType, String refId) {
      variations.add(new Reference(refType, refId));
    }

    @Override
    @JsonProperty("!currentVariation")
    public String getCurrentVariation() {
      return currentVariation;
    }

    @Override
    @JsonProperty("!currentVariation")
    public void setCurrentVariation(String currentVariation) {
      this.currentVariation = currentVariation;
    }
  }

  private static class XPerson extends Person {}

}
