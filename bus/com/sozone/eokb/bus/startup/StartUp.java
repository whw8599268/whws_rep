/**
 * 包名：com.sozone.eokb.fjs_gsgl_ljsg_hldjf_v1;
 * 文件名：BidOpenStart.java<br/>
 * 创建时间：2017-7-27 上午11:45:27<br/>
 * 创建者：jack<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.startup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.GenerateCodeUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.Random;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.ext.rs.ResultVO;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.util.TimestampUtils;
import com.sozone.eokb.bus.video.Video;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;
import com.sozone.eokb.utils.HttpClientUtils;
import com.sozone.eokb.utils.InterfaceFieldMappingUtils;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.utils.TenderProjectParamUtils;

/**
 * 登陆后服务接口<br/>
 * Time：2017-7-25 下午7:11:15<br/>
 * 
 * @author jack
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/startup", desc = "开标启动服务接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class StartUp
{

	/**
	 * 项目解密锁
	 */
	private static final Map<String, ReentrantLock> PROJECT_INIT_LOCK = new ConcurrentHashMap<String, ReentrantLock>();

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(StartUp.class);

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
	 * 获取招标项目初始化锁<br/>
	 * <p>
	 * 为了防止同一个项目在同一个解密时间段内重复初始化,所有为每一个项目加了一个同步锁,该方法用于创建项目单例互斥锁
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 互斥锁锁对象
	 */
	private static synchronized ReentrantLock getTPInitLock(String tpid)
	{
		// 获取项目锁
		ReentrantLock lock = PROJECT_INIT_LOCK.get(tpid);
		// 如果锁不存在
		if (null == lock)
		{
			// 构建互斥锁
			lock = new ReentrantLock();
			PROJECT_INIT_LOCK.put(tpid, lock);
		}
		return lock;
	}

	/**
	 * 
	 * 初始化招标项目开标数据<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 响应结果
	 * @throws FacadeException
	 *             服务异常
	 */
	@Path(value = "/init", desc = "初始化招标项目开标数据")
	@HttpMethod(HttpMethod.GET)
	public ResultVO<String> initTenderProjectData(AeolusData data)
			throws FacadeException
	{
		logger.info(LogUtils.format("初始化招标项目开标数据", data));
		String tpid = SessionUtils.getTPID();

		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getTPInitLock(tpid);
		logger.info(LogUtils.format("获取锁实例成功"));
		// 获取锁,这一步其实就是为了让线程挂起
		lock.lock();
		logger.info(LogUtils.format("获取锁成功"));
		try
		{
			String tpType = SessionUtils.getTenderProjectType();
			// 判断投递表是否有数据
			int count = this.activeRecordDAO
					.auto()
					.table(TableName.EKB_T_TBIMPORTBIDDING)
					.setCondition("AND", "V_TPID = #{tpid}")
					.count(new RecordImpl<String, Object>().setColumn("tpid",
							tpid));
			logger.info(LogUtils.format("投递表的数据共" + count + "条"));
			ResultVO<String> result = new ResultVO<String>(true);
			// 如果投递表没有数据
			if (0 >= count)
			{
				// 加载投标人投递信息
				loadDeliveryList();
				logger.info(LogUtils.format("投标人加载完成"));
				// 生成投递编号及修改投递时间
				generateDeliveryNOAndTime();
				logger.info(LogUtils.format("投递编号生成完成"));
				// 初始标段信息表中投标人数量
				initSectionBidderNumber();
				logger.info(LogUtils.format("标段投标人信息初始化完成"));
				// 如果不是房建市政
				if (!StringUtils.equals(
						ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_FWJZ_TYPE,
						tpType)
						&& !StringUtils
								.equals(ConstantEOKB.TENDERPROJECT_APP_TYPE.XMS_SZGC_TYPE,
										tpType))
				{
					// 加载关联企业信息
					loadCognateEnts();
					logger.info(LogUtils.format("加载关联企业信息完成"));
				}
				result.setSuccess(false);

				// 设置KQ值
				int methodType = SessionUtils.getEvaluationMethodType();
				// 56:经A；57经B；58综A；59综B
				if (56 <= methodType && methodType <= 59)
				{
					logger.info(LogUtils.format("准备设置KQ值"));
					setKQValue();
					logger.info(LogUtils.format("KQ值设置完成"));
				}

				// 开标视频是否可用
				int videoStatus = SystemParamUtils
						.getInteger(ConstantEOKB.SysParamKey.EOV_VIDEO_STATUS);
				if (videoStatus == 1)
				{
					logger.info(LogUtils.format("开始解除直播流"));
					// 解除禁用的直播流
					Video.relieveVideo(tpid);
					logger.info(LogUtils.format("解除直播流完成"));
					// 启动视频
					logger.info(LogUtils.format("开始启动直播流"));
					Video.beginVideo(tpid);
					logger.info(LogUtils.format("开启直播流完成"));
				}
			}
			logger.info(LogUtils.format("同步投递数据结束!", result));
			return result;
		}
		finally
		{
			// 如果已经没有线程等待获取锁
			if (!lock.hasQueuedThreads())
			{
				// 干掉锁
				PROJECT_INIT_LOCK.remove(tpid);
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	/**
	 * 
	 * 设置KQ值<br/>
	 * <p>
	 * 设置KQ值
	 * </p>
	 * 
	 * @param
	 * @throws ServiceException
	 */
	private void setKQValue() throws ServiceException
	{
		JSONObject pbMethod = SessionUtils
				.getAttribute(ConstantEOKB.PB_METHOD_JSON_INFO_SESSION_KEY);

		logger.info(LogUtils.format("设置KQ值", pbMethod));
		// 施工经评审和综合有KQ值
		if (!CollectionUtils.isEmpty(pbMethod))
		{
			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("tpid", SessionUtils.getTPID());
			Record<String, Object> sectionInfo = this.activeRecordDAO.auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID=#{tpid}").get(param);
			if (CollectionUtils.isEmpty(sectionInfo))
			{
				throw new ServiceException("", "查询不到对应的标段信息！");
			}

			// 设置K值信息
			JSONObject sectionExtInfo = sectionInfo.getJSONObject("V_JSON_OBJ");

			if (CollectionUtils.isEmpty(sectionExtInfo))
			{
				return;
			}

			// KQ值只写入一次
			if (StringUtils.isNotEmpty(sectionExtInfo.getString("INIT")))
			{
				return;
			}

			sectionExtInfo.put("MAX_K", pbMethod.getString("MAX_K"));
			sectionExtInfo.put("MIN_K", pbMethod.getString("MIN_K"));
			// 投标报价小于等于评标基准价的时候
			sectionExtInfo.put("Q1", pbMethod.getString("Q1"));
			// 投标报价大于评标基准价的时候
			sectionExtInfo.put("Q2", pbMethod.getString("Q2"));
			sectionExtInfo.put("INIT", true);
			sectionInfo.setColumn("V_JSON_OBJ", sectionExtInfo.toJSONString());

			this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
					.modify(sectionInfo);
		}
	}

	/**
	 * 加载投标人投递信息<br/>
	 * 
	 * @throws ServiceException
	 *             ServiceException
	 */
	private void loadDeliveryList() throws ServiceException
	{
		logger.info(LogUtils.format("加载投标人投递信息"));
		Record<String, String> param = new RecordImpl<String, String>();
		String tpid = SessionUtils.getTPID();
		param.clear();
		// 招标项目
		param.setColumn("TENDERPROJECTID", SessionUtils.getTenderProjectInfo()
				.getString("V_TENDER_PROJECT_ID"));
		// 如果有扩展信息
		String json = SessionUtils.getTenderProjectInfo().getString(
				"V_JSON_OBJ");
		if (StringUtils.isEmpty(json))
		{
			param.setColumn("REVIEWSORT", 2 + "");
		}
		else
		{
			JSONObject ext = JSON.parseObject(json);
			param.setColumn("REVIEWSORT", ext.getString("N_BID_ORDER"));
		}
		// 招标项目对应标段
		String bid = SessionUtils.getTenderProjectInfo().getString(
				"V_BID_SECTION_ID");
		if (StringUtils.isNotEmpty(bid))
		{
			// 标段ID
			param.setColumn("SECTIONID", bid);
		}
		// 调用交易平台接口获取投递信息列表
		List<Record<String, Object>> deliverys = loadRemoteDeliveryList(param);
		if (CollectionUtils.isEmpty(deliverys))
		{
			return;
		}
		param.setColumn("tpid", tpid);
		// 先删除数据
		this.activeRecordDAO.auto().table(TableName.EKB_T_TBIMPORTBIDDING)
				.setCondition("AND", "V_TPID = #{tpid}").remove(param);
		// 再插入数据
		this.activeRecordDAO.auto().table(TableName.EKB_T_TBIMPORTBIDDING)
				.save(deliverys);
	}

	/**
	 * 从交易平台中获取招标项目编号对应的企业投递信息<br/>
	 * 
	 * @param params
	 *            招标项目编号
	 * 
	 * @throws ServiceException
	 *             ServiceException
	 * @return 投递信息列表
	 */
	private List<Record<String, Object>> loadRemoteDeliveryList(
			Record<String, String> params) throws ServiceException
	{
		logger.info(LogUtils.format("从交易平台中获取招标项目编号对应的企业投递信息"));
		// 获取头部Token
		Record<String, String> headMap = HttpClientUtils.getHeadMapOfToken();
		// 投标企业信息请求url
		String url = TenderProjectParamUtils
				.getSystemParamValue(SysParamKey.EDE_ENTBID_INFO_URL);
		logger.info(LogUtils.format("请求地址", url));
		logger.info(LogUtils.format("请求参数", params));
		String json = null;
		try
		{
			json = HttpClientUtils.doGet(url, params, headMap,
					ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (Exception e)
		{
			throw new ServiceException("E-1007", "获取企业投标信息失败!", e);
		}
		logger.info(LogUtils.format("响应结果", json));
		JSONObject result = JSON.parseObject(json);
		boolean success = result.getBoolean("success");
		if (!success)
		{
			throw new ServiceException(result.getString("errorCode"),
					result.getString("errorDesc"));
		}
		// 返回的投标人投递信息List
		List<Record<String, Object>> rows = new ArrayList<Record<String, Object>>();
		// 投标人投递信息JSON
		JSONArray array = result.getJSONArray("rows");
		// 如果没有投递信息
		if (CollectionUtils.isEmpty(array))
		{
			logger.error(LogUtils.format("从交易平台中获取到企业投递信息为空!", params, headMap,
					json));
			return rows;
		}
		Record<String, Object> record = null;
		JSONObject jobj = null;
		Record<String, Object> temp = new RecordImpl<String, Object>();
		for (int i = 0; i < array.size(); i++)
		{
			record = new RecordImpl<String, Object>();
			jobj = array.getJSONObject(i);
			temp.clear();
			temp.putAll(jobj);
			// 根据excel中的配置将交易平台的字段转换成本地字段
			record.putAll(InterfaceFieldMappingUtils.FiledlocalToInterFace(
					temp, TableName.EKB_T_TBIMPORTBIDDING));
			record.setColumn("ID", SZUtilsID.getUUID());
			record.setColumn("V_TPID", SessionUtils.getTPID());
			rows.add(record);
		}
		return rows;
	}

	/**
	 * 加载关联企业信息<br/>
	 * 
	 * @throws ServiceException
	 *             ServiceException
	 */
	private void loadCognateEnts() throws ServiceException
	{
		logger.info(LogUtils.format("加载关联企业信息"));
		String tpid = SessionUtils.getTPID();
		Record<String, String> param = new RecordImpl<String, String>();
		// 删除当前项目的关联企业信息
		param.setColumn("tpid", tpid);
		this.activeRecordDAO.auto().table(TableName.EKB_T_CORRELATE_ENTER)
				.setCondition("AND", "V_TPID = #{tpid}").remove(param);
		// 根据时间获取关联企业信息
		param.clear();
		param.setColumn("TIME", "1970-01-01 00:00:00");
		List<Record<String, Object>> enteInfos = loadRemoteCognateEnts(param);
		if (CollectionUtils.isEmpty(enteInfos))
		{
			return;
		}
		for (Record<String, Object> enteInfo : enteInfos)
		{
			enteInfo.setColumn("V_SID", enteInfo.getString("ID"));
			enteInfo.setColumn("V_TPID", tpid);
			enteInfo.setColumn("ID", Random.generateUUID());
		}
		this.activeRecordDAO.auto().table(TableName.EKB_T_CORRELATE_ENTER)
				.save(enteInfos);
	}

	/**
	 * 
	 * 从交易平台中获关联企业信息<br/>
	 * <p>
	 * </p>
	 * 
	 * @param params
	 * @return
	 * @throws ServiceException
	 */
	private List<Record<String, Object>> loadRemoteCognateEnts(
			Record<String, String> params) throws ServiceException
	{
		logger.info(LogUtils.format("从交易平台中获关联企业信息", params));
		Record<String, String> headMap = HttpClientUtils.getHeadMapOfToken();
		String url = TenderProjectParamUtils
				.getSystemParamValue(SysParamKey.EDE_ASSOCIATED_ENT_URL);
		String json = null;
		try
		{
			json = HttpClientUtils.doGet(url, params, headMap,
					ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (Exception e)
		{
			throw new ServiceException("E-1011", "获取关联企业信息!", e);
		}

		JSONObject result = JSON.parseObject(json);
		boolean success = result.getBoolean("success");
		if (!success)
		{
			throw new ServiceException(result.getString("errorCode"),
					result.getString("errorDesc"));
		}
		List<Record<String, Object>> ents = new ArrayList<Record<String, Object>>();
		JSONArray array = result.getJSONArray("rows");
		if (CollectionUtils.isEmpty(array))
		{
			return ents;
		}
		Record<String, Object> record = null;
		JSONObject jobj = null;
		for (int i = 0; i < array.size(); i++)
		{
			jobj = array.getJSONObject(i);
			record = new RecordImpl<String, Object>();
			// 关联企业表与本地表一致,不做转换
			record.putAll(jobj);
			ents.add(record);
		}
		return ents;
	}

	/**
	 * 
	 * 生成投递编号及修改投递时间<br/>
	 * <p>
	 * </p>
	 */
	private void generateDeliveryNOAndTime() throws ServiceException
	{
		logger.info(LogUtils.format("生成投递编号及修改投递时间"));
		String tpid = SessionUtils.getTPID();
		// 获取指定招标项目的每一个投标人的最后投递时间
		List<Record<String, Object>> rows = this.activeRecordDAO.statement()
				.selectList("StartUp.getTenderProjectMaxDeliveryTime", tpid);
		if (CollectionUtils.isEmpty(rows))
		{
			return;
		}
		// 总长3
		int maxTotalLength = 3;
		// 最长3
		int maxLength = 3;
		// 投递位数
		int length = StringUtils.length(rows.size() + "");
		if (length > 3)
		{
			maxTotalLength = length;
			maxLength = length;
		}
		// 投递编号
		String deliveryNO = null;
		for (Record<String, Object> record : rows)
		{
			// 生成投递编号
			deliveryNO = GenerateCodeUtils.generateNextCode(null, deliveryNO,
					maxTotalLength, maxLength);
			// 设置编号
			record.setColumn("V_DELIVER_NO", deliveryNO);
			// 修改所有投递时间
			record.setColumn("V_DELIVER_TIME", record.getString("MAXTIME"));
			// 设置tpid
			record.setColumn("tpid", tpid);
			// 修改该招标项目下该投标人的所有投递数据的编号以及时间
			this.activeRecordDAO.auto().table(TableName.EKB_T_TBIMPORTBIDDING)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND", " V_ORG_CODE = #{V_ORG_CODE}")
					.modify(record);
		}
	}

	/**
	 * 
	 * 初始化标段投标人数量<br/>
	 * <p>
	 * 初始化标段投标人数量
	 * </p>
	 */
	private void initSectionBidderNumber() throws ServiceException
	{
		logger.info(LogUtils.format("初始化标段投标人数量"));
		String tpid = SessionUtils.getTPID();
		// 获取指定招标项目下的每一个标段的投标人数量
		List<Record<String, Object>> sections = this.activeRecordDAO
				.statement().selectList("StartUp.getSectionBidderNumber", tpid);
		for (Record<String, Object> section : sections)
		{
			section.setColumn("tpid", tpid);
			this.activeRecordDAO
					.auto()
					.table(TableName.EKB_T_SECTION_INFO)
					.setCondition("AND", "V_TPID = #{tpid}")
					.setCondition("AND",
							"V_BID_SECTION_ID = #{V_BID_SECTION_ID}")
					.modify(section);
		}
	}

	/**
	 * 
	 * 招标代理或投标人获取开标设置信息<br/>
	 * <p>
	 * 招标代理或投标人获取开标设置信息
	 * </p>
	 * 
	 * @param data
	 * @return
	 * @throws FacadeException
	 */
	@Path(value = "/list", desc = "招标代理或投标人获取开标设置信息")
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getBidOpenList(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("招标代理或投标人获取开标设置信息", data));
		String tpid = SessionUtils.getTPID();
		boolean group = SessionUtils.isSectionGroup();
		// 如果是标段组的情况
		if (group)
		{
			return this.activeRecordDAO.statement().selectList(
					"StartUp.getBidOpenList_Group", tpid);
		}

		// 非标段组情况下
		return this.activeRecordDAO.statement().selectList(
				"StartUp.getBidOpenList", tpid);
	}

	/**
	 * 
	 * 修改标段或者标段组的开标状态<br/>
	 * <p>
	 * 修改标段或者标段组的开标状态
	 * </p>
	 * 
	 * @param data
	 * @throws FacadeException
	 */
	@Path(value = "/msbos", desc = "修改标段或者标段组的开标状态")
	@HttpMethod(HttpMethod.POST)
	// 增加操作日志
	@Handler(OperationLogHandler.class)
	public void modifySectionBidOpenStatus(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("修改标段或者标段组的开标状态", data));
		Record<String, Object> param = data.getParam("STATUS_LIST");
		String tpid = SessionUtils.getTPID();
		if (CollectionUtils.isEmpty(param))
		{
			return;
		}
		// 获取到标段ID数组字符串集合
		Set<String> sectionIDs = param.keySet();
		//
		String[] sids = null;
		String status = null;
		Record<String, Object> section = new RecordImpl<String, Object>();
		section.setColumn("tpid", tpid);
		for (String sectiondID : sectionIDs)
		{
			// 获取当前标段的开标状态
			status = param.getString(sectiondID);
			// 分割取到每一个标段ID
			sids = StringUtils.split(sectiondID, ",");
			section.setColumn("V_BID_OPEN_STATUS", status);
			for (String sid : sids)
			{
				if (StringUtils.isEmpty(sid))
				{
					continue;
				}
				section.setColumn("sid", sid);
				this.activeRecordDAO.auto().table(TableName.EKB_T_SECTION_INFO)
						.setCondition("AND", "V_TPID = #{tpid}")
						.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
						.modify(section);
			}
		}
	}

	/**
	 * 
	 * 判断是否有存在未设置状态的标段<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             服务异常
	 */
	@Path(value = "/gnsbos", desc = "判断是否有存在未设置状态的标段")
	@HttpMethod(HttpMethod.GET)
	public ResultVO<String> getNoSetBidOpenStatusSection(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("判断是否有存在未设置状态的标段", data));
		ResultVO<String> result = new ResultVO<String>(true);
		String tpid = SessionUtils.getTPID();
		int count = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_BID_OPEN_STATUS = '0'")
				.setCondition("AND", "V_TPID = #{tpid}")
				.count(new RecordImpl<String, Object>().setColumn("tpid", tpid));
		if (0 < count)
		{
			result.setSuccess(false);
		}
		return result;
	}

	/**
	 * 
	 * 验证系统当前时间是否在开标时间内<br/>
	 * <p>
	 * 验证系统当前时间是否在开标时间内
	 * </p>
	 * 
	 * @param data
	 * @return
	 * @throws FacadeException
	 */
	@Path(value = "/verifyOpenTiem", desc = "验证系统当前时间是否在开标时间内")
	@HttpMethod(HttpMethod.GET)
	public boolean verifyOpenTime(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("验证系统当前时间是否在开标时间内", data));
		String openTime = SessionUtils.getBidOpenTime();
		if (StringUtils.isEmpty(openTime))
		{
			throw new ServiceException("E-1013", "获取开标时间失败!");
		}
		long bidOpentime = TimestampUtils.getTimestamp(openTime,
				"yyyy-MM-dd HH:mm:ss");
		long currentTime = System.currentTimeMillis();
		return currentTime >= bidOpentime;
	}

	public static void main(String[] args)
	{
		// System.out.println("123");
		// Record<String, String> params = new RecordImpl<String, String>();
		// params.setColumn("TENDERPROJECTCODE", "0123456789");
		try
		{
			// // 获取头部Token
			// Record<String, String> headMap = new RecordImpl<String,
			// String>();
			// headMap.setColumn(
			// "Authorization",
			// "Basic "
			// + TokenUtils
			// .generateToken("SYS_EKB_HW", "b546f85c"));
			// // 投标企业信息请求url
			// String url =
			// "http://ebid-data-exchange-fjjt-test.okap.com/authorize/ekb/entBidInfo";
			// String json = null;
			// json = HttpClientUtils.doGet(url, params, headMap,
			// ConstantEOKB.DEFAULT_CHARSET);
			// System.out.println(json);
			System.out.println(GenerateCodeUtils.generateNextCode(null, "003",
					3, 3));
		}
		catch (Exception e)
		{

		}
	}
}
