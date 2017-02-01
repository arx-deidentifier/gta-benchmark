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
public abstract class BenchmarkExperiment4 extends BenchmarkExperiment {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK   = new Benchmark(new String[] {});
    /** PARAM */
    private static final int       ATTRIBUTES  = BENCHMARK.addMeasure("Attributes");
    /** PARAM */
    private static final int       RECORDS     = BENCHMARK.addMeasure("Records");
    /** MEASUREMENT PARAM */
    private static final int       TIME_0      = BENCHMARK.addMeasure("Time-0");
    /** MEASUREMENT PARAM */
    private static final int       TIME_250    = BENCHMARK.addMeasure("Time-250");
    /** MEASUREMENT PARAM */
    private static final int       TIME_500    = BENCHMARK.addMeasure("Time-500");
    /** MEASUREMENT PARAM */
    private static final int       TIME_750    = BENCHMARK.addMeasure("Time-750");
    /** MEASUREMENT PARAM */
    private static final int       TIME_1000   = BENCHMARK.addMeasure("Time-1000");
    /** MEASUREMENT PARAM */
    private static final int       TIME_1250   = BENCHMARK.addMeasure("Time-1250");
    /** MEASUREMENT PARAM */
    private static final int       TIME_1500   = BENCHMARK.addMeasure("Time-1500");
    /** MEASUREMENT PARAM */
    private static final int       TIME_1750   = BENCHMARK.addMeasure("Time-1750");
    /** MEASUREMENT PARAM */
    private static final int       TIME_2000   = BENCHMARK.addMeasure("Time-2000");
    
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
        BENCHMARK.addAnalyzer(ATTRIBUTES, new ValueBuffer());
        BENCHMARK.addAnalyzer(RECORDS, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_0, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_250, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_500, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_750, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_1000, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_1250, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_1500, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_1750, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_2000, new ValueBuffer());
        
        // Setup
        String[] qis = BenchmarkSetup.getQuasiIdentifyingAttributes(dataset);
        
        // Perform
        for (int records = 2000; records <= 30000; records+=2000) {

            // Plos|ONE defaults
            config = ARXCostBenefitConfiguration.create()
                                                .setAdversaryCost(BenchmarkSetup.getDefaultAdversaryCost())
                                                .setAdversaryGain(BenchmarkSetup.getDefaultAdversaryGain())
                                                .setPublisherLoss(BenchmarkSetup.getDefaultPublisherLoss())
                                                .setPublisherBenefit(BenchmarkSetup.getDefaultPublisherBenefit());
            
            System.out.println("Records: " + records);
            BENCHMARK.addRun();
            analyze(dataset, config, qis, records, qis.length);
            BENCHMARK.getResults().write(new File("results/"+dataset.toString()+"-experiment4.csv"));
        }

        // Perform
        
        for (int attributes = 1; attributes <= qis.length; attributes++) {

            // Plos|ONE defaults
            config = ARXCostBenefitConfiguration.create()
                                                .setAdversaryCost(BenchmarkSetup.getDefaultAdversaryCost())
                                                .setAdversaryGain(BenchmarkSetup.getDefaultAdversaryGain())
                                                .setPublisherLoss(BenchmarkSetup.getDefaultPublisherLoss())
                                                .setPublisherBenefit(BenchmarkSetup.getDefaultPublisherBenefit());

            System.out.println("Attributes: " + attributes);
            BENCHMARK.addRun();
            analyze(dataset, config, qis, 30000, attributes);
            BENCHMARK.getResults().write(new File("results/"+dataset.toString()+"-experiment4.csv"));
        }
    }

    /**
     * Run the benchmark
     * @param dataset
     * @param qis 
     * @param config
     * @throws IOException
     */
    private static void analyze(BenchmarkDataset dataset, ARXCostBenefitConfiguration configuration, String[] qis, int records, int attributes) throws IOException {

        final int REPETITIONS = BenchmarkSetup.getNumberOfRepetitions(dataset);
 
        BENCHMARK.addValue(ATTRIBUTES, attributes);
        BENCHMARK.addValue(RECORDS, records);
        
        // Load data
        Data data = BenchmarkSetup.getData(dataset);
        data = getExtract(data, qis, records, attributes);

        double time = 0d;

        // Warmup run
        getExecutionTime(data, configuration, true);
        
        // Repetitions
        configuration.setAdversaryGain(0);
        configuration.setPublisherLoss(0);
        for (int i=0; i < REPETITIONS; i++) {
            time += getExecutionTime(data, configuration, true); 
        }
        time /= (REPETITIONS*1000d);
        BENCHMARK.addValue(TIME_0, time);
        time = 0d;

        // Repetitions
        configuration.setAdversaryGain(250);
        configuration.setPublisherLoss(250);
        for (int i=0; i < REPETITIONS; i++) {
            time += getExecutionTime(data, configuration, true); 
        }
        time /= (REPETITIONS*1000d);
        BENCHMARK.addValue(TIME_250, time);
        time = 0d;

        // Repetitions
        configuration.setAdversaryGain(500);
        configuration.setPublisherLoss(500);
        for (int i=0; i < REPETITIONS; i++) {
            time += getExecutionTime(data, configuration, true); 
        }
        time /= (REPETITIONS*1000d);
        BENCHMARK.addValue(TIME_500, time);
        time = 0d;

        // Repetitions
        configuration.setAdversaryGain(750);
        configuration.setPublisherLoss(750);
        for (int i=0; i < REPETITIONS; i++) {
            time += getExecutionTime(data, configuration, true); 
        }
        time /= (REPETITIONS*1000d);
        BENCHMARK.addValue(TIME_750, time);
        time = 0d;

        // Repetitions
        configuration.setAdversaryGain(1000);
        configuration.setPublisherLoss(1000);
        for (int i=0; i < REPETITIONS; i++) {
            time += getExecutionTime(data, configuration, true); 
        }
        time /= (REPETITIONS*1000d);
        BENCHMARK.addValue(TIME_1000, time);
        time = 0d;

        // Repetitions
        configuration.setAdversaryGain(1250);
        configuration.setPublisherLoss(1250);
        for (int i=0; i < REPETITIONS; i++) {
            time += getExecutionTime(data, configuration, true); 
        }
        time /= (REPETITIONS*1000d);
        BENCHMARK.addValue(TIME_1250, time);
        time = 0d;

        // Repetitions
        configuration.setAdversaryGain(1500);
        configuration.setPublisherLoss(1500);
        for (int i=0; i < REPETITIONS; i++) {
            time += getExecutionTime(data, configuration, true); 
        }
        time /= (REPETITIONS*1000d);
        BENCHMARK.addValue(TIME_1500, time);
        time = 0d;

        // Repetitions
        configuration.setAdversaryGain(1750);
        configuration.setPublisherLoss(1750);
        for (int i=0; i < REPETITIONS; i++) {
            time += getExecutionTime(data, configuration, true); 
        }
        time /= (REPETITIONS*1000d);
        BENCHMARK.addValue(TIME_1750, time);
        time = 0d;

        // Repetitions
        configuration.setAdversaryGain(2000);
        configuration.setPublisherLoss(2000);
        for (int i=0; i < REPETITIONS; i++) {
            time += getExecutionTime(data, configuration, true); 
        }
        time /= (REPETITIONS*1000d);
        BENCHMARK.addValue(TIME_2000, time);
        time = 0d;
    }
}
