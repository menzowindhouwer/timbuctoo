package nl.knaw.huygens.timbuctoo.server.databasemigration;

import nl.knaw.huygens.timbuctoo.server.GraphWrapper;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class DatabaseMigrator {
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseMigrator.class);
  protected static final String EXECUTED_MIGRATIONS_TYPE = "executed-migrations";
  private final GraphWrapper graphWrapper;
  private final Map<String, DatabaseMigration> migrations;

  public DatabaseMigrator(GraphWrapper graphWrapper, Map<String, DatabaseMigration> migrations) {
    this.graphWrapper = graphWrapper;
    this.migrations = migrations;
  }

  public void execute() {
    Graph graph = graphWrapper.getGraph();
    List<String> executedMigrations = graph.traversal().V()
                                           .has("type", EXECUTED_MIGRATIONS_TYPE)
                                           .map(vertexTraverser -> (String) vertexTraverser.get()
                                                                                           .property("name")
                                                                                           .value())
                                           .toList();

    Long vertexCount = graph.traversal().V().count().next();

    migrations.forEach((name, migration) -> {

      if (vertexCount == 0L) {
        LOG.info("Skipping migration with name '{}' because this is a clean database. ", name);
        this.saveExecution(graph, name);
      } else  if (!executedMigrations.contains(name)) {
        LOG.info("Executing migration with name '{}'", name);
        try {
          migration.execute(graphWrapper);
          this.saveExecution(graph, name);
        } catch (IOException e) {
          LOG.error("Could not complete migration with name '{}'", name);
        }
      } else {
        LOG.info("Skipping migration with name '{}', because it has already been executed.", name);
      }
    });
  }

  private void saveExecution(Graph graph, String name) {
    Transaction transaction = graph.tx();
    if (!transaction.isOpen()) {
      transaction.open();
    }
    Vertex migrationVertex = graph.addVertex();
    migrationVertex.property("type", EXECUTED_MIGRATIONS_TYPE);
    migrationVertex.property("name", name);
    migrationVertex.property("tim_id", UUID.randomUUID().toString());
    transaction.commit();
  }
}
