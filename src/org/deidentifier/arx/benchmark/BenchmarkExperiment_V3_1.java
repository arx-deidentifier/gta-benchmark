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
public abstract class BenchmarkExperiment_V3_1 extends BenchmarkExperiment {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK                     = new Benchmark(new String[] { "adversary gain = publisher loss" });
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_FULL_DOMAIN            = BENCHMARK.addMeasure("Full-domain generalization + record suppression");
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_FULL_DOMAIN_NO_ATTACK = BENCHMARK.addMeasure("Full-domain generalization + record suppression (no attack)");
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_RECORD_LEVEL           = BENCHMARK.addMeasure("Record-level generalization");
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_RECORD_LEVEL_NO_ATTACK = BENCHMARK.addMeasure("Record-level generalization (no attack)");
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_SAFE_HARBOR            = BENCHMARK.addMeasure("HIPAA Safe Harbor");

    /**
     * Main
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        BenchmarkDataset dataset = BenchmarkDataset.ADULT_TN_288;
        BenchmarkDataset dataset_sh = BenchmarkDataset.ADULT_TN_SAFE_HARBOR;

        // Init
        BENCHMARK.addAnalyzer(PAYOUT_FULL_DOMAIN, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_FULL_DOMAIN_NO_ATTACK, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_RECORD_LEVEL, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_RECORD_LEVEL_NO_ATTACK, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_SAFE_HARBOR, new ValueBuffer());
        
        // Perform
        ARXCostBenefitConfiguration config = ARXCostBenefitConfiguration.create()
                                                                        .setAdversaryCost(BenchmarkSetup.getDefaultAdversaryCost())
                                                                        .setAdversaryGain(BenchmarkSetup.getDefaultAdversaryGain())
                                                                        .setPublisherLoss(BenchmarkSetup.getDefaultPublisherLoss())
                                                                        .setPublisherBenefit(BenchmarkSetup.getDefaultPublisherBenefit());

        double[] parameters = BenchmarkSetup.getParametersGainLoss2();
        for (double parameter : new double[]{50,100, 300}) {
            config.setAdversaryGain(parameter);
            config.setPublisherLoss(parameter);
            System.out.println(" - Adversary gain = publisher loss - " + parameter + " - " + Arrays.toString(parameters));
            BENCHMARK.addRun(config.getAdversaryGain());
            analyze(dataset, dataset_sh, config);
            BENCHMARK.getResults().write(new File("results/"+dataset.toString()+"-experiment-v3-1.csv"));
        }
    }

    /**
     * Run the benchmark
     * @param dataset
     * @param dataset_sh 
     * @param config
     * @throws IOException
     */
    private static void analyze(BenchmarkDataset dataset, BenchmarkDataset dataset_sh, ARXCostBenefitConfiguration configuration) throws IOException {
     
        // Load data
        Data data = BenchmarkSetup.getData(dataset);
        Data data_sh = BenchmarkSetup.getData(dataset_sh);
        
        // Run benchmarks
        BENCHMARK.addValue(PAYOUT_FULL_DOMAIN, getCostBenefitPayout(data, configuration));
        // Run benchmarks
        BENCHMARK.addValue(PAYOUT_FULL_DOMAIN_NO_ATTACK, getFullDomainPayoutNoAttack(data, configuration));
        // Payout
        BENCHMARK.addValue(PAYOUT_RECORD_LEVEL, getRecordLevelPayout(data, configuration, true));
        // Payout
        BENCHMARK.addValue(PAYOUT_RECORD_LEVEL_NO_ATTACK, getRecordLevelPayoutNoAttack(data, configuration));
        // Ugly hack
        BENCHMARK.addValue(PAYOUT_SAFE_HARBOR, getSafeHarborPayout(data_sh, configuration));
    }
}