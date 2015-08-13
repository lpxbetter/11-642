/**
 *  This class implements the SYN operator for all retrieval models.
 *  The synonym operator creates a new inverted list that is the union
 *  of its constituents.  Typically it is used for morphological or
 *  conceptual variants, e.g., #SYN (cat cats) or #SYN (cat kitty) or
 *  #SYN (astronaut cosmonaut). 
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopIlWin extends QryopIl {
	public int distance ;
  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new QryopIlSyn (arg1, arg2, arg3, ...).
   */
  public QryopIlWin(int d, Qryop... q) {
	this.distance = d;
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
    //  Initialization
    allocDaaTPtrs (r);
    //syntaxCheckArgResults (this.daatPtrs);

    QryResult result = new QryResult ();
    
// For win_DEBUG   
    QryResult resultwin_DEBUG;
 if(QryEval.win_DEBUG){
    for (int i=0; i<this.daatPtrs.size() ; i++ ){
	    //resultwin_DEBUG = args.get(i).evaluate(r);
	    //resultwin_DEBUG.invertedList.print();
    }
}
///////
    // Find the same docid
    DaaTPtr ptr0 = this.daatPtrs.get(0);
    String field = ptr0.invList.field;

    EVALUATEDOCUMENTS:
    for ( ; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc ++) {
      int ptr0Docid = ptr0.invList.getDocid (ptr0.nextDoc);
      double defaultScore = 1.0;

      //  Do the other query arguments have the same ptr0Docid?
      for (int j=1; j<this.daatPtrs.size(); j++) {
    	  
	    DaaTPtr ptrj = this.daatPtrs.get(j);

	    while (true) {
	    	if (ptrj.nextDoc >= ptrj.invList.postings.size())
	    		break EVALUATEDOCUMENTS;		// No more docs can match
	    	else
	    		if (ptrj.invList.getDocid (ptrj.nextDoc) > ptr0Docid)
	    			continue EVALUATEDOCUMENTS;	// The ptr0docid can't match.
	    	else
	    		if (ptrj.invList.getDocid (ptrj.nextDoc) < ptr0Docid)
	    				ptrj.nextDoc ++;			// Not yet at the right doc.
	    	else
	    		break;				// ptrj matches ptr0Docid
	    }
      }
      int sameDocid = ptr0.invList.getDocid(ptr0.nextDoc);
      if(QryEval.win_DEBUG){
      	System.out.println("\nsameDocid:"+ sameDocid );
      }
      //  The doc matched all query arguments,then examine the positions.
      result = checkPositonsInOneDoc(sameDocid,result);
      result.invertedList.field = field;
    }
    freeDaaTPtrs();
    return result;
    }    
 /*
  * Question: How is #WINDOW/n implemented when there are many query terms? 
  * Answer: Consider #WINDOW/n(a b c). Your software iterates down the locations for a, b, and c in parallel.
  *  Suppose the three iterators all start at the first location for each term. The window size that covers 
  *  those 3 term occurrences is of size 1 + Max (a.currentloc, b.currentloc, c.currentloc) - Min (a.currentloc, 
  *  b.currentloc, c.currentloc). If the size is > N, advance the iterator that has the Min location. 
  *  If the size is <= N, you have a match, and you advance all 3 iterators. 
  *  Continue until any iterator reaches the end of its location list.
*/
  public QryResult checkPositonsInOneDoc(int docid, QryResult result){
	    DaaTPtr ptr0 = this.daatPtrs.get(0);
	    int matchCount = 0;
	    int N = this.distance;
	    //List<Integer> positions = new ArrayList<Integer>(); 
	    Vector<Integer> positions = new Vector<Integer>();
	    
	  
	    
		boolean endFlag = false;
		EVALUATEDOCUMENTS:
		while(!endFlag)
		{
			// When no loc to check in one arg's doc, then break
			// Since win require terms occur in all docs
		  for (int j=0; j<this.daatPtrs.size(); j++) {
			  DaaTPtr ptrCur = this.daatPtrs.get(j);
			  //Get all the locs of the cur arg's doc
			  Vector<Integer> posCurV = ptrCur.invList.postings.get(ptrCur.nextDoc).positions;
			  // One arg's doc's locs are all checked
			  if(ptrCur.posIdx >= posCurV.size() ) {
				  endFlag=true; 
				  break EVALUATEDOCUMENTS;}
		  }	
		  
		    Vector<Integer> posVec0 = ptr0.invList.postings.get(ptr0.nextDoc).positions;
			int maxLoc = posVec0.get(ptr0.posIdx);
			int minLoc = maxLoc;
			int sizeLoc = 1 + maxLoc - minLoc;
			DaaTPtr minLocPtr = ptr0;
		 
		//Fine maxLoc and minLoc, caculate the size
		for (int j=1; j<this.daatPtrs.size(); j++) {
			 DaaTPtr ptrCur = this.daatPtrs.get(j); // Current DaatPtr
			 Vector<Integer> posCurV = ptrCur.invList.postings.get(ptrCur.nextDoc).positions;
			if( posCurV.get(ptrCur.posIdx) > maxLoc) maxLoc = posCurV.get(ptrCur.posIdx) ;
			if( posCurV.get(ptrCur.posIdx) < maxLoc) {
				minLoc = posCurV.get(ptrCur.posIdx) ;
				minLocPtr = ptrCur;// record the ptr which h
				
			}
		}
		sizeLoc = 1 + maxLoc - minLoc;
		if(QryEval.win_DEBUG){
			System.out.println("maxLoc:"+maxLoc+" minLoc:"+minLoc+" sizeLoc:"+sizeLoc+" N:"+N);
			// printCurState();
		}

		if (sizeLoc > N ) {// advance the iterator that has the Min location
			minLocPtr.posIdx ++;
		}
		else{ // Match, advance all locs 
			this.printMatchPos();// For debug
			DaaTPtr ptrLast = this.daatPtrs.get(this.daatPtrs.size() - 1 );
			Vector<Integer> posVec = ptrLast.invList.postings.get(ptrLast.nextDoc).positions;
			positions.add(posVec.get(ptrLast.posIdx));
			advanceAllPosIdx();
			if(QryEval.win_DEBUG){
				//System.out.println("match, add position:"+ posVec.get(ptrLast.posIdx));
				this.printPosV(positions);				
			}
			
		}
		}
	  result.invertedList.appendPosting(docid, positions);
	  // Clear the posIdx
	  clearAllPosIdx();
	  return result;
	 // return matchCount;
  }
  
  //This func is for win_DEBUG
  public void printMatchPos(){
	  if(!QryEval.win_DEBUG) return;
	  System.out.print("Match doc's positions:");
	  for (int j=0; j<this.daatPtrs.size(); j++) {
		  DaaTPtr ptrj = this.daatPtrs.get(j);
		  // nextDoc is pointing to the match doc, and posIdx is pointing to the match position. 
		  Vector<Integer> posVec = ptrj.invList.postings.get(ptrj.nextDoc).positions;
		  this.printPosV(posVec);
		  //System.out.print(posVec.get(ptrj.posIdx) +" ");
	  }
	  //System.out.println();
}
  
  public void clearAllPosIdx(){
	  for (int j=0; j<this.daatPtrs.size(); j++) {
		  DaaTPtr ptrj = this.daatPtrs.get(j);
		  ptrj.posIdx = 0 ;
	  }
}
  public void advanceAllPosIdx(){
	  for (int j=0; j<this.daatPtrs.size(); j++) {
		  DaaTPtr ptrj = this.daatPtrs.get(j);
		  ptrj.posIdx ++ ;
	  }
}
  /*
 public void printOneDoc(){
	 if(!QryEval.win_DEBUG)return;
	  for (int j=0; j<this.daatPtrs.size(); j++) {
		  DaaTPtr ptrj = this.daatPtrs.get(j);
		    QryResult resultwin_DEBUG = args.get(j).evaluate(r);
		    resultwin_DEBUG.invertedList.printOneDoc(docId);		  
	  }
 }
 */
 public void printPosV(List <Integer> posV) {
	 if(!QryEval.win_DEBUG) return;
	 System.out.println("The position vector:");
	 for(int j = 0; j < posV.size(); j++){
		 System.out.print(posV.get(j)+" ");
	 }
	 System.out.println();
 }

  /**
   *  Return the smallest unexamined docid from the DaaTPtrs.
   *  @return The smallest internal document id.
   */
  public int getSmallestCurrentDocid () {

    int nextDocid = Integer.MAX_VALUE;

    for (int i=0; i<this.daatPtrs.size(); i++) {
      DaaTPtr ptri = this.daatPtrs.get(i);
      if (nextDocid > ptri.invList.getDocid (ptri.nextDoc))
	nextDocid = ptri.invList.getDocid (ptri.nextDoc);
      }

    return (nextDocid);
  }

  /**
   *  syntaxCheckArgResults does syntax checking that can only be done
   *  after query arguments are evaluated.
   *  @param ptrs A list of DaaTPtrs for this query operator.
   *  @return True if the syntax is valid, false otherwise.
   */
  public Boolean syntaxCheckArgResults (List<DaaTPtr> ptrs) {

    for (int i=0; i<this.args.size(); i++) {

      if (! (this.args.get(i) instanceof QryopIl)) 
	QryEval.fatalError ("Error:  Invalid argument in " +
			    this.toString());
      else
	if ((i>0) &&
	    (! ptrs.get(i).invList.field.equals (ptrs.get(0).invList.field)))
	  QryEval.fatalError ("Error:  Arguments must be in the same field:  " +
			      this.toString());
    }

    return true;
  }

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
      result += (i.next().toString() + " ");

    return ("#NEAR( " + result + ")");
  }
}
