/**
 * 包名：com.sozone.eokb.fjs_ptgl_gcsg_hldjf_v1
 * 文件名：BidBenchmarkPriceAlgorithmUtils.java<br/>
 * 创建时间：2017-9-18 下午2:25:49<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.fjs_ptgl_gcsg_hldjf_v1;

import java.util.LinkedList;
import java.util.List;

import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.ServiceException;
import com.sozone.aeolus.util.CollectionUtils;

/**
 * 评标基准价计算方法工具类<br/>
 * <p>
 * 评标基准价计算方法工具类<br/>
 * </p>
 * Time：2017-9-18 下午2:25:49<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public class BidBenchmarkPriceAlgorithmUtils
{

	/**
	 * 算法枚举<br/>
	 * <p>
	 * 算法枚举<br/>
	 * </p>
	 * Time：2017-9-18 下午2:27:39<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	public enum BBPAlgorithm
	{

		/**
		 * 小于15家算法
		 */
		LessThenFifteen("小于15家算法", -1),

		/**
		 * 方法一
		 */
		MethodOne("方法一", 1),

		/**
		 * 方法二
		 */
		MethodTwo("方法二", 2),

		/**
		 * 方法三
		 */
		MethodThree("方法三", 3);

		/**
		 * 算法名称
		 */
		private String name;

		/**
		 * 类型
		 */
		private int type;

		/**
		 * @param name
		 * @param type
		 */
		private BBPAlgorithm(String name, int type)
		{
			this.name = name;
			this.type = type;
		}

		/**
		 * name属性的get方法
		 * 
		 * @return the name
		 */
		public String getName()
		{
			return name;
		}

		/**
		 * type属性的get方法
		 * 
		 * @return the type
		 */
		public int getType()
		{
			return type;
		}

		/**
		 * 根据状态值返回枚举值<br/>
		 * <p>
		 * </p>
		 * 
		 * @param type
		 *            类型值
		 * @return 枚举对象
		 */
		public static BBPAlgorithm valueOf(int type)
		{
			for (BBPAlgorithm n : values())
			{
				if (n.type == type)
				{
					return n;
				}
			}
			return null;
		}

	}

	/**
	 * 是否可以使用方法三<br/>
	 * <p>
	 * </p>
	 * 
	 * @param prices
	 *            有效投标报价列表(小于等于最高限价)
	 * @param maxPrice
	 *            最高限价
	 * @return 结果
	 */
	public static Record<String, Object> canUseMethodThree(List<Double> prices,
			Double maxPrice)
	{
		Record<String, Object> result = new RecordImpl<String, Object>();
		result.setColumn("ADAPTE", false);
		// 不存在有效投标报价
		if (CollectionUtils.isEmpty(prices))
		{
			result.setColumn("MEMO", "不存在有效投标报价!");
			return result;
		}
		// 有效投标报价家数小于15
		if (prices.size() < 15)
		{
			result.setColumn("ADAPTE", false);
			result.setColumn("MEMO", "有效投标报价共" + prices.size() + "家!");
			return result;
		}
		// 四舍五入值,计算最高限价90%
		Long mp = Math.round(maxPrice * 0.9);
		// 有效报价
		int count = 0;
		for (Double price : prices)
		{
			if (null == price)
			{
				continue;
			}
			// 小于等于最高限价的90%的有效投标报价
			if (price <= mp)
			{
				count++;
			}
		}
		// 无效
		if (0 == count)
		{
			result.setColumn("ADAPTE", false);
			result.setColumn("MEMO", "不存在小于等于最高限价的90%的有效投标报价!");
			return result;
		}
		result.setColumn("ADAPTE", true);
		result.setColumn("MEMO", "小于等于最高限价的90%的有效投标报价共" + count + "家!");
		return result;
	}

	/**
	 * 是否可以使用方法二<br/>
	 * <p>
	 * </p>
	 * 
	 * @param prices
	 *            有效投标报价列表(小于等于最高限价)
	 * @param maxPrice
	 *            最高限价
	 * @return 结果
	 */
	public static Record<String, Object> canUseMethodTwo(List<Double> prices,
			Double maxPrice)
	{
		Record<String, Object> result = new RecordImpl<String, Object>();
		result.setColumn("ADAPTE", false);
		// 不存在有效投标报价
		if (CollectionUtils.isEmpty(prices))
		{
			result.setColumn("MEMO", "不存在有效投标报价!");
			return result;
		}
		// 有效投标报价家数小于15
		if (prices.size() < 15)
		{
			result.setColumn("ADAPTE", false);
			result.setColumn("MEMO", "有效投标报价共" + prices.size() + "家!");
			return result;
		}
		// 四舍五入值,计算最高限价95%
		Long mp1 = Math.round(maxPrice * 0.95);
		// 四舍五入值,计算最高限价90%
		Long mp2 = Math.round(maxPrice * 0.9);
		// 有效报价
		int count = 0;
		for (Double price : prices)
		{
			if (null == price)
			{
				continue;
			}
			// 小于最高限价的95%且大于最高限价的90%的有效投标报价
			if (price < mp1 && price > mp2)
			{
				count++;
			}
		}
		// 无效
		if (0 == count)
		{
			result.setColumn("ADAPTE", false);
			result.setColumn("MEMO", "不存在小于最高限价的95%且大于最高限价的90%的有效投标报价!");
			return result;
		}
		result.setColumn("ADAPTE", true);
		result.setColumn("MEMO", "小于最高限价的95%且大于最高限价的90%的有效投标报价共" + count + "家!");
		return result;
	}

	/**
	 * 是否可以使用方法一<br/>
	 * <p>
	 * </p>
	 * 
	 * @param prices
	 *            有效投标报价列表(小于等于最高限价)
	 * @param maxPrice
	 *            最高限价
	 * @return 结果
	 */
	public static Record<String, Object> canUseMethodOne(List<Double> prices,
			Double maxPrice)
	{
		Record<String, Object> result = new RecordImpl<String, Object>();
		result.setColumn("ADAPTE", false);
		// 不存在有效投标报价
		if (CollectionUtils.isEmpty(prices))
		{
			result.setColumn("MEMO", "不存在有效投标报价!");
			return result;
		}
		// 有效投标报价家数小于15
		if (prices.size() < 15)
		{
			result.setColumn("ADAPTE", false);
			result.setColumn("MEMO", "有效投标报价共" + prices.size() + "家!");
			return result;
		}
		// 四舍五入值,计算最高限价95%
		Long mp = Math.round(maxPrice * 0.95);
		// 有效报价
		int count = 0;
		for (Double price : prices)
		{
			if (null == price)
			{
				continue;
			}
			// 大于等于最高限价的95%的有效投标报价
			if (price >= mp)
			{
				count++;
			}
		}
		// 无效
		if (0 == count)
		{
			result.setColumn("ADAPTE", false);
			result.setColumn("MEMO", "不存在大于等于最高限价的95%的有效投标报价!");
			return result;
		}
		result.setColumn("ADAPTE", true);
		result.setColumn("MEMO", "大于等于最高限价的95%的有效投标报价共" + count + "家!");
		return result;
	}

	/**
	 * 小于15时的系数情况<br/>
	 * <p>
	 * </p>
	 * 
	 * @param prices
	 *            有效投标报价列表(小于等于最高限价)
	 * @param maxPrice
	 *            最高限价
	 * @return 结果
	 */
	public static Record<String, Object> getLTFCoefficient(List<Double> prices,
			Double maxPrice)
	{

		Record<String, Object> result = new RecordImpl<String, Object>();
		// 有效报价算术平均值
		Long avg = Math.round(avg(prices));

		// a) 大于等于最高限价的95%，再将该平均值下浮2～5%后作为评标基准价。
		// 四舍五入值,计算最高限价95%
		Long mp = Math.round(maxPrice * 0.95);
		if (avg >= mp)
		{
			result.setColumn("COEFFICIENT_NO", 1);
			result.setColumn("MEMO", "所有有效投标报价的算数平均值为:" + avg
					+ ",大于等于最高限价的95%!");
			return result;
		}

		// b) 小于最高限价的95%且大于最高限价的90%，将该平均值下浮-1～2%后作为评标基准价。
		// 四舍五入值,计算最高限价90%
		Long mp1 = Math.round(maxPrice * 0.9);
		if (avg < mp && avg > mp1)
		{
			result.setColumn("COEFFICIENT_NO", 2);
			result.setColumn("MEMO", "所有有效投标报价的算数平均值为:" + avg
					+ ",小于最高限价的95%且大于最高限价的90%!");
			return result;
		}

		// c) 小于等于最高限价的90%，将该平均值下浮-4～-1%后作为评标基准价。
		if (avg <= mp1)
		{
			result.setColumn("COEFFICIENT_NO", 3);
			result.setColumn("MEMO", "所有有效投标报价的算数平均值为:" + avg
					+ ",小于等于最高限价的90%!");
			return result;
		}
		return null;
	}

	/**
	 * 计算评标基准价<br/>
	 * <p>
	 * </p>
	 * 
	 * @param prices
	 *            有效投标报价列表(小于等于最高限价)
	 * @param maxPrice
	 *            最高限价
	 * @param coefficient
	 *            下浮系数
	 * @param algorithm
	 *            算法枚举
	 * @return 基准价值
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Long calculate(List<Double> prices, Double maxPrice,
			Double coefficient, int algorithm) throws ServiceException
	{
		BBPAlgorithm mt = BBPAlgorithm.valueOf(algorithm);
		if (null == mt)
		{
			throw new ServiceException("", "无效的评标基准价计算算法类型");
		}
		// 小于15家
		if (BBPAlgorithm.LessThenFifteen == mt)
		{
			return lessThenFifteen(prices, maxPrice, coefficient);
		}
		// 方法一
		if (BBPAlgorithm.MethodOne == mt)
		{
			return methodOne(prices, maxPrice, coefficient);
		}
		// 方法二
		if (BBPAlgorithm.MethodTwo == mt)
		{
			return methodTwo(prices, maxPrice, coefficient);
		}
		// 方法三
		if (BBPAlgorithm.MethodThree == mt)
		{
			return methodThree(prices, maxPrice, coefficient);
		}
		return null;
	}

	/**
	 * 平均值四舍五入<br/>
	 * 
	 * @param list
	 * @return
	 */
	public static Long average(List<Double> list)
	{
		return Math.round(avg(list));
	}

	/**
	 * 求平均值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param list
	 * @return
	 */
	private static double avg(List<Double> list)
	{
		double total = sum(list);
		return total / list.size();
	}

	/**
	 * 求和<br/>
	 * <p>
	 * </p>
	 * 
	 * @param list
	 * @return
	 */
	private static double sum(List<Double> list)
	{
		double total = 0d;
		for (Double d : list)
		{
			total += d;
		}
		return total;
	}

	/**
	 * 计算方法一<br/>
	 * <p>
	 * 方法一：将所有大于等于最高限价的95%的有效投标报价进行算数平均，再将该平均值下浮2～5%后作为评标基准价。
	 * </p>
	 * 
	 * @param prices
	 *            有效投标报价列表(小于等于最高限价)
	 * @param maxPrice
	 *            最高限价
	 * @param coefficient
	 *            下浮系数
	 * @return 基准价值
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Long methodOne(List<Double> prices, Double maxPrice,
			Double coefficient) throws ServiceException
	{
		// 先校验是否可以执行该方法进行计算
		// 判断下浮系数是否在2～5%
		if (null == coefficient)
		{
			throw new ServiceException("", "下浮系数不允许为空,且该值必须满足2～5%之间!");
		}
		if (coefficient < 0.02 || coefficient > 0.05)
		{
			throw new ServiceException("", "无效的下浮系数,该值必须满足2～5%之间!");
		}
		if (CollectionUtils.isEmpty(prices))
		{
			throw new ServiceException("", "投标报价列表不允许为空!");
		}
		if (prices.size() < 15)
		{
			throw new ServiceException("", "有效投标报价数量小于15,不能使用方法一计算评标基准价!");
		}
		// 计算最高限价
		if (null == maxPrice)
		{
			throw new ServiceException("", "最高限价不允许为空!");
		}
		// 四舍五入值,计算最高限价95%
		Long mp = Math.round(maxPrice * 0.95);
		// 有效报价
		List<Double> effectivePrices = new LinkedList<Double>();
		for (Double price : prices)
		{
			if (null == price)
			{
				throw new ServiceException("", "投标报价不允许为空!");
			}
			// 大于等于最高限价的95%的有效投标报价
			if (price >= mp)
			{
				effectivePrices.add(price);
			}
		}
		// 无效
		if (CollectionUtils.isEmpty(effectivePrices))
		{
			throw new ServiceException("",
					"无法执行方法一计算,投标报价没有任意一家满足大于等于最高限价的95%!");
		}

		// 有效报价算术平均值
		Double avg = avg(effectivePrices);
		Double result = avg - avg * coefficient;
		// 四舍五入取整
		return Math.round(result);
	}

	/**
	 * 计算方法二<br/>
	 * <p>
	 * 方法二：将所有小于最高限价的95%且大于最高限价的90%的有效投标报价进行算数平均，将该平均值下浮-1～2%后作为评标基准价。
	 * </p>
	 * 
	 * @param prices
	 *            有效投标报价列表(小于等于最高限价)
	 * @param maxPrice
	 *            最高限价
	 * @param coefficient
	 *            下浮系数
	 * @return 基准价值
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Long methodTwo(List<Double> prices, Double maxPrice,
			Double coefficient) throws ServiceException
	{
		// 先校验是否可以执行该方法进行计算
		// 判断下浮系数是否在-1～2%
		if (null == coefficient)
		{
			throw new ServiceException("", "下浮系数不允许为空,且该值必须满足-1～2%之间!");
		}
		if (coefficient < -0.01 || coefficient > 0.02)
		{
			throw new ServiceException("", "无效的下浮系数,该值必须满足-1～2%之间!");
		}
		if (CollectionUtils.isEmpty(prices))
		{
			throw new ServiceException("", "投标报价列表不允许为空!");
		}
		if (prices.size() < 15)
		{
			throw new ServiceException("", "有效投标报价数量小于15,不能使用方法二计算评标基准价!");
		}
		// 计算最高限价
		if (null == maxPrice)
		{
			throw new ServiceException("", "最高限价不允许为空!");
		}
		// 四舍五入值,计算最高限价95%
		Long mp1 = Math.round(maxPrice * 0.95);
		// 四舍五入值,计算最高限价90%
		Long mp2 = Math.round(maxPrice * 0.9);
		// 有效报价
		List<Double> effectivePrices = new LinkedList<Double>();
		for (Double price : prices)
		{
			if (null == price)
			{
				throw new ServiceException("", "投标报价不允许为空!");
			}
			// 小于最高限价的95%且大于最高限价的90%的有效投标报价
			if (price < mp1 && price > mp2)
			{
				effectivePrices.add(price);
			}
		}
		// 无效
		if (CollectionUtils.isEmpty(effectivePrices))
		{
			throw new ServiceException("",
					"无法执行方法一计算,投标报价没有任意一家满足小于最高限价的95%且大于最高限价的90%!");
		}
		// 有效报价算术平均值
		Double avg = avg(effectivePrices);
		Double result = avg - avg * coefficient;
		// 四舍五入取整
		return Math.round(result);
	}

	/**
	 * 计算方法三<br/>
	 * <p>
	 * 方法三：将所有小于等于最高限价的90%的有效投标报价进行算数平均，将该平均值下浮-4～-1%后作为评标基准价。
	 * </p>
	 * 
	 * @param prices
	 *            有效投标报价列表(小于等于最高限价)
	 * @param maxPrice
	 *            最高限价
	 * @param coefficient
	 *            下浮系数
	 * @return 基准价值
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Long methodThree(List<Double> prices, Double maxPrice,
			Double coefficient) throws ServiceException
	{
		// 先校验是否可以执行该方法进行计算
		// 判断下浮系数是否在-4～-1%
		if (null == coefficient)
		{
			throw new ServiceException("", "下浮系数不允许为空,且该值必须满足-4～-1%之间!");
		}
		if (coefficient > -0.01 || coefficient < -0.04)
		{
			throw new ServiceException("", "无效的下浮系数,该值必须满足-4～-1%之间!");
		}
		if (CollectionUtils.isEmpty(prices))
		{
			throw new ServiceException("", "投标报价列表不允许为空!");
		}
		if (prices.size() < 15)
		{
			throw new ServiceException("", "有效投标报价数量小于15,不能使用方法三计算评标基准价!");
		}
		// 计算最高限价
		if (null == maxPrice)
		{
			throw new ServiceException("", "最高限价不允许为空!");
		}
		// 四舍五入值,计算最高限价90%
		Long mp = Math.round(maxPrice * 0.9);
		// 有效报价
		List<Double> effectivePrices = new LinkedList<Double>();
		for (Double price : prices)
		{
			if (null == price)
			{
				throw new ServiceException("", "投标报价不允许为空!");
			}
			// 小于等于最高限价的90%的有效投标报价
			if (price <= mp)
			{
				effectivePrices.add(price);
			}
		}
		// 无效
		if (CollectionUtils.isEmpty(effectivePrices))
		{
			throw new ServiceException("",
					"无法执行方法一计算,投标报价没有任意一家满足小于等于最高限价的90%!");
		}
		// 有效报价算术平均值
		Double avg = avg(effectivePrices);
		Double result = avg - avg * coefficient;
		// 四舍五入取整
		return Math.round(result);
	}

	/**
	 * 小于15的报价数量方法<br/>
	 * <p>
	 * 若被宣读的有效投标价（去掉超出限价的投标报价）家数小于15家时，计算所有有效投标报价的算数平均值，若该值：<br/>
	 * a)大于等于最高限价的95%，再将该平均值下浮2～5%后作为评标基准价。<br/>
	 * b)小于最高限价的95%且大于最高限价的90%，将该平均值下浮-1～2%后作为评标基准价。<br/>
	 * c)小于等于最高限价的90%，将该平均值下浮-4～-1%后作为评标基准价。<br/>
	 * </p>
	 * 
	 * @param prices
	 *            有效投标报价列表(小于等于最高限价)
	 * @param maxPrice
	 *            最高限价
	 * @param coefficient
	 *            下浮系数
	 * @return 基准价值
	 * @throws ServiceException
	 *             服务异常
	 */
	public static Long lessThenFifteen(List<Double> prices, Double maxPrice,
			Double coefficient) throws ServiceException
	{
		// 先校验是否可以执行该方法进行计算
		// 判断下浮系数是否在2～5%
		if (null == coefficient)
		{
			throw new ServiceException("", "下浮系数不允许为空!");
		}
		// 先校验是否可以执行该方法进行计算
		if (CollectionUtils.isEmpty(prices))
		{
			throw new ServiceException("", "投标报价列表不允许为空!");
		}
		if (prices.size() >= 15)
		{
			throw new ServiceException("", "有效投标报价数量大于等于15,不能使用该方法计算评标基准价!");
		}
		// 计算最高限价
		if (null == maxPrice)
		{
			throw new ServiceException("", "最高限价不允许为空!");
		}
		// 四舍五入,有效报价算术平均值
		Long avg = Math.round(avg(prices));
		// 四舍五入值,计算最高限价95%
		Long mp1 = Math.round(maxPrice * 0.95);
		// 四舍五入值,计算最高限价90%
		Long mp2 = Math.round(maxPrice * 0.9);
		// 大于等于最高限价的95%，再将该平均值下浮2～5%后作为评标基准价。
		if (avg >= mp1)
		{
			return ltfm1(avg, coefficient);
		}
		// 小于最高限价的95%且大于最高限价的90%，将该平均值下浮-1～2%后作为评标基准价。
		if (avg < mp1 && avg > mp2)
		{
			return ltfm2(avg, coefficient);
		}
		// 小于等于最高限价的90%，将该平均值下浮-4～-1%后作为评标基准价。
		if (avg <= mp2)
		{
			return ltfm3(avg, coefficient);
		}
		throw new ServiceException("", "无法找到对应的评标基准价计算方法!");
	}

	/**
	 * 大于等于最高限价的95%，再将该平均值下浮2～5%后作为评标基准价。<br/>
	 * <p>
	 * </p>
	 * 
	 * @param avg
	 * @param coefficient
	 * @return
	 * @throws ServiceException
	 */
	private static Long ltfm1(Long avg, Double coefficient)
			throws ServiceException
	{
		// 判断下浮系数是否在2～5%
		if (coefficient < 0.02 || coefficient > 0.05)
		{
			throw new ServiceException("", "无效的下浮系数,该值必须满足2～5%之间!");
		}
		Double result = avg - avg * coefficient;
		// 四舍五入取整
		return Math.round(result);
	}

	/**
	 * 小于最高限价的95%且大于最高限价的90%，将该平均值下浮-1～2%后作为评标基准价<br/>
	 * <p>
	 * </p>
	 * 
	 * @param avg
	 * @param coefficient
	 * @return
	 * @throws ServiceException
	 */
	private static Long ltfm2(Long avg, Double coefficient)
			throws ServiceException
	{
		// 判断下浮系数是否在-1～2%
		if (coefficient < -0.01 || coefficient > 0.02)
		{
			throw new ServiceException("", "无效的下浮系数,该值必须满足-1～2%之间!");
		}
		Double result = avg - avg * coefficient;
		// 四舍五入取整
		return Math.round(result);
	}

	/**
	 * 小于等于最高限价的90%，将该平均值下浮-4～-1%后作为评标基准价。<br/>
	 * <p>
	 * </p>
	 * 
	 * @param avg
	 * @param coefficient
	 * @return
	 * @throws ServiceException
	 */
	private static Long ltfm3(Long avg, Double coefficient)
			throws ServiceException
	{
		// 判断下浮系数是否在-4～-1%
		if (coefficient > -0.01 || coefficient < -0.04)
		{
			throw new ServiceException("", "无效的下浮系数,该值必须满足-4～-1%之间!");
		}
		Double result = avg - avg * coefficient;
		// 四舍五入取整
		return Math.round(result);
	}

	public static void main(String[] args)
	{
		System.out.println(Math.round(25.9));
		System.out.println(Math.round(25.49));
		System.out.println(Math.round(25.50));
	}
}
