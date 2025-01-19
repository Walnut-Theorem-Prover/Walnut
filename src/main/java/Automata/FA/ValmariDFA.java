/*	 Copyright 2016 Hamoon Mousavi, 2025 John Nicol
 *
 * 	 This file is part of Walnut.
 *
 *   Walnut is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Walnut is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Walnut.  If not, see <http://www.gnu.org/licenses/>.
 */
package Automata.FA;

import it.unimi.dsi.fastutil.ints.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Adapted from Antti Valmari
// "Fast brief practical DFA minimization." Information Processing Letters 112.6 (2012): 213-217
public class ValmariDFA {
    ValmariPartition blocks;
    private ValmariPartition cords;

    private final int numStates;
    private int numTransitions;
    private int numFinalstates;

    private int[] _A; // Adjacent transitions
    private int[] _F; // Adjacent states

    private int[] T;
    // labels of transitions
    private int[] L;
    // heads of transitions
    private int[] H;

    public ValmariDFA(List<Int2IntMap> dfaD, int numStates) {
        // Pre-size the arrays.
        for(int q = 0; q != dfaD.size();++q){
            numTransitions += dfaD.get(q).keySet().size();
        }

        T = new int[numTransitions];
        L = new int[numTransitions];
        H = new int[numTransitions];

        int arrIndex = 0;
        for(int q = 0; q != dfaD.size();++q){
            for(Int2IntMap.Entry entry: dfaD.get(q).int2IntEntrySet()) {
                H[arrIndex] = entry.getIntValue();
                T[arrIndex] = q;
                L[arrIndex] = entry.getIntKey();
                arrIndex++;
            }
        }
        this.numStates = numStates;
        blocks = new ValmariPartition();
        cords = new ValmariPartition();
    }

    /**
     * Minimize. Clear O for additional space (it's rebuilt later).
     * @param O - output
     */
    void minValmari(IntList O) {
        blocks.init(numStates);

        for(int q = 0; q < numStates; ++q ){
            if(O.getInt(q) != 0){
                reach( q );
            }
        }
        O.clear();
        _A = new int[numTransitions]; _F = new int[ numStates +1 ];

        numFinalstates = rr; rem_unreachable();

        /* Make initial partition */
        ValmariPartition.W = new int[ numTransitions +1 ]; ValmariPartition.M = new int[ numTransitions +1];
        ValmariPartition.M[0] = numFinalstates;
        if( numFinalstates != 0 ){ ValmariPartition.W[ValmariPartition.w++] = 0; blocks.split(); }

        /* Make transition partition */
        cords.init(numTransitions);
        if( numTransitions != 0 ){
            IntArrays.quickSort(cords.E, (a, b) -> Integer.compare(L[a], L[b]));
            cords.z = ValmariPartition.M[0] = 0; int a = L[cords.E[0]];
            for(int i = 0; i < numTransitions; ++i ){
                int t = cords.E[i];
                if( L[t] != a ){
                    a = L[t]; cords.P[cords.z++] = i;
                    cords.F[cords.z] = i; ValmariPartition.M[cords.z] = 0;
                }
                cords.S[t] = cords.z; cords.L[t] = i;
            }
            cords.P[cords.z++] = numTransitions;
        }

        /* Split blocks and cords */
        make_adjacent( H );
        int b = 1, c = 0;
        while( c < cords.z ){
            for(int i = cords.F[c]; i < cords.P[c]; ++i ){
                blocks.mark( T[cords.E[i]] );
            }
            blocks.split(); ++c;
            while( b < blocks.z ){
                for(int i = blocks.F[b]; i < blocks.P[b]; ++i ){
                    for(int j = _F[blocks.E[i]]; j < _F[blocks.E[i]+1]; ++j){
                        cords.mark( _A[j] );
                    }
                }
                cords.split(); ++b;
            }
        }
    }

    private void make_adjacent(int[] K) {
        Arrays.fill(_F, 0);
        for(int t = 0; t < numTransitions; ++t ) {
            ++_F[K[t]];
        }
        for(int q = 0; q < numStates; ++q ) {
            _F[q+1] += _F[q];
        }
        for(int t = numTransitions; t-- != 0; ) {
            _A[--_F[K[t]]] = t;
        }
    }

    /* Removal of irrelevant parts */
    int rr = 0;   // number of reached states

    private void reach( int q ) {
        int i = blocks.L[q];
        if( i >= rr ){
            blocks.E[i] = blocks.E[rr]; blocks.L[blocks.E[i]] = i;
            blocks.E[rr] = q; blocks.L[q] = rr++; }
    }

    private void rem_unreachable(){
        make_adjacent( H ); int i, j;
        for( i = 0; i < rr; ++i ){
            for(j = _F[blocks.E[i]]; j < _F[blocks.E[i] + 1]; ++j ){
                reach( T[_A[j]] );
            }
        }
        j = 0;
        for(int t = 0; t < numTransitions; ++t ){
            if( blocks.L[H[t]] < rr ){
                T[j] = T[t]; L[j] = L[t];
                H[j] = H[t]; ++j;
            }
        }
        numTransitions = j; blocks.P[0] = rr; rr = 0;
    }

    /**
     * Replace fields of FA in a memory-efficient way.
     */
    void replaceFields(FA f) {
        f.setQ(blocks.z);
        f.setQ0(blocks.S[f.getQ0()]);
        f.setO(null);
        _A = _F = blocks.E = blocks.P = null;
        ValmariPartition.M = ValmariPartition.W = null; // this fixes an actual leak
        cords = null;
        f.setNfaD(determineD()); // needs blocks.(L,F,S), L, T, H
        f.reduceNfaDMemory();
        L = T = H = blocks.L = blocks.S = null;
        f.setO(determineO()); // needs blocks.F
    }

    private List<Int2ObjectRBTreeMap<IntList>> determineD() {
        List<Int2ObjectRBTreeMap<IntList>> d = new ArrayList<>(blocks.z);
        for(int q = 0; q < blocks.z; ++q){
            d.add(new Int2ObjectRBTreeMap<>());
        }
        for(int t = 0; t < numTransitions; ++t ){
            if( blocks.L[T[t]] == blocks.F[blocks.S[T[t]]] ){
                int q = blocks.S[T[t]];
                int l = L[t];
                int p = blocks.S[H[t]];
                d.get(q).computeIfAbsent(l, key -> new IntArrayList()).add(p);
            }
        }
        return d;
    }

    private IntList determineO() {
        IntList O = new IntArrayList(blocks.z);
        for(int q = 0; q < blocks.z; ++q ){
            if( blocks.F[q] < numFinalstates){
                O.add(1);
            }
            else {
                O.add(0);
            }
        }
        return O;
    }

}
