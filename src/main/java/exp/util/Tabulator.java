package exp.util;

import java.util.Vector;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public abstract class Tabulator
{
	public Tabulator(){}
	
	public void setVariableCategories( Instances data )
	{
		m_AllVarCats = new Vector<Vector<Integer>>(data.numAttributes());
		for( int i = 0; i < data.numAttributes(); i++ ){
			Attribute attr = data.attribute( i );
			Vector<Integer> values = new Vector<Integer>(attr.numValues());
			for( int j = 0; j < attr.numValues(); j++ ){
				values.add(j);
			}
			m_AllVarCats.add(values);
		}
	}
	
	public void setFrequencyVariable( int freqVarInd )
	{
		m_FreqVarInd = freqVarInd;
	}
	
	public int getFrequencyVariable()
	{
		return m_FreqVarInd;
	}
	
	protected abstract boolean updateTables( Instance instance );
	
	public double getRecordCount()
	{
		return m_RecordCount;
	}
	
	public boolean isSmallSample(){
		return m_SmallSample;
	}
	
	protected Vector<Vector<Integer>> m_AllVarCats;
	
	protected int m_FreqVarInd;
	
	protected double m_RecordCount;
	
	protected boolean m_SmallSample;
}
