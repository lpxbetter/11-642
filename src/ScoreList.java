/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.*;
//import QryEval.QryEval;

import org.apache.lucene.document.Document;

public class ScoreList {

  //  A little utilty class to create a <docid, score> object.

  protected class ScoreListEntry {
    private int docid;   
    private double score;
    private String extid = null;

    private ScoreListEntry(int docid, double score) {
      this.docid = docid;
      this.score = score;
    }
  }

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *  Append a document score to a score list.
   *  @param docid An internal document id.
   *  @param score The document's score.
   *  @return void
   */
  public void add(int docid, double score) {
    scores.add(new ScoreListEntry(docid, score));
  }

  /**
   *  Get the n'th document id.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }
  
  public String getDocextid(int n) {
	    return this.scores.get(n).extid;
	  }

  /**
   *  Get the score of the n'th document.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }
  /*
  Slow sorting: If your sort comparator looks up the external document ids each time 
  it does a comparison, your sort will be slow. Instead, create an ArrayList that 
  contains entries such as {internal document id, external document id, score}. 
  Copy the ScoreList into this ArrayList, then sort the ArrayList.
  */
  public void assignExtid(){
	  for(int i = 0 ; i< this.scores.size(); i++ ){
	    	try{
			 this.scores.get(i).extid = QryEval.getExternalDocid (this.scores.get(i).docid);
	    	}
	    	catch (Exception e) {
	    		System.out.println("exception");
	    		e.printStackTrace();
	    		}		 
	  }
  }
  public void print(){
	  System.out.println("scoreList.socres.size:"+ this.scores.size());
	  //for (int i = 0; i< this.scores.size() ; i++){
	  //}
  }
  
  public void rankResultScore(){
	  
	  this.assignExtid();
	  Comparator<ScoreListEntry> comparator = new Comparator<ScoreListEntry>(){
		  
	   public int compare(ScoreListEntry s1, ScoreListEntry s2) {
	    // socres
		String extDocid1 = null;
		String extDocid2 = null;
	    if(s1.score > s2.score){
	     //return int(s1.score-s2.score);
	    	return -1;
	    }
	    else if(s1.score < s2.score){
	    	return 1;
	    }
	    else{ // Scores are equal,then compare external doc id 
	    	//getExternalDocid (result.docScores.getDocid(i))
	    	
	    	try{
	    	//extDocid1 = QryEval.getExternalDocid (s1.docid);
	    	//extDocid2 = QryEval.getExternalDocid (s2.docid);
		    extDocid1 = s1.extid;
		    extDocid2 = s2.extid;
	    	}
	    	catch (Exception e) {
	    		System.out.println("exception");
	    		e.printStackTrace();
	    		}
	    	
	    	if(!extDocid1.equals(extDocid2)){
	    		  return extDocid1.compareTo(extDocid2);
	    	}
	    	
	    	return 0;
	    	
	    }
	   }
	  };
	  
	  Collections.sort(this.scores, comparator);
	    
} 
  
}
