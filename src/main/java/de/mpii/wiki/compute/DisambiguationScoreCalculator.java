package de.mpii.wiki.compute;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class DisambiguationScoreCalculator {
    public static double compute(TIntList list1, TIntList list2) {
        TIntSet set1 = new TIntHashSet(list1 == null ? new TIntArrayList() : list1);
        TIntSet set2 = new TIntHashSet(list2 == null ? new TIntArrayList() : list2);

        int sizeCurrentSet = set1.size();
        set1.retainAll(set2);
        set2.removeAll(set1);

        int union = sizeCurrentSet + set2.size();
        int intersection = set1.size();
        return ((double)intersection)/(double)union;
    }
}
