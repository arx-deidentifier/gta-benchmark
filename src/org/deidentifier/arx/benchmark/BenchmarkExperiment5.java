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
public abstract class BenchmarkExperiment5 extends BenchmarkExperiment {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK          = new Benchmark(new String[] { "adversary gain = publisher loss" });
    /** MEASUREMENT PARAMETER */
    private static final int       ALL_OPTIMIZATIONS  = BENCHMARK.addMeasure("With all optimizations");
    /** MEASUREMENT PARAMETER */
    private static final int       MICRO_OPTIMIZATION = BENCHMARK.addMeasure("Expression simplification");
    /** MEASUREMENT PARAMETER */
    private static final int       NO_OPTIMIZATION    = BENCHMARK.addMeasure("Without optimizations");

    /**
     * Main
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        BenchmarkDataset dataset = BenchmarkSetup.getBenchmarkDataset(args[0]);

        // Perform
        ARXCostBenefitConfiguration config = ARXCostBenefitConfiguration.create()
                                                                        .setAdversaryCost(BenchmarkSetup.getDefaultAdversaryCost())
                                                                        .setAdversaryGain(BenchmarkSetup.getDefaultAdversaryGain())
                                                                        .setPublisherLoss(BenchmarkSetup.getDefaultPublisherLoss())
                                                                        .setPublisherBenefit(BenchmarkSetup.getDefaultPublisherBenefit());

        // Init
        BENCHMARK.addAnalyzer(ALL_OPTIMIZATIONS, new ValueBuffer());
        BENCHMARK.addAnalyzer(MICRO_OPTIMIZATION, new ValueBuffer());
        BENCHMARK.addAnalyzer(NO_OPTIMIZATION, new ValueBuffer());

        // Perform
        for (double gainloss : BenchmarkSetup.getParametersGainLoss()) {
            config.setAdversaryGain(gainloss);
            config.setPublisherLoss(gainloss);
            System.out.println(gainloss);
            BENCHMARK.addRun(config.getAdversaryGain());
            analyze(dataset, config);
            BENCHMARK.getResults().write(new File("results/"+dataset.toString()+"-experiment5.csv"));
        }
    }

    /**
     * Run the benchmark
     * @param dataset
     * @param config
     * @throws IOException
     */
    private static void analyze(BenchmarkDataset dataset, ARXCostBenefitConfiguration configuration) throws IOException {

        final int REPETITIONS = BenchmarkSetup.getNumberOfRepetitions(dataset);
     
        // Load data
        Data data = BenchmarkSetup.getData(dataset);

        // ******************
        // W/o optimization
        // ******************
        
        double withoutOptimization = 0d;
        
        // Warmup run
        getExecutionTime(data, configuration, false, false);
        
        // Repetitions
        for (int i=0; i < REPETITIONS; i++) {
            withoutOptimization += getExecutionTime(data, configuration, false, false); 
        }
        withoutOptimization /= (REPETITIONS * 1000d);
        BENCHMARK.addValue(NO_OPTIMIZATION, withoutOptimization);

        // ******************
        // W/o optimization
        // ******************
        
        double microOptimization = 0d;
        
        // Warmup run
        getExecutionTime(data, configuration, false, true);
        
        // Repetitions
        for (int i=0; i < REPETITIONS; i++) {
            microOptimization += getExecutionTime(data, configuration, false, true); 
        }
        microOptimization /= (REPETITIONS * 1000d);
        BENCHMARK.addValue(MICRO_OPTIMIZATION, microOptimization);
        
        // ******************
        // With optimization
        // ******************
        
        double allOptimizations = 0d;

        // Warmup run
        getExecutionTime(data, configuration, true, true);
        
        // Repetitions
        for (int i=0; i < REPETITIONS; i++) {
            allOptimizations += getExecutionTime(data, configuration, true, true); 
        }
        allOptimizations /= (REPETITIONS * 1000d);
        BENCHMARK.addValue(ALL_OPTIMIZATIONS, allOptimizations);
    }
}
