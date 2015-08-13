/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {
	
	public DocLengthStore docLenS;
	public int CTFt=0;
	public int DFt=0;
    public int N=0;// # of doc in the corpus
    public long C=0;//
    public String term = null;
    public String field = null;
    public double p_MLE = 0.0;
    
  /**
   *  Construct a new SCORE operator.  The SCORE operator accepts just
   *  one argument.
   *  @param q The query operator argument.
   *  @return @link{QryopSlScore}
 * @throws IOException 
   */
  public QryopSlScore(Qryop q) throws IOException {
	docLenS = new DocLengthStore(QryEval.READER);
    this.args.add(q);
	//this.setWeight(this.args.get(0).wgt);
  }

  /**
   *  Construct a new SCORE operator.  Allow a SCORE operator to be
   *  created with no arguments.  This simplifies the design of some
   *  query parsing architectures.
   *  @return @link{QryopSlScore}
   */
  public QryopSlScore() {
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param q The query argument to append.
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluate the query operator.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean)
      	return (evaluateBoolean (r));
    else if(r instanceof RetrievalModelBM25 ){
    	RetrievalModelBM25 bm25 = (RetrievalModelBM25)r;
    	return evaluateBM25(bm25);
    }
    else if(r instanceof RetrievalModelIndri ){
    	RetrievalModelIndri IndriM = (RetrievalModelIndri)r;
    	return evaluateIndri(IndriM);
    }

    return null;
  }

 /**
   *  Evaluate the query operator for boolean retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

    // Evaluate the query argument.

    QryResult result = args.get(0).evaluate(r);
///////For debug
    //result.invertedList.print();
    //result.invertedList.write2File();
    
//////////////
    // Each pass of the loop computes a score for one document. Note:
    // If the evaluate operation above returned a score list (which is
    // very possible), this loop gets skipped.

    for (int i = 0; i < result.invertedList.df; i++) {

      // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY. 
      // Unranked Boolean. All matching documents get a score of 1.0.
      if (r instanceof RetrievalModelUnrankedBoolean){
          result.docScores.add(result.invertedList.postings.get(i).docid,
   			   (double) 1.0);   	  
      }
      else{
    	  result.docScores.add(result.invertedList.postings.get(i).docid,
			   (double) (result.invertedList.postings.get(i).tf));
    	  //QryEval.printDbg("tf value:"+ result.invertedList.postings.get(i).tf);
      }
    }

    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.

    if (result.invertedList.df > 0)
	result.invertedList = new InvList();

    return result;
  }

/*
 * The #SCORE() operator does the RSJ weight (the collection or idf weight) and 
 * the tf weight (the document weight). The #SCORE() operator handles just one query argument, 
 * and has only the information for that one query argument.   
 * The #SUM() operator is responsible for the whole query, and has access to qtf.
 */
public QryResult evaluateBM25( RetrievalModelBM25 r ) throws IOException {  
	QryResult result = args.get(0).evaluate(r);
	int N = QryEval.READER.numDocs(); // num of docs in corpus
    long doclen = 0;
    int curDocid = 0;
    String field = result.invertedList.field;
    double avgDoclen = QryEval.READER.getSumTotalTermFreq(field) / (float)QryEval.READER.getDocCount(field);
    double scoreSum = 0;
    double TFt, DFt,RJSw,TFw ;
    	
    DFt = result.invertedList.df; // The term occur in how many docs
    
	// Caculate score for each doc that contains the term
	for (int i = 0; i < DFt; i++) {
		scoreSum = 0 ;// Caculate one score for one doc
		curDocid=result.invertedList.postings.get(i).docid;
		doclen = this.docLenS.getDocLength(field, curDocid);
		TFt = result.invertedList.postings.get(i).tf;
	    RJSw = (double) Math.log( (double) ( ( N - DFt + 0.5 ) / ( DFt + 0.5 ) ) );
	    TFw = TFt / ( TFt + r.k1*( (1-r.b) + r.b * ( (double)doclen / avgDoclen) ) );
	    // USERw is calculated in #SUM opt
	    // The #SUM() operator is responsible for the whole query, and has access to qtf.
	   scoreSum = RJSw * TFw;
	   result.docScores.add(curDocid, scoreSum);
	}
	
    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.
    if (result.invertedList.df > 0)
	result.invertedList = new InvList();
    
	return result;
}
public QryResult evaluateIndri( RetrievalModelIndri r ) throws IOException {  
	QryResult result = args.get(0).evaluate(r);
	result.term = result.invertedList.term;
	//System.out.println("SlScore term:" + result.term);
	
	//if(QryEval.DEBUG) result.invertedList.print();
	int curDocid = 0;
	int TFt = 0;
    this.DFt = result.invertedList.df; // The term occur in how many docs
    //QryEval.printDbg("term:"+ result.invertedList.term);
    
    this.field = result.invertedList.field;
	this.C = QryEval.READER.getSumTotalTermFreq(this.field);
    this.CTFt = result.invertedList.ctf;
    this.p_MLE = (double)this.CTFt / (double)this.C;
    RetrievalModelIndri IndriM = (RetrievalModelIndri)r;
    
    //this.print();
	// Caculate score for each doc that contains the term
	for (int i = 0; i < this.DFt; i++) {
		//QryEval.printDbg("evaluateIndri ith doc:"+ i);
		//curDocid = result.invertedList.postings.get(i).docid;
		curDocid = result.invertedList.postings.get(i).docid;
		TFt = result.invertedList.postings.get(i).tf;
		//scoreDoc= getScore(r, curDocid, TFt);
		double doclen = this.docLenS.getDocLength( this.field, curDocid);
        this.p_MLE = (double)this.CTFt / (double)this.C;
        //QryEval.printDbg("p_MLE:"+p_MLE);
        
	    double score = IndriM.lambda * ( ((double)TFt + IndriM.mu * this.p_MLE) / (double)(doclen + IndriM.mu) );
	    score += ((double)1-IndriM.lambda) * this.p_MLE;
	    		
		result.docScores.add(curDocid, score );
	}
	if (result.invertedList.df > 0)
			result.invertedList = new InvList();
	return result;
}
  /*
   *  Calculate the default score for a document that does not match
   *  the query argument.  This score is 0 for many retrieval models,
   *  but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

    if (r instanceof RetrievalModelIndri){
    	//QryEval.printDbg("getDefaultScore:"+ docid);
		return getScore(r,(int)docid, 0);
		
	}
    else{
    	return 0.0;
    }
  }
  public double getScore (RetrievalModel r, int docid,int TFt) throws IOException {
    if (r instanceof RetrievalModelIndri){
    	RetrievalModelIndri IndriM = (RetrievalModelIndri)r;
		long doclen = this.docLenS.getDocLength( this.field, docid);
        double score = 1.0;
        this.p_MLE = (double)this.CTFt / (double)this.C;
        //QryEval.printDbg("p_MLE:"+p_MLE);
        
	    score = IndriM.lambda * ( ((double)TFt + IndriM.mu * this.p_MLE) / (double)(doclen + IndriM.mu) );
	    score += ((double)1-IndriM.lambda) * this.p_MLE;
	    
		//QryEval.printDbg("getScore:" + docid + " TFt:" + TFt+ " "+ " score:" + score);
		return score; 
		}
    else{
    	System.err.println("Wrong model!");
    	return 0.0;
    }
  }

  /**
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
      result += (i.next().toString() + " ");

    return ("#SCORE( " + result + ")");
  }
  public void print(){
	  System.out.println("CTFt:"+this.CTFt);
	  System.out.println("DFt:"+this.DFt);
	  System.out.println("C:"+this.C);
	  System.out.println("field:"+this.field);
  }
  
}
