/**
 * 包名：com.sozone.eokb.fjs_gsgl_ljsg_hldjf_v1
 * 文件名：CreateFile.java<br/>
 * 创建时间：2017-9-18 下午2:26:17<br/>
 * 创建者：LDH<br/>
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import com.sozone.aeolus.download.util.AeolusDownloadUtils;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.servlet.BaseAction;
import com.sozone.aeolus.servlet.ModelAndView;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.aeolus.utils.FileUtils;
import com.sozone.aeolus.view.BeetlView;
import com.sozone.eokb.common.ConstantEOKB;
import com.sozone.eokb.fjs_gsgl.common.GsglUtils;
import com.sozone.eokb.utils.ListSortUtils;
import com.sozone.eokb.utils.SessionUtils;

/**
 * 创建文件<br/>
 * <p>
 * 创建文件<br/>
 * </p>
 * Time：2017-9-18 下午2:26:17<br/>
 * 
 * @author LDH
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/gsgl/bus/createFile", desc = "创建文件")
// 登录即可访问
@Permission(Level.Authenticated)
public class CreateFileGSGL extends BaseAction
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
		String template = SessionUtils.getTenderProjectTypeCode();
		String type = data.getParam("modelType");
		String tpid = SessionUtils.getTPID();
		// 关联企业
		if (StringUtils.equals(type, "correlate"))
		{
			createCorrelateFile(data);
			return;
		}

		// 合理低价法
		if (StringUtils.contains(template, "hldjf")
				|| StringUtils.contains(template, "hldfj")
				|| StringUtils.equals(template,
						"fjs_gsgl_jgjg_ysqzljc_zhpff_v1")
				|| StringUtils.equals(template,
						"fjs_gsgl_jgjg_ysqzljc_zhpff_v2"))
		{
			getBidRecordsForHldjf(data, tpid, type, template);
			return;
		}
		// 综合评估法
		getBidRecordsForZhpgf(data, tpid, type, template);
	}

	/**
	 * 下载信用等级doc文件<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @return
	 * @throws FacadeException
	 *             FacadeException
	 */
	// 定义路径
	@Path(value = "/creditDoc", desc = "下载信用等级doc文件")
	// GET访问方式
	@HttpMethod(HttpMethod.GET)
	public void downloadCreditDoc(AeolusData data) throws FacadeException
	{
		logger.debug(LogUtils.format("下载信用等级doc文件", data));
		String template = SessionUtils.getTenderProjectTypeCode();
		String tpid = SessionUtils.getTPID();
		String type = "";
		// 合理低价法
		if (StringUtils.contains(template, "hldjf")
				|| StringUtils.contains(template, "hldfj")
				|| StringUtils.equals(template,
						"fjs_gsgl_jgjg_ysqzljc_zhpff_v1"))
		{
			type = "DYXF_offer";
			getBidRecordsForHldjf(data, tpid, type, template);
		}
		// 综合评估法
		else
		{
			type = "DYXF_credit";
			getBidRecordsForZhpgf(data, tpid, type, template);
		}

		String fileUrl = SystemParamUtils
				.getString(ConstantEOKB.SysParamKey.EBIDKB_FILE_PATH_URL)
				+ "bidReport"
				+ File.separator
				+ SessionUtils.getTPID()
				+ File.separator + "firstRecord.doc";
		String comment = "信用等级";
		// 获取开标记录文件
		File file = new File(fileUrl);
		// 下载开标记录文件
		try
		{
			InputStream input = new FileInputStream(file);
			AeolusDownloadUtils.write(data, input, comment + ".doc");
		}
		catch (FileNotFoundException e)
		{
			throw new ValidateException("", "下载开标记录文件出现异常!");
		}
	}

	/**
	 * 
	 * 生成关联企业记录doc<br/>
	 * <p>
	 * 生成关联企业记录doc
	 * </p>
	 * 
	 * @param data
	 *            AeolusData
	 * @param tpid
	 *            招标项目ID
	 * @throws FacadeException
	 * @throws IOException
	 */
	private void createCorrelateFile(AeolusData data) throws FacadeException,
			IOException
	{
		logger.debug(LogUtils.format("生成关联企业记录doc", data));
		Record<String, Object> model = new RecordImpl<String, Object>();
		String url = null;
		String fileName = "/correlate.doc";
		if (SessionUtils.isSectionGroup())
		{
			url = getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/setting_fail_bidder/setting.fail.bidder.group.doc.html";
			model.put("SECTION_LIST", GsglUtils.getCorrelateModelGroup());
		}
		else
		{
			url = getTheme(data.getHttpServletRequest())
					+ "/eokb/bus/setting_fail_bidder/setting.fail.bidder.doc.html";
			model.put("SECTION_LIST", GsglUtils.getCorrelateModel());
		}
		createDocFile(url, model, fileName);
	}

	/**
	 * 
	 * 获取高速公路合理低价法的开标记录表<br/>
	 * <p>
	 * 获取高速公路合理低价法的开标记录表
	 * </p>
	 * 
	 * @param data
	 *            页面请求
	 * @param tpid
	 *            招标项目主键
	 * @param type
	 *            doc文件类型标记
	 * @param template
	 *            范本code
	 * @throws ServiceException
	 *             服务异常
	 */
	private void getBidRecordsForHldjf(AeolusData data, String tpid,
			String type, String template) throws ServiceException
	{
		logger.debug(LogUtils.format("获取高速公路合理低价法的开标记录表", data, tpid, type,
				template));
		String url = null;
		String fileName = null;
		Record<String, Object> model = new RecordImpl<String, Object>();
		if (StringUtils.equals("DYXF_credit", type))// 投标人所投标段组及信用组
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/credit.doc.html";
			fileName = "/firstRecord.doc";
			model.putAll(GsglUtils.getFirstEnvelopeDecryptSituation(tpid, true,
					ConstantEOKB.EOKBFlowCode.DYXF_CREDIT));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DYXF_electronics", type))// 电子摇号
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/electronics.doc.html";
			fileName = "/electronics.doc";
			model.putAll(GsglUtils.getFirstElectronics(tpid, true,
					ConstantEOKB.EOKBFlowCode.DYXF_ELECTRONICS));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DYXF_offer", type))// 第一信封开标结果
		{
			if (SessionUtils.isSectionGroup())
			{
				fileName = "/DYXF_offer.doc";
				url = getTheme(data.getHttpServletRequest()) + "/eokb/"
						+ template + "/firstEnvelope/preview.doc.html";
			}
			else
			{
				fileName = "/firstRecord.doc";
				url = getTheme(data.getHttpServletRequest()) + "/eokb/"
						+ template + "/firstEnvelope/preview.no.group.doc.html";
			}
			model.putAll(GsglUtils.getBidResultViewForGroup(tpid,
					ConstantEOKB.EOKBFlowCode.DYXF_OFFER));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DYXF_review", type))// 第一信封评审结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/review.doc.html";
			fileName = "/review.doc";
			model.putAll(GsglUtils.getfirstReviewView(tpid));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_offer", type))// 第二信封文件解密结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/decrypt.doc.html";
			fileName = "/secondRecord.doc";
			model.putAll(GsglUtils.getSecondDecryptSituation(true, tpid,
					ConstantEOKB.EOKBFlowCode.DEXF_OFFER));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_price", type))// 评标基准价记录
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/benchmark.record.doc.html";
			fileName = "/price.doc";

			if (StringUtils.contains(template, "fjs_gsgl_jgjg_ysqzljc_zhpff"))
			{
				model.putAll(GsglUtils.getSecondDecryptSituation(true, tpid,
						ConstantEOKB.EOKBFlowCode.DEXF_OFFER));
				createDocFile(url, model, fileName);
				return;
			}

			model.putAll(GsglUtils.getBenchmarkRecordView(tpid,
					ConstantEOKB.EOKBFlowCode.DEXF_PRICE));
			createDocFile(url, model, fileName);
		}
	}

	/**
	 * 
	 * 获取高速公路综合评估法的开标记录表<br/>
	 * <p>
	 * 获取高速公路综合评估法的开标记录表
	 * </p>
	 * 
	 * @param data
	 *            页面请求
	 * @param tpid
	 *            招标项目主键
	 * @param type
	 *            doc文件类型标记
	 * @param template
	 *            范本code
	 * @throws ServiceException
	 *             服务异常
	 */
	private void getBidRecordsForZhpgf(AeolusData data, String tpid,
			String type, String template) throws ServiceException
	{
		logger.debug(LogUtils.format("获取高速公路综合评估法的开标记录表", data, tpid, type,
				template));
		String url = null;
		String fileName = null;
		Record<String, Object> model = new RecordImpl<String, Object>();
		if (StringUtils.equals("DYXF_credit", type))// 第一数字信封解密情况一览表

		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/credit.doc.html";
			fileName = "/firstRecord.doc";
			model.putAll(GsglUtils.getfirstDecryptSituationForZhpgf(tpid, true,
					ConstantEOKB.EOKBFlowCode.DYXF_CREDIT));
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DYXF_review", type))// 第一信封评审结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/firstEnvelope/review.doc.html";
			fileName = "/review.doc";
			model.putAll(GsglUtils.getfirstReviewView(tpid));
			// 获取各个标段的投标人并且将他们按照评审得分降序
			List<Record<String, Object>> sections = model
					.getList("SECTION_LIST");
			List<Record<String, Object>> bidders = null;
			for (Record<String, Object> section : sections)
			{
				bidders = section.getList("TENDER_LIST");
				if (CollectionUtils.isEmpty(bidders))
				{
					continue;
				}
				ListSortUtils.sort(bidders, false, "N_TOTAL");
				section.setColumn("TENDER_LIST", bidders);
			}
			model.put("SECTION_LIST", sections);
			createDocFile(url, model, fileName);
			return;
		}
		if (StringUtils.equals("DEXF_offer", type))// 第二信封文件解密结果
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/decrypt.doc.html";
			fileName = "/secondRecord.doc";
			model.putAll(GsglUtils.getSecondDecryptSituation(true, tpid,
					ConstantEOKB.EOKBFlowCode.DEXF_OFFER));
			createDocFile(url, model, fileName);
		}

		if (StringUtils.equals("DEXF_price", type))// 评标基准价记录
		{
			url = getTheme(data.getHttpServletRequest()) + "/eokb/" + template
					+ "/secondEnvelope/benchmark.doc.html";
			fileName = "/price.doc";

			model.putAll(GsglUtils.getSecondDecryptSituation(true, tpid,
					ConstantEOKB.EOKBFlowCode.DEXF_PRICE));
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
