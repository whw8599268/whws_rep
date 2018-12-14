/**
 * 包名：com.sozone.eokb.bus.service
 * 文件名：BidderDocumentParseService.java<br/>
 * 创建时间：2017-9-4 下午1:37:52<br/>
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
 * 投标文件解析服务接口定义<br/>
 * <p>
 * 投标文件解析服务接口定义<br/>
 * </p>
 * Time：2017-9-4 下午1:37:52<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
public interface BidderDocumentParseService
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
	List<Record<String, Object>> getBidderDeliveryInfo(String tpid,
			String unitfy, String orgCode) throws ServiceException;

	/**
	 * 解析投标文件<br/>
	 * <p>
	 * 根据不同的业务去解析投标文件
	 * </p>
	 * 
	 * @throws ServiceException
	 *             服务异常
	 */
	void parseBidderDocument() throws ServiceException;

	/**
	 * 解析投标文件<br/>
	 * <p>
	 * 根据不同的业务去解析投标文件
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
	void parseBidderDocument(String tpid, String unitfy, String orgCode)
			throws ServiceException;

}
