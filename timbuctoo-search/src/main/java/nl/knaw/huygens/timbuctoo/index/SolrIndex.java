package nl.knaw.huygens.timbuctoo.index;

import java.io.IOException;
import java.util.List;

import nl.knaw.huygens.facetedsearch.model.parameters.FacetedSearchParameters;
import nl.knaw.huygens.solr.AbstractSolrServer;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.SearchResult;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

public class SolrIndex implements Index {

  private final SolrInputDocumentCreator solrDocumentCreator;
  private final AbstractSolrServer solrServer;
  private final String name;
  protected static final SolrQuery COUNT_QUERY;

  static {
    COUNT_QUERY = new SolrQuery();
    COUNT_QUERY.setQuery("*:*");
    COUNT_QUERY.setRows(0);
  }

  public SolrIndex(String name, SolrInputDocumentCreator solrDocumentCreator, AbstractSolrServer solrServer) {
    this.name = name;
    this.solrDocumentCreator = solrDocumentCreator;
    this.solrServer = solrServer;
  }

  @Override
  public void add(List<? extends DomainEntity> variations) throws IndexException {
    updateIndex(variations);

  }

  private void updateIndex(List<? extends DomainEntity> variations) throws IndexException {
    SolrInputDocument document = solrDocumentCreator.create(variations);

    try {
      solrServer.add(document);
    } catch (SolrServerException e) {
      throw new IndexException(e);
    } catch (IOException e) {
      throw new IndexException(e);
    }
  }

  @Override
  public void update(List<? extends DomainEntity> variations) throws IndexException {
    updateIndex(variations);
  }

  @Override
  public void deleteById(String id) throws IndexException {
    try {
      solrServer.deleteById(id);
    } catch (SolrServerException e) {
      throw new IndexException(e);
    } catch (IOException e) {
      throw new IndexException(e);
    }

  }

  @Override
  public void deleteById(List<String> ids) throws IndexException {
    try {
      solrServer.deleteById(ids);
    } catch (SolrServerException e) {
      throw new IndexException(e);
    } catch (IOException e) {
      throw new IndexException(e);
    }

  }

  @Override
  public void clear() throws IndexException {
    try {
      solrServer.deleteByQuery("*:*");
      solrServer.commit();
    } catch (SolrServerException e) {
      throw new IndexException(e);
    } catch (IOException e) {
      throw new IndexException(e);
    }
  }

  @Override
  public long getCount() throws IndexException {
    try {
      SolrDocumentList results = solrServer.search(COUNT_QUERY).getResults();
      return results.getNumFound();
    } catch (SolrServerException e) {
      throw new IndexException(e);
    }
  }

  @Override
  public void commit() throws IndexException {
    try {
      solrServer.commit();
    } catch (SolrServerException e) {
      throw new IndexException(e);
    } catch (IOException e) {
      throw new IndexException(e);
    }

  }

  @Override
  public void close() throws IndexException {
    try {
      this.commit();
    } finally {
      try {
        solrServer.shutdown();
      } catch (SolrServerException e) {
        throw new IndexException(e);
      } catch (IOException e) {
        throw new IndexException(e);
      }
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public <T extends FacetedSearchParameters<T>> SearchResult search(FacetedSearchParameters<T> searchParamaters) {
    // TODO Auto-generated method stub
    return null;

  }
}
