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

import java.io.File;
import java.util.concurrent.Callable;

import org.deidentifier.arx.ARXCostBenefitConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataSubset;
import org.deidentifier.arx.algorithm.FLASHAlgorithm;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;

import cern.colt.Arrays;
import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * Benchmark of ARX's implementation of the game theoretic approach proposed in: <br>
 * A Game Theoretic Framework for Analyzing Re-Identification Risk. <br>
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin <br>
 * PLOS|ONE. 2015.
 *
 * @author Fabian Prasser
 */
public abstract class BenchmarkExperiment_V3_6 extends BenchmarkExperiment {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK         = new Benchmark(new String[] { "adversary gain = publisher loss" });
    /** MEASUREMENT PARAMETER */
    private static final int       TABLE_NAIVE       = BENCHMARK.addMeasure("[PopulationTable] NAIVE Full-domain generalization + record suppression");
    /** MEASUREMENT PARAMETER */
    private static final int       TABLE_OPTIMIZED   = BENCHMARK.addMeasure("[PopulationTable] OPTIMIZED Full-domain generalization + record suppression");
    /** MEASUREMENT PARAMETER */
    private static final int       DATASET_NAIVE     = BENCHMARK.addMeasure("[Dataset] NAIVE Full-domain generalization + record suppression");
    /** MEASUREMENT PARAMETER */
    private static final int       DATASET_OPTIMIZED = BENCHMARK.addMeasure("[Dataset] OPTIMIZED Full-domain generalization + record suppression");

    /**
     * Main
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {

        // Init
        BENCHMARK.addAnalyzer(TABLE_NAIVE, new ValueBuffer());
        BENCHMARK.addAnalyzer(TABLE_OPTIMIZED, new ValueBuffer());
        BENCHMARK.addAnalyzer(DATASET_NAIVE, new ValueBuffer());
        BENCHMARK.addAnalyzer(DATASET_OPTIMIZED, new ValueBuffer());
        
        // Perform
        ARXCostBenefitConfiguration config = ARXCostBenefitConfiguration.create()
                                                                        .setAdversaryCost(BenchmarkSetup.getDefaultAdversaryCost())
                                                                        .setAdversaryGain(BenchmarkSetup.getDefaultAdversaryGain())
                                                                        .setPublisherLoss(BenchmarkSetup.getDefaultPublisherLoss())
                                                                        .setPublisherBenefit(BenchmarkSetup.getDefaultPublisherBenefit());

        double[] parameters = BenchmarkSetup.getParametersGainLoss3();
        for (double parameter : new double[]{0d,300d,2000d}){//parameters) {
            config.setAdversaryGain(parameter);
            config.setPublisherLoss(parameter);
            System.out.println(" - Adversary gain = publisher loss - " + parameter + " - " + Arrays.toString(parameters));
            BENCHMARK.addRun(config.getAdversaryGain());
            analyze(config);
            BENCHMARK.getResults().write(new File("results/adult-tn-288-experiment-v3-6.csv"));
        }
    }

    /**
     * Run the benchmark
     * @param dataset
     * @param dataset_sh 
     * @param config
     * @throws Exception 
     */
    private static void analyze(final ARXCostBenefitConfiguration configuration) throws Exception {
     
        // Load data
        final Data table = BenchmarkSetup.getData(BenchmarkDataset.ADULT_TN_288_TABLE);
        final Data dataset = BenchmarkSetup.getData(BenchmarkDataset.ADULT_TN_288);
        final DataSubset subset = BenchmarkSetup.getDataSubset(BenchmarkDataset.ADULT_TN_288_TABLE);
        
        FLASHAlgorithm.DEFAULT_TO_NONE_NONE = true;
        
        // Run benchmarks
        BENCHMARK.addValue(TABLE_NAIVE, getExecutionTime(new Callable<Long>(){
            @Override
            public Long call() throws Exception {
                return getTimeCostBenefitPayoutPopulationTableNoAttack(table, subset, configuration, false);
            }
        }));

        // Run benchmarks
        BENCHMARK.addValue(TABLE_OPTIMIZED, getExecutionTime(new Callable<Long>(){
            @Override
            public Long call() throws Exception {
                return getTimeCostBenefitPayoutPopulationTableNoAttack(table, subset, configuration, true);
            }
        }));

        // Run benchmarks
        BENCHMARK.addValue(DATASET_NAIVE, getExecutionTime(new Callable<Long>(){
            @Override
            public Long call() throws Exception {
                return getTimeCostBenefitPayoutNoAttack(dataset, configuration, false);
            }
        }));

        // Run benchmarks
        BENCHMARK.addValue(DATASET_OPTIMIZED, getExecutionTime(new Callable<Long>(){
            @Override
            public Long call() throws Exception {
                return getTimeCostBenefitPayoutNoAttack(dataset, configuration, true);
            }
        }));
    }

    private static double getExecutionTime(Callable<Long> callable) throws Exception {
        callable.call();
        callable.call();
        callable.call();
        int repetitions = 50;
        long time = 0;
        for (int i=0; i<repetitions; i++) {
            time += callable.call();
        }
        return (double) time / (double)repetitions;
    }
}