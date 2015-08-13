/**
 *  All query operators that return score lists are subclasses of the
 *  QryopSl class.  This class has two main purposes.  First, it
 *  allows query operators to easily recognize any nested query
 *  operator that returns a score list (e.g., #AND (a #OR (b c)).
 *  Second, it is a place to store data structures and methods that are
 *  common to all query operators that return score lists.
 *  
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;


//import Qryop.DaaTPtr;

public abstract class QryopSl extends Qryop {

  /**
   *  Use the specified retrieval model to evaluate the query arguments.
   *  Define and return DaaT pointers that the query operator can use.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return void
   *  @throws IOException
   */
  public void allocDaaTPtrs (RetrievalModel r) throws IOException {
	  
   for (int i=0; i<this.args.size(); i++) {

      //  If this argument doesn't return ScoreLists, wrap it
      //  in a #SCORE operator.
	  DaaTPtr ptri = new DaaTPtr ();
      if (! QryopSl.class.isInstance (this.args.get(i))){
	    this.args.set(i, new QryopSlScore(this.args.get(i)));
	   // ptri.term = this.args.get(i).term;
      }
      
      ptri.invList = null;
      QryResult result= this.args.get(i).evaluate(r);
      ptri.scoreList = result.docScores;
      ptri.nextDoc = 0;
      //Add by me
      ptri.posIdx = 0;
      ptri.term = result.term;
      if(QryEval.wgt_DEBUG)System.out.println("allocDaatPtr term:"+ ptri.term);
      this.daatPtrs.add (ptri);
    }
  }
  
  /* Separate the args, even args are the weights, */
  public ArrayList<Integer> createNewPtrs() throws IOException{
	  ArrayList<DaaTPtr> newPtrs = new ArrayList<DaaTPtr>();
	  ArrayList<Integer> stopList = new ArrayList<Integer>(); 
	  if(QryEval.wgt_DEBUG) System.out.println("createNewPtrs: ptrs size:" + this.daatPtrs.size());
	  
	  // i points to the weight,i+1 points to term
	  for (int i=0; i<this.daatPtrs.size();) {
		  if(QryEval.wgt_DEBUG) System.out.println("i:"+i);
		  DaaTPtr ptri = this.daatPtrs.get(i); // weight
		  DaaTPtr ptrNext = this.daatPtrs.get(i+1); //term
		  //if(QryEval.wgt_DEBUG) System.out.println("createNewPtrs: ptri.term:" + ptri.term);
		  //if(QryEval.wgt_DEBUG) System.out.println("createNewPtrs: term:" + ptrNext.term);
		  //Current ptr is the weight. Next ptr is the real arg
		  ptrNext.argWgt = QryEval.getDigitWgt(ptri.term );
		  // Set weight for args
		  this.args.get(i+1).wgt = QryEval.getDigitWgt(ptri.term );
		  
  		// Stop words need be deleted 
		if(ptrNext.term != null){
			String[] tokenProcArr = QryEval.tokenizeQuery(ptrNext.term);
			//Only add when term is not stop word
			if (tokenProcArr.length != 0)  {newPtrs.add( ptrNext) ;}
			else{
				stopList.add(i+1);
				System.err.println("stopwords:"+ ptrNext.term);
				}
		}else{
			newPtrs.add( ptrNext);
		}
		
		i+=2; // i points to next weight
	  }
	  this.daatPtrs = newPtrs;
	  if(QryEval.wgt_DEBUG)this.printPtrs();
	  return stopList;
  }
  
  public void removeEvenArgs(ArrayList<Integer> stopL){
	  ArrayList<Qryop> newArgs = new ArrayList<Qryop>();
	  for(int i=0; i<this.args.size();i++){
		  // If it's even position or i is in stopl(means the term is stop word)
		  if (stopL.contains(i)) System.err.println("rmvEvenArgs:there is a stop word, arg index:"+ i);
		  if(i%2 == 0 || stopL.contains(i)) continue;
		  if(QryEval.wgt_DEBUG) System.out.println("add arg(i):"+i);
		  newArgs.add(this.args.get(i));
	  }
	  this.args = newArgs;
	  if(QryEval.wgt_DEBUG)this.printArgs();
  }

  /*
   *  Calculate the default score for the specified document if it
   *  does not match the query operator.  This score is 0 for many
   *  retrieval models, but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public abstract double getDefaultScore (RetrievalModel r, long docid) throws IOException;
  
  public double getTotalWgt(){
		double w_total = 0.0;
		
		for(int j=0;j< this.args.size();j++){
			//DaaTPtr ptrj = this.daatPtrs.get(j);
			w_total += this.args.get(j).wgt;
			  if(QryEval.wand_DEBUG){
				  System.out.println("argj.wgt:"+ this.args.get(j).wgt);
			  }
		  }
		  if(QryEval.wand_DEBUG){
			  System.out.println("w_total:"+w_total);
		  }
		  return w_total;
  }
  
/*
  public double getTotalWgt(){
		double w_total = 0.0;
		
		for(int j=0;j< this.daatPtrs.size();j++){
			//DaaTPtr ptrj = this.daatPtrs.get(j);
			w_total += this.daatPtrs.get(j).argWgt;
			  if(QryEval.wand_DEBUG){
				  System.out.println("argj.wgt:"+ this.daatPtrs.get(j).argWgt);
			  }
		  }
		  if(QryEval.wand_DEBUG){
			  System.out.println("w_total:"+w_total);
		  }
		  return w_total;
  }
  */
  
  public void printScoreList(){
	  System.out.println("ScoreList:");
	  for(int j=0;j< this.daatPtrs.size();j++){
		  System.out.println( this.daatPtrs.get(j).scoreList.scores.size()) ;
	  }
  }
}
