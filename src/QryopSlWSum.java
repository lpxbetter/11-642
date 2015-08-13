/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

//import Qryop.DaaTPtr;

public class QryopSlWSum extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlWSum(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param {q} q The query argument (query operator) to append.
   *  @return void
   *  @throws IOException
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluates the query operator, including any child operators and
   *  returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    //if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean )
      //{return (evaluateBoolean (r));}
    //else if(r instanceof RetrievalModelIndri ){
    if(r instanceof RetrievalModelIndri ){
		  //RetrievalModelIndri IndriM = (RetrievalModelBM25)r;
		  return evaluateIndri(r);
	 }
	 return null;     
  }
  
  public QryResult evaluateIndri (RetrievalModel r) throws IOException {

    if(QryEval.wand_DEBUG){System.out.println("wand evaluateIndri begin");}
    allocDaaTPtrs (r);
    /*
    RetrievalModelIndri IndriM = null;
	 if(r instanceof RetrievalModelIndri ){
		  IndriM = (RetrievalModelIndri)r;
	 }*/
    QryResult result = new QryResult ();
  
    double w_total = getTotalWgt();
    if(QryEval.wsum_DEBUG) System.out.println(" w_total: "+ w_total);
    //printScoreList();

    double combScore = 0.0;
    while (true) {
    	 combScore = 0.0 ; // every time, clear to zero
    	// Check whether all SocreLists are examined
		boolean isOver = true;
		for(int j=0;j< this.daatPtrs.size();j++)
		{
			DaaTPtr ptrj = this.daatPtrs.get(j);
			if (ptrj.nextDoc < ptrj.scoreList.scores.size())
				{
					isOver = false;
					break;
				}
		}
	  if(isOver) break;
     
      int smallestDocid = getSmallestCurrentDocid ();
      
      //QryEval.printDbg("smallest docid="+smallestDocid);
      // Examine each list that contains the smallest Docid to compute score
      for (int i=0; i<this.daatPtrs.size(); i++) {
	    DaaTPtr ptri = this.daatPtrs.get(i);
	    // In this scorelist, no more doc to examine
			if(ptri.nextDoc >= ptri.scoreList.scores.size() || (ptri.scoreList.getDocid(ptri.nextDoc) != smallestDocid)){
				QryopSl pi= (QryopSl) this.args.get(i);
				combScore += pi.getDefaultScore(r, smallestDocid) * 
						( (double)this.daatPtrs.get(i).argWgt/w_total ) ;
				 //if(QryEval.wsum_DEBUG) System.out.println("argi.wgt/w_total:"+ this./w_total);
				 //if(QryEval.wsum_DEBUG) System.out.println("combScore:"+ combScore);
			}
			else{
				combScore += this.daatPtrs.get(i).scoreList.getDocidScore(ptri.nextDoc) 
						* ( (double)this.daatPtrs.get(i).argWgt/w_total );
				ptri.nextDoc ++;
				
			  //if(QryEval.wsum_DEBUG) System.out.println("combScore: "+ combScore);
			  if(QryEval.wsum_DEBUG) System.out.println("argi.wgt/w_total:"
			  + this.daatPtrs.get(i).argWgt/w_total);
			  
			}
      }
      if(QryEval.wsum_DEBUG) System.out.println("combScore: "+ combScore);
     result.docScores.add (smallestDocid,combScore);
     
    }
    if(QryEval.wand_DEBUG){System.out.println("wand evaluateIndri end");}
    
    freeDaaTPtrs();
    return result;

  }
  

  /*
   *  Calculate the default score for the specified document if it
   *  does not match the query operator.  This score is 0 for many
   *  retrieval models, but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean
    		|| r instanceof RetrievalModelRankedBoolean)
      {return (0.0);}
    else if (r instanceof RetrievalModelIndri)
    {
		double scoreDef = 0.0;
		double tmp;
    	int q_len = this.args.size();
    	double w_total = this.getTotalWgt();
        for (int j=0; j< q_len; j++) {
        	QryopSl pj= (QryopSl) this.args.get(j);
			tmp = pj.getDefaultScore(r,docid);
			//scoreDef *= Math.pow(tmp, this.daatPtrs.get(j).argWgt/getTotalWgt());
			//scoreDef += tmp * ( this.daatPtrs.get(j).argWgt/getTotalWgt() ) ;
			scoreDef += tmp * ( this.daatPtrs.get(j).argWgt/w_total ) ;
        }
		return scoreDef;
    }

    return 0.0;
  }
  
  /**
   *  Return the smallest unexamined docid from the DaaTPtrs.
   *  @return The smallest internal document id.
   */
  public int getSmallestCurrentDocid () {

    int smallestDocid = Integer.MAX_VALUE;

    for (int i=0; i<this.daatPtrs.size(); i++) {
      DaaTPtr ptri = this.daatPtrs.get(i);
      if (ptri.scoreList.scores.size() == 0){continue;}
      if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
    	  //System.out.println("ptri.nextDoc:" + ptri.nextDoc);
    	  continue;
    	  }
      if (smallestDocid > ptri.scoreList.getDocid (ptri.nextDoc))
    	  smallestDocid = ptri.scoreList.getDocid (ptri.nextDoc);
      }

    return (smallestDocid);
  }

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#WAND( " + result + ")");
  }
  
  public void printScoreList(){
	  System.out.println("ScoreList:");
	  for(int j=0;j< this.daatPtrs.size();j++){
		  System.out.println( this.daatPtrs.get(j).scoreList.scores.size()) ;
	  }
  }
  /*
  public double getTotalWgt(){
		double w_total = 0.0;
		for(int j=0;j< this.args.size();j++){
			//DaaTPtr ptrj = this.daatPtrs.get(j);
			//w_total += ptrj.argWgt;
			w_total += this.args.get(j).wgt;
			  if(QryEval.wsum_DEBUG){
				  System.out.println("prtj.argWgt:"+ this.args.get(j).wgt);
				  System.out.println("prtj:"+ this.args.get(j).toString());
			  }
		  }
		  if(QryEval.wsum_DEBUG){
			  //System.out.println("w_total:"+w_total);
		  }
		  return w_total;
  }
  */
  
}
