package nl.knaw.huygens.timbuctoo.storage.graph.tinkerpop;

import static nl.knaw.huygens.timbuctoo.storage.graph.tinkerpop.VertexMockBuilder.aVertex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import nl.knaw.huygens.timbuctoo.model.Entity;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.pipes.PipeFunction;

public class PipeFunctionFactoryTest {
  private static final String ID = "id";

  @Test
  public void forPropertyCreatesAPipeFunctionThatRetrievesAPropertyFromAnElement() {
    // setup
    Vertex vertex = aVertex().withId(ID).build();

    PipeFunctionFactory instance = new PipeFunctionFactory();

    // action
    PipeFunction<Vertex, String> pipeFunction = instance.forDistinctProperty(Entity.ID_DB_PROPERTY_NAME);

    // verify
    assertThat(pipeFunction.compute(vertex), is(ID));
  }
}
