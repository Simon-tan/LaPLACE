package exp.util;

import weka.core.Instance;
import weka.core.Instances;


public class ContTableProcessor
{
	public ContTableProcessor(){
	}
	
	/**
	 * Data processor.
	 * 
	 * @param data The original data which must be binned already.
	 * @param tabulator The contingency table to be updated given the data.
	 */
	public void ProcessSource( Instances data, Tabulator tabulator )
	{
		fRecordCount = 0;
		fValidCount = 0;
			
		tabulator.setVariableCategories( data );
		for( int i = 0; i < data.numInstances(); i++ ){
			Instance instance = data.instance(i);
			if( instance != null ){
				fRecordCount++;
				if( IsValidInstance(instance) )	{
					if( tabulator.updateTables( instance ) )
						fValidCount++;
				}
			}
		}		
	}
	
	/**
	 * Check the validation of given instance based on pre-defined rules.
	 * 
	 * @param instance
	 * @return True always for the time being.
	 */
	private boolean IsValidInstance( Instance instance )
	{
		boolean result = true;
		
		return result;
	}
	
	private int fRecordCount;
	private int fValidCount;
	private int fFreqVarInd;
}
