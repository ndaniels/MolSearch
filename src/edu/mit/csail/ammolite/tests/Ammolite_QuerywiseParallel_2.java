package edu.mit.csail.ammolite.tests;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.mit.csail.ammolite.compression.MolStruct;
import edu.mit.csail.ammolite.database.IStructDatabase;
import edu.mit.csail.ammolite.mcs.MCS;
import edu.mit.csail.ammolite.utils.CommandLineProgressBar;
import edu.mit.csail.ammolite.utils.MCSUtils;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.Pair;
import edu.mit.csail.ammolite.utils.ParallelUtils;
import edu.mit.csail.ammolite.utils.StructID;

public class Ammolite_QuerywiseParallel_2 implements Tester {
    private static final String NAME = "Ammolite_QuerywiseParallel_QUEUE";
    private static final int COARSE_QUEUE_SIZE = 250;
    private static final int FINE_QUEUE_SIZE = 250;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService service = ParallelUtils.buildNewExecutorService();
        
    public Ammolite_QuerywiseParallel_2() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void test(List<IAtomContainer> queries, IStructDatabase db,
            Iterator<IAtomContainer> targets, Iterator<MolStruct> sTargets,
            double thresh, double prob, String name, PrintStream out) {
        
        SearchResultDocumenter scribe = new SearchResultDocumenter( out);
        
        for(IAtomContainer query: queries){
            MolStruct cQuery = db.makeMoleculeStruct(query);
            Pair<SearchResult, Collection<StructID>> p = coarseSearch(cQuery, query, db.iterator(), db.numReps(), prob);
            
            SearchResult coarseResult = p.left();
            Collection<StructID> coarseHits = p.right();
            System.out.println("Writing coarse results...");
            scribe.documentSingleResult(coarseResult);
            // Make sure the JVM automatically garbage collects these bad boys
            p = null;
            coarseResult = null;
            
            SearchResult fineResult = fineSearch(query, coarseHits, db, thresh);
            scribe.documentSingleResult(fineResult);
            System.out.println("Writing fine results...");
            fineResult = null;
        }
        service.shutdown();

    }
    

    
    private SearchResult fineSearch(IAtomContainer query, Collection<StructID> coarseHits, IStructDatabase db, double thresh){
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString(), coarseHits.size()*12);
        SearchResult result = new SearchResult(query, getName());
        result.start();
        BlockingQueue<IAtomContainer> queue = new ArrayBlockingQueue<IAtomContainer>(FINE_QUEUE_SIZE,false);

        Thread producer = new Thread( new FineProducer(coarseHits, db, queue));
        producer.start();
        List<Thread> consumers = new ArrayList<Thread>(NUM_THREADS);
        for(int i=0; i<NUM_THREADS; i++){
            Thread t = new Thread( new FineConsumer(query, queue, result, bar, thresh));
            consumers.add(t);
            t.start();
        }
        try {
            producer.join();
            while(queue.size() > 0){
                Thread.sleep(1000);
            }
            for(Thread t: consumers){
                t.interrupt();
            }
            for(Thread t: consumers){
                t.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Fail from fine call");
            e.printStackTrace();
        }
        result.end();
        return result;
    }
    
    
    private Pair<SearchResult, Collection<StructID>> coarseSearch(MolStruct cQuery, IAtomContainer query, Iterator<MolStruct> sTargets, int numReps, double thresh){
        CommandLineProgressBar bar = new CommandLineProgressBar(MolUtils.getPubID(query).toString() + "_COARSE", numReps);
        SearchResult coarseResult = new SearchResult(cQuery, getName() + "_COARSE");
        coarseResult.start();
        BlockingQueue<MolStruct> queue = new ArrayBlockingQueue<MolStruct>(COARSE_QUEUE_SIZE,false);
        Collection<StructID> hits = Collections.synchronizedCollection(new HashSet<StructID>());
        Thread producer = new Thread( new CoarseProducer(sTargets, queue));
        producer.start();
        List<Thread> consumers = new ArrayList<Thread>(NUM_THREADS);
        for(int i=0; i<NUM_THREADS; i++){
            Thread t = new Thread( new CoarseConsumer(cQuery, queue, coarseResult, hits, bar, thresh));
            consumers.add(t);
            t.start();
        }
        try {
            producer.join();
            while(queue.size() > 0){
                if(Math.random() < 0.01){
                    System.out.println( "\n [Diagnostic] Queue Size:"+queue.size());
                }
                Thread.sleep(1000);
            }
            for(Thread t: consumers){
                t.interrupt();
            }
            for(Thread t: consumers){
                t.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Fail from coarse call");
            e.printStackTrace();
        }
        coarseResult.end();
        return new Pair<SearchResult, Collection<StructID>>(coarseResult, hits);
    }

    @Override
    public String getName() {
        return NAME;
    }
    
    private class CoarseProducer implements Runnable {
        BlockingQueue< MolStruct> queue;
        Iterator<MolStruct> targets;
        
        public CoarseProducer(Iterator<MolStruct> sTargets, BlockingQueue< MolStruct> queue){
              this.targets = sTargets;
            this.queue = queue;
        }

        @Override
        public void run() {
            MolStruct target;
             while(targets.hasNext()){
                target = targets.next();
                try {
                    queue.offer(target, 1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } 
            }
            return;
        } 
    }
    
    private class FineProducer implements Runnable {
        BlockingQueue< IAtomContainer> queue;
        Collection<StructID> hits;
        IStructDatabase db;
        
        public FineProducer(Collection<StructID> hits, IStructDatabase db, BlockingQueue< IAtomContainer> queue){
            this.hits = hits;
            this.queue = queue;
            this.db = db;
        }

        @Override
        public void run() {
            for(StructID hit: hits){
                for(IAtomContainer target: db.getMatchingMolecules(hit)){
                    try {
                        queue.offer(target, 1, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        } 
    }
    
    private class CoarseConsumer implements Runnable {
        BlockingQueue<MolStruct> queue;
        MolStruct query;
        SearchResult result;
        Collection<StructID> hits;
        CommandLineProgressBar bar;
        double threshold;
        
        public CoarseConsumer(MolStruct cQuery, BlockingQueue<MolStruct> queue, SearchResult result, Collection<StructID> hits, CommandLineProgressBar bar, double threshold){
            this.query = cQuery;
            this.queue = queue;
            this.result = result;
            this.threshold = threshold;
            this.hits = hits;
            this.bar = bar;
        }

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    MolStruct target = queue.poll(500, TimeUnit.MILLISECONDS);
                    
                    if(target != null){
                        int overlap = MCS.getSMSDOverlap(target, query);
                        if(MCSUtils.overlapCoeff(overlap, target, query) > threshold){
                            result.addMatch(new SearchMatch(query, target, overlap));
                            hits.add(MolUtils.getStructID(target));
                        } else {
                            result.addMiss(new SearchMiss(query, target, overlap));
                        }
                        bar.event();
                    }
                } catch (InterruptedException end) {
                    break;
                }
            }
            return;
            
        }
        
    }
    
    private class FineConsumer implements Runnable {
        BlockingQueue<IAtomContainer> queue;
        IAtomContainer query;
        SearchResult result;
        CommandLineProgressBar bar;
        double threshold;
        
        public FineConsumer(IAtomContainer query, BlockingQueue<IAtomContainer> queue, SearchResult result, CommandLineProgressBar bar, double threshold){
            this.query = query;
            this.queue = queue;
            this.result = result;
            this.threshold = threshold;
            this.bar = bar;
        }

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    IAtomContainer target = queue.poll(500, TimeUnit.MILLISECONDS);
                    if(target != null){
                        int overlap = MCS.getSMSDOverlap(target, query);
                        if(MCSUtils.overlapCoeff(overlap, target, query) > threshold){
                            result.addMatch(new SearchMatch(query, target, overlap));
                        } else {
                            result.addMiss(new SearchMiss(query, target, overlap));
                        }
                        bar.event();
                    }
                } catch (InterruptedException end) {
                    break;
                }
            }
            return;
            
        }
        
    }

}
