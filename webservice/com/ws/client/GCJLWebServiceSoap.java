package com.ws.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService(name = "GCJLWebServiceSoap", targetNamespace = "http://tempuri.org/")
@SOAPBinding(use = SOAPBinding.Use.LITERAL, parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public interface GCJLWebServiceSoap
{

	@WebMethod(operationName = "GetGCJL_YearScore", action = "http://tempuri.org/GetGCJL_YearScore")
	@WebResult(name = "GetGCJL_YearScoreResult", targetNamespace = "http://tempuri.org/")
	public String getGCJL_YearScore(
			@WebParam(name = "year", targetNamespace = "http://tempuri.org/") String year,
			@WebParam(name = "organcode", targetNamespace = "http://tempuri.org/") String organcode);

	@WebMethod(operationName = "GetGCJL_QuarterScoreAvg", action = "http://tempuri.org/GetGCJL_QuarterScoreAvg")
	@WebResult(name = "GetGCJL_QuarterScoreAvgResult", targetNamespace = "http://tempuri.org/")
	public String getGCJL_QuarterScoreAvg(
			@WebParam(name = "year", targetNamespace = "http://tempuri.org/") String year,
			@WebParam(name = "quarter", targetNamespace = "http://tempuri.org/") String quarter);

	@WebMethod(operationName = "GetGCJL_QuarterScore", action = "http://tempuri.org/GetGCJL_QuarterScore")
	@WebResult(name = "GetGCJL_QuarterScoreResult", targetNamespace = "http://tempuri.org/")
	public String getGCJL_QuarterScore(
			@WebParam(name = "year", targetNamespace = "http://tempuri.org/") String year,
			@WebParam(name = "quarter", targetNamespace = "http://tempuri.org/") String quarter,
			@WebParam(name = "organcode", targetNamespace = "http://tempuri.org/") String organcode);

	@WebMethod(operationName = "HelloWorld", action = "http://tempuri.org/HelloWorld")
	@WebResult(name = "HelloWorldResult", targetNamespace = "http://tempuri.org/")
	public String helloWorld();

}
