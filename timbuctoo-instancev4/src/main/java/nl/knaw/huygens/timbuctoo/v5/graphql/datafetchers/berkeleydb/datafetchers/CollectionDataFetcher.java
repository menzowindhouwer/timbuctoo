package nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.berkeleydb.datafetchers;

import nl.knaw.huygens.timbuctoo.v5.datastores.collectionindex.CursorSubject;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.berkeleydb.dto.LazyTypeSubjectReference;
import nl.knaw.huygens.timbuctoo.v5.datastores.collectionindex.CollectionIndex;
import nl.knaw.huygens.timbuctoo.v5.datastores.quadstore.QuadStore;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.CollectionFetcher;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.dto.PaginatedList;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.dto.PaginationArguments;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.dto.SubjectReference;

import java.util.stream.Stream;

import static nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.berkeleydb.datafetchers.PaginationHelper.getPaginatedList;

public class CollectionDataFetcher implements CollectionFetcher {
  private final String collectionName;
  private final CollectionIndex collectionIndex;
  private final QuadStore quadStore;

  public CollectionDataFetcher(String collectionName, CollectionIndex collectionIndex, QuadStore quadStore) {
    this.collectionName = collectionName;
    this.collectionIndex = collectionIndex;
    this.quadStore = quadStore;
  }

  @Override
  public PaginatedList<SubjectReference> getList(PaginationArguments arguments) {
    String cursor = arguments.getCursor();
    try (Stream<CursorSubject> subjectStream = collectionIndex.getSubjects(collectionName, cursor)) {
      return getPaginatedList(
        subjectStream,
        cursorSubject -> new LazyTypeSubjectReference(cursorSubject.getSubjectUri(), quadStore),
        arguments.getCount(),
        !cursor.isEmpty()
      );
    }
  }

}