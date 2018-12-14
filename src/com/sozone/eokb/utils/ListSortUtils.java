/**
 * 包名：com.sozone.ebidpb.utils
 * 文件名：ListSortUtils.java<br/>
 * 创建时间：2017年10月8日 上午12:01:25<br/>
 * 创建者：don<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;

/**
 * 在数据库中查出来的列表中，往往需要对不同的字段重新排序。 一般的做法都是使用排序的字段，重新到数据库中查询。
 * 如果不到数据库查询，直接在第一次查出来的list中排序，无疑会提高系统的性能。 下面就写一个通用的方法，对list排序，
 * 
 * 至少需要满足以下5点：
 * 
 * ①.list元素对象类型任意 ---->使用泛型解决
 * 
 * ②.可以按照list元素对象的任意多个属性进行排序,即可以同时指定多个属性进行排序 --->使用java的可变参数解决
 * 
 * ③.list元素对象属性的类型可以是数字(byte、short、int、long、float、double等，包括正数、负数、0)、字符串
 * (char、String)、日期(java.util.Date)
 * --->对于数字：统一转换为固定长度的字符串解决,比如数字3和123，转换为"003"和"123"
 * ;再比如"-15"和"7"转换为"-015"和"007" --->对于日期：可以先把日期转化为long类型的数字，数字的解决方法如上
 * 
 * ④.list元素对象的属性可以没有相应的getter和setter方法 --->可以使用java反射进行获取private和protected修饰的属性值
 * 
 * ⑤.list元素对象的对象的每个属性都可以指定是升序还是降序
 * -->使用2个重写的方法(一个方法满足所有属性都按照升序(降序)，另外一个方法满足每个属性都能指定是升序(降序))
 * 
 * @author don
 * @version 1.0.0
 * @since 1.0.0
 */
public class ListSortUtils
{
	/**
	 * 对list的元素按照多个属性名称排序,
	 * list元素的属性可以是数字（byte、short、int、long、float、double等，支持正数、负数、0）、char、String、
	 * java.util.Date
	 * 
	 * 
	 * @param lsit
	 * @param sortname
	 *            list元素的属性名称
	 * @param isAsc
	 *            true升序，false降序
	 */
	public static <E> void sort(List<Record<String, Object>> list,
			final boolean isAsc, final String... sortnameArr)
	{
		Collections.sort(list, new Comparator<Record<String, Object>>()
		{

			public int compare(Record<String, Object> a,
					Record<String, Object> b)
			{
				int ret = 0;
				try
				{
					for (int i = 0; i < sortnameArr.length; i++)
					{
						ret = compareObject(sortnameArr[i], isAsc, a, b);
						if (0 != ret)
						{
							break;
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				return ret;
			}
		});
	}

	/**
	 * 给list的每个属性都指定是升序还是降序
	 * 
	 * @param list
	 * @param sortnameArr
	 *            参数数组
	 * @param typeArr
	 *            每个属性对应的升降序数组， true升序，false降序
	 */

	public static <E> void sort(List<Record<String, Object>> list,
			final String[] sortnameArr, final boolean[] typeArr)
	{
		if (sortnameArr.length != typeArr.length)
		{
			throw new RuntimeException("属性数组元素个数和升降序数组元素个数不相等");
		}
		Collections.sort(list, new Comparator<Record<String, Object>>()
		{
			public int compare(Record<String, Object> a,
					Record<String, Object> b)
			{
				int ret = 0;
				try
				{
					for (int i = 0; i < sortnameArr.length; i++)
					{
						ret = compareObject(sortnameArr[i], typeArr[i], a, b);
						if (0 != ret)
						{
							break;
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				return ret;
			}
		});
	}

	/**
	 * 对2个对象按照指定属性名称进行排序
	 * 
	 * @param sortname
	 *            属性名称
	 * @param isAsc
	 *            true升序，false降序
	 * @param a
	 * @param b
	 * @return
	 * @throws Exception
	 */
	private static <E> int compareObject(final String sortname,
			boolean isAsc, Record<String, Object> a,
			Record<String, Object> b) throws Exception
	{
		int ret;
		Object value1 = a.get(sortname);
		Object value2 = b.get(sortname);
		String str1 = value1.toString();
		String str2 = value2.toString();
		if (value1 instanceof Number && value2 instanceof Number)
		{
			int maxlen = Math.max(str1.length(), str2.length());
			str1 = addZero2Str((Number) value1, maxlen);
			str2 = addZero2Str((Number) value2, maxlen);
		}
		else if (value1 instanceof Date && value2 instanceof Date)
		{
			long time1 = ((Date) value1).getTime();
			long time2 = ((Date) value2).getTime();
			int maxlen = Long.toString(Math.max(time1, time2)).length();
			str1 = addZero2Str(time1, maxlen);
			str2 = addZero2Str(time2, maxlen);
			//如若两比较数字都为负数
			if(Integer.valueOf(str1) < 0 && Integer.valueOf(str2) < 0)
			{
				isAsc=!isAsc;
			}
			//如若两比较数字都为负数
			if( Double.valueOf(str1) < 0 && Double.valueOf(str2) < 0)
			{
				isAsc=!isAsc;
			}
		}
		if (isAsc)
		{
			ret = str1.compareTo(str2);
		}
		else
		{
			ret = str2.compareTo(str1);
		}
		return ret;
	}

	/**
	 * 给数字对象按照指定长度在左侧补0.
	 * 
	 * 使用案例: addZero2Str(11,4) 返回 "0011", addZero2Str(-18,6)返回 "-000018"
	 * 
	 * @param numObj
	 *            数字对象
	 * @param length
	 *            指定的长度
	 * @return
	 */
	private static String addZero2Str(Number numObj, int length)
	{
		NumberFormat nf = NumberFormat.getInstance();
		// 设置是否使用分组
		nf.setGroupingUsed(false);
		// 设置最大整数位数
		nf.setMaximumIntegerDigits(length);
		// 设置最小整数位数
		nf.setMinimumIntegerDigits(length);
		return nf.format(numObj);
	}

	public static void main(String[] args)
	{

		List<Record<String, Object>> statBListNew = new ArrayList<Record<String, Object>>();

		{
			Record<String, Object> rec = new RecordImpl<String, Object>();
			rec.put("a1", 90);
			rec.put("a2", 10);
			rec.put("a3", 2);
			statBListNew.add(rec);
		}
		{
			Record<String, Object> rec = new RecordImpl<String, Object>();
			rec.put("a1", 90);
			rec.put("a2", 11);
			rec.put("a3", 2);
			statBListNew.add(rec);
		}

		{
			Record<String, Object> rec = new RecordImpl<String, Object>();
			rec.put("a1", 90);
			rec.put("a2", 8);
			rec.put("a3", 112);
			statBListNew.add(rec);
		}
		{
			Record<String, Object> rec = new RecordImpl<String, Object>();
			rec.put("a1", 90);
			rec.put("a2", 8);
			rec.put("a3", 2);
			statBListNew.add(rec);
		}
		{
			Record<String, Object> rec = new RecordImpl<String, Object>();
			rec.put("a1", 190);
			rec.put("a2", 8);
			rec.put("a3", 2);
			statBListNew.add(rec);
		}
		String[] sortNameArr = { "a1", "a2", "a3" };
		boolean[] isAscArr = { false, false, true };
		ListSortUtils.sort(statBListNew, sortNameArr, isAscArr);

		for (Record<String, Object> b : statBListNew)
		{
			System.out.println(b);
		}
	}
}
