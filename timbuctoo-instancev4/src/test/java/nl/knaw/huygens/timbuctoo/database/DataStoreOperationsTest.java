package nl.knaw.huygens.timbuctoo.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import javaslang.control.Try;
import nl.knaw.huygens.timbuctoo.database.dto.CreateEntity;
import nl.knaw.huygens.timbuctoo.database.dto.CreateRelation;
import nl.knaw.huygens.timbuctoo.database.dto.DataStream;
import nl.knaw.huygens.timbuctoo.database.dto.ReadEntity;
import nl.knaw.huygens.timbuctoo.database.dto.UpdateEntity;
import nl.knaw.huygens.timbuctoo.database.dto.UpdateRelation;
import nl.knaw.huygens.timbuctoo.database.dto.dataset.Collection;
import nl.knaw.huygens.timbuctoo.database.dto.dataset.CollectionBuilder;
import nl.knaw.huygens.timbuctoo.database.dto.property.StringProperty;
import nl.knaw.huygens.timbuctoo.database.dto.property.TimProperty;
import nl.knaw.huygens.timbuctoo.database.exceptions.RelationNotPossibleException;
import nl.knaw.huygens.timbuctoo.model.Change;
import nl.knaw.huygens.timbuctoo.model.vre.Vre;
import nl.knaw.huygens.timbuctoo.model.vre.Vres;
import nl.knaw.huygens.timbuctoo.model.vre.vres.VresBuilder;
import nl.knaw.huygens.timbuctoo.server.GraphWrapper;
import org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static nl.knaw.huygens.timbuctoo.model.GraphReadUtils.getProp;
import static nl.knaw.huygens.timbuctoo.model.properties.PropertyTypes.localProperty;
import static nl.knaw.huygens.timbuctoo.util.EdgeMatcher.likeEdge;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsn;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsnO;
import static nl.knaw.huygens.timbuctoo.util.StreamIterator.stream;
import static nl.knaw.huygens.timbuctoo.util.TestGraphBuilder.newGraph;
import static nl.knaw.huygens.timbuctoo.util.VertexMatcher.likeVertex;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class DataStoreOperationsTest {

  @Test
  public void emptyDatabaseIsShownAsEmpty() throws Exception {
    DataStoreOperations instance = new DataStoreOperations(newGraph().wrap(), null, null, null);

    boolean isEmpty = instance.databaseIsEmptyExceptForMigrations();

    assertThat(isEmpty, is(true));
  }

  @Test
  public void nonEmptyDatabaseIsShownAsFull() throws Exception {
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(UUID.randomUUID().toString())
      ).wrap();
    DataStoreOperations instance = new DataStoreOperations(graphWrapper, null, null, null);

    boolean isEmpty = instance.databaseIsEmptyExceptForMigrations();

    assertThat(isEmpty, is(false));
  }

  @Test
  public void ensureVreExistsCreatesAVreIfNeeded() throws Exception {
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(UUID.randomUUID().toString())
      ).wrap();
    DataStoreOperations instance = new DataStoreOperations(graphWrapper, null, null, null);

    instance.ensureVreExists("SomeVre");
    assertThat(
      graphWrapper.getGraph().traversal().V()
                  .has(T.label, LabelP.of(Vre.DATABASE_LABEL))
                  .has(Vre.VRE_NAME_PROPERTY_NAME, "SomeVre")
                  .hasNext(),
      is(true)
    );
  }

  @Test
  public void createEntityAddsAnEntityWithItsPropertiesToTheDatabase() throws Exception {
    GraphWrapper graphWrapper = newGraph().wrap();
    Vres vres = createConfiguration();
    final Collection collection = vres.getCollection("testthings").get();
    final DataStoreOperations instance = new DataStoreOperations(graphWrapper, mock(ChangeListener.class), null, vres);
    List<TimProperty<?>> properties = Lists.newArrayList();
    properties.add(new StringProperty("prop1", "val1"));
    properties.add(new StringProperty("prop2", "val2"));
    CreateEntity createEntity = new CreateEntity(properties);
    UUID id = UUID.randomUUID();
    createEntity.setId(id);
    createEntity.setCreated(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.createEntity(collection, Optional.empty(), createEntity);

    assertThat(graphWrapper.getGraph()
                           .traversal().V()
                           .has("tim_id", id.toString())
                           .has("testthing_prop1", "val1")
                           .has("testthing_prop2", "val2")
                           .hasNext(),
      is(true));
  }

  @Test // TODO move the TimbuctooActions
  public void createEntitySetsTheRevToOne() throws Exception {
    GraphWrapper graphWrapper = newGraph().wrap();
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    DataStoreOperations instance = new DataStoreOperations(graphWrapper, mock(ChangeListener.class), null, vres);
    List<TimProperty<?>> properties = Lists.newArrayList();
    CreateEntity createEntity = new CreateEntity(properties);
    UUID id = UUID.randomUUID();
    createEntity.setId(id);
    createEntity.setCreated(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.createEntity(collection, Optional.empty(), createEntity);

    assertThat(graphWrapper.getGraph()
                           .traversal().V()
                           .has("tim_id", id.toString())
                           .has("rev", 1)
                           .hasNext(),
      is(true));
  }

  @Test
  public void createEntitySetsTypesWithTheCollectionAndTheBaseCollection() throws Exception {
    GraphWrapper graphWrapper = newGraph().wrap();
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    DataStoreOperations instance = new DataStoreOperations(graphWrapper, mock(ChangeListener.class), null, vres);
    List<TimProperty<?>> properties = Lists.newArrayList();
    CreateEntity createEntity = new CreateEntity(properties);
    UUID id = UUID.randomUUID();
    createEntity.setId(id);
    createEntity.setCreated(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.createEntity(collection, Optional.empty(), createEntity);

    assertThat(graphWrapper.getGraph()
                           .traversal().V()
                           .has("tim_id", id.toString())
                           .next().value("types"),
      allOf(containsString("testthing"), containsString("thing"))
    );
  }

  private Vres createConfiguration() {
    return new VresBuilder()
      .withVre("testVre", "test", vre -> vre
        .withCollection("testthings", col -> col
          .withProperty("prop1", localProperty("testthing_prop1"))
          .withProperty("prop2", localProperty("testthing_prop2"))
          .withDisplayName(localProperty("testthing_displayName"))
        )
        .withCollection("teststuffs")
        .withCollection("testrelations", CollectionBuilder::isRelationCollection)
      )
      .withVre("otherVre", "other", vre -> vre
        .withCollection("otherthings", col -> col
          .withProperty("prop1", localProperty("otherthing_prop1"))
          .withProperty("prop2", localProperty("otherthing_prop2"))
        ))
      .build();
  }

  @Test
  public void createEntitySetsCreatedAndModified() throws Exception {
    GraphWrapper graphWrapper = newGraph().wrap();
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    DataStoreOperations instance = new DataStoreOperations(graphWrapper, mock(ChangeListener.class), null, vres);
    List<TimProperty<?>> properties = Lists.newArrayList();
    CreateEntity createEntity = new CreateEntity(properties);
    UUID id = UUID.randomUUID();
    createEntity.setId(id);
    String userId = "userId";
    long timeStamp = Instant.now().toEpochMilli();
    createEntity.setCreated(new Change(timeStamp, userId, null));

    instance.createEntity(collection, Optional.empty(), createEntity);

    assertThat(graphWrapper.getGraph()
                           .traversal().V()
                           .has("tim_id", id.toString())
                           .next().value("created"),
      sameJSONAs(String.format("{\"timeStamp\": %s,\"userId\": \"%s\"}", timeStamp, userId))
    );

    assertThat(graphWrapper.getGraph()
                           .traversal().V()
                           .has("tim_id", id.toString())
                           .next().value("modified"),
      sameJSONAs(String.format("{\"timeStamp\": %s,\"userId\": \"%s\"}", timeStamp, userId))
    );
  }

  @Test
  public void createEntityDuplicatesTheVertex() throws Exception {
    GraphWrapper graphWrapper = newGraph().wrap();
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    DataStoreOperations instance = new DataStoreOperations(graphWrapper, mock(ChangeListener.class), null, vres);
    List<TimProperty<?>> properties = Lists.newArrayList();
    CreateEntity createEntity = new CreateEntity(properties);
    UUID id = UUID.randomUUID();
    createEntity.setId(id);
    String userId = "userId";
    long timeStamp = Instant.now().toEpochMilli();
    createEntity.setCreated(new Change(timeStamp, userId, null));

    instance.createEntity(collection, Optional.empty(), createEntity);

    assertThat(graphWrapper.getGraph().traversal().V().has("tim_id", id.toString()).count().next(), is(2L));
  }

  @Test
  public void createEntityNotifiesTheChangeListener() throws Exception {
    GraphWrapper graphWrapper = newGraph().wrap();
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    ChangeListener changeListener = mock(ChangeListener.class);
    DataStoreOperations instance = new DataStoreOperations(graphWrapper, changeListener, null, vres);
    List<TimProperty<?>> properties = Lists.newArrayList();
    CreateEntity createEntity = new CreateEntity(properties);
    UUID id = UUID.randomUUID();
    createEntity.setId(id);
    String userId = "userId";
    long timeStamp = Instant.now().toEpochMilli();
    createEntity.setCreated(new Change(timeStamp, userId, null));

    instance.createEntity(collection, Optional.empty(), createEntity);

    Vertex vertex =
      graphWrapper.getGraph().traversal().V().has("tim_id", id.toString()).in("VERSION_OF").next();
    verify(changeListener).onCreate(vertex);
  }

  @Test
  public void createEntityMarksOneVertexAsLatest() throws Exception {
    GraphWrapper graphWrapper = newGraph().wrap();
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    ChangeListener changeListener = mock(ChangeListener.class);
    DataStoreOperations instance = new DataStoreOperations(graphWrapper, changeListener, null, vres);
    List<TimProperty<?>> properties = Lists.newArrayList();
    CreateEntity createEntity = new CreateEntity(properties);
    UUID id = UUID.randomUUID();
    createEntity.setId(id);
    String userId = "userId";
    long timeStamp = Instant.now().toEpochMilli();
    createEntity.setCreated(new Change(timeStamp, userId, null));

    instance.createEntity(collection, Optional.empty(), createEntity);


    assertThat(graphWrapper.getGraph()
                           .traversal().V()
                           .has("tim_id", id.toString())
                           .has("isLatest", true)
                           .count().next(),
      is(1L));

  }

  @Test(expected = IOException.class)
  public void createEntityThrowsAnIoExceptionWhenItEncountersAnUnknownProperty() throws Exception {
    GraphWrapper graphWrapper = newGraph().wrap();
    Vres vres = createConfiguration();
    final Collection collection = vres.getCollection("testthings").get();
    final DataStoreOperations instance = new DataStoreOperations(graphWrapper, mock(ChangeListener.class), null, vres);
    List<TimProperty<?>> properties = Lists.newArrayList();
    properties.add(new StringProperty("prop1", "val1"));
    properties.add(new StringProperty("unknowProp", "val2"));
    CreateEntity createEntity = new CreateEntity(properties);
    UUID id = UUID.randomUUID();
    createEntity.setId(id);
    createEntity.setCreated(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.createEntity(collection, Optional.empty(), createEntity);
  }

  // TODO move increase of the rev to TimbuctooActions
  @Test
  public void deleteEntityIncreasesTheRevision() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    UUID id = UUID.randomUUID();
    String idString = id.toString();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(idString)
        .withProperty("isLatest", true)
        .withVre("test")
        .withType("thing")
        .withProperty("rev", 1)
        .withIncomingRelation("VERSION_OF", "orig")
      )
      .withVertex("orig", v -> v
        .withTimId(idString)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", false)
        .withProperty("rev", 1)
      )
      .wrap();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    int rev = instance.deleteEntity(collection, id, new Change(Instant.now().toEpochMilli(), "userId", null));

    assertThat(rev, is(2));
  }

  @Test
  public void deleteEntityRemovesTypeWhenOtherTypesExist() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    UUID id = UUID.randomUUID();
    String idString = id.toString();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(idString)
        .withVre("test")
        .withVre("other")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withLabel("testthing")
        .withLabel("otherthing")
      )
      .wrap();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    instance.deleteEntity(collection, id, new Change(Instant.now().toEpochMilli(), "userId", null));

    String types = (String) graphWrapper.getGraph().traversal().V()
                                        .has("tim_id", idString)
                                        .has("isLatest", true)
                                        .properties("types").value()
                                        .next();

    assertThat(types, is("[\"otherthing\"]"));

    // Type should also be removed from the Neo4j labels
    assertThat(graphWrapper.getGraph().traversal().V()
                           .has("tim_id", idString)
                           .has("isLatest", true)
                           .has(T.label, LabelP.of("testthing")).hasNext(), is(false));

    // Other type should not be removed from the Neo4j labels
    assertThat(graphWrapper.getGraph().traversal().V()
                           .has("tim_id", idString)
                           .has("isLatest", true)
                           .has(T.label, LabelP.of("otherthing")).hasNext(), is(true));
  }

  @Test
  public void deleteEntitySetsDeletedToTrueWhenLastTypeIsRemoved() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    UUID id = UUID.randomUUID();
    String idString = id.toString();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(idString)
        .withProperty("isLatest", true)
        .withVre("test")
        .withType("thing")
        .withProperty("rev", 1)
        .withIncomingRelation("VERSION_OF", "orig")
      )
      .withVertex("orig", v -> v
        .withTimId(idString)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", false)
        .withProperty("rev", 1)
      )
      .wrap();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    instance.deleteEntity(collection, id, new Change(Instant.now().toEpochMilli(), "userId", null));

    assertThat(graphWrapper.getGraph().traversal().V()
                           .has("tim_id", idString)
                           .has("deleted", true).hasNext(), is(true));
  }

  @Test
  public void deleteEntitySetsModified() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    UUID id = UUID.randomUUID();
    String idString = id.toString();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(idString)
        .withProperty("isLatest", true)
        .withVre("test")
        .withType("thing")
        .withProperty("rev", 1)
        .withIncomingRelation("VERSION_OF", "orig")
      )
      .withVertex("orig", v -> v
        .withTimId(idString)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", false)
        .withProperty("rev", 1)
      )
      .wrap();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    long timeStamp = Instant.now().toEpochMilli();
    String userId = "userId";

    instance.deleteEntity(collection, id, new Change(timeStamp, userId, null));

    assertThat(graphWrapper.getGraph().traversal().V()
                           .has("tim_id", idString)
                           .has("deleted", true).next()
                           .value("modified"),
      sameJSONAs(String.format("{\"timeStamp\": %s,\"userId\": \"%s\"}", timeStamp, userId)));
  }

  @Test
  public void deleteEntityPreparesBackupCopyAfterMakingChanges() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    UUID id = UUID.randomUUID();
    String idString = id.toString();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(idString)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withIncomingRelation("VERSION_OF", "orig")
      )
      .withVertex("orig", v -> v
        .withTimId(idString)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", false)
        .withProperty("rev", 1)
      )
      .wrap();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);


    Vertex beforeUpdate = graphWrapper.getGraph().traversal().V()
                                      .has("tim_id", idString)
                                      .has("isLatest", true)
                                      .next();

    instance.deleteEntity(collection, id, new Change(Instant.now().toEpochMilli(), "userId", null));

    Vertex afterUpdate = graphWrapper.getGraph().traversal().V()
                                     .has("tim_id", idString)
                                     .has("isLatest", true)
                                     .next();

    assertThat(afterUpdate.id(), is(not(beforeUpdate.id())));
    //single edge, containing the VERSION_OF pointer
    assertThat(afterUpdate.edges(Direction.IN).next().outVertex().id(), is(beforeUpdate.id()));
  }

  @Test
  public void deleteEntityNotifiesTheChangeListenerBeforeDuplicatingTheVertex() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    UUID id = UUID.randomUUID();
    String idString = id.toString();
    GraphWrapper graphWrapper = newGraph()
      .withVertex("orig", v -> v
        .withTimId(idString)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", false)
        .withProperty("rev", 1)
      )
      .withVertex(v -> v
        .withTimId(idString)
        .withProperty("isLatest", true)
        .withVre("test")
        .withType("thing")
        .withProperty("rev", 1)
        .withIncomingRelation("VERSION_OF", "orig")
      ).wrap();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    ChangeListener changeListener = mock(ChangeListener.class);
    DataStoreOperations instance = new DataStoreOperations(graphWrapper, changeListener, entityFetcher, vres);

    instance.deleteEntity(collection, id, new Change(Instant.now().toEpochMilli(), "userId", null));

    Vertex latestAfterDuplication = graphWrapper.getGraph()
                                                .traversal().V()
                                                .has("tim_id", idString).has("isLatest", true).next();
    Vertex newVertex = latestAfterDuplication.vertices(Direction.IN, "VERSION_OF").next();
    Vertex oldVertex = newVertex.vertices(Direction.IN, "VERSION_OF").next();
    verify(changeListener).onUpdate(Optional.of(oldVertex), newVertex);
  }

  @Test
  public void deleteEntityMovesRelationsToNewestVertex() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    UUID id = UUID.randomUUID();
    String idString = id.toString();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(idString)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withOutgoingRelation("hasWritten", "stuff")
        .withIncomingRelation("isFriendOf", "friend")
      )
      .withVertex("stuff", v -> v
        .withVre("test")
        .withType("stuff")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      )
      .withVertex("friend", v -> v
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      )
      .wrap();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    Vertex orig = graphWrapper.getGraph().traversal().V().has("tim_id", idString).has("isLatest", true).next();
    assertThat(stream(orig.edges(Direction.BOTH, "hasWritten", "isFriendOf")).count(), is(2L));

    instance.deleteEntity(collection, id, new Change(Instant.now().toEpochMilli(), "userId", null));

    Vertex replacement = graphWrapper.getGraph().traversal().V().has("tim_id", idString).has("isLatest", true).next();
    assertThat(stream(orig.edges(Direction.BOTH, "hasWritten", "isFriendOf")).count(), is(0L));
    assertThat(stream(replacement.edges(Direction.BOTH, "hasWritten", "isFriendOf")).count(), is(2L));
  }

  @Test
  public void deletesAllRelationsOfCurrentVre() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    UUID id = UUID.randomUUID();
    String idString = id.toString();
    final String testOnlyId = "10000000-0000-0000-0000-000000000000";
    final String otherOnlyId = "20000000-0000-0000-0000-000000000000";
    final String inBothId = "30000000-0000-0000-0000-000000000000";
    GraphWrapper graphManager = newGraph()
      .withVertex(v -> v
        .withTimId(idString)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withOutgoingRelation("hasWritten", "stuff", rel -> rel
          .withTim_id(UUID.fromString(testOnlyId))
          .removeType("other")
          .withAccepted("testrelation", true)
        )
        .withIncomingRelation("isFriendOf", "friend", rel -> rel
          .withTim_id(UUID.fromString(inBothId))
          .withAccepted("testrelation", true)
          .withAccepted("otherrelation", true)
        )
        .withIncomingRelation("isFriendOf", "friend", rel -> rel
          .withTim_id(UUID.fromString(otherOnlyId))
          .removeType("test")
          .withAccepted("otherrelation", true)
        )
      )
      .withVertex("stuff", v -> v
        .withVre("test")
        .withType("stuff")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      )
      .withVertex("friend", v -> v
        .withVre("test")
        .withVre("other")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      )
      .wrap();

    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    DataStoreOperations instance =
      new DataStoreOperations(graphManager, mock(ChangeListener.class), entityFetcher, vres);

    instance.deleteEntity(collection, id, new Change(Instant.now().toEpochMilli(), "userId", null));

    assertThat(graphManager.getGraph().traversal().E()
                           .has("tim_id", testOnlyId)
                           .has("isLatest", true)
                           .has("testrelation_accepted", false).not(has("otherrelation_accepted"))
                           .hasNext(), is(true));
    assertThat(graphManager.getGraph().traversal().E()
                           .has("tim_id", inBothId)
                           .has("isLatest", true)
                           .has("testrelation_accepted", false).has("otherrelation_accepted", true)
                           .hasNext(), is(true));
    assertThat(graphManager.getGraph().traversal().E()
                           .has("tim_id", otherOnlyId)
                           .has("isLatest", true)
                           .not(has("testrelation_accepted")).has("otherrelation_accepted", true)
                           .hasNext(), is(true));

  }

  @Test(expected = NotFoundException.class)
  public void deleteEntityThrowsNotFoundWhenTheEntityIsNotOfThisVre() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    UUID id = UUID.randomUUID();
    String idString = id.toString();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(idString)
        .withVre("other")
        .withType("person")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withProperty("deleted", false)
      )
      .wrap();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    instance.deleteEntity(collection, id, null);
  }

  @Test(expected = NotFoundException.class)
  public void deleteEntitythrowsNotFoundWhenTheIdIsNotInTheDatabase() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph().wrap();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    instance.deleteEntity(collection, id, null);
  }

  @Test
  public void getEntityMapsTheId() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getId(), is(id));
  }

  @Test
  public void getEntityOnlyMapsTheKnownProperties() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withType("thing")
        .withVre("test")
        .withProperty("testthing_prop1", "val1")
        .withProperty("testthing_prop2", "val2")
        .withProperty("testthing_unknownProp", "val")
      )
      .wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getProperties(), containsInAnyOrder(
      allOf(hasProperty("name", equalTo("prop1")), hasProperty("value", equalTo("val1"))),
      allOf(hasProperty("name", equalTo("prop2")), hasProperty("value", equalTo("val2")))
    ));
    assertThat(entity.getProperties(), not(contains(
      allOf(hasProperty("name", equalTo("unknownProp")), hasProperty("value", equalTo("val")))
    )));
  }

  @Test
  public void getEntityIgnoresThePropertiesWithAWrongValue() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withType("thing")
        .withVre("test")
        .withProperty("testthing_prop2", 42) // should be a string
      )
      .wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getProperties(), emptyIterable());
  }

  @Test
  public void getEntityReturnsTheLatestEntityIfTheRefIsNull() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("testthing_prop1", "old")
        .withProperty("rev", 1)
        .withProperty("isLatest", false)
        .withOutgoingRelation("VERSION_OF", "replacement")
      )
      .withVertex("replacement", v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("testthing_prop1", "new")
        .withProperty("rev", 2)
        .withProperty("isLatest", false)
        .withOutgoingRelation("VERSION_OF", "dangling")
      )
      .withVertex("dangling", v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("testthing_prop1", "new")
        .withProperty("rev", 2)
        .withProperty("isLatest", true)
      )
      .wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getRev(), is(2));
    assertThat(entity.getProperties(), contains(
      allOf(hasProperty("name", equalTo("prop1")), hasProperty("value", equalTo("new")))
    ));
  }

  @Test
  public void getEntityReturnsTheSpecifiedRevision() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("testthing_prop1", "old")
        .withProperty("rev", 1)
        .withProperty("isLatest", false)
        .withOutgoingRelation("VERSION_OF", "replacement")
      )
      .withVertex("replacement", v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("testthing_prop1", "new")
        .withProperty("rev", 2)
        .withProperty("isLatest", false)
        .withOutgoingRelation("VERSION_OF", "dangling")
      )
      .withVertex("dangling", v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("testthing_prop1", "new")
        .withProperty("rev", 2)
        .withProperty("isLatest", true)
      )
      .wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, 1, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getRev(), is(1));
    assertThat(entity.getProperties(), contains(
      allOf(hasProperty("name", equalTo("prop1")), hasProperty("value", equalTo("old")))
    ));
  }

  @Test(expected = NotFoundException.class)
  public void getEntityThrowsANotFoundExceptionWhenTheDatabaseDoesNotContainTheEntity() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph().wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );
  }

  @Test
  public void getEntityReturnsTheRelations() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    UUID relatedId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex("source", v -> v
        .withOutgoingRelation("isRelatedTo", "relatedThing")
        .withVre("test")
        .withVre("")
        .withType("thing")
        .isLatest(true)
        .withTimId(id.toString())
      )
      .withVertex("relatedThing", v -> v
        .withVre("test")
        .withVre("")
        .withType("thing")
        .withTimId(relatedId.toString())
        .withProperty("testthing_displayName", "displayName")
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getRelations(), contains(
      allOf(
        hasProperty("entityId", equalTo(relatedId.toString())),
        hasProperty("collectionName", equalTo("testthings")),
        hasProperty("entityType", equalTo("testthing")),
        hasProperty("relationType", equalTo("isRelatedTo")),
        hasProperty("displayName", equalTo("displayName"))
      )
    ));
  }

  @Test
  public void getEntityUsesTheInverseRelationName() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    UUID stuffId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex("source", v -> v
        .withIncomingRelation("isCreatedBy", "stuff")
        .withVre("test")
        .withVre("")
        .withType("thing")
        .isLatest(true)
        .withTimId(id.toString())
      )
      .withVertex("stuff", v -> v
        .withVre("test")
        .withVre("")
        .withType("stuff")
        .withTimId(stuffId.toString())
      )
      .withVertex(v -> v
        .withProperty("relationtype_regularName", "isCreatedBy")
        .withProperty("relationtype_inverseName", "isCreatorOf")
      )
      .withVertex(v -> v
        .withProperty("relationtype_regularName", "otherRelationType")
        .withProperty("relationtype_inverseName", "otherInverseType")
      )
      .wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getRelations(), contains(
      allOf(hasProperty("entityId", equalTo(stuffId.toString())), hasProperty("relationType", equalTo("isCreatorOf")))
    ));
  }

  @Test
  public void getEntityOmitsDeletedRelations() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    UUID relatedId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex("source", v -> v
        .withOutgoingRelation("isRelatedTo", "relatedThing", rel -> rel.withDeleted(true))
        .withOutgoingRelation("hasOtherRelationWith", "relatedThing")
        .withVre("test")
        .withVre("")
        .withType("thing")
        .isLatest(true)
        .withTimId(id.toString())
      )
      .withVertex("relatedThing", v -> v
        .withVre("test")
        .withVre("")
        .withType("thing")
        .withTimId(relatedId.toString())
        .withProperty("testthing_displayName", "displayName")
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getRelations(), hasSize(1));
    assertThat(entity.getRelations(), contains(hasProperty("relationType", equalTo("hasOtherRelationWith"))));
  }

  @Test
  public void getEntityOnlyReturnsTheLatestRelations() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    UUID relatedId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex("source", v -> v
        .withOutgoingRelation("isRelatedTo", "relatedThing", rel -> rel.withIsLatest(true).withRev(2))
        .withOutgoingRelation("isRelatedTo", "relatedThing", rel -> rel.withIsLatest(false).withRev(1))
        .withVre("test")
        .withVre("")
        .withType("thing")
        .isLatest(true)
        .withTimId(id.toString())
      )
      .withVertex("relatedThing", v -> v
        .withVre("test")
        .withVre("")
        .withType("thing")
        .withTimId(relatedId.toString())
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getRelations(), hasSize(1));
    assertThat(entity.getRelations(), contains(
      allOf(
        hasProperty("entityId", equalTo(relatedId.toString())),
        hasProperty("relationType", equalTo("isRelatedTo")),
        hasProperty("relationRev", equalTo(2))
      )
    ));
  }

  @Test
  public void getEntityOnlyReturnsAcceptedRelations() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    UUID relatedId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex("source", v -> v
        .withOutgoingRelation(
          "isRelatedTo",
          "relatedThing",
          rel -> rel.withIsLatest(true).withAccepted("testrelation", true)
        )
        .withOutgoingRelation(
          "hasOtherRelation",
          "relatedThing",
          rel -> rel.withIsLatest(true).withAccepted("testrelation", false)
        )
        .withVre("test")
        .withVre("")
        .withType("thing")
        .isLatest(true)
        .withTimId(id.toString())
      )
      .withVertex("relatedThing", v -> v
        .withVre("test")
        .withVre("")
        .withType("thing")
        .withTimId(relatedId.toString())
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getRelations(), hasSize(1));
    assertThat(entity.getRelations(), contains(hasProperty("relationType", equalTo("isRelatedTo"))));
  }

  @Test
  public void getEntityReturnsTheTypesOfTheEntity() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withProperty("types", "[\"testthing\", \"otherthing\"]")
      )
      .wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getTypes(), containsInAnyOrder("testthing", "otherthing"));
  }

  @Test(expected = NotFoundException.class)
  public void getEntityThrowsNotFoundIfTheEntityDoesNotContainTheRequestType() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("other")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );
  }

  @Test(expected = NotFoundException.class)
  public void getEntityThrowsNotFoundIfTheEntityIsDeleted() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("isLatest", true)
        .withProperty("deleted", true)
        .withProperty("rev", 1)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );
  }

  @Test
  public void getEntityAlwaysReturnsTheDeletedProperty() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getDeleted(), is(false));
  }

  @Test
  public void getEntityReturnsThePid() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withProperty("pid", "pidValue")
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getPid(), is("pidValue"));
  }

  @Test
  public void getEntityReturnsANullPidWhenTheEntityDoesNotContainOne() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withType("thing")
        .withVre("test")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    ReadEntity entity = instance.getEntity(id, null, collection,
      (readEntity, vertex) -> {
      },
      (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    assertThat(entity.getPid(), is(nullValue()));
  }

  @Test
  public void getCollectionReturnsAllTheLatestEntitiesOfACollection() {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    UUID id3 = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withLabel("testthing")
        .withType("thing")
        .withVre("test")
        .isLatest(true)
        .withTimId(id1.toString())
      )
      .withVertex(v -> v
        .withLabel("testthing")
        .withType("thing")
        .withVre("test")
        .isLatest(true)
        .withTimId(id2.toString())
      )
      .withVertex(v -> v
        .withLabel("testthing")
        .withType("thing")
        .withVre("test")
        .isLatest(false)
        .withTimId(id2.toString())
      )
      .withVertex(v -> v
        .withLabel("testthing")
        .withType("thing")
        .withVre("test")
        .isLatest(true)
        .withTimId(id3.toString())
      )
      .withVertex(v -> v
        .withLabel("teststuff")
        .withType("stuff")
        .withVre("test")
        .isLatest(true)
        .withTimId(UUID.randomUUID().toString())
      )
      .wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    DataStream<ReadEntity> entities = instance.getCollection(
      collection, 0, 3, false,
      (readEntity, vertex) -> {
      }, (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );


    List<UUID> ids = entities.map(ReadEntity::getId);
    assertThat(ids, hasSize(3));
    assertThat(ids, containsInAnyOrder(id1, id2, id3));
  }

  @Test
  public void getCollectionReturnsRelationsIfRequested() {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID thingId = UUID.randomUUID();
    UUID stuffId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex("v1", v -> v
        .withLabel("testthing")
        .withVre("test")
        .withType("thing")
        .isLatest(true)
        .withTimId(thingId.toString())
        .withOutgoingRelation("isCreatorOf", "stuff")
      )
      .withVertex("stuff", v -> v
        .withVre("test")
        .withType("stuff")
        .withTimId(stuffId.toString())
      )
      .withVertex(v -> v
        .withProperty("relationtype_regularName", "isCreatedBy")
        .withProperty("relationtype_inverseName", "isCreatorOf")
      )
      .wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    DataStream<ReadEntity> entities = instance.getCollection(collection, 0, 1, true,
      (readEntity, vertex) -> {
      }, (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    ReadEntity readEntity = entities.map(entity -> entity).get(0);
    assertThat(readEntity.getRelations(), contains(
      allOf(
        hasProperty("entityId", equalTo(stuffId.toString())),
        hasProperty("collectionName", equalTo("teststuffs")),
        hasProperty("entityType", equalTo("teststuff")),
        hasProperty("relationType", equalTo("isCreatorOf"))
      )
    ));
  }

  @Test
  public void getCollectionReturnsTheKnowsDisplayNameForEachItem() {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withLabel("testthing")
        .withVre("test")
        .withType("thing")
        .isLatest(true)
        .withTimId(id1.toString())
        .withProperty("testthing_displayName", "displayName1") // configured in JsonCrudServiceBuilder
      )
      .withVertex(v -> v
        .withLabel("testthing")
        .withVre("test")
        .withType("thing")
        .isLatest(true)
        .withTimId(id2.toString())
        .withProperty("testthing_displayName", "displayName2") // configured in JsonCrudServiceBuilder
      )
      .wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);

    DataStream<ReadEntity> entities = instance.getCollection(collection, 0, 2, true,
      (readEntity, vertex) -> {
      }, (graphTraversalSource, vre, vertex, relationRef) -> {
      }
    );

    List<String> displayNames = entities.map(ReadEntity::getDisplayName);
    assertThat(displayNames, containsInAnyOrder("displayName1", "displayName2"));
  }

  @Test
  public void replaceEntityUpdatesTheRevisionByOne() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withIncomingRelation("VERSION_OF", "orig")
      )
      .withVertex("orig", v -> v
        .withTimId(id.toString())
        .withProperty("isLatest", false)
        .withProperty("rev", 1)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    UpdateEntity updateEntity = new UpdateEntity(id, Lists.newArrayList(), 1);
    updateEntity.setModified(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.replaceEntity(collection, updateEntity);

    int rev = (int) graphWrapper.getGraph().traversal().V()
                                .has("tim_id", id.toString()).has("isLatest", true)
                                .values("rev").next();
    assertThat(rev, is(2));
  }

  @Test
  public void replaceEntityAddsAType() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id)
        .withVre("other")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withProperty("otherthing_prop1", "the name")
        .withIncomingRelation("VERSION_OF", "orig")
      )
      .withVertex("orig", v -> v
        .withTimId(id)
        .withVre("other")
        .withType("thing")
        .withProperty("isLatest", false)
        .withProperty("rev", 1)
        .withProperty("otherthing_prop1", "the name")
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    UpdateEntity updateEntity = new UpdateEntity(id, Lists.newArrayList(), 1);
    updateEntity.setModified(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.replaceEntity(collection, updateEntity);

    String types = (String) graphWrapper.getGraph().traversal().V()
                                        .has("tim_id", id.toString())
                                        .has("isLatest", true)
                                        .values("types")
                                        .next();
    assertThat(types, containsString("\"testthing\""));
  }

  @Test
  public void replaceEntityCallTheChangeListenerWithAVertexWithoutPid() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id)
        .withVre("other")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withProperty("otherthing_prop1", "the name")
        .withIncomingRelation("VERSION_OF", "orig")
      )
      .withVertex("orig", v -> v
        .withTimId(id)
        .withVre("other")
        .withType("thing")
        .withProperty("isLatest", false)
        .withProperty("rev", 1)
        .withProperty("otherthing_prop1", "the name")
      ).wrap();
    ChangeListener changeListener = mock(ChangeListener.class);
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, changeListener, entityFetcher, vres);
    UpdateEntity updateEntity = new UpdateEntity(id, Lists.newArrayList(), 1);
    updateEntity.setModified(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.replaceEntity(collection, updateEntity);

    verify(changeListener).onUpdate(
      argThat(is(instanceOf(Optional.class))),
      argThat(likeVertex().withTimId(id.toString()).withProperty("rev", 2).withoutProperty("pid"))
    );
  }

  @Test
  public void replaceEntityUpdatesModified() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    UpdateEntity updateEntity = new UpdateEntity(id, Lists.newArrayList(), 1);
    long timeStamp = Instant.now().toEpochMilli();
    updateEntity.setModified(new Change(timeStamp, "userId", null));

    instance.replaceEntity(collection, updateEntity);

    String modified = (String) graphWrapper.getGraph().traversal().V()
                                           .has("tim_id", id.toString())
                                           .has("isLatest", true)
                                           .properties("modified").value()
                                           .next();
    assertThat(
      modified,
      sameJSONAs(String.format("{\"timeStamp\": %s,\"userId\": \"%s\"}", timeStamp, "userId"))
    );
  }

  @Test
  public void replaceEntityUpdatesTheKnownProperties() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id)
        .withVre("test")
        .withType("thing")
        .withProperty("testthing_prop1", "oldValue")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    ArrayList<TimProperty<?>> properties = Lists.newArrayList(
      new StringProperty("prop1", "newValue"),
      new StringProperty("prop2", "prop2Value")
    );
    UpdateEntity updateEntity = new UpdateEntity(id, properties, 1);
    long timeStamp = Instant.now().toEpochMilli();
    updateEntity.setModified(new Change(timeStamp, "userId", null));

    instance.replaceEntity(collection, updateEntity);

    Vertex vertex = graphWrapper.getGraph().traversal().V().has("tim_id", id.toString()).has("isLatest", true).next();
    assertThat(vertex, is(
      likeVertex().withProperty("testthing_prop1", "newValue").withProperty("testthing_prop2", "prop2Value")
    ));
  }

  @Test
  public void replaceEntityRemovesThePropertiesThatAreNotProvided() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id)
        .withVre("test")
        .withType("thing")
        .withProperty("testthing_prop1", "oldValue1")
        .withProperty("testthing_prop2", "oldValue2")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    ArrayList<TimProperty<?>> properties = Lists.newArrayList(
      new StringProperty("prop1", "newValue")
    );
    UpdateEntity updateEntity = new UpdateEntity(id, properties, 1);
    long timeStamp = Instant.now().toEpochMilli();
    updateEntity.setModified(new Change(timeStamp, "userId", null));

    instance.replaceEntity(collection, updateEntity);

    Vertex vertex = graphWrapper.getGraph().traversal().V().has("tim_id", id.toString()).has("isLatest", true).next();
    assertThat(vertex, is(likeVertex().withoutProperty("testthing_prop2")));
  }

  @Test
  public void replaceEntityPreparesABackupCopyAfterMakingTheChanges() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    List<TimProperty<?>> properties = Lists.newArrayList(
      new StringProperty("prop1", "newValue")
    );
    UpdateEntity updateEntity = new UpdateEntity(id, properties, 1);
    long timeStamp = Instant.now().toEpochMilli();
    updateEntity.setModified(new Change(timeStamp, "userId", null));

    Vertex beforeUpdate = graphWrapper.getGraph().traversal().V()
                                      .has("tim_id", id.toString())
                                      .has("isLatest", true)
                                      .next();

    instance.replaceEntity(collection, updateEntity);

    Vertex afterUpdate = graphWrapper.getGraph().traversal().V()
                                     .has("tim_id", id.toString())
                                     .has("isLatest", true)
                                     .next();

    assertThat(afterUpdate.id(), is(not(beforeUpdate.id())));
    assertThat(afterUpdate.edges(Direction.IN).next().outVertex().id(), is(beforeUpdate.id()));
  }

  @Test
  public void replaceEntityMovesTheRelationsToTheNewVertex() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 1)
        .withOutgoingRelation("hasWritten", "stuff")
        .withIncomingRelation("isFriendOf", "friend")
      )
      .withVertex("stuff", v -> v
        .withVre("test")
        .withType("stuff")
        .isLatest(true)
        .withProperty("rev", 1)
      )
      .withVertex("friend", v -> v
        .withVre("test")
        .withType("thing")
        .isLatest(true)
        .withProperty("rev", 1)
      )
      .wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    List<TimProperty<?>> properties = Lists.newArrayList(
      new StringProperty("prop1", "newValue")
    );
    UpdateEntity updateEntity = new UpdateEntity(id, properties, 1);
    long timeStamp = Instant.now().toEpochMilli();
    updateEntity.setModified(new Change(timeStamp, "userId", null));

    Vertex origVertex = graphWrapper.getGraph().traversal().V()
                                    .has("tim_id", id.toString())
                                    .has("isLatest", true)
                                    .next();
    assertThat(stream(origVertex.edges(Direction.BOTH, "hasWritten", "isFriendOf")).count(), is(2L));

    instance.replaceEntity(collection, updateEntity);

    Vertex newVertex = graphWrapper.getGraph().traversal().V()
                                   .has("tim_id", id.toString())
                                   .has("isLatest", true)
                                   .next();
    assertThat(stream(origVertex.edges(Direction.BOTH, "hasWritten", "isFriendOf")).count(), is(0L));
    assertThat(stream(newVertex.edges(Direction.BOTH, "hasWritten", "isFriendOf")).count(), is(2L));
  }

  @Test(expected = NotFoundException.class)
  public void replaceEntityThrowsANotFoundExceptionWhenTheEntityIsNotInTheDatabase() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper emptyDatabase = newGraph().wrap();
    DataStoreOperations instance =
      new DataStoreOperations(emptyDatabase, mock(ChangeListener.class), entityFetcher, vres);
    UpdateEntity updateEntity = new UpdateEntity(id, Lists.newArrayList(), 1);
    long timeStamp = Instant.now().toEpochMilli();
    updateEntity.setModified(new Change(timeStamp, "userId", null));

    instance.replaceEntity(collection, updateEntity);
  }

  @Test(expected = AlreadyUpdatedException.class)
  public void replaceEntityThrowsAnAlreadyUpdatedExceptionWhenTheRevDoesNotMatch() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testthings").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withProperty("rev", 2)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    UpdateEntity updateEntity = new UpdateEntity(id, Lists.newArrayList(), 1);
    long timeStamp = Instant.now().toEpochMilli();
    updateEntity.setModified(new Change(timeStamp, "userId", null));

    instance.replaceEntity(collection, updateEntity);
  }

  @Test
  public void acceptRelationCreatesANewRelationWhenItDoesNotExist() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testrelations").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID typeId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(typeId.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "regularName")
        .withProperty("rev", 1)
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(sourceId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(targetId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    CreateRelation createRelation = new CreateRelation(sourceId, typeId, targetId);
    createRelation.setCreated(new Change(Instant.now().toEpochMilli(), "userId", null));

    UUID relId = instance.acceptRelation(collection, createRelation);

    assertThat(graphWrapper.getGraph().traversal().E().has("tim_id", relId.toString()).next(), is(likeEdge()
      .withSourceWithId(sourceId)
      .withTargetWithId(targetId)
      .withTypeId(typeId)
    ));
  }

  @Test
  public void acceptRelationSetsTheCreatedInformation() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testrelations").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID typeId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(typeId.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "regularName")
        .withProperty("rev", 1)
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(sourceId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(targetId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    CreateRelation createRelation = new CreateRelation(sourceId, typeId, targetId);
    long timeStamp = Instant.now().toEpochMilli();
    String userId = "userId";
    createRelation.setCreated(new Change(timeStamp, userId, null));

    UUID relId = instance.acceptRelation(collection, createRelation);

    Edge newEdge = graphWrapper.getGraph().traversal().E().has("tim_id", relId.toString()).next();
    assertThat(getModificationInfo("modified", newEdge), is(jsnO(
      "timeStamp", jsn(timeStamp),
      "userId", jsn(userId)
    )));
    assertThat(getModificationInfo("created", newEdge), is(jsnO(
      "timeStamp", jsn(timeStamp),
      "userId", jsn(userId)
    )));
  }

  private ObjectNode getModificationInfo(String prop, Element elm) {
    return getProp(elm, prop, String.class)
      .map(data -> Try.of(() -> (ObjectNode) new ObjectMapper().readTree(data)))
      .orElse(Try.success(null))
      .get();
  }

  @Test(expected = RelationNotPossibleException.class)
  public void acceptRelationThrowsARelationNotPossibleExceptionIfTheSourceIsNotInTheRightVre() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testrelations").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID typeId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(typeId.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "regularName")
        .withProperty("rev", 1)
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(sourceId.toString())
        .withProperty("rev", 1)
        .withVre("other")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(targetId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      ).wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    CreateRelation createRelation = new CreateRelation(sourceId, typeId, targetId);
    createRelation.setCreated(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.acceptRelation(collection, createRelation);
  }

  @Test(expected = RelationNotPossibleException.class)
  public void acceptRelationThrowsARelationNotPossibleExceptionIfTheTargetIsNotInTheRightVre() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testrelations").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID typeId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(typeId.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "regularName")
        .withProperty("rev", 1)
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(sourceId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(targetId.toString())
        .withProperty("rev", 1)
        .withVre("other")
        .withType("thing")
        .withProperty("isLatest", true)
      ).wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    CreateRelation createRelation = new CreateRelation(sourceId, typeId, targetId);
    createRelation.setCreated(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.acceptRelation(collection, createRelation);
  }

  @Test(expected = RelationNotPossibleException.class)
  public void acceptRelationsThrowsARelationNotPossibleExceptionIfTheTypeOfTheSourceIsInvalid() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testrelations").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID typeId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(typeId.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "isRelatedTo")
        .withProperty("relationtype_sourceTypeName", "teststuff")
        .withProperty("relationtype_targeTypeName", "testthing")
        .withProperty("rev", 1)
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(sourceId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(targetId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      ).wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    CreateRelation createRelation = new CreateRelation(sourceId, typeId, targetId);
    createRelation.setCreated(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.acceptRelation(collection, createRelation);
  }

  @Test(expected = RelationNotPossibleException.class)
  public void acceptRelationsThrowsARelationNotPossibleExceptionIfTheTypeOfTheTargetIsInvalid() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testrelations").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID typeId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(typeId.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "isRelatedTo")
        .withProperty("relationtype_sourceTypeName", "testthing")
        .withProperty("relationtype_targeTypeName", "teststuff")
        .withProperty("rev", 1)
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(sourceId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(targetId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      ).wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    CreateRelation createRelation = new CreateRelation(sourceId, typeId, targetId);
    createRelation.setCreated(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.acceptRelation(collection, createRelation);
  }

  @Test
  public void replaceRelationUpdatesTheExistingRelation() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testrelations").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID typeId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    UUID relId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(typeId.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "regularName")
        .withProperty("rev", 1)
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(sourceId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withOutgoingRelation("regularName", "otherVertex", r -> r
          .withTim_id(relId)
          .withAccepted("testrelation", true)
          .withTypeId(typeId)
          .withRev(1)
          .addType("testrelation")
        )
      )
      .withVertex("otherVertex", v -> v
        .withTimId(targetId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    UpdateRelation updateRelation = new UpdateRelation(relId, 1, false);
    updateRelation.setModified(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.replaceRelation(collection, updateRelation);

    assertThat(graphWrapper.getGraph().traversal().E().has("tim_id", relId.toString()).has("isLatest", true).next(),
      is(likeEdge()
        .withSourceWithId(sourceId)
        .withTargetWithId(targetId)
        .withTypeId(typeId)
        .withProperty("testrelation_accepted", false)
      ));
  }

  @Test
  public void replaceRelationUpdatesTheModifiedInformation() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testrelations").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID typeId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    UUID relId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(typeId.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "regularName")
        .withProperty("rev", 1)
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(sourceId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withOutgoingRelation("regularName", "otherVertex", r -> r
          .withTim_id(relId)
          .withAccepted("testrelation", true)
          .withTypeId(typeId)
          .withRev(1)
          .addType("testrelation")
        )
      )
      .withVertex("otherVertex", v -> v
        .withTimId(targetId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    UpdateRelation updateRelation = new UpdateRelation(relId, 1, false);
    long timeStamp = Instant.now().toEpochMilli();
    String userId = "userId";
    updateRelation.setModified(new Change(timeStamp, userId, null));

    instance.replaceRelation(collection, updateRelation);

    Edge newEdge = graphWrapper.getGraph().traversal().E().has("tim_id", relId.toString()).has("isLatest", true).next();
    assertThat(getModificationInfo("modified", newEdge), is(jsnO(
      "timeStamp", jsn(timeStamp),
      "userId", jsn(userId)
    )));
  }

  @Test
  public void replaceRelationDuplicatesTheEdge() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testrelations").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID typeId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    UUID relId = UUID.randomUUID();
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(typeId.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "regularName")
        .withProperty("rev", 1)
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(sourceId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withOutgoingRelation("regularName", "otherVertex", r -> r
          .withTim_id(relId)
          .withAccepted("testrelation", true)
          .withTypeId(typeId)
          .withRev(1)
          .addType("testrelation")
        )
      )
      .withVertex("otherVertex", v -> v
        .withTimId(targetId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    UpdateRelation updateRelation = new UpdateRelation(relId, 1, false);
    long timeStamp = Instant.now().toEpochMilli();
    String userId = "userId";
    updateRelation.setModified(new Change(timeStamp, userId, null));

    instance.replaceRelation(collection, updateRelation);

    assertThat(graphWrapper.getGraph().traversal().E().has("tim_id", relId.toString()).count().next(), is(2L));
  }

  @Test
  public void replaceRelationUpdatesTheRevisionByOne() throws Exception {
    Vres vres = createConfiguration();
    Collection collection = vres.getCollection("testrelations").get();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID typeId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    UUID relId = UUID.randomUUID();
    int relationRev = 1;
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(typeId.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "regularName")
        .withProperty("rev", 1)
        .withProperty("isLatest", true)
      )
      .withVertex(v -> v
        .withTimId(sourceId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
        .withOutgoingRelation("regularName", "otherVertex", r -> r
          .withTim_id(relId)
          .withAccepted("testrelation", true)
          .withTypeId(typeId)
          .withRev(relationRev)
          .addType("testrelation")
        )
      )
      .withVertex("otherVertex", v -> v
        .withTimId(targetId.toString())
        .withProperty("rev", 1)
        .withVre("test")
        .withType("thing")
        .withProperty("isLatest", true)
      )
      .wrap();

    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    UpdateRelation updateRelation = new UpdateRelation(relId, 1, false);
    updateRelation.setModified(new Change(Instant.now().toEpochMilli(), "userId", null));

    instance.replaceRelation(collection, updateRelation);

    assertThat(graphWrapper.getGraph().traversal().E().has("tim_id", relId.toString()).has("isLatest", true).next(), is(
      likeEdge().withProperty("rev", relationRev + 1))
    );
  }

  @Test
  public void addPidAddsAPidToEachVertexInTheCollectionWithTheIdAndRev() throws Exception {
    Vres vres = createConfiguration();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    int rev = 1;
    GraphWrapper graphWrapper = newGraph()
      .withVertex(v -> v
        .withTimId(id.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "regularName")
        .withProperty("rev", rev)
        .withProperty("isLatest", true)
        .withIncomingRelation("VERSION_OF", "version")
      ).withVertex("version", v -> v
        .withTimId(id.toString())
        .withType("relationtype")
        .withProperty("relationtype_regularName", "regularName")
        .withProperty("rev", rev)
        .withProperty("isLatest", false)
      ).wrap();
    DataStoreOperations instance =
      new DataStoreOperations(graphWrapper, mock(ChangeListener.class), entityFetcher, vres);
    URI pidUri = new URI("http://example.com/pid");

    instance.addPid(id, rev, pidUri);

    assertThat(graphWrapper.getGraph().traversal().V().has("pid", pidUri.toString()).count().next(), is(2L));
  }

  @Test(expected = NotFoundException.class)
  public void addPidThrowsANotFoundExceptionWhenNoVertexCanBeFound() throws Exception {
    Vres vres = createConfiguration();
    GremlinEntityFetcher entityFetcher = new GremlinEntityFetcher();
    UUID id = UUID.randomUUID();
    int rev = 1;
    GraphWrapper emptyDatabase = newGraph().wrap();
    DataStoreOperations instance =
      new DataStoreOperations(emptyDatabase, mock(ChangeListener.class), entityFetcher, vres);
    URI pidUri = new URI("http://example.com/pid");

    instance.addPid(id, rev, pidUri);
  }

}