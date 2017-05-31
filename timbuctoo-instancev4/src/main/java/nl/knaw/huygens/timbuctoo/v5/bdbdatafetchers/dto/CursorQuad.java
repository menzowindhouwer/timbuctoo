package nl.knaw.huygens.timbuctoo.v5.bdbdatafetchers.dto;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
public interface CursorQuad extends CursorContainer {
  String getSubject();

  String getPredicate();

  String getObject();

  Optional<String> getValuetype();

  Optional<String> getLanguage();

  @Value.Auxiliary
  String getCursor();

  static CursorQuad create(String subject, String predicate, String object, String valueType, String language,
                           String cursor) {
    return ImmutableCursorQuad.builder()
      .subject(subject)
      .predicate(predicate)
      .object(object)
      .valuetype(Optional.ofNullable(valueType))
      .language(Optional.ofNullable(language))
      .cursor(cursor)
      .build();
  }


}