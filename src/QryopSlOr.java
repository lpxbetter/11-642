/**
 *  This class implements the OR operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlOr extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlOr(Qryop... q) {
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

      return (evaluateBoolean (r));
  }

  /**
   *  Evaluates the query operator for boolean retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean (RetrievalModel r) throws IOException {
    allocDaaTPtrs (r);

    QryResult result = new QryResult ();
    double maxScore = 0.0;
    
    while (this.daatPtrs.size() > 0) {
      int nextDocid = getSmallestCurrentDocid ();
      //System.out.println("smallest docid="+nextDocid);
      maxScore = 0.0;
      // Examine each list that contains the smallest Docid to compute score
      for (int i=0; i<this.daatPtrs.size(); i++) {
	    DaaTPtr ptri = this.daatPtrs.get(i);
	    if (ptri.scoreList.scores.size() == 0){continue;}
	    // Find the MAX score
	    if (ptri.scoreList.getDocid (ptri.nextDoc) == nextDocid) {
		  if ( ptri.scoreList.getDocidScore (ptri.nextDoc) > maxScore){
			maxScore = ptri.scoreList.getDocidScore (ptri.nextDoc);
		}
	  	ptri.nextDoc ++;
	  	
	    }
      }
      // Add Docid and score to result 
      if (r instanceof RetrievalModelUnrankedBoolean )
    	  result.docScores.add (nextDocid,1.0);
      else if (r instanceof RetrievalModelRankedBoolean )
    	  result.docScores.add (nextDocid,maxScore);

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

    int nextDocid = Integer.MAX_VALUE;

    for (int i=0; i<this.daatPtrs.size(); i++) {
      DaaTPtr ptri = this.daatPtrs.get(i);
      if (ptri.scoreList.scores.size() == 0){continue;}
      if (nextDocid > ptri.scoreList.getDocid (ptri.nextDoc))
    	  nextDocid = ptri.scoreList.getDocid (ptri.nextDoc);
      }

    return (nextDocid);
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
