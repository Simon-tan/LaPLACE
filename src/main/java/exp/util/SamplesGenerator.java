package exp.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;
import weka.classifiers.bayes.net.ParentSet;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.estimators.Estimator;

public class SamplesGenerator
{ 
	BIFReader m_BIFReader;
	
	int m_NrInstances = 100;
	
	Instances m_Instances;
	
	Random m_Random;
	
	ParentSet[] m_ParentSets;
	
	BayesNet m_BayesNet;
	
	/**
     * The attribute estimators containing CPTs.
     */
    public Estimator[][] m_Distributions;
	
	public SamplesGenerator( String xmlBif ){
		m_BIFReader = new BIFReader();
		try{
			m_BayesNet = new BayesNet();
			m_BayesNet = m_BIFReader.processFile( xmlBif );
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void setNumSamples( int n ){
		if( n > 0 )	m_NrInstances = n;
	}
	
	/**
	 * GenerateInstances generates random instances sampling from the
	 * distribution represented by the Bayes network structure. It assumes
	 * a Bayes network structure has been initialized
	 * 
	 * @throws Exception if something goes wrong
	 */
	public void generateInstances ( int numSamples ) throws Exception {
		
		setNumSamples( numSamples );		
		init();		// Prepare some data structures and metadata
		
	    int [] order = getOrder();
		for (int iInstance = 0; iInstance < m_NrInstances; iInstance++) {
		    int nNrOfAtts = m_Instances.numAttributes();
			Instance instance = new DenseInstance(nNrOfAtts);
			instance.setDataset(m_Instances);
			for (int iAtt2 = 0; iAtt2 < nNrOfAtts; iAtt2++) {
			    int iAtt = order[iAtt2];

				double iCPT = 0;

				for (int iParent = 0; iParent < m_ParentSets[iAtt].getNrOfParents(); iParent++) {
				  int nParent = m_ParentSets[iAtt].getParent(iParent);
				  iCPT = iCPT * m_Instances.attribute(nParent).numValues() + instance.value(nParent);
				} 
	
				double fRandom = m_Random.nextInt(1000) / 1000.0f;
				int iValue = 0;
				
				while ( fRandom > m_Distributions[iAtt][(int) iCPT].getProbability(iValue)) {
					fRandom = fRandom - m_Distributions[iAtt][(int) iCPT].getProbability(iValue);
					iValue++ ;
				}
				instance.setValue(iAtt, iValue);
			}
			m_Instances.add(instance);
		}
	} // GenerateInstances
	
	public Instances getInstances(){
		return m_Instances;
	}
	
	public void saveAsARFF( String arff ) throws IOException{
		ArffSaver saver = new ArffSaver();
		saver.setInstances( m_Instances );
		saver.setFile( new File( arff) );
		//saver.setDestination( new File(arff) );
		saver.writeBatch();
	}
	
	protected int[] getOrder(){
		
		int nAttrs = m_Instances.numAttributes();
		int[] order = new int[ nAttrs ];
		boolean[] covered = new boolean[ nAttrs ];
		int nCovered = 0;
		
		// Find those without parents first
		for( int iNode = 0; iNode < nAttrs; iNode++ ){
			if( m_ParentSets[iNode].getNrOfParents() < 1 ){
				covered[iNode] = true;
				order[nCovered++] = iNode;
			}else{
				covered[iNode] = false;			
			}
		}
		
		// Processing the remaining ones
		while( nCovered < nAttrs ){			
			for( int iNode = 0; iNode < nAttrs; iNode++ ){
				if( covered[ iNode ] )	
					continue;
				
				boolean toIgnore = false;
				for (int iParent = 0; iParent < m_ParentSets[iNode].getNrOfParents(); iParent++) {
					int nParent = m_ParentSets[iNode].getParent(iParent);
					if( !covered[nParent] ){		// There is one parent not covered.
						toIgnore = true;
						break;
					}
				}
				if( !toIgnore ){
					order[nCovered++] = iNode;
					covered[ iNode ] = true;
				}
			}
		}
		
		return order;
	}
	
	private void init(){
		
		m_Random = new Random( System.currentTimeMillis() );
		
		FastVector attInfo = new FastVector( m_BIFReader.getNrOfNodes() );
		
		int nNodes = m_BIFReader.getNrOfNodes();
		/*
		for (int iNode = 0; iNode < nNodes; iNode++) {
			int nValues = m_BIFReader.getCardinality( iNode);
			FastVector nomStrings = new FastVector( nValues );
			for( int iValue = 0; iValue < nValues; iValue++ )
				nomStrings.addElement( m_BIFReader.getNodeValue(iNode, iValue) );
			Attribute att = new Attribute(m_BIFReader.getNodeName(iNode), nomStrings);
			attInfo.addElement(att);
						
		}		
		m_Instances = new Instances( m_BIFReader.getName(), attInfo, m_NrInstances );
 		m_Instances.setClassIndex( nNodes - 1);
 		*/
 		m_Instances = new Instances( m_BIFReader.m_Instances, m_NrInstances );
 		
 		m_ParentSets = new ParentSet[ nNodes ];
 		for (int iNode = 0; iNode < nNodes; iNode++) {	
			ParentSet parents = new ParentSet();
			for( int iParent = 0; iParent < m_BIFReader.getNrOfParents(iNode); iParent++ ){				
				parents.addParent( m_BIFReader.getParent(iNode, iParent), iParent, m_Instances );
			}
			m_ParentSets[ iNode ] = parents;
		}
		
 		m_Distributions = m_BIFReader.getDistributions(); 		
 	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append( m_Instances.numInstances() + " instances are generated \n" ); 
		buffer.append( m_Instances.numAttributes() + " attributes totally\n");
		for( int i = 0; i < m_Instances.numAttributes(); i++ ){
			buffer.append( i + ":" + m_Instances.attribute(i).name() + 
					"(" + m_Instances.attribute(i).numValues() + ")\n"); 
		}
		
		return buffer.toString();
	}
	
	/**
	 * Generate a simple graph to be referred by MBLearnerEvaluator
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void generateGraphFile( String file ) throws IOException{
		BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
		StringBuffer buffer = new StringBuffer();
		buffer.append("%" + m_BIFReader.getName() + "\n");
		buffer.append("% by Sein Minn(sein.minn.cs@gmail.com)\n");
		buffer.append("% Totally there are " + m_ParentSets.length + " nodes\n");
		for( int iNode = 0; iNode < m_ParentSets.length; iNode++ ){
			buffer.append(iNode);
			for( int iParent = 0; iParent < m_ParentSets[iNode].getNrOfParents(); iParent++ ){
				buffer.append(","+ m_ParentSets[iNode].getParent(iParent));
			}
			buffer.append("\n");
		}
		writer.write( buffer.toString() );
		writer.close();
	}
	
	public static void main( String[] args ){
	
		int numInstances = 5000;
		String bif = "/home/simon/Documents/Interpret/BN_XML/alarm.xml";
		String arff = "/home/simon/Documents/Interpret/BN_XML/Generate/alarm_" + numInstances/1000 + "k.arff";
		String graph = "/home/simon/Documents/Interpret/BN_XML/Generate/alarm_" + numInstances/1000 + "k_graph.txt";
		
		SamplesGenerator sg = new SamplesGenerator( bif );
		try{
			sg.generateInstances( numInstances );
			System.out.println(sg.toString());
			sg.saveAsARFF( arff );
			sg.generateGraphFile( graph );
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
}
