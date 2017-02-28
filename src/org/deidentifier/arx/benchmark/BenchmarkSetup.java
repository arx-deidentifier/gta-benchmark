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
import java.nio.charset.Charset;

import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;

/**
 * Benchmark of ARX's implementation of the game theoretic approach proposed in: <br>
 * A Game Theoretic Framework for Analyzing Re-Identification Risk. <br>
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin <br>
 * PLOS|ONE. 2015.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkSetup {
    
    public static enum BenchmarkDataset {
        
        ADULT {
            @Override
            public String toString() {
                return "adult";
            }
        },
        ADULT_NC {
            @Override
            public String toString() {
                return "adult-nc";
            }
        },
        ADULT_TN {
            @Override
            public String toString() {
                return "adult-tn";
            }
        },
        ADULT_TN_288 {
            @Override
            public String toString() {
                return "adult-tn-288";
            }
        },
        ADULT_TN_SAFE_HARBOR {
            @Override
            public String toString() {
                return "adult-tn-safe-harbor";
            }
        },
        CUP {
            @Override
            public String toString() {
                return "cup";
            }
        },
        FARS {
            @Override
            public String toString() {
                return "fars";
            }
        },
        ATUS {
            @Override
            public String toString() {
                return "atus";
            }
        },
        IHIS {
            @Override
            public String toString() {
                return "ihis";
            }
        },
    }
    
    /**
     * Returns the dataset for the given name
     * @param name
     * @return
     */
    public static BenchmarkDataset getBenchmarkDataset(String name) {
        for (BenchmarkDataset dataset : BenchmarkDataset.values()) {
            if (dataset.toString().equals(name)) {
                return dataset;
            }
        }
        throw new IllegalArgumentException("Unknown dataset");
    }
    
    /**
     * Configures and returns the dataset
     * @param dataset
     * @param criteria
     * @return
     * @throws IOException
     */
    
    public static Data getData(BenchmarkDataset dataset) throws IOException {
        Data data = null;
        switch (dataset) {
        case ADULT:
            data = Data.create("data/adult.csv", Charset.defaultCharset(), ';');
            break;
        case ADULT_NC:
            data = Data.create("data/adult_nc.csv", Charset.defaultCharset(), ';');
            break;
        case ADULT_TN:
            data = Data.create("data/adult_tn.csv", Charset.defaultCharset(), ';');
            break;
        case ADULT_TN_288:
            data = Data.create("data/adult_tn_288.csv", Charset.defaultCharset(), ',');
            break;
        case ADULT_TN_SAFE_HARBOR:
            data = Data.create("data/adult_tn.csv", Charset.defaultCharset(), ';');
            break;
        case ATUS:
            data = Data.create("data/atus.csv", Charset.defaultCharset(), ';');
            break;
        case CUP:
            data = Data.create("data/cup.csv", Charset.defaultCharset(), ';');
            break;
        case FARS:
            data = Data.create("data/fars.csv", Charset.defaultCharset(), ';');
            break;
        case IHIS:
            data = Data.create("data/ihis.csv", Charset.defaultCharset(), ';');
            break;
        default:
            throw new RuntimeException("Invalid dataset");
        }
        
        for (String qi : getQuasiIdentifyingAttributes(dataset)) {
            data.getDefinition().setAttributeType(qi, getHierarchy(dataset, qi));
        }
        
        if (dataset == BenchmarkDataset.ADULT_TN_SAFE_HARBOR) {
            for (String qi : getQuasiIdentifyingAttributes(dataset)) {
                int max = data.getDefinition().getMaximumGeneralization(qi);
                data.getDefinition().setMaximumGeneralization(qi, max);
                data.getDefinition().setMinimumGeneralization(qi, max);
            }
        }
        
        return data;
    }

    /**
     * Default parameter
     * @return
     */
    public static int getDefaultPublisherLoss() {
        return 300;
    }

    /**
     * Default parameter
     * @return
     */
    public static int getDefaultAdversaryGain() {
        return 300;
    }

    /**
     * Default parameter
     * @return
     */
    public static int getDefaultPublisherBenefit() {
        return 1200;
    }

    /**
     * Default parameter
     * @return
     */
    public static int getDefaultAdversaryCost() {
        return 4;
    }
    
    public static int getNumberOfRepetitions(BenchmarkDataset dataset) {
        if (dataset == BenchmarkDataset.ADULT_NC ||
            dataset == BenchmarkDataset.ADULT_TN) {
            return 100;
        } else {
            return 10;
        }
    }

    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersPublisherLoss() {
        return new double[]{0, 250, 500, 750, 1000, 1250, 1500, 1750, 2000};
    }

    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersAdversaryGain() {
        return new double[]{1d, 1.01d, 1.1d, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 750, 1000, 1250, 1500, 1750, 2000};
    }

    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersAdversaryCost() {
        return new double[]{1d, 1.01d, 1.1d, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 750, 1000, 1250, 1500, 1750, 2000};
    }
    
    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersPublisherBenefit() {
        return new double[]{250, 500, 750, 1000, 1250, 1500, 1750, 2000};
    }

    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersGainLoss() {
        return new double[]{0, 250, 500, 750, 1000, 1250, 1500, 1750, 2000};
    }

    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersGainLoss2() {
        return new double[]{0, 200, 400, 600, 800, 1000, 1200, 1400, 1600, 1800, 2000};
    }
    /**
     * Returns the generalization hierarchy for the dataset and attribute
     * @param dataset
     * @param attribute
     * @return
     * @throws IOException
     */
    public static Hierarchy getHierarchy(BenchmarkDataset dataset, String attribute) throws IOException {
        switch (dataset) {
        case ADULT:
            return Hierarchy.create("hierarchies/adult_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case ADULT_NC:
            return Hierarchy.create("hierarchies/adult_nc_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case ADULT_TN:
            return Hierarchy.create("hierarchies/adult_tn_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case ADULT_TN_288:
            return Hierarchy.create("hierarchies/adult_tn_288_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case ADULT_TN_SAFE_HARBOR:
            return Hierarchy.create("hierarchies/adult_tn_safe_harbor_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case ATUS:
            return Hierarchy.create("hierarchies/atus_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case CUP:
            return Hierarchy.create("hierarchies/cup_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case FARS:
            return Hierarchy.create("hierarchies/fars_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case IHIS:
            return Hierarchy.create("hierarchies/ihis_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }
    
    /**
     * Returns the number of records in the given dataset
     * @param dataset
     * @return
     * @throws IOException 
     */
    public static int getNumRecords(BenchmarkDataset dataset) throws IOException {
        return getData(dataset).getHandle().getNumRows();
    }

    /**
     * Returns the quasi-identifiers for the dataset
     * @param dataset
     * @return
     */
    public static String[] getQuasiIdentifyingAttributes(BenchmarkDataset dataset) {
        switch (dataset) {
        case ADULT:
            return new String[] {   "age",
                                    "education",
                                    "marital-status",
                                    "native-country",
                                    "race",
                                    "salary-class",
                                    "sex",
                                    "workclass",
                                    "occupation" };
        case ADULT_TN_288:
        case ADULT_TN:
            return new String[] {   "sex", "age", "zip", "race"};
        case ADULT_TN_SAFE_HARBOR:
            return new String[] {   "sex", "age", "zip", "race"};
        case ADULT_NC:
            return new String[] {   "sex", "age", "zip", "race"};
        case ATUS:
            return new String[] {   "Age",
                                    "Birthplace",
                                    "Citizenship status",
                                    "Labor force status",
                                    "Marital status",
                                    "Race",
                                    "Region",
                                    "Sex",
                                    "Highest level of school completed" };
        case CUP:
            return new String[] {   "AGE",
                                    "GENDER",
                                    "INCOME",
                                    "MINRAMNT",
                                    "NGIFTALL",
                                    "STATE",
                                    "ZIP",
                                    "RAMNTALL" };
        case FARS:
            return new String[] {   "iage",
                                    "ideathday",
                                    "ideathmon",
                                    "ihispanic",
                                    "iinjury",
                                    "irace",
                                    "isex",
                                    "istatenum" };
        case IHIS:
            return new String[] {   "AGE",
                                    "MARSTAT",
                                    "PERNUM",
                                    "QUARTER",
                                    "RACEA",
                                    "REGION",
                                    "SEX",
                                    "YEAR",
                                    "EDUC" };
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }
}
