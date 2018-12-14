/**
 * 包名：com.sozone.eokb.common.bus.utils;
 * 文件名：BidderComparator.java<br/>
 * 创建时间：2017-8-1 下午3:25:01<br/>
 * 创建者：jack<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.utils;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

import com.sozone.aeolus.dao.data.Record;

/**
 * 投标人比较器<br/>
 * Time：2017-8-1 下午3:25:01<br/>
 * 
 * @author jack
 * @version 1.0.0
 * @since 1.0.0
 */
public class BidderComparator implements Comparator<Record<String, Object>>
{

	@Override
	public int compare(Record<String, Object> o1, Record<String, Object> o2)
	{
		int creditLevel1 = paserCreditLevel2Int(o1.getString("V_CREDIT_LEVEL"));
		int creditLevel2 = paserCreditLevel2Int(o2.getString("V_CREDIT_LEVEL"));
		if (creditLevel1 != creditLevel2)
		{
			return creditLevel1 - creditLevel2;
		}
		String decodeNo1 = o1.getString("V_BIDDER_NO");
		String decodeNo2 = o2.getString("V_BIDDER_NO");
		return decodeNo1.compareTo(decodeNo2);
	}

	private int paserCreditLevel2Int(String creditLeve)
	{
		if (StringUtils.equals(creditLeve, "AA"))
		{
			return 10;
		}
		if (StringUtils.equals(creditLeve, "A"))
		{
			return 20;
		}
		if (StringUtils.equals(creditLeve, "B"))
		{
			return 30;
		}
		if (StringUtils.equals(creditLeve, "C"))
		{
			return 40;
		}
		if (StringUtils.equals(creditLeve, "D"))
		{
			return 50;
		}
		return 99;
	}
}
