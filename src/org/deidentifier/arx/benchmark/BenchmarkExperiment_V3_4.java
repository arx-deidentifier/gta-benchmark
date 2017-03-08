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
import java.io.IOException;

import org.deidentifier.arx.ARXCostBenefitConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataSubset;
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
public abstract class BenchmarkExperiment_V3_4 extends BenchmarkExperiment {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK                         = new Benchmark(new String[] { "adversary gain = publisher loss" });
    /** MEASUREMENT PARAMETER */
    private static final int       TIME_FULL_DOMAIN                  = BENCHMARK.addMeasure("[PopulationTable] Full-domain generalization + record suppression");
    /** MEASUREMENT PARAMETER */
    private static final int       TIME_FULL_DOMAIN_NO_ATTACK        = BENCHMARK.addMeasure("[PopulationTable] Full-domain generalization + record suppression (no attack)");
    /** MEASUREMENT PARAMETER */
    private static final int       TIME_FULL_DOMAIN_NO_ATTACK_NAIVE  = BENCHMARK.addMeasure("[PopulationTable] NAIVE Full-domain generalization + record suppression (no attack)");

    /**
     * Main
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        BenchmarkDataset dataset = BenchmarkDataset.ADULT_TN_288_TABLE;

        // Init
        BENCHMARK.addAnalyzer(TIME_FULL_DOMAIN, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_FULL_DOMAIN_NO_ATTACK, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_FULL_DOMAIN_NO_ATTACK_NAIVE, new ValueBuffer());
        
        // Perform
        ARXCostBenefitConfiguration config = ARXCostBenefitConfiguration.create()
                                                                        .setAdversaryCost(BenchmarkSetup.getDefaultAdversaryCost())
                                                                        .setAdversaryGain(BenchmarkSetup.getDefaultAdversaryGain())
                                                                        .setPublisherLoss(BenchmarkSetup.getDefaultPublisherLoss())
                                                                        .setPublisherBenefit(BenchmarkSetup.getDefaultPublisherBenefit());

        double[] parameters = BenchmarkSetup.getParametersGainLoss2();
        for (double parameter : new double[]{0d, 300d, 2000d}) {
            config.setAdversaryGain(parameter);
            config.setPublisherLoss(parameter);
            System.out.println(" - Adversary gain = publisher loss - " + parameter + " - " + Arrays.toString(parameters));
            BENCHMARK.addRun(config.getAdversaryGain());
            analyze(dataset, config);
            BENCHMARK.getResults().write(new File("results/"+dataset.toString()+"-experiment-v3-4.csv"));
        }
    }

    /**
     * Run the benchmark
     * @param dataset
     * @param dataset_sh 
     * @param config
     * @throws IOException
     */
    private static void analyze(BenchmarkDataset dataset,  final ARXCostBenefitConfiguration configuration) throws IOException {
     
        // Load data
        final Data data = BenchmarkSetup.getData(dataset);
        final DataSubset subset = BenchmarkSetup.getDataSubset(dataset);
        
        // Run benchmarks
        BENCHMARK.addValue(TIME_FULL_DOMAIN, getExecutionTime(new Runnable(){
            public void run() {
                try {
                    getCostBenefitPayoutPopulationTable(data, subset, configuration);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }));
        
        BENCHMARK.addValue(TIME_FULL_DOMAIN_NO_ATTACK, getExecutionTime(new Runnable(){
            public void run() {
                try {
                    getFullDomainPayoutNoAttackPopulationTable(data, subset, configuration);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }));

        BENCHMARK.addValue(TIME_FULL_DOMAIN_NO_ATTACK_NAIVE, getExecutionTime(new Runnable(){
            public void run() {
                try {
                    getFullDomainPayoutNoAttackNaivePopulationTable(data, subset, configuration);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }));
    }

    private static double getExecutionTime(Runnable runnable) {
        runnable.run();
        long time = System.currentTimeMillis();
        runnable.run();
        runnable.run();
        runnable.run();
        runnable.run();
        runnable.run();
        runnable.run();
        runnable.run();
        runnable.run();
        runnable.run();
        runnable.run();
        return (double)(System.currentTimeMillis()-time) / 10d;
    }
}