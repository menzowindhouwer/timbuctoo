package nl.knaw.huygens.timbuctoo.server.databasemigration;

import nl.knaw.huygens.timbuctoo.server.GraphWrapper;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScaffoldMigrator {
  private static final Logger LOG = LoggerFactory.getLogger(ScaffoldMigrator.class);


  private final GraphWrapper graphWrapper;

  public ScaffoldMigrator(GraphWrapper graphWrapper) {
    this.graphWrapper = graphWrapper;
  }

  public void execute() {
    Graph graph = graphWrapper.getGraph();
    Long vertexCount = graph
      .traversal().V().not(__.has("type", DatabaseMigrator.EXECUTED_MIGRATIONS_TYPE))
      .count().next();

    if (vertexCount == 0) {
      LOG.info("Setting up a new scaffold for empty database");
    }
  }
}
