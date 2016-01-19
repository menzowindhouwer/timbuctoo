package nl.knaw.huygens.timbuctoo.server.rest;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.Test;

import static nl.knaw.huygens.timbuctoo.server.rest.EntityRefMatcher.likeEntityRef;
import static nl.knaw.huygens.timbuctoo.server.rest.TestGraphBuilder.newGraph;
import static nl.knaw.huygens.timbuctoo.server.rest.VertexBuilder.vertex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class TimbuctooQueryTest {

  public static final WwPersonSearchDescription DESCRIPTION = new WwPersonSearchDescription();

  @Test
  public void returnsASearchRefsWithTheRefsOfTheVerticesWithTheTypeOfTheDescription() {
    TimbuctooQuery instance = new TimbuctooQuery(DESCRIPTION);
    Graph graph = newGraph()
      .withVertex(vertex().withType("wwperson").isLatest(true).withId("id1"))
      .withVertex(vertex().withType("otherperson").isLatest(true).withId("id2"))
      .withVertex(vertex().withType("otherperson").isLatest(true).withType("wwperson").withId("id3"))
      .build();
    SearchResult searchResult = instance.execute(graph);

    assertThat(searchResult.getRefs(), containsInAnyOrder(
      likeEntityRef().withId("id1").withType("wwperson"),
      likeEntityRef().withId("id3").withType("wwperson")));
    assertThat(searchResult.getFullTextSearchFields(),
      containsInAnyOrder(DESCRIPTION.getFullTextSearchFields().toArray()));
    assertThat(searchResult.getSortableFields(), containsInAnyOrder(DESCRIPTION.getSortableFields().toArray()));
  }

  @Test
  public void returnsOnlyTheLatestRefsInTheSearchResult() {
    TimbuctooQuery instance = new TimbuctooQuery(DESCRIPTION);

    WwPersonSearchDescription.Names names1 = new WwPersonSearchDescription.Names();
    PersonName name = PersonName.newInstance("forename", "surname");
    names1.list.add(name);
    names1.list.add(PersonName.newInstance("forename2", "surname2"));

    Graph graph = newGraph().withVertex(vertex().withType("wwperson").withProperty("wwperson_names", names1)
                                                .withId("id1").isLatest(true))
                            .withVertex(vertex().withType("wwperson").withId("id1").isLatest(false)).build();

    SearchResult searchResult = instance.execute(graph);

    assertThat(searchResult.getRefs(), contains(likeEntityRef().withDisplayName(name.getShortName())));
  }

}
