package br.edu.univercidade.cc.xithcluster.configuration;


public class InvalidParameterException extends BadParameterException {
	
	private static final long serialVersionUID = 1L;
	
	public InvalidParameterException(String parameterName) {
		super(parameterName);
	}
	
	@Override
	public String getMessage() {
		return "Invalid parameter: " + parameterName;
	}
}
