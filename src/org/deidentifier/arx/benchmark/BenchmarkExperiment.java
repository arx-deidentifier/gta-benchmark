/*
 * Benchmark of ARX's implementation of the game theoretic approach proposed in:
 * A Game Theoretic Framework for Analyzing Re-Identification Risk.
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin
 * PLOS|ONE. 2015.
 * 
 * Copyright 2017 - Fabian Prasser
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
package org.deidentifier.arx.benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXCostBenefitConfiguration;
import org.deidentifier.arx.ARXLattice.ARXNode;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.DataSubset;
import org.deidentifier.arx.algorithm.FLASHAlgorithmImpl;
import org.deidentifier.arx.criteria.AverageReidentificationRisk;
import org.deidentifier.arx.criteria.DPresence;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.ProfitabilityJournalist;
import org.deidentifier.arx.criteria.ProfitabilityJournalistNoAttack;
import org.deidentifier.arx.criteria.ProfitabilityProsecutor;
import org.deidentifier.arx.criteria.ProfitabilityProsecutorNoAttack;
import org.deidentifier.arx.exceptions.RollbackRequiredException;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.v2.MetricMDNUNMNormalizedEntropyPotentiallyPrecomputed;
import org.deidentifier.arx.metric.v2.MetricSDNMEntropyBasedInformationLoss;
import org.deidentifier.arx.metric.v2.MetricSDNMKLDivergence;
import org.deidentifier.arx.metric.v2.MetricSDNMPublisherPayout;


/**
 * Benchmark of ARX's implementation of the game theoretic approach proposed in: <br>
 * A Game Theoretic Framework for Analyzing Re-Identification Risk. <br>
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin <br>
 * PLOS|ONE. 2015.
 *
 * @author Fabian Prasser
 */
public class BenchmarkExperiment {

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param k
     * @return
     * @throws IOException 
     */
    public static double getAverageRiskPayout(Data data, ARXCostBenefitConfiguration configuration, int k) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new AverageReidentificationRisk(1d/(double)k));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param k
     * @param metric 
     * @return
     * @throws IOException 
     */
    public static double getAverageRiskQuality(Data data, ARXCostBenefitConfiguration configuration, int k, Metric<?> metric) throws IOException {

        double quality = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        if (configuration != null) {
            config.setCostBenefitConfiguration(configuration);
        }
        config.setQualityModel(metric);
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new AverageReidentificationRisk(1d/(double)k));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        quality = Double.valueOf(result.getGlobalOptimum().getHighestScore().toString());
        data.getHandle().release();
        
        if (metric instanceof MetricMDNUNMNormalizedEntropyPotentiallyPrecomputed) {
            quality /= (double)data.getDefinition().getQuasiIdentifyingAttributes().size();
        } else if (metric instanceof MetricSDNMKLDivergence) {
            quality /= Double.valueOf(result.getLattice().getTop().getHighestScore().toString());
        }
        
        return 1d-quality;
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @return
     * @throws IOException 
     */
    public static double getCostBenefitPayoutCENSUS(Data data, ARXCostBenefitConfiguration configuration) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityProsecutor());
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        
        result.getOutput(result.getLattice().getTop(), false);
        MetricSDNMPublisherPayout.USE_CENSUS_DATA = true;
        result.getOutput(result.getGlobalOptimum(), false);
        MetricSDNMPublisherPayout.USE_CENSUS_DATA = false;
        
        payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }
    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @return
     * @throws IOException 
     */
    public static double getCostBenefitPayoutPopulationTable(Data data, DataSubset subset, ARXCostBenefitConfiguration configuration) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityJournalist(subset));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }
    
    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @return
     * @throws IOException 
     */
    public static double getCostBenefitPayout(Data data, ARXCostBenefitConfiguration configuration) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityProsecutor());
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }
    
    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param metric 
     * @return
     * @throws IOException 
     */
    public static double getCostBenefitPayout(Data data, ARXCostBenefitConfiguration configuration, Metric<?> metric) throws IOException {

        // Optimize for quality model
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(metric);
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityProsecutor());
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        Map<String, Integer> transformation = getTransformation(anonymizer.anonymize(data, config).getGlobalOptimum());
        data.getHandle().release();
        
        // Return associated payout
        config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityProsecutor());
        for (Entry<String, Integer> level : transformation.entrySet()) {
            data.getDefinition().setMinimumGeneralization(level.getKey(), level.getValue());
            data.getDefinition().setMaximumGeneralization(level.getKey(), level.getValue());
        }
        anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        double payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param metric 
     * @return
     * @throws IOException 
     */
    public static double getCostBenefitQuality(Data data, ARXCostBenefitConfiguration configuration, Metric<?> metric) throws IOException {

        double quality = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(metric);
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityProsecutor());
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        quality = Double.valueOf(result.getGlobalOptimum().getHighestScore().toString());

        if (metric instanceof MetricMDNUNMNormalizedEntropyPotentiallyPrecomputed) {
            quality /= (double)data.getDefinition().getQuasiIdentifyingAttributes().size();
        } else if (metric instanceof MetricSDNMKLDivergence) {
            quality /= Double.valueOf(result.getLattice().getTop().getHighestScore().toString());
        }
        data.getHandle().release();
        
        return 1d-quality;
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param metric 
     * @return
     * @throws IOException 
     */
    public static double getCostBenefitQuality2(Data data, ARXCostBenefitConfiguration configuration, Metric<?> metric) throws IOException {

        // Optimize for quality model
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(true));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityProsecutor());
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        int[] transformation = anonymizer.anonymize(data, config).getGlobalOptimum().getTransformation();
        data.getHandle().release();
        
        
        
        // Return associated payout
        config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(metric);
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityProsecutor());
        anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        for (ARXNode[] level : result.getLattice().getLevels()) {
            for (ARXNode node : level) {
                if (Arrays.equals(node.getTransformation(), transformation)) {
                    result.getOutput(node);
                    double quality = Double.valueOf(result.getGlobalOptimum().getHighestScore().toString());
                    if (metric instanceof MetricMDNUNMNormalizedEntropyPotentiallyPrecomputed) {
                        quality /= (double)data.getDefinition().getQuasiIdentifyingAttributes().size();
                    } else if (metric instanceof MetricSDNMKLDivergence) {
                        quality /= Double.valueOf(result.getLattice().getTop().getHighestScore().toString());
                    }
                    return 1d-quality;
                }
            }
        }
        data.getHandle().release();
        throw new IllegalStateException("Unknown transfomation");
    }
    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param pruning
     * @return
     * @throws IOException 
     */
    public static double getExecutionTime(Data data, ARXCostBenefitConfiguration configuration, boolean pruning) throws IOException {
        return getExecutionTime(data, configuration, pruning, true);
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param pruning
     * @param expressionSimplification
     * @return
     * @throws IOException 
     */
    public static double getExecutionTime(Data data, ARXCostBenefitConfiguration configuration, boolean pruning, boolean expressionSimplification) throws IOException {

        MetricSDNMEntropyBasedInformationLoss.OPTIMIZED = expressionSimplification;
        double result = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityProsecutor());
        FLASHAlgorithmImpl.USE_LOWER_BOUND = pruning;
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        long time = System.nanoTime();
        anonymizer.anonymize(data, config);
        time = System.nanoTime() - time;
        result = (double)time / 1000000d;
        data.getHandle().release();
        return result;
    }
    
    /**
     * Extracts a data subset
     * @param data
     * @param qis
     * @param records
     * @param attributes
     * @return
     */
    public static Data getExtract(Data data, String[] qis, int records, int attributes) {

        // Extract records
        List<String[]> list = new ArrayList<>();
        Iterator<String[]> iter = data.getHandle().iterator();
        while (iter.hasNext()) {
            list.add(iter.next());
            if (list.size() == records + 1) {
                break;
            }
        }
        
        Data result = Data.create(list.iterator());
        for (int i=0; i < attributes; i++) {
            result.getDefinition().setAttributeType(qis[i], Hierarchy.create(data.getDefinition().getHierarchy(qis[i])));
        }
        return result;
    }


    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param k
     * @return
     * @throws IOException 
     */
    public static double getIndividualRiskPayout(Data data, ARXCostBenefitConfiguration configuration, int k) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new KAnonymity(k));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param k
     * @param metric 
     * @return
     * @throws IOException 
     */
    public static double getIndividualRiskQuality(Data data, ARXCostBenefitConfiguration configuration, int k, Metric<?> metric) throws IOException {

        double quality = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        if (configuration != null) {
            config.setCostBenefitConfiguration(configuration);
        }
        config.setQualityModel(metric);
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new KAnonymity(k));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        quality = Double.valueOf(result.getGlobalOptimum().getHighestScore().toString());
        data.getHandle().release();

        if (metric instanceof MetricMDNUNMNormalizedEntropyPotentiallyPrecomputed) {
            quality /= (double)data.getDefinition().getQuasiIdentifyingAttributes().size();
        } else if (metric instanceof MetricSDNMKLDivergence) {
            quality /= Double.valueOf(result.getLattice().getTop().getHighestScore().toString());
        }
        
        return 1d-quality;
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @return
     * @throws IOException 
     * @throws RollbackRequiredException 
     */
    public static double getMultidimensionalGlobalPayout(Data data, ARXCostBenefitConfiguration configuration) throws IOException, RollbackRequiredException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(true, 0.05d));
        config.setMaxOutliers(1d);
        ProfitabilityJournalist profitability = new ProfitabilityJournalist(DataSubset.create(data, getSet(data)));
        profitability.setOptimize(true);
        config.addPrivacyModel(profitability);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        DataHandle handle = result.getOutput();
        payout += (Double) result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        result.optimizeIterative(handle, 0.05d, Integer.MAX_VALUE, 0.05d);
        handle.release();
        data.getHandle().release();
        payout += result.getPayout();
        payout /= (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
        return payout;
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getRecordLevelPayout(Data data, ARXCostBenefitConfiguration configuration, boolean optimal) throws IOException {

        double payout = 0d;
        int max = data.getHandle().getNumRows();
        //max = 100;
        
        for (int record=0; record<max; record++) {
            
            ARXConfiguration config = ARXConfiguration.create();
            config.setCostBenefitConfiguration(configuration);
            config.setQualityModel(Metric.createPublisherPayoutMetric(true));
            config.setMaxOutliers(0d);
            config.addPrivacyModel(new ProfitabilityJournalist(DataSubset.create(data, getSet(record))));
            if (!optimal) {
                config.setHeuristicSearchEnabled(true);
                config.setHeuristicSearchTimeLimit(100);
            }
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            ARXResult result = anonymizer.anonymize(data, config);
            payout += (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
            data.getHandle().release();
        }
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }
    
    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getRecordLevelPayoutCENSUS(Data data, ARXCostBenefitConfiguration configuration, boolean optimal) throws IOException {

        double payout = 0d;
        int max = data.getHandle().getNumRows();
        //max = 100;
        
        for (int record=0; record<max; record++) {
            
            ARXConfiguration config = ARXConfiguration.create();
            config.setCostBenefitConfiguration(configuration);
            config.setQualityModel(Metric.createPublisherPayoutMetric(true));
            config.setMaxOutliers(0d);
            config.addPrivacyModel(new ProfitabilityJournalist(DataSubset.create(data, getSet(record))));
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            ARXResult result = anonymizer.anonymize(data, config);
            
            result.getOutput(result.getLattice().getTop(), false);
            MetricSDNMPublisherPayout.USE_CENSUS_DATA = true;
            result.getOutput(result.getGlobalOptimum(), false);
            MetricSDNMPublisherPayout.USE_CENSUS_DATA = false;
            
            payout += (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
            data.getHandle().release();
        }
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }
    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getWanPayoutNoAttackCENSUS(Data data, ARXCostBenefitConfiguration configuration) throws IOException {

        ProfitabilityJournalist.USE_CENSUS_DATA = true;
        MetricSDNMPublisherPayout.USE_CENSUS_DATA = true;
        ProfitabilityJournalist.NAIVE_NO_ATTACK = true;
        double payout = 0d;
        int max = data.getHandle().getNumRows();
        //max = 100;
        
        for (int record=0; record<max; record++) {
            
            ARXConfiguration config = ARXConfiguration.create();
            config.setCostBenefitConfiguration(configuration);
            config.setQualityModel(Metric.createPublisherPayoutMetric(true));
            config.setMaxOutliers(0d);
            config.addPrivacyModel(new ProfitabilityJournalist(DataSubset.create(data, getSet(record))));
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            ARXResult result = anonymizer.anonymize(data, config);
            payout += (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
            data.getHandle().release();
        }
        MetricSDNMPublisherPayout.USE_CENSUS_DATA = false;
        ProfitabilityJournalist.USE_CENSUS_DATA = false;
        ProfitabilityJournalist.NAIVE_NO_ATTACK = false;
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }
    
    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getWanPayoutCENSUS(Data data, ARXCostBenefitConfiguration configuration, boolean optimal) throws IOException {

        ProfitabilityJournalist.USE_CENSUS_DATA = true;
        MetricSDNMPublisherPayout.USE_CENSUS_DATA = true;

        double payout = 0d;
        int max = data.getHandle().getNumRows();
        //max = 100;
        
        for (int record=0; record<max; record++) {
            
            ARXConfiguration config = ARXConfiguration.create();
            config.setCostBenefitConfiguration(configuration);
            config.setQualityModel(Metric.createPublisherPayoutMetric(true));
            config.setMaxOutliers(0d);
            config.addPrivacyModel(new ProfitabilityJournalist(DataSubset.create(data, getSet(record))));
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            ARXResult result = anonymizer.anonymize(data, config);
            payout += (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
            data.getHandle().release();
        }
        MetricSDNMPublisherPayout.USE_CENSUS_DATA = false;
        ProfitabilityJournalist.USE_CENSUS_DATA = false;

        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }
    
    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getRecordLevelPayoutNoAttack(Data data, ARXCostBenefitConfiguration configuration) throws IOException {

        double payout = 0d;
        int max = data.getHandle().getNumRows();
        for (int record=0; record<max; record++) {
            ARXConfiguration config = ARXConfiguration.create();
            config.setCostBenefitConfiguration(configuration);
            config.setQualityModel(Metric.createPublisherPayoutMetric(true));
            config.setMaxOutliers(0d);
            config.addPrivacyModel(new ProfitabilityJournalistNoAttack(DataSubset.create(data, getSet(record))));
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            ARXResult result = anonymizer.anonymize(data, config);
            payout += (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
            data.getHandle().release();
        }
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getRecordLevelPayoutNoAttackCENSUS(Data data, ARXCostBenefitConfiguration configuration) throws IOException {

        double payout = 0d;
        int max = data.getHandle().getNumRows();
        for (int record=0; record<max; record++) {
            
            ARXConfiguration config = ARXConfiguration.create();
            config.setCostBenefitConfiguration(configuration);
            config.setQualityModel(Metric.createPublisherPayoutMetric(true));
            config.setMaxOutliers(0d);
            config.addPrivacyModel(new ProfitabilityJournalistNoAttack(DataSubset.create(data, getSet(record))));
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            ARXResult result = anonymizer.anonymize(data, config);
            
            result.getOutput(result.getLattice().getTop(), false);
            MetricSDNMPublisherPayout.USE_CENSUS_DATA = true;
            result.getOutput(result.getGlobalOptimum(), false);
            MetricSDNMPublisherPayout.USE_CENSUS_DATA = false;
            
            payout += (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
            data.getHandle().release();
        }
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getRecordLevelPayoutNoAttackNaive(Data data, ARXCostBenefitConfiguration configuration) throws IOException {

        ProfitabilityJournalist.NAIVE_NO_ATTACK = true;
        double payout = 0d;
        int max = data.getHandle().getNumRows();
        for (int record=0; record<max; record++) {
            ARXConfiguration config = ARXConfiguration.create();
            config.setCostBenefitConfiguration(configuration);
            config.setQualityModel(Metric.createPublisherPayoutMetric(true));
            config.setMaxOutliers(0d);
            config.addPrivacyModel(new ProfitabilityJournalist(DataSubset.create(data, getSet(record))));
            ARXAnonymizer anonymizer = new ARXAnonymizer();
            ARXResult result = anonymizer.anonymize(data, config);
            payout += (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
            data.getHandle().release();
        }
        ProfitabilityJournalist.NAIVE_NO_ATTACK = false;
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getFullDomainPayoutNoAttackPopulationTable(Data data, DataSubset subset, ARXCostBenefitConfiguration configuration) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(true));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityJournalistNoAttack(subset));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout += (Double) result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getFullDomainPayoutNoAttack(Data data, ARXCostBenefitConfiguration configuration) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(true));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityProsecutorNoAttack());
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout += (Double) result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getFullDomainPayoutNoAttackCENSUS(Data data, ARXCostBenefitConfiguration configuration) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(true));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityProsecutorNoAttack());
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        
        result.getOutput(result.getLattice().getTop(), false);
        MetricSDNMPublisherPayout.USE_CENSUS_DATA = true;
        result.getOutput(result.getGlobalOptimum(), false);
        MetricSDNMPublisherPayout.USE_CENSUS_DATA = false;
        
        payout += (Double) result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getFullDomainPayoutNoAttackNaivePopulationTable(Data data, DataSubset subset, ARXCostBenefitConfiguration configuration) throws IOException {
        ProfitabilityJournalist.NAIVE_NO_ATTACK = true;
        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(true));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityJournalist(subset));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout += (Double) result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        ProfitabilityJournalist.NAIVE_NO_ATTACK = false;
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimal 
     * @return
     * @throws IOException 
     */
    public static double getFullDomainPayoutNoAttackNaive(Data data, ARXCostBenefitConfiguration configuration) throws IOException {
        ProfitabilityJournalist.NAIVE_NO_ATTACK = true;
        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(true));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new ProfitabilityJournalist(DataSubset.create(data,data)));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout += (Double) result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        ProfitabilityJournalist.NAIVE_NO_ATTACK = false;
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run
     * @param data
     * @param configuration
     * @param optimized 
     * @return
     * @throws IOException 
     */
    public static void performRecordLevelGeneralizationNoAttack(Data data, ARXCostBenefitConfiguration configuration, boolean optimized) throws IOException {

        int max = data.getHandle().getNumRows();
        if (!optimized) {
            ProfitabilityJournalist.NAIVE_NO_ATTACK = true;
            for (int record=0; record<max; record++) {
                ARXConfiguration config = ARXConfiguration.create();
                config.setCostBenefitConfiguration(configuration);
                config.setQualityModel(Metric.createPublisherPayoutMetric(true));
                config.setMaxOutliers(1d);
                config.addPrivacyModel(new ProfitabilityJournalist(DataSubset.create(data, getSet(record))));
                ARXAnonymizer anonymizer = new ARXAnonymizer();
                anonymizer.anonymize(data, config);
                data.getHandle().release();
            }
            ProfitabilityJournalist.NAIVE_NO_ATTACK = false;
        } else {
            for (int record=0; record<max; record++) {
                ARXConfiguration config = ARXConfiguration.create();
                config.setCostBenefitConfiguration(configuration);
                config.setQualityModel(Metric.createEntropyBasedInformationLossMetric());
                config.setMaxOutliers(0d);
                config.addPrivacyModel(new ProfitabilityJournalistNoAttack(DataSubset.create(data, getSet(record))));
                ARXAnonymizer anonymizer = new ARXAnonymizer();
                anonymizer.anonymize(data, config);
                data.getHandle().release();
            }
        }
    }

    /**
     * Perform benchmark run.
     * @param data
     * @param subset
     * @param configuration
     * @return
     * @throws IOException 
     */
    public static double getSafeHarborPayout(Data data, DataSubset subset, ARXCostBenefitConfiguration configuration) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(true));
        config.setMaxOutliers(0d);
        // Ugly hack
        config.addPrivacyModel(new DPresence(0d, 1d, subset));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        System.out.println(payout);
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run.
     * @param data
     * @param configuration
     * @return
     * @throws IOException 
     */
    public static double getSafeHarborPayout(Data data, ARXCostBenefitConfiguration configuration) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        // Ugly hack
        config.addPrivacyModel(new DPresence(0d, 1d, DataSubset.create(data, data)));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Perform benchmark run.
     * @param data
     * @param configuration
     * @return
     * @throws IOException 
     */
    public static double getSafeHarborPayoutCENSUS(Data data, ARXCostBenefitConfiguration configuration) throws IOException {

        MetricSDNMPublisherPayout.USE_CENSUS_DATA = true;
        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        // Ugly hack
        config.addPrivacyModel(new DPresence(0d, 1d, DataSubset.create(data, data)));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        MetricSDNMPublisherPayout.USE_CENSUS_DATA = false;
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    /**
     * Returns a set including all records
     * @param data
     * @return
     */
    private static Set<Integer> getSet(Data data) {
        Set<Integer> result = new HashSet<>();
        for (int i=0; i<data.getHandle().getNumRows(); i++)  {
            result.add(i);
        }
        return result;
    }

    /**
     * Creates a set with one record
     * @param record
     * @return
     */
    private static Set<Integer> getSet(int record) {
        Set<Integer> result = new HashSet<Integer>();
        result.add(record);
        return result;
    }

    /**
     * Creates a map
     * @param transformation
     * @return
     */
    private static Map<String, Integer> getTransformation(ARXNode transformation) {
        Map<String, Integer> result = new HashMap<>();
        for (String qi : transformation.getQuasiIdentifyingAttributes()) {
            result.put(qi, transformation.getGeneralization(qi));
        }
        return result;
    }
}
