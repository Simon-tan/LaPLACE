package exp.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class SubSet
{
	List elements;
	
	public SubSet( List<Integer> elements ){
		if( elements != null && elements.size() > 0 ){
			this.elements = new ArrayList<Integer>( elements );			
		}
	}	
	
	public SubSet( Set<Integer> elements ){
		if( elements != null && elements.size() > 0 ){
			this.elements = new ArrayList<Integer>( elements );
		}
	}
	
	public void remove( Integer element )
	{
		if( element != null && elements != null ){
			if( elements.contains(element) ){
				elements.remove(element);
			}
		}
	}
	
	public void add( Integer element ){
		if( element != null && elements != null ){
			if( !elements.contains(element) )
				elements.add( element );
		}
	}
	
	// find all the subset with given size
	public void findSubSets( int size, Vector<Set<Integer>> result ){
		if( size < 1 )	return;
		
		if( result.size() > 0 )
			result.clear();
		
		if( size <= elements.size() ){
			List<Integer> subset = new ArrayList<Integer>( size );
			for( int i = 0; i <= elements.size() - size; i++ )
			{	
				getNextElem( result, subset, i, size );				
				subset.clear();
			}
		}
	}
	
	private int getNextElem( Vector<Set<Integer>> subSets, List<Integer> subSet, 
			int index, int size )
	{
		subSet.add( (Integer)elements.get(index) );
		size--;
		index++;
		if( 0 == size ){
			subSets.add( new HashSet<Integer>( subSet ) );						
		}
		else{
			for( int i = index; i <= elements.size() - size; i++ ){
				getNextElem( subSets, subSet, i, size );				
			}
		}
		subSet.remove( subSet.size() - 1 );
		
		return 1;
	}
	
	public static void main(String[] args){
		List<Integer> test = new ArrayList<Integer>();
		test.add(0);
		test.add(1);
		test.add(2);
		test.add(3);
		test.add(4);
		test.add(5);
		test.add(6);
		test.add(7);
		
		SubSet set = new SubSet(test);
		Vector<Set<Integer>> powerset = new Vector<Set<Integer>>();
		set.findSubSets(4,powerset);
		
		if( powerset.size() > 0 ){
			for( int i = 0; i < powerset.size(); i++ ){
				System.out.print("<" + (i+1) + ">\t");
				for( int j = 0; j < powerset.get(i).size(); j++ ){
					//System.out.print( powerset.get(i). + "\t");
				}
				System.out.print("\n");
			}
		}
		else
			System.out.println("no power set at all");
	}
}
