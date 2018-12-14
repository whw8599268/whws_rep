/**
 * 包名：com.sozone.eokb.bus.push
 * 文件名：SystemInspectZip.java<br/>
 * 创建时间：2017-12-21 下午1:43:36<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.push;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sozone.aeolus.annotation.Handler;
import com.sozone.aeolus.annotation.Path;
import com.sozone.aeolus.annotation.Service;
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
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.SysParamKey;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.handler.OperationLogHandler;

/**
 * 系统预清标文件下载<br/>
 * Time：2017-12-21 下午1:43:36<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
@Path(value = "/bus/push", desc = "系统预清标文件下载")
// 登录即可访问
@Permission(Level.Guest)
// 增加操作日志
@Handler(OperationLogHandler.class)
public class SystemInspectZip
{

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(SystemInspectZip.class);

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
	 * 下载招投标文件zip包<br/>
	 * 
	 * @param data
	 *            AeolusData
	 * @throws FacadeException
	 *             FacadeException
	 */
	@Path(value = "/haimai/systemInspectZip", desc = "下载招投标文件zip包")
	@Service
	public void downloadSystemInspectZip(AeolusData data)
			throws FacadeException
	{
		logger.debug(LogUtils.format("下载招投标文件zip包", data));
		String sectionID = data.getParam("sectionID");
		if (StringUtils.isEmpty(sectionID))
		{
			throw new ValidateException("", "标段ID不能为空!");
		}
		File testFile = new File(getRootClassPath()+File.separator+"testHaiMai.zip");
		if(testFile.exists()){
			logger.warn("由于有存在测试文件，系统将直接提供测试文件进行下载："+testFile.getAbsolutePath());
			AeolusDownloadUtils.write(data, testFile);
			return ;
		}
		Record<String, Object> record = this.activeRecordDAO
				.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_BID_SECTION_ID=#{sectionID}")
				.get(new RecordImpl<String, Object>().setColumn("sectionID",
						sectionID));
		if (CollectionUtils.isEmpty(record))
		{
			throw new ValidateException("", "找不到对应的标段信息");
		}
		String bidSectionCode = record.getString("V_BID_SECTION_CODE");
		
//		String bidSectionCode = "E3502000072012347001001";
		String systemInspectZipPath = SystemParamUtils
				.getString(SysParamKey.EBIDKB_FILE_PATH_URL)
				+ "systemInspect/"
				+ bidSectionCode + ".zip";
		File file = new File(systemInspectZipPath);
		if (!file.exists())
		{
			throw new ValidateException("", "文件不存在");
		}
		AeolusDownloadUtils.write(data, file);
	}
	
	// 注意：命令行返回的是命令行所在的当前路径
	public static String getRootClassPath() {
		try {
			String path = SystemInspectZip.class.getClassLoader().getResource("").toURI().getPath();
			return new File(path).getAbsolutePath();
		}
		catch (Exception e) {
			String path = SystemInspectZip.class.getClassLoader().getResource("").getPath();
			return new File(path).getAbsolutePath();
		}
	}
}
