/**
 *  The ranked Boolean retrieval model has no parameters.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

public class RetrievalModelBM25 extends RetrievalModel {
	public double k1=1.2;
	public double b=0.75;
	public double k3=0.0;

	public RetrievalModelBM25(double k1,double b, double k3){
		this.k1=k1;
		this.b=b;
		this.k3=k3;
	}
	
  /**
   * Set a retrieval model parameter.
   * @param parameterName
   * @param parametervalue
   * @return Always false because this retrieval model has no parameters.
   */
  public boolean setParameter (String parameterName, double value) {
    System.err.println ("Error: Unknown parameter name for retrieval model " +
			"BM25: " +
			parameterName);
    return false;
  }

  /**
   * Set a retrieval model parameter.
   * @param parameterName
   * @param parametervalue
   * @return Always false because this retrieval model has no parameters.
   */
  public boolean setParameter (String parameterName, String value) {
    System.err.println ("Error: Unknown parameter name for retrieval model " +
			"BM25: " +
			parameterName);
    return false;
  }

}
