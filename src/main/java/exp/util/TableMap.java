package exp.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import weka.core.Instance;

public class TableMap extends Tabulator{
	
	private Map< Vector<Integer>, Map<Vector<Integer>, Double> > m_HashMaps;
    
    private TabulatorTests m_TableTests;
    
	/**
	 * C'tr of the class. It initializes the inner data structure
	 * given the variable vectors be given.
	 * 
	 * @param varVec A set of variable vectors indicating those contigency
	 * 				tables to be processed. 
	 */
	public TableMap( Vector<Set<Integer>> varVec ){
		
		m_HashMaps = new HashMap< Vector<Integer>, Map<Vector<Integer>, Double>>();
		for( int i = 0; i < varVec.size(); i++ ){
			Vector<Integer> key = new Vector<Integer>(varVec.get(i));
			Collections.sort(key);		// Sort to guarantee the order so as the key given a set
			//m_HashMaps.put(key, new HashMap<Vector<Integer>, Double>() );
			m_HashMaps.put(key, null );			// Postpone the allocation only when it is necessary later, to save memory
		}
		
		//m_TableTests = new TabulatorTests( TabulatorTests.TestMethod.PearsonChiSquare );
		m_TableTests = new TabulatorTests( TabulatorTests.TestMethod.GSquare );
	}
	
	/**
	 * Update the ALL inner contigency tables given an instance
	 */
	protected boolean updateTables( Instance instance ){
		
		boolean result = true;
		double freq = instance.weight();
		m_RecordCount += freq;
		
		// get the actual frequency of this stance; 1.0 by default
		//Set<Vector<Integer>> keySet = fHashMaps.keySet();
		Iterator<Vector<Integer>> iterKeys = m_HashMaps.keySet().iterator();
		while( iterKeys.hasNext() ){
			Vector<Integer> key = iterKeys.next();						//key for the outter layer
			Vector<Integer> key2 = new Vector<Integer>(key.size());		//key for the inner layer
			for( int i = 0; i < key.size(); i++ ){	
				key2.add(i, (int)instance.value(key.get(i)));
			}
			
			Map<Vector<Integer>, Double> hashMap = m_HashMaps.get( key );
			if( null == hashMap ){
				hashMap = new HashMap<Vector<Integer>, Double>();
				m_HashMaps.put(key, hashMap);
			}
	
			Double value = hashMap.get(key2);
			if( value != null )
				hashMap.put(key2, value + freq);
			else
				hashMap.put(key2, freq);
	
			/*
			if( hashMap.containsKey(key2) )
			{	
				// Update existing cell
				hashMap.put(key2, hashMap.get(key2) + freq );
			}
			else
			{
				hashMap.put(key2, freq);
			}
			*/
		}
		
		return result;
	}
	
	/**
     * Conducts test of independence for specified variables using the
     * the table corresponding to the given vector of variable indices.
     * The two variables must be contained in the vector while the rest
     * of the variables are used as the conditioning set.
     *
     * @param xVar    Variable for independence testing contained in vars.
     * @param yVar    Variable for independence testing contained in vars.
     * @param vars    Vector of variable indices given in the constructor
     *                argument varVec.
     * @param pValue  (Out) p-value for the test of independence.
     *
     * @return True if test is valid.
     */
	/*boolean getCITest( int xVar, int yVar, Vector<Integer> vars, double pVal )
	{
		boolean result = true;
		boolean smallSample = false;
		
		
		return getCITest( xVar, yVar, vars, pVal, smallSample );
	}*/
	
	/**
     * Overloaded getCITest method reporting if the small sample size
     * has been found.
     *
     * @param xVar    Variable for independence testing contained in vars.
     * @param yVar    Variable for independence testing contained in vars.
     * @param vars    Vector of variable indices given in the constructor
     *                argument varVec.
     * @param testStats  (Out) Statistics for the test of independence.
     * @param smallSample (Out) True if test is unreliable because of
     *                          the small sample size.
     *
     * @return True if test is valid.
     */
    public boolean getCITest( int xVar, int yVar, Set< Integer > varSet, 
    		Vector<Double> testStats
    ){
    	
    	boolean result = true;
    	
    	if( xVar == yVar ){
    		System.out.println( "xVar == yVar in getCITest" );
    		result = false;
    	}
    	
    	Vector<Integer> vars = new Vector<Integer>(varSet);
    	Collections.sort( vars );			//Sort to corresponds to the constructor, to ensure get the correct key   	
    	
    	int nVars = vars.size();
    	int indX = 0;
    	// retrieve the actual position of X in set
    	while( indX < nVars && xVar != vars.get(indX) ) ++indX;
    	if( indX >= nVars )	{
    		System.out.println( "indX >= nVars in getCITest" + xVar );
    		result = false;
    	}
    	
    	int indY = 0;
    	// retrieve the actual position of Y in set
    	while( indY < nVars && yVar != vars.get(indY) ) ++indY;
    	if( indY >= nVars ){
    		System.out.println( "indY >= nVars in getCITest: " + xVar + ", " + yVar );
    		result = false;
    	}
    	
    	Map<Vector<Integer>, Double> hashMap = m_HashMaps.get(vars);
    	
    	if( result && hashMap != null ){
    		
    		//m_SmallSample = false;
    		Vector<Double> stats = new Vector<Double>( m_TableTests.getCondTest(hashMap, indX, indY, 
    				m_AllVarCats.get(xVar), m_AllVarCats.get(yVar) ) );
    		for( int i = 0; i < stats.size(); i++ ){
    			testStats.set(i, stats.get(i));
    		}
    		//m_SmallSample = m_TableTests.isSmallSample();
    		m_SmallSample = false;
    		
    		if( Math.abs( testStats.get(2) + 1 ) < 1e-5 || m_SmallSample )
    			result = false;
    	}
    	
    	// return result;
    	return ( result );
    }
    
    public static void main(String[] args){
    	Set<Integer> testSet = new HashSet<Integer>();
    	
    	testSet.add( 10 );
    	testSet.add( 1 );
    	testSet.add( 3 );
    	testSet.add( 35 );
    	
    	Vector<Integer> testVect = new Vector<Integer>(testSet);
    	for( int i = 0; i < testVect.size(); i++ )
    		System.out.println( testVect.get(i) );
    	
    	Map<Set<Integer>, Double> testMap = new HashMap<Set<Integer>, Double>();
    	testMap.put( testSet, 1.0);
    	
    	Set<Integer> testSet2 = new HashSet<Integer>();
    	
    	testSet.add( 1 );
    	testSet.add( 3 );
    	testSet.add( 10 );
    	testSet.add( 35 );
    	System.out.println( testMap.get( testSet2 ) );
    	System.out.println( testMap.get( testSet ) );
    	
    	Map<Vector<Integer>, Double> testMap2 = new HashMap<Vector<Integer>, Double>();
    	testMap2.put( testVect, 1.0);
    	System.out.println( testMap2.get(testVect) ) ;
    	
    	Vector<Integer> testVect2 = new Vector<Integer>(testVect);
    	System.out.println( testMap2.get(testVect2) ) ;
    	
    	Vector<Integer> testVect3 = new Vector<Integer>(testVect.size());
    	for( int i = 0; i < testVect.size(); i++ )
    		testVect3.add( testVect.get(i));
    	System.out.println( testMap2.get(testVect3) ) ;
    	
    	
    }
    
}
