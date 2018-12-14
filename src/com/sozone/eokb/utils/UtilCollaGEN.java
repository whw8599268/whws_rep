package com.sozone.eokb.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sozone.aeolus.exception.ServiceException;
import com.sunsharing.collagen.data_item.Item;
import com.sunsharing.collagen.expection.RequestException;
import com.sunsharing.collagen.request.RequestResult;
import com.sunsharing.collagen.request.RequestSetting;
import com.sunsharing.collagen.request.WebServiceProxy;

/**
 * 畅享业务协同平台总线（集美接口总线） 工具类
 * 
 * @author zhenglin
 *
 */
public class UtilCollaGEN
{
	private static ConcurrentHashMap<String, String> xm_jm_haimai = new ConcurrentHashMap<String, String>();
	
	public static final String PROTOCOL="jm://";
	
	/**授权码*/
	private static final String AUTHORIZE_ID="577bd1014d6f47e7932769ed7f5cb0fd";

	static
	{
		xm_jm_haimai.put("/rest/systemInspect",
				"XM.JM::03::XM.GOV.YZ.JM.KPBXTZWWW.ZXYQB");// 执行预清标
		xm_jm_haimai.put("/rest/getProjectState",
				"XM.JM::03::XM.GOV.YZ.JM.KPBXTZWWW.HQYQBZT");// 获取预清标状态
		xm_jm_haimai.put("/rest/createUser",
				"XM.JM::03::XM.GOV.YZ.JM.KPBXTZWWW.createUser");// 创建项目相关帐号
		xm_jm_haimai.put("/rest/refreshBidder",
				"XM.JM::03::XM.GOV.YZ.JM.KPBXTZWWW.refreshBidder");// 刷新进入商务评审的投标人
		xm_jm_haimai.put("/rest/reOpenEvaluation",
				"XM.JM::03::XM.GOV.YZ.JM.KPBXTZWWW.reOpenEvaluation");// 重启评标流程
		xm_jm_haimai.put("/rest/getTenderPrice",
				"XM.JM::03::XM.GOV.YZ.JM.KPBXTZWWW.getTenderPrice");// 获取招标控制价等费用值
		
	}
	
	public static String post(String url,String path,Map<String, String> params) throws ServiceException{
		try
		{
			RequestSetting.getInstance()
			.setIpAndPort(getIp(url), getPort(url))
			.setSocketTimeOutInSecond(300)
			.setSyncWsProxyFlowId("FLOW_WEBSERVICE_PROXY")//固定的
			.setRequestId("XM.GOV.APP.JM.KPBXTZWWW")//应用ID
			.setLocalDomain("XM.JM");
			
			WebServiceProxy wsProxy = new WebServiceProxy(xm_jm_haimai.get(path));
			//业务授权ID
		    wsProxy.setAuthorizeId(AUTHORIZE_ID);
		    
			if (params != null && !params.isEmpty())
			{
				StringBuffer sb=new StringBuffer();
				for (Map.Entry<String, String> entry : params.entrySet())
				{
					String value = entry.getValue();
					if (value != null)
					{
						sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
//						wsProxy.appendStringParameter(entry.getKey(),entry.getValue());
					}
				}
				if(sb.length()>0) {
					sb.setLength(sb.length()-1);
				}
				wsProxy.appendStringParameter("BODY",sb.toString());
			}
			RequestResult ret = wsProxy.request();
			StringBuffer sb=new StringBuffer();
			for(Item it:ret.getRetItems()) {
				sb.append(it.asSimple().getValueIfString(""));
			}
			return sb.toString();
		}
		catch (RequestException e)
		{
			throw new ServiceException("","ErrorCode:" + e.getErrorCodeStr()    + ",ErrorMsg:" + e.getErrorMsg());
		}
	}
	
	private static String getIp(String url) {
		return url.substring(PROTOCOL.length(), url.indexOf(":", PROTOCOL.length()));
	}
	
	private static int getPort(String url) {
		String post=url.substring(url.indexOf(":", PROTOCOL.length())+1);
		if(post.indexOf("/")!=-1) {
			post=post.substring(0,post.indexOf("/"));
		}
		return Integer.parseInt(post);
	}
	
}
