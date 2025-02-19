package edu.mit.csail.ammolite.utils;
/*
 * Copyright 2011 David Jurgens
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.mit.csail.ammolite.compression.LabeledWeightedGraph;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.WeightedEdge;
import edu.ucla.sspace.graph.WeightedGraph;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;

/**
 * An implementation of the <a
 * href="http://en.wikipedia.org/wiki/Floyd-Warshall_algorithm">Floyd-Warshall</a>
 * algorithm for computing all the pair-wise shortest paths in O(n<sup>3</sup>)
 * time.  
 */
public class FloydsAlgorithm {

    public FloydsAlgorithm() { }

    /**
     * Computes the pair-wise shortest paths between the vertices in {@code g},
     * returning a matrix of the path lengths.
     */
    public static Matrix computeAllPairsDistance(Graph<? extends Edge> g) {
        int verts = g.order();

        // NOTE: this code is going to break if the graph does not have
        // contiguous indices starting at 0.  Perhaps perform a compaction step?

        // dm = shorthand for distanceMatrix
        Matrix dm = new ArrayMatrix(verts, verts);
        
        // Initialize the distance matrix with the shortest path.

        // Check whether the graph has edge weights
        if (false && g instanceof WeightedGraph) {
            @SuppressWarnings("unchecked")
            WeightedGraph<WeightedEdge> wg = (WeightedGraph<WeightedEdge>)g;
            for (int i = 0; i < verts; ++i) {
                for (int j = 0; j < verts; ++j) {
                    Double weight = Double.MAX_VALUE;
                    Set<WeightedEdge> edges = wg.getEdges(i, j);
                    if (edges != null) {
                        for (WeightedEdge e : edges) {
                            if (e.from() == i && e.to() == j
                                    && e.weight() < weight)
                                weight = e.weight();
                        }
                    }
                    dm.set(i, j, weight);
                }
            }
        }
        
        
        
        // If unweighted, assume unit distance for all edges
        else {
            for (int i = 0; i < verts; ++i) {
                Set<Integer> adjacent = g.getNeighbors(i);
                for (int j = 0; j < verts; ++j) {
                    if( adjacent.contains(j)){
                        dm.set(i, j, 1);
                    } else if( i == j) {
                        dm.set(i, j, 0);
                    } else {
                        dm.set(i, j, Double.MAX_VALUE);
                    }
                }
            }
        }

        for (int k = 0; k < verts; ++k) {
            for (int i = 0; i < verts; ++i) {
                for (int j = 0; j < verts; ++j) {
                    double current = dm.get(i,j);
                    double updated = dm.get(i,k) + dm.get(k,j);
                    if(updated < current){
                        dm.set(i, j, updated);
                    }
                }
            }
        }
        return dm;
    }
    
    public static Map<Integer, Map<Integer, String>> getAllPairsPaths(LabeledWeightedGraph g){
        Map<Integer, Map<Integer,String>> paths = new HashMap<Integer, Map<Integer,String>>();
        int verts = g.order();
        
        for( int i=0; i < verts; i++){
            paths.put(i, new HashMap<Integer,String>());
        }
        
        for (int i = 0; i < verts; ++i) {
            Set<Integer> adjacent = g.getNeighbors(i);
            for (int j = 0; j < verts; ++j) {
                if( adjacent.contains(j)){
                    paths.get(i).put(j, g.labelOf(i) +  g.labelOf(j));
                } else if( i == j) {
                    paths.get(i).put(j, g.labelOf(i));
                } else {
                    paths.get(i).put(j, "");
                }
            }
        }

        for (int k = 0; k < verts; ++k) {
            for (int i = 0; i < verts; ++i) {
                for (int j = 0; j < verts; ++j) {
                    String current = paths.get(i).get(j);
                    String updated = paths.get(i).get(k) + paths.get(k).get(j);
                    if(current.equals("") || (updated.length() < current.length() && updated.length() > 0)){
                        paths.get(i).put(j, updated);
                    }
                }
            }
        }
        return paths;
    }
    
    
}