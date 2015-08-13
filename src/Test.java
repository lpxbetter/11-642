 import java.io.*;
import java.util.*;
 
 public class Test{
	 
	 
	 
 public void testRank(){ 	 
	 QryResult result = new QryResult ();
	 result.docScores.add(20, 5.0);
	 result.docScores.add(20, 4.0);
	 result.docScores.add (30,5.0);
	 result.docScores.add (40,5.0);
	 result.docScores.rankResultScore();
	 print(result);

 }
	 
 public void print( QryResult result){
	 String eid = null;
	 for(int i=0;i<result.docScores.scores.size();i++){
	    	int rank = i + 1;
	    	try{
	    		eid = QryEval.getExternalDocid (result.docScores.getDocid(i)) ;
	    	}
	    	catch (Exception e) {
	    		System.out.println("exception");
	    		e.printStackTrace();
	    		}
	        System.out.println(eid
	        	   //+ result.docScores.getDocid(i)
				   + " " + rank
				   + " " + result.docScores.getDocidScore(i)
	        	   + " " + "run-1\n"
	      		   );
	      }
 }
	
	
 }