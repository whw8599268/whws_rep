/**
 * 包名：com.sozone.eokb.bus.decrypt.service
 * 文件名：BidderElementParseUtils.java<br/>
 * 创建时间：2017-10-30 上午11:45:14<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt.service;

import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.dao.data.Record;

/**
 * 唱标要素解析工具类<br/>
 * <p>
 * 唱标要素解析工具类<br/>
 * </p>
 * Time：2017-10-30 上午11:45:14<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class BidderElementParseUtils
{

	/**
	 * 获取唱标信息中指定键的值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param key
	 *            键
	 * @param sings
	 *            唱标信息
	 * @return 值
	 */
	public static String getSingObjAttribute(String key,
			Record<String, Object> sings)
	{

		JSONArray list = sings.getColumn("objSing");
		JSONObject obj = null;
		for (int i = 0; i < list.size(); i++)
		{
			obj = list.getJSONObject(i);
			for (Entry<String, Object> entry : obj.entrySet())
			{
				if (StringUtils.equals(entry.getKey(), key))
				{
					return obj.getString(entry.getKey());
				}
			}
		}
		return null;
	}

	/**
	 * 获取唱标信息中指定键的值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param json
	 *            唱标信息JSON字符串
	 * @param key
	 *            键
	 * @return 值
	 */
	public static String getSingObjAttribute(String json, String key)
	{
		JSONObject jobj = JSON.parseObject(json);
		JSONArray sings = jobj.getJSONArray("objSing");
		JSONObject sing = null;
		if (sings == null)
		{
			return "";
		}
		for (int i = 0; i < sings.size(); i++)
		{
			sing = sings.getJSONObject(i);
			for (Entry<String, Object> entry : sing.entrySet())
			{
				if (StringUtils.equals(entry.getKey(), key))
				{
					return sing.getString(entry.getKey());
				}
			}
		}
		return "";
	}

	/**
	 * 获取唱标信息中指定键的值和<br/>
	 * <p>
	 * </p>
	 * 
	 * @param json
	 *            唱标信息JSON字符串
	 * @param key
	 *            键
	 * @return 值
	 */
	public static String getSingObjAttributeSum(String json, String key)
	{
		JSONObject jobj = JSON.parseObject(json);
		JSONArray sings = jobj.getJSONArray("objSing");
		JSONObject sing = null;
		String g = "0";
		float g1 = 0;
		boolean flag = false;
		if (sings == null)
		{
			return g;
		}
		for (int i = 0; i < sings.size(); i++)
		{
			sing = sings.getJSONObject(i);
			int key_size = 0;// 判断是否多个key出现
			for (Entry<String, Object> entry : sing.entrySet())
			{
				if (StringUtils.indexOf(entry.getKey(), key) >= 0)
				{
					if (key_size > 0)
					{
						flag = true;
						if (g1 == 0)
						{
							g1 = Float.parseFloat(g)
									+ sing.getFloat(entry.getKey());
						}
						else
						{
							g1 += sing.getFloat(entry.getKey());
						}
					}
					else
					{
						g = sing.getString(entry.getKey());
					}
					key_size++;
				}
			}
		}
		if (flag == false)
		{
			return g;
		}
		else
		{
			return g1 + "";
		}

	}

	public static void main(String[] args)
	{
		String s = "{ \"firstName\": \"Brett\", \"lastName\":\"McLaughlin\", \"email\": \"aaaa\" }";
		JSONObject json = JSON.parseObject(s);
		System.out.println(json.containsKey("firstName"));
	}
}
