/**
 * 包名：com.sozone.eokb.utils
 * 文件名：NumberToCharUtils.java<br/>
 * 创建时间：2018-1-26 下午5:00:35<br/>
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

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;

/**
 * 将金额转换成中文<br/>
 * <p>
 * 将金额转换成中文<br/>
 * </p>
 * Time：2018-1-26 下午5:00:35<br/>
 * 
 * @author wengdm
 * @version 1.0.0
 * @since 1.0.0
 */
public class NumberToCharUtils
{
	/**
	 * 汉语数字
	 */
	private static final String[] CN_UPPER_NUMBER = { "零", "一", "二", "三", "四",
			"五", "六", "七", "八", "九" };
	/**
	 * 汉语中货币单位大写，这样的设计类似于占位符
	 */
	private static final String[] CN_UPPER_MONETRAY_UNIT = { "分", "角", "元",
			"十", "百", "千", "万", "十", "百", "千", "亿", "十", "百", "千", "兆", "十",
			"百", "千" };
	/**
	 * 特殊字符：整
	 */
	private static final String CN_FULL = "整";
	/**
	 * 特殊字符：负
	 */
	private static final String CN_NEGATIVE = "负";
	/**
	 * 金额的精度，默认值为2
	 */
	private static final int MONEY_PRECISION = 2;
	/**
	 * 特殊字符：零元整
	 */
	private static final String CN_ZEOR_FULL = "零元" + CN_FULL;

	/**
	 * 把输入的金额转换为汉语中人民币的大写
	 * 
	 * @param moneyString
	 *            输入的金额
	 * @return 对应的汉语大写
	 */
	public static String number2CNMontrayUnit(String moneyString)
	{
		// 判断字符串是否为数字
		if (!isNumeric(moneyString))
		{
			return moneyString;
		}
		BigDecimal numberOfMoney = new BigDecimal(moneyString);
		StringBuffer sb = new StringBuffer();
		// -1, 0, or 1 as the value of this BigDecimal is negative, zero, or
		// positive.
		int signum = numberOfMoney.signum();
		// 零元整的情况
		if (signum == 0)
		{
			return CN_ZEOR_FULL;
		}
		// 这里会进行金额的四舍五入
		long number = numberOfMoney.movePointRight(MONEY_PRECISION)
				.setScale(0, 4).abs().longValue();
		// 得到小数点后两位值
		long scale = number % 100;
		int numUnit = 0;
		int numIndex = 0;
		boolean getZero = false;
		// 判断最后两位数，一共有四中情况：00 = 0, 01 = 1, 10, 11
		if (!(scale > 0))
		{
			numIndex = 2;
			number = number / 100;
			getZero = true;
		}
		if ((scale > 0) && (!(scale % 10 > 0)))
		{
			numIndex = 1;
			number = number / 10;
			getZero = true;
		}
		int zeroSize = 0;
		while (true)
		{
			if (number <= 0)
			{
				break;
			}
			// 每次获取到最后一个数
			numUnit = (int) (number % 10);
			if (numUnit > 0)
			{
				if ((numIndex == 9) && (zeroSize >= 3))
				{
					sb.insert(0, CN_UPPER_MONETRAY_UNIT[6]);
				}
				if ((numIndex == 13) && (zeroSize >= 3))
				{
					sb.insert(0, CN_UPPER_MONETRAY_UNIT[10]);
				}
				sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
				sb.insert(0, CN_UPPER_NUMBER[numUnit]);
				getZero = false;
				zeroSize = 0;
			}
			else
			{
				++zeroSize;
				if (!getZero)
				{
					sb.insert(0, CN_UPPER_NUMBER[numUnit]);
				}
				if (numIndex == 2)
				{
					if (number > 0)
					{
						sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
					}
				}
				else if (((numIndex - 2) % 4 == 0) && (number % 1000 > 0))
				{
					sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
				}
				getZero = true;
			}
			// 让number每次都去掉最后一个数
			number = number / 10;
			++numIndex;
		}
		// 如果signum == -1，则说明输入的数字为负数，就在最前面追加特殊字符：负
		if (signum == -1)
		{
			sb.insert(0, CN_NEGATIVE);
		}
		// 输入的数字小数点后两位为"00"的情况，则要在最后追加特殊字符：整
		if (!(scale > 0))
		{
			sb.append(CN_FULL);
		}
		return sb.toString();
	}

	public static void main(String[] args)
	{
		String money = "123似懂非懂是";
		String s = NumberToCharUtils.number2CNMontrayUnit(money);
		System.out.println(s);
	}

	/**
	 * ASCII码
	 * 
	 * @param str
	 *            需要校验的字符
	 * @return 是否为数字
	 */
	public static boolean isNumeric(String str)
	{
		if (StringUtils.isEmpty(str))
		{
			return false;
		}
		// 统计小数点
		int pointCount = 0;
		for (int i = str.length(); --i >= 0;)
		{
			int chr = str.charAt(i);
			if (chr < 48 || chr > 57)
			{
				if (chr != 46)
				{
					return false;
				}
				pointCount++;
			}
			if (pointCount > 1)
			{
				return false;
			}
		}
		return true;
	}
}
