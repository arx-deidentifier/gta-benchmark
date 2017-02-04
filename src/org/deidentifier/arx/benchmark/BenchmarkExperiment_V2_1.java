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
public abstract class BenchmarkExperiment_V2_1 extends BenchmarkExperiment {


    /** The benchmark instance */
    private static final Benchmark BENCHMARK           = new Benchmark(new String[] {"adversary gain = publisher loss"});
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_COST_BENEFIT = BENCHMARK.addMeasure("Cost/benefit");
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_09_AVG_RISK  = BENCHMARK.addMeasure("9% avg. risk");
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_20_AVG_RISK  = BENCHMARK.addMeasure("20% avg. risk");
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_09_IND_RISK  = BENCHMARK.addMeasure("9% ind. risk");
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_20_IND_RISK  = BENCHMARK.addMeasure("20% ind. risk");

    /**
     * Main
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        BenchmarkDataset dataset = BenchmarkSetup.getBenchmarkDataset("adult-tn");

        // Init
        BENCHMARK.addAnalyzer(PAYOUT_COST_BENEFIT, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_09_AVG_RISK, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_20_AVG_RISK, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_09_IND_RISK, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_20_IND_RISK, new ValueBuffer());
        
        // Perform
        ARXCostBenefitConfiguration config = ARXCostBenefitConfiguration.create()
                                                                        .setAdversaryCost(BenchmarkSetup.getDefaultAdversaryCost())
                                                                        .setAdversaryGain(BenchmarkSetup.getDefaultAdversaryGain())
                                                                        .setPublisherLoss(BenchmarkSetup.getDefaultPublisherLoss())
                                                                        .setPublisherBenefit(BenchmarkSetup.getDefaultPublisherBenefit());

        double[] parameters = BenchmarkSetup.getParametersGainLoss2();
        for (double parameter : parameters) {
            config.setAdversaryGain(parameter);
            config.setPublisherLoss(parameter);
            System.out.println(" - Adversary gain = publisher loss - " + parameter + " - " + Arrays.toString(parameters));
            BENCHMARK.addRun(config.getAdversaryGain());
            analyze(dataset, config);
            BENCHMARK.getResults().write(new File("results/"+dataset.toString()+"-experiment-v2-1.csv"));
        }
    }

    /**
     * Run the benchmark
     * @param dataset
     * @param config
     * @throws IOException
     */
    private static void analyze(BenchmarkDataset dataset, ARXCostBenefitConfiguration configuration) throws IOException {
     
        // Load data
        Data data = BenchmarkSetup.getData(dataset);
        
        // Run benchmarks
        BENCHMARK.addValue(PAYOUT_COST_BENEFIT, getCostBenefitPayout(data, configuration));
        BENCHMARK.addValue(PAYOUT_09_AVG_RISK, getAverageRiskPayout(data, configuration, 11));
        BENCHMARK.addValue(PAYOUT_20_AVG_RISK, getAverageRiskPayout(data, configuration, 5));
        BENCHMARK.addValue(PAYOUT_09_IND_RISK, getIndividualRiskPayout(data, configuration, 11));
        BENCHMARK.addValue(PAYOUT_20_IND_RISK, getIndividualRiskPayout(data, configuration, 5));
    }
}