/**
 * 包名：com.sozone.eokb.bus.sing
 * 文件名：Sing.java<br/>
 * 创建时间：2017-12-24 下午3:07:16<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.sing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.NumberToCharUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 唱标服务接口<br/>
 * <p>
 * 唱标服务接口<br/>
 * </p>
 * Time：2017-12-24 下午3:07:16<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/sing", desc = "唱标服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class Sing
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Sing.class);

	/**
	 * 持久化接口
	 */
	protected ActiveRecordDAO activeRecordDAO = null;

	/**
	 * activeRecordDAO属性的set方法
	 * 
	 * @param activeRecordDAO
	 *            the activeRecordDAO to set
	 */
	public void setActiveRecordDAO(ActiveRecordDAO activeRecordDAO)
	{
		this.activeRecordDAO = activeRecordDAO;
	}

	/**
	 * 生成mp3 并查询是否推送过投标人信息<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 是否已推送过投标人信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/create", desc = "生成mp3 并查询是否推送过投标人信息")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public boolean doCreateWav(AeolusData data) throws FacadeException
	{
		logger.error(LogUtils.format("生成mp3 并查询是否推送过投标人信息", data));
		String tpid = SessionUtils.getTPID();
		// 评标办法类型
		String code = SessionUtils.getTenderProjectTypeCode();

		// 只有房建市政需要电子唱标
		if (!StringUtils.contains(code, "xms")
				|| StringUtils.contains(code, "xms_fwjz_sg_")
				|| StringUtils.contains(code, "xms_fwjz_yllh_")
				|| StringUtils.contains(code, "xms_szgc_sg_")
				|| StringUtils.contains(code, "xms_szgc_yllh_"))
		{
			return getPushBidderStatus(tpid);
		}

		// 监理
		if (StringUtils.contains(code, "_jl_"))
		{
			createMp3(tpid, "1", false);
			return getPushBidderStatus(tpid);
		}
		// 设计
		if (StringUtils.contains(code, "_sj_"))
		{
			// 设计预审
			if (StringUtils.contains(code, "_v1_1")||StringUtils.contains(code, "_v2_1"))
			{
				createMp3(tpid, "2", true);
				return getPushBidderStatus(tpid);
			}
			createMp3(tpid, "2", false);
			return getPushBidderStatus(tpid);
		}
		// 通用版
		if (StringUtils.contains(code, "_tyb_"))
		{
			createMp3(tpid, "3", false);
			return getPushBidderStatus(tpid);
		}
		// 勘察
		if (StringUtils.contains(code, "_kc_"))
		{
			// 设计预审
			if (StringUtils.contains(code, "_v1_1")||StringUtils.contains(code, "_v2_1"))
			{
				createMp3(tpid, "4", true);
				return getPushBidderStatus(tpid);
			}
			createMp3(tpid, "4", false);
			return getPushBidderStatus(tpid);
		}
		// 小项目
		if (StringUtils.contains(code, "_jmxxm_")||StringUtils.contains(code, "_taxxm_"))
		{
			createMp3(tpid, "5", false);
			return getPushBidderStatus(tpid);
		}
		return true;
	}

	/**
	 * 
	 * 生成唱标音频文件<br/>
	 * <p>
	 * 生成唱标音频文件
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目主键
	 * @param status
	 *            标识（1监理，2设计，3通用版,4勘察,5小项目）
	 * @param flag
	 *            是否是第一次资格预审
	 * @throws ServiceException
	 *             ServiceException
	 */
	public void createMp3(String tpid, String status, boolean flag)
			throws ServiceException
	{
		logger.error(LogUtils.format("生成唱标音频文件", tpid, status, flag));
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		List<Record<String, Object>> sections = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_OPEN_STATUS !='0'")
				.setCondition("AND", "V_BID_OPEN_STATUS NOT LIKE '10%'")
				.setCondition("ORDER BY", "V_BID_SECTION_NAME").list(param);
		if (CollectionUtils.isEmpty(sections))
		{
			throw new ServiceException("", "无法获取招标项目的标段信息！");
		}
		String singTxt = null;
		// 1监理，2设计，3通用版
		if (StringUtils.equals("1", status))
		{
			singTxt = getMp3ContentForJl(sections, tpid);
		}
		else if (StringUtils.equals("2", status))
		{
			singTxt = getMp3ContentForSJ(sections, tpid, flag);
		}
		else if (StringUtils.equals("3", status))
		{
			singTxt = getMp3ContentForTyb(sections, tpid);
		}
		else if (StringUtils.equals("4", status))
		{
			singTxt = getMp3ContentForKC(sections, tpid, flag);
		}
		else if (StringUtils.equals("5", status))
		{
			singTxt = getMp3ContentForXXM(sections, tpid, flag);
		}

		if (StringUtils.isEmpty(singTxt))
		{
			throw new ServiceException("", "获取唱标内容异常");
		}
		// 生成mp3文件
		createMp3File(tpid, singTxt);
	}

	/**
	 * 
	 * 获取监理的电子唱标内容（监理）<br/>
	 * <p>
	 * 获取监理的电子唱标内容（监理）
	 * </p>
	 * 
	 * @param sections
	 *            标段集
	 * @param tpid
	 *            招标项目主键
	 * @return
	 * @throws ServiceException
	 */
	private String getMp3ContentForJl(List<Record<String, Object>> sections,
			String tpid) throws ServiceException
	{
		logger.error(LogUtils.format("获取设计的电子唱标内容（监理"));
		Record<String, Object> param = new RecordImpl<String, Object>();
		List<Record<String, Object>> bidders = null;
		StringBuffer singMsg = new StringBuffer();

		// 投标人扩展信息
		String vjson = null;
		JSONObject jobj = null;
		// 投标人唱标信息集
		JSONArray objSing = null;
		// 投标人唱标信息
		JSONObject sing = null;

		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sectionId", section.getColumn("V_BID_SECTION_ID"));
			bidders = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sectionId}")
					.setCondition("AND", "N_ENVELOPE_0 = 1")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			for (Record<String, Object> bidder : bidders)
			{
				vjson = bidder.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				objSing = jobj.getJSONArray("objSing");
				if (!StringUtils.isEmpty(jobj.getString("objSing")))
				{
					for (int i = 0; i < objSing.size(); i++)
					{
						sing = objSing.getJSONObject(i);
						bidder.putAll(sing);
					}
				}
				singMsg.append("投标人名称");
				singMsg.append(bidder.get("V_BIDDER_NAME") == null ? "无"
						: bidder.getString("V_BIDDER_NAME") + "，");
				// singMsg.append("组织机构代码");
				// singMsg.append(bidder.getString("bidder_org_code") == null ?
				// "无"
				// : makeDigitPlusSpace(bidder
				// .getString("bidder_org_code")) + "，");
				singMsg.append("社会统一信用代码");
				singMsg.append(bidder.get("bidder_unify_code") == null ? "无"
						: makeDigitPlusSpace(bidder
								.getString("bidder_unify_code")) + "，");
				singMsg.append("总监理工程师");
				singMsg.append(bidder.get("tbPeName") == null ? "无" : bidder
						.getString("tbPeName") + "，");
				singMsg.append("总监理工程师注册证书号");
				singMsg.append(bidder.get("zsbh") == null ? "无" : bidder
						.getString("zsbh") + "，");
				singMsg.append("投标保证金");
				singMsg.append(bidder.get("bzjje") == null ? "无"
						: NumberToCharUtils.number2CNMontrayUnit(bidder
								.getString("bzjje")));
				singMsg.append("投标保证金递交情况");
				singMsg.append(bidder.get("bzjdjqk") == null ? "无" : bidder
						.getString("bzjdjqk") + "，");
				singMsg.append("监理酬金");
				singMsg.append(bidder.get("tbbj") == null ? "无"
						: NumberToCharUtils.number2CNMontrayUnit(bidder
								.getString("tbbj")));
				singMsg.append("质量目标");
				singMsg.append(bidder.get("zlmb") == null ? "无" : bidder
						.getString("zlmb") + "，");
				singMsg.append("监理期限");
				singMsg.append(bidder.get("gongqi") == null ? "无" : bidder
						.getString("gongqi"));
			}
			// 保存信息
			saveData(tpid, singMsg.toString(),
					section.getString("V_BID_SECTION_ID"));
		}
		logger.error(LogUtils.format("成功获取设计的电子唱标内容（监理"));
		return singMsg.toString();
	}

	/**
	 * 
	 * 获取设计的电子唱标内容（设计）<br/>
	 * <p>
	 * 获取设计的电子唱标内容（设计）
	 * </p>
	 * 
	 * @param sections
	 *            标段集
	 * @param tpid
	 *            招标项目主键
	 * @param flag
	 *            是否是第一次资格预审
	 * @return
	 * @throws ServiceException
	 */
	private String getMp3ContentForSJ(List<Record<String, Object>> sections,
			String tpid, boolean flag) throws ServiceException
	{
		logger.error(LogUtils.format("获取设计的电子唱标内容（设计）"));
		Record<String, Object> param = new RecordImpl<String, Object>();
		List<Record<String, Object>> bidders = null;
		StringBuffer singMsg = new StringBuffer();

		// 投标人扩展信息
		String vjson = null;
		JSONObject jobj = null;
		// 投标人唱标信息集
		JSONArray objSing = null;
		// 投标人唱标信息
		JSONObject sing = null;

		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sectionId", section.getColumn("V_BID_SECTION_ID"));
			bidders = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sectionId}")
					.setCondition("AND", "N_ENVELOPE_0 = 1")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			for (Record<String, Object> bidder : bidders)
			{
				vjson = bidder.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				objSing = jobj.getJSONArray("objSing");
				if (!StringUtils.isEmpty(jobj.getString("objSing")))
				{
					for (int i = 0; i < objSing.size(); i++)
					{
						sing = objSing.getJSONObject(i);
						bidder.putAll(sing);
					}
				}
				singMsg.append("投标人名称");
				singMsg.append(bidder.get("V_BIDDER_NAME") == null ? "无"
						: bidder.getString("V_BIDDER_NAME") + "，");
				// singMsg.append("组织机构代码");
				// singMsg.append(bidder.getString("bidder_org_code") == null ?
				// "无"
				// : makeDigitPlusSpace(bidder
				// .getString("bidder_org_code")) + "，");
				singMsg.append("社会统一信用代码");
				singMsg.append(bidder.get("bidder_unify_code") == null ? "无"
						: makeDigitPlusSpace(bidder
								.getString("bidder_unify_code")) + "，");
				singMsg.append("拟担任设计项目负责人姓名");
				singMsg.append(bidder.get("tbPeName") == null ? "无" : bidder
						.getString("tbPeName") + "，");
				singMsg.append("拟担任设计项目负责人执业资格（职称）证书级别");
				singMsg.append(bidder.get("zsjb") == null ? "无" : bidder
						.getString("zsjb") + "，");
				singMsg.append("拟担任设计项目负责人执业资格（职称）证书编号");
				singMsg.append(bidder.get("zsbh") == null ? "无" : bidder
						.getString("zsbh") + "，");
				// 第一次预审无需唱报价信息
				if (flag)
				{
					continue;
				}
				singMsg.append("投标报价");
				singMsg.append(bidder.get("tbbj") == null ? "无"
						: NumberToCharUtils.number2CNMontrayUnit(bidder
								.getString("tbbj")));
				singMsg.append("设计周期");
				singMsg.append(bidder.get("gongqi") == null ? "无" : bidder
						.getString("gongqi") + "，");
				singMsg.append("投标保证金");
				singMsg.append(bidder.get("bzjje") == null ? "无"
						: NumberToCharUtils.number2CNMontrayUnit(bidder
								.getString("bzjje")));
				singMsg.append("投标保证金递交情况");
				singMsg.append(bidder.get("bzjdjqk") == null ? "无" : bidder
						.getString("bzjdjqk") + "，");
			}
			// 保存信息
			saveData(tpid, singMsg.toString(),
					section.getString("V_BID_SECTION_ID"));
		}
		logger.error(LogUtils.format("成功获取设计的电子唱标内容（设计）"));
		return singMsg.toString();
	}

	/**
	 * 
	 * 获取通用版的电子唱标内容（通用版）<br/>
	 * <p>
	 * 获取通用版的电子唱标内容（通用版）
	 * </p>
	 * 
	 * @param sections
	 *            标段集
	 * @param tpid
	 *            招标项目主键
	 * @return
	 * @throws ServiceException
	 */
	private String getMp3ContentForTyb(List<Record<String, Object>> sections,
			String tpid) throws ServiceException
	{
		logger.error(LogUtils.format("获取设计的电子唱标内容（通用版）"));
		Record<String, Object> param = new RecordImpl<String, Object>();
		List<Record<String, Object>> bidders = null;
		StringBuffer singMsg = new StringBuffer();

		// 投标人扩展信息
		String vjson = null;
		JSONObject jobj = null;
		// 投标人唱标信息集
		JSONArray objSing = null;
		// 投标人唱标信息
		JSONObject sing = null;

		// 中文正则表达式
		Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sectionId", section.getColumn("V_BID_SECTION_ID"));
			bidders = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sectionId}")
					.setCondition("AND", "N_ENVELOPE_0 = 1")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			for (Record<String, Object> bidder : bidders)
			{
				vjson = bidder.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				objSing = jobj.getJSONArray("objSing");
				if (!StringUtils.isEmpty(jobj.getString("objSing")))
				{
					for (int i = 0; i < objSing.size(); i++)
					{
						sing = objSing.getJSONObject(i);
						if (sing.containsKey("组织机构代码"))
						{
							continue;
						}
						// 去除下标（Index）
						sing.remove("index");
						// 读取中文添加标题头
						for (Entry<String, Object> entry : sing.entrySet())
						{
							if (p.matcher(entry.getKey()).find())
							{
								logger.info("读取中文添加标题头：" + entry.getKey());
								singMsg.append(entry.getKey());
							}
						}
						// 读取英文添加内容
						for (Entry<String, Object> entry : sing.entrySet())
						{
							if (!p.matcher(entry.getKey()).find())
							{
								logger.info("读取英文添加内容：" + entry.getValue());
								// 统一社会信用代码啊转成字符
								if (StringUtils.equals("bidder_unify_code",
										entry.getKey()))
								{
									singMsg.append(entry.getValue().toString() == null ? "无"
											: makeDigitPlusSpace(entry
													.getValue().toString()));
									break;
								}
								// 报价转成金额
								if (StringUtils.equals("tbbj", entry.getKey())
										|| StringUtils.equals("bzjje",
												entry.getKey()))
								{
									singMsg.append(entry.getValue().toString() == null ? "无"
											: NumberToCharUtils
													.number2CNMontrayUnit(entry
															.getValue()
															.toString()));
									break;
								}
								singMsg.append(entry.getValue());
							}
						}
					}
				}
			}
			// 保存信息
			saveData(tpid, singMsg.toString(),
					section.getString("V_BID_SECTION_ID"));
		}
		logger.error(LogUtils.format("成功获取设计的电子唱标内容（通用版）"));
		return singMsg.toString();
	}

	/**
	 * 
	 * 获取勘察的电子唱标内容（勘察）<br/>
	 * <p>
	 * 获取勘察的电子唱标内容（勘察）
	 * </p>
	 * 
	 * @param sections
	 *            标段集
	 * @param tpid
	 *            招标项目主键
	 * @param flag
	 *            是否是第一次资格预审
	 * @return
	 * @throws ServiceException
	 */
	private String getMp3ContentForKC(List<Record<String, Object>> sections,
			String tpid, boolean flag) throws ServiceException
	{
		logger.error(LogUtils.format("获取勘察的电子唱标内容（勘察）"));
		Record<String, Object> param = new RecordImpl<String, Object>();
		List<Record<String, Object>> bidders = null;
		StringBuffer singMsg = new StringBuffer();

		// 投标人扩展信息
		String vjson = null;
		JSONObject jobj = null;
		// 投标人唱标信息集
		JSONArray objSing = null;
		// 投标人唱标信息
		JSONObject sing = null;

		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sectionId", section.getColumn("V_BID_SECTION_ID"));
			bidders = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sectionId}")
					.setCondition("AND", "N_ENVELOPE_0 = 1")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			for (Record<String, Object> bidder : bidders)
			{
				vjson = bidder.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				objSing = jobj.getJSONArray("objSing");
				if (!StringUtils.isEmpty(jobj.getString("objSing")))
				{
					for (int i = 0; i < objSing.size(); i++)
					{
						sing = objSing.getJSONObject(i);
						bidder.putAll(sing);
					}
				}
				singMsg.append("投标人名称");
				singMsg.append(bidder.get("V_BIDDER_NAME") == null ? "无"
						: bidder.getString("V_BIDDER_NAME") + "，");
				// singMsg.append("组织机构代码");
				// singMsg.append(bidder.getString("bidder_org_code") == null ?
				// "无"
				// : makeDigitPlusSpace(bidder
				// .getString("bidder_org_code")) + "，");
				singMsg.append("社会统一信用代码");
				singMsg.append(bidder.get("bidder_unify_code") == null ? "无"
						: makeDigitPlusSpace(bidder
								.getString("bidder_unify_code")) + "，");
				singMsg.append("拟担任勘察项目负责人姓名");
				singMsg.append(bidder.get("tbPeName") == null ? "无" : bidder
						.getString("tbPeName") + "，");
				singMsg.append("注册土木工程师（岩土）执业资格证书注册编号");
				singMsg.append(bidder.get("zsbh") == null ? "无" : bidder
						.getString("zsbh") + "，");
				// 第一次预审无需唱报价信息
				if (flag)
				{
					continue;
				}

				singMsg.append("勘察费投标报价");
				singMsg.append(bidder.get("kc_tbbj") == null ? "无"
						: NumberToCharUtils.number2CNMontrayUnit(bidder
								.getString("kc_tbbj")));
				singMsg.append("勘察周期");
				singMsg.append(bidder.get("gongqi") == null ? "无" : bidder
						.getString("gongqi") + "，");
				singMsg.append("投标保证金");
				singMsg.append(bidder.get("bzjje") == null ? "无"
						: NumberToCharUtils.number2CNMontrayUnit(bidder
								.getString("bzjje")));
				singMsg.append("投标保证金递交情况");
				singMsg.append(bidder.get("bzjdjqk") == null ? "无" : bidder
						.getString("bzjdjqk") + "，");
			}
			// 保存信息
			saveData(tpid, singMsg.toString(),
					section.getString("V_BID_SECTION_ID"));
		}
		logger.error(LogUtils.format("成功获取设计的电子唱标内容（设计）"));
		return singMsg.toString();
	}

	/**
	 * 
	 * 获取小项目的电子唱标内容<br/>
	 * <p>
	 * 获取小项目的电子唱标内容
	 * </p>
	 * 
	 * @param sections
	 *            标段集
	 * @param tpid
	 *            招标项目主键
	 * @param flag
	 *            是否是第一次资格预审
	 * @return
	 * @throws ServiceException
	 */
	private String getMp3ContentForXXM(List<Record<String, Object>> sections,
			String tpid, boolean flag) throws ServiceException
	{
		logger.error(LogUtils.format("获取小项目的电子唱标内容"));
		Record<String, Object> param = new RecordImpl<String, Object>();
		List<Record<String, Object>> bidders = null;
		StringBuffer singMsg = new StringBuffer();

		// 投标人扩展信息
		String vjson = null;
		JSONObject jobj = null;
		// 投标人唱标信息集
		JSONArray objSing = null;
		// 投标人唱标信息
		JSONObject sing = null;

		for (Record<String, Object> section : sections)
		{
			param.clear();
			param.setColumn("tpid", tpid);
			param.setColumn("sectionId", section.getColumn("V_BID_SECTION_ID"));
			bidders = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_TENDER_LIST)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", "V_BID_SECTION_ID = #{sectionId}")
					.setCondition("AND", "N_ENVELOPE_0 = 1")
					.addSortOrder("V_BIDDER_NO", "ASC").list(param);
			for (Record<String, Object> bidder : bidders)
			{
				vjson = bidder.getString("V_JSON_OBJ");
				jobj = JSON.parseObject(vjson);
				objSing = jobj.getJSONArray("objSing");
				if (!StringUtils.isEmpty(jobj.getString("objSing")))
				{
					for (int i = 0; i < objSing.size(); i++)
					{
						sing = objSing.getJSONObject(i);
						bidder.putAll(sing);
					}
				}
				singMsg.append("投标人名称");
				singMsg.append(bidder.get("V_BIDDER_NAME") == null ? "无"
						: bidder.getString("V_BIDDER_NAME") + "，");
				// singMsg.append("组织机构代码");
				// singMsg.append(bidder.getString("bidder_org_code") == null ?
				// "无"
				// : makeDigitPlusSpace(bidder
				// .getString("bidder_org_code")) + "，");
				singMsg.append("社会统一信用代码");
				singMsg.append(bidder.get("bidder_unify_code") == null ? "无"
						: makeDigitPlusSpace(bidder
								.getString("bidder_unify_code")) + "，");
				singMsg.append("项目负责人姓名");
				singMsg.append(bidder.get("tbPeName") == null ? "无" : bidder
						.getString("tbPeName") + "，");

				singMsg.append("投标报价");
				singMsg.append(bidder.get("kc_tbbj") == null ? "无"
						: NumberToCharUtils.number2CNMontrayUnit(bidder
								.getString("kc_tbbj")));
				singMsg.append("工期");
				singMsg.append(bidder.get("gongqi") == null ? "无" : bidder
						.getString("gongqi") + "，");
			}
			// 保存信息
			saveData(tpid, singMsg.toString(),
					section.getString("V_BID_SECTION_ID"));
		}
		logger.error(LogUtils.format("成功获取设计的电子唱标内容（设计）"));
		return singMsg.toString();
	}

	/**
	 * 
	 * 调用Speech生成唱标文件<br/>
	 * <p>
	 * 调用Speech生成唱标文件
	 * </p>
	 * 
	 * @param tpid
	 * @param context
	 */
	private void createMp3File(String tpid, String context)
	{
		logger.error(LogUtils.format("调用Speech生成唱标文件"));
		// 文件保存路径
		String rootPath = SystemParamUtils
				.getString(ConstantEOKB.SysParamKey.EBIDKB_FILE_PATH_URL)
				+ "bidReport" + File.separator + tpid + File.separator + "sing";
		// 创建目录
		logger.error(LogUtils.format("开始创建目录"));
		File dir = new File(rootPath);
		if (!dir.exists())
		{
			dir.mkdirs();
		}
		logger.error(LogUtils.format("创建目录完成"));
		String filePath = rootPath + File.separator + "sing.mp3";
		String cmdText = SystemParamUtils
				.getString(ConstantEOKB.SysParamKey.EBIDKB_SING_PATH_URL)
				+ " "
				+ context + " " + filePath;
		try
		{
			Process pro = Runtime.getRuntime().exec(cmdText);
			BufferedReader bf = new BufferedReader(new InputStreamReader(
					pro.getInputStream()));
			bf.close();
			pro.waitFor();
			logger.error(LogUtils.format("生成唱标文件成功"));
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("生成唱标文件失败"));
		}

	}

	/**
	 * 
	 * 备份音频文字<br/>
	 * <p>
	 * 备份音频文字
	 * </p>
	 * 
	 * @param tpid
	 * @param msg
	 * @param sid
	 * @throws ServiceException
	 */
	private void saveData(String tpid, String msg, String sid)
			throws ServiceException
	{
		logger.error(LogUtils.format("备份音频文字"));

		Record<String, Object> param = new RecordImpl<String, Object>();
		// 删除冗余数据,为了防止出现重复
		param.setColumn("flag", ConstantEOKB.XMFJSZ_SING);
		param.setColumn("tpid", tpid);
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE = #{flag}").remove(param);

		// 如果有数据
		Record<String, Object> singInfo = new RecordImpl<String, Object>();
		Record<String, Object> record = null;

		record = new RecordImpl<String, Object>();
		singInfo.setColumn("ID", Random.generateUUID());
		singInfo.setColumn("V_BUS_FLAG_TYPE", ConstantEOKB.XMFJSZ_SING);
		singInfo.setColumn("V_CREATE_USER", ApacheShiroUtils.getCurrentUserID())
				.setColumn("N_CREATE_TIME", System.currentTimeMillis());
		singInfo.setColumn("V_TPID", tpid);
		singInfo.setColumn("V_BUS_ID", sid);

		record.setColumn("singMsg", msg);
		singInfo.setColumn("V_JSON_OBJ", JSON.toJSONString(record));
		this.activeRecordDAO.auto().table(TableName.TENDER_PROJECT_NODE_DATA)
				.save(singInfo);
	}

	/**
	 * 
	 * 字符串中的数字补空格<br/>
	 * <p>
	 * 语音唱标组织机构代码这类的数字需要一个一个的读
	 * </p>
	 * 
	 * @param content
	 *            字符内容
	 * @return
	 */
	private static String makeDigitPlusSpace(String content)
	{
		StringBuilder sb = new StringBuilder();
		String regex = "\\d";
		char temChar;
		for (int i = 0; i < content.length(); i++)
		{
			temChar = content.charAt(i);
			sb.append(temChar);
			if (String.valueOf(temChar).matches(regex))
			{
				sb.append(" ");
			}
		}
		return sb.toString();

	}

	/**
	 * 电子唱标<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 * @throws IOException
	 *             IOException
	 */
	// 定义路径
	@Path(value = "", desc = "电子唱标")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void doSingMp3(AeolusData data) throws FacadeException, IOException
	{
		logger.error(LogUtils.format("电子唱标", data));
		String tpid = SessionUtils.getTPID();
		// 唱标保存路径
		String singPath = SystemParamUtils
				.getString(ConstantEOKB.SysParamKey.EBIDKB_FILE_PATH_URL)
				+ "bidReport"
				+ File.separator
				+ tpid
				+ File.separator
				+ "sing"
				+ File.separator + "sing.mp3";
		logger.warn(LogUtils.format("mp3路径：" + singPath));
		HttpServletResponse response = data.getHttpServletResponse();
		response.setContentType("video/mpeg");
		response.addHeader("Content-Disposition", "attachment; filename=\""
				+ "sing.mp3");
		OutputStream out = response.getOutputStream();
		File file = new File(singPath);
		if (file.exists())
		{
			FileUtils.copyFile(file, out);
		}
		else
		{
			throw new ServiceException("", "未找到对应的唱标文件");
		}
		logger.error(LogUtils.format("电子唱标成功"));
	}

	/**
	 * 
	 * 查看是否推送过投标人信息<br/>
	 * <p>
	 * 查看是否推送过投标人信息
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return true（代理点击过推送）,false（代理没点击过推送）
	 * @throws FacadeException
	 *             FacadeException
	 */
	public boolean getPushBidderStatus(String tpid) throws FacadeException
	{
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("flag", ConstantEOKB.PUSH_BIDDER_INFO);
		int count = this.activeRecordDAO.auto()
				.table(TableName.TENDER_PROJECT_NODE_DATA)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BUS_FLAG_TYPE=#{flag}").count(param);
		return count > 0;
	}
}
