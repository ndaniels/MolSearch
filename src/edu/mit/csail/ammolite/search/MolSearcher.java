package edu.mit.csail.ammolite.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.compression.MoleculeStruct;
import edu.mit.csail.ammolite.database.StructDatabase;

import edu.mit.csail.ammolite.mcs.MCS;

import edu.mit.csail.ammolite.utils.Logger;

public class MolSearcher implements IMolSearcher {

	private StructDatabase db;
	private boolean useTanimoto;
	
	public MolSearcher( StructDatabase _db, boolean _useTanimoto) {
		db = _db;
		useTanimoto = _useTanimoto;
		
	}
	@Override
	public MolTriple[] search(IAtomContainer query, double threshold, double probability) {
		query = new AtomContainer(AtomContainerManipulator.removeHydrogens(query));
		double repThreshold = db.convertThreshold(threshold, probability, useTanimoto);
		Logger.debug("Using rep threshold of "+repThreshold);
		String[] repMatches = thresholdRepMatches( query, repThreshold);
		Logger.debug("Found "+repMatches.length+" representative matches");
		List<MolTriple> molMatches = thresholdMoleculeMatches( query, repMatches, threshold);
		Logger.debug("Found "+molMatches.size()+" molecule matches");
		return molMatches.toArray(new MolTriple[0]);
	}

	private List<MolTriple> thresholdMoleculeMatches( IAtomContainer query, String[]  targetIDs, double threshold){
		IAtomContainer target;
		List<MolTriple> matches = new ArrayList<MolTriple>();
		
		for(String id: targetIDs){
			target = db.getMolecule(id);
			target = new AtomContainer(AtomContainerManipulator.removeHydrogens(target));
			MCS.beatsOverlapThreshold(query, target, threshold);
		}
		
		return matches;
	}
	
	/**
	 * Returns all representative matches above a certain given threshold
	 * 
	 * @param query
	 * @param reprThreshold
	 * @return
	 */
	private String[] thresholdRepMatches(IAtomContainer query, double threshold){
		MoleculeStruct sQuery = db.makeMoleculeStruct(query);

		List<MoleculeStruct> matches = new ArrayList<MoleculeStruct>();
		Iterator<IAtomContainer> structs = db.iterator();
		MoleculeStruct target;
		
		while( structs.hasNext() ){
			target = db.makeMoleculeStruct( structs.next());
			if( MCS.beatsOverlapThreshold(sQuery, target, threshold)){
				matches.add(target);
			} 
		}
		
		ArrayList<String> ids = new ArrayList<String>(3 * matches.size());
		
		for(int i=0;i<matches.size(); i++){
			for(String id: matches.get(i).getIDNums()){
				ids.add(id);
			}
		}
		
		return ids.toArray(new String[0]);
	}

}
