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

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.DataSubset;
import org.deidentifier.arx.framework.check.groupify.HashGroupifyEntry;
import org.deidentifier.arx.framework.data.DataManager;
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
        
        // Calculate information loss and success probability
        double informationLoss = MetricSDNMEntropyBasedInformationLoss.getEntropyBasedInformationLoss(transformation,
                                                                                                      entry,
                                                                                                      shares,
                                                                                                      this.microaggregationFunctions,
                                                                                                      this.microaggregationStartIndex,
                                                                                                      maxIL);
        double successProbability = getSuccessProbability(entry);
        double publisherPayoff = riskModel.getExpectedPublisherPayout(informationLoss, successProbability);
                
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