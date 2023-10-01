package exp.mb;

import java.io.BufferedReader;

import java.io.FileReader;
import java.util.ArrayList;



import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.Debug.Random;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.Remove;
import weka.core.converters.CSVLoader;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/** 
* @author Sein Minn 
* @version $Revision: 1.00 $
*/
public class Evaluate {
	
	static final long serialVersionUID = Evaluate.class.getCanonicalName().hashCode();
	


	public static void writeListToCSV(List<String[]> data, String filePath) throws IOException {
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
	        for (String[] row : data) {
	            String csvRow = Utils.arrayToString(row);
	            writer.write(csvRow);
	            writer.newLine();
	        }
	    }
	}
	
	public static double get_WeightedF1(Evaluation eval, int numclass){
		
		double value = eval.weightedFMeasure();
		int v_count=0;
        // Check if the value is NaN
        if (Double.isNaN(value)) {
        	value=0.0;        	
        	for (int i = 0; i < numclass; i++) {
        		double s_v=eval.fMeasure(i);
        		if (Double.isNaN(s_v)){
        			value+=s_v;
        			v_count+=1;
        		}
        	}
            value=value/v_count;
        } 
		return value;
		
		
	}
		
		
		  	  
		
	
	public static void main(String [] args) 
	{
		
		try
		{
			
			String method="LaPLACE"; 
			String dataset="compas";
            
			String dataFile = "/home/sm/Documents/datasets/arff/"+dataset+".arff"; // directory for data file
			String exp_file = "/home/sm/Documents/Interpret/Eval/"+method+"_"+dataset+"_selected.csv"; // directory for selected features file from output of LaPLACE_Explainer
			
			CSVLoader csvLoader = new CSVLoader(); 
            
            csvLoader.setSource(new java.io.File(exp_file));
            
            Instances SF_data = csvLoader.getDataSet();            
			BufferedReader reader = new BufferedReader(new FileReader(dataFile) );	
			
			Instances data = new Instances( reader );
			reader.close();
			int target = data.numAttributes()-1;
			data.setClassIndex(target);
			
			
			int splitIndex = (int) (data.size() * 0.8); // 80% of the data size
            // Step 3: Create training and testing datasets
            Instances trainingData = new Instances(data, 0, splitIndex);
            Instances testingData = new Instances(data, splitIndex, data.size() - splitIndex);
            data.delete();
            
            
            Discretize filter = new Discretize();
			filter.setBins(20);	 // Set the number of bins 
			filter.setInputFormat(trainingData);
			trainingData = Filter.useFilter(trainingData, filter);
			testingData = Filter.useFilter(testingData, filter);
            
            System.out.print(method+":"+dataset+",RF_S,BN_S, SVM_S");
            System.out.println();
            
            RandomForest randomForest;
            BayesNet bayes;
            SMO svm;                  
            Evaluation eval;
            
            
            int num_exp=SF_data.numInstances();
            
    		for (int j = 0; j < num_exp; j++) {
    			
        		Instance SF = SF_data.instance(j);
        		List<Integer> result = new ArrayList<>();
        		for (int i = 0; i < SF_data.numAttributes(); i++)if (SF.value(i)>-1)  result.add((int)SF.value(i));        		
        		result.add(trainingData.classIndex());
    			System.out.print(result);
    			
    			int[] result_a = result.stream().mapToInt(i->i).toArray();
    			Remove removeFilter = new Remove();
    			removeFilter.setAttributeIndicesArray(result_a);
    			removeFilter.setInvertSelection(true);
    			removeFilter.setInputFormat(trainingData);
    			Instances newtrain = Filter.useFilter(trainingData, removeFilter);
    			Instances newtest = Filter.useFilter(testingData, removeFilter);
    			newtrain.setClassIndex(newtrain.numAttributes()-1);
    			newtest.setClassIndex(newtest.numAttributes()-1);
    			
    			System.out.print(newtest.numInstances()+":");
    			
    			
    			randomForest = new RandomForest();
        		randomForest.buildClassifier(newtrain);        		
        		eval = new Evaluation(newtrain);
        		eval.evaluateModel(randomForest, newtest);                
                System.out.print(eval.weightedFMeasure()+ ",");
    			
    			
    			bayes = new BayesNet();
    			bayes.buildClassifier(newtrain);
    			eval = new Evaluation(newtrain);
        		eval.evaluateModel( bayes, newtest );        		
                System.out.print(eval.weightedFMeasure()+ ",");
        		
        		     		    
     		    
     		    svm = new SMO();
                svm.setOptions(weka.core.Utils.splitOptions("-K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\""));
                svm.buildClassifier(newtrain);               
   			    eval = new Evaluation(newtrain);
       		    eval.evaluateModel( svm, newtest );        		    
                System.out.print(eval.weightedFMeasure());
                System.out.println();
        		
    			
    		}
    		
    		
    		
    		
    		
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
