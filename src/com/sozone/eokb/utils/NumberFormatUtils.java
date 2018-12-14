/**
 * 包名：com.sozone.eokb.utils
 * 文件名：NumberFormatUtils.java<br/>
 * 创建时间：2017-9-14 下午12:33:03<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

/**
 * 数值格式化工具<br/>
 * <p>
 * 数值格式化工具<br/>
 * </p>
 * Time：2017-9-14 下午12:33:03<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
public class NumberFormatUtils
{

	/**
	 * 格式化double<br/>
	 * <p>
	 * </p>
	 * 
	 * @param patten
	 *            样式
	 * @param number
	 *            数值
	 * @return 结果
	 */
	public static String format(String patten, Double number)
	{
		if (null == number)
		{
			number = 0D;
		}
		NumberFormat fmt = new DecimalFormat(patten, new DecimalFormatSymbols());
		return fmt.format(number);
	}
}
