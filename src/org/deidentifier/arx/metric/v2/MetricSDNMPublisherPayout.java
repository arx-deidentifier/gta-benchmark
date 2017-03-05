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

package org.deidentifier.arx.metric.v2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXCostBenefitConfiguration;
import org.deidentifier.arx.DataDefinition;
import org.deidentifier.arx.PopulationStatistics;
import org.deidentifier.arx.framework.check.distribution.DistributionAggregateFunction;
import org.deidentifier.arx.framework.check.groupify.HashGroupify;
import org.deidentifier.arx.framework.check.groupify.HashGroupifyEntry;
import org.deidentifier.arx.framework.data.Data;
import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.data.Dictionary;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;
import org.deidentifier.arx.framework.lattice.Transformation;
import org.deidentifier.arx.metric.InformationLossWithBound;
import org.deidentifier.arx.metric.MetricConfiguration;
import org.deidentifier.arx.metric.MetricConfiguration.MetricConfigurationAttackerModel;
import org.deidentifier.arx.risk.RiskModelCostBenefit;

/**
 * This class implements a model which maximizes publisher benefit according to the model proposed in:<br>
 * A Game Theoretic Framework for Analyzing Re-Identification Risk.
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin
 * PLOS|ONE. 2015.
 * 
 * @author Fabian Prasser
 */
public class MetricSDNMPublisherPayout extends AbstractMetricSingleDimensional {

    /** SUID. */
    private static final long           serialVersionUID = 5729454129866471107L;

    /** Parameter strings */
    private static final String         PUBLISHER_PAYOUT = "Publisher payout";

    /** Parameter strings */
    private static final String         MAXIMAL_PAYOUT   = "Theoretical maximum";

    /** Hook for use of census data */
    public static boolean               USE_CENSUS_DATA  = false;

    /** Configuration for the Stackelberg game */
    private ARXCostBenefitConfiguration config;

    /** Domain shares for each dimension. */
    private DomainShare[]               shares;

    /** Maximal information loss */
    private double                      maxIL;

    /** Risk model */
    private RiskModelCostBenefit        modelRisk;

    /** Journalist attacker model */
    private boolean                     journalistAttackerModel;

    /** Maximal payout */
    private QualityMetadata<Double>     maximalPayout;
    
    private Map<Integer, Set<String>>[] CENSUS_map = null;

    /**
     * Creates a new instance. Default constructor which treats all transformation methods equally.
     * @param journalistAttackerModel If set to true, the journalist attacker model will be assumed, 
     *                                the prosecutor model will be assumed, otherwise
     */
    public MetricSDNMPublisherPayout(boolean journalistAttackerModel) {
       this(journalistAttackerModel, 0.5d);
    }
    
    /**
     * Creates a new instance
     * @param journalistAttackerModel If set to true, the journalist attacker model will be assumed, 
     *                                the prosecutor model will be assumed, otherwise
     * @param gsFactor A factor [0,1] weighting generalization and suppression.
     *            The default value is 0.5, which means that generalization
     *            and suppression will be treated equally. A factor of 0
     *            will favor suppression, and a factor of 1 will favor
     *            generalization. The values in between can be used for
     *            balancing both methods.
     */
    public MetricSDNMPublisherPayout(boolean journalistAttackerModel, double gsFactor) {
        super(false, false, false, gsFactor);
        this.journalistAttackerModel = journalistAttackerModel;
    }
    
    @Override
    public ILSingleDimensional createMaxInformationLoss() {
        Double rows = getNumTuples();
        if (rows == null) {
            throw new IllegalStateException("Metric must be initialized first");
        } else {
            return new ILSingleDimensional(rows * this.config.getPublisherBenefit());
        }
    }

    @Override
    public ILSingleDimensional createMinInformationLoss() {
        return new ILSingleDimensional(0d);
    }

    /**
     * Returns the configuration of this metric.
     * 
     * @return
     */
    public MetricConfiguration getConfiguration() {
        return new MetricConfiguration(false, 
                                       super.getGeneralizationSuppressionFactor(), // gs-factor
                                       false, 
                                       0.0d, 
                                       this.getAggregateFunction(),
                                       this.journalistAttackerModel ? MetricConfigurationAttackerModel.JOURNALIST : 
                                                                      MetricConfigurationAttackerModel.PROSECUTOR);
    }
    
    /**
     * Returns the cost/benefit configuration
     */
    public ARXCostBenefitConfiguration getCostBenefitConfiguration() {
        return this.config;
    }

    @Override
    public String getName() {
        return "Publisher benefit";
    }

    @Override
    public boolean isAbleToHandleMicroaggregation() {
        return true;
    }

    @Override
    public boolean isGSFactorSupported() {
        return true;
    }

    /**
     * Returns whether the journalist attacker model is being assumed.
     * @return
     */
    public boolean isJournalistAttackerModel() {
        return this.journalistAttackerModel;
    }

    /**
     * Returns whether the prosecutor attacker model is being assumed.
     * @return
     */
    public boolean isProsecutorAttackerModel() {
        return !this.journalistAttackerModel;
    }

    @Override
    public String toString() {
        String result = "PublisherBenefit (" + (journalistAttackerModel ? "Journalist" : "Prosecutor");
        if (config == null) {
            result += ")";
        } else {
            result += ", Benefit=" + config.getPublisherBenefit() + ")";
        }
        return result;
    }

    /**
     * Returns the success probability. If the game is configured to use journalist risk, 
     * but no population table is available, we silently default to the prosecutor model.
     * @param entry
     * @return
     */
    private double getSuccessProbability(HashGroupifyEntry entry) {
        return !journalistAttackerModel || entry.pcount == 0 ? 1d / entry.count : 1d / entry.pcount;
    }


    /**
     * Returns the success probability. If the game is configured to use journalist risk, 
     * but no population table is available, we silently default to the prosecutor model.
     * @param entry
     * @return
     */
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
    protected ILSingleDimensionalWithBound getInformationLossInternal(Transformation transformation, HashGroupify groupify) {
        
        if (USE_CENSUS_DATA) {
            System.out.println("USE_CENSUS");
        }
        
        // Prepare
        double real = 0;
        double bound = 0;
        double gFactor = super.getGeneralizationFactor();
        double sFactor = super.getSuppressionFactor();
        HashGroupifyEntry entry = groupify.getFirstEquivalenceClass();
        double maxPayout = this.config.getPublisherBenefit();
        double payout = 0d;
        DistributionAggregateFunction[] microaggregationFunctions = super.getMicroaggregationFunctions();
        int microaggregationStartIndex = super.getMicroaggregationStartIndex();
        
        // Compute
        while (entry != null) {
            if (entry.count > 0) {

                double adversarySuccessProbability = USE_CENSUS_DATA ? this.getSuccessProbabilityCensus(entry) : this.getSuccessProbability(entry);
                double informationLoss = MetricSDNMEntropyBasedInformationLoss.getEntropyBasedInformationLoss(transformation,
                                                                                                              entry,
                                                                                                              shares,
                                                                                                              microaggregationFunctions,
                                                                                                              microaggregationStartIndex,
                                                                                                              maxIL);
                double realPayout = modelRisk.getExpectedPublisherPayout(informationLoss, adversarySuccessProbability);
                double boundPayout = modelRisk.getExpectedPublisherPayout(informationLoss, 0d);
                real += !entry.isNotOutlier ? (sFactor * entry.count * maxPayout) : 
                                              (gFactor * entry.count * (maxPayout - realPayout));
                bound += gFactor * entry.count * (maxPayout - boundPayout);
                payout += !entry.isNotOutlier ? 0d : entry.count * realPayout;
            }
            entry = entry.nextOrdered;
        }
        
        // Return
        ILSingleDimensionalWithBound result = super.createInformationLoss(real, bound);
        result.getInformationLoss().addMetadata(new QualityMetadata<Double>(PUBLISHER_PAYOUT, payout));
        result.getInformationLoss().addMetadata(maximalPayout);
        return result;
    }

    @Override
    protected InformationLossWithBound<ILSingleDimensional> getInformationLossInternal(Transformation transformation, HashGroupifyEntry entry) {

        // Prepare
        double gFactor = super.getGeneralizationFactor();
        double sFactor = super.getSuppressionFactor();
        DistributionAggregateFunction[] microaggregationFunctions = super.getMicroaggregationFunctions();
        int microaggregationStartIndex = super.getMicroaggregationStartIndex();
        
        // Compute
        double adversarySuccessProbability = this.getSuccessProbability(entry);
        double informationLoss = MetricSDNMEntropyBasedInformationLoss.getEntropyBasedInformationLoss(transformation,
                                                                                                      entry,
                                                                                                      shares,
                                                                                                      microaggregationFunctions,
                                                                                                      microaggregationStartIndex,
                                                                                                      maxIL);
        double maxPayout = this.config.getPublisherBenefit();
        double realPayout = modelRisk.getExpectedPublisherPayout(informationLoss, adversarySuccessProbability);
        double boundPayout = modelRisk.getExpectedPublisherPayout(informationLoss, 0d);
        double real =  !entry.isNotOutlier ? (sFactor * entry.count * maxPayout) : 
                                             (gFactor * entry.count * (maxPayout - realPayout));
        double bound = gFactor * entry.count * (maxPayout - boundPayout);

        // Return
        return super.createInformationLoss(real, bound);
    }
    
    @Override
    protected ILSingleDimensional getLowerBoundInternal(Transformation transformation) {
        return null;
    }

    @Override
    protected ILSingleDimensional getLowerBoundInternal(Transformation transformation,
                                                        HashGroupify groupify) {

        // Compute
        double bound = 0;
        double gFactor = super.getGeneralizationFactor();
        double maxPayout = this.config.getPublisherBenefit();
        HashGroupifyEntry entry = groupify.getFirstEquivalenceClass();
        while (entry != null) {
            if (entry.count > 0) {
                double informationLoss = MetricSDNMEntropyBasedInformationLoss.getEntropyBasedInformationLoss(transformation, entry, shares, null, 0, maxIL);
                double boundPayout = modelRisk.getExpectedPublisherPayout(informationLoss, 0d);
                bound += gFactor * entry.count * (maxPayout - boundPayout);
            }
            entry = entry.nextOrdered;
        }
        
        // Return
        return new ILSingleDimensional(bound);
    }
    
    private GeneralizationHierarchy[] hierarchies;
    private Dictionary dictionary;

    @Override
    protected void initializeInternal(final DataManager manager,
                                      final DataDefinition definition,
                                      final Data input,
                                      final GeneralizationHierarchy[] hierarchies,
                                      final ARXConfiguration config) {

        super.initializeInternal(manager, definition, input, hierarchies, config);

        // Compute domain shares
        this.hierarchies = hierarchies;
        this.dictionary = manager.getDataGeneralized().getDictionary();
        this.shares =  manager.getDomainShares();
        this.config = config.getCostBenefitConfiguration();
        this.modelRisk = new RiskModelCostBenefit(this.config);
        this.maximalPayout = new QualityMetadata<Double>(MAXIMAL_PAYOUT, super.getNumRecords(config, input) * this.config.getPublisherBenefit());
                
        // Calculate MaxIL
        this.maxIL = MetricSDNMEntropyBasedInformationLoss.getMaximalEntropyBasedInformationLoss(this.shares, super.getMicroaggregationDomainSizes());
    }

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
}
