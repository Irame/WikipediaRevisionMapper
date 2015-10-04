package de.mpii.wiki.result;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.List;


public class MappedResults {
  private final List<MappedResult> results;

  private final TObjectIntMap<MappedType> stats;
  
  public MappedResults() {
    results = new ArrayList<>();
    stats = new TObjectIntHashMap<>();
  }

  public void add(MappedResult result) {
    results.add(result);
    stats.adjustOrPutValue(result.getMappingType(), 1, 1);
  }
  
  public List<MappedResult> getResults() {
    return results;
  }

  public int getCount(MappedType type) {
    return stats.get(type);
  }

  public void printResultStats() {
    for(MappedType type : MappedType.values()) {
      int num = 0;
      if(stats.containsKey(type)) {
        num = stats.get(type);  
      }
      System.out.println(type.name() + "\t:\t" + num);
    }
  }
  
  public int size() {
    return results.size();
  }
}
