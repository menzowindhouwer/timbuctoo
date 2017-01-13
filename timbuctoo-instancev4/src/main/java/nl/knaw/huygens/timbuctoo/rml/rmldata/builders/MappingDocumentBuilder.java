package nl.knaw.huygens.timbuctoo.rml.rmldata.builders;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import nl.knaw.huygens.timbuctoo.rml.DataSource;
import nl.knaw.huygens.timbuctoo.rml.rdfshim.RdfResource;
import nl.knaw.huygens.timbuctoo.rml.rmldata.RmlMappingDocument;
import nl.knaw.huygens.timbuctoo.rml.rmldata.RrTriplesMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static nl.knaw.huygens.timbuctoo.rml.util.TopologicalSorter.topologicalSort;

public class MappingDocumentBuilder {
  private List<TriplesMapBuilder> tripleMapBuilders = new ArrayList<>();
  private Map<String, List<PromisedTriplesMap>> requestedTripleMaps = new HashMap<>();
  private List<String> errors = new ArrayList<>();

  public MappingDocumentBuilder(){
  }

  public MappingDocumentBuilder withTripleMap(String uri, Consumer<TriplesMapBuilder> subBuilder) {
    subBuilder.accept(withTripleMap(uri));
    return this;
  }

  public TriplesMapBuilder withTripleMap(String uri) {
    final TriplesMapBuilder subBuilder = new TriplesMapBuilder(uri);
    this.tripleMapBuilders.add(subBuilder);
    return subBuilder;
  }

  private List<TriplesMapBuilder> breakCyclesAndSort(List<TriplesMapBuilder> triplesMapBuilders) {

    Map<String, TriplesMapBuilder> lookup = triplesMapBuilders
      .stream().collect(Collectors.toMap(TriplesMapBuilder::getUri, v -> v));

    Set<Multiset<TriplesMapBuilder>> cycles = detectCycles(triplesMapBuilders, lookup);
    List<TriplesMapBuilder> buildersListWithoutCycles = breakCycles(triplesMapBuilders, cycles);
    return topologicalSort(buildersListWithoutCycles, lookup, ((currentChain, current, dependency) -> {
      //chains should no longer be possible
      throw new IllegalStateException("Chains detected");
    }), errors::add);
  }

  //we're doing a topological sort without the actual sorting. We're just keeping track of the cycles so we can break
  //the nodes who are part of most cycles.
  private Set<Multiset<TriplesMapBuilder>> detectCycles(List<TriplesMapBuilder> triplesMapBuilders,
                                                        Map<String, TriplesMapBuilder> lookup) {
    Set<Multiset<TriplesMapBuilder>> cycles = new HashSet<>();
    topologicalSort(
      triplesMapBuilders,
      lookup,
      ((currentChain, current, dependency) -> {
        ImmutableMultiset.Builder<TriplesMapBuilder> cycleChain = ImmutableMultiset.builder();
        cycleChain.addAll(currentChain);
        if (current == dependency) {
          //self reference
          cycleChain.add(dependency);//make sure the self referencing item is counted twice
        }
        cycles.add(cycleChain.build());
      }),
      e -> {
        //errors are handled in the actual sort and ignored for now
      }
    );
    return cycles;
  }

  private List<TriplesMapBuilder> breakCycles(List<TriplesMapBuilder> triplesMapBuilders,
                                              Set<Multiset<TriplesMapBuilder>> cycles) {
    List<TriplesMapBuilder> result = new LinkedList<>();
    result.addAll(triplesMapBuilders);
    while (cycles.size() > 0) {
      Multiset<TriplesMapBuilder> cycleOccurrence = HashMultiset.create();
      for (Multiset<TriplesMapBuilder> cycle : cycles) {
        for (TriplesMapBuilder triplesMapBuilder : cycle) {
          cycleOccurrence.add(triplesMapBuilder);
        }
      }
      //get the triplesMapBuilder that is currently taking part in most of the cycles
      triplesMapBuilders
        .stream()
        .max(comparing(cycleOccurrence::count))
        .ifPresent(x -> {
          result.add(x.splitOffDependendingPredObjMaps());
          List<Multiset<TriplesMapBuilder>> itemsToRemove = new LinkedList<>();
          for (Multiset<TriplesMapBuilder> cycle : cycles) {
            if (cycle.contains(x)) {
              itemsToRemove.add(cycle);
            }
          }
          cycles.removeAll(itemsToRemove);
        });
    }
    return result;
  }

  public RmlMappingDocument build(Function<RdfResource, Optional<DataSource>> dataSourceFactory) {

    final List<RrTriplesMap> triplesMaps = breakCyclesAndSort(this.tripleMapBuilders)
      .stream()
      // Build the tripleMapBuilders with lambda to resolve otherMap they are dependent on
      .map(tripleMapBuilder -> tripleMapBuilder.build(dataSourceFactory, this::getRrTriplesMap, errors::add))
      .filter(x -> x != null)
      // First collect all the builders, so requestedTripleMaps is filled via getRrTriplesMap lambda
      .collect(Collectors.toList());
    triplesMaps
      .stream()
      // Resolve uri's of requested triple maps to actual RrTripleMap using PromisedTriplesMap.setTriplesMap
      .forEach(current -> {
        List<PromisedTriplesMap> maps = requestedTripleMaps.get(current.getUri());
        if (maps != null) {
          maps.forEach(promise -> promise.setTriplesMap(current));
        }
      });

    return new RmlMappingDocument(triplesMaps, errors);
  }

  private PromisedTriplesMap getRrTriplesMap(String requesterUri, String requestedUri) {
    PromisedTriplesMap promisedTriplesMap = new PromisedTriplesMap();

    requestedTripleMaps
      .computeIfAbsent(requestedUri, key -> new ArrayList<>())
      .add(promisedTriplesMap);

    return promisedTriplesMap;
  }
}
