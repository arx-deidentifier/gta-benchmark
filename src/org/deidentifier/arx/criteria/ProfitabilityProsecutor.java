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
import org.deidentifier.arx.ARXCostBenefitConfiguration;
import org.deidentifier.arx.DataSubset;
import org.deidentifier.arx.framework.check.distribution.DistributionAggregateFunction;
import org.deidentifier.arx.framework.check.groupify.HashGroupifyEntry;
import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.lattice.Transformation;
import org.deidentifier.arx.metric.v2.DomainShare;
import org.deidentifier.arx.metric.v2.MetricSDNMEntropyBasedInformationLoss;
import org.deidentifier.arx.risk.RiskModelCostBenefit;

/**
 * Privacy model for the game theoretic approach proposed in:
 * A Game Theoretic Framework for Analyzing Re-Identification Risk.
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin
 * PLOS|ONE. 2015.
 * 
 * @author Fabian Prasser
 */
public class ProfitabilityProsecutor extends ImplicitPrivacyCriterion {

    /** SVUID */
    private static final long               serialVersionUID = -1698534839214708559L;

    public static boolean NAIVE_NO_ATTACK = false;

    /** Configuration */
    protected ARXCostBenefitConfiguration     config;

    /** Domain shares for each dimension. */
    protected DomainShare[]                   shares;

    /** The microaggregation functions. */
    protected DistributionAggregateFunction[] microaggregationFunctions;

    /** The start index of the attributes with microaggregation in the data array (dataAnalyzed) */
    protected int                             microaggregationStartIndex;

    /** Domain size for each microaggregated attribute */
    private int[]                           microaggregationDomainSizes;

    /** MaxIL */
    protected double                          maxIL;

    /** Risk model */
    protected RiskModelCostBenefit            riskModel;

    /**
     * Creates a new instance of game theoretic approach proposed in:
     * A Game Theoretic Framework for Analyzing Re-Identification Risk.
     * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton, 
     * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin
     * PLOS|ONE. 2015. 
     */
    public ProfitabilityProsecutor(){
        // This model is not monotonic:
        // Often, generalization only marginally reduces the adversary's success
        // probability but at the same time it significantly reduces the
        // publisher's payout - up to the point where it does not make sense to
        // keep a record anymore.
        super(false, false);
    }

    @Override
    public ProfitabilityProsecutor clone() {
        return new ProfitabilityProsecutor();
    }

    @Override
    public PrivacyCriterion clone(DataSubset subset) {
        return clone();
    }
    
    @Override
    public DataSubset getDataSubset() {
        return null;
    }
    
    @Override
    public int getRequirements(){
        return ARXConfiguration.REQUIREMENT_COUNTER;
    }

    @Override
    public void initialize(DataManager manager, ARXConfiguration config) {

        // Compute domain shares
        this.shares =  manager.getDomainShares();
        this.config = config.getCostBenefitConfiguration();
        this.riskModel = new RiskModelCostBenefit(this.config);

        // Prepare consideration of microaggregation
        this.microaggregationFunctions = manager.getMicroaggregationFunctions();
        this.microaggregationStartIndex = manager.getMicroaggregationStartIndex();
        this.microaggregationDomainSizes = manager.getMicroaggregationDomainSizes();
                
        // Calculate MaxIL
        this.maxIL = MetricSDNMEntropyBasedInformationLoss.getMaximalEntropyBasedInformationLoss(this.shares, this.microaggregationDomainSizes);
    }
    
    @Override
    public boolean isAnonymous(Transformation transformation, HashGroupifyEntry entry) {
        
        // This is a class containing only records from the population
        if (entry.count == 0) {
            return false;
        }
        
        double successProbability = getSuccessProbability(entry);
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
        
        double publisherPayoff = riskModel.getExpectedPublisherPayout(informationLoss, successProbability);
        
        // We keep the set of records if the payoff is > 0
        return publisherPayoff > 0;
    }

    @Override
    public boolean isLocalRecodingSupported() {
        return true;
    }
    
    @Override
    public boolean isSubsetAvailable() {
        return false;
    }
    
    @Override
    public String toString() {
        return toString("prosecutor");
    }

    /**
     * Returns the current configuration
     * @return
     */
    protected ARXCostBenefitConfiguration getConfiguration() {
        return this.config;
    }

    /**
     * Returns the success probability. If the game is configured to use the journalist risk, 
     * but no population table is available, we silently default to the prosecutor model.
     * @param entry
     * @return
     */
    protected double getSuccessProbability(HashGroupifyEntry entry) {
        return 1d / entry.count;
    }

    /**
     * Returns a string representation
     */
    protected String toString(String attackerModel) {
        return "profitability (" + attackerModel + ")" + (config != null ? config.toString() : "");
    }
}
