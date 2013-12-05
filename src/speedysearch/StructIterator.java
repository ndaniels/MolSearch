package speedysearch;

import java.util.Iterator;

public class StructIterator implements Iterator<MoleculeStruct> {
	Iterator<MoleculeStruct> currentListIterator;
	Iterator<Object> keyIterator;
	KeyListMap<Object, MoleculeStruct> in;
	
	public StructIterator(KeyListMap<Object, MoleculeStruct> _in){
		in = _in;
		keyIterator = in.keySet().iterator();
		if(keyIterator.hasNext()){
			currentListIterator = in.get(keyIterator.next()).iterator();
		}
	}

	@Override
	public boolean hasNext() {
		if( currentListIterator.hasNext() ){
			return true;
		} else {
			if( keyIterator.hasNext()){
				currentListIterator = in.get(keyIterator.next()).iterator();
				return this.hasNext();
				
			} else {
				return false;
			}
		}
	}

	@Override
	public MoleculeStruct next() {
		if( this.hasNext()){
			return currentListIterator.next();
		} else{
			return null;
		}
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

}
