/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2017 Fabian Prasser, Florian Kohlmayer and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.criteria;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.DataSubset;
import org.deidentifier.arx.PopulationStatistics;
import org.deidentifier.arx.framework.check.groupify.HashGroupifyEntry;
import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.data.Dictionary;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;
import org.deidentifier.arx.framework.lattice.Transformation;
import org.deidentifier.arx.metric.v2.MetricSDNMEntropyBasedInformationLoss;

/**
 * Privacy model for the game theoretic approach proposed in:
 * A Game Theoretic Framework for Analyzing Re-Identification Risk.
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin
 * PLOS|ONE. 2015.
 * 
 * @author Fabian Prasser
 */
public class ProfitabilityJournalist extends ProfitabilityProsecutor {

    /** SVUID */
    private static final long serialVersionUID = 5089787798100584405L;

    /** Implements a naive version of the "No-Attack" variant */
    public static boolean     NAIVE_NO_ATTACK  = false;

    /** Data subset */
    private DataSubset        subset;

    /** Local recoding */
    private double            gsFactor         = 0.5d;

    /** Local recoding */
    private boolean           optimize         = false;

    /**
     * Creates a new instance of game theoretic approach proposed in:
     * A Game Theoretic Framework for Analyzing Re-Identification Risk.
     * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton, 
     * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin
     * PLOS|ONE. 2015. 
     */
    public ProfitabilityJournalist(DataSubset subset){
        super();
        this.subset = subset;
    }

    @Override
    public ProfitabilityJournalist clone() {
        return new ProfitabilityJournalist(this.subset.clone());
    }

    @Override
    public PrivacyCriterion clone(DataSubset subset) {
       return new ProfitabilityJournalist(subset);
    }
    
    @Override
    public DataSubset getDataSubset() {
        return this.subset;
    }
    
    @Override
    public int getRequirements(){
        return ARXConfiguration.REQUIREMENT_COUNTER | ARXConfiguration.REQUIREMENT_SECONDARY_COUNTER;
    }
    
    @Override
    public void initialize(DataManager manager, ARXConfiguration config) {
        super.initialize(manager, config);
        this.gsFactor = config.getQualityModel().getGeneralizationSuppressionFactor();
        this.hierarchies = manager.getHierarchies();
        this.dictionary = manager.getDataGeneralized().getDictionary();
    }

    /** Hook for use of census data */
    public static boolean               USE_CENSUS_DATA  = false;


    private GeneralizationHierarchy[] hierarchies;
    private Dictionary dictionary;

    /**
     * Creates the census map
     * @return
     */
    private Map<Integer, Set<String>>[] getCensusMap() {
        @SuppressWarnings("unchecked")
        Map<Integer, Set<String>>[] result = new Map[4];
        for (int i=0; i<hierarchies.length; i++) {
            result[i] = new HashMap<Integer, Set<String>>();
            int[][] array = hierarchies[i].getArray();
            for (int column = 0; column < array[0].length; column++) {
                for (int row = 0; row < array.length; row++) {
                    String value = dictionary.getMapping()[i][array[row][0]];
                    int key = array[row][column];
                    if (!result[i].containsKey(key)) {
                        result[i].put(key, new HashSet<String>());
                    }
                    result[i].get(key).add(value);
                }
            }
        }
        return result;
    }

    private Map<Integer, Set<String>>[] CENSUS_map = null;

    private double getSuccessProbabilityCensus(HashGroupifyEntry entry) {
        if (CENSUS_map == null) {
            CENSUS_map = getCensusMap();
        }
        double size = 0d;
        for(String value0 : CENSUS_map[0].get(entry.key[0])) {
            for(String value1 : CENSUS_map[1].get(entry.key[1])) {
                for(String value2 : CENSUS_map[2].get(entry.key[2])) {
                    for(String value3 : CENSUS_map[3].get(entry.key[3])) {
                        size += PopulationStatistics.getSize(new String[]{value0, value1, value2, value3});
                    }    
                }    
            }    
        }
        if (size == 0d) {
            throw new RuntimeException("This record does not exist in the population!");
        }
        double risk = 1d / size;
        if (risk > 1d) {
            risk = 1d;
        }
        if (risk > getSuccessProbability(entry)) {
            throw new RuntimeException("Population risk is > record risk!");
        }
        //System.out.println("CENSUS: " + risk + "/" + getSuccessProbability(entry));
        return risk;
    }

    @Override
    public boolean isLocalRecodingSupported() {
        return true;
    }

    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    @Override
    public boolean isAnonymous(Transformation transformation, HashGroupifyEntry entry) {
        
        // This is a class containing only records from the population
        if (entry.count == 0) {
            return false;
        }
        
        double successProbability = USE_CENSUS_DATA ? getSuccessProbabilityCensus(entry) : getSuccessProbability(entry);
        double adversaryPayoff = (config.getAdversaryGain() * successProbability - config.getAdversaryCost());
        if (NAIVE_NO_ATTACK && adversaryPayoff >= 0){
            return false;
        }
        
        // Calculate information loss and success probability
        double informationLoss = MetricSDNMEntropyBasedInformationLoss.getEntropyBasedInformationLoss(transformation,
                                                                                                      entry,
                                                                                                      shares,
                                                                                                      this.microaggregationFunctions,
                                                                                                      this.microaggregationStartIndex,
                                                                                                      maxIL);
        
        // Arguments will be checked in subsequent method calls
        double publisherPayoff = (config.getPublisherBenefit() * (1d - informationLoss)) - 
               (adversaryPayoff > 0 ? config.getPublisherLoss() * successProbability : 0);
        
        // We keep the set of records if the payoff is sufficient
        return !optimize ? (publisherPayoff > 0) : (publisherPayoff > ((1d - gsFactor) * config.getPublisherBenefit()));
    }

    @Override
    public boolean isSubsetAvailable() {
        return true;
    }
    
    @Override
    public String toString() {
        return toString("journalist");
    }

    @Override
    protected double getSuccessProbability(HashGroupifyEntry entry) {
        return entry.pcount == 0 ? 1d / entry.count : 1d / entry.pcount;
    }
}