/**
 *  This class implements the OR operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlSum extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlSum(Qryop... q) {
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
	  if(QryEval.process_DEBUG)System.out.println("#wsum evaluate");
	  //if (r instanceof RetrievalModelBM25){
		  //return (evaluateBM25 (r));
	  //}
	 if(r instanceof RetrievalModelBM25 ){
		  //RetrievalModelBM25 bm25 = (RetrievalModelBM25)r;
		  return evaluateBM25(r);
	 }
	 return null;
  }

  /**
   *  Evaluates the query operator for boolean retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBM25 (RetrievalModel r) throws IOException {

    allocDaaTPtrs (r);
    //ArrayList<Integer> stopList = createNewPtrs(); // need to check
    //removeEvenArgs(stopList);
    
    RetrievalModelBM25 bm25 = null;
	 if(r instanceof RetrievalModelBM25 ){
		  bm25 = (RetrievalModelBM25)r;
		  //evaluateBM25(bm25);
	 }
    QryResult result = new QryResult ();
    double combScore = 0.0;
    //double QTFt = 0.5;
    double QTFt = 1;
    double USERw = (double)(bm25.k3 + 1)*QTFt/ (double)(bm25.k3 + QTFt);

    while (this.daatPtrs.size() > 0) {
      int smallestDocid = getSmallestCurrentDocid ();
      //QryEval.printDbg("smallest docid="+smallestDocid);
      //System.out.println("smallest docid="+smallestDocid);
      combScore = 0.0;
      // Examine each list that contains the smallest Docid to compute score
      for (int i=0; i<this.daatPtrs.size(); i++) {
	    DaaTPtr ptri = this.daatPtrs.get(i);
	    // In this scorelist, no more doc to examine
	    if (ptri.scoreList.scores.size() == 0){continue;}
	    // If the term occurs in this doc,add its score to the doc's score
	    // Otherwise, it is 0
	    if (ptri.scoreList.getDocid (ptri.nextDoc) == smallestDocid) {
			combScore += ptri.scoreList.getDocidScore (ptri.nextDoc) * USERw;
			ptri.nextDoc ++;
	    }
	    
      }
      // Add Docid and score to result
     // if (r instanceof RetrievalModelBM25 )
     //QryEval.printDbg("smallestDocid, combScore:"+smallestDocid + "," + combScore);
     result.docScores.add (smallestDocid,combScore);

      //  If a DaatPtr has reached the end of its list, remove it.
      //  The loop is backwards so that removing an arg does not
      //  interfere with iteration.

      for (int i=this.daatPtrs.size()-1; i>=0; i--) {
    	  DaaTPtr ptri = this.daatPtrs.get(i);

    	  if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
    		  //System.out.println("The end,remove:"+i);
    		  this.daatPtrs.remove (i);
    	  }
      }
    }

    freeDaaTPtrs();
    return result;
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
      if (smallestDocid > ptri.scoreList.getDocid (ptri.nextDoc))
    	  smallestDocid = ptri.scoreList.getDocid (ptri.nextDoc);
      }

    return (smallestDocid);
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

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);

    return 0.0;
  }

  /*
   *  Return a string version of this query operator.
   *  @return The string version of this query operator.
   */
  public String toString(){

    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#AND( " + result + ")");
  }
}
