package com.ws.client;

import javax.jws.WebService;

@WebService(serviceName = "GCJLWebService", targetNamespace = "http://tempuri.org/", endpointInterface = "com.ws.client.GCJLWebServiceSoap")
public class GCJLWebServiceImpl implements GCJLWebServiceSoap
{

	public String getGCJL_YearScore(String year, String organcode)
	{
		throw new UnsupportedOperationException();
	}

	public String getGCJL_QuarterScoreAvg(String year, String quarter)
	{
		throw new UnsupportedOperationException();
	}

	public String getGCJL_QuarterScore(String year, String quarter,
			String organcode)
	{
		throw new UnsupportedOperationException();
	}

	public String helloWorld()
	{
		throw new UnsupportedOperationException();
	}

}
