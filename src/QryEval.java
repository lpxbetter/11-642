/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

//import ScoreList.ScoreListEntry;

public class QryEval {

  static String usage = "Usage:  java " + System.getProperty("sun.java.command")
      + " paramFile\n\n";

  //  The index file reader is accessible via a global variable. This
  //  isn't great programming style, but the alternative is for every
  //  query operator to store or pass this value, which creates its
  //  own headaches.

  public static IndexReader READER;
  public static BufferedWriter writer = null;
  public static BufferedWriter feaWriter = null;
  public static BufferedWriter testfeaWriter = null;

  public static int featureNum = 18;
  // the value 1 means this feature is enabled,otherwise disabled.
  public static Map<Integer,Integer> allFeaMap = new TreeMap<Integer,Integer>();

  final static boolean DEBUG = false;
  final static boolean relsFile_DEBUG = false;
  final static boolean cust_fea_debug = false;
  final static boolean fea_debug = false;
  final static boolean gene_debug = false;
  final static boolean url_debug = false;
  final static boolean cal_debug = false;
  final static boolean rankScore_debug = false;
  final static boolean overlap_debug = false;
  final static boolean bm25Score_debug = false;
  final static boolean indriScore_debug = false;
  final static boolean norm_debug = false;
  final static boolean and_DEBUG = false;
  final static boolean process_DEBUG = false;
  final static boolean wgt_DEBUG = false;
  final static boolean qParser_DEBUG = false;
  final static boolean SHOWQUERY = false;
  final static boolean PRINTRESULT = false;
  final static boolean win_DEBUG = false;
  final static boolean wand_DEBUG = false;
  final static boolean wsum_DEBUG = false;
  final static boolean exp_DEBUG = false;
  final static boolean docsMap_DEBUG = false;
  final static boolean termSort_DEBUG = false; 
  

  //  Create and configure an English analyzer that will be used for
  //  query parsing.

  public static EnglishAnalyzerConfigurable analyzer =
      new EnglishAnalyzerConfigurable (Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }
  public static void testFun() throws Exception{
	  /////////////////////////
	  Map< Integer, Double > feaMap = new HashMap< Integer, Double >() ;
	  Map< String, Map< Integer, Double >> docFeaMap = new HashMap< String, Map< Integer, Double>>();
	  
	  feaMap.put(1,2.0);
	  feaMap.put(2,3.5);
	  docFeaMap.put("doc1",feaMap);
	  Map< Integer, Double > feaMap2 = new HashMap< Integer, Double >() ;
	  feaMap2.put(1,8.0);
	  feaMap2.put(3,9.0);
	  docFeaMap.put("doc2",feaMap2);
	  Map< Integer, Double > feaMap3 = new HashMap< Integer, Double >() ;
	  feaMap3.put(1,3.0);
	  feaMap3.put(2, 3.5);
	  docFeaMap.put("doc3",feaMap3);
	  Map< Integer, Double > feaMap4 = new HashMap< Integer, Double >() ;
	  feaMap4.put(1,0.8);
	  feaMap4.put(3,1.0);
	  feaMap4.put(4,0.0);
	  docFeaMap.put("doc4",feaMap4);
	  printdocFeaMap(docFeaMap);
	  docFeaMap = normalizeFeature(docFeaMap,5);
	  printdocFeaMap(docFeaMap);
	  //testMap.put(1,0.0);
	  
	  /*
	  if(testMap.isEmpty()) {
		  System.err.println("it is empty");
		  testMap.put(1,0.3);
	  }
	  for (Entry<Integer, Double> feaEntry : testMap.entrySet() ) {
		  System.err.println("is not empty");
	  }
	  */

	  ////////////////////////////
  }
  
  /**
   *  @param args The only argument is the path to the parameter file.
   *  @throws Exception
   */
  public static void main(String[] args) throws Exception {    
	// Get current time
	long  timeStart=System.currentTimeMillis();
    // must supply parameter file
    if (args.length < 1) {
      System.err.println(usage);
      System.exit(1);
    }

    // read in the parameter file; one parameter per line in format of key=value
    Map<String, String> params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(args[0]));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());
    scan.close();
    
    // parameters required for this example to run
    if (!params.containsKey("indexPath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }

    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));

    if (READER == null) {
      System.err.println(usage);
      System.exit(1);
    }

    DocLengthStore s = new DocLengthStore(READER);
    RetrievalModel model = null;
    
    //RetrievalModel model = new RetrievalModelUnrankedBoolean();
    if (params.get("retrievalAlgorithm").equalsIgnoreCase("UnrankedBoolean")){
    	model = new RetrievalModelUnrankedBoolean();
    }
    else if (params.get("retrievalAlgorithm").equalsIgnoreCase("RankedBoolean")){
    	model = new RetrievalModelRankedBoolean();
    }
    else if (params.get("retrievalAlgorithm").equalsIgnoreCase("BM25")){
		double	k_1 = Double.parseDouble(params.get("BM25:k_1"));
		double	b = Double.parseDouble(params.get("BM25:b"));
		double  k_3 = Double.parseDouble(params.get("BM25:k_3"));
    	model = new RetrievalModelBM25(k_1,b,k_3);
    }   
    else if (params.get("retrievalAlgorithm").equalsIgnoreCase("Indri")){
		model = new RetrievalModelIndri(Double.parseDouble(params.get("Indri:mu")),Double.parseDouble(params.get("Indri:lambda")));
    }     
    else{     
    	;
  	}
    if (! params.get("retrievalAlgorithm").equalsIgnoreCase("letor")){
    ///////////////// HW4 expanded query
    File rankingFile = null;
    // fb parameter is missing from the parameter file or set to false,
    if (!params.containsKey("fb") || params.get("fb").equalsIgnoreCase("false") ) { 
    	System.err.println("fb false.Orig Indri System."); 
        retrievalForQueries(params, model,params.get("queryFilePath"));
    }   
    else{
    	
    	if (params.containsKey("fbInitialRankingFile")){
    		System.err.println("Reference System,has fbInitialRankingFile");
            //read a document ranking in trec_eval input format from the fbInitialRankingFile
            rankingFile = new File(params.get("fbInitialRankingFile"));   		
    	}
    	else{
    		//Use the retrieval documents as the rankingFile 
    		System.err.println("My System");    		
    		retrievalForQueries(params, model,params.get("queryFilePath"));
    		rankingFile = new File(params.get("trecEvalOutputPath"));
    	}
    	// Combined the original query and expanded query and use the combined query to retrieval
    	
        expQueryRetrieval(params,rankingFile,model);
    } 
    }
    else{ ///////////////// HW5 learning to rank
    	for(int i = 1; i<= featureNum;i++){
    		  allFeaMap.put(i,1);
    	}
    	if (params.containsKey("letor:featureDisable")){
    		String disableStr = params.get("letor:featureDisable");
    		String[] items = disableStr.split(",");
    		for(int i=0; i<items.length;i++){
    			System.out.println( "disable: " + Integer.parseInt(items[i]) );
    			allFeaMap.put( Integer.parseInt(items[i]), 0);
    		}
    	}
    	geneTrData(params);
    	run_svm_rank(params,0);
    	geneTestData(params);
    	reRankTestData(params);
    }
    /////////////////////////////
    // Later HW assignments will use more RAM, so you want to be aware
    // of how much memory your program uses.
    printMemoryUsage(false);
    // Calculate the running time
    long  timeEnd=System.currentTimeMillis();
    long diff = timeEnd - timeStart; // mini seconds
    long sec = diff/1000;
    System.out.println("time spend:"+ sec +"s");
  }
 

  /**
   * parseQuery converts a query string into a query tree.
   * 
   * @param qString
   *          A string containing a query.
   * @param qTree
   *          A query tree
 * @throws Exception 
   */
  static Qryop parseQuery(String qString) throws Exception {

    Qryop currentOp = null;
    int distance = 0;
    String[] tokenProcArr ;
    Stack<Qryop> stack = new Stack<Qryop>();
    qString = qString.toLowerCase().trim();
	//System.out.println("enter parseQuery:qString: "+qString);

    // Tokenize the query.
    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null;
    
    while (tokens.hasMoreTokens()) {
      token = tokens.nextToken();
      if(qParser_DEBUG)System.out.println("token:" + token);

     if (token.matches("[ ,(\t\n\r]")) continue;
     if (token.startsWith(")")) { // Finish current query operator.
        stack.pop();
        if (stack.empty())
          break;
        Qryop arg = currentOp;
        currentOp = stack.peek();
        currentOp.add(arg);
        continue;
     }
     // Other tokens are either operators or terms
     if (token.equalsIgnoreCase("#and")) {
        currentOp = new QryopSlAnd();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#wand")) {
          currentOp = new QryopSlWAnd();
          stack.push(currentOp);
          currentOp.isWgtOpt = true;
      } else if (token.equalsIgnoreCase("#wsum")) {
          currentOp = new QryopSlWSum();
          stack.push(currentOp); 
          currentOp.isWgtOpt = true;
      } else if (token.equalsIgnoreCase("#or")) {
          currentOp = new QryopSlOr();
          stack.push(currentOp);  
      } else if (token.startsWith("#near")) {
    	  distance = getDistance(token);
          currentOp = new QryopIlNear(distance);
          stack.push(currentOp);   
      } else if (token.startsWith("#win")) {
    	  distance = getDistance(token);
          currentOp = new QryopIlWin(distance);
          stack.push(currentOp);           
      } else if (token.equalsIgnoreCase("#syn")) {
        currentOp = new QryopIlSyn();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#sum")) {
          currentOp = new QryopSlSum();
          stack.push(currentOp);        
      } else { // terms
    	// Strings are the terms and their fields
    	String term = token; 
    	String field = "body";
    	// weight opt don't delete stop words, otherwise will delete the weights
    	QryopIlTerm Termop ;
    	if(currentOp.isWgtOpt){
        	Termop = new QryopIlTerm( term, field );
    	}else{
			if ( !isDouble(token) && -1 != token.indexOf('.') ){
				int idx = token.indexOf('.');
				term = token.substring(0,idx);
				field = token.substring(idx+1);
				if(qParser_DEBUG) {System.out.println( " term:"+term+" field:"+ field); }
			} 		
    		// Stop words need be deleted 
    		tokenProcArr = tokenizeQuery(term);
    		if (tokenProcArr.length == 0) continue;
    		Termop = new QryopIlTerm( tokenProcArr[0], field );
    	}
		currentOp.add( Termop);
		continue;
      }
    }

    // A broken structured query can leave unprocessed tokens on the
    // stack, so check for that.
    if (tokens.hasMoreTokens()) {
      if(qParser_DEBUG)System.out.println("still have token:" + tokens.nextToken());
      if(qParser_DEBUG) System.out.println("token count: "+ tokens.countTokens());
      System.err.println("Error:  Query syntax is incorrect. query is=" + qString);
      return null;
    }

    return currentOp;
  }
  
  /*Parse queries and then retrieval the docs, write rst to result fileName. */
  public static void retrievalForQueries( Map<String, String> params, RetrievalModel model,String queryFilePath) throws Exception{
	    //Read the query file
	    BufferedReader qryReader = new BufferedReader(new FileReader(new File(queryFilePath) ));
	    writer = new BufferedWriter(new FileWriter(new File(params.get("trecEvalOutputPath"))));
	    String tempString = null;

	    while ((tempString = qryReader.readLine()) != null) {

	    	tempString = tempString.trim();
	      	if (tempString.equals("")){
	    		System.err.println("Query cann't be empty!");
	    		continue;
	    	}
	      	retrievalForOneQuery(params, model, tempString);
	    }
	    qryReader.close();
	    writer.close();
  }
  
  public static void retrievalForOneQuery(Map<String, String> params, RetrievalModel model, String tempString) throws Exception {
	int queryID = 0;

	if(QryEval.SHOWQUERY){
		//System.out.println("\nori line: " + tempString);
	}
	
	int idxSep = tempString.indexOf(':');
	String qString = tempString; 
	if (idxSep != -1){ 
		String numStr = tempString.substring(0,idxSep).trim();
		queryID = Integer.parseInt(numStr);
		//System.out.println("nums:"+numStr+ "queryID:"+ queryID);
	
		qString = tempString.substring(idxSep+1); 
		//System.out.println("After extracting lineNum, qString="+ qString);
	}
    qString = qString.trim();
    
	printDbg("retrievalAlgorithm:"+ params.get("retrievalAlgorithm"));
	qString = addDefOpt(params, qString);
	
	Qryop  qryTree= parseQuery(qString);
	QryResult result = qryTree.evaluate (model);
	//printResults (tempString,queryID, result);
    writeResults (tempString,queryID, result);
	
  }
  
  public static String addDefOpt( Map<String, String> params, String qString){
		if (params.get("retrievalAlgorithm").equalsIgnoreCase("BM25")){
	            qString = "#sum(" + qString + ")";
		}
		else if (params.get("retrievalAlgorithm").equalsIgnoreCase("Indri")){
	            qString = "#and(" + qString + ")";
		}
		else{ // Boolean, hw1   
			qString = "#and(" + qString + ")";
		}
		return qString;
  }

  /*
   *   // generate training data
  while a training query q is available {
    use QryEval.tokenizeQuery to stop & stem the query
    foreach document d in the relevance judgements for training query q {
      create an empty feature vector
      read the PageRank feature from a file
      fetch the term vector for d
      calculate other features for <q, d>
    }

    normalize the feature values for query q to [0..1] 
    write the feature vectors to file
  }
   */
  public static void geneTrData(Map<String, String> params) throws Exception{	  
	  System.out.println("generate training data begin.");
	    feaWriter = new BufferedWriter(new FileWriter(new File(params.get("letor:trainingFeatureVectorsFile"))));
	    String tempString = null, extid=null, query = null;
	    int queryID =0 ,docid;
	    
		  // cw09training_nomissing.qrels,like this:2 0 clueweb09-en0000-02-03513 0
		  //letor:trainingQrelsFile= A file of relevance judgments. Column 1 is the query id. 
		  //Column 2 is ignored. Column 3 is the document id. Column 4 indicates the degree of relevance (0-2).
	    Map<Integer,  Map<String, Integer> > allDocsMap = getAllDocsMapFromQrelsFile(params);
	    
	    //cw09training.query,like this "2:french lick resort and casino \n 3:getting organized
	    // read the PageRank feature from a file, PageRankInIndex
		Map<String, Double> pRankMap = readPageRankScore(params);
		 
		Map<Integer, String> queryMap = new TreeMap<Integer,String>();
		queryMap = getQueryMap(params, params.get("letor:trainingQueryFile"));
		Map< String, Map< Integer, Double >> docFeaMap = new TreeMap< String, Map< Integer, Double>>();
		
		// For each query,get the features for each doc, store to docFeaMap
		for (Iterator<Integer> it = queryMap.keySet().iterator(); it.hasNext();) {
			queryID = it.next();
			query = queryMap.get(queryID);
			System.out.println("Training data: "+queryID + ":" + query);
		  
		  //<extid, degree>
		  Map<String, Integer> docsMap  = allDocsMap.get(queryID);
		  
		  docFeaMap = getFeaForOneQuery(params, query, docsMap,pRankMap);
			 
		  //step5: normalize the feature values for query q to [0..1]
		  normalizeFeature(docFeaMap,featureNum);
			  
		  // write the feature vectors to file,need docsMap to get the degree for each doc
		  writedocFeaMap(queryID,docsMap, docFeaMap,feaWriter);
		}
		
	//qryReader.close();
	feaWriter.close();
	System.out.println("generating training data end.");
  }
  public static Map<Integer,String> getQueryMap(Map<String,String> params, String filePath) throws Exception{
	  //<queryID, query>
	  Map<Integer, String> queryMap = new TreeMap<Integer,String>();
	  String tempString;
	  BufferedReader qryReader = new BufferedReader(new FileReader(new File(filePath) ));
	  
	  while ((tempString = qryReader.readLine()) != null) {
	    	tempString = tempString.trim();
	    	//System.out.println("orig query: "+tempString);
	      	if (tempString.equals("")){
	    		System.err.println("Query cann't be empty!");
	    		continue;
	      	} 
			int queryID = getQueryID(tempString);
			String query = getQuery(tempString);
			queryMap.put(queryID, query);
	  }
	  qryReader.close();
	  return queryMap;
  }
  public static Map< String, Map< Integer, Double>> getFeaForOneQuery(Map<String,String>params, String query, 
	  Map<String, Integer> docsMap, Map<String, Double> pRankMap) throws Exception{
	  String extid = null;
	  int docid;
	  //S1: use QryEval.tokenizeQuery to stop & stem the query
	  String[] queryStems = QryEval.tokenizeQuery(query);
	  if(gene_debug){
		  for (int i=0;i<queryStems.length;i++){
			  System.out.println(queryStems[i]);
		  }
	  }
	  Map< String, Map< Integer, Double >> docFeaMap = new TreeMap< String, Map< Integer, Double>>();
	  
	  //S2: for each document d in the relevance judgements for training query q 
	  debugOut("docsMap size: "+ docsMap.size());
	  
	  for (Entry<String, Integer> entry : docsMap.entrySet()){
		 extid = entry.getKey();
		 if(gene_debug)System.out.println("extid: "+ extid);
		 docid = getInternalDocid(extid);
		 
		 //step1: create an empty feature vector
		 Map< Integer, Double > feaMap = new HashMap< Integer, Double >() ;
		  
		  //  f1: Spam score for d (read from index).
		  Document d = QryEval.READER.document(docid);
		  int spamscore = Integer.parseInt(d.get("score"));
		  feaMap.put(1,(double) spamscore);
		  
		  //  f2: Url depth for d(number of '/' in the rawUrl field).
		  String rawUrl = d.get("rawUrl");
		  // http://logoku.co.cc/1/1008/urlDep4
		  //int urlDep = rawUrl.split("/"，-1).length - 1 - 2; // need to check
		  //int urlDep = rawUrl.split("/",-1).length - 1;
		  int cnt =0;
		  for(int i=0; i< rawUrl.length() ;i++){
			  //System.out.println(rawUrl.charAt(i));
			  if('/' == rawUrl.charAt(i) )  {cnt++;}
		  }
		  cnt = cnt -2;
		  //if(url_debug)System.out.println("rawUrl:"+rawUrl +" urlDep"+ urlDep + " cnt" +cnt);
		  if(url_debug)System.out.println("rawUrl:"+rawUrl + " cnt" +cnt);
		  feaMap.put(2, (double) (cnt - 2) );
		  
		  //  f3: FromWikipedia score for d (1 if the rawUrl contains "wikipedia.org", otherwise 0).
		  Double wikiScore = 0.0;
		  if (rawUrl.contains("wikipedia.org")) wikiScore = 1.0;
		  feaMap.put(3,wikiScore); 
		  
	     // f4: read the PageRank feature from a file, PageRankInIndex
		 //Map<Integer, Double> pRankMap = readPageRankScore(params);
		 
		 if(pRankMap.containsKey(extid)) {
			 feaMap.put(4, pRankMap.get(extid));
		 }
		 else{
			 if(gene_debug)System.err.println(extid +" doesn't have rankScore.");
		 }
		 
	     //step4: calculate other features for <q, d>
		 feaMap = calOtherFea(params,docid,feaMap,queryStems);
		 //printFeaMap(feaMap);
		 // Add all features of one doc to the docFeaMap
		 docFeaMap.put(extid,feaMap) ;
	  }
	  return docFeaMap;
  }
	  
  public static void geneTestData(Map<String, String> params) throws Exception{	  
	    System.out.println("generating test data begin.");
	    testfeaWriter = new BufferedWriter(new FileWriter(new File(params.get("letor:testingFeatureVectorsFile"))));
	    String query = null;
	    int queryID =0 ;
	    
		double	k_1 = Double.parseDouble(params.get("BM25:k_1"));
		double	b = Double.parseDouble(params.get("BM25:b"));
		double  k_3 = Double.parseDouble(params.get("BM25:k_3"));
		RetrievalModel model = new RetrievalModelBM25(k_1,b,k_3);
	    
	    // read the PageRank feature from a file, PageRankInIndex
		Map<String, Double> pRankMap = readPageRankScore(params);
		
		Map<Integer, String> queryMap = new TreeMap<Integer,String>();
		queryMap = getQueryMap(params,params.get("queryFilePath"));
		Map< String, Map< Integer, Double >> docFeaMap = new TreeMap< String, Map< Integer, Double>>();
		
		for (Iterator<Integer> it = queryMap.keySet().iterator(); it.hasNext();) {
			queryID = it.next();
			query = queryMap.get(queryID);
			System.out.println(queryID + ":" + query);
	      	
	      writer = new BufferedWriter(new FileWriter(new File(params.get("trecEvalOutputPath"))));	
	      params.put("retrievalAlgorithm","BM25");
	      System.out.println(params.get("retrievalAlgorithm"));
		  retrievalForOneQuery(params, model, queryID+":"+query);
		  writer.close();
		  
		  Map<String, Integer>  docsMap = getDocsMapFromInitialRanking(params,queryID);
		  //printDocsMap(docsMap);
		  
		  docFeaMap = getFeaForOneQuery(params, query, docsMap,pRankMap);
		  
		  //step5: normalize the feature values for query q to [0..1]
		  normalizeFeature(docFeaMap,featureNum);
			  
		  // write the feature vectors to file,need docsMap to get the degree for each doc
		  //printdocFeaMap(docFeaMap);
		  writedocFeaMap(queryID,docsMap, docFeaMap,testfeaWriter);
		  
	    }
	//qryReader.close();
	testfeaWriter.close();
	System.out.println("generating test data end.");
}
  /*
   *   // re-rank test data
  call svmrank to produce scores for the test data
  read in the svmrank scores and re-rank the initial ranking based on the scores
  output re-ranked result into trec_eval format
   */
  public static void reRankTestData(Map<String,String> params) throws Exception{
	  //call svmrank to produce scores for the test data
	  run_svm_rank(params,1);
	  
	  //read in the svmrank scores and re-rank the initial ranking based on the scores
	  //Map<String,Double> scoreMap = new TreeMap<String,Double>();
	  Map<Integer,TreeMap<String,Double>> scoreMap = new TreeMap<Integer, TreeMap<String,Double>>();
	  scoreMap = readScore(params);
	  
	  //output re-ranked result into trec_eval format
	  //10 Q0 clueweb09-en0005-08-29722 1 1.878010680000 yubinletor
	  writeRerankResults (params,scoreMap);
	  
  }
  /*
   * scoreMap stores all the doc extid and corresponding scores.
   * key: queryID, 
   * value: a map, <extid_1,score_1>....<extid_n,score_n>
   * First,convert all the scores in the testingDocumentScores to a scoreList.
   * Second,traverse the lines of testingFeatureVectorsFile, find extid from the each line. Then find its corresponding score
   * from the scoreList. Put <extid,score> to one value of scoreMap which obtained from queryID. 
   */
  public static  Map<Integer,TreeMap<String,Double>> readScore(Map<String, String> params) throws Exception{
	  String example_file = params.get("letor:testingFeatureVectorsFile");
  	  String score_file = params.get("letor:testingDocumentScores");
  	  //<extid, score>
  	  
  	  Map<Integer,TreeMap<String,Double>> scoreMap = new TreeMap<Integer, TreeMap<String,Double>>();
  	  
  	  BufferedReader examReader = new BufferedReader(new FileReader(new File(example_file) ));
  	  BufferedReader scoreReader = new BufferedReader(new FileReader(new File(score_file) ));
  	
  	  String line = null, extid = null;
  	  int idxSep;
  	  
  	  ArrayList<Double> scoreList = new ArrayList<Double>();
  	  // Store the scores to a list
      while ((line = scoreReader.readLine()) != null) {
    	  scoreList.add(  Double.parseDouble( line.trim() )  );
   	  }
      
  	  int i = 0;// List starts from 0. one extid in feature file corresponds to one score in score file
  	  TreeMap<String, Double> scoreMapOneQuery = null ;
  	  int preQueryID = 0;
  	  while ((line = examReader.readLine()) != null) { // 0 qid:10 1:0.14583333333333334 2:0.75 ... # clueweb09-en0000-18-28703
  		  String[] items = line.split(" ");
  		  int queryID = Integer.parseInt( items[1].trim().split(":")[1].trim() );
  		  
  		  if( queryID != preQueryID){
  			  scoreMapOneQuery = new TreeMap<String,Double>();
  			  preQueryID =  queryID;
  		  }
  		  idxSep = line.indexOf('#');
  		  extid = line.substring(idxSep+1).trim();
  		  if(rankScore_debug)System.out.println("extid:"+ extid);
  		  //scoreMapOneQuery.put(queryID +"&"+extid, scoreList.get(i));
  		  if(scoreMapOneQuery == null){System.err.println("scoreMapOneQuery is null.");}
  		  else{
  			  scoreMapOneQuery.put(extid, scoreList.get(i));
  			  scoreMap.put(queryID, scoreMapOneQuery);
  			  i++;
  		  }
  	  }
  	  
  	  scoreReader.close(); 
  	  examReader.close();
  	  return scoreMap;
  }
  static void writeRerankResults(Map<String,String>params,Map<Integer,TreeMap<String,Double>> scoreMap) throws Exception{
	  writer = new BufferedWriter(new FileWriter(new File(params.get("trecEvalOutputPath"))));
	  
		 for (Iterator<Integer> it = scoreMap.keySet().iterator(); it.hasNext();) {
			 	TreeMap<String, Double> val = new TreeMap<String,Double>();
				int queryID = it.next();
				val = scoreMap.get(queryID);
				debugOut(scoreMap.get(queryID).size()+"");
				debugOut("writeRerankRes queryID:"+queryID);
				debugOut("writeRerankRes val.size:"+val.size());
				writeRerankResForOneQuery(queryID,val);
			}
  	writer.close();
  }
  
  static void writeRerankResForOneQuery( int queryID, Map<String,  Double> scoreMapOneQuery) throws IOException {
	  //10 Q0 clueweb09-en0005-08-29722 1 1.878010680000 yubinletor
	  //writer = new BufferedWriter(new FileWriter(new File(params.get("trecEvalOutputPath"))));
	  
      // Convert map.entrySet() to list
      List<Map.Entry<String,Double>> list = new ArrayList<Map.Entry<String,Double>>(scoreMapOneQuery.entrySet());
      Collections.sort(list,new Comparator<Map.Entry<String,Double>>() {
          public int compare(Map.Entry<String, Double> o1,
                  Map.Entry<String, Double> o2) {
              //return o1.getValue().compareTo(o2.getValue());
          	//Decreasing
          	return o2.getValue().compareTo(o1.getValue());
          }
          
      });
      int rank = 1;
      for(Map.Entry<String,Double> mapping:list){ 
    	  //String queryID = mapping.getKey().split("&")[0];
    	  //String extid = mapping.getKey().split("&")[1];
          //System.out.println(+":"+mapping.getValue()); 
    	  String extid = mapping.getKey();
    	  
    	  writer.write(queryID +" Q0 "
				   + extid
				   //+ " docid"+ result.docScores.getDocid(i)
				   + " " + rank
				   + " " + mapping.getValue()
	        	   + " " + "run-1\n"
	      		   );
    	  if(rankScore_debug){
    	  debugOut(queryID +" Q0 "
				   + extid
				   //+ " docid"+ result.docScores.getDocid(i)
				   + " " + rank
				   + " " + mapping.getValue()
	        	   + " " + "run-1\n"
	      		   );
    	  }
	        rank++;
        } 
	  //writer.close();
      /*
	    if (result.docScores.scores.size() < 1) {
	    	writer.write(queryID +" Q0"+" dummy 1 0 run-1\n" );
	    } else {
	    try {
	      //result.docScores.rankResultScore();
	      
	      int printNum = result.docScores.scores.size();
	      if (printNum > 100) printNum = 100;
	      for (int i = 0; i < printNum; i++) {
	    	int rank = i + 1;
	        writer.write(queryID +" Q0 "
				   + getExternalDocid (result.docScores.getDocid(i))
				   //+ " docid"+ result.docScores.getDocid(i)
				   + " " + rank
				   + " " + result.docScores.getDocidScore(i)
	        	   + " " + "run-1\n"
	      		   );
	        if(QryEval.PRINTRESULT){
	        	
	        	System.out.println(queryID +" Q0 "
	        			+ result.docScores.getDocid(i)
					   + result.docScores.getDocextid(i)
					   + " " + rank
					   + " " + result.docScores.getDocidScore(i)
		        	   + " " + "run-1\n"	        			
	        			);
	        }
	      }
	    } catch (Exception e) {
	      e.printStackTrace();
	    } 
	    
	    }
	    */
  }
  /*
   * letor:svmRankLearnPath= A path to the svm_rank_learn executable.
letor:svmRankClassifyPath= A path to the svm_rank_classify executable.
letor:svmRankParamC= The value of the c parameter for SVMrank. 0.001 is a good default.
letor:svmRankModelFile= The file where svm_rank_learn will write the learned model.
letor:testingFeatureVectorsFile= The file of feature vectors that your software will write for the testing queries.
letor:testingDocumentScores= The file of document scores that svm_rank_classify will write for the testing feature vectors.
   */
  public static void run_svm_rank(Map<String, String> params,int trainORtest) throws Exception{
	    // runs svm_rank_learn from within Java to train the model
	    // execPath is the location of the svm_rank_learn utility, 
	    // which is specified by letor:svmRankLearnPath in the parameter file.
	    // FEAT_GEN.c is the value of the letor:c parameter.
	  	Process cmdProc;
	  	
	    if(trainORtest == 0){// train
	    String execPath = params.get("letor:svmRankLearnPath");
	    String FEAT_GEN_c = params.get("letor:svmRankParamC");
	    String qrelsFeatureOutputFile = params.get("letor:trainingFeatureVectorsFile");
	    String modelOutputFile = params.get("letor:svmRankModelFile");
	    cmdProc = Runtime.getRuntime().exec(
	            new String[] { execPath, "-c", String.valueOf(FEAT_GEN_c), qrelsFeatureOutputFile,
	                modelOutputFile });
	    }
	    else{ //test
	    	String execPath = params.get("letor:svmRankClassifyPath");
	    	String example_file = params.get("letor:testingFeatureVectorsFile");
	    	String model_file = params.get("letor:svmRankModelFile");
	    	String output_file = params.get("letor:testingDocumentScores");
	    	
	    	//usage: svm_struct_classify [options] example_file model_file output_file
		    cmdProc = Runtime.getRuntime().exec(
		            new String[] { execPath, example_file , model_file,
		                output_file });
	    }
	    	
	    // The stdout/stderr consuming code MUST be included.
	    // It prevents the OS from running out of output buffer space and stalling.

	    // consume stdout and print it out for debugging purposes
	    BufferedReader stdoutReader = new BufferedReader(
	        new InputStreamReader(cmdProc.getInputStream()));
	    String line;
	    while ((line = stdoutReader.readLine()) != null) {
	      System.out.println(line);
	    }
	    // consume stderr and print it for debugging purposes
	    BufferedReader stderrReader = new BufferedReader(
	        new InputStreamReader(cmdProc.getErrorStream()));
	    while ((line = stderrReader.readLine()) != null) {
	      System.out.println(line);
	    }

	    // get the return value from the executable. 0 means success, non-zero 
	    // indicates a problem
	    int retValue = cmdProc.waitFor();
	    if (retValue != 0) {
	      throw new Exception("SVM Rank crashed.");
	    }
}
  
  //f4: PageRank score for d (read from file)."clueweb09-en0011-58-31570	6.984868"
  public static Map<String, Double> readPageRankScore(Map<String, String> params) throws Exception{
	  File prankFile = new File(params.get("letor:pageRankFile"));
	  BufferedReader fileReader = new BufferedReader(new FileReader(prankFile));
	  Map<String, Double> pRankMap = new HashMap<String, Double>();
	  String line = null;
	
	  while ((line = fileReader.readLine()) != null  ) {
		  // clueweb09-en0011-58-31570	6.984868
		  String[] items = line.split("\t");
		 
		  //System.out.println("relsFile line: "+line);
		  //System.out.println(" items[0].trim();"+ items[0].trim());
		  //System.out.println(" items[0].trim();"+ items[1].trim());
		  //int docid = getInternalDocid( items[0].trim() ); // Ext id to internal id
		  pRankMap.put(items[0].trim(), Double.parseDouble(items[1].trim()) );
	  }
	  fileReader.close();
	  return pRankMap;
  }
  /*
   * f1: Spam score for d (read from index).
f2: Url depth for d(number of '/' in the rawUrl field).
f3: FromWikipedia score for d (1 if the rawUrl contains "wikipedia.org", otherwise 0).
f4: PageRank score for d (read from file).
f5: BM25 score for <q, dbody>.
f6: Indri score for <q, dbody>.
f7: Term overlap score for <q, dbody>.
f8: BM25 score for <q, dtitle>.
f9: Indri score for <q, dtitle>.
f10: Term overlap score for <q, dtitle>.
f11: BM25 score for <q, durl>.
f12: Indri score for <q, durl>.
f13: Term overlap score for <q, durl>.
f14: BM25 score for <q, dinlink>.
f15: Indri score for <q, dinlink>.
f16: Term overlap score for <q, dinlink>.
f17: A custom feature - use your imagination.
f18: A custom feature - use your imagination.
Term overlap is defined as the percentage of query terms that match the document field.

To get a document's spam score and raw (unparsed) URL from the index:

  Document d = QryEval.READER.document(docid);
  int spamscore = Integer.parseInt(d.get("score"));
  String rawUrl = d.get("rawUrl");
   */
  public static Map<Integer, Double> calOtherFea(Map<String,String>params,int docid, Map<Integer, Double> feaMap, String[] queryStems) throws Exception{
	  // calculate other features for <q, d>
	  Map<String, Integer> fieldMap = new HashMap<String, Integer>();
	  fieldMap.put("body",5); // Means the score for body field will put to f5,f6,f7
	  fieldMap.put("title",8);
	  fieldMap.put("url",11);
	  fieldMap.put("inlink",14);
	  
	  String field = null; 
	  TermVector tv = null;
	  for (Entry<String, Integer> entry : fieldMap.entrySet()){                                                                                      
		  field = entry.getKey();
		     //fetch the term vector for d
			  /*
			   * Common Pitfall #1: Not all documents have every field. For example, not many documents 
			   * have an inlink field value. If you try to instantiate a TermVector for non-existent fields, 
			   * you will get a NullPointerException and the system will crash. 
			   * Therefore, you should always check to see if that field exists first.
			   */
			  Terms terms = QryEval.READER.getTermVector(docid, field);
			  if (terms == null) { // field doesn't exist!
				  if(cal_debug)System.err.println("Doc missing field: " + docid + " " + field+". Ignore this field.");
				  continue;
				}
			  else{
				  tv = new TermVector(docid, field);
			  }
	  // f5: BM25 score for <q, dbody>.
	  feaMap.put( fieldMap.get(field), getBM25Score(params,queryStems,tv,docid,field));
	  // f6: Indri score for <q, dbody>.
	  feaMap.put( fieldMap.get(field) + 1 , getIndriScore(params,queryStems,tv,docid,field)) ;
	  // f7: Term overlap score for <q, dbody>.
	  feaMap.put( fieldMap.get(field) + 2 , getOverlapScore(queryStems,tv));
	  }
	  if (allFeaMap.get(17) == 1){
		  //f17: A custom feature - use your imagination.
		  //f18: A custom feature - use your imagination.
		  //feaMap.put(17, 0.0); // tf*idf of body
		  
		  Double tfidf = getTfidfOfField(docid,"body");
		  if(tfidf != -1.0){
			  feaMap.put(17, tfidf);
		  }
		  /*
		  Double VSMScore = getVSMScore(params,queryStems,docid,"title") ;
		  if(VSMScore != -100.0){
			  feaMap.put(17, VSMScore);
		  }
		  */
	  }
	  
	  if (allFeaMap.get(18) == 1){
		  //feaMap.put(18, 0.0); // dl(document length) of title 
		  long doclen = getLenOfField(docid, "body");
		  if(doclen != -1){
			  feaMap.put(18, (double) doclen);
		  }
	  }
	  
	  return feaMap;
  }
  

  
  public static long getLenOfField(int docid, String field) throws Exception{
	  TermVector tv = null;
	  Terms terms = QryEval.READER.getTermVector(docid, field);
	  
	  if (terms == null) { // field doesn't exist!
		  if(cal_debug)System.err.println("Doc missing field: " + docid + " " + field+". Ignore this field.");
		  return -1;
		}
	  else{
		  tv = new TermVector(docid, field);
	  }
	  
	DocLengthStore s = new DocLengthStore(READER);
	long doclen = s.getDocLength(field, docid);
	if(cust_fea_debug)System.out.println("title len=" + doclen);
	return doclen;
  }
  
  public static Double getTfidfOfField(int docid, String field) throws Exception{
	  TermVector tv = null;
	  Terms terms = QryEval.READER.getTermVector(docid, field);
	  
	  if (terms == null) { // field doesn't exist!
		  if(cal_debug)System.err.println("Doc missing field: " + docid + " " + field+". Ignore this field.");
		  return -1.0;
		}
	  else{
		  tv = new TermVector(docid, field);
	  }
	  Double tfidf=0.0;
	  double TFt, DFt;
	  int N = QryEval.READER.numDocs(); // num of docs in corpus
	  
	  for (int i = 1; i < tv.stemsLength(); i++ ){ 
		  String stem = tv.stemString(i);
		  TFt = tv.stemFreq(i);
		  DFt = tv.stemDf(i) ; 
		  Double IDFt = (double) Math.log( (double) ( N/DFt ) );
		  tfidf += TFt * IDFt;
		  //System.out.println("TFt="+TFt+" DFt="+DFt+" IDFt="+IDFt+" tf*idf="+ TFt*IDFt);
		  //System.out.println("tfidf="+tfidf);
	  }
	if(cust_fea_debug)System.out.println(tfidf); 
	tfidf = tfidf / tv.stemsLength();
	return tfidf;		  
  }
  //Term overlap is defined as the percentage of query terms that match the document field.
  public static Double getOverlapScore(String[] queryStems,TermVector tv) throws Exception{
	  //System.out.println("docid: "+docid);
	  Double overlapScore = 0.0;

		  int overlapNum = 0;
		  for (int i = 1; i < tv.stemsLength(); i++ ){
			  String stem = tv.stemString(i);
			  if(Arrays.asList(queryStems).contains(stem) ){
				  if(overlap_debug)System.out.println("stem is in query:"+stem);
				  overlapNum += 1;
			  }
		  }
		  //Term overlap is defined as the percentage of query terms that match the document field.
		  overlapScore = (double) overlapNum /queryStems.length;
		  if(overlap_debug)System.out.println("overlapNum: "+ overlapNum + "overlapScore: "+overlapScore);
		
		return overlapScore;
  }
  
  public static Double getVSMScore(Map<String,String>params,String[] queryStems,int docid,String field) throws Exception{

	Double totalVSMScore = 0.0, termVSMScore=0.0;
	TermVector tv =null;
	  Terms terms = QryEval.READER.getTermVector(docid, field);
	  if (terms == null) { // field doesn't exist!
		  System.err.println("Doc missing field: " + docid + " " + field+" when VSM score");
		  return -100.0;
		}
	  else{
		  tv = new TermVector(docid, field);
	  }

	//double scoreSum = 0;
	double TFt, DFt,RJSw,TFw,USERw ;
	int qtf = 1;
	//You can assume qtf to be 1.
	//USERw = (k_3+1)*qtf / (double)(k_3+qtf);

	long doclen = 0;
	double avgDoclen = QryEval.READER.getSumTotalTermFreq(field) / (float)QryEval.READER.getDocCount(field);
	DocLengthStore s = new DocLengthStore(READER);
	doclen = s.getDocLength(field, docid);
	
	
	int N = QryEval.READER.numDocs(); // num of docs in corpus
	// you can use TermVector.stemDf(index) to get the Document Frequency of the stem
	Double TFSum = 0.0;
	//Double UserWgt = 0.0;
	Double docLenNorm = 0.0;
	Double queryLenNorm = 0.0;
	
	for(int i=0; i< queryStems.length;i++){
		int qDFt = QryEval.READER.docFreq(new Term( field, new BytesRef(queryStems[i]) ) );
		queryLenNorm += Math.pow( (Math.log(1) + 1) * Math.log( N/qDFt) , 2);
	}
	queryLenNorm = Math.sqrt(queryLenNorm);
	
	for (int i = 1; i < tv.stemsLength(); i++ ){
	  String stem = tv.stemString(i);
	  DFt = tv.stemDf(i) ; 
	  TFt = tv.stemFreq(i);
	  
	  docLenNorm += Math.pow( Math.log(TFt) + 1, 2); 
	  
	  if(Arrays.asList(queryStems).contains(stem) ){
		  TFSum += (Double)(Math.log(TFt) + 1) * (Double)( Math.log(1) + 1 ) * (Double)( Math.log(N/DFt) ) ;
	  }
	}
	docLenNorm = Math.sqrt(docLenNorm);
	totalVSMScore = TFSum / (queryLenNorm * docLenNorm);
	//System.out.println("totalVSMScore="+totalVSMScore);
	return totalVSMScore; 	  
  }
  /*
  The main advantage of this implementation is its efficiency and simplicity. It does not use inverted lists, 
  or sort and store ranked lists. Its main disadvantage is that you must reimplement BM25 and Indri to work from 
  a term vector data structure (the same data structure used in HW4). You may assume BOW queries (no query operators 
  of any kind), which simplifies implementation considerably. Pseudo code is shown below for a BM25 implementation 
  that uses term vectors.
  Recall that a document term vector is a parsed representation of a document. See Lecture 13 for details.
  You may access Lucene's term vectors directly, if you wish. Or, if you prefer, the example QryEval software includes a simple TermVector class that provides a simple, Indri-like API that has the following capabilities.

Constructor:
TermVector (int docId, String fieldName)	Create a TermVector for a field in a document. Throws IOException.
Methods:
int	positionsLength()	The number of term positions in this field.
int	stemAt (int position)	The index of the stem that occurred at the specified position.
int	totalStemFreq (int i)	The total frequency of the ith stem across all documents, or -1 if the index is invalid. The frequency for stopwords (i=0) is not stored (0 is returned).
String	stemString (int i)	The string for the ith stem, or null if the index is invalid.
int	stemsLength ()	The number of unique stems in this field.

*/
  /*
  queryStems = QryEval.tokenizeQuery(query);
  totalBM25Score = 0
  for each stem in field
    if stem is a queryStem
       totalBM25Score += BM25 term score for stem
    end
  end
   */
  
  public static Double getBM25Score(Map<String,String>params,String[] queryStems,TermVector tv,int curDocid,String field) throws Exception{
	double  k_1 = Double.parseDouble(params.get("BM25:k_1"));
	double  b = Double.parseDouble(params.get("BM25:b"));
	double  k_3 = Double.parseDouble(params.get("BM25:k_3"));

	Double totalBM25Score = 0.0, termBM25Score=0.0;

	//double scoreSum = 0;
	double TFt, DFt,RJSw,TFw,USERw ;
	int qtf = 1;
	//You can assume qtf to be 1.
	USERw = (k_3+1)*qtf / (double)(k_3+qtf);

	long doclen = 0;
	double avgDoclen = QryEval.READER.getSumTotalTermFreq(field) / (float)QryEval.READER.getDocCount(field);
	DocLengthStore s = new DocLengthStore(READER);
	doclen = s.getDocLength(field, curDocid);
	
	int N = QryEval.READER.numDocs(); // num of docs in corpus
	// you can use TermVector.stemDf(index) to get the Document Frequency of the stem

	for (int i = 1; i < tv.stemsLength(); i++ ){
	  String stem = tv.stemString(i);
	  if(Arrays.asList(queryStems).contains(stem) ){
		  DFt = tv.stemDf(i) ; 
		  TFt = tv.stemFreq(i);
		  
		  RJSw = (double) Math.log( (double) ( ( N - DFt + 0.5 ) / ( DFt + 0.5 ) ) );
		  TFw = TFt / ( TFt + k_1*( (1-b) + b * ( (double)doclen / avgDoclen) ) );
			
		  termBM25Score = RJSw * TFw * USERw;
		  
		  totalBM25Score += termBM25Score;
	  }
	}

	//Double p_MLE = (double) (tv.totalStemFreq(i) / C_len); //ctf/C
	//Double P_td = ( (double) tv.stemFreq(i) + fbMu * p_MLE ) / (doclen + fbMu)
	return totalBM25Score; 	  
  }
 
  /*
   * public QryResult evaluateBM25( RetrievalModelBM25 r ) throws IOException {  
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
   */
  public static Double getIndriScore(Map<String,String>params,String[] queryStems,TermVector tv,int curDocid,String field) throws Exception{
	  //step1: get the parameters
		double  lambda = Double.parseDouble(params.get("Indri:lambda"));
		double  mu = Double.parseDouble(params.get("Indri:mu"));
		if(indriScore_debug) debugOut("mu: "+mu+"lambda: "+ lambda);

		Double totalIndriScore = 1.0, termIndriScore=0.0;
	  	//Double defaultScore = 0.0;
		long TFt = 0, CTFt ;
		DocLengthStore s = new DocLengthStore(READER);
		long doclen = s.getDocLength(field, curDocid);
		boolean matchAnyQuery = false;// If false, this doc doesn't match any term.
		Map<String, Integer> isMatchMap = new HashMap<String, Integer>();
		for(int k=0; k<queryStems.length; k++){
			isMatchMap.put(queryStems[k],0); // Initialize to 0
			if(indriScore_debug) debugOut("queryStems[k]: " + queryStems[k] +" isMatch: "+ isMatchMap.get(queryStems[k]));
		}
		
		// To clarify, calculate default scores for missing terms. However, if a field contains NO query terms at all, 
		//that field should get a 0 score, rather than the default score for query terms.
		
		// Check every term in this doc field; stemsLength(): The number of unique stems in this field.
		for (int i = 1; i < tv.stemsLength(); i++ ){
			String stem = tv.stemString(i);
			// The stem is in query, get its TF. Otherwise it's TF is 0.
			if(Arrays.asList(queryStems).contains(stem) ){
				if(indriScore_debug) debugOut("match stem: "+ stem);
				matchAnyQuery = true; // This doc match at least one query term.
				isMatchMap.put(stem, 1);
				TFt = tv.stemFreq(i);
			
				CTFt = QryEval.READER.totalTermFreq (new Term(field, new BytesRef(stem)));
				//tv.totalStemFreq(i); // Also can get the ctf
				Double p_MLE = (double)CTFt / (double)( QryEval.READER.getSumTotalTermFreq(field) );
				if(indriScore_debug) debugOut("p_MLE:"+p_MLE);
				
				termIndriScore = lambda * ( ((double)TFt + mu * p_MLE) / (double)(doclen + mu) ) + ((double)1 - lambda) * p_MLE;
				if(indriScore_debug) debugOut("termIndriScore: "+termIndriScore);
				
				totalIndriScore *= Math.pow(termIndriScore, 1.0/queryStems.length);
				}
		}
		for(int k=0; k<queryStems.length; k++){
			if(indriScore_debug) debugOut("after check:"+"queryStems[k]: " + queryStems[k] +" isMatch: "+ isMatchMap.get(queryStems[k]));
		}
		
		for (Entry<String, Integer> entry : isMatchMap.entrySet()){                                                                                      
			 String qterm = entry.getKey();
			 // This term doesn't occur in the doc, calulate the default score
			 if(isMatchMap.get(qterm) == 0){
				 if(indriScore_debug) debugOut("qterm doesn't occur in doc: "+ qterm);
					CTFt = QryEval.READER.totalTermFreq (new Term(field, new BytesRef(qterm)));
					//tv.totalStemFreq(i); // Also can get the ctf
					Double p_MLE = (double)CTFt / (double)( QryEval.READER.getSumTotalTermFreq(field) );
					if(indriScore_debug) debugOut("p_MLE:"+p_MLE);
					
					termIndriScore = lambda * ( (0 + mu * p_MLE) / (double)(doclen + mu) ) + ((double)1 - lambda) * p_MLE;
					if(indriScore_debug) debugOut("termIndriScore: "+termIndriScore);
					
					totalIndriScore *= Math.pow(termIndriScore, 1.0/queryStems.length);				 
			 }
		}
		  /*
		   * Yes, you should calculate default scores for the remaining 2 terms.However, in the case that the field 
		   * does not match ANY query terms at all, instead of calculating default scores for 
		   * all five query terms and multiplying them together, you should use 0.
		   */
		if(! matchAnyQuery) totalIndriScore = 0.0;
		if(indriScore_debug) debugOut("totalIndriScore: " + totalIndriScore);

		return totalIndriScore; 
  }
  
  /*
   * public QryResult evaluateIndri( RetrievalModelIndri r ) throws IOException {  
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
   */
  /*
   * The most convenient software architecture creates a feature vector, writes it to disk, creates the next feature vector, writes it to disk, 
   * and so on. Unfortunately, SVMrank is a little more effective if your features are all normalized 
   * to be in the same range, for example [0..1]. 
   * The software does not know the range of Indri, BM25, PageRank, and other features in advance, 
   * thus it cannot normalize feature vectors incrementally. Instead, it must wait, and 
   * do normalization after all feature vectors for that query are created.
	The grading software expects that your features are normalized to [0..1].
	To normalize a feature (e.g., f5) for a specific query, identify the maximum and minimum values for that feature, 
	and then do standard [0..1] normalization. For example, the normalized value for feature f5 (q3, d21) is:

(featureValue_f5 (q3, d21) - minValue_f5 (q3)) / (maxValue_f5 (q3) - minValue_f5 (q3)). 

featureValue_f5 (q3, d21): The value of feature f5 for query q3 and document d21.
minValue_f5 (q3): The minimum value of feature f5 across all feature vectors for query q3. 
maxValue_f5 (q3): The maximum value of feature f5 across all feature vectors for query q3.
If the min and max are the same value, set the feature value to 0.

Thus, that each feature is normalized separately and for each query.
   */
  
  public static Map< String, Map< Integer, Double >> normalizeFeature(Map< String, Map< Integer, Double >> docFeaMap, int feaCnt){
	  String extid=null;
	  String arrStr = "";
	  //int feaCnt = docFeaMap.get(1).size(); // the number of features per doc
	  // every feature has a scoreList. key:feature, value=scoreList
	  Map<Integer,ArrayList<Double>> feaScoreListMap = new HashMap<Integer,ArrayList<Double>>();
	  
	  // Initialize the scoreLists for all features
	  for(int key=1; key<=feaCnt ;key++){
		  ArrayList<Double> oneFeaScoreList = new ArrayList<Double>();
		  feaScoreListMap.put(key, oneFeaScoreList);
	  }
	  
	  // Traverse the docFeaMap and add the score of one feature to the list of this feature
	  for (Entry<String, Map<Integer, Double>> entry : docFeaMap.entrySet() ) {
		  extid = entry.getKey();
		  if(norm_debug)System.out.println("norm extid: "+ extid);
		  Map<Integer, Double> feaMap = new HashMap<Integer, Double>();
		  feaMap = entry.getValue();// The features of one doc
		  //Map< Integer, Double > feaMap
		  if(norm_debug)printFeaMap(feaMap);
		  // Add the score to the list. Some features may be missed.
		  for (Entry<Integer, Double> feaEntry : feaMap.entrySet() ) {
			  int feature = feaEntry.getKey();
			  feaScoreListMap.get(feature).add(feaEntry.getValue()) ;
		  }
	  }
	  
	  // Fine min and max from each scoreList and store them to a Map
	  // <feature, [min,max] >
	  Map<Integer, ArrayList<Double>> minMaxMap = new HashMap<Integer, ArrayList<Double>>();
	  
	  for (Entry<Integer, ArrayList<Double>> listEntry : feaScoreListMap.entrySet() ) {  
		  // Find the min and max of each feature's scores
		  ArrayList<Double> scoreList = listEntry.getValue();
		  if(scoreList.isEmpty()) continue;
		  Double min = scoreList.get(0);// List starts from 0
		  Double max = scoreList.get(0);
		  // Find min and max from te list
		  for (int i=0;i<scoreList.size();i++ ) {
	            max = max > scoreList.get(i) ? max : scoreList.get(i);  
	            min = min > scoreList.get(i) ? scoreList.get(i) : min; 
		  }
		  // <feature, [min,max] >
		  ArrayList<Double> minMaxList = new ArrayList<Double>();
		  minMaxList.add(min);
		  minMaxList.add(max);
		  minMaxMap.put(listEntry.getKey(), minMaxList);
		  
		  /*if(norm_debug){
			  debugOut("scoreList.size(): "+scoreList.size());
			  arrStr = "[" + arrStr.substring(0, arrStr.length() - 1) + "]";
			  debugOut(arrStr );
		  	  debugOut( "max：" + max + "min：" + min);
		  }*/
	  }
	  if(norm_debug)printminMaxMap(minMaxMap);
	  
	  // normalize each doc's features
	  for (Entry<String, Map<Integer, Double>> entry : docFeaMap.entrySet() ) {
		  //normVal = (featureValue_f5 (q3, d21) - minValue_f5 (q3)) / (maxValue_f5 (q3) - minValue_f5 (q3));
		  extid = entry.getKey();
		  Map<Integer, Double> feaMap = new HashMap<Integer, Double>();
		  feaMap = entry.getValue();// The features of one doc
		  
		  for (Entry<Integer, Double> feaEntry : entry.getValue().entrySet() ) {
			  int feature = feaEntry.getKey();
			  Double minVal = minMaxMap.get(feature).get(0);
			  Double maxVal = minMaxMap.get(feature).get(1);
			  if(minVal.equals(maxVal)) feaMap.put(feature, 0.0);
			  else{
				  Double newScore = (feaEntry.getValue() - minVal ) / (maxVal - minVal);
			  	  feaMap.put(feature, newScore);// Update the score
			  }
		  }
		  
		  // If one doc doesn't have some features,but those features are enabled.Then should put
		  // 0.0 to these features.
		  for (Iterator<Integer> it = allFeaMap.keySet().iterator(); it.hasNext();) {
			  int key = it.next();
			//This feature is disabled.Remove this feature.all docs can't have this feature.
			  if(allFeaMap.get(key) == 0) { 
				  feaMap.remove(key); 
			  }
			  else{ //This feature is enbaled, but this doc doesn't contain this feature, add it,and put 0.0 score
				  if( !feaMap.containsKey(key) ) feaMap.put(key, 0.0);
				  //System.out.println(Id + " " + val);
			  }
			}
		  /*
		  for(int key=1; key<=feaCnt ;key++){ // need to check
			  //feaMap.isEmpty();
			  // doesn't contain a feature, add the feature to map and set its value to 0.0
			  if(!feaMap.containsKey(key)) feaMap.put(key, 0.0);
		  }
		  */
		  // update docFeaMap with new feaMap
		  docFeaMap.put(extid, feaMap);
	  }
	  
	  return docFeaMap;
  }
  

  
  public static void printFeaMap(Map<Integer, Double> feaMap){
  if (feaMap==null){return;}
  for (Entry<Integer, Double> feaEntry : feaMap.entrySet() ) {
	  System.out.print("" + feaEntry.getKey()+":");
	  System.out.print(""+ feaEntry.getValue()+",");
  }
  System.out.println();
  }
  
  public static void writeFeaVec(int degree, int queryID,Map<Integer, Double>feaMap, String extid,BufferedWriter featureWriter) throws Exception{
	  //2 qid:1 1:1 2:1 3:0 4:0.2 5:0 # clueweb09-en0000-48-24794
	  //2 qid:1 1:0 2:0 3:1 4:0.1 5:1 # clueweb09-en0011-93-16495
	  //The first column is the score or target value of a <q, d> pair. 
	  ///In a training file, use the relevance value obtained for this <q, d> pair from the 
	  //relevance judgments ("qrels") file. In a test file, this value should be 0.
	  // Feature ids must be integers starting with 1. Features may be integers or floats. 
	  //Features must be in canonical order (1 first, 18 last).
	  // However, the grading software examines the comment field. Each line must end with the external document id.
	  //for (Entry<String, Integer> entry : docsMap.entrySet()){
	  String feaStr = "";
	  if(fea_debug)System.out.println("feaMap.size():"+feaMap.size());
	  
	  // Convert HashMap to TreeMap
	  Map<Integer, Double> treeMap = new TreeMap<Integer, Double>(feaMap);
	  //System.out.println("\nDisplay entries in ascending order of key");
	  //System.out.println(treeMap);
	  for (Iterator<Integer> it = treeMap.keySet().iterator(); it.hasNext();) {
		  int key = it.next();
		  feaStr += key +":" + treeMap.get(key) + " ";
	  }
	  /*  
	  for(int i=1; i<= feaMap.size(); i++){
		  feaStr += i+ ":" +feaMap.get(i) + " ";
	  }
	  */
  
	  featureWriter.write(degree + " "
			  		+ "qid:" + queryID + " "
			  		+ feaStr
			  		+ "# " + extid
			  		+"\n"
			  		); 
  }
  /*
  public static void writedocFeaMap(int queryID,Map<String, Integer> docsMap,Map< String, Map< Integer, Double >> docFeaMap,BufferedWriter featureWriter ) throws Exception{
	  for (Entry<String, Map<Integer, Double>> entry : docFeaMap.entrySet() ) {
		  String extid = entry.getKey();
		  //System.out.print(entry.getKey() + "\n");
		  Map<Integer, Double> feaMap = new HashMap<Integer, Double>();
		  feaMap = entry.getValue();
		  writeFeaVec(docsMap.get(extid), queryID, feaMap, extid,featureWriter);
	  }	  
		  
  }
  */
  public static void writedocFeaMap(int queryID,Map<String, Integer> docsMap,Map< String, Map< Integer, Double >> docFeaMap,BufferedWriter featureWriter ) throws Exception{
	  for (Entry<String, Map<Integer, Double>> entry : docFeaMap.entrySet() ) {
		  String extid = entry.getKey();
		  //System.out.print(entry.getKey() + "\n");
		  Map<Integer, Double> feaMap = new HashMap<Integer, Double>();
		  feaMap = entry.getValue();
		  writeFeaVec(docsMap.get(extid), queryID, feaMap, extid,featureWriter);
	  }	  
		  
  }
  
  public static void writedocFeaMapToSepcificFile(Map<String, Integer> docsMap,Map< String, Map< Integer, Double >> docFeaMap ) throws Exception{
	  BufferedWriter docMapWriter = new BufferedWriter(new FileWriter(new File("docFeaMap")));
	  Map<Integer, Double> feaMap = new HashMap<Integer, Double>();
	  
	  for (Entry<String, Map<Integer, Double>> entry : docFeaMap.entrySet() ) {
		  docMapWriter.write(entry.getKey() + "\n");
		  feaMap = entry.getValue();
		  for (Entry<Integer, Double> feaEntry : feaMap.entrySet() ) {
			  docMapWriter.write("" + feaEntry.getKey() + ":");
			  docMapWriter.write(""+ feaEntry.getValue() + ",");
		  }
		  docMapWriter.write("\n");
	  }
	  docMapWriter.close();
  }
  public static void printdocFeaMap(Map< String, Map< Integer, Double >> docFeaMap ) throws Exception{
	  Map<Integer, Double> feaMap = new HashMap<Integer, Double>();
	  
	  for (Entry<String, Map<Integer, Double>> entry : docFeaMap.entrySet() ) {
		  
		  System.out.print(entry.getKey() + "\n");
		  feaMap = entry.getValue();
		  //feaMap.putAll(entry.getValue());
		  for (Entry<Integer, Double> feaEntry : feaMap.entrySet() ) {
			  System.out.print("" + feaEntry.getKey() + ":");
			  System.out.print(""+ feaEntry.getValue() + ",");
		  }
		  System.out.print("\n");
	  }
  }
  
  public static void printminMaxMap(Map<Integer, ArrayList<Double>> minMaxMap){
	  System.out.println("print minMaxMap");
	  for (Entry<Integer, ArrayList<Double>> entry : minMaxMap.entrySet() ) {
		  int feature = entry.getKey();
		  System.out.print("feature:"+ entry.getKey() + " min and max is: ");
		  
		  for (int i=0;i< entry.getValue().size();i++){
			  System.out.print(entry.getValue().get(i) + " ");
		  }
		  System.out.println();
	  }
  }
  
  public static void printScoreListMap(Map< Integer,ArrayList<Double>>feaScoreListMap,int feaCnt){
	  
	  for(int k=1; k<=feaCnt ;k++){
		  System.out.println("kth fea:"+ k + " and its scores:");
		  for (int i=0;i< feaScoreListMap.get(k).size();i++){
			  System.out.print(feaScoreListMap.get(k).get(i) + "," );
		  }
		  System.out.println();
	  }
  }
  public static void printDocsMap(Map<String,Integer> docsMap){
	  for (Entry<String, Integer> entry : docsMap.entrySet() ) {
		  debugOut(entry.getKey() + ":" +entry.getValue());
	  }
  }
  
  public static Map<Integer,  Map<String, Integer> > getAllDocsMapFromQrelsFile(Map<String, String> params) throws Exception{
	  File relsFile = new File(params.get("letor:trainingQrelsFile"));
	  BufferedReader fileReader = new BufferedReader(new FileReader(relsFile));
	  String line = null ;
	  Map<Integer, Integer> queryIDMap = new HashMap<Integer, Integer>();
	  // <queryID,Map>
	  Map<Integer,  Map<String, Integer > > allDocsMap = new HashMap<Integer,  Map<String, Integer> >();
	  while ((line = fileReader.readLine()) != null ) {
		  String[] items = line.split(" ");
		  int queryID = Integer.parseInt( items[0].trim() );
		  if( !queryIDMap.containsKey(queryID)) {
			  queryIDMap.put(queryID, 0);
			  allDocsMap.put(queryID, getDocsMapFromQrelsFile(params, queryID) );
		  }
	  }
	  fileReader.close();
	  return allDocsMap;
  }			 
			  
  public static Map<String, Integer>  getDocsMapFromQrelsFile(Map<String, String> params,int queryID) throws Exception{
	  File relsFile = new File(params.get("letor:trainingQrelsFile"));
	  BufferedReader fileReader = new BufferedReader(new FileReader(relsFile));
	  String line = null;
	
	  //< doc internal id, degree of relevance>
	  Map<String, Integer> docsMap = new HashMap<String, Integer>();
	 
	  while ((line = fileReader.readLine()) != null) {
		  if(QryEval.relsFile_DEBUG) System.out.println(line);
		  //line = 2 0 clueweb09-en0000-02-03513 0
		  String[] items = line.split(" ");
		  if ( Integer.parseInt( items[0].trim() )  == queryID){
			  if(relsFile_DEBUG) {
				  System.out.println("queryID:"+queryID);
				  System.out.println("relsFile line: "+line);
			  }
			  
			  // Extract ext docid and its degree of relevance
			  //int docid = getInternalDocid( items[2].trim() ); // Ext id to internal id
			  //docsMap.put(0, 0.2);
			  docsMap.put(items[2].trim(), Integer.parseInt(items[3].trim()) );
			  //printDocs(docsMap);
		  }
		  
	  }
	  fileReader.close();
	  return docsMap;
  }
  public static Map<String, Integer>  getDocsMapFromInitialRanking(Map<String, String> params,int queryID) throws Exception{
	  File relsFile = new File(params.get("trecEvalOutputPath"));
	  BufferedReader fileReader = new BufferedReader(new FileReader(relsFile));
	  String line = null;
	
	  //< doc internal id, degree of relevance>
	  Map<String, Integer> docsMap = new HashMap<String, Integer>();
	 
	  while ((line = fileReader.readLine()) != null) {
		  if(QryEval.relsFile_DEBUG) System.out.println(line);
		  //line = 2 0 clueweb09-en0000-02-03513 0
		  // 10 Q0 clueweb09-en0005-08-29722 1 1.878010680000 yubinletor
		  String[] items = line.split(" ");
		  if ( Integer.parseInt( items[0].trim() )  == queryID){
			  if(relsFile_DEBUG) {
				  System.out.println("queryID:"+queryID);
				  System.out.println("relsFile line: "+line);
			  }
			  
			  // Extract ext docid and its degree of relevance
			  //int docid = getInternalDocid( items[2].trim() ); // Ext id to internal id
			  //docsMap.put(0, 0.2);
			  docsMap.put(items[2].trim(), 0);
			  //printDocs(docsMap);
		  }
		  
	  }
	  fileReader.close();
	  return docsMap;
  }
	  
  
  
  /*
   * Expand query,write to a file.
   * Create a combined query and use combining query to retrieval.
   * Do it for all queries in a file.
  */
  public static void expQueryRetrieval(Map<String, String> params, File rankingFile, RetrievalModel model) throws Exception{
	  BufferedWriter expQwriter =  new BufferedWriter(new FileWriter(new File(params.get("fbExpansionQueryFile"))));
	  File qryFile = new File(params.get("queryFilePath"));
	  BufferedReader qryReader = new BufferedReader(new FileReader(qryFile));
	    String tempString = null;
	    // Get all the topN document Map for all queries, every queryID has a Map which contains the 
	    // topN docs for this query.
	    Map<Integer,  Map<Integer, Double> > allDocsMap = getAllDocsMap(params, rankingFile);
	    
	    writer = new BufferedWriter(new FileWriter(new File(params.get("trecEvalOutputPath"))));
	    int queryID = 0;

	    while ((tempString = qryReader.readLine()) != null) {

	    	tempString = tempString.trim();
	    	if (tempString.equals("")){
	    		System.err.println("Query cann't be empty!");
	    		continue;
	    	}	 
	    	//System.out.println("orig query:"+ tempString);
	    	int idxSep = tempString.indexOf(':');
	    	String qString = tempString;
			if (idxSep != -1){ 
				String numStr = tempString.substring(0,idxSep).trim();
				queryID = Integer.parseInt(numStr);
				//System.out.println("nums:"+numStr+ "queryID:"+ queryID);
	    	
				qString = tempString.substring(idxSep+1); 
				//System.out.println("After extracting lineNum, qString="+ qString);
			}
		    qString = qString.trim();
		    // Add default operator 
		    qString = addDefOpt(params, qString);
		    
		    //Map<Integer, Double> docsMap = getDocsMap(params,qString,queryID, rankingFile);
		    Map<Integer, Double> docsMap  = allDocsMap.get(queryID);
			String expQuery = getExpQuery( params, docsMap);
			//System.out.println("expQuery:"+ expQuery);
			
			//write the expanded query to a file specified by the fbExpansionQueryFile= parameter
	    	expQwriter.write( queryID + ": "+ expQuery+ "\n");
	    	
	    	// Create a combined query and use combining query to retrieval
	    	Double origQueWgt = getDigitWgt( params.get("fbOrigWeight") );
	    	String combQuery = queryID +":#wand( " + params.get("fbOrigWeight") + " " + qString + " " +
	    	( 1 - origQueWgt) + " " + expQuery + ")"; 
	    	//System.out.println("combined query:"+ combQuery);
	    	retrievalForOneQuery(params, model, combQuery);	
	    }
	  writer.close();  
	  expQwriter.close();
	  qryReader.close();
  }
  public static Map<Integer,  Map<Integer, Double> > getAllDocsMap(Map<String, String> params, File rankingFile) throws Exception{
	  BufferedReader fileReader = new BufferedReader(new FileReader(rankingFile));
	  String line = null ;
	  Map<Integer, Integer> queryIDMap = new HashMap<Integer, Integer>();
	  // <queryID,Map>
	  Map<Integer,  Map<Integer, Double> > allDocsMap = new HashMap<Integer,  Map<Integer, Double> >();
	  while ((line = fileReader.readLine()) != null ) {
		  String[] items = line.split(" ");
		  int queryID = Integer.parseInt( items[0].trim() );
		  if( !queryIDMap.containsKey(queryID)) {
			  queryIDMap.put(queryID, 0);
			  allDocsMap.put(queryID, getDocsMap(params, queryID, rankingFile) );
		  }
	  }
	  fileReader.close();
	  return allDocsMap;
  }
  
  public static Map<Integer, Double>  getDocsMap(Map<String, String> params,int queryID, File rankingFile) throws Exception{
	  BufferedReader fileReader = new BufferedReader(new FileReader(rankingFile));
	  String line = null;
	  /*
	  use the Indri query expansion algorithm (Lecture 11, slides #23-28) to produce an expanded query;
	  write the expanded query to a file specified by the fbExpansionQueryFile= parameter (the format is below);
	  create a combined query as #wand (w qoriginal + (1-w) qexpandedquery);
	  use the combined query to retrieve documents;
	  */
	  int fbDocs = Integer.parseInt( params.get("fbDocs") ); //topN docs

	  Map<Integer, Double> docsMap = new HashMap<Integer, Double>();
	  int i = 1;
	  while ((line = fileReader.readLine()) != null && i <= fbDocs ) {
		  if(QryEval.exp_DEBUG) System.out.println(line);
		  //line = "10 Q0 clueweb09-en0001-18-32681 1 0.081073555377 reference-bow" ;
		  String[] items = line.split(" ");
		  if ( Integer.parseInt( items[0].trim() )  == queryID){
			  if(docsMap_DEBUG) {
				  System.out.println("queryID:"+queryID);
				  System.out.println("ranking Fiel line: "+line);
			  }
			  
			  // Extract ext docid and its score
			  // 10 Q0 clueweb09-en0001-18-32681 1 0.081073555377 reference-bow
			  int docid = getInternalDocid( items[2].trim() ); // Ext id to internal id
			  //docsMap.put(0, 0.2);
			  docsMap.put(docid, Double.parseDouble(items[4].trim()) );
			  //printDocs(docsMap);
			  i++;
		  }
		  
	  }
	  fileReader.close();
	  return docsMap;

  }
  
  /*
   *   use the Indri query expansion algorithm (Lecture 11, slides #23-28) to produce an expanded query;
  write the expanded query to a file specified by the fbExpansionQueryFile= parameter (the format is below);
  create a combined query as #wand (w qoriginal + (1-w) qexpandedquery);
  use the combined query to retrieve documents;
   */
  public static String getExpQuery( Map<String, String> params, Map<Integer, Double> docsMap) throws Exception{
	  if(exp_DEBUG) System.out.println("getExpQuery begin.");
	  Map<String, Double> vocMap = new HashMap<String, Double>();
	  int fbTerms = Integer.parseInt( params.get("fbTerms") ); //topN terms
	  int fbMu = Integer.parseInt( params.get("fbMu") );
	  
	  // Check each doc, add the unique term to the vocMap, this is the vocabulary
	  // Initilize the score to 0.0
	  for (Entry<Integer, Double> entry : docsMap.entrySet()){
		  TermVector tv = new TermVector (entry.getKey(), "body");
		  //1. "apple" is the stem of  term "apples"
		  //2. If a document is like "Apple, apples, apples!". This doc has 3 terms, thus its positionsLength is 3. 
		  //But it has only 1 stem "apple", so its stemsLength is 1.
		  long len=tv.stemsLength();
		  for (int i = 1; i < len ;i++ ){
			  String stem = tv.stemString(i);
			  //ignore any candidate expansion term that contains a period ('.') or a comma (',')
			  if(stem == null) continue;
			  if (-1 != stem.indexOf('.') || -1 != stem.indexOf(',') )  continue; 
			  vocMap.put(stem, 0.0); // Get the vocabulary,store to vocMap
		  }
	  }
	  
	  long C_len = READER.getSumTotalTermFreq("body");
	  DocLengthStore s = new DocLengthStore(READER);
	  long doclen = 0;
	  
	  // Cacualte the term score of a doc,add to the term's total socre
	  for (Entry<Integer, Double> entry : docsMap.entrySet()){
		  TermVector tv = new TermVector (entry.getKey(), "body");
		  doclen =  s.getDocLength("body", entry.getKey());
		  
		  // i is the index of the stem 
		  for (int i = 1; i < tv.stemsLength(); i++ ){
			  if(tv.stemString(i) == null) continue;
			  if (-1 != tv.stemString(i).indexOf('.') || -1 != tv.stemString(i).indexOf(',') )  continue; 
			  
			  Double p_MLE = (double) (tv.totalStemFreq(i) / C_len); //ctf/C
			  Double P_td = ( (double) tv.stemFreq(i) + fbMu * p_MLE ) / (doclen + fbMu);
			  Double curScore = P_td * entry.getValue() * Math.log( (double)C_len/ (double) (tv.totalStemFreq(i)) ) ;
			  Double newScore = vocMap.get( tv.stemString(i) ) + curScore; 
			  vocMap.put(tv.stemString(i), newScore); // replace the old score
		  }
	  }
	  // Find the topN terms in vocMap
	  //printVoc(vocMap, vocMap.size(),"vocMap.txt");
	    
	  return rankTermScore(vocMap, fbTerms);
  }
  /*
   * Rank the termScore, and return the topN 
   */
  public static String rankTermScore(Map< String, Double > map, int fbTerms) throws IOException{
	  
	  Comparator< Map.Entry<String,Double> > comparator = new Comparator< Map.Entry<String,Double> >(){
		  
	   public int compare(Map.Entry<String,Double > m , Map.Entry<String, Double> n ) {
		   if( m.getValue() > n.getValue() ) return -1;
		   else if ( m.getValue() < n.getValue() ) return 1;
		   else  return 0;
		   //return (int)( n.getValue() - m.getValue() );
	   }
	  };
	  List<Map.Entry<String,Double>> list = new ArrayList< Map.Entry<String,Double>>(map.entrySet());
	  Collections.sort(list, comparator);
	  //Map< String, Double > newMap = new HashMap< String, Double >() ;
	  ArrayList<String> topTermList = new ArrayList<String>();
	  
	  //printSortedList(list);
	  String expQuery = "#wand( ";
	  int i = 1;
		for(Iterator<Entry<String, Double>> it=list.iterator();it.hasNext();)
		{   
			if(i>fbTerms) break;
			Entry<String, Double> curit = it.next();
			topTermList.add(curit.getKey());
			expQuery += " " + curit.getValue() + " " + curit.getKey();
			//System.out.println(curit);
			//System.out.println(curit.getKey() );
			//System.out.println(curit.getValue() );
			//newMap.put(curit.getKey(), curit.getValue());
			i ++;
		}
		//printVoc(newMap, fbTerms);
		//System.out.println(newMap.keySet() );
		if(termSort_DEBUG) System.out.println(topTermList);
		expQuery += " )";
		
		return expQuery;
}  
  
	public static void testTermSort( ) throws IOException 
	{
		Map< String, Double > map = new HashMap< String, Double >() ;
		map.put("a",0.1);
		map.put("c",0.3);
		map.put("b",0.5);
		map.put("f",0.2);
		map.put("e",0.6);
		map.put("d",0.8);
		rankTermScore(map,3);
		//List<Map.Entry<String,Double>> list = new ArrayList< Map.Entry<String,Double>>(map.entrySet());
		//list.addAll(map.entrySet());
		//Test.ValueComparator vc=new ValueComparator();
	//	Collections.sort(list,vc);

	}

  public static int getQueryID(String tempString){
	int queryID = 0;
  	int idxSep = tempString.indexOf(':');
  	//String qString = tempString;
		if (idxSep != -1){ 
			String numStr = tempString.substring(0,idxSep).trim();
			queryID = Integer.parseInt(numStr);
			//System.out.println("nums:"+numStr+ "queryID:"+ queryID);
  	
			//qString = tempString.substring(idxSep+1); 
			//System.out.println("After extracting lineNum, qString="+ qString);
		}
		return queryID;
  }
  public static String getQuery(String tempString){
	String query = tempString;
  	int idxSep = tempString.indexOf(':');
  	if (idxSep != -1){ 
  		query = tempString.substring(idxSep+1); 
  	}
  	return query;

  }
	
  public static void printDocs(Map<Integer, Double> docsMap){
	  if(!QryEval.exp_DEBUG) return;
	  System.out.println("PRINT DOCS:");
	  int i = 1;
	  for (Entry<Integer, Double> entry : docsMap.entrySet()) {  
		    System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());  
		    i++;
		} 
  }
 // print the vocabulary
  public static void printVoc(Map<String, Double> vocMap, int num, String fileName) throws IOException{
	  if(!QryEval.exp_DEBUG) return;
	   
	  BufferedWriter vocWriter =  new BufferedWriter(new FileWriter(new File(fileName)));
	  
	  //System.out.println("PRINT VOCABULARYLY:");
	  vocWriter.write("vocubulary\n");
	  int i = 1;
	  for (Entry<String, Double> entry : vocMap.entrySet() ) {  
		    //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
		    vocWriter.write("Key = " + entry.getKey() + ", Value = " + entry.getValue() + "\n");
		    i++;
		    if(i>num) break;
		} 
	  vocWriter.close();
  } 
  
  public static void printSortedList( List<Map.Entry<String,Double>>  list) throws IOException{
	  BufferedWriter svocWriter =  new BufferedWriter(new FileWriter(new File("sortedvoc.txt")));
	  for(Iterator<Entry<String, Double>> it=list.iterator();it.hasNext();)
	  { 
		  Entry<String, Double> curit = it.next();
		  svocWriter.write( " " + curit.getKey() + " " + curit.getValue() + "\n");
	  }
	  svocWriter.close();
  }
  public static void printQuery( BufferedReader qryReader ){
	    String tempString = null;
	    int queryCnt = 0;
	    try {
			while ((tempString = qryReader.readLine()) != null) {
				queryCnt++;
				tempString = tempString.trim();
				if (tempString.equals("")){
					System.err.println("Query cann't be empty!");
					continue;
				}
				if(QryEval.SHOWQUERY) System.out.println(tempString);
			}
			if(QryEval.SHOWQUERY) System.out.println("queryCnt:" + queryCnt);
			qryReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  }
  
  public static void test(){
	  Test test = new Test();
	  test.testRank();
  }

  /**
   *  Write an error message and exit.  This can be done in other
   *  ways, but I wanted something that takes just one statement so
   *  that it is easy to insert checks without cluttering the code.
   *  @param message The error message to write before exiting.
   *  @return void
   */
  static void fatalError (String message) {
    System.err.println (message);
    System.exit(1);
  }

  /**
   *  Get the external document id for a document specified by an
   *  internal document id. If the internal id doesn't exists, returns null.
   *  
   * @param iid The internal document id of the document.
   * @throws IOException 
   */
  static String getExternalDocid (int iid) throws IOException {
    Document d = QryEval.READER.document (iid);
    String eid = d.get ("externalId");
    return eid;
  }

  /**
   *  Finds the internal document id for a document specified by its
   *  external id, e.g. clueweb09-enwp00-88-09710.  If no such
   *  document exists, it throws an exception. 
   * 
   * @param externalId The external document id of a document.s
   * @return An internal doc id suitable for finding document vectors etc.
   * @throws Exception
   */
  static int getInternalDocid (String externalId) throws Exception {
    Query q = new TermQuery(new Term("externalId", externalId));
    
    IndexSearcher searcher = new IndexSearcher(QryEval.READER);
    TopScoreDocCollector collector = TopScoreDocCollector.create(1,false);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
    
    if (hits.length < 1) {
      throw new Exception("External id not found.");
    } else {
      return hits[0].doc;
    }
  }

  
  /*
  public static Qryop parseWgtQuery(String qString) throws Exception{
	   Qryop currentOp = null;
	   StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
	   String token = null;
	   Double wgt = 1.0;
	   
	   token = tokens.nextToken();// To ignore the #and
	   Qryop rootOp = new QryopSlWAnd();
	   while (tokens.hasMoreTokens()) {
		   token = tokens.nextToken();
		   if (token.matches("[ ,(\t\n\r]")) {
			   continue;
		   }
		   if( token.startsWith("#wand") )break;
	   }
	   //token = tokens.nextToken();
	   //tokens = stepTokens(tokens);\
	   while (tokens.hasMoreTokens()){
		   
		   while (tokens.hasMoreTokens()) {
		   token = tokens.nextToken();
		   if (token.matches("[ ,(\t\n\r]")) {
			   continue;
		   }
		   else break;
	  }
	   if(token.equalsIgnoreCase(")")) {return rootOp;}
	   wgt = getDigitWgt(token); // get the weight
	   System.out.println("wgt" + wgt);
	   while (tokens.hasMoreTokens()) {
		   token = tokens.nextToken();
		   if (token.matches("[ ,(\t\n\r]")) {
			   continue;
		   }
		   else break;
	  }
	   
	   currentOp = parseWgtSubQuery(tokens,token);
	   currentOp.setWeight(wgt);
       rootOp.add(currentOp);
	   }
       return rootOp;
       
  }
  
  public static StringTokenizer stepTokens(StringTokenizer tokens){
	  String token = null;
  while (tokens.hasMoreTokens()) {
	   token = tokens.nextToken();
	   if (token.matches("[ ,(\t\n\r]")) {
		   continue;
	   }
	   else break;
  }
  return tokens;
  }
  public static Qryop parseWgtSubQuery(StringTokenizer tokens, String curToken) throws Exception{
	   Qryop currentOp = null;
	   int distance;
	    String[] tokenProcArr ;
	    Stack<Qryop> stack = new Stack<Qryop>();
	    int termCnt = 0;

	    // Tokenize the query.
	    //StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
	    String token = null;
	    boolean flag = false;// For weighted queries
	    Double wgt = 1.0;
	    if(!curToken.startsWith ("#")){
	    	token = curToken;
	    	// Strings are the terms and their fields
	    	String term = token; // If has field, need to modified
	    	String field = "body";// Default is body
	    	if (-1 != token.indexOf('.') ){ // Has field
	    		int idx = token.indexOf('.');
	    		// Get term and field from the token
	    		term = token.substring(0,idx);
	    		field = token.substring(idx+1);
	    	} 		
	    		// Stop words will be deleted, so need to check
	    		tokenProcArr = tokenizeQuery(term);
	    		if (tokenProcArr.length == 0) {
	    			//if(!wgtStack.empty()) wgtStack.pop() ;
	    			;
	    			}
	    		// Not stop words
	    		if(qParser_DEBUG) {System.out.println( " term:"+term+" field:"+ field); }
	    		QryopIlTerm Termop = new QryopIlTerm( tokenProcArr[0], field );
	    		//Termop.setWeight(wgt);
	    		return  Termop;	
	    }
	     if (curToken.equalsIgnoreCase("#and")) {
	        currentOp = new QryopSlAnd();
	        stack.push(currentOp);
	      } else if (curToken.equalsIgnoreCase("#wand")) {
	          currentOp = new QryopSlWAnd();
	          stack.push(currentOp);
	          flag = true;
	      }   
	     
	    while (tokens.hasMoreTokens()) {
	    	token = tokens.nextToken();
	      if (token.matches("[ ,(\t\n\r]")) {
	    	  continue;
	      }
	     if (token.startsWith(")")) { // Finish current query operator.
	         stack.pop();
	         if (stack.empty())
	        	 return currentOp;;
	         Qryop arg = currentOp;
	         currentOp = stack.peek();
	         currentOp.add(arg);
	         continue;
	     }
	     
	     // Other tokens are either operators or terms, need to add weights
	     if (token.equalsIgnoreCase("#and")) {
	        currentOp = new QryopSlAnd();
	        stack.push(currentOp);
	      } else if (token.equalsIgnoreCase("#wand")) {
	          currentOp = new QryopSlWAnd();
	          stack.push(currentOp);
	          flag = true;
	      } else if (token.equalsIgnoreCase("#wsum")) {
	          currentOp = new QryopSlWSum();
	          stack.push(currentOp); 
	          flag = true;
	      } else if (token.equalsIgnoreCase("#or")) {
	          currentOp = new QryopSlOr();
	          stack.push(currentOp);  
	      } else if ( token.startsWith("#near")) {
	    	  distance = getDistance(token);
	          currentOp = new QryopIlNear(distance);
	          stack.push(currentOp);   
	      } else if (token.startsWith("#win")) {
	    	  distance = getDistance(token);
	          currentOp = new QryopIlWin(distance);
	          stack.push(currentOp);           
	      } else if (token.equalsIgnoreCase("#syn")) {
	        currentOp = new QryopIlSyn();
	        stack.push(currentOp);
	      } else if (token.equalsIgnoreCase("#sum")) {
	          currentOp = new QryopSlSum();
	          stack.push(currentOp);        
	      } else { // terms
	    	if(flag){
	    		wgt = getDigitWgt(token);
	    			   while (tokens.hasMoreTokens()) {
	    			   token = tokens.nextToken();
	    			   if (token.matches("[ ,(\t\n\r]")) {
	    				   continue;
	    			   }
	    			   else break;
	    		  }
	    		// token=tokens.nextToken();
	    	}
	    	// Strings are the terms and their fields
	    	String term = token; // If has field, need to modified
	    	String field = "body";// Default is body
	    	if (-1 != token.indexOf('.') ){ // Has field
	    		int idx = token.indexOf('.');
	    		// Get term and field from the token
	    		term = token.substring(0,idx);
	    		field = token.substring(idx+1);
	    	} 		
	    		// Stop words will be deleted, so need to check
	    		tokenProcArr = tokenizeQuery(term);
	    		if (tokenProcArr.length == 0) {
	    			//if(!wgtStack.empty()) wgtStack.pop() ;
	    			continue;
	    			}
	    		// Not stop words
	    		if(qParser_DEBUG) {System.out.println( " term:"+term+" field:"+ field); }
	    		QryopIlTerm Termop = new QryopIlTerm( tokenProcArr[0], field );
	    		if(flag) Termop.setWeight(wgt);
	    		currentOp.add( Termop);
	    		continue;
	      }
	    }
		return currentOp;
	 
  }
  */

  
  /*
   * change strWgt to digit.
   * But it might be int or doulbe, so need to check.
   */
  public static Double getDigitWgt(String strWgt){
	  //System.err.println("tset"+  Double.parseDouble("2") );
	  return Double.parseDouble(strWgt) ;
	  /*
	  if ( isDouble(strWgt) ) return Double.parseDouble(strWgt) ;
	  else if(isInteger(strWgt)){
		  Double digWgt = (double) Integer.parseInt(strWgt);
		  return  digWgt;
	  }
	  else {
		  System.err.println("getDigitWgt error,strWgt is not digit:"+ strWgt);
		  return 0.0;
	  }
	  */
  }
  
  public static boolean isNumeric(String str){
	  for (int i = str.length();--i>=0;){   
	   if (!Character.isDigit(str.charAt(i))){
	    return false;
	   }
	  }
	  return true;
	 }
  
  public static boolean isDouble(String str) {    
	    Pattern pattern = Pattern.compile("^[-\\+]?[.\\d]*$");    
	    return pattern.matcher(str).matches();    
	  } 
  
   public static boolean isInteger(String str) {  
     Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");  
     return pattern.matcher(str).matches();  
   }
  
  /* 
   * Exact the distance from the NEAR operator
   * in: NEAR token, such as "#NEAR/13"
   * out: Distance, such as 13
   */
  public static int getDistance(String token){
	    // Get the num from token
		int idxNumBeg = token.indexOf('/');
			
		String numStr = token.substring(idxNumBeg+1).trim();
		int distance = Integer.parseInt(numStr);
        if(QryEval.DEBUG){
	    	  System.out.println("nums:"+numStr+ "distance:"+ distance);
        }
	    	
	    return distance;
  }

  /**
   *  Print a message indicating the amount of memory used.  The
   *  caller can indicate whether garbage collection should be
   *  performed, which slows the program but reduces memory usage.
   *  @param gc If true, run the garbage collector before reporting.
   *  @return void
   */
  public static void printMemoryUsage (boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc) {
      runtime.gc();
    }

    System.out.println ("Memory used:  " +
			((runtime.totalMemory() - runtime.freeMemory()) /
			 (1024L * 1024L)) + " MB");
  }
  
  /**
   * Print the query results. 
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT.  YOU MUST CHANGE THIS
   * METHOD SO THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK
   * PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName Original query.
   * @param result Result object generated by {@link Qryop#evaluate()}.
   * @throws IOException
   */
  
  static void writeResults(String queryName,int queryID, QryResult result) throws IOException {
	  //BufferedWriter writer
	  if(QryEval.PRINTRESULT){
	    //System.out.println("queryName:"+ queryName);
	    System.out.println("queryID Q0 docid extid rank score run-1");
	  }
	    if (result.docScores.scores.size() < 1) {
	    	writer.write(queryID +" Q0"+" dummy 1 0 run-1\n" );
	    } else {
	    try {
	      result.docScores.rankResultScore();
	      
	      int printNum = result.docScores.scores.size();
	      if (printNum > 100) printNum = 100;
	      for (int i = 0; i < printNum; i++) {
	    	int rank = i + 1;
	        writer.write(queryID +" Q0 "
				   + getExternalDocid (result.docScores.getDocid(i))
				   //+ " docid"+ result.docScores.getDocid(i)
				   + " " + rank
				   + " " + result.docScores.getDocidScore(i)
	        	   + " " + "run-1\n"
	      		   );
	        if(QryEval.PRINTRESULT){
	        	
	        	System.out.println(queryID +" Q0 "
	        			+ result.docScores.getDocid(i)
					   + result.docScores.getDocextid(i)
					   + " " + rank
					   + " " + result.docScores.getDocidScore(i)
		        	   + " " + "run-1\n"	        			
	        			);
	        }
	      }
	    } catch (Exception e) {
	      e.printStackTrace();
	    } 
	    
	    }
  }
  
  static void printResults(String queryName,int queryID, QryResult result) throws IOException {
	  //BufferedWriter writer
	    System.out.println("Begin printResult.-----");
	    System.out.println("queryName:"+ queryName);
	    
	    if (result.docScores.scores.size() < 1) {
	      System.out.println("\tNo results.");
	      System.out.println(queryID +" Q0"+" dummy 1 0 run-1\n" );
	    } else {
	    try {
	      result.docScores.rankResultScore();
	      //writer.write("2 Q0 clueweb09-enwp00-70-20490 1 0.9 run-1");
	      int printNum = result.docScores.scores.size();
	      if (printNum > 100) printNum = 100;
	      for (int i = 0; i < printNum; i++) {
	    	int rank = i + 1;
	        System.out.println(queryID +" Q0 "
				   //+ getExternalDocid (result.docScores.getDocid(i))
	        	   + result.docScores.getDocid(i)
				   + " " + rank
				   + " " + result.docScores.getDocidScore(i)
	        	   + " " + "run-1\n"
	      		   );
	      }
	    } catch (Exception e) {
	      e.printStackTrace();
	    } 
	    }
	    System.out.println("End printResult.-----");
  }
  
  static void printResults_old(String queryName, QryResult result) throws IOException {
    
    System.out.println(queryName + ":  ");
    
    if (result.docScores.scores.size() < 1) {
      System.out.println("\tNo results.");
    } else {
      System.out.println("QueryID Q0 DocID Rank Score RunID");
      result.docScores.rankResultScore();
      
      for (int i = 0; i < result.docScores.scores.size(); i++) {
        System.out.println("\t" + i + ":  "
			   //+ getExternalDocid (result.docScores.getDocid(i))
        	   + result.docScores.getDocid(i)
			   + ", "
        	   + result.docScores.getDocidScore(i));
      }
    }
  }

  /**
   *  Given a query string, returns the terms one at a time with stopwords
   *  removed and the terms stemmed using the Krovetz stemmer. 
   * 
   *  Use this method to process raw query terms. 
   * 
   *  @param query String containing query
   *  @return Array of query tokens
   *  @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {

    TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }
  
  public static void printDbg(String s){
	  if(QryEval.DEBUG){
		  System.out.println(s);
	  }
  }
  public static void debugOut(String s){
	  System.out.println(s);
  }
}
