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
import org.deidentifier.arx.exceptions.RollbackRequiredException;

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
public abstract class BenchmarkExperiment6 extends BenchmarkExperiment {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK                = new Benchmark(new String[] { "adversary gain = publisher loss" });
    /** TOTAL */
    private static final int       TIME_FULL_DOMAIN         = BENCHMARK.addMeasure("TIME Full-domain generalization + record suppression");
    /** TOTAL */
    private static final int       PAYOUT_FULL_DOMAIN       = BENCHMARK.addMeasure("PAYOUT Full-domain generalization + record suppression");
    /** TOTAL */
    private static final int       TIME_OPTIMAL_RECORD      = BENCHMARK.addMeasure("TIME Optimal record-level generalization");
    /** TOTAL */
    private static final int       PAYOUT_OPTIMAL_RECORD    = BENCHMARK.addMeasure("PAYOUT Optimal record-level generalization");
    /** TOTAL */
    private static final int       TIME_MULTI_DIMENSIONAL   = BENCHMARK.addMeasure("TIME Multi-dimensional global recoding");
    /** TOTAL */
    private static final int       PAYOUT_MULTI_DIMENSIONAL = BENCHMARK.addMeasure("PAYOUT Multi-dimensional global recoding");

    /**
     * Main
     * @param args
     * @throws IOException
     * @throws RollbackRequiredException 
     */
    public static void main(String[] args) throws IOException, RollbackRequiredException {

        BenchmarkDataset dataset = BenchmarkSetup.getBenchmarkDataset(args[0]);

        // Perform
        ARXCostBenefitConfiguration config = ARXCostBenefitConfiguration.create()
                                                                        .setAdversaryCost(BenchmarkSetup.getDefaultAdversaryCost())
                                                                        .setAdversaryGain(BenchmarkSetup.getDefaultAdversaryGain())
                                                                        .setPublisherLoss(BenchmarkSetup.getDefaultPublisherLoss())
                                                                        .setPublisherBenefit(BenchmarkSetup.getDefaultPublisherBenefit());

        // Init
        BENCHMARK.addAnalyzer(TIME_FULL_DOMAIN, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_FULL_DOMAIN, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_OPTIMAL_RECORD, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_OPTIMAL_RECORD, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_MULTI_DIMENSIONAL, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_MULTI_DIMENSIONAL, new ValueBuffer());
        
        // Perform
        for (double gainloss : BenchmarkSetup.getParametersGainLoss()) {
            config.setAdversaryGain(gainloss);
            config.setPublisherLoss(gainloss);
            System.out.println(gainloss);
            BENCHMARK.addRun(config.getAdversaryGain());
            analyze(dataset, config);
            BENCHMARK.getResults().write(new File("results/"+dataset.toString()+"-experiment6.csv"));
        }
    }

    /**
     * Run the benchmark
     * @param dataset
     * @param config
     * @throws IOException
     * @throws RollbackRequiredException 
     */
    private static void analyze(BenchmarkDataset dataset, ARXCostBenefitConfiguration configuration) throws IOException, RollbackRequiredException {

        final int REPETITIONS = BenchmarkSetup.getNumberOfRepetitions(dataset);
     
        // Load data
        Data data = BenchmarkSetup.getData(dataset);
        
        // GLOBAL RECODING TIME

        // Warmup run
        getExecutionTime(data, configuration, true);
        
        // Repetitions
        double time = 0d;
        for (int i=0; i < REPETITIONS; i++) {
            time += getExecutionTime(data, configuration, true); 
        }
        time /= (REPETITIONS * 1000d);
        BENCHMARK.addValue(TIME_FULL_DOMAIN, time);
        BENCHMARK.addValue(PAYOUT_FULL_DOMAIN, getCostBenefitPayout(data, configuration));
        
        time = System.currentTimeMillis();
        BENCHMARK.addValue(PAYOUT_OPTIMAL_RECORD, getRecordLevelPayout(data, configuration, true));
        time = (System.currentTimeMillis() - time) / 1000d;
        BENCHMARK.addValue(TIME_OPTIMAL_RECORD, time);

        time = System.currentTimeMillis();
        BENCHMARK.addValue(PAYOUT_MULTI_DIMENSIONAL, getMultidimensionalGlobalPayout(data, configuration));
        time = (System.currentTimeMillis() - time) / 1000d;
        BENCHMARK.addValue(TIME_MULTI_DIMENSIONAL, time);
    }
}
