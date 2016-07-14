package nl.knaw.huygens.timbuctoo.rdf;

import nl.knaw.huygens.timbuctoo.model.vre.Vre;
import nl.knaw.huygens.timbuctoo.server.GraphWrapper;
import org.apache.jena.graph.Node;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.time.Clock;
import java.util.List;

import static nl.knaw.huygens.timbuctoo.model.vre.Collection.ENTITY_TYPE_NAME_PROPERTY_NAME;
import static nl.knaw.huygens.timbuctoo.model.vre.Collection.HAS_ENTITY_NODE_RELATION_NAME;
import static nl.knaw.huygens.timbuctoo.model.vre.Collection.HAS_ENTITY_RELATION_NAME;

public class Database {
  public static final String RDF_URI_PROP = "rdfUri";
  private final GraphWrapper graphWrapper;
  private final SystemPropertyModifier systemPropertyModifier;

  public Database(GraphWrapper graphWrapper) {
    this(graphWrapper, new SystemPropertyModifier(Clock.systemDefaultZone()));
  }

  Database(GraphWrapper graphWrapper, SystemPropertyModifier systemPropertyModifier) {
    this.graphWrapper = graphWrapper;
    this.systemPropertyModifier = systemPropertyModifier;
  }

  public Vertex findOrCreateEntityVertex(Node node, CollectionDescription collectionDescription) {
    Graph graph = graphWrapper.getGraph();
    final GraphTraversal<Vertex, Vertex> existingT = graph
      .traversal().V()
      .hasLabel(Vre.DATABASE_LABEL)
      .has(Vre.VRE_NAME_PROPERTY_NAME, collectionDescription.getVreName())
      .out(Vre.HAS_COLLECTION_RELATION_NAME)
      .out(HAS_ENTITY_NODE_RELATION_NAME)
      .out(HAS_ENTITY_RELATION_NAME)
      .has(RDF_URI_PROP, node.getURI());

    Collection collection = findOrCreateCollection(collectionDescription);

    if (existingT.hasNext()) {
      final Vertex foundVertex = existingT.next();
      collection.add(foundVertex, getCollectionDescriptions(foundVertex, collectionDescription.getVreName()));
      return foundVertex;
    } else {
      Vertex vertex = graph.addVertex();
      vertex.property(RDF_URI_PROP, node.getURI());

      systemPropertyModifier.setCreated(vertex, "rdf-importer");
      systemPropertyModifier.setModified(vertex, "rdf-importer");
      systemPropertyModifier.setTimId(vertex);
      systemPropertyModifier.setRev(vertex, 1);
      systemPropertyModifier.setIsLatest(vertex, true);
      systemPropertyModifier.setIsDeleted(vertex, false);

      collection.add(vertex, getCollectionDescriptions(vertex, collectionDescription.getVreName()));
      return vertex;
    }
  }

  public Entity findOrCreateEntity(String vreName, Node node) {
    final Vertex subjectVertex = findOrCreateEntityVertex(node, CollectionDescription.getDefault(vreName));
    // TODO *HERE SHOULD BE A COMMIT* (autocommit?)
    final List<CollectionDescription> collections = getCollectionDescriptions(subjectVertex, vreName);
    return new Entity(subjectVertex, collections);
  }

  public Collection findOrCreateCollection(String vreName, Node node) {
    CollectionDescription collectionDescription = new CollectionDescription(node.getLocalName(), vreName);
    return findOrCreateCollection(collectionDescription);
  }

  private Collection findOrCreateCollection(CollectionDescription collectionDescription) {
    Graph graph = graphWrapper.getGraph();
    final GraphTraversal<Vertex, Vertex> colTraversal =
      graph.traversal().V()
           .hasLabel(Vre.DATABASE_LABEL)
           .has(Vre.VRE_NAME_PROPERTY_NAME, collectionDescription.getVreName())
           .out(Vre.HAS_COLLECTION_RELATION_NAME)
           .has(ENTITY_TYPE_NAME_PROPERTY_NAME,
        collectionDescription.getEntityTypeName());

    Vertex collectionVertex;
    if (colTraversal.hasNext()) {
      collectionVertex = colTraversal.next();
    } else {
      collectionVertex = graph.addVertex(nl.knaw.huygens.timbuctoo.model.vre.Collection.DATABASE_LABEL);
    }

    collectionVertex.property(nl.knaw.huygens.timbuctoo.model.vre.Collection.COLLECTION_NAME_PROPERTY_NAME,
      collectionDescription.getCollectionName());
    collectionVertex.property(ENTITY_TYPE_NAME_PROPERTY_NAME,
      collectionDescription.getEntityTypeName());

    if (!collectionVertex.vertices(Direction.IN, Vre.HAS_COLLECTION_RELATION_NAME).hasNext()) {
      addCollectionToVre(collectionDescription, collectionVertex);
    }
    addCollectionToArchetype(collectionVertex);

    return new Collection(collectionDescription.getVreName(), collectionVertex, graphWrapper);
  }

  private Vertex addCollectionToArchetype(Vertex collectionVertex) {

    final Vertex archetypeVertex = graphWrapper.getGraph().traversal().V().hasLabel(Vre.DATABASE_LABEL)
                                        .has(Vre.VRE_NAME_PROPERTY_NAME, "Admin")
                                        .out(Vre.HAS_COLLECTION_RELATION_NAME)
                                        .has(
                                          ENTITY_TYPE_NAME_PROPERTY_NAME,
                                          "concept")
                                        .next();
    if (!collectionVertex
      .vertices(Direction.OUT, nl.knaw.huygens.timbuctoo.model.vre.Collection.HAS_ARCHETYPE_RELATION_NAME).hasNext()) {
      collectionVertex
        .addEdge(nl.knaw.huygens.timbuctoo.model.vre.Collection.HAS_ARCHETYPE_RELATION_NAME, archetypeVertex);
    }
    return archetypeVertex;
  }

  public void addCollectionToVre(CollectionDescription collectionDescription, Vertex collectionVertex) {
    Vertex vreVertex = graphWrapper.getGraph().traversal().V()
                            .hasLabel(Vre.DATABASE_LABEL)
                            .has(Vre.VRE_NAME_PROPERTY_NAME, collectionDescription.getVreName())
                            .next();
    vreVertex.addEdge(Vre.HAS_COLLECTION_RELATION_NAME, collectionVertex);
  }

  public List<CollectionDescription> getCollectionDescriptions(Vertex vertex, String vreName) {
    return graphWrapper
      .getGraph().traversal()
      .V(vertex.id())
      .in(nl.knaw.huygens.timbuctoo.model.vre.Collection.HAS_ENTITY_RELATION_NAME)
      .in(nl.knaw.huygens.timbuctoo.model.vre.Collection.HAS_ENTITY_NODE_RELATION_NAME)
      .where(
        __.in(Vre.HAS_COLLECTION_RELATION_NAME)
          .has(Vre.VRE_NAME_PROPERTY_NAME, vreName)
      ).map(collectionT -> {
        return new CollectionDescription(collectionT.get().value(ENTITY_TYPE_NAME_PROPERTY_NAME), vreName);
      }).toList();
  }
}
