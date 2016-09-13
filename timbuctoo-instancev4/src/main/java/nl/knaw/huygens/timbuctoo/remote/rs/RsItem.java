package nl.knaw.huygens.timbuctoo.remote.rs;


import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class RsItem<T extends RsItem> {

  private String loc;
  private ZonedDateTime lastmod;
  private String changefreq;
  @XmlElement(name = "md", namespace = "http://www.openarchives.org/rs/terms/")
  private RsMd rsMd;
  @XmlElement(name = "ln", namespace = "http://www.openarchives.org/rs/terms/")
  private List<RsLn> rsLnList = new ArrayList<>();

  public String getLoc() {
    return loc;
  }

  public T setLoc(@Nonnull String loc) {
    this.loc = Preconditions.checkNotNull(loc);
    return (T) this;
  }

  public Optional<ZonedDateTime> getLastmod() {
    return Optional.ofNullable(lastmod);
  }

  public T setLastmod(ZonedDateTime lastmod) {
    this.lastmod = lastmod;
    return (T) this;
  }

  public Optional<String> getChangefreq() {
    return Optional.ofNullable(changefreq);
  }

  public T setChangefreq(String changefreq) {
    this.changefreq = changefreq;
    return (T) this;
  }

  public Optional<RsMd> getMetadata() {
    return Optional.ofNullable(rsMd);
  }

  public T setMetadata(RsMd rsMd) {
    this.rsMd = rsMd;
    return (T) this;
  }

  public List<RsLn> getLinkList() {
    return rsLnList;
  }

  public T add(RsLn rsLn) {
    rsLnList.add(rsLn);
    return (T) this;
  }

}
