/**
 * 包名：com.ws.client
 * 文件名：CreditScoreUtils.java<br/>
 * 创建时间：2017-12-2 下午3:34:42<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.ws.client;

import java.util.Calendar;
import java.util.Date;

import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;

/**
 * 信用分工具类<br/>
 * <p>
 * 信用分工具类<br/>
 * </p>
 * Time：2017-12-2 下午3:34:42<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class CreditScoreUtils
{
	
	/**
	 * 获取信用分同步的年份与季度<br/>
	 * <p>
	 * </p>
	 * @return 年份与季度
	 */
	public static Record<String, Object> getYearAndQuarter()
	{
		// 特别注意一点,只能查上一个季度,每一个季度的第一个月的10号以后才能使用上一季度的信用分.否则只能使用上上个季度的信用分
		// 注：①在每季度首月10日后开标的招标项目，纳入招投标评分的建筑施工企业信用综合评价分值，应为投标人在上季度的企业季度信用得分。而在每季度首月10日前（含10日）开标的，则为投标人在上季度前一个季度的企业季度信用得分。
		// ②福建省建筑施工企业信用综合评价系统（网址：xy.fjjs.gov.cn，下称“评价系统”）每季度公布投标人的企业季度信用得分（房屋建筑、市政工程）。投标人可以通过评价系统查询本单位的企业季度信用得分。
		// 企业季度信用得分以项目截标时在评价系统已发布的数据为准。项目截标后不论何种原因变更的信用评价信息，不在变更前已截标的招投标项目中使用。
		Date now = new Date();
		// 获取当前年份
		int year = getYear(now);
		// 获取当前季度
		int quarter = getQuarter(now);
		// 上一季度
		quarter--;
		// 是否为每一个季度的第一个月的前10天
		boolean isQfmftd = isQuarterFirstMonthFirstTenDay(now);
		if (isQfmftd)
		{
			//如果是每个季度的第一个月前10天，用上上季度
			quarter--;
		}
		// 如果小于等于0,为去上一年的末尾两个季度之一
		// 上年度第四季度
		if (quarter == 0)
		{
			quarter = 4;
			year--;
		}
		// 上年度第三季度
		if (quarter == -1)
		{
			quarter = 3;
			year--;
		}
		// 获取季度名称
		String quarterName = getQuarterName(quarter);
		Record<String, Object> params = new RecordImpl<String, Object>();
		params.setColumn("YEAR", year);
		params.setColumn("QUARTER_NAME", quarterName);
		params.setColumn("QUARTER", quarter);
		return params;
	}

	/**
	 * 是否为每一个季度的第一个月的前10天<br/>
	 * <p>
	 * </p>
	 * 
	 * @param date
	 * @return
	 */
	private static boolean isQuarterFirstMonthFirstTenDay(Date date)
	{
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		int month = c.get(Calendar.MONTH);
		// 是否为每个季度第一个月,一/四/七/十
		boolean isQuarterFirstMonth = (month == Calendar.JANUARY
				|| month == Calendar.APRIL || month == Calendar.JULY || month == Calendar.OCTOBER);
		if (isQuarterFirstMonth)
		{
			return c.get(Calendar.DAY_OF_MONTH) <= 10;
		}
		return false;
	}

	/**
	 * 
	 * 1 第一季度 2 第二季度 3 第三季度 4 第四季度
	 * 
	 * @param date
	 * @return
	 */
	private static int getQuarter(Date date)
	{
		int quarter = 0;
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		int month = c.get(Calendar.MONTH);
		switch (month)
		{
			case Calendar.JANUARY:
			case Calendar.FEBRUARY:
			case Calendar.MARCH:
				return 1;
			case Calendar.APRIL:
			case Calendar.MAY:
			case Calendar.JUNE:
				return 2;
			case Calendar.JULY:
			case Calendar.AUGUST:
			case Calendar.SEPTEMBER:
				return 3;
			case Calendar.OCTOBER:
			case Calendar.NOVEMBER:
			case Calendar.DECEMBER:
				return 4;
			default:
				break;
		}
		return quarter;
	}

	/**
	 * 获取季度名称<br/>
	 * <p>
	 * </p>
	 * 
	 * @param date
	 * @return
	 */
	private static String getQuarterName(int quarter)
	{
		switch (quarter)
		{
			case 1:
				return "第一季度";
			case 2:
				return "第二季度";
			case 3:
				return "第三季度";
			case 4:
				return "第四季度";
			default:
				break;
		}
		return "";
	}

	/**
	 * 取得日期：年
	 * 
	 * @param date
	 * @return
	 */
	private static int getYear(Date date)
	{
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		int year = c.get(Calendar.YEAR);
		return year;
	}

}
