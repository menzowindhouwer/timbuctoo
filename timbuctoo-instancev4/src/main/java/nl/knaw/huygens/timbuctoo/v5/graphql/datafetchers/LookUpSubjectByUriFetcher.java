package nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers;

import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.dto.SubjectReference;

public interface LookUpSubjectByUriFetcher {
  SubjectReference getItem(String uri);
}
