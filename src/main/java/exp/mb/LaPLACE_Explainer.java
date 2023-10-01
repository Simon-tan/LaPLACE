package exp.mb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import exp.util.ContTableProcessor;
import exp.util.MBLearnerTestingStatistics;
import exp.util.SubSet;
import exp.util.TableMap;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;



/** 
* @author Sein Minn 
* @version $Revision: 1.00 $
*/
public class LaPLACE_Explainer extends MBLearner{
	
	static final long serialVersionUID = LaPLACE_Explainer.class.getCanonicalName().hashCode();
	
	protected int m_MaxDegree; 
	
	private Vector<Double> testStats;
	
	/** 
	 *Constructor 
	 **/
	public LaPLACE_Explainer( Map<String, String> options ){
		
		testStats = new Vector<Double>(3);
		for( int i = 0; i < 3; i++ ) testStats.add( new Double(0) );
		
		if( options.containsKey( MBLearner.MB_ALPHA ) ){
			m_Alpha = Double.valueOf( options.get( MBLearner.MB_ALPHA ) );
		}else{
			m_Alpha = 0.05;
		}
		
		if( options.containsKey( MBLearner.MB_LEARNERNAME ) ){ 
			m_LearnerName = options.get( MBLearner.MB_LEARNERNAME );
		}else{
			m_LearnerName = "MB Learner";
		}
		
		if( options.containsKey( MBLearner.MB_MAXDEGREE ) ){ 
			m_MaxDegree = Integer.valueOf( options.get( MBLearner.MB_MAXDEGREE ) );
		}else{
			m_MaxDegree = -1;		// Minus number means it doesn't work
		}
			
		m_TestStat = new MBLearnerTestingStatistics( m_LearnerName );
	}
	  
	public int[] search( Instances data ) throws Exception{
		
		int[] result = null;
		// Exit if fail in initialization
		if( !init( data ) )
			return result;
				
		// Phase 1: Find a set containing parents/children as well as some descendants 
		ContTableProcessor sourceProc = new ContTableProcessor();
		List<Integer> adjSet = initCanADJSet( m_Target, m_Attributes );
		List<Integer> canTargetPC = recognizePC( m_Target.index(), adjSet, data, sourceProc );	  

		// Phase 2: Remove false parents/children, and find the spouse candidates.
		Set<Integer> targetPC = new HashSet<Integer>( (int)(canTargetPC.size() * 1.25) );		  
		Map<Integer, Set<Integer>> canTargetSPs = new HashMap<Integer, Set<Integer>>( (int)(canTargetPC.size() * 1.25) );
		for( int i = 0; i < canTargetPC.size(); i++ ){
			int x = canTargetPC.get(i);
			adjSet = initCanADJSet( m_Attributes.get( x ), m_Attributes );
			Set<Integer> canPCx = new HashSet<Integer>( recognizePC( x, adjSet, data, sourceProc ) );
			if( canPCx.contains( m_Target.index() ) ){		
				targetPC.add( x );										// A true parent/child is found
				m_SelAttributes.put( x, AttributeRole.ParentChild  );		// Mark its role
				canTargetSPs.put( x, canPCx );							// Save for later reference
				continue;
			}			  
		}
		
	

		// Phase 3: Determine the true spouses
		Set<Integer> canHashTargetPC = new HashSet<Integer>( canTargetPC );
		canHashTargetPC.add( m_Target.index() );
		Set<Integer> targetSP = new HashSet<Integer>( (int)( m_Attributes.size() - targetPC.size()) );
		for ( Iterator<Integer> pcIter = targetPC.iterator(); pcIter.hasNext(); ){
			int pcId = pcIter.next();
			Set<Integer> canSP = canTargetSPs.get( pcId );
			TableMap tableMap = null;
			tableMap = buildContTables( m_Target.index(), canSP, pcId, canHashTargetPC, data, sourceProc );
			if( null == tableMap )		// No interesting thing to do for the given canSP
				continue;
			
			for( Iterator<Integer> spIter = canSP.iterator(); spIter.hasNext(); ){
				int spId = spIter.next();
				// Ignore parents, children, descendants of target and found spouses
				if( !canHashTargetPC.contains( spId ) && !targetSP.contains( spId ) ){
					// Build the TableMap for this candidate spouse set only when it is not built yet
					Set<Integer> sepset = new HashSet<Integer>( m_Sepsets[ m_Target.index()][spId] );
					sepset.add( pcId );
						m_TestStat.incrCITests( 1 );					
						m_TestStat.updateFreqCondSize(  sepset.size() - 2, 1 );
						if( tableMap.getCITest( m_Target.index(), spId, sepset, testStats ) ){
							if( testStats.get(2) <= m_Alpha ){
								targetSP.add( spId );					  
								m_SelAttributes.put( pcId, AttributeRole.Child );		// Determine a child
								m_SelAttributes.put( spId, AttributeRole.Spouse );		// Find a spouse
							}
						}
					//}
				}
			}
			tableMap = null;			//Remember to reset it for next round.
		}

		// Save space by releasing		
		if( m_SelAttributes.size() > 0 ){
			result = new int[ m_SelAttributes.size() ];
			int i = 0;
			for( Iterator<Integer> iter = m_SelAttributes.keySet().iterator(); iter.hasNext();  ){
				result[ i++ ] = iter.next();
			}
		}
		
		return result;
	}
	
	// Make the running and updating come together
	private void updateContTables( Instances data, ContTableProcessor sourceProc, TableMap tableMap ){
		if( data != null && sourceProc != null && tableMap != null ){
			sourceProc.ProcessSource(data, tableMap);
			m_TestStat.incrDataPasses( 1 );
		}
	}
	
	private TableMap buildContTables( int target, Set<Integer> attrs, int toInclude, Set<Integer> toExclude, Instances data, ContTableProcessor sourceProc ){
		
		TableMap tableMap = null;
		if( attrs != null && attrs.size() > 0 ){
			Vector<Set<Integer>> contTables = new Vector<Set<Integer>>();

			for( Iterator<Integer> iter = attrs.iterator(); iter.hasNext(); ){
				Integer attr = iter.next();
				if( !toExclude.contains( attr ) ){	
					Set<Integer> sepset = new HashSet<Integer>( m_Sepsets[ target ][attr] );
					sepset.add( toInclude );
					if( isDataEnough(sepset) )		
						contTables.add( sepset );
				}
			}
			
			if( contTables.size() > 0 ){		// Not necessary to scan the data if nothing is required
				tableMap = new TableMap( contTables );
				updateContTables( data, sourceProc, tableMap );
			}
		}

		return tableMap;		
	}
			  
	private List<Integer> recognizePC( 
			int target, 
			List<Integer> adjSet, 
			Instances data,
			ContTableProcessor sourceProc)
			{   
		if( adjSet.size() < 1 ){
			System.out.println("The adjacency set must be at least of size 1");
			return null;
		}

		boolean mbGrow = true;
		int i, j, cutSetSize = 0;
		List<Integer> toRemoveAttrs = new ArrayList<Integer>();
		Set<Set<Integer>> contTables = new HashSet<Set<Integer>>();		  
		Map<Integer, Vector<Set<Integer>>> subSetMap = new HashMap<Integer, Vector<Set<Integer>>>(); 
		boolean allInvalidTests;

		while( mbGrow ){

			allInvalidTests = true;
			// Step 1: Prepare all related contigency tables of the size cutSetSize;
			SubSet wholeSet = new SubSet( adjSet );
			
			for( i = 0; i < adjSet.size(); i++ ) {
				// find the subsets of adjSet/adjSet[i] with size of cutset size
				int checkIndex = adjSet.get(i);							
				Vector<Set<Integer>> subSets = new Vector<Set<Integer>>( );
				if( cutSetSize > 0 ){
					wholeSet.remove( checkIndex );
					wholeSet.findSubSets( cutSetSize, subSets );
					for( j = 0; j < subSets.size(); j++ ) {
						// Each contingency table is composed on X,Y and conditioning set Z
						subSets.get(j).add( target );
						subSets.get(j).add( checkIndex );
						if( isDataEnough( subSets.get(j)) )		// Add by Shunkai Fu on Feb 19, 2010
							contTables.add( subSets.get(j) );	
					}
					wholeSet.add( checkIndex );			// Add the removed one to have a complete ADJ set for next round usage
				}else{					
					Set<Integer> subSet = new HashSet<Integer>();					
					subSet.add( target );
					subSet.add( checkIndex );
					subSets.add( subSet );
					if( isDataEnough( subSet ) ){							
						contTables.add( subSet );
					}
				}
				subSetMap.put(checkIndex, subSets);	// For reference right now
			}			  
			// This version can only deal small problem. To deal with larger one, we need
			// more data passes.
			TableMap tableMap = new TableMap( new Vector<Set<Integer>>( contTables ) );
			updateContTables( data, sourceProc, tableMap );
			
			// Step 2: Iterate each adjacent node to determine if it is independent 
			// with the target or not.		
			for( i =0; i < adjSet.size(); i++ ){			
				int attr = adjSet.get(i);
				Vector<Set<Integer>> subSets = subSetMap.get( attr );
				//boolean allInvalidTestsForAttr = true;
				// Do the real CI test given (Target, Attribute, subSet)				
				for( j = 0; j < subSets.size(); j++ ){
					if( isDataEnough(subSets.get(j))){
						m_TestStat.incrCITests(1);
						m_TestStat.updateFreqCondSize(  cutSetSize , 1 );
						//allInvalidTestsForAttr = false;
						if( tableMap.getCITest(target, attr, subSets.get(j), testStats ) ) {
							allInvalidTests = false;
							if( testStats.get(2) > m_Alpha ){
								toRemoveAttrs.add( attr );
								m_Sepsets[ target ][ attr ] = subSets.get(j);
								break;
							}
						}
					}
				}
						
			}
			// Do the real updating!
			// NOTE: Even there is nothing to remove, it is suggested to going on till all CI tests are not reliable!!!!
			cutSetSize++;
			adjSet.removeAll( toRemoveAttrs );
			toRemoveAttrs.clear();
			
			if( adjSet.size() <= cutSetSize || allInvalidTests || (m_MaxDegree > 0 && cutSetSize > m_MaxDegree) )
				mbGrow = false;
			else
				contTables.clear();		//clear for a new round!!
		}
		
		return adjSet;	
		
	}

	/**
	 * Construct the adjacency list by deleting the one toRemove from attributes. 
	 * 
	 * @param toRemove	The one to remove. 
	 * @param attributes	All attributes. 	
	 * @param	target		Target attribute which is not in attributes 
	 * @return			The adjacency set by excluding 'toRemove' from attributes + target.
	 */
	private List<Integer> initCanADJSet( Attribute toRemove, List<Attribute> attributes, Attribute target ){  
		List<Integer> result = new ArrayList<Integer>();
		int toRemoveIndex = toRemove.index();

		for( int i = 0; i < attributes.size(); i++ ){
			int ind = attributes.get(i).index();
			if( ind != toRemoveIndex )
				result.add(ind);
		}

		if( toRemove != target )
			result.add( target.index() );

		return result;
	}	
	
	  
	// In this version, target is contained in attributes
	private List<Integer> initCanADJSet( Attribute toRemove, List<Attribute> attributes ){
		
		List<Integer> result = new ArrayList<Integer>();		
		for( int i = 0; i < attributes.size(); i++ ){		
			if( attributes.get(i).index() != toRemove.index() )
				result.add( attributes.get(i).index() );
		}

		return result;
	}
	
	public static void writeListToCSV(List<String[]> data, String filePath) throws IOException {
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
	        for (String[] row : data) {	        	
	            String csvRow = Utils.arrayToString(row);
	            writer.write(csvRow);
	            writer.newLine();
	        }
	    }
	}
		
		  	  
		
	
	public static void main(String [] args) 
	{
		Map<String, String> options = new HashMap<String, String>();
		options.put( MBLearner.MB_ALPHA, "0.001" );		
		options.put( MBLearner.MB_MAXDEGREE, "5" );
		MBLearner mbLearner = new LaPLACE_Explainer(options);
		try
		{
			
			String dataset="compas";
			String dataFile = "/home/sm/Documents/datasets/"+dataset+".arff";
			String exp_File = "/home/sm/Documents/Interpret/LaPLACE_"+dataset+"_selected.csv"; 
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));		
			int max_len=1;
			int num_run=101;
			Instances data = new Instances( reader );
			reader.close();
			int target = data.numAttributes()-1;
			data.setClassIndex(target);
			
			Discretize filter = new Discretize();
			filter.setBins(20);	 // Set the number of bins 
			filter.setInputFormat(data);
			data = Filter.useFilter(data, filter);
			
			int numAttributes = data.numAttributes();
            for (int i = 0; i < numAttributes; i++) {
                AttributeStats attributeStats = data.attributeStats(i);
                String attributeName = data.attribute(i).name();
                int distinctValues = attributeStats.distinctCount;
                int missingValues = attributeStats.missingCount;
                System.out.print("Attribute (" + String.valueOf(i) + ") :" + attributeName+ "\t");
                System.out.print("Distinct Values: " + distinctValues+ "\t");
                System.out.print("Missing Values: " + missingValues+ "\t");
                
                if (data.attribute(i).isNumeric()) {
                    double mean = attributeStats.numericStats.mean;
                    double stdDev = attributeStats.numericStats.stdDev;
                    System.out.print("Mean: " + Utils.doubleToString(mean, 4)+ "\t");
                    System.out.println("Standard Deviation: " + Utils.doubleToString(stdDev, 4)+ "\t");
                }
                System.out.println();
            }
			
			int splitIndex = (int) (data.size() * 0.8); // 80% of the data size
			

            // Step 3: Create training and testing datasets
            Instances trainingData = new Instances(data, 0, splitIndex);
            Instances testingData = new Instances(data, splitIndex, num_run);
			data.delete();
            List<String[]> csv_selected_data = new ArrayList<String[]>();            
    		RandomForest randomForest_org = new RandomForest();
    		randomForest_org.buildClassifier(trainingData);
    		Evaluation evaluation = new Evaluation(trainingData);
            evaluation.evaluateModel(randomForest_org, testingData);
            System.out.println(evaluation.toSummaryString());
            System.out.println(evaluation.toClassDetailsString());
            
            double[] attributeWeights= get_data_statics(trainingData);
            int[] get_num_values= get_num_values(trainingData);
            
            trainingData.delete();
            ArrayList<List<Integer>> temp = new ArrayList<List<Integer>>();  
            int num_perturbed= 5000;
            Instances syntheticData = new Instances(trainingData, num_perturbed);
            int num_exp=num_run;
    		for (int j = 1; j < num_exp; j++) {
    			double predictedClass=0.0;
    			Instance exp_instance=testingData.get(j);
    			Instances perturbed= perturbCategoricalData(syntheticData, attributeWeights, get_num_values, exp_instance, num_perturbed);    			
    			for (int i = 0; i < perturbed.numInstances(); i++) {
                    Instance newInst = perturbed.instance(i);                    
                    predictedClass = randomForest_org.classifyInstance(newInst);
                    newInst.setValue(perturbed.classIndex(), perturbed.classAttribute().value((int) predictedClass));                   
                }
    			mbLearner.search(perturbed);    
    			perturbed.delete();
    			String s_mblist=String.valueOf(mbLearner.tolist());
    			System.out.println(s_mblist.substring(1, s_mblist.length()-1));
    			
        		List<Integer> result =  mbLearner.tolist(); 
        		temp.add(result);
    			
    			
    			if (max_len<result.size()) max_len=result.size();
    			
    		}
    		String [] header = new String[max_len];
    		for (int i = 0; i < max_len; i++) header[i] = String.valueOf(i);
    		csv_selected_data.add(header);
    		
    		int num_exfea=0;
    		
    		for (int j = 0; j < temp.size(); j++) {
    			int[] result_a = temp.get(j).stream().mapToInt(i->i).toArray();
    			String [] result_s = new String[max_len];
    			num_exfea+=result_a.length;
    			for (int i = 0; i < max_len; i++) {
    				if (i<result_a.length) result_s[i] = String.valueOf(result_a[i]);
    				else result_s[i] = "NaN";
    			}    			
    			csv_selected_data.add(result_s);
    		} 
    		System.out.println(max_len);
    		System.out.println(num_exfea);
    		System.out.println(temp.size());
    		
    		writeListToCSV(csv_selected_data, exp_File);

            System.out.println("Explained features file saved successfully!");
        	
    		
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
