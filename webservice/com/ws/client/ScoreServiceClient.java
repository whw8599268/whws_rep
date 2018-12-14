package com.ws.client;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.aegis.AegisBindingProvider;
import org.codehaus.xfire.annotations.AnnotationServiceFactory;
import org.codehaus.xfire.annotations.jsr181.Jsr181WebAnnotations;
import org.codehaus.xfire.client.XFireProxyFactory;
import org.codehaus.xfire.jaxb2.JaxbTypeRegistry;
import org.codehaus.xfire.service.Endpoint;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.transport.TransportManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.sozone.aeolus.dao.data.Record;

public class ScoreServiceClient
{

	private static XFireProxyFactory proxyFactory = new XFireProxyFactory();
	private Map<QName, Endpoint> endpoints = new HashMap<QName, Endpoint>();
	private Service service0;

	public ScoreServiceClient()
	{
		create0();
		Endpoint ScoreServiceSoapLocalEndpointEP = service0.addEndpoint(
				new QName("http://tempuri.org/",
						"ScoreServiceSoapLocalEndpoint"), new QName(
						"http://tempuri.org/", "ScoreServiceSoapLocalBinding"),
				"xfire.local://ScoreService");
		endpoints.put(new QName("http://tempuri.org/",
				"ScoreServiceSoapLocalEndpoint"),
				ScoreServiceSoapLocalEndpointEP);
		Endpoint ScoreServiceSoapEP = service0.addEndpoint(new QName(
				"http://tempuri.org/", "ScoreServiceSoap"), new QName(
				"http://tempuri.org/", "ScoreServiceSoap"),
				"http://120.35.29.43:98/zjxypj/WebServices/ScoreService.asmx");
		endpoints.put(new QName("http://tempuri.org/", "ScoreServiceSoap"),
				ScoreServiceSoapEP);
	}

	public Object getEndpoint(Endpoint endpoint)
	{
		try
		{
			return proxyFactory.create((endpoint).getBinding(),
					(endpoint).getUrl());
		}
		catch (MalformedURLException e)
		{
			throw new XFireRuntimeException("Invalid URL", e);
		}
	}

	public Object getEndpoint(QName name)
	{
		Endpoint endpoint = ((Endpoint) endpoints.get((name)));
		if ((endpoint) == null)
		{
			throw new IllegalStateException("No such endpoint!");
		}
		return getEndpoint((endpoint));
	}

	public Collection<Endpoint> getEndpoints()
	{
		return endpoints.values();
	}

	private void create0()
	{
		TransportManager tm = (org.codehaus.xfire.XFireFactory.newInstance()
				.getXFire().getTransportManager());
		Map<String, Boolean> props = new HashMap<String, Boolean>();
		props.put("annotations.allow.interface", true);
		AnnotationServiceFactory asf = new AnnotationServiceFactory(
				new Jsr181WebAnnotations(), tm, new AegisBindingProvider(
						new JaxbTypeRegistry()));
		asf.setBindingCreationEnabled(false);
		service0 = asf.create((ScoreServiceSoap.class), props);
		asf.createSoap11Binding(service0, new QName("http://tempuri.org/",
				"ScoreServiceSoapLocalBinding"), "urn:xfire:transport:local");
		asf.createSoap11Binding(service0, new QName("http://tempuri.org/",
				"ScoreServiceSoap"), "http://schemas.xmlsoap.org/soap/http");
	}

	public ScoreServiceSoap getScoreServiceSoapLocalEndpoint()
	{
		return ((ScoreServiceSoap) (this).getEndpoint(new QName(
				"http://tempuri.org/", "ScoreServiceSoapLocalEndpoint")));
	}

	public ScoreServiceSoap getScoreServiceSoapLocalEndpoint(String url)
	{
		ScoreServiceSoap var = getScoreServiceSoapLocalEndpoint();
		org.codehaus.xfire.client.Client.getInstance(var).setUrl(url);
		return var;
	}

	public ScoreServiceSoap getScoreServiceSoap()
	{
		return ((ScoreServiceSoap) (this).getEndpoint(new QName(
				"http://tempuri.org/", "ScoreServiceSoap")));
	}

	public ScoreServiceSoap getScoreServiceSoap(String url)
	{
		ScoreServiceSoap var = getScoreServiceSoap();
		org.codehaus.xfire.client.Client.getInstance(var).setUrl(url);
		return var;
	}

	public static void main(String[] args)
	{
		Record<String, Object> record = CreditScoreUtils.getYearAndQuarter();

		String year = record.getString("YEAR");
		String quarterName = record.getString("QUARTER_NAME");

		ScoreServiceClient client = new ScoreServiceClient();

		// create a default service endpoint
		ScoreServiceSoap service = client.getScoreServiceSoap();
		String str = service.companyScoreInfoToJason("房建", year, quarterName,
				"");
		JSONArray array = JSON.parseArray(str);
		System.out.println(array.size());
		for (int i = 0; i < array.size(); i++)
		{
			System.out.println(FieldMapping.transformToLocalFields(
					array.getJSONObject(i), "EKB_T_COMPANY_CREDIT_SCORE_INFO"));
		}

		System.out
				.println("----------------------调用获取平均分------------------------");
		long start = System.currentTimeMillis();
		str = service.companyAvgScoreInfoToJason("房建", year, quarterName);
		long end = System.currentTimeMillis();
		System.out.println("耗时：" + (end - start) + "毫秒!");
		array = JSON.parseArray(str);
		System.out.println(array.size());
		for (int i = 0; i < array.size(); i++)
		{
			System.out.println(FieldMapping.transformToLocalFields(
					array.getJSONObject(i),
					"EKB_T_INDUSTRY_AVG_CREDIT_SCORE_INFO"));
		}
		// Gson gson = new Gson();
		// List<Map<String, String>> list = gson.fromJson(str,
		// new TypeToken<List<Map<String, String>>>()
		// {
		// }.getType());
		// for (int i = 0; i < list.size(); i++)
		// {
		// Map<String, String> map = list.get(i);
		// Iterator<String> iterator = map.keySet().iterator();
		// while (iterator.hasNext())
		// {
		// String key = iterator.next();
		// System.out.print(key + ": " + map.get(key));
		// }
		// System.out.println();
		// }

		System.out.println("test client completed");
		System.exit(0);
	}

}
