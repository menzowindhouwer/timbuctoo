package nl.knaw.huygens.timbuctoo.storage;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.Reference;
import nl.knaw.huygens.timbuctoo.model.SystemEntity;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.model.BaseDomainEntity;
import test.model.DomainEntityWithDates;
import test.model.DomainEntityWithReferences;
import test.model.TestSystemEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tests the EntityInducer and EntityReducer in the simplest possible way:
 * start with an entity, induce, reduce, giving an entity that should match
 * the original entity.
 */
public class EntityInducerReducerTest {

  private final static String ID = "TEST042";

  private static TypeRegistry registry;

  private EntityInducer inducer;
  private EntityReducer reducer;

  @BeforeClass
  public static void setupRegistry() {
    registry = new TypeRegistry("test.model test.model.projecta test.model.projectb");
  }

  @Before
  public void setup() throws Exception {
    inducer = new EntityInducer();
    reducer = new EntityReducer(registry);
  }

  private void validateEntityProperties(Entity initial, Entity reduced) {
    assertEquals(initial.getId(), reduced.getId());
    assertEquals(initial.getRev(), reduced.getRev());
    assertEquals(initial.getLastChange(), reduced.getLastChange());
    assertEquals(initial.getCreation(), reduced.getCreation());
  }

  private void validateSystemEntityProperties(SystemEntity initial, SystemEntity reduced) {
    validateEntityProperties(initial, reduced);
  }

  private void validateDomainEntityProperties(DomainEntity initial, DomainEntity reduced) {
    validateEntityProperties(initial, reduced);
    assertEquals(initial.getPid(), reduced.getPid());
    assertEquals(initial.getVariations(), reduced.getVariations());
  }

  private void validateBaseDomainEntityProperties(BaseDomainEntity initial, BaseDomainEntity reduced) {
    validateDomainEntityProperties(initial, reduced);
    assertEquals(initial.getValue1(), reduced.getValue1());
    assertEquals(initial.getValue2(), reduced.getValue2());
  }

  // -------------------------------------------------------------------

  @Test
  public void testSystemEntity() throws Exception {
    TestSystemEntity initial = new TestSystemEntity(ID, "v1", "v2", null);

    JsonNode tree = inducer.induceSystemEntity(TestSystemEntity.class, initial);
    TestSystemEntity reduced = reducer.reduceVariation(TestSystemEntity.class, tree);

    validateSystemEntityProperties(initial, reduced);
    assertEquals(initial.getValue1(), reduced.getValue1());
    assertEquals(initial.getValue2(), reduced.getValue2());
    assertEquals(initial.getValue3(), reduced.getValue3());
  }

  @Test
  public void testDomainEntityWithDates() throws Exception {
    long time = new Date().getTime();
    DomainEntityWithDates initial = new DomainEntityWithDates(ID);
    initial.setSharedDate(new Date(time + 1000));
    initial.setUniqueDate(new Date(time + 2000));

    JsonNode tree = inducer.induceDomainEntity(DomainEntityWithDates.class, initial);
    DomainEntityWithDates reduced = reducer.reduceVariation(DomainEntityWithDates.class, tree);

    validateBaseDomainEntityProperties(initial, reduced);
    assertEquals(initial.getSharedDate(), reduced.getSharedDate());
    assertEquals(initial.getUniqueDate(), reduced.getUniqueDate());
  }

  @Test
  public void testDomainEntityWithComplexProperty() throws Exception {
    DomainEntityWithReferences initial = new DomainEntityWithReferences(ID);
    initial.setSharedReference(new Reference("type1", "id1"));
    initial.setUniqueReference(new Reference("type2", "id2"));

    JsonNode tree = inducer.induceDomainEntity(DomainEntityWithReferences.class, initial);
    DomainEntityWithReferences reduced = reducer.reduceVariation(DomainEntityWithReferences.class, tree);

    validateBaseDomainEntityProperties(initial, reduced);
    assertEquals(initial.getSharedReference(), reduced.getSharedReference());
    assertEquals(initial.getUniqueReference(), reduced.getUniqueReference());
  }

}
