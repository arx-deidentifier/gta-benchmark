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

import org.deidentifier.arx.Data;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.metric.Metric;

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
public abstract class BenchmarkExperiment_V2_2_Other extends BenchmarkExperiment {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK            = new Benchmark(new String[] { "quality model"});
    /** MEASUREMENT PARAMETER */
    private static final int       QUALITY_09_AVG_RISK  = BENCHMARK.addMeasure("Quality (9% avg. risk)");
    /** MEASUREMENT PARAMETER */
    private static final int       QUALITY_20_AVG_RISK  = BENCHMARK.addMeasure("Quality (20% avg. risk)");
    /** MEASUREMENT PARAMETER */
    private static final int       QUALITY_09_IND_RISK  = BENCHMARK.addMeasure("Quality (9% ind. risk)");
    /** MEASUREMENT PARAMETER */
    private static final int       QUALITY_20_IND_RISK  = BENCHMARK.addMeasure("Quality (20% ind. risk)");
    
    /**
     * Main
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        BenchmarkDataset dataset = BenchmarkSetup.getBenchmarkDataset("adult-tn");

        // Init
        BENCHMARK.addAnalyzer(QUALITY_09_AVG_RISK, new ValueBuffer());
        BENCHMARK.addAnalyzer(QUALITY_20_AVG_RISK, new ValueBuffer());
        BENCHMARK.addAnalyzer(QUALITY_09_IND_RISK, new ValueBuffer());
        BENCHMARK.addAnalyzer(QUALITY_20_IND_RISK, new ValueBuffer());

            for (Metric<?> metric : new Metric[] { Metric.createPrecomputedLossMetric(1d),
                    Metric.createPrecomputedNormalizedEntropyMetric(1d),
                    Metric.createKLDivergenceMetric() }) {
                BENCHMARK.addRun(metric.getName());
                analyze(dataset, metric);
                BENCHMARK.getResults().write(new File("results/"+dataset.toString()+"-experiment-v2-2-2.csv"));
            }
    }

    /**
     * Run the benchmark
     * @param dataset
     * @param metric
     * @throws IOException
     */
    private static void analyze(BenchmarkDataset dataset, Metric<?> metric) throws IOException {
     
        // Load data
        Data data = BenchmarkSetup.getData(dataset);
        BENCHMARK.addValue(QUALITY_09_AVG_RISK, getAverageRiskQuality(data, null, 11, metric));
        data = BenchmarkSetup.getData(dataset);
        BENCHMARK.addValue(QUALITY_20_AVG_RISK, getAverageRiskQuality(data, null, 5, metric));
        data = BenchmarkSetup.getData(dataset);
        BENCHMARK.addValue(QUALITY_09_IND_RISK, getIndividualRiskQuality(data, null, 11, metric));
        data = BenchmarkSetup.getData(dataset);
        BENCHMARK.addValue(QUALITY_20_IND_RISK, getIndividualRiskQuality(data, null, 5, metric));
    }
}
