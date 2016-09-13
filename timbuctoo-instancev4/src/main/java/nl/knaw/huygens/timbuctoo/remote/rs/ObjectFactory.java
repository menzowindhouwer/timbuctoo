package nl.knaw.huygens.timbuctoo.remote.rs;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

  public ObjectFactory() {}

  public Urlset createUrlset() {
    System.out.println("createUrlset called");
    return new Urlset();
  }

  public Sitemapindex createSitemapIndex() {
    System.out.println("create Sitemapindex called");
    return new Sitemapindex();
  }

  @XmlElementDecl(namespace = "http://www.sitemaps.org/schemas/sitemap/0.9", name = "urlset")
  public JAXBElement<Urlset> createUrlset(Urlset value) {
    return new JAXBElement<Urlset>(Urlset.QNAME, Urlset.class, null, value);
  }

  @XmlElementDecl(namespace = "http://www.sitemaps.org/schemas/sitemap/0.9", name = "sitemapindex")
  public JAXBElement<Sitemapindex> createSitemapIndex(Sitemapindex value) {
    return new JAXBElement<Sitemapindex>(Sitemapindex.QNAME, Sitemapindex.class, null, value);
  }
}
