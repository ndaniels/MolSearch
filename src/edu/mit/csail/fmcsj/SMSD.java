package edu.mit.csail.fmcsj;

import java.util.List;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smsd.Isomorphism;
import org.openscience.cdk.smsd.interfaces.Algorithm;

public class SMSD extends AbstractMCS {
	Isomorphism comparison = null;

	public SMSD(IAtomContainer _compoundOne, IAtomContainer _compoundTwo) {
		super(_compoundOne, _compoundTwo);

	}

	@Override
	public long calculate() {
		long startTime = System.currentTimeMillis();

		comparison = new Isomorphism(Algorithm.DEFAULT, true);
        try {
			comparison.init(smallCompound, bigCompound, true, true);
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        long runTime = System.currentTimeMillis() - startTime;
		return runTime;
		
	}

	@Override
	public int size() {
		return comparison.getFirstAtomMapping().size();

	}

	@Override
	public List<IAtomContainer> getSolutions() {
		throw new UnsupportedOperationException();
	}

}

