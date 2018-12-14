/**
 * 包名：com.sozone.eokb.bus.createFile
 * 文件名：CreateFileSYGC.java<br/>
 * 创建时间：2017-10-20 下午2:06:55<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.createFile;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.beetl.core.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.HttpMethod;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.authorize.annotation.Level;
import com.sozone.aeolus.authorize.annotation.Permission;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.authorize.utlis.SystemParamUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.data.AeolusData;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.utils.DateUtils;
import com.sozone.aeolus.utils.FileUtils;
import com.sozone.aeolus.view.BeetlView;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.fjs_sygc.common.SygcUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 水运工程生成doc服务接口<br/>
 * <p>
 * 水运工程生成doc服务接口<br/>
 * </p>
 * Time：2017-10-20 下午2:06:55<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/sygc/bus/createFile", desc = "创建文件")
// 登录即可访问
@Permission(Level.Authenticated)
public class CreateFileSYGC extends BaseAction
{
	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(CreateFileGSGL.class);

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
	 * 创建doc文件<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return
	 * @throws Exception
	 *             Exception
	 */
	// 定义路径
	@Path(value = "/doc", desc = "创建DOC文件")
	// GET访问方式
	@HttpMethod(HttpMethod.POST)
	public void buildDocFile(AeolusData data) throws Exception
	{
		logger.debug(LogUtils.format("创建DOC文件", data));
		String url = "";
		String fileName = "";
		String template = data.getParam("template");
		String type = data.getParam("modelType");
		Record<String, Object> model = new RecordImpl<String, Object>();
		String tpid = SessionUtils.getTPID();

		if (StringUtils.equals("DYXF_credit", type))// 投标人所投标段组及信用组
		{
			template = SessionUtils.getTenderProjectTypeCode();
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/bidOpen/credit.doc.html";
			fileName = "/credit.doc";
			model.put("SECTION_LIST", SygcUtils.getDecryptSituation(tpid));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_BSPM_YAOHAO", type))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/bidOpen/bspm.doc.html";
			fileName = "/bspm.doc";

			model.put("TENDER_PROJECT_BSPM_LIST",
					SygcUtils.getBidStandardPriceMethodView(tpid, ""));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_HIGHEST_WEIGHT", type))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/bidOpen/hw.doc.html";
			fileName = "/hw.doc";
			// 最高权重视图
			model.put("TENDER_PROJECT_HW_LIST",
					SygcUtils.getHighestWeightView(tpid, ""));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_LOWER_COEFFICIENT", type))
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/bidOpen/lower.doc.html";
			fileName = "/lower.doc";
			// 下浮系数视图
			model.put("TENDER_PROJECT_LC_LIST",
					SygcUtils.getLowerCoefficientView(tpid, ""));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_E_VALUE", type))
		{
			template = SessionUtils.getTenderProjectTypeCode();
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/bidOpen/e.value.doc.html";
			fileName = "/ev.doc";
			model.put("TENDER_PROJECT_EV_LIST", SygcUtils.getEValueRecord(tpid));

			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("record", type))
		{
			template = SessionUtils.getTenderProjectTypeCode();
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/bidOpen/bid.record.doc.html";
			fileName = "/firstRecord.doc";
			model.putAll(SygcUtils.getOpenBidRecordForm(tpid));
			// 项目名称
			model.put("PROJECT_NAME", SessionUtils.getBidProjectName());
			// 开标时间
			String vTime = SessionUtils.getBidOpenTime();
			model.put("TIME", DateFormatUtils.format(
					DateUtils.parseDate(vTime, "yyyy-MM-dd HH:mm:ss"),
					"yyyy年MM月dd日 HH时mm分"));
			createDocFile(url, model, fileName);
		}
	}

	/**
	 * 生成doc文件<br/>
	 * <p>
	 * </p>
	 * 
	 * @param url
	 * @param model
	 * @param fileName
	 * @throws ServiceException
	 */
	private void createDocFile(String url, Record<String, Object> model,
			String fileName) throws ServiceException
	{
		try
		{
			// 添加DOC文件
			StringBuffer buf = new StringBuffer();
			String header = new BeetlView().converthtmlByNone(new ModelAndView(
					"/default/eokb/bus/template/kb.report.css.html"));
			buf.append(header);
			Template bodyUrl = new BeetlView()
					.convertToTemplate(new ModelAndView(url, model));
			buf.append(bodyUrl.render());
			buf.append("</div></body></html>");
			String fileUrl = SystemParamUtils
					.getString("aeolus.ebidKB.file.path.url")
					+ "bidReport/"
					+ SessionUtils.getTPID();
			File dir = new File(fileUrl);
			// 判断文件夹是否存在
			if (!dir.exists())
			{
				dir.mkdirs();
			}
			url = fileUrl + fileName;
			FileUtils.write(new File(url), buf.toString(),
					ConstantEOKB.DEFAULT_CHARSET);
		}
		catch (Exception e)
		{
			logger.error(LogUtils.format("生成DOC文件失败!"), e);
			throw new ServiceException("", "生成DOC文件失败!");
		}
	}
}
