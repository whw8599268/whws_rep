/**
 * 包名：com.sozone.eokb.bus.benchmark
 * 文件名：benchmark.java<br/>
 * 创建时间：2017-11-16 下午12:32:35<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.benchmark;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.PathParam;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.servlet.ModelMap;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.bus.decrypt.service.BidderElementParseUtils;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 评标基准价计算接口<br/>
 * <p>
 * 评标基准价计算接口<br/>
 * </p>
 * Time：2017-11-16 下午12:32:35<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/benchmark", desc = "评标基准价计算接口")
// 登录即可访问
@Permission(Level.Authenticated)
public class Benchmark extends BaseAction
{

	private static final NumberFormat FMT_D = new DecimalFormat("###,##0",
			new DecimalFormatSymbols());
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory.getLogger(Benchmark.class);

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
	 * 获取所选标段投标人列表<br/>
	 * 
	 * @param sid
	 *            sid
	 * @return 状态值
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "getbidders/{sid}", desc = "获取所选标段投标人列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public List<Record<String, Object>> getStatus(@PathParam("sid") String sid)
			throws FacadeException
	{
		logger.debug(LogUtils.format("获取所选标段投标人列表", sid));
		String tpid = SessionUtils.getTPID();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("sid", sid);
		param.setColumn("status", 1);
		List<Record<String, Object>> bidders = this.activeRecordDAO.auto()
				.table(TableName.EKB_T_TENDER_LIST)
				.setCondition("AND", "V_TPID=#{tpid}")
				.setCondition("AND", "V_BID_SECTION_ID=#{sid}")
				.setCondition("AND", "N_ENVELOPE_1=1")
				.addSortOrder("V_BIDDER_NO", "ASC").list(param);

		if (CollectionUtils.isEmpty(bidders))
		{
			throw new FacadeException("", "该标段没有投标人");
		}

		for (Record<String, Object> bidder : bidders)
		{
			bidder.setColumn(
					"tbRatingsInEvl",
					BidderElementParseUtils.getSingObjAttribute(
							bidder.getString("V_JSON_OBJ"), "tbRatingsInEvl"));
		}
		logger.debug(LogUtils.format("获取所选标段投标人列表完成", bidders));
		return bidders;
	}

	/**
	 * 保存选中的投标人，并且获取评标办法信息<br/>
	 * <p>
	 * 保存选中的投标人，并且获取评标办法信息
	 * </p>
	 * 
	 * @param ids
	 *            资产ID字符拼接
	 * @return 评标办法信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 声明路径
	@Path(value = "/savebidders/{ids}", desc = "保存选中的投标人，并且获取评标办法信息")
	// 声明为DELETE方法
	@HttpMethod(HttpMethod.GET)
	public Record<String, Object> saveBidders(@PathParam("ids") String ids)
			throws FacadeException
	{
		logger.debug(LogUtils.format("保存选中的投标人", ids));
		List<Record<String, Object>> bidders = new ArrayList<Record<String, Object>>();
		Record<String, Object> param = new RecordImpl<String, Object>();
		Record<String, Object> bidder = null;
		if (StringUtils.isNotEmpty(ids))
		{
			String[] biddersIds = StringUtils.split(ids, ',');
			for (String id : biddersIds)
			{
				param.clear();
				param.setColumn("ID", id);
				bidder = this.activeRecordDAO.auto()
						.table(TableName.EKB_T_TENDER_LIST).get(param);
				if (!CollectionUtils.isEmpty(bidder))
				{
					// 将投标人添加到缓存
					bidders.add(bidder);
				}
			}
		}

		param.clear();
		param.setColumn("sid", bidders.get(0).getString("V_BID_SECTION_ID"));
		Record<String, Object> section = this.activeRecordDAO.auto()
				.table(ConstantEOKB.TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_BID_SECTION_ID=#{sid}").get(param);

		return BenchmarkUtils.getMethodInfo(bidders, section);
	}

	/**
	 * 
	 * 获取下浮信息<br/>
	 * <p>
	 * 获取下浮信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 下浮信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/saveMethod", desc = "获取下浮信息")
	@HttpMethod(HttpMethod.POST)
	public Record<String, Object> saveMethod(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("计算基准价", data));
		String method = data.getParam("method");
		return BenchmarkUtils.getDownInfo(Integer.parseInt(method));
	}

	/**
	 * 
	 * 计算基准价信息<br/>
	 * <p>
	 * 获取基准价信息
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @return 基准价
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/saveDown", desc = "计算基准价")
	@HttpMethod(HttpMethod.POST)
	public String saveDown(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("计算基准价", data));
		String down = data.getParam("down");
		double avg = (Double) ApacheShiroUtils.getSession().getAttribute(
				"BID_QI");
		double benchmark = 0D;
		if (StringUtils.contains(down, "-"))
		{
			benchmark = Math.round(Math.round(avg)
					* (1 + Double.parseDouble(down.replaceAll("-", ""))));
			String msg = "评标基准价计算公式#{style}ri=qi*(1-f1)</a>，评标基准价#{style}"
					+ FMT_D.format(benchmark) + "=" + FMT_D.format(avg)
					+ "*(1 +" + down.replaceAll("-", "") + ")</a>";
			return BenchmarkUtils.replaceStyle(msg);
		}
		benchmark = Math
				.round(Math.round(avg) * (1 - Double.parseDouble(down)));
		String msg = "评标基准价计算公式#{style}ri=qi*(1-f1)</a>，评标基准价#{style}"
				+ FMT_D.format(benchmark) + "=" + FMT_D.format(avg) + "*(1- "
				+ down + ")</a>";
		return BenchmarkUtils.replaceStyle(msg);
	}

	/**
	 * 
	 * 获取评标基准价列表<br/>
	 * <p>
	 * 获取评标基准价列表
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param sid
	 *            标段ID
	 * @return 评标基准价列表
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/record/{sid}", desc = "获取评标基准价列表")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public ModelAndView getMethodBySectionCode(AeolusData data,
			@PathParam("sid") String sid) throws FacadeException
	{
		logger.debug(LogUtils.format("获取评标基准价列表", data));

		ModelMap model = new ModelMap();
		String tpid = SessionUtils.getTPID();

		Record<String, Object> section = BenchmarkUtils
				.getBenchamarkInfoByParam(tpid, sid);

		model.put("SECTION_LIST", section);
		return new ModelAndView(getTheme(data.getHttpServletRequest())
				+ "/eokb/bus/benchmark/benchmark.method.list.html", model);
	}
}
