/**
 * 包名：com.sozone.eokb.bus.decrypt
 * 文件名：DecryptStatus.java<br/>
 * 创建时间：2018-5-29 下午3:31:54<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.decrypt;

/**
 * 解密状态枚举<br/>
 * <p>
 * 解密状态枚举<br/>
 * </p>
 * Time：2018-5-29 下午3:31:54<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public enum DecryptStatus
{

	/**
	 * 尚未开始
	 */
	NotYetStarted(0, "尚未开始"),

	/**
	 * 获取JSON信息成功
	 */
	ParseJsonInfoSuccess(10, "JSON描述信息解析成功"),

	/**
	 * 获取JSON信息失败
	 */
	ParseJsonInfoFail(-10, "JSON描述信息解析失败"),

	/**
	 * 密码解密成功
	 */
	DecryptPwdSuccess(20, "密码解密成功"),

	/**
	 * 密码解密失败
	 */
	DecryptPwdFail(-20, "密码解密失败"),

	/**
	 * 摘要解密成功
	 */
	DecryptSummarySuccess(30, "摘要解密成功"),

	/**
	 * 摘要解密失败
	 */
	DecryptSummaryFail(-30, "摘要解密失败"),

	/**
	 * 解析临时文件成功
	 */
	ParseTempFileSuccess(40, "解析临时文件成功"),

	/**
	 * 解析临时文件失败
	 */
	ParseTempFileFail(-40, "解析临时文件失败"),

	/**
	 * 临时文件解密成功
	 */
	DecryptTempFileSuccess(50, "临时文件解密成功"),

	/**
	 * 临时文件解密失败
	 */
	DecryptTempFileFail(-50, "临时文件解密失败"),

	/**
	 * 文件解压成功
	 */
	UnpackFileSuccess(60, "文件解压成功"),

	/**
	 * 文件解压失败
	 */
	UnpackFileFail(-60, "文件解压失败");

	/**
	 * 状态
	 */
	private int status;

	/**
	 * 描述
	 */
	private String desc;

	/**
	 * 构造函数
	 * 
	 * @param status
	 * @param desc
	 */
	private DecryptStatus(int status, String desc)
	{
		this.status = status;
		this.desc = desc;
	}

	/**
	 * status属性的get方法
	 * 
	 * @return the status
	 */
	public int getStatus()
	{
		return status;
	}

	/**
	 * desc属性的get方法
	 * 
	 * @return the desc
	 */
	public String getDesc()
	{
		return desc;
	}

}
