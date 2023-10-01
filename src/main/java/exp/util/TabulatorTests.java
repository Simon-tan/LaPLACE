package exp.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class TabulatorTests
{
	// Conditional sets
	private HashMap<Vector<Integer>, Double> m_CondMap;
	
	private int m_KeyLen;

	// Testing method
	private TestMethod m_TestMethod;
	
	// Table index of variable X
	private int m_VarIndX;
	
	// Table index of variable Y
	private int m_VarIndY;
	
	// Conditional variables indices
	Vector<Integer> m_CondVarInd;

	// Frequency(cell counts) matrix
	List<Double> m_FreqMat;
	
	// Row totals( of cell counts)
	List<Double> m_RowTotals;
	
	// Colum totals (of cell counts)
	List<Double> m_ColTotals;
	
	// Test statistic value
	double m_TestStat;
	
	// Degrees of freedom
	double m_Df;
	
	int m_nLargeCells;

	public static enum TestMethod{
		PearsonChiSquare,
		GSquare				// likelihood method
	};
	
	private boolean isSmallSample = false;
	
	public TabulatorTests( TestMethod testMethod ){
		
		m_TestMethod = testMethod;
		m_VarIndX = -1;
		m_VarIndY = -1;
		m_TestStat = 0.0;
		m_Df = 0.0;
		m_KeyLen = 0;
		m_CondMap = null; //new HashMap<Vector<Integer>, Double>();
		m_CondVarInd = new Vector<Integer>();
		
		m_RowTotals = new ArrayList<Double>(); 
		m_FreqMat = new ArrayList<Double>();
		m_ColTotals = new ArrayList<Double>();
	}
	
	public boolean isSmallSample(){
		return isSmallSample;
	}
	
	/**
	 * Construct a contingency table from the original one excluding xVar and yVar
	 * 
	 * @param hashMap
	 * @return
	 */
	public boolean SetCondTable( Map<Vector<Integer>, Double> hashMap ){
		
		boolean result = false;
		
		m_CondMap = null;
				
		if( m_KeyLen > 2 ){
			m_CondVarInd.clear();
			int keyInd = 0;
			for( ; keyInd < m_KeyLen; ++keyInd ){
				if( keyInd != m_VarIndX && keyInd != m_VarIndY )
					m_CondVarInd.add( keyInd );			// Cache the index for each variable in the key
			}
			
			// Create table of conditional variables
			m_CondMap = new HashMap<Vector<Integer>, Double>();
			Vector<Integer> condKey = new Vector<Integer>( m_KeyLen - 2 );
			for( int i = 0; i < m_KeyLen-2; i++ ) condKey.add(new Integer(-1));
			
			Iterator<Vector<Integer>> iter = hashMap.keySet().iterator();
			while( iter.hasNext() )
			{
				Vector<Integer> key = iter.next();
				// Construct a new key (about the conditioning set) excluding xVar and yVar
				for( keyInd = 0; keyInd < m_KeyLen - 2; ++keyInd ){
					//key.get(fCondVarInd.get(keyInd));
					condKey.set( keyInd, key.get(m_CondVarInd.get(keyInd)) );
				}
				
				Double pVal = m_CondMap.get( condKey );
				if( pVal != null )
					m_CondMap.put( condKey, pVal + 1.0 );
				else
					m_CondMap.put( new Vector<Integer>(condKey), new Double(1.0) );			
			}
			
			// set result to true if the map is not empty.
			if( m_CondMap.size() > 0 )	result = true;				
		}
		
		return result;
	}
	
	/**
	 * Do the actual conditional testing here.
	 * 
	 * @param hashMap 	it stores the actual pair count corresponding to <x,y,condition set>
	 * @param xVarInd	it is the actual position of x in the key set of hashMap
	 * @param yVarInd	it is the actual position of y in the key set of hashMap
	 * @param xVarCats	x's categories
	 * @param yVarCats	y's categories
	 * @return
	 */
	public Vector<Double> getCondTest(
		Map<Vector<Integer>, Double> hashMap,
		int xVarInd,
		int yVarInd,
		final Vector<Integer> xVarCats,
		final Vector<Integer> yVarCats
	){
		
		Vector<Double> statVec = new Vector<Double>(3);
		for( int i = 0; i < 3; i++ )	statVec.add( new Double(0) );
		isSmallSample = false;
		m_nLargeCells = 0;
		
		if( hashMap.size() > 0 ){
			// Get the length of the key
			m_KeyLen = hashMap.keySet().iterator().next().size();
			if( m_KeyLen < 2 )	System.out.println("Key's length must be at least 2");
						
			m_VarIndX = xVarInd;
			m_VarIndY = yVarInd;
			
			int nRows = xVarCats.size();
			int nCols = yVarCats.size();
			m_FreqMat.clear(); //= new ArrayList<Double>( nRows * nCols );
			m_RowTotals.clear(); // = new ArrayList<Double>( nRows );
			m_ColTotals.clear(); // = new ArrayList<Double>( nCols );
			int i, j;
			for( i = 0; i < nRows; i++ ){
				m_RowTotals.add( i, new Double(0.0) );
				for( j = 0; j < nCols; j++ ){
					m_FreqMat.add( i * nCols + j, new Double(0.0));		// Pay attention to index construction i * nCols + j					
					if( i < 1 ) m_ColTotals.add( j, new Double(0.0) );
				}
			}
			
			Vector<Integer> key = new Vector<Integer>( m_KeyLen );
			for( i = 0; i < m_KeyLen; i++ )	key.add(new Integer(0));
			
			if( m_KeyLen > 2 && SetCondTable(hashMap) ){
				
				statVec.set(0, 0.0);
				statVec.set(1, 0.0);
				
				Iterator<Map.Entry<Vector<Integer>, Double>> iter = m_CondMap.entrySet().iterator();
				while( iter.hasNext() ){
					
					Map.Entry<Vector<Integer>, Double> entry = iter.next();
					Vector<Integer> condKey = (Vector<Integer>)entry.getKey();
					int keyInd = 0;
					for( ; keyInd < m_KeyLen - 2; keyInd++ )
						key.set( m_CondVarInd.get(keyInd), condKey.get(keyInd) ); 
					ComputeTestStats(hashMap, key, xVarCats,yVarCats );
					if( -1 != m_Df ){		//-1 is tag for missing. need replace with a
						statVec.set( 0, statVec.get(0) + m_TestStat );
						statVec.set(1, statVec.get(1) + m_Df);
					}
				}
				
				if( statVec.get(1) <= 0.0 )
					statVec.set(1, -1.0);		//set as missing
			}else if( 2 == m_KeyLen ){
				ComputeTestStats(hashMap,key, xVarCats,yVarCats );
				statVec.set(0, m_TestStat);
				statVec.set(1, m_Df);
			}			
		}
		
		if( statVec.get(1) != -1 ){
			double pValue = -1;
			double cdf = CdfChi( statVec.get(0), statVec.get(1) );
			
			if( cdf != -1 )	pValue = 1.0 - cdf;
			statVec.set(2, pValue);
		}
		
		return statVec;
	}
	
	public Vector<Double> getCondTest
	(
		Map<Vector<Integer>, Double> hashMap,
		int xVarInd,
		int yVarInd,
		final Vector<Integer> xVarCats,
		final Vector<Integer> yVarCats,
		boolean smallSample
	)
	{
		Vector<Double> statVec = new Vector<Double>(3);
		for( int i = 0; i < 3; i++ )	statVec.add( new Double(0) );
		isSmallSample = false;
		
		if( hashMap.size() > 0 )
		{
			// Get the length of the key
			m_KeyLen = hashMap.keySet().iterator().next().size();
			if( m_KeyLen < 2 )	System.out.println("Key's length must be at least 2");
						
			m_VarIndX = xVarInd;
			m_VarIndY = yVarInd;
			
			int nRows = xVarCats.size();
			int nCols = yVarCats.size();
			m_FreqMat = new ArrayList<Double>( nRows * nCols );
			m_RowTotals = new ArrayList<Double>( nRows );
			m_ColTotals = new ArrayList<Double>( nCols );
			int i, j;
			for( i = 0; i < nRows; i++ ){
				m_RowTotals.add( new Double(0.0) );
				for( j = 0; j < nCols; j++ ){
					m_FreqMat.add( new Double(0.0) );
					if( i < 1 ) m_ColTotals.add( new Double(0.0) );
				}
			}
			
			Vector<Integer> key = new Vector<Integer>( m_KeyLen );
			for( i = 0; i < m_KeyLen; i++ )	key.add(new Integer(0));
			
			if( m_KeyLen > 2 && SetCondTable(hashMap) )
			{
				statVec.set(0, 0.0);
				statVec.set(1, 0.0);
				
				Iterator<Map.Entry<Vector<Integer>, Double>> iter = m_CondMap.entrySet().iterator();
				while( iter.hasNext() ){
					
					Map.Entry<Vector<Integer>, Double> entry = iter.next();
					Vector<Integer> condKey = (Vector<Integer>)entry.getKey();
					int keyInd = 0;
					for( ; keyInd < m_KeyLen - 2; keyInd++ )
						key.set( m_CondVarInd.get(keyInd), condKey.get(keyInd) ); 
					ComputeTestStats(hashMap, key, xVarCats,yVarCats );
					if( -1 != m_Df )		//-1 is tag for missing. need replace with a 
					{
						statVec.set( 0, statVec.get(0) + m_TestStat );
						statVec.set(1, statVec.get(1) + m_Df);
					}
				}
				
				if( statVec.get(1) <= 0.0 )
					statVec.set(1, -1.0);		//set as missing
			}else if( 2 == m_KeyLen ){
				ComputeTestStats(hashMap,key, xVarCats,yVarCats );
				statVec.set(0, m_TestStat);
				statVec.set(1, m_Df);
			}
		}
		
		if( statVec.get(1) != -1 ){
			double pValue = -1;
			double cdf = CdfChi( statVec.get(0), statVec.get(1) );
			
			if( cdf != -1 )	pValue = 1.0 - cdf;
			statVec.set(2, pValue);
		}
		
		return statVec;
	}


	private void ComputeTestStats(
			Map<Vector<Integer>, Double> hashMap,
			Vector<Integer> key,
			Vector<Integer> xVarCats,
			Vector<Integer> yVarCats
	)
	{	
		double matTotal = 0.0;			// overall total count
		double cellCount = 0.0;			// cell count
		isSmallSample = false;
		
		int nRows = xVarCats.size();
		int nCols = yVarCats.size();
		int iFreq = 0;
		int iRow;
		int iCol;
		
		// Reset Col's totals upon necessity
		for( iCol = 0; iCol < nCols; iCol++ )	m_ColTotals.set(iCol, 0.0);
		
		// Prepare the row totals and column totals as needed by the test
		for( iRow = 0; iRow < nRows; iRow++ ){
			
			key.set( m_VarIndX,xVarCats.get(iRow) );			
			double rowSum = 0.0;
			
			for( iCol = 0; iCol < nCols; iCol++ ){	
				////////// Retrieve the cell count
				key.set( m_VarIndY, yVarCats.get(iCol) );
				
				Double pVal = hashMap.get(key);
				if( pVal != null)
					cellCount = pVal;
				else
					cellCount = 0.0;
			
				m_FreqMat.set( iFreq, cellCount );
				iFreq++;
				
				rowSum += cellCount;
				m_ColTotals.set( iCol, m_ColTotals.get(iCol) + cellCount );
			}
			m_RowTotals.set( iRow, rowSum );
			matTotal += rowSum;
		}
		
		// Compute expected counts and accumulate chi-square
		m_TestStat = 0.0;
		int nRowsNonZero = 0;		// number of rows with non-zero row totals
		int nColsNonZero = 0;		// number of cols with non-zero col totals
		int nLargeExpCount = 0;		// number of cells with large expected count
		
		iFreq = 0;
		for( iRow = 0; iRow < nRows; iRow++ ){
			if( m_RowTotals.get(iRow) > 0 ){
				nRowsNonZero++;
				double rt = m_RowTotals.get( iRow );
				for( iCol = 0; iCol < nCols; iCol++ )
				{
					if( m_ColTotals.get(iCol) > 0)
					{
						if( nRowsNonZero == 1)	nColsNonZero++;
						
						//Expected count
						double expectedCount = rt * m_ColTotals.get(iCol) / matTotal;
						
						// Actual count 
						cellCount = m_FreqMat.get( iFreq );
						
						if( TestMethod.GSquare == m_TestMethod && cellCount > 0.0 ){
							// Increment G-Square
							m_TestStat += 2.0 * cellCount * Math.log( cellCount / expectedCount );
						}else if ( TestMethod.PearsonChiSquare == m_TestMethod ){
							// Increment chi-square
							double diff = Math.abs( cellCount - expectedCount );							
							if( expectedCount < 5.0 ){
								diff -= 0.5;
							}
							m_TestStat += diff * diff / expectedCount;
						}
						
						// Check if the expected count is large enough
						if( expectedCount >= 5.0 )							
							m_nLargeCells++;						
						
					}
					iFreq++;
				}
			}
			else iFreq += nCols;
		}
		
		// Set degree of freedom
		if( ( nRowsNonZero > 1 ) && ( nColsNonZero > 1 ) )
			m_Df = ( nRowsNonZero - 1.0 ) * ( nColsNonZero - 1.0 );
		else
			m_Df = -1.0;
		
		// Compute the ratio of large cells
		/*
		if( nRows > 0 && nCols > 0 ){
			double largeCellsRatio = (double)nLargeExpCount / (double)nRows / (double)nCols;
			
			if( largeCellsRatio < 0.8 )
				isSmallSample = true;
		}
		*/
	}
			
	static class GammaResult{
		public double value;
		public long retcod;
		public GammaResult(double p1, long p2)
		{
			value = p1;
			retcod = p2;
		}
	}
	
	public static double CdfChi( double x, double df )
	{
		double result = -1;
		
		if( df > 0.0 )
		{
			if( x <= 0.0 )
				result = 0.0;
			else
			{
				GammaResult gammaResult = gammad( x/2.0, df/2.0, true );
				if( gammaResult.retcod != 0 )
					result = -1; 
				else
					result = gammaResult.value;
			}
		}
		
		return result;
	}
	
	public static GammaResult gammad( double x, double a, boolean qcdf )
	{
		final double oflo = Math.sqrt(Double.MAX_VALUE);
		
		final long maxLong = Long.MAX_VALUE;
		final double r0 = 0.0;
		final double r1 = 1.0;
		final double r2 = 2.0;
		final double r3 = 3.0;
		final double r4 = 4.0;
		final double r9 = 9.0;
		final double r10 = 10.0;
		final double rhalf = 0.5;
		final double minExp = -708.0;
		final double maxExp = 709.0;
		
		final double tol = 1.0e-14;		// Tolerance can be increased (e.g. to 1e-7) to improve speed
		
		//Local variables
		double aa, an, bb, cc, pn1, pn2, pn3, pn4, pn5, pn6;
		double exparg;			// argument for exp()
		int method;				// Mehtod type: 
								// 0 - Undertmined
								// 1 - Pearson's series expansion
								// 2 - Continued fraction expansion
								// 3 - Normal approximation
								// 4 - explicit formula
		double nterm;			// Number of terms needed in Pearson's series expansion
		//double result; 			// Returned value
		double rn;				// Next value of result in iterations
		GammaResult result = new GammaResult(r0,0);
		
		exparg = r0;
		method = 0;
		
		// Determine method type
		if( a > r0 )
		{
			// Obvious case: x <= 0
			if( x <= r0 )
			{
				method = 4;
				if( !qcdf )
					result.value = r1;
			}
			else if( r1 == a )
			{
				method = 4;
				exparg = -x;
				if( exparg >= minExp )
					result.value = Math.exp(exparg);
				
				if( qcdf )
					result.value = r1 - result.value;
			}
			else if( (x<=r1)||(x<=a))
			{
				method = 1;
				// Estimate number of terms needed to achieve the desired
				// tolerance. log10 is used because tol is usually expressed
				// in power of 10.
				nterm = Math.log10(x/(a+r1));
				if( nterm != r0 )
				{
					nterm = Math.log10(tol)/nterm;
					if( nterm > r0 )
					{
						// This equation approximates the actual terms used very well
						nterm = r10 + r2 * Math.sqrt(nterm);
						pn1 = maxLong;
						
						// If too many terms, use normal approximation
						if( ( nterm * nterm ) >= pn1 )
							method = 3;
					}
				}
				else
				{
					// Case 1: x <= a but x/(a+r1) = 1. This implies both x and a are very big and very close
					// Case 2: x <= 1 but x/(a+r1) = 1. This implies a is very small but x is very close to 1
					method = 3;
				}
				
				if( 1 == method )
				{
					exparg = dnlgam( a + r1 );
					if( exparg == -1 )
						method =3;
				}
			}
			else
			{
				method = 2;
				exparg = dnlgam(a);
				if( exparg == -1 || r4*Math.log(x) >= maxExp )
					method = 3;
			}
			
			if( method == 1 || method == 2 )
				exparg = a * Math.log(x) - (exparg + x);
		}
		else
			// Invalid value for "a"
			result.retcod = 1;
		
		if ( method == 1 )
	    {
	        // Use Pearson's series expansion.
	        aa = a;
	        cc = r1;
	        result.value = r1;

	        // Do until result increment is small enough
	        for ( ; ; )
	        {
	            aa = aa + r1;
	            cc = cc * x / aa;
	            result.value = result.value + cc;

	            if ( cc < tol )
	            {
	                break;
	            }
	        }

	        exparg = exparg + Math.log( result.value );

	        if ( exparg > r0 )
	        {
	            method = 3;             // result must be less than 1.
	        }
	        else if ( exparg >= minExp )
	        {
	            result.value = Math.exp( exparg );

	            if ( !qcdf)
	            {
	                result.value = r1 - result.value;
	            }
	        }
	        else
	        {
	            if ( qcdf )
	            {
	                result.value = r0;
	            }
	            else
	            {
	                result.value = r1;
	            }
	        }
	    }

	    if ( method == 2 )
	    {
	        // Use a continued fraction expansion
	        aa = r1 - a;
	        bb = aa + x + r1;
	        cc = r0;
	        pn1 = r1;
	        pn2 = x;
	        pn3 = x + r1;
	        pn4 = x * bb;
	        result.value = pn3 / pn4;

	        // Do until result increment is small enough
	        for ( ; ; )
	        {
	            aa = aa + r1;
	            bb = bb + r2;
	            cc = cc + r1;
	            an = aa * cc;
	            pn5 = bb * pn3 - an * pn1;
	            pn6 = bb * pn4 - an * pn2;

	            if ( Math.abs( pn6 ) > r0 )
	            {
	                rn = pn5 / pn6;

	                // Check for break out of the loop
	                double tolMin = tol*rn;

	                if ( tol < tolMin )
	                {
	                    tolMin = tol;
	                }

	                if ( Math.abs( result.value-rn ) <= tolMin )
	                {
	                    break;
	                }
	                else
	                {
	                    result.value = rn;
	                }
	            }

	            pn1 = pn3;
	            pn2 = pn4;
	            pn3 = pn5;
	            pn4 = pn6;

	            if ( Math.abs( pn5 ) >= oflo )
	            {
	                // Re-scale terms in continued fraction
	                pn1 = pn1 / oflo;
	                pn2 = pn2 / oflo;
	                pn3 = pn3 / oflo;
	                pn4 = pn4 / oflo;
	            }
	        }

	        exparg = exparg + Math.log( result.value );

	        if ( exparg > r0 )
	        {
	            method = 3;             // result must be less than 1.
	        }
	        else if ( exparg >= minExp )
	        {
	            result.value = Math.exp( exparg );

	            if ( qcdf )
	            {
	                result.value = r1 - result.value;
	            }
	        }
	        else
	        {
	            if ( qcdf )
	            {
	               result.value = r1;
	            }
	            else
	            {
	               result.value = r0;
	            }
	        }
	    }

	    if ( method == 3 )
	    {
	        // The "safe harbor" normal approximation.
	        if ( qcdf || (a < rhalf) )
	        {
	            // Wilson & Hilferty (1931) approximation.
	            pn1 = r3 * Math.sqrt( a );

	            if ( x != a )
	            {
	                exparg = Math.log( x / a ) / r3;     // won't overflow EXP.

	                if ( exparg >= minExp )
	                {
	                    pn2 = Math.exp( exparg );
	                }
	                else
	                {
	                    pn2 = r0;
	                }

	                pn1 = pn1 * (pn2 + r1 / (r9 * a) - r1);
	            }
	            else
	            {
	                pn1 = r1 / pn1;
	            }

	            if ( !qcdf )
	            {
	                pn1 = -pn1;
	            }

	            result.value = prbnrm( pn1 );
	        }
	        else
	        {
	            // Peizer & Pratt (1968) approximation:
	            // left tail and a >= 0.5.
	            pn1 = ( a - rhalf ) / x;      // pn1 >= 0.

	            if ( pn1 == r0 )
	            {
	               pn2 = Math.sqrt( r2 / x );
	            }
	            else if ( pn1 == r1 )
	            {
	               pn2 = Math.sqrt( r1 / x );
	            }
	            else
	            {
	               pn2 = r2 * ( r1 + pn1 * Math.log( pn1 ) - pn1 );
	               pn2 = Math.sqrt( pn2 / x ) / Math.abs( r1 - pn1 );
	            }

	            // The constant: 0.022 is due to Molennar (1970).
	            pn1 = ( a + (0.022 / a) - x - (r1 / r3) ) * pn2;
	            result.value = prbnrm( pn1 );
	        }
	    }

	    if ( result.retcod > 0 )
	    {
	        result.value = -1;
	    }
		
		return result;
	}
	
	// Natural logarithm of the gamma function
	static double dnlgam( double x )
	{
//		 Constants
	    final double PI = 3.14159265358979323846;
	    final double r0 = 0.0;
	    final double r0p25 = 0.25;
	    final double r0p5 = 0.5;
	    final double r1 = 1.0;
	    final double r2 = 2.0;
	    final double r3p5 = 3.5;
	    final double r4 = 4.0;
	    final double r4p5 = 4.5;
	    final double r8 = 8.0;

	    // a[22] - first set of coefficients
	    final double a[] =
	    {
	         0.00009967270908702825,
	        -0.00019831672170162227,
	        -0.00117085315349625822,
	         0.00722012810948319552,
	        -0.00962213009367802970,
	        -0.04219772092994235254,
	         0.16653861065243609743,
	        -0.04200263501129018037,
	        -0.65587807152061930091,
	         0.57721566490153514421,
	         0.99999999999999999764,
	         0.00004672097259011420,
	        -0.00006812300803992063,
	        -0.00132531159076610073,
	         0.00733521178107202770,
	        -0.00968095666383935949,
	        -0.04217642811873540280,
	         0.16653313644244428256,
	        -0.04200165481709274859,
	        -0.65587818792782740945,
	         0.57721567315209190522,
	         0.99999999973565236061
	    };

	    // b[98] - second set of coefficients
	    final double b[] =
	    {
	        -0.00000000004587497028,
	         0.00000000019023633960,
	         0.00000000086377323367,
	         0.00000001155136788610,
	        -0.00000002556403058605,
	        -0.00000015236723372486,
	        -0.00000316805106385740,
	         0.00000122903704923381,
	         0.00002334372474572637,
	         0.00111544038088797696,
	         0.00344717051723468982,
	         0.03198287045148788384,
	        -0.32705333652955399526,
	         0.40120442440953927615,
	        -0.00000000005184290387,
	        -0.00000000083355121068,
	        -0.00000000256167239813,
	         0.00000001455875381397,
	         0.00000013512178394703,
	         0.00000029898826810905,
	        -0.00000358107254612779,
	        -0.00002445260816156224,
	        -0.00004417127762011821,
	         0.00112859455189416567,
	         0.00804694454346728197,
	         0.04919775747126691372,
	        -0.24818372840948854178,
	         0.11071780856646862561,
	         0.00000000030279161576,
	         0.00000000160742167357,
	        -0.00000000405596009522,
	        -0.00000005089259920266,
	        -0.00000002029496209743,
	         0.00000135130272477793,
	         0.00000391430041115376,
	        -0.00002871505678061895,
	        -0.00023052137536922035,
	         0.00045534656385400747,
	         0.01153444585593040046,
	         0.07924014651650476036,
	        -0.12152192626936502982,
	        -0.07916438300260539592,
	        -0.00000000050919149580,
	        -0.00000000115274986907,
	         0.00000001237873512188,
	         0.00000002937383549209,
	        -0.00000030621450667958,
	        -0.00000077409414949954,
	         0.00000816753874325579,
	         0.00002412433382517375,
	        -0.00026061217606063700,
	        -0.00091000087658659231,
	         0.01068093850598380797,
	         0.11395654404408482305,
	         0.07209569059984075595,
	        -0.10971041451764266684,
	         0.00000000040119897187,
	        -0.00000000013224526679,
	        -0.00000001002723190355,
	         0.00000002569249716518,
	         0.00000020336011868466,
	        -0.00000118097682726060,
	        -0.00000300660303810663,
	         0.00004402212897757763,
	        -0.00001462405876235375,
	        -0.00164873795596001280,
	         0.00513927520866443706,
	         0.13843580753590579416,
	         0.32730190978254056722,
	         0.08588339725978624973,
	        -0.00000000015413428348,
	         0.00000000064905779353,
	         0.00000000160702811151,
	        -0.00000002655645793815,
	         0.00000007619544277956,
	         0.00000047604380765353,
	        -0.00000490748870866195,
	         0.00000821513040821212,
	         0.00014804944070262948,
	        -0.00122152255762163238,
	        -0.00087425289205498532,
	         0.14438703699657968310,
	         0.61315889733595543766,
	         0.55513708159976477557,
	         0.00000000001049740243,
	        -0.00000000025832017855,
	         0.00000000139591845075,
	        -0.00000000021177278325,
	        -0.00000005082950464905,
	         0.00000037801785193343,
	        -0.00000073982266659145,
	        -0.00001088918441519888,
	         0.00012491810452478905,
	        -0.00049171790705139895,
	        -0.00425707089448266460,
	         0.13595080378472757216,
	         0.89518356003149514744,
	         1.31073912535196238583
	    };

	    // c[65] - third set of coefficients
	    final double c[] =
	    {
	         0.0000000116333640008,
	        -0.0000000833156123568,
	         0.0000003832869977018,
	        -0.0000015814047847688,
	         0.0000065010672324100,
	        -0.0000274514060128677,
	         0.0001209015360925566,
	        -0.0005666333178228163,
	         0.0029294103665559733,
	        -0.0180340086069185819,
	         0.1651788780501166204,
	         1.1031566406452431944,
	         1.2009736023470742248,
	         0.0000000013842760642,
	        -0.0000000069417501176,
	         0.0000000342976459827,
	        -0.0000001785317236779,
	         0.0000009525947257118,
	        -0.0000052483007560905,
	         0.0000302364659535708,
	        -0.0001858396115473822,
	         0.0012634378559425382,
	        -0.0102594702201954322,
	         0.1243625515195050218,
	         1.3888709263595291174,
	         2.4537365708424422209,
	         0.0000000001298977078,
	        -0.0000000008029574890,
	         0.0000000049454846150,
	        -0.0000000317563534834,
	         0.0000002092136698089,
	        -0.0000014252023958462,
	         0.0000101652510114008,
	        -0.0000774550502862323,
	         0.0006537746948291078,
	        -0.0066014912535521830,
	         0.0996711934948138193,
	         1.6110931485817511402,
	         3.9578139676187162939,
	         0.0000000000183995642,
	        -0.0000000001353537034,
	         0.0000000009984676809,
	        -0.0000000076346363974,
	         0.0000000599311464148,
	        -0.0000004868554120177,
	         0.0000041441957716669,
	        -0.0000377160856623282,
	         0.0003805693126824884,
	        -0.0045979851178130194,
	         0.0831422678749791178,
	         1.7929113303999329439,
	         5.6625620598571415285,
	         0.0000000000034858778,
	        -0.0000000000297587783,
	         0.0000000002557677575,
	        -0.0000000022705728282,
	         0.0000000207024992450,
	        -0.0000001954426390917,
	         0.0000019343161886722,
	        -0.0000204790249102570,
	         0.0002405181940241215,
	        -0.0033842087561074799,
	         0.0713079483483518997,
	         1.9467574842460867884,
	         7.5343642367587329552
	    };

	    // d[7] - fourth set of coefficients
	    final double d[] =
	    {
	        -0.00163312359200500807,
	         0.00083644533703385956,
	        -0.00059518947575728181,
	         0.00079365057505415415,
	        -0.00277777777735463043,
	         0.08333333333333309869,
	         0.91893853320467274178
	    };

	    int k;             // Index of first referenced coefficient
	    double t;           // Temporary variable
	    double v;           // Another temporary variable
	    double w;           // Local copy of x
	    double y;           // Returned value

	    w = x;

	    if ( w >= r8 )
	    {
	        // 8.0 <= x < infinity
	        v = r1 / w;
	        t = v * v;
	        y = (((((d[0] * t + d[1]) * t + d[2]) * t + d[3]) * t +
	            d[4]) * t + d[5]) * v + d[6];
	        y = y + ((w - r0p5) * Math.log( w ) - w);
	    }
	    else if ( w >= r3p5 )
	    {
	        // 3.5 <= x < 8.0
	        k = (int)( Math.floor(w) ) - 3;
	        t = w - (k + r3p5);
	        k = k * 13;
	        y = (((((((((((c[k] * t + c[k + 1]) * t + c[k + 2]) * t +
	            c[k + 3]) * t + c[k + 4]) * t + c[k + 5]) * t +
	            c[k + 6]) * t + c[k + 7]) * t + c[k + 8]) * t +
	            c[k + 9]) * t + c[k + 10]) * t + c[k + 11]) * t + c[k + 12];
	    }
	    else if ( (w == r1) || (w == r2) )
	    {
	        // Only if w = 1 or w = 2 exactly up to machine precision
	        y = r0;
	    }
	    else if ( w > r0p5 )
	    {
	        // 0.5 < x < 3.5
	        t = w - r4p5 / (w + r0p5);
	        k = (int)( Math.floor( t + r4 ) );
	        t = t - (k - r3p5);
	        k = k * 14;
	        y = ((((((((((((b[k] * t + b[k + 1]) * t + b[k + 2]) * t +
	            b[k + 3]) * t + b[k + 4]) * t + b[k + 5]) * t +
	            b[k + 6]) * t + b[k + 7]) * t + b[k + 8]) * t +
	            b[k + 9]) * t + b[k + 10]) * t + b[k + 11]) * t +
	            b[k + 12]) * t + b[k + 13];
	    }
	    else if ( w == r0p5 )
	    {
	        // Only if w = 0.5 exactly up to machine precision
	        y = Math.log( PI ) / r2;
	    }
	    else
	    {
	        // 0 < x < 0.5
	        if ( w < r0p25)
	        {
	            k = 0;
	        }
	        else
	        {
	            k = 11;
	        }

	        y = ((((((((((a[k] * w + a[k + 1]) * w + a[k + 2]) * w +
	            a[k + 3]) * w + a[k + 4]) * w + a[k + 5]) * w +
	            a[k + 6]) * w + a[k + 7]) * w + a[k + 8]) * w +
	            a[k + 9]) * w + a[k + 10]) * w;

	        if ( y > r0 )
	        {
	            y = -Math.log( y );
	        }
	        else
	        {
	            y = -1;
	        }
	    }

	    return y;
	}
	
	// Cumulative standard normal
	static double prbnrm( double z )
	{
		final double half = 0.5;
	    final double r0 = 0.0;
	    final double r1 = 1.0;
	    final double sqrt2 = Math.sqrt( 2.0 );

	    double result;      // Returned value

	    if ( z > r0 )
	    {
	       result = half * (derf( z/sqrt2 ) + r1);
	    }
	    else if ( z < r0 )
	    {
	       result = half * derfc( -z/sqrt2 );
	    }
	    else
	    {
	       result = half;
	    }

	    return result;
	}
	
	static double derf( double x )
	{
		final double r0 = 0.0;
	    final double r1 = 1.0;

	    // a[65] - coefficients for 0.0 <= x < 2.2
	    final double a[] =
	    {
	         0.00000000005958930743,
	        -0.00000000113739022964,
	         0.00000001466005199839,
	        -0.00000016350354461960,
	         0.00000164610044809620,
	        -0.00001492559551950604,
	         0.00012055331122299265,
	        -0.00085483269811296660,
	         0.00522397762482322257,
	        -0.02686617064507733420,
	         0.11283791670954881569,
	        -0.37612638903183748117,
	         1.12837916709551257377,
	         0.00000000002372510631,
	        -0.00000000045493253732,
	         0.00000000590362766598,
	        -0.00000006642090827576,
	         0.00000067595634268133,
	        -0.00000621188515924000,
	         0.00005103883009709690,
	        -0.00037015410692956173,
	         0.00233307631218880978,
	        -0.01254988477182192210,
	         0.05657061146827041994,
	        -0.21379664776456006580,
	         0.84270079294971486929,
	         0.00000000000949905026,
	        -0.00000000018310229805,
	         0.00000000239463074000,
	        -0.00000002721444369609,
	         0.00000028045522331686,
	        -0.00000261830022482897,
	         0.00002195455056768781,
	        -0.00016358986921372656,
	         0.00107052153564110318,
	        -0.00608284718113590151,
	         0.02986978465246258244,
	        -0.13055593046562267625,
	         0.67493323603965504676,
	         0.00000000000382722073,
	        -0.00000000007421598602,
	         0.00000000097930574080,
	        -0.00000001126008898854,
	         0.00000011775134830784,
	        -0.00000111992758382650,
	         0.00000962023443095201,
	        -0.00007404402135070773,
	         0.00050689993654144881,
	        -0.00307553051439272889,
	         0.01668977892553165586,
	        -0.08548534594781312114,
	         0.56909076642393639985,
	         0.00000000000155296588,
	        -0.00000000003032205868,
	         0.00000000040424830707,
	        -0.00000000471135111493,
	         0.00000005011915876293,
	        -0.00000048722516178974,
	         0.00000430683284629395,
	        -0.00003445026145385764,
	         0.00024879276133931664,
	        -0.00162940941748079288,
	         0.00988786373932350462,
	        -0.05962426839442303805,
	         0.49766113250947636708
	    };

	    // b[65] - coefficients for 2.2 <= x < 6.9
	    final double b[] =
	    {
	        -0.00000000029734388465,
	         0.00000000269776334046,
	        -0.00000000640788827665,
	        -0.00000001667820132100,
	        -0.00000021854388148686,
	         0.00000266246030457984,
	         0.00001612722157047886,
	        -0.00025616361025506629,
	         0.00015380842432375365,
	         0.00815533022524927908,
	        -0.01402283663896319337,
	        -0.19746892495383021487,
	         0.71511720328842845913,
	        -0.00000000001951073787,
	        -0.00000000032302692214,
	         0.00000000522461866919,
	         0.00000000342940918551,
	        -0.00000035772874310272,
	         0.00000019999935792654,
	         0.00002687044575042908,
	        -0.00011843240273775776,
	        -0.00080991728956032271,
	         0.00661062970502241174,
	         0.00909530922354827295,
	        -0.20160072778491013140,
	         0.51169696718727644908,
	         0.00000000003147682272,
	        -0.00000000048465972408,
	         0.00000000063675740242,
	         0.00000003377623323271,
	        -0.00000015451139637086,
	        -0.00000203340624738438,
	         0.00001947204525295057,
	         0.00002854147231653228,
	        -0.00101565063152200272,
	         0.00271187003520095655,
	         0.02328095035422810727,
	        -0.16725021123116877197,
	         0.32490054966649436974,
	         0.00000000002319363370,
	        -0.00000000006303206648,
	        -0.00000000264888267434,
	         0.00000002050708040581,
	         0.00000011371857327578,
	        -0.00000211211337219663,
	         0.00000368797328322935,
	         0.00009823686253424796,
	        -0.00065860243990455368,
	        -0.00075285814895230877,
	         0.02585434424202960464,
	        -0.11637092784486193258,
	         0.18267336775296612024,
	        -0.00000000000367789363,
	         0.00000000020876046746,
	        -0.00000000193319027226,
	        -0.00000000435953392472,
	         0.00000018006992266137,
	        -0.00000078441223763969,
	        -0.00000675407647949153,
	         0.00008428418334440096,
	        -0.00017604388937031815,
	        -0.00239729611435071610,
	         0.02064129023876022970,
	        -0.06905562880005864105,
	         0.09084526782065478489
	    };

	    int k;             // Index of first referenced coefficient
	    double t;           // Temporary variable
	    double w;           // Absolute value of x
	    double y;           // Returned value

	    w = Math.abs( x );
	    if ( w <= r0 )
	    {
	        y = r0;
	    }
	    else if ( w < 2.2 )
	    {
	        t = w * w;
	        k = (int)( Math.floor(t) );
	        t = t - k;
	        k = 13 * k;
	        y = ((((((((((((a[k] * t + a[k + 1]) * t + a[k + 2]) * t +
	            a[k + 3]) * t + a[k + 4]) * t + a[k + 5]) * t +
	            a[k + 6]) * t + a[k + 7]) * t + a[k + 8]) * t +
	            a[k + 9]) * t + a[k + 10]) * t + a[k + 11]) * t +
	            a[k + 12]) * w;
	    }
	    else if ( w < 6.9 )
	    {
	        k = (int)( Math.floor(w) );
	        t = w - k;
	        k = 13 * (k - 2);
	        y = (((((((((((b[k] * t + b[k + 1]) * t + b[k + 2]) * t +
	            b[k + 3]) * t + b[k + 4]) * t + b[k + 5]) * t +
	            b[k + 6]) * t + b[k + 7]) * t + b[k + 8]) * t +
	            b[k + 9]) * t + b[k + 10]) * t + b[k + 11]) * t + b[k + 12];

	        // The next 3 lines raise (the above) y to the power 8
	        y = y * y;
	        y = y * y;
	        y = y * y;
	        y = r1 - y * y;
	    }
	    else
	    {

	        // 6.9 <= |x| < infinity
	        y = r1;
	    }

	    if ( x < r0 )
	    {
	        y = -y;
	    }

	    return y;
	}
	
	static double derfc(double x)
	{
		 // p0 to p22 are coefficients of the polynomial
	    final double p0 = 2.75374741597376782e-1;
	    final double p1 = 4.90165080585318424e-1;
	    final double p2 = 7.74368199119538609e-1;
	    final double p3 = 1.07925515155856677;
	    final double p4 = 1.31314653831023098;
	    final double p5 = 1.37040217682338167;
	    final double p6 = 1.18902982909273333;
	    final double p7 = 8.05276408752910567e-1;
	    final double p8 = 3.57524274449531043e-1;
	    final double p9 = 1.66207924969367356e-2;
	    final double p10 = -1.19463959964325415e-1;
	    final double p11 = -8.38864557023001992e-2;
	    final double p12 =  2.49367200053503304e-3;
	    final double p13 =  3.90976845588484035e-2;
	    final double p14 =  1.61315329733252248e-2;
	    final double p15 = -1.33823644533460069e-2;
	    final double p16 = -1.27223813782122755e-2;
	    final double p17 =  3.83335126264887303e-3;
	    final double p18 =  7.73672528313526668e-3;
	    final double p19 = -8.70779635317295828e-4;
	    final double p20 = -3.96385097360513500e-3;
	    final double p21 =  1.19314022838340944e-4;
	    final double p22 =  1.27109764952614092e-3;

	    final double pa = 3.97886080735226000;
	    final double r0 = 0.0;
	    final double r1 = 1.0;
	    final double r2 = 2.0;
	    final double rhalf = 0.5;
	    
	    final double minExp = -708.0;

	    double exparg;    // Argument for exp() function
	    double t;         // Temporary variable
	    double u;         // Temporary variable
	    double w;         // Absolute value of x
	    double y;         // Returned value

	    w = Math.abs( x );
	    if ( w <= r0 )
	    {
	        y = r1;
	    }
	    else
	    {
	        t = pa / (pa + w);
	        u = t - rhalf;
	        y = (((((((((p22 * u + p21) * u + p20) * u + p19) * u +
	            p18) * u + p17) * u + p16) * u + p15) * u + p14) * u +
	            p13) * u + p12;
	        y = ((((((((((((y * u + p11) * u + p10) * u + p9) * u +
	            p8) * u + p7) * u + p6) * u + p5) * u + p4) * u +
	            p3) * u + p2) * u + p1) * u + p0) * t;

	        // Compute y * exp(-w*w)
	        if ( y > r0 )
	        {
	            exparg = Math.log( y ) - w * w;      // won't overflow exp()
	            if ( exparg >= minExp )
	            {
	                y = Math.exp( exparg );
	            }
	            else
	            {
	                y = r0;
	            }
	        }
	        else
	        {
	            y = r0;
	        }
	        if ( x < r0 )
	        {
	            y = r2 - y;
	        }
	    }

	  return y;
	}
	
}
