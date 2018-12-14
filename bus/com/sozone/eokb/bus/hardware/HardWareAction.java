/**
 * 包名：com.sozone.eokb.bus.hardware
 * 文件名：HardWareAction.java<br/>
 * 创建时间：2018-9-26 下午2:06:48<br/>
 * 创建者：wengdm<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.hardware;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.base.utils.id.SZUtilsID;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.eokb.bus.createFile.CreateFileFJSZ;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;
import com.sozone.eokb.xms_fjsz.common.FjszUtils;
import com.strongsoft.encrypt.EncryptTools;

/**
 * 施工软硬件信息<br/>
 * <p>
 * 施工软硬件信息<br/>
 * </p>
 * Time：2018-9-26 下午2:06:48<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "hwa", desc = "施工软硬件信息")
@Permission(Level.Authenticated)
public class HardWareAction extends BaseAction
{

	/**
	 * 软硬件信息锁解密锁
	 */
	private static final Map<String, ReentrantLock> HARDWARE_INFO_LOCK = new ConcurrentHashMap<String, ReentrantLock>();

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(HardWareAction.class);

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
	 * 获取软硬件信息锁<br/>
	 * <p>
	 * 为了防止同一个项目在同一时间段内重复初始化,所有为每一个项目加了一个获取软硬件锁,该方法用于创建项目单例互斥锁
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @return 互斥锁锁对象
	 */
	private static synchronized ReentrantLock getHWInfoLock(String tpid)
	{
		// 获取项目锁
		ReentrantLock lock = HARDWARE_INFO_LOCK.get(tpid);
		// 如果锁不存在
		if (null == lock)
		{
			// 构建互斥锁
			lock = new ReentrantLock();
			HARDWARE_INFO_LOCK.put(tpid, lock);
		}
		return lock;
	}

	/**
	 * 
	 * 获取施工软硬件信息<br/>
	 * <p>
	 * 获取施工软硬件信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 软硬件信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "ghwi", desc = "获取施工软硬件信息")
	@Service
	public ModelAndView getHardWareInfo(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("获取施工软硬件信息", data));

		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/hardware_info/hardware.info.info.html",
				getModelInfo());
	}

	/**
	 * 
	 * 生成硬件信息记录文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "bf", desc = "生成硬件信息记录文件")
	@Service
	public void bulidFile(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("生成硬件信息记录文件", data));
		String tpid = SessionUtils.getTPID();
		String dirUrl = SystemParamUtils
				.getString("aeolus.ebidKB.file.path.url") + "bidReport/" + tpid;

		File dir = new File(dirUrl);
		// 判断文件夹是否存在
		if (!dir.exists())
		{
			dir.mkdirs();
		}
		String fileUrl = dirUrl + "/secondRecord.doc";
		File docFile = new File(fileUrl);
		if (docFile.exists())
		{
			return;
		}

		// 文件不存在，先生成文件
		CreateFileFJSZ cf = new CreateFileFJSZ();
		cf.createhardwareInfoRecordDoc(data, tpid);
	}

	/**
	 * 
	 * 获取软硬件信息<br/>
	 * <p>
	 * 获取软硬件信息
	 * </p>
	 * 
	 * @return 软硬件信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	public static Record<String, Object> getModelInfo() throws FacadeException
	{
		logger.debug(LogUtils.format("获取软硬件信息"));

		String tpid = SessionUtils.getTPID();
		// -----------------------------------------------------------------
		// 获取当前项目的解密锁
		// 这样写其实还是有问题的，如果在极端情况下，线程执行到这一步，被挂起，此时有可能会有线程已经把获取到的锁从缓存中干掉了。
		ReentrantLock lock = getHWInfoLock(tpid);
		logger.info(LogUtils.format("获取锁实例成功"));
		// 获取锁,这一步其实就是为了让线程挂起
		lock.lock();
		logger.info(LogUtils.format("获取锁成功"));

		try
		{
			// 获取项目信息
			Record<String, Object> project = ActiveRecordDAOImpl.getInstance()
					.pandora()
					.SELECT_ALL_FROM(TableName.EKB_T_TENDER_PROJECT_INFO)
					.EQUAL("ID", tpid).get();
			if (CollectionUtils.isEmpty(project))
			{
				throw new ServiceException("", "未获取到招标项目信息");
			}
			Record<String, Object> section = ActiveRecordDAOImpl.getInstance()
					.pandora().SELECT_ALL_FROM(TableName.EKB_T_SECTION_INFO)
					.EQUAL("V_TPID", tpid).get();
			if (CollectionUtils.isEmpty(section))
			{
				throw new ServiceException("", "未获取到标段信息");
			}
			Record<String, Object> model = new RecordImpl<String, Object>();

			// 先查询是否过已有记录
			Record<String, Object> hardwareInfo = ActiveRecordDAOImpl
					.getInstance().pandora().SELECT("V_JSON_OBJ")
					.FROM(TableName.TENDER_PROJECT_NODE_DATA)
					.EQUAL("V_BUS_FLAG_TYPE", "HARDWARE_INFO")
					.EQUAL("V_TPID", tpid).get();
			if (!CollectionUtils.isEmpty(hardwareInfo))
			{
				model.put("BIDDERS", hardwareInfo.getJSONArray("V_JSON_OBJ"));
				model.put("SECTIONS", section.getString("V_BID_SECTION_NAME"));
				// 项目名称
				model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
				return model;
			}

			Record<String, Object> param = new RecordImpl<String, Object>();
			param.setColumn("tpid", tpid);
			List<Record<String, Object>> bidders = ActiveRecordDAOImpl
					.getInstance().statement()
					.selectList("xms_fjsz_common.getBidderMacInfo", param);
			if (CollectionUtils.isEmpty(bidders))
			{
				throw new ServiceException("", "未获取到投标人信息");
			}
			// 加密锁序列号列表
			Map<String, Set<String>> jmsxlhMap = new HashMap<String, Set<String>>();
			// 硬件信息列表
			Map<String, Set<String>> hardInfoMap = new HashMap<String, Set<String>>();

			// 投标文件投递mac地址集
			JSONArray macsJarr = new JSONArray();
			JSONObject macsJobj = null;
			// mac地址集
			List<String> aMacs = new ArrayList<String>();
			List<String> bMacs = new ArrayList<String>();
			Map<String, String> aMacMap = new HashMap<String, String>();
			Map<String, String> bMacMap = new HashMap<String, String>();

			String tempMac = null;

			List<String> cpuids = new ArrayList<String>();
			String rootPath = SystemParamUtils.getString(
					SysParamKey.EBIDKB_DECRYPTFILE_PATH_URL,
					"D:/fileEbid-fileTb_decrypt/")
					+ project.getString("V_TENDER_PROJECT_CODE")
					+ File.separator;

			// 评标办法55：简易评标法，没有xml清单
			if (55 == project.getInteger("V_BID_EVALUATION_METHOD_TYPE"))
			{
				for (Record<String, Object> bidder : bidders)
				{
					// 无需解析工程量清单，给个空的
					bidder.setColumn("XML_JSON_LIST",
							new ArrayList<JSONObject>());

					macsJarr = bidder.getJSONArray("V_JSON_OBJ");
					if (null == bidder.getList("V_JSON_OBJ"))
					{
						bidder.setColumn("A_MAC_INFO", new JSONObject());
						bidder.setColumn("B_MAC_INFO", new JSONObject());
						continue;
					}
					// 循环解析投递Mac上传地址
					// TYPE-----'A:表示投标文件生成，B:表示投标文件投递，C:表示投标文件暂存';
					for (int i = 0; i < macsJarr.size(); i++)
					{
						macsJobj = macsJarr.getJSONObject(i);
						if (CollectionUtils.isEmpty(macsJobj))
						{
							if (CollectionUtils.isEmpty(bidder
									.getJSONObject("A_MAC_INFO")))
							{
								bidder.setColumn("A_MAC_INFO", new JSONObject());
							}
							if (CollectionUtils.isEmpty(bidder
									.getJSONObject("B_MAC_INFO")))
							{
								bidder.setColumn("B_MAC_INFO", new JSONObject());
							}
							continue;
						}
						tempMac = macsJobj.getString("MAC")
								+ macsJobj.getString("CPUID")
								+ macsJobj.getString("DISKID");
						if (StringUtils.equals(ConstantEOKB.TB_TYPE_A,
								macsJobj.getString("TYPE")))
						{
							bidder.setColumn("A_MAC_INFO", macsJobj);
							bidder.setColumn("A_MAC", macsJobj.getString("MAC"));
							bidder.setColumn("A_MAC_CONTENT", tempMac);
							aMacs.add(tempMac);
							aMacMap.put(bidder.getString("V_BIDDER_ORG_CODE"),
									tempMac);
							continue;
						}
						bidder.setColumn("B_MAC_INFO", macsJobj);
						bidder.setColumn("B_MAC", macsJobj.getString("MAC"));
						bidder.setColumn("B_MAC_CONTENT", tempMac);
						bMacMap.put(bidder.getString("V_BIDDER_ORG_CODE"),
								tempMac);
						bMacs.add(tempMac);
					}
				}
			}
			else
			{
				// 工程量清单路径
				String xmlPath = "";

				long priceTime = parseTime(section
						.getString("V_CONTROLPRICE_ISSUETIME"));

				for (Record<String, Object> bidder : bidders)
				{
					xmlPath = getXmlPath(rootPath,
							bidder.getString("V_BIDDER_ORG_CODE"),
							bidder.getString("V_FILEADDR"));

					parseXml(xmlPath, bidder, jmsxlhMap, hardInfoMap, priceTime);

					macsJarr = bidder.getJSONArray("V_JSON_OBJ");
					if (null == bidder.getList("V_JSON_OBJ"))
					{
						bidder.setColumn("A_MAC_INFO", new JSONObject());
						bidder.setColumn("B_MAC_INFO", new JSONObject());
						continue;
					}
					// 循环解析投递Mac上传地址
					// TYPE-----'A:表示投标文件生成，B:表示投标文件投递，C:表示投标文件暂存';
					for (int i = 0; i < macsJarr.size(); i++)
					{
						macsJobj = macsJarr.getJSONObject(i);
						if (CollectionUtils.isEmpty(macsJobj))
						{
							if (CollectionUtils.isEmpty(bidder
									.getJSONObject("A_MAC_INFO")))
							{
								bidder.setColumn("A_MAC_INFO", new JSONObject());
							}
							if (CollectionUtils.isEmpty(bidder
									.getJSONObject("B_MAC_INFO")))
							{
								bidder.setColumn("B_MAC_INFO", new JSONObject());
							}
							continue;
						}
						tempMac = macsJobj.getString("MAC")
								+ macsJobj.getString("CPUID")
								+ macsJobj.getString("DISKID");
						if (StringUtils.equals(ConstantEOKB.TB_TYPE_A,
								macsJobj.getString("TYPE")))
						{
							bidder.setColumn("A_MAC_INFO", macsJobj);
							bidder.setColumn("A_MAC", macsJobj.getString("MAC"));
							bidder.setColumn("A_MAC_CONTENT", tempMac);
							aMacs.add(tempMac);
							aMacMap.put(bidder.getString("V_BIDDER_ORG_CODE"),
									tempMac);
							continue;
						}
						bidder.setColumn("B_MAC_INFO", macsJobj);
						bidder.setColumn("B_MAC", macsJobj.getString("MAC"));
						bidder.setColumn("B_MAC_CONTENT", tempMac);
						bMacs.add(tempMac);
						bMacMap.put(bidder.getString("V_BIDDER_ORG_CODE"),
								tempMac);
					}
				}
			}

			// 重复的加密锁序列号列表
			List<String> jmsxlhs = disjunction(jmsxlhMap);
			// 重复的硬件地址列表
			List<String> hardInfo = disjunction(hardInfoMap);
			// 重复制作mac地址列表
			aMacs = FjszUtils.disjunction(aMacs);
			// 重复投递mac地址列表
			bMacs = FjszUtils.disjunction(bMacs);
			// 重复的网卡地址列表
			cpuids = FjszUtils.disjunction(cpuids);

			// 判断哪些企业检测项重复
			List<JSONObject> tempList = null;
			JSONObject tempJson = null;

			// 投递mac地址是否重复标识
			for (Record<String, Object> bidder : bidders)
			{
				bidder.setColumn(
						"IS_A_MAC_SAME",
						aMacs.contains(bidder.getString("A_MAC_CONTENT"))
								|| compareMac(
										bidder.getString("V_BIDDER_ORG_CODE"),
										bidder.getString("A_MAC_CONTENT"),
										bMacMap));

				bidder.setColumn(
						"IS_B_MAC_SAME",
						bMacs.contains(bidder.getString("B_MAC_CONTENT"))
								|| compareMac(
										bidder.getString("V_BIDDER_ORG_CODE"),
										bidder.getString("B_MAC_CONTENT"),
										aMacMap));

				tempList = bidder.getList("XML_JSON_LIST");

				for (JSONObject jobj : tempList)
				{
					tempJson = jobj.getJSONObject("HARDINFO");
					tempJson.put("IS_SAME",
							hardInfo.contains(tempJson.getString("HARDINFO")));
					tempJson = jobj.getJSONObject("JMSXLH");
					tempJson.put("IS_SAME",
							jmsxlhs.contains(tempJson.getString("JMSXLH")));
				}
				bidder.setColumn("COL_NUM", tempList.size());
			}
			String jsonStr = JSON.toJSONString(bidders);

			// 保存数据
			Record<String, Object> tpData = new RecordImpl<String, Object>();
			tpData.setColumn("V_BUS_FLAG_TYPE", "HARDWARE_INFO");
			tpData.setColumn("V_TPID", tpid);
			tpData.setColumn("ID", SZUtilsID.getUUID());
			tpData.setColumn("V_JSON_OBJ", jsonStr);
			tpData.setColumn("V_CREATE_USER",
					ApacheShiroUtils.getCurrentUserID()).setColumn(
					"N_CREATE_TIME", System.currentTimeMillis());
			ActiveRecordDAOImpl.getInstance().pandora()
					.INSERT_INTO(TableName.TENDER_PROJECT_NODE_DATA)
					.VALUES(tpData).excute();
			model.put("BIDDERS", bidders);
			model.put("SECTIONS", section.getString("V_BID_SECTION_NAME"));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			return model;
		}
		finally
		{
			// 如果已经没有线程等待获取锁
			if (!lock.hasQueuedThreads())
			{
				// 干掉锁
				HARDWARE_INFO_LOCK.remove(tpid);
			}
			// 释放锁,这里一定要注意，释放锁一定要放在干掉后面，否则会出现干掉的时候有另一个线程其实已经获取的锁
			lock.unlock();
		}
	}

	/**
	 * 
	 * 交叉比对投递和编制的mac地址<br/>
	 * <p>
	 * 交叉比对投递和编制的mac地址
	 * </p>
	 * 
	 * @param orgCode
	 * @param mac
	 * @param macMap
	 * @return
	 */
	private static boolean compareMac(String orgCode, String mac,
			Map<String, String> macMap)
	{
		for (String key : macMap.keySet())
		{
			// 排除自己
			if (StringUtils.equals(key, orgCode))
			{
				continue;
			}
			if (StringUtils.equals(macMap.get(key), mac))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * 获取工程量 清单路径<br/>
	 * <p>
	 * 获取工程量 清单路径
	 * </p>
	 * 
	 * @param xmlPath
	 *            xml前半部分路径
	 * @param orgCode
	 *            组织机构号码
	 * @param fileAddr
	 *            投递文件名称
	 * @return 工程量 清单路径
	 */
	public static String getXmlPath(String xmlPath, String orgCode,
			String fileAddr)
	{
		logger.debug(LogUtils.format("获取工程量 清单路径", xmlPath, orgCode));
		String tag = File.separator;

		String jsonPath = xmlPath + orgCode + tag + "ZipFolder" + tag
				+ "0-目录描述.json";
		File jsonFile = new File(jsonPath);
		if (!jsonFile.isFile())
		{
			logger.error(LogUtils.format("获取目录文件.json失败", jsonPath));
			return "";
		}

		try
		{
			String json = FileUtils.readFileToString(jsonFile,
					ConstantEOKB.DEFAULT_CHARSET);

			// 如果文件时空的
			if (StringUtils.isEmpty(json))
			{
				logger.error("", "无效的目录文件信息描述文件!");
				return "";
			}
			JSONArray array = JSON.parseArray(json);
			for (int i = 0; i < array.size(); i++)
			{
				JSONObject jobj = array.getJSONObject(i);
				if (StringUtils.equals("1", jobj.getString("QINGBIAO_XML")))
				{
					return jobj.getString("PATH");
				}
			}

		}
		catch (IOException e)
		{
			logger.error("", "无效的目录文件信息描述文件!");
			return "";

		}
		return "";
	}

	/**
	 * 
	 * 筛选出重复数据<br/>
	 * <p>
	 * 筛选出重复数据
	 * </p>
	 * 
	 * @param map
	 *            需要筛选的数据集
	 * 
	 * @return 重复数据的集
	 */
	public static List<String> disjunction(Map<String, Set<String>> map)
	{
		logger.debug(LogUtils.format("筛选出重复数据", map));
		Map<String, Set<String>> tempMap = new HashMap<String, Set<String>>();
		tempMap.putAll(map);

		List<String> sameList = new ArrayList<String>();
		// 循环每个投标人
		for (String key : map.keySet())
		{
			// 将每次循环过的投标人剔除掉
			tempMap.remove(key);
			// 获取需要筛选的集合
			Set<String> set = map.get(key);
			// 循环每个属性
			for (String val : set)
			{
				for (Set<String> tempSet : tempMap.values())
				{
					if (tempSet.contains(val))
					{
						sameList.add(val);
						break;
					}
				}
			}
		}

		return sameList;
	}

	/**
	 * 
	 * 解析工程量清单<br/>
	 * <p>
	 * 解析工程量清单
	 * </p>
	 * 
	 * @param xmlPath
	 *            工程量清单路径
	 * @param bidder
	 *            投标人
	 * @param jmsxlhs
	 *            加密锁序列号列表
	 * @param cpuxlhs
	 *            cpu地址列表
	 * @param wkdzs
	 *            网卡地址列表
	 * @throws ServiceException
	 *             ServiceException
	 */
	private static void parseXml(String xmlPath, Record<String, Object> bidder,
			Map<String, Set<String>> jmsxlhMap,
			Map<String, Set<String>> hardInfoMap, long priceTime)
			throws ServiceException
	{
		logger.debug(LogUtils.format("解析工程量清单", bidder, xmlPath));
		// 判断文件是否存在
		File xml = new File(xmlPath);
		if (!xml.isFile())
		{
			logger.error(LogUtils.format("获取工程量清单失败", bidder, xmlPath));
			bidder.setColumn("XML_JSON_LIST", new ArrayList<JSONObject>());
			return;
		}
		try
		{
			SAXReader reader = new SAXReader();
			Document dom = reader.read(xml);
			Element root = dom.getRootElement();

			Element xtxx = root.element("XTXX");
			Element pyjxx = xtxx.element("RYJXX");

			String log = pyjxx.asXML();
			// 被加密内容
			log = log.substring(log.indexOf(">") + 1, log.lastIndexOf("<"));

			// 密文
			Attribute xxmw = pyjxx.attribute("XXMW");
			String sign = xxmw.getText();

			// 比对密文是否一致 1:一致，0：失败，-3密钥错误
			int result = EncryptTools.getInstance().SignVerify(log, sign,
					"93b198d6bf484e199614b14cc64d5b34");
			boolean encryptFlag = true;
			if (1 != result)
			{
				encryptFlag = false;
			}
			bidder.setColumn("ENCRYPT_FLAG", encryptFlag);

			// 获取最后一条记录的硬件信息
			List<Element> es = pyjxx.elements();
			if (null == es)
			{
				return;
			}

			Set<String> jmsxlhSet = new HashSet<String>();
			Set<String> hardInfoSet = new HashSet<String>();
			List<JSONObject> list = new ArrayList<JSONObject>();
			JSONObject parentJson = null;
			JSONObject childJson = null;
			for (Element e : es)
			{
				parentJson = new JSONObject();
				childJson = new JSONObject();
				// 固化时间
				Attribute attr = e.attribute("GHSJ");
				String ghsj = attr.getText();
				ghsj = comparaTime(priceTime, ghsj);
				if (StringUtils.isEmpty(ghsj))
				{
					continue;
				}
				parentJson.put("GHSJ", ghsj);

				// 加密锁序列号
				attr = e.attribute("JMSXLH");
				String jmsxlh = attr.getText();
				jmsxlhSet.add(jmsxlh);
				childJson.put("JMSXLH", jmsxlh);
				childJson.put("IS_SAME", false);
				parentJson.put("JMSXLH", childJson);

				// 网卡MAC地址
				attr = e.attribute("WKDZ");
				String wkdz = attr.getText();

				// CPU序列号
				attr = e.attribute("CPUXLH");
				String cpuxlh = attr.getText();

				// 硬盘序列号
				attr = e.attribute("YPXLH");
				String ypxlh = attr.getText();

				hardInfoSet.add(wkdz + cpuxlh + ypxlh);
				childJson = new JSONObject();
				childJson.put("HARDINFO", wkdz + cpuxlh + ypxlh);
				childJson.put("IS_SAME", false);
				childJson.put("WKDZ", wkdz);
				childJson.put("CPUXLH", cpuxlh);
				childJson.put("YPXLH", ypxlh);
				parentJson.put("HARDINFO", childJson);
				list.add(parentJson);
			}

			jmsxlhMap.put(bidder.getString("V_BIDDER_ORG_CODE"), jmsxlhSet);
			hardInfoMap.put(bidder.getString("V_BIDDER_ORG_CODE"), hardInfoSet);
			bidder.setColumn("XML_JSON_LIST", list);
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("解析xml失败", e.getMessage()));
		}
	}

	/**
	 * 
	 * 解析工程量清单<br/>
	 * <p>
	 * 解析工程量清单
	 * </p>
	 * 
	 * @param xmlPath
	 *            xml 路径
	 * @param bidder
	 *            投标人
	 */
	public static void parseXmlContent(String xmlPath,
			Record<String, Object> bidder)
	{

		logger.debug(LogUtils.format("解析工程量清单", bidder, xmlPath));
		// 判断文件是否存在
		File xml = new File(xmlPath);
		if (!xml.isFile())
		{
			logger.error(LogUtils.format("获取工程量清单失败", bidder, xmlPath));
			bidder.setColumn("VALUATION_XML", "");
		}
		try
		{
			SAXReader reader = new SAXReader();
			Document dom = reader.read(xml);
			Element root = dom.getRootElement();

			Element xtxx = root.element("XTXX");
			Element pyjxx = xtxx.element("RYJXX");

			bidder.setColumn("VALUATION_XML",
					null == pyjxx ? "" : pyjxx.asXML());
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("解析xml失败", e.getMessage()));
		}
	}

	/**
	 * 
	 * 比较固化时间和控制价发布时间<br/>
	 * <p>
	 * 比较固化时间和控制价发布时间
	 * </p>
	 * 
	 * @param priceTime
	 * @param times
	 * @return
	 */
	private static String comparaTime(long priceTime, String times)
	{
		// 截取第一个固化时间
		Date date = DateUtils.parseDate(times.split(",")[0], "yyyyMMddHHmmss");
		if (priceTime >= date.getTime())
		{
			return null;
		}
		return DateUtils.formatDate(date, "yyyy-MM-dd HH:mm:ss");
	}

	private static long parseTime(String time)
	{
		if (StringUtils.isEmpty(time))
		{
			return new Date().getTime();
		}
		Date date = DateUtils.parseDate(time, "yyyy-MM-dd HH:mm:ss");
		return date.getTime();
	}
}
