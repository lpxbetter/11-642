/**
 *  The ranked Boolean retrieval model has no parameters.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

public class RetrievalModelIndri extends RetrievalModel {
	public double mu = 2500;
	public double lambda = 0.4;
	public RetrievalModelIndri(double mu,double lambda){
			this.mu = mu;
			this.lambda = lambda;
		}

  /**
   * Set a retrieval model parameter.
   * @param parameterName
   * @param parametervalue
   * @return Always false because this retrieval model has no parameters.
   */
  public boolean setParameter (String parameterName, double value) {
    System.err.println ("Error: Unknown parameter name for retrieval model " +
			"Indri: " +
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
			"Indri: " +
			parameterName);
    return false;
  }

}
