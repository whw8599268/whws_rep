
package com.ws.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService(name = "ScoreServiceSoap", targetNamespace = "http://tempuri.org/")
@SOAPBinding(use = SOAPBinding.Use.LITERAL, parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public interface ScoreServiceSoap {

	@WebMethod(operationName = "CompanyScoreInfoToJason", action = "http://tempuri.org/CompanyScoreInfoToJason")
	@WebResult(name = "CompanyScoreInfoToJasonResult", targetNamespace = "http://tempuri.org/")
	public String companyScoreInfoToJason(
			@WebParam(name = "ScoreType", targetNamespace = "http://tempuri.org/") String ScoreType,
			@WebParam(name = "Year", targetNamespace = "http://tempuri.org/") String Year,
			@WebParam(name = "Quarter", targetNamespace = "http://tempuri.org/") String Quarter,
			@WebParam(name = "OrgCode", targetNamespace = "http://tempuri.org/") String OrgCode);

	@WebMethod(operationName = "CompanyAvgScoreInfoToJason", action = "http://tempuri.org/CompanyAvgScoreInfoToJason")
	@WebResult(name = "CompanyAvgScoreInfoToJasonResult", targetNamespace = "http://tempuri.org/")
	public String companyAvgScoreInfoToJason(
			@WebParam(name = "ScoreType", targetNamespace = "http://tempuri.org/") String ScoreType,
			@WebParam(name = "Year", targetNamespace = "http://tempuri.org/") String Year,
			@WebParam(name = "Quarter", targetNamespace = "http://tempuri.org/") String Quarter);

	@WebMethod(operationName = "CompanyScoreInfo", action = "http://tempuri.org/CompanyScoreInfo")
	@WebResult(name = "CompanyScoreInfoResult", targetNamespace = "http://tempuri.org/")
	public String companyScoreInfo(
			@WebParam(name = "ScoreType", targetNamespace = "http://tempuri.org/") String ScoreType,
			@WebParam(name = "Year", targetNamespace = "http://tempuri.org/") String Year,
			@WebParam(name = "Quarter", targetNamespace = "http://tempuri.org/") String Quarter,
			@WebParam(name = "OrgCode", targetNamespace = "http://tempuri.org/") String OrgCode);

	@WebMethod(operationName = "CompanyAvgScoreInfo", action = "http://tempuri.org/CompanyAvgScoreInfo")
	@WebResult(name = "CompanyAvgScoreInfoResult", targetNamespace = "http://tempuri.org/")
	public String companyAvgScoreInfo(
			@WebParam(name = "ScoreType", targetNamespace = "http://tempuri.org/") String ScoreType,
			@WebParam(name = "Year", targetNamespace = "http://tempuri.org/") String Year,
			@WebParam(name = "Quarter", targetNamespace = "http://tempuri.org/") String Quarter);

}
