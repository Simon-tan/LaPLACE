package exp.util;

import java.util.HashMap;
import java.util.Vector;

import weka.core.Instance;

public class TableList extends Tabulator
{
	private Vector<Integer> m_FixVars;
	private Vector<Integer> m_AddVars;
	
	private Vector< HashMap<Vector<Integer>, Double> > m_HashTables;
	private TabulatorTests m_TableTests;
	
	boolean m_CheckMissing;
	
	public TableList(Vector<Integer> fixVars, Vector<Integer> addVars) throws Exception
	{
		if( fixVars.size() > 0 && addVars.size() > 0 )
		{
			m_FixVars = new Vector<Integer>(fixVars);
			m_AddVars = new Vector<Integer>(addVars);
			m_HashTables = new Vector<HashMap<Vector<Integer>, Double>>(m_AddVars.size());
			
			int i;
			for( i = 0; i < m_AddVars.size(); i++ )
			{
				boolean overlapping = m_FixVars.contains(m_AddVars.get(i));
				if( overlapping )
				{
					throw new Exception ("fixVars and addVars must be mutual one another, i.e. no overallping");
				}
			}
			
			for( i = 0; i < m_AddVars.size(); i++ )
				m_HashTables.add(new HashMap<Vector<Integer>, Double>());
			
			m_TableTests = new TabulatorTests(TabulatorTests.TestMethod.PearsonChiSquare);
			m_CheckMissing = false;			//test only listwise deletion!
		}
		else
		{
			//System.out.println("fixVars.size() and addVars.size() must > 0");
			throw new Exception("fixVars.size() and addVars.size() must > 0");
		}
	}
	
	protected boolean updateTables( Instance instance )
	{
		boolean result = true;
		double freq = 1.0;
		m_RecordCount += freq;
		
		if( m_CheckMissing )
			updatePairwise( instance, freq );
		else
			updateListwise( instance, freq );
		
		return result;
	}
		
	// Update tables assuming pairwise deletion for missing values
	private void updatePairwise(Instance instance, double freq)
	{
		
	}
	
	// Update tables assuming listwise deletion for missing values
	private void updateListwise(Instance instance, double freq)
	{
		int i, j;
		Vector<Integer> key = new Vector<Integer>();		// Fix vars plus one addVar
		for( i = 0; i < m_FixVars.size() + 1; i++ ) key.add(0);
		for( i = 0; i < m_AddVars.size(); i++ )
		{
			HashMap<Vector<Integer>, Double> hashMap = m_HashTables.get(i);			
			for( j = 0; j < m_FixVars.size(); j++ )
			{	
				key.set(j, (int)instance.value(m_FixVars.get(j)));
			}
			key.set(m_FixVars.size(), (int)instance.value(m_AddVars.get(i)));
			
			Double value = hashMap.get(key);
			if( value != null )
				hashMap.put(key, value + freq);
			else
				hashMap.put(key, freq);
			
		}
	}
	
	public boolean getCITest( int fixVar, int addVar, Vector<Double> testStats )
	{
		boolean result = true;
		
		int indX = 0; 
		while( indX < m_FixVars.size() && fixVar != m_FixVars.get(indX) ) ++indX;
		if( indX >= m_FixVars.size() )
		{
			System.out.println("indX >= m_FixVars.size() in TableList.getCITest");
			result = false;
		}
		
		int indY = 0;
		while( indY < m_AddVars.size() && addVar != m_AddVars.get(indY) ) ++indY;
		if( indY >= m_AddVars.size() )
		{
			System.out.println("indY >= m_AddVars.size() in TableList.getCITest");
			result = false;
		}
		
		if( result )
		{
			Vector<Double> testResults = m_TableTests.getCondTest(m_HashTables.get(indY), indX, m_FixVars.size(),  // the single addVar lies on the tail, i.e. m_FixVars.size() 
    				m_AllVarCats.get(fixVar), m_AllVarCats.get(addVar) );
			m_SmallSample = m_TableTests.isSmallSample();
    		for( int i = 0; i < 3; i++ ) testStats.set(i, testResults.get(i));
			m_SmallSample = false;
    		//double nCats = 1.0;
    		
    		//int iVar = 0;
    		//for(; iVar < m_FixVars.size(); ++iVar )
    		//	nCats *= (double)m_AllVarCats.get( m_FixVars.get(iVar) ).size();
    		//nCats *= (double)m_AllVarCats.get(addVar).size();
    		//if( m_RecordCount < 5.0 * nCats )	m_SmallSample = true;
    		
    		if( Math.abs( testStats.get(2) + 1 ) < 1e-5 || m_SmallSample )
    			result = false;
		}
		
		return result;
	}
}
