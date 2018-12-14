/**
 * 
 */
package com.sozone.eokb.bus.push;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * ba文件验证工具<br/>
 * Time：2018-01-02<br/>
 * 
 * @author hyc
 * @version 1.0.0
 * @since 1.0.0
 */
public class BaValidationKit
{
	public static void main(String[] args)
	{
		String baFilePath = "D:/FileWS/海迈/test.ba";
		String xmlFilePath = "C:/Users/YSZY/Desktop/f94dd95a68e34e12823f47f5eda4bfc5.xml";
		// System.out.println(validationMD5(baFilePath, xmlFilePath));
		System.out.println(getPrices(xmlFilePath));

		System.out.println(getStringMD5("SZTEST001"));
		// 8F1E390719F7084E152428D22D25C85B
		// 8f1e390719f7084e152428d22d25c85b
		// 20180103165123
	}

	/**
	 * 获取控制价，暂列金额总和，专业工程暂估价总和<br/>
	 * 
	 * @param xmlFilePath
	 *            xml文件路径
	 * @return Map<String, Double> <br/>
	 *         例子：{ZYGCZGJ=0.0, ZLJE=401050.0, GCZJ=1.825235718E7}
	 */
	public static Map<String, Double> getPrices(String xmlFilePath)
	{
		Map<String, Double> map = new HashMap<String, Double>();
		try
		{
			String xmlStr = FileUtils.readFileToString(new File(xmlFilePath),
					"UTF-8");
			Document parse = Jsoup.parse(xmlStr);
			Element gczjzcElement = parseElement(parse
					.getElementsByTag("GCZJZC"));
			String gczjPrice = gczjzcElement.attr("GCZJ");
			map.put("GCZJ", Double.parseDouble(gczjPrice));// 控制价
			Double zlje = 0D;
			Double zygczgj = 0D;

			Elements dxgcElements = parse.getElementsByTag("DXGC");
			for (int i = 0; null != dxgcElements && i < dxgcElements.size(); i++)
			{
				Element dxgcElement = dxgcElements.get(i);
				Elements dwgcElements = dxgcElement.getElementsByTag("DWGC");
				for (int j = 0; null != dwgcElements && j < dwgcElements.size(); j++)
				{
					Element dwgcElement = dwgcElements.get(j);
					Element qtxmfhzElement = parseElement(dwgcElement
							.getElementsByTag("QTXMFHZ"));
					Element zljeElement = parseElement(qtxmfhzElement
							.getElementsByAttributeValue("XMMC", "暂列金额"));
					String je = zljeElement.attr("JE");
					zlje += Double.parseDouble(je);

					// Element zygczgjElement = parseElement(qtxmfhzElement
					// .getElementsByAttributeValue("XMMC", "专业工程暂估价"));
					// je = zygczgjElement.attr("JE");
					// SB海迈的这个节点说允许为空
					zygczgj += getZYGCZGJ(qtxmfhzElement);
				}
			}
			map.put("ZLJE", zlje);// 暂列金额总和
			map.put("ZYGCZGJ", zygczgj);// 专业工程暂估价总和

		}
		catch (IOException e)
		{
			throw new RuntimeException("解析XML信息失败!", e);
		}
		return map;
	}

	/**
	 * 获取专业工程暂估价<br/>
	 * <p>
	 * </p>
	 * 
	 * @param qtxmfhz
	 * @return
	 */
	private static double getZYGCZGJ(Element qtxmfhz)
	{
		Elements elements = qtxmfhz.getElementsByAttributeValue("XMMC",
				"专业工程暂估价");
		if (null == elements || 1 != elements.size())
		{
			return 0d;
		}
		Element zygczgj = elements.get(0);
		return Double.valueOf(zygczgj.attr("JE"));
	}

	/**
	 * 验证ba文件的MD5，ba总造价的MD5<br/>
	 * 
	 * @param baFilePath
	 *            ba文件路径
	 * @param xmlFilePath
	 *            xml文件路径
	 * @return 验证是否成功 true/false
	 */
	public static boolean validationMD5(String baFilePath, String xmlFilePath)
	{
		try
		{
			String baFileMD5 = getFileMD5(baFilePath);
			String xmlStr = FileUtils.readFileToString(new File(xmlFilePath),
					"UTF-8");
			Document parse = Jsoup.parse(xmlStr);
			Element baFileElement = parseElement(parse
					.getElementsByAttributeValue("TZMC", "BA文件"));
			String baFileXmlMD5 = baFileElement.attr("TZNR");
			if (!StringUtils.equalsIgnoreCase(baFileMD5, baFileXmlMD5))
			{
				return false;
			}

			Element baPriceElement = parseElement(parse
					.getElementsByAttributeValue("TZMC", "BA总造价"));
			String baPriceXmlMD5 = baPriceElement.attr("TZNR");
			Element gczjzcElement = parseElement(parse
					.getElementsByTag("GCZJZC"));
			String gczjPrice = gczjzcElement.attr("GCZJ");
			String gczjPriceMD5 = getStringMD5(gczjPrice);
			if (!StringUtils.equalsIgnoreCase(baPriceXmlMD5, gczjPriceMD5))
			{
				return false;
			}
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}

	/**
	 * 转换成单个Element<br/>
	 * 
	 * @param elements
	 *            Elements
	 * @return Element
	 */
	private static Element parseElement(Elements elements)
	{
		if (null == elements || 1 != elements.size())
		{
			throw new RuntimeException("转单个元素失败!");
		}
		Element element = elements.get(0);
		return element;
	}

	/**
	 * 获取文件的MD5值<br/>
	 * 
	 * @param fileName
	 *            文件路径
	 * @return MD5 值
	 * @throws Exception
	 *             异常
	 */
	private static String getFileMD5(String fileName) throws Exception
	{
		FileInputStream input = new FileInputStream(fileName);
		DigestInputStream dis = null;
		MessageDigest digest = null;
		try
		{
			digest = MessageDigest.getInstance("MD5");
			dis = new DigestInputStream(input, digest);
			byte[] buffer = new byte[1024 * 1024 * 2];
			int n = 0;
			while (-1 != (n = dis.read(buffer)))
			{
			}
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(dis);
		}
		finally
		{
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(dis);
		}
		return new String(Hex.encodeHex(dis.getMessageDigest().digest()))
				.toUpperCase();
	}

	/**
	 * 
	 * 获取文件MD5值<br/>
	 * 
	 * @param file
	 *            文件
	 * @return 文件的MD5值
	 */
	private static String getStringMD5(String str)
	{
		byte[] secretBytes = null;
		try
		{

			secretBytes = MessageDigest.getInstance("md5").digest(
					str.getBytes());
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException("error happens", e);
		}
		return new String(Hex.encodeHex(secretBytes)).toUpperCase();
	}
}