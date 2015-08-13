/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlWAnd extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlWAnd(Qryop... q) {
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
	 //if(QryEval.process_DEBUG)System.out.println("#wand evaluate");
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
	  if(QryEval.process_DEBUG)System.out.println("------wand evaluateIndri begin------");
    
    // Create the ptrs array
    allocDaaTPtrs (r);
    // Separate the weight and terms .Create a new args list
    ArrayList<Integer> stopList = this.createNewPtrs();
    this.removeEvenArgs(stopList);
    
    RetrievalModelIndri IndriM = null;
	 if(r instanceof RetrievalModelIndri ){
		  IndriM = (RetrievalModelIndri)r;
	 }
    QryResult result = new QryResult ();
    double combScore = 1.0;
    double scoreDef = 1.0;
	double q_size = this.daatPtrs.size();
    double w_total = getTotalWgt();
    //printScoreList();

    while (true) {
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
      combScore = 1.0 ;
      int smallestDocid = getSmallestCurrentDocid ();
      
      //QryEval.printDbg("smallest docid="+smallestDocid);
      // Examine each list that contains the smallest Docid to compute score
      for (int i=0; i<this.daatPtrs.size(); i++) {
	    DaaTPtr ptri = this.daatPtrs.get(i);
	    // In this scorelist, no more doc to examine
			if(ptri.nextDoc >= ptri.scoreList.scores.size() || (ptri.scoreList.getDocid(ptri.nextDoc) != smallestDocid)){
				QryopSl pi= (QryopSl) this.args.get(i);
				combScore *= Math.pow(pi.getDefaultScore(r, smallestDocid), (double)this.daatPtrs.get(i).argWgt/w_total );
			}
			else{
				combScore *= Math.pow(this.daatPtrs.get(i).scoreList.getDocidScore(ptri.nextDoc),(double)this.daatPtrs.get(i).argWgt/w_total);
				ptri.nextDoc ++;

			  if(QryEval.wand_DEBUG){
				  //System.out.println("ptri.wgt/w_total:"+ ptri.argWgt/w_total);
			  }
			}
      }
     result.docScores.add (smallestDocid,combScore);
    }
    if(QryEval.wand_DEBUG){System.out.println("wand evaluateIndri end");}
    freeDaaTPtrs();
    if(QryEval.process_DEBUG)System.out.println("-------wand evaluateIndri end-----");
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
		double scoreDef = 1.0;
		Double totalWgt = getTotalWgt();
		double tmp;
    	int q_len = this.args.size();
    	/*
    	System.err.println("q_len:"+ q_len);
    	System.err.println("ptrs size:"+ this.daatPtrs.size());
    	System.err.println("Total wgt:"+ totalWgt);
    	*/
        for (int j=0; j< q_len; j++) {
        	QryopSl pj= (QryopSl) this.args.get(j);
			tmp = pj.getDefaultScore(r,docid);
			//scoreDef *= Math.pow(tmp, this.daatPtrs.get(j).argWgt/totalWgt);
			//System.err.println("arg wgt j:"+ this.args.get(j).wgt);
			scoreDef *= Math.pow(tmp, this.args.get(j).wgt/totalWgt);
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
  

  
}
