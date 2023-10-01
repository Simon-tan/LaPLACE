package exp.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MBLearnerTestingStatistics implements Serializable
{
	private static final long serialVersionUID = MBLearnerTestingStatistics.class.getCanonicalName().hashCode();
	
	protected String m_LearnerName;
	
	protected int m_NumCITests = 0;
	
	protected int m_NumDataPasses = 0;
	
	// Frequencies given different conditioning set size
	protected Map<Integer, Integer> m_FreqCondSize = null;
	
	// Number of removal of false positives given different conditioning set.
	protected Map<Integer, Integer> m_NumRemovalsCondSize = null;
	
	
	public MBLearnerTestingStatistics( String name ){
		m_LearnerName = name;
		m_FreqCondSize = new HashMap<Integer, Integer>();
		m_NumRemovalsCondSize = new HashMap<Integer, Integer>();
	}
	
	public final String getLearnerName(){
		return m_LearnerName;
	}
	
	public int getNumCITests(){
		return m_NumCITests;
	}
	
	public int getNumDataPasses(){
		return m_NumDataPasses;
	}
	
	public final Map getFreqCondSize(){
		return m_FreqCondSize;
	}
	
	public final Map getNumRemovalsCondSize(){
		return m_NumRemovalsCondSize;
	}
	
	public void incrCITests( int n ){
		m_NumCITests += n;
	}
	
	public void incrDataPasses( int n ){
		m_NumDataPasses += n;
	}
	
	public void updateFreqCondSize( int condSize, int freq ){
		
		if( m_FreqCondSize.containsKey( condSize ) )
			m_FreqCondSize.put( condSize, m_FreqCondSize.get(condSize) + freq );
		else
			m_FreqCondSize.put( condSize, freq );
	}
	
	public void updateNumRemovalsCondSize( int condSize, int freq ){
		if( m_NumRemovalsCondSize.containsKey( condSize ) )
			m_NumRemovalsCondSize.put( condSize, m_FreqCondSize.get(condSize) + freq );
		else
			m_NumRemovalsCondSize.put( condSize, freq );
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		
		buffer.append( m_LearnerName + "\n" );
		buffer.append( "# CI Test\t" + m_NumCITests + "\n" );
		buffer.append("# Data Passes\t" + m_NumDataPasses + "\n" );
		buffer.append( "Number of CI Tests given different conditioning set:\n" );
		if( m_FreqCondSize.size() > 0 ){
			for( Iterator<Map.Entry<Integer, Integer>> iter = m_FreqCondSize.entrySet().iterator(); iter.hasNext(); ){
				Map.Entry<Integer, Integer> entry = iter.next();
				Integer condSize = entry.getKey();
				Integer times = entry.getValue();
				buffer.append( "\t[" + condSize + "]\t" + times + "\n" );
			}
		}
		
		if( m_NumRemovalsCondSize.size() > 0 ){
			
		}
		
		return buffer.toString();
	}
	
	public void clear(){
		m_NumCITests = 0;
		m_NumDataPasses = 0;
		m_FreqCondSize.clear();
		m_NumRemovalsCondSize.clear();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		MBLearnerTestingStatistics testStat = new MBLearnerTestingStatistics("IPC-MB");
		testStat.incrCITests( 1 );
		testStat.incrDataPasses( 2 );
		testStat.updateFreqCondSize( 0 , 1 );
		testStat.updateFreqCondSize( 1 , 2 );
		testStat.updateFreqCondSize( 0 , 1 );
		System.out.println( testStat.toString() );
		testStat.clear();
		System.out.println( testStat.toString() );
	}

}
