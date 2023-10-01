package exp.util;

import java.util.Arrays;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.supervised.instance.Resample;

public class PerturbDataWithSampling {

    public static void main(String[] args) {
        try {
        	
        	int numInstances=5000;
            // Step 1: Load the training data
            DataSource source = new DataSource("/home/simon/Documents/Interpret/BN_XML/Generate/alarm_" + numInstances/1000 + "k.arff");
            Instances data = source.getDataSet();

            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            // Step 2: Calculate the class distribution of the training data
            int numClasses = data.numClasses();
            double[] classDistribution = new double[numClasses];

            for (Instance instance : data) {
                int classValue = (int) instance.classValue();
                classDistribution[classValue]++;
            }
            
            System.out.println(Arrays.toString(classDistribution));

            // Step 3: Sample new data instances according to the class distribution
            Resample resampleFilter = new Resample();
            resampleFilter.setBiasToUniformClass(1.0); // Set to 1.0 to sample according to the class distribution
            resampleFilter.setInputFormat(data);
            resampleFilter.setSampleSizePercent(10.0);            
            Instances perturbedData = resampleFilter.useFilter(data, resampleFilter);
            

            // Step 4: Save or use the perturbed data as needed
            System.out.println("Perturbed data: ");
            System.out.println(perturbedData.numInstances());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
