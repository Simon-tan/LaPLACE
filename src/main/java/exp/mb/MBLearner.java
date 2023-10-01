package exp.mb;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.net.ParentSet;
import weka.classifiers.bayes.net.estimate.SimpleEstimator;
import weka.classifiers.bayes.net.search.ci.ICSSearchAlgorithm;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.core.Attribute;
import weka.core.Debug.Random;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import exp.util.MBLearnerTestingStatistics;


import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.*;
import weka.classifiers.bayes.net.estimate.SimpleEstimator;
import weka.classifiers.bayes.net.search.ci.ICSSearchAlgorithm;
import weka.classifiers.lazy.IBk;
import weka.classifiers.functions.SMO;

public abstract class MBLearner implements Serializable {

	static final long serialVersionUID = MBLearner.class.getCanonicalName().hashCode();
	
	public static String MB_ALPHA = "alpha";
	public static String MB_LEARNERNAME = "learner name";
	public static String MB_MAXDEGREE = "max degree";
	
	public static enum AttributeRole{
		Target,
		Parent,
		Child,
		Spouse,
		ParentChild,
		Unknown
	};
	
	public static class AttributeRoleInfo{
		public int attrb;				// Index of the attribute in m_Attributes
		public AttributeRole role;
		public AttributeRoleInfo( int a, AttributeRole r ){			
			attrb = a;
			role = r;
		 }
	};
	
	protected int m_MinNumInstancesPerCell = 5;
	
	protected String m_LearnerName;
	  
	// Reference to training instances
	protected Instances m_Instances;
	 
	/**
	 * The discretization filter.
	 */
	protected weka.filters.supervised.attribute.Discretize m_Disc = null;

	/**
	 * All attributes except for the class
	 */
	protected List<Attribute> m_Attributes;

	protected Attribute m_Target;
	  
	/**
	 * Selected attributes and their role
	 */
	protected Map<Integer, AttributeRole> m_SelAttributes;

	/**
	 * Whether to use discretization than normal distribution
	 * for numeric attributes
	 */
	protected boolean m_UseDiscretization = false;
	  
	/**
	 * The separator sets between the target and each attribute
	 */
	protected Set[][] m_Sepsets;

	/** Threshold value for CI test*/
	protected double m_Alpha;

	protected MBLearnerTestingStatistics m_TestStat;
	
	protected boolean init( Instances data ) throws Exception{
		boolean result = true;

		// Check the existing of class variable and at least 2 class label
		if( null == data || data.classIndex() < 0 || data.numClasses() < 2 )
			result = false;
		else{
			// Extract features other than the class variable. Do selection
			// only when there are at least 2 features.
			m_Instances = new Instances( data );
			m_Target = m_Instances.classAttribute();
			int numAttributes = m_Instances.numAttributes();
			m_Attributes = new ArrayList<Attribute>( numAttributes );
			for( int i = 0; i < numAttributes; i++ ){
				//if( i != m_Target.index() )
				m_Attributes.add( m_Instances.attribute(i) );
			}
			
			// Do selection only when there are at least 2 independent attributes and 1 dependent class.
			if( m_Attributes.size() < 2 )
				result = false;
		}

		if( result ){					  
			
			m_Sepsets = new Set[ m_Attributes.size() ][ m_Attributes.size() ];
			// remove instances with missing class
			m_Instances.deleteWithMissingClass();

			// Discretize instances if required
			if ( m_UseDiscretization ){
				m_Disc = new weka.filters.supervised.attribute.Discretize();
				try{
					m_Disc.setInputFormat(m_Instances);
					m_Instances = weka.filters.Filter.useFilter(data, m_Disc);
				}catch(Exception e){
					System.out.println("Exception while discretizing");
					result = false;
				}
			} 
			else{
				m_Disc = null;
			}		   
			
			m_SelAttributes = new HashMap<Integer, AttributeRole>( m_Attributes.size() );
			m_TestStat.clear();
		}

		return result;
	}
	
	public int getNumPasses(){
		return m_TestStat.getNumDataPasses();
	}

	public int getNumCITests(){
		return m_TestStat.getNumCITests();
	}

	public Map<Integer, AttributeRole> getAttributeInfo(){
		return m_SelAttributes;
	}
	
	public MBLearnerTestingStatistics getTestStat(){
		return m_TestStat;
	}
	
	public String getName(){ return m_LearnerName; }
	
	abstract public int[] search( Instances data ) throws Exception;
	
	/**
	 * Check if there are enough observations per cell 
	 * 
	 * @param target
	 * @param attr
	 * @param condSet
	 * @return
	 */
	protected boolean isDataEnough( int target, int attr, int[] condSet ){
		
		int numFreedom = 1;
		
		numFreedom *= m_Instances.numDistinctValues( target );
		numFreedom *= m_Instances.numDistinctValues( attr );
		if( condSet != null && condSet.length > 0 ){
			for( int i = 0; i < condSet.length; i++ )
				numFreedom *= m_Instances.numDistinctValues( condSet[i] );
		}
		
		if( m_Instances.numInstances() / numFreedom < m_MinNumInstancesPerCell )
			return false;
		else
			return true;
	}
	
	/**
	 * Check if 
	 * 
	 * @param target
	 * @param attr
	 * @param condSet
	 * @return
	 */
	
	
	protected boolean isDataEnough( int target, int attr, Set<Integer> condSet ){
		
		int numFreedom = 1;
		
		numFreedom *= m_Instances.numDistinctValues( target );
		numFreedom *= m_Instances.numDistinctValues( attr );
		if( condSet != null && condSet.size() > 0 ){
			for( Iterator<Integer> iterCondVar = condSet.iterator(); iterCondVar.hasNext(); ){
				int iAttr = iterCondVar.next();
				if( iAttr != target && iAttr != attr )	
					numFreedom *= m_Instances.numDistinctValues( iAttr );
			}
		}		
		if( m_Instances.numInstances() / numFreedom < m_MinNumInstancesPerCell )
			return false;
		else
			return true;
	}
	
	/**
	 * Check if 
	 * 
	 * @param target
	 * @param attr
	 * @param condSet
	 * @return
	 */
	protected boolean isDataEnough( Set<Integer> condSet ){
		
		int numFreedom = 1;
		
		
		if( condSet != null && condSet.size() > 0 ){
			for( Iterator<Integer> iterCondVar = condSet.iterator(); iterCondVar.hasNext(); )
				numFreedom *= m_Instances.numDistinctValues( iterCondVar.next() );
		}
		
		
		if( m_Instances.numInstances() / numFreedom < m_MinNumInstancesPerCell )
			return false;
		else
			return true;
	}
	
	public String toString(){
		
		StringBuffer buffer = new StringBuffer();
		
		buffer.append( "\t====Statistics of " + m_LearnerName + "====\n" );
		buffer.append( "Target = " + m_Target.index() + "(" + m_Target.name() + ")\n" );
		buffer.append( "|U| = " + (m_Attributes.size() - 1) + "\n" );
		buffer.append( "# of instances = " + m_Instances.numInstances() + "\n" );
		buffer.append( "Alpha = " + m_Alpha + "\n\n");
		
		buffer.append("Selected Features (id, role): ");
		for( Iterator<Map.Entry<Integer, AttributeRole>> iter = m_SelAttributes.entrySet().iterator(); iter.hasNext(); ){
			Map.Entry<Integer, AttributeRole> entry = iter.next();
			buffer.append( entry.getKey() + "-" + m_Attributes.get(entry.getKey()).name());
			switch( entry.getValue() ){
				case Child:
					buffer.append( "(C)\t" );
					break;
				case Parent:
					buffer.append( "(P)\t" );
					break;
				case ParentChild:
					buffer.append( "(P/C)\t" );
					break;
				case Spouse:
					buffer.append( "(S)\t" );
					break;
				case Unknown:
					buffer.append( "(U)\t" );
					break;
			}
		}
		buffer.append("\n\n");
		buffer.append( "# of data passes = " + getNumPasses() + "\n" );
		buffer.append( "# of CI tests = " + getNumCITests() + "\n" );
		
		return buffer.toString();
	}
	
	
public String toShortString(){
		
		StringBuffer buffer = new StringBuffer();
		buffer.append( "Target = " + m_Target.index() + "(" + m_Target.name() + ")\t" );
		buffer.append("Selected Features (id, role): ");
		for( Iterator<Map.Entry<Integer, AttributeRole>> iter = m_SelAttributes.entrySet().iterator(); iter.hasNext(); ){
			Map.Entry<Integer, AttributeRole> entry = iter.next();
			buffer.append( entry.getKey() + "-" + m_Attributes.get(entry.getKey()).name());
			switch( entry.getValue() ){
				case Child:
					buffer.append( "(C)\t" );
					break;
				case Parent:
					buffer.append( "(P)\t" );
					break;
				case ParentChild:
					buffer.append( "(P/C)\t" );
					break;
				case Spouse:
					buffer.append( "(S)\t" );
					break;
				case Unknown:
					buffer.append( "(U)\t" );
					break;
			}
		}
		
		
		return buffer.toString();
	}
	
	

	
	public List<Integer> tolist(){
		
		List<Integer> result = new ArrayList<>();
		
		if( m_SelAttributes.size() > 0 ){				
			int i = 0;
			for( Iterator<Integer> iter = m_SelAttributes.keySet().iterator(); iter.hasNext();  ){
				result.add(iter.next());
			}
		}			
		
		return result;
	}
	
	
	public BayesNet setBayes(BayesNet bayes, Instances data, List<List<String>> MB, int target){
		
		for(int j = 0; j < MB.get(0).size(); j++)
    	{
			if (!bayes.getParentSet(target).contains(j))
			{
				bayes.getParentSet(target).SetParent((int) data.attribute(MB.get(0).get(j)).index(), j);
			}
    	}   
		return bayes;
	}
	
	public static double[] get_data_statics(Instances data) {
		double[] attributeWeights = new double[data.numAttributes()];
		for (int i = 0; i < data.numAttributes(); i++) attributeWeights[i] = data.attribute(i).weight();
		return attributeWeights;
        
	}
	
	public static int[] get_num_values(Instances data) {
		int[] num_values = new int[data.numAttributes()];
		for (int i = 0; i < data.numAttributes(); i++) num_values[i] = data.attribute(i).numValues();
		return num_values;
        
	}
	
	
public static Instances perturbCategoricalData(Instances syntheticData, double[] attributeWeights, int[] num_values, Instance originalInstance, int num_perturb) {
		
        Random random = new Random();        
        originalInstance.setMissing(syntheticData.numAttributes()-1);
        syntheticData.add(originalInstance);
        for (int i = 1; i < num_perturb; i++) {
            
            DenseInstance perturbedInstance = new DenseInstance(originalInstance);

            for (int j = 0; j < syntheticData.numAttributes()-1; j++) {
                Attribute attribute = syntheticData.attribute(j);
                if (attribute.isNominal()) {
                    double[] distribution = new double[num_values[j]];

                    // Normalize the attribute weights to create a probability distribution
                    double totalWeight = Utils.sum(attributeWeights);
                    for (int k = 0; k < num_values[j]; k++) {
                        distribution[k] = attributeWeights[j] / totalWeight;
                    }
                    int perturbedValueIndex = Utils.indexOfMax(distribution, random);
                    perturbedInstance.setValue(j, perturbedValueIndex);
                }
            }
            perturbedInstance.setMissing(syntheticData.numAttributes()-1);

            syntheticData.add(perturbedInstance);
            
        }
        syntheticData.setClassIndex(syntheticData.numAttributes()-1);

        return syntheticData;
    }
	
	
	
	public static Instances perturbCategoricalData1(Instances data, Instance originalInstance, int num_perturb) {
		
		double[] attributeWeights = new double[data.numAttributes()];
        for (int i = 0; i < data.numAttributes(); i++) attributeWeights[i] = data.attribute(i).weight();
        
        Instances syntheticData = new Instances(data, num_perturb);
        
        Random random = new Random();
        
        originalInstance.setMissing(syntheticData.numAttributes()-1);
        syntheticData.add(originalInstance);
        for (int i = 1; i < num_perturb; i++) {
            
            DenseInstance perturbedInstance = new DenseInstance(originalInstance);

            for (int j = 0; j < data.numAttributes()-1; j++) {
                Attribute attribute = data.attribute(j);
                if (attribute.isNominal()) {
                    double[] distribution = new double[attribute.numValues()];

                    // Normalize the attribute weights to create a probability distribution
                    double totalWeight = Utils.sum(attributeWeights);
                    for (int k = 0; k < attribute.numValues(); k++) {
                        distribution[k] = attributeWeights[j] / totalWeight;
                    }
                    int perturbedValueIndex = Utils.indexOfMax(distribution, random);
                    perturbedInstance.setValue(j, perturbedValueIndex);
                }
            }
            perturbedInstance.setMissing(syntheticData.numAttributes()-1);

            syntheticData.add(perturbedInstance);
            
        }
        syntheticData.setClassIndex(syntheticData.numAttributes()-1);

        return syntheticData;
    }
	
	
	
	
	
	

	public List<List<String>> getMB(){
	
	List<List<String>> resultList = new ArrayList<>();
	List<String> Pa = new ArrayList<>();
	List<String> Ch = new ArrayList<>();
	List<String> Sp = new ArrayList<>();
	
	if( m_SelAttributes.size() > 0 ){				
		int i = 0;
		for( Iterator<Map.Entry<Integer, AttributeRole>> iter = m_SelAttributes.entrySet().iterator(); iter.hasNext(); ){
			Map.Entry<Integer, AttributeRole> entry = iter.next();
			
			switch( entry.getValue() ){
			case Child:
				Ch.add(m_Attributes.get(entry.getKey()).name());
				break;
			case Parent:
				Pa.add(m_Attributes.get(entry.getKey()).name());
				break;
			case ParentChild:
				Pa.add(m_Attributes.get(entry.getKey()).name());
				break;
			case Spouse:
				Sp.add(m_Attributes.get(entry.getKey()).name());
				break;
			
		}
			
		}
	}
	
	resultList.add(Pa);
    resultList.add(Ch);
    resultList.add(Sp);
	
	return resultList;
}
	
	
public String evaluate(Instances newData) throws Exception{
		
		StringBuffer buffer = new StringBuffer();
		
		buffer.append( "\t====Statistics of Dataset====\n" );
		buffer.append( "Target = " + newData.classIndex() + "(" + newData.attribute(newData.classIndex()).name() + ")\n" );
		buffer.append( "|U| = " + (newData.numAttributes()-1) + "\n" );
		buffer.append( "# of instances = " + newData.numInstances() + "\n" );
		buffer.append("\n\n");
		
		
		
		
		buffer.append("******************* Bayesian network");
		
		// Create the Bayesian network classifier
        BayesNet bayes = new BayesNet();             
        ICSSearchAlgorithm CBL = new ICSSearchAlgorithm ();
        SimpleEstimator simpleEstimator = new SimpleEstimator();
        bayes.setSearchAlgorithm(CBL);
        bayes.setEstimator(simpleEstimator);       
        bayes.buildClassifier(newData);
        
        Evaluation eval = new Evaluation(newData);
		eval.evaluateModel( bayes, newData );
		//buffer.append(bayes.toString()+ "\n" );
		//buffer.append(eval.toSummaryString()+ "\n" );
		buffer.append(eval.toClassDetailsString()+ "\n" );  
        //buffer.append(eval.toMatrixString()+ "\n" );
		buffer.append("\n\n");
        
		buffer.append("******************* NaiveBayes"+ "\n" );
        
        // Create the NaiveBayes classifier
        Classifier nbc = new NaiveBayes();
        nbc.buildClassifier(newData);
        
        Evaluation eval1 = new Evaluation(newData);
		eval1.evaluateModel( nbc, newData );			
		buffer.append(eval1.toSummaryString()+ "\n" );
		buffer.append(eval1.toClassDetailsString()+ "\n" );  
        //buffer.append(eval1.toMatrixString()+ "\n" );
		buffer.append("\n\n");
		
		
		buffer.append("******************* KNN"+ "\n" );
        int k = newData.numAttributes()-2;
        Classifier knn = new IBk(k);
        knn.buildClassifier(newData);
        
        Evaluation eval3 = new Evaluation(newData);
		eval3.evaluateModel( knn, newData );			
        //buffer.append(eval3.toSummaryString()+ "\n" );
		buffer.append(eval3.toClassDetailsString()+ "\n" );  
        //buffer.append(eval3.toMatrixString()+ "\n" );
		buffer.append("\n\n");
        
		buffer.append("******************* SVM");
        SMO svm = new SMO();
        svm.setOptions(weka.core.Utils.splitOptions("-K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\""));
        svm.buildClassifier(newData);
        
        Evaluation eval4 = new Evaluation(newData);
		eval4.evaluateModel( svm, newData );			
		buffer.append(eval4.toSummaryString()+ "\n" );
		buffer.append(eval4.toClassDetailsString()+ "\n" );  
        //buffer.append(eval4.toMatrixString()+ "\n" );
		buffer.append("\n\n");
		
		
		
		return buffer.toString();
	}


public String explain(Instances Data, BayesNet bayes,  Integer target) throws Exception{
	
	
	
	List<String[]> buffer = new ArrayList<String[]>();    
    String[] header=new String[(bayes.getNrOfParents(target)*2)+5];
    int num_Pa= bayes.getNrOfParents(target);
    int num_Class= bayes.getCardinality(target);
    
    for(int j = 0; j < num_Pa; j++)
	{
    	header[j]=bayes.getNodeName(bayes.getParent(target, j));
	}
    
    for(int i = 0; i < bayes.getCardinality(target); i++)
    {
    	for(int j = 0; j < num_Pa; j++)
    	{
        	header[(num_Pa*i)+j]=bayes.getNodeName(bayes.getParent(target, j));
    	}
    	
    }
    
    for(int i = 0; i < bayes.getCardinality(target); i++)
    {
    	header[(num_Pa*num_Class)+i]=bayes.getNodeValue(target, i);
    }     
    
    header[((num_Pa*num_Class)+bayes.getCardinality(target))+0]="Class";
    
    System.out.println(Arrays.toString(header));
    buffer.add(header);
    
    
    /*
    
    //String exp_file = "/home/simon/Documents/Interpret/BN_XML/perturbed/alarm/alarm_p1_exp.csv";
    
    
    int PaC=0;
    for (int i = 0; i < Data.numInstances(); i++) {
    	String[] row=new String[(bayes.getNrOfParents(target)*2)+5];
    	Instance exp_instance1 = (Instance) Data.instance(i).copy();
        int class_value1= (int)exp_instance1.classValue();
        //int PaC=0;
        
        
        for(int j = 0; j < num_Pa; j++)
    	{
        	row[j]=exp_instance1.stringValue(bayes.getParent(target, j));
    	}
        
        for(int j = 0; j < num_Pa; j++)
    	{
    		int Pa =bayes.getParent(target, j);            		
    		for(int k = 0; k < bayes.getCardinality(Pa); k++)
    		{            			
    			if(bayes.getNodeValue(Pa, k).equals(exp_instance1.stringValue(Pa)))
    			{
    				PaC= j * k ;
    			}
    		}
    		double p_prob= bayes.getProbability(target, PaC, class_value1);        		
    		//System.out.println(bayes.getNodeValue(target, class_value1 )+" Parent "+ bayes.getNodeName(Pa) + " value "+ exp_instance1.stringValue(Pa) + " = " + p_prob  + " ");
    		row[num_Pa+j]=String.valueOf(p_prob);
    		//data.add(new String[] { String.valueOf(i) , bayes.getNodeValue(target, class_value ) , bayes.getNodeName(Pa),exp_instance1.stringValue(Pa), String.valueOf(p_prob) });
    	}
        
        double[] predictions = bayes.distributionForInstance(exp_instance1);
        for(int j = 0; j < bayes.getCardinality(target); j++)
        {
        	row[(num_Pa*2)+j]= String.valueOf(Array.get(predictions,j));
        }
        row[((num_Pa*2)+bayes.getCardinality(target))+0]=String.valueOf((int)exp_instance1.classValue());;
        
        data.add(row);
	}
	
	
	
	
	
	
	*/
	return buffer.toString();
}
	
	
	

}
