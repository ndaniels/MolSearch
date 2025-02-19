package edu.mit.csail.ammolite.compression;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.PubchemID;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.SimpleWeightedEdge;
import edu.ucla.sspace.graph.isomorphism.AbstractIsomorphismTester;

public class LabeledWeightedMolStruct extends AtomContainer implements IMolStruct {
    
    /**
     * 
     */
    protected int fingerprint;
    protected Set<PubchemID> mol_ids;
    protected LabeledWeightedGraph graph;
    protected HashMap<IAtom,Integer> atomsToNodes = new HashMap<IAtom, Integer>();
    protected HashMap<Integer, IAtom> nodesToAtoms = new HashMap<Integer, IAtom>();
    protected HashMap<IBond,Edge> bondsToEdges = new HashMap<IBond, Edge>();
    protected HashMap<Edge, IBond> edgesToBonds = new HashMap<Edge, IBond>();

    public LabeledWeightedMolStruct() {}
    
    public LabeledWeightedMolStruct(IAtomContainer base){
        super( new AtomContainer(AtomContainerManipulator.removeHydrogens(base)) );
        mol_ids = new HashSet<PubchemID>();
        
        Iterator<IAtom> atoms = this.atoms().iterator();
        while( atoms.hasNext() ){
            IAtom atom = atoms.next();
            String atomType = atom.getSymbol();
            if(!atomType.equals("C")){
                atom.setSymbol("N");
            }
            atom.setAtomTypeName("S");
        }
        Iterator<IBond> bonds = this.bonds().iterator();
        while( bonds.hasNext() ){
            IBond bond = bonds.next();
            if(bond.getOrder() == IBond.Order.SINGLE){
                bond.setOrder(IBond.Order.SINGLE);
            } else {
                bond.setOrder(IBond.Order.DOUBLE);
            }
        }
        
        makeGraph();

        setFingerprint();
        
        PubchemID pubID = MolUtils.getPubID(base);
        this.mol_ids.add( pubID);
        this.setProperty("PUBCHEM_COMPOUND_CID", pubID.toString());
    }
    
    @Override
    public void removeAtom(IAtom atom){
        graph.remove( atomsToNodes.get(atom));
        super.removeAtom(atom);
    }
    
    @Override
    public void removeBond(IBond bond){
        graph.remove(bondsToEdges.get(bond));
        super.removeBond(bond);
    }
    
    @Override
    public void addAtom(IAtom atom){
        int node = this.atomCount;
        nodesToAtoms.put(node, atom);
        atomsToNodes.put(atom, node);
        graph.add(node);
        super.addAtom(atom);
    }
    
    protected void setFingerprint(){
        int h = this.atomCount;
        h += 1000 * 1000 * this.bondCount;
        
        int[] degree = new int[this.atoms.length];
        int i=0;
        for(IAtom atom: this.atoms){
            degree[i] = this.getConnectedAtomsCount(atom);
            i++;
        }
        Arrays.sort(degree);
        int bound = degree.length;
        int maxBound = 6;
        if(bound > maxBound){
            bound = maxBound;// Max int32 is a 10 digit number so this is very unlikely to overflow.
        }
        for(int j=0; j<bound; j++){
            h += Math.pow(10, j) * degree[ degree.length - 1 - j];
        }
        
        this.fingerprint = h;
        this.fingerprint = 0;
    }
    
    protected void makeGraph(){
        graph = new LabeledWeightedGraph();
        for(int i=0; i<this.getAtomCount(); i++){
            String atomTypeName = this.getAtom(i).getSymbol();
            String label = "N";
            if(atomTypeName.equals("C")){
                label = "C";
            }
            graph.add(i, label);
            atomsToNodes.put(this.getAtom(i), i);
            nodesToAtoms.put(i, this.getAtom(i));
            for(int j=0; j<i; j++){
                IBond bond = this.getBond(this.getAtom(i), this.getAtom(j));
                if(  bond != null){
                    SimpleWeightedEdge newEdge;
                    if( bond.getOrder() == IBond.Order.SINGLE){
                        newEdge = new SimpleWeightedEdge(i,j, 1);
                    } else {
                        newEdge = new SimpleWeightedEdge(i,j, 2);
                    }
                    bondsToEdges.put(bond, newEdge);
                    edgesToBonds.put(newEdge, bond);
                    graph.add( newEdge);
                }
            }
        }
    }

    @Override
    public boolean isIsomorphic(IAtomContainer struct, AbstractIsomorphismTester tester) {
        if(tester instanceof LabeledVF2IsomorphismTester){
            if(struct instanceof BinaryLabeledMolStruct){
                return ((LabeledVF2IsomorphismTester) tester).areIsomorphic(this.getGraph(), ((BinaryLabeledMolStruct) struct).getGraph());
            }
            return false;
        }
        return false;
    }

    @Override
    public void addID(PubchemID id) {
        this.mol_ids.add(id);
        
    }

    @Override
    public Set<PubchemID> getIDNums() {
        return mol_ids;
    }

    @Override
    public Graph<? extends Edge> getGraph() {
        return this.graph;
    }

    @Override
    public int fingerprint() {
        return this.fingerprint;
    }

    @Override
    public int nonCarbons() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int carbons() {
        // TODO Auto-generated method stub
        return 0;
    }

}
