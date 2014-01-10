package fmcs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;


import speedysearch.IteratingSDFReader;

public class FMCS {
	
	public static void doFMCS(String input, String output) throws IOException, CDKException{

		IteratingSDFReader molecules =new IteratingSDFReader( input);
		
		IAtomContainer a = null;
		IAtomContainer b = null;
		
		a = molecules.next();
		Logger.log("molecule one: "+a.getID(), 2);
		
		
		b = molecules.next();
		Logger.log("molecule two: "+b.getID(), 2);
		
		molecules.close();
		a = new AtomContainer(AtomContainerManipulator.removeHydrogens(a));
		b = new AtomContainer(AtomContainerManipulator.removeHydrogens(b));
		MCS myMCS = new MCS(a,b);
		myMCS.calculate();
		SDFWriter sdfwriter = new SDFWriter(new BufferedWriter( new FileWriter( output + ".sdf" )));
		for(IAtomContainer overlap: myMCS.getSolutions()){
			sdfwriter.write(overlap);
		}
		sdfwriter.close();
		
	}
}
