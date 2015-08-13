/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

//import Qryop.DaaTPtr;

public class QryopSlAnd extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlAnd(Qryop... q) {
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
	 
    if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean )
      {return (evaluateBoolean (r));}
    else if(r instanceof RetrievalModelIndri ){
		  //RetrievalModelIndri IndriM = (RetrievalModelBM25)r;
		  return evaluateIndri(r);
	 }
	 return null;     
  }
  
  public QryResult evaluateIndri (RetrievalModel r) throws IOException {
	  if(QryEval.process_DEBUG)System.out.println("----#and evaluateIndri begin----");
	  if(QryEval.and_DEBUG)printArgs();
    allocDaaTPtrs (r);
    RetrievalModelIndri IndriM = null;
	 if(r instanceof RetrievalModelIndri ){
		  IndriM = (RetrievalModelIndri)r;
	 }
    QryResult result = new QryResult ();
    double combScore = 1.0;
    double scoreDef = 1.0;
	double q_size = this.daatPtrs.size();
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
	    //if (ptri.scoreList.scores.size() == 0){continue;}

	    // If the term occurs in this doc,multiply its score to the doc's score
	    //if ((ptri.nextDoc < ptri.scoreList.scores.size()) && (ptri.scoreList.getDocid (ptri.nextDoc) == smallestDocid) ) {
			//combScore *= Math.pow(ptri.scoreList.getDocidScore (ptri.nextDoc) , 1/this.args.size() );

			// Doc don't contain the term,get the default score of this doc
			if(ptri.nextDoc >= ptri.scoreList.scores.size() || (ptri.scoreList.getDocid(ptri.nextDoc) != smallestDocid)){
				QryopSl pi= (QryopSl) this.args.get(i);
				combScore *= Math.pow(pi.getDefaultScore(r, smallestDocid),1.0/(double)this.daatPtrs.size());
				//System.out.println( pi.getDefaultScore(r, smallestDocid) );
			}
			else{
				combScore *= Math.pow(this.daatPtrs.get(i).scoreList.getDocidScore(ptri.nextDoc), 1.0/q_size);
				//System.out.println(this.daatPtrs.get(i).scoreList.getDocidScore(ptri.nextDoc));
				ptri.nextDoc ++;
			}
      }
      //System.out.println();
     result.docScores.add (smallestDocid,combScore);
    }
    if(QryEval.process_DEBUG)System.out.println("----#and evaluateIndri end----");
    freeDaaTPtrs();
    return result;
  }
  
  /**
   *  Evaluates the query operator for boolean retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean (RetrievalModel r) throws IOException {

    //  Initialization

    allocDaaTPtrs (r);
    QryResult result = new QryResult ();

    //  Sort the arguments so that the shortest lists are first.  This
    //  improves the efficiency of exact-match AND without changing
    //  the result.

    for (int i=0; i<(this.daatPtrs.size()-1); i++) {
      for (int j=i+1; j<this.daatPtrs.size(); j++) {
	if (this.daatPtrs.get(i).scoreList.scores.size() >
	    this.daatPtrs.get(j).scoreList.scores.size()) {
	    ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
	    this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
	    this.daatPtrs.get(j).scoreList = tmpScoreList;
	}
      }
    }

    //  Exact-match AND requires that ALL scoreLists contain a
    //  document id.  Use the first (shortest) list to control the
    //  search for matches.

    //  Named loops are a little ugly.  However, they make it easy
    //  to terminate an outer loop from within an inner loop.
    //  Otherwise it is necessary to use flags, which is also ugly.

    DaaTPtr ptr0 = this.daatPtrs.get(0);

    EVALUATEDOCUMENTS:
    for ( ; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc ++) {

      int ptr0Docid = ptr0.scoreList.getDocid (ptr0.nextDoc);
      double minScore = ptr0.scoreList.getDocidScore (ptr0.nextDoc);
      //double docScore = 1.0;

      //  Do the other query arguments have the ptr0Docid?

      for (int j=1; j<this.daatPtrs.size(); j++) {

	DaaTPtr ptrj = this.daatPtrs.get(j);

	while (true) {
	  if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
	    break EVALUATEDOCUMENTS;		// No more docs can match
	  else
	    if (ptrj.scoreList.getDocid (ptrj.nextDoc) > ptr0Docid)
	      continue EVALUATEDOCUMENTS;	// The ptr0docid can't match.
	  else
	    if (ptrj.scoreList.getDocid (ptrj.nextDoc) < ptr0Docid)
	      ptrj.nextDoc ++;			// Not yet at the right doc.
	  else{
		  if(ptrj.scoreList.getDocidScore (ptrj.nextDoc) < minScore){
			  minScore = ptrj.scoreList.getDocidScore (ptrj.nextDoc) ;
		  }
	      break;				// ptrj matches ptr0Docid
	  }
	}
      }

      //  The ptr0Docid matched all query arguments, so save it.
      if (r instanceof RetrievalModelUnrankedBoolean )
    	  result.docScores.add (ptr0Docid,1.0);
      else if (r instanceof RetrievalModelRankedBoolean )
    	  result.docScores.add (ptr0Docid, minScore);
    }

    freeDaaTPtrs ();
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
		double tmp;
    	int q_len = this.args.size();
        for (int j=0; j< q_len; j++) {
        	QryopSl pj= (QryopSl) this.args.get(j);
			tmp = pj.getDefaultScore(r,docid);
			scoreDef *= Math.pow(tmp,1.0/q_len);
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

    return ("#AND( " + result + ")");
  }
  
  public void printScoreList(){
	  System.out.println("ScoreList:");
	  for(int j=0;j< this.daatPtrs.size();j++){
		  System.out.println( this.daatPtrs.get(j).scoreList.scores.size()) ;
	  }
  }
  
  
}
