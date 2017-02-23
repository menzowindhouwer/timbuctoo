package nl.knaw.huygens.timbuctoo.rdf;

import org.apache.commons.lang.builder.ToStringBuilder;

public class LiteralTriple implements Triple {
  private final String subject;
  private final String predicate;
  private final Object object;

  public LiteralTriple(String subject, String predicate, Object object) {
    this.subject = subject;
    this.predicate = predicate;
    this.object = object;
  }

  @Override
  public String getSubject() {
    return subject;
  }

  @Override
  public String getPredicate() {
    return predicate;
  }

  @Override
  public String getObject() {
    return "" + object;
  }

  @Override
  public String getStringValue() {
    String subject = isBlankNode(getSubject()) ? getSubject() : String.format("<%s>", getSubject());
    return String.format("%s <%s> %s .\n", subject, getPredicate(), getObject());
  }

  private boolean isBlankNode(String node) {
    return node.startsWith("_:");
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
