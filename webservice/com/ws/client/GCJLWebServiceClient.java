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

public class GCJLWebServiceClient
{

	private static XFireProxyFactory proxyFactory = new XFireProxyFactory();
	private Map<QName, Endpoint> endpoints = new HashMap<QName, Endpoint>();
	private Service service2;

	public GCJLWebServiceClient()
	{
		create2();
		Endpoint gcjLWebServiceSoapEP = service2
				.addEndpoint(new QName("http://tempuri.org/",
						"GCJLWebServiceSoap"), new QName("http://tempuri.org/",
						"GCJLWebServiceSoap"),
						"http://www.fjjs.gov.cn:98/zjxypjweb2/WebServices/GCJLWebService.asmx");
		endpoints.put(new QName("http://tempuri.org/", "GCJLWebServiceSoap"),
				gcjLWebServiceSoapEP);
		Endpoint gcjlWebServiceSoapLocalEndpointEP = service2.addEndpoint(
				new QName("http://tempuri.org/",
						"GCJLWebServiceSoapLocalEndpoint"),
				new QName("http://tempuri.org/",
						"GCJLWebServiceSoapLocalBinding"),
				"xfire.local://GCJLWebService");
		endpoints.put(new QName("http://tempuri.org/",
				"GCJLWebServiceSoapLocalEndpoint"),
				gcjlWebServiceSoapLocalEndpointEP);
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

	private void create2()
	{
		TransportManager tm = (org.codehaus.xfire.XFireFactory.newInstance()
				.getXFire().getTransportManager());
		Map<String, Boolean> props = new HashMap<String, Boolean>();
		props.put("annotations.allow.interface", true);
		AnnotationServiceFactory asf = new AnnotationServiceFactory(
				new Jsr181WebAnnotations(), tm, new AegisBindingProvider(
						new JaxbTypeRegistry()));
		asf.setBindingCreationEnabled(false);
		service2 = asf.create(com.ws.client.GCJLWebServiceSoap.class, props);
		asf.createSoap11Binding(service2, new QName("http://tempuri.org/",
				"GCJLWebServiceSoap"), "http://schemas.xmlsoap.org/soap/http");
		asf.createSoap11Binding(service2, new QName("http://tempuri.org/",
				"GCJLWebServiceSoapLocalBinding"), "urn:xfire:transport:local");
	}

	public GCJLWebServiceSoap getGCJLWebServiceSoap()
	{
		return ((GCJLWebServiceSoap) (this).getEndpoint(new QName(
				"http://tempuri.org/", "GCJLWebServiceSoap")));
	}

	public GCJLWebServiceSoap getGCJLWebServiceSoap(String url)
	{
		GCJLWebServiceSoap var = getGCJLWebServiceSoap();
		org.codehaus.xfire.client.Client.getInstance(var).setUrl(url);
		return var;
	}

	public GCJLWebServiceSoap getGCJLWebServiceSoapLocalEndpoint()
	{
		return ((GCJLWebServiceSoap) (this).getEndpoint(new QName(
				"http://tempuri.org/", "GCJLWebServiceSoapLocalEndpoint")));
	}

	public GCJLWebServiceSoap getGCJLWebServiceSoapLocalEndpoint(String url)
	{
		GCJLWebServiceSoap var = getGCJLWebServiceSoapLocalEndpoint();
		org.codehaus.xfire.client.Client.getInstance(var).setUrl(url);
		return var;
	}

	public static void main(String[] args)
	{

		GCJLWebServiceClient client = new GCJLWebServiceClient();
		GCJLWebServiceSoap service = client.getGCJLWebServiceSoap();

		String str = service.getGCJL_QuarterScoreAvg("2018", "2");
		System.out.println(str);

		str = service.getGCJL_QuarterScore("2018", "2", null);
		System.out.println(str);

		System.out.println("test client completed");
		System.exit(0);
	}

}
