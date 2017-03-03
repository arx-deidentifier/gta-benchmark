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

import org.deidentifier.arx.ARXCostBenefitConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.exceptions.RollbackRequiredException;

/**
 * Benchmark of ARX's implementation of the game theoretic approach proposed in: <br>
 * A Game Theoretic Framework for Analyzing Re-Identification Risk. <br>
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin <br>
 * PLOS|ONE. 2015.
 *
 * @author Fabian Prasser
 */
public abstract class BenchmarkExperimentNoAttack extends BenchmarkExperiment {

    /**
     * Main
     * @param args
     * @throws IOException
     * @throws RollbackRequiredException 
     */
    public static void main(String[] args) throws IOException, RollbackRequiredException {

        // Setup
        BenchmarkDataset dataset = BenchmarkSetup.getBenchmarkDataset("adult-tn-288");

        // Configure
        ARXCostBenefitConfiguration configuration = ARXCostBenefitConfiguration.create()
                                                                        .setAdversaryCost(BenchmarkSetup.getDefaultAdversaryCost())
                                                                        .setAdversaryGain(BenchmarkSetup.getDefaultAdversaryGain())
                                                                        .setPublisherLoss(BenchmarkSetup.getDefaultPublisherLoss())
                                                                        .setPublisherBenefit(BenchmarkSetup.getDefaultPublisherBenefit());

        // Perform
        for (double gainloss : new double[] { 300d }) {
            configuration.setAdversaryGain(gainloss);
            configuration.setPublisherLoss(gainloss);
            System.out.println("Gain=loss=" + gainloss);
            analyze(dataset, configuration, true);
            analyze(dataset, configuration, false);
        }
    }

    /**
     * Run the benchmark
     * @param dataset
     * @param config
     * @throws IOException
     * @throws RollbackRequiredException 
     */
    private static void analyze(BenchmarkDataset dataset, ARXCostBenefitConfiguration configuration, boolean optimized) throws IOException, RollbackRequiredException {

        final int REPETITIONS = 5;
     
        // Load data
        Data data = BenchmarkSetup.getData(dataset);
        
        // Warmup
        performRecordLevelGeneralizationNoAttack(data, configuration, optimized);
        
        // Benchmark
        double elapsed = System.currentTimeMillis();
        for (int i=0; i < REPETITIONS; i++) {
            performRecordLevelGeneralizationNoAttack(data, configuration, optimized);
        }
        elapsed = System.currentTimeMillis() - elapsed;
        elapsed /= (REPETITIONS * 1000d);
        System.out.println(" - Optimized: " + optimized + " -> Elapsed time: " + elapsed + " seconds");
    }
}
