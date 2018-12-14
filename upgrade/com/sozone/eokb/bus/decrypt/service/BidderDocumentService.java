/**
 * 包名：com.sozone.eokb.bus.decrypt.service
 * 文件名：BidderDocumentService.java<br/>
 * 创建时间：2018-4-11 下午4:44:45<br/>
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

import java.util.List;

import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.exception.ServiceException;

/**
 * 投标文件服务接口<br/>
 * <p>
 * 投标文件服务接口<br/>
 * </p>
 * Time：2018-4-11 下午4:44:45<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public interface BidderDocumentService
{

	/**
	 * 
	 * 获取投标人投递信息列表<br/>
	 * <p>
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param unitfy
	 *            统一社会信用代码
	 * @param orgCode
	 *            组织机构代码
	 * @return 投递信息列表
	 * @throws ServiceException
	 *             服务异常
	 */
	List<Record<String, Object>> getDeliveryDocuments(String tpid,
			String unitfy, String orgCode) throws ServiceException;

	/**
	 * 解析投标文件解密数据<br/>
	 * <p>
	 * 根据具体的业务逻辑去解析解密数据
	 * </p>
	 * 
	 * @param tpid
	 *            招标项目ID
	 * @param unitfy
	 *            统一社会信用代码
	 * @param orgCode
	 *            组织机构代码
	 * @throws ServiceException
	 *             服务异常
	 */
	void parseDecryptTempData(String tpid, String unitfy, String orgCode)
			throws ServiceException;
}
