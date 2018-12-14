/**
 * 包名：com.sozone.eokb.fjs_gsgl_jdsg_hldjf_v1
 * 文件名：Utils.java<br/>
 * 创建时间：2017-8-29 下午4:46:23<br/>
 * 创建者：Administrator<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.bus.benchmark;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sozone.aeolus.authorize.utlis.ApacheShiroUtils;
import com.sozone.aeolus.authorize.utlis.LogUtils;
import com.sozone.aeolus.dao.ActiveRecordDAO;
import com.sozone.aeolus.dao.ActiveRecordDAOImpl;
import com.sozone.aeolus.dao.data.Record;
import com.sozone.aeolus.dao.data.RecordImpl;
import com.sozone.aeolus.exception.DAOException;
import com.sozone.aeolus.exception.FacadeException;
import com.sozone.aeolus.exception.ValidateException;
import com.sozone.aeolus.util.CollectionUtils;
import com.sozone.eokb.common.ConstantEOKB.TableName;
import com.sozone.eokb.utils.Arith;

/**
 * </p> Time：2017-8-29 下午4:46:23<br/>
 * 
 * @author Administrator
 * @version 1.0.0
 * @since 1.0.0
 */
public class BenchmarkUtils
{
	private static final NumberFormat FMT_D = new DecimalFormat("###,##0",
			new DecimalFormatSymbols());

	/**
	 * 日志
	 */
	private static Logger logger = LoggerFactory
			.getLogger(BenchmarkUtils.class);

	/**
	 * activeRecordDAO属性的get方法
	 * 
	 * @return the activeRecordDAO
	 */
	protected static ActiveRecordDAO getActiveRecordDAO()
	{
		return ActiveRecordDAOImpl.getInstance();
	}

	/**
	 * 
	 * 标段使用的评标基准价方法信息<br/>
	 * <p>
	 * 标段使用的评标基准价方法信息
	 * </p>
	 * 
	 * @param bidders
	 *            投标人列表
	 * @param section
	 *            标段信息
	 * @return 基准价方法信息
	 * @throws FacadeException
	 *             FacadeException
	 */
	public static Record<String, Object> getMethodInfo(
			List<Record<String, Object>> bidders, Record<String, Object> section)
			throws FacadeException
	{
		int n = 0;// Hi企业个数
		/*** 获取最高价格 *****/
		Double xi = section.getDouble("N_CONTROL_PRICE");
		ApacheShiroUtils.getSession().setAttribute("BID_XI", xi);
		if (null == xi)
		{
			xi = 0D;
		}

		Double pi = 0D;
		List<Record<String, Object>> Hi = bidders;
		ApacheShiroUtils.getSession().setAttribute("BID_HI", Hi);
		/*** 第三部生成评标基准价 *******/
		for (Record<String, Object> r : Hi)
		{
			if (null == r.getDouble("N_PRICE"))
			{
				r.setColumn("N_PRICE", "0");
			}
			pi += r.getDouble("N_PRICE");
		}
		int k3 = 0;
		n = Hi.size();
		StringBuilder msg = new StringBuilder();
		msg.append("进入基准价投标企业#{style}" + n + "</a>家,");
		// 大于5家企业
		if (n > 5)
		{
			k3 = (int) (pi % 5) + 1;
			msg.append("因有效企业大于#{style}5</a>家");
			msg.append("</br>依据公式#{style}k=mod(pi，5)+ 1</a>，得出#{style}k = mod("
					+ FMT_D.format(pi) + " ， 5) + 1");
			msg.append("</a>得出方法#{style}k=" + k3 + "</a>");
		}
		else if (n < 6)
		{
			k3 = (int) (pi % 3) + 1;
			if (k3 == 3)
			{
				k3 = 5;
			}
			msg.append("因有效投标报价家数少于等于#{style}5</a>家，则所有投标企业都参加基准价的计算");
			msg.append("抽取有效投标报价企业家数#{style}" + n + "</a>家。");
			msg.append("</br>依据公式#{style}k=mod(pi，3)+ 1</a>，得出#{style}k = mod("
					+ FMT_D.format(pi) + " ， 3) + 1</a>");
			msg.append("得出方法#{style}k=" + k3 + "</a>");
		}

		Record<String, Object> methodInfo = new RecordImpl<String, Object>();
		methodInfo.setColumn("msg", replaceStyle(msg.toString()));
		methodInfo.setColumn("method", k3);
		ApacheShiroUtils.getSession().setAttribute("BID_PI", pi);
		return methodInfo;
	}

	/**
	 * 
	 * @param msg
	 * @return smsg
	 */
	public static String replaceStyle(String msg)
	{
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("style", "<a style='font-size:16px;color: #ff7300;'>");
		// 表达式解析器,创建一个#{}的表达式解析器
		StrSubstitutor strs = new StrSubstitutor(params, "#{", "}");
		String smsg = strs.replace(msg);
		return smsg;
	}

	/**
	 * 
	 * 获取下浮系数信息<br/>
	 * <p>
	 * 获取下浮系数信息
	 * </p>
	 * 
	 * @param method
	 * @return
	 */
	public static Record<String, Object> getDownInfo(int method)
	{
		List<Record<String, Object>> Hi = (List<Record<String, Object>>) ApacheShiroUtils
				.getSession().getAttribute("BID_HI");
		Double pi = (Double) ApacheShiroUtils.getSession().getAttribute(
				"BID_PI");
		Double xi = (Double) ApacheShiroUtils.getSession().getAttribute(
				"BID_XI");
		Record<String, Object> downInfo = kqi(method, pi, Hi.size(), xi, Hi);
		Double qi = downInfo.getDouble("qi");
		ApacheShiroUtils.getSession().setAttribute("BID_QI", qi);
		double f1 = 0;
		String msg = null;
		if (qi >= xi * 0.95)
		{
			f1 = Arith.add(Math.round(qi) % 7 * 0.005, 0.01);
			f1 = BenchmarkUtils.interval(f1, 0.01, 0.04, true, true);
			msg = "。</br>若#{style}"
					+ FMT_D.format(qi)
					+ "≥"
					+ FMT_D.format(xi)
					+ "＊95％</a>, 则以0.5%为一档，在1%至4%之间确定下浮系数f1。</br>下浮系数计算公式#{style}f1＝mod(qi，7)*0.5%+1%</a>，即：#{style}"
					+ f1 + "＝mod(" + FMT_D.format(qi) + "，7)*0.5%+1%</a>";
		}
		else if (xi * 0.95 >= qi && qi > xi * 0.9)
		{
			f1 = Arith.sub(Math.round(qi) % 7 * 0.005, 0.01);
			f1 = BenchmarkUtils.interval(f1, -0.01, 0.02, true, true);
			msg = "。</br>若#{style}"
					+ FMT_D.format(xi)
					+ "＊90％<"
					+ FMT_D.format(qi)
					+ "<="
					+ FMT_D.format(xi)
					+ "＊95％</a>, 则以0.5%为一档，在-1%至2%之间确定下浮系数f1。</br>下浮系数计算公式#{style}f1＝mod(qi，7)*0.5%-1%</a>,即：#{style}"
					+ f1 + "＝mod(" + FMT_D.format(qi) + "，7)*0.5%-1%</a></br>";
		}
		else if (xi * 0.9 >= qi)
		{
			// 修改日期（2018.02.20）根据2017.12.28的勘误，由下浮系数f1＝mod(qi，7)*0.5%-3%修改为f1＝mod(qi，7)*0.5%-4%
			f1 = Arith.sub(Math.round(qi) % 7 * 0.005, 0.04);
			// f1 = BenchmarkUtils.interval(f1, -0.03, 0, true, true);
			// 修改通知（补遗6,2017.12.20）
			// 将③中的下浮区间修改为与①中的对称，即修改为-4%～-1%。路基、路面、土建监理、试验检测招标文件同步修改
			f1 = BenchmarkUtils.interval(f1, -0.04, -0.01, true, true);
			msg = "。</br>若#{style}"
					+ FMT_D.format(xi)
					+ "＊90％≥"
					+ FMT_D.format(qi)
					+ ", 则以0.5%为一档，在-4%至-1%之间确定下浮系数f1。</br>下浮系数计算公式f1＝mod(qi，7)*0.5%-4%</a>,即：#{style}"
					+ f1 + "＝mod(" + FMT_D.format(qi) + "，7)*0.5%-4%</a></br>";
		}

		msg = downInfo.getString("avgMsg") + msg;
		Record<String, Object> avgInfo = new RecordImpl<String, Object>();
		avgInfo.setColumn("msg", replaceStyle(msg));
		avgInfo.setColumn("down", f1);
		return avgInfo;
	}

	// public double getDown()
	// {
	// double qi = kqi(k3, pi, Hi.size(), xi, Hi)
	// // 计算评标基准价
	// double f1 = 0;
	// if (qi >= xi * 0.95)
	// {
	// f1 = Arith.add(Math.round(qi) % 7 * 0.005, 0.01);
	// f1 = benchmarkUtils.interval(f1, 0.01, 0.04, true, true);
	// }
	// else if (xi * 0.95 >= qi && qi > xi * 0.9)
	// {
	// f1 = Arith.sub(Math.round(qi) % 7 * 0.005, 0.01);
	// f1 = benchmarkUtils.interval(f1, -0.01, 0.02, true, true);
	// }
	// else if (xi * 0.9 >= qi)
	// {
	// f1 = Arith.sub(Math.round(qi) % 7 * 0.005, 0.03);
	// f1 = benchmarkUtils.interval(f1, -0.03, 0, true, true);
	// }
	//
	// return qi;
	// }

	/**
	 * 
	 * 递归生成HI<br/>
	 * <p>
	 * </p>
	 * 
	 * @param sumPrice
	 *            投标企业总价
	 * @param listSectionB
	 *            B级企业集合
	 * @param listSectionSecond
	 *            HI集合
	 * @param listSection
	 *            总集合
	 * @param b_size
	 *            B级企业数量
	 * @param k
	 *            余数
	 * @return
	 */
	public static List<Record<String, Object>> putHI(double z,
			List<Record<String, Object>> L, List<Record<String, Object>> Hi,
			int m, int b_size, int k)
	{
		b_size--;
		if (Hi.size() == (m * 2 / 3))
		{
			return Hi;
		}
		else
		{
			k = (int) (z % b_size);
			Hi.add(L.get(k));
			z = z - L.get(k).getDouble("N_PRICE");
		}
		return putHI(z, L, Hi, m, b_size, k);
	}

	/**
	 * 
	 * 计算加权平均值<br/>
	 * <p>
	 * </p>
	 * 
	 * @return 加权平均值
	 */
	public static Record<String, Object> kqi(int k, double pi, int n,
			double xi, List<Record<String, Object>> hil)
	{

		LinkedList<Double> hi = new LinkedList<Double>();
		Double d = null;
		for (Record<String, Object> r : hil)
		{
			d = r.getDouble("N_PRICE");
			hi.add(d);
		}
		Collections.sort(hi);
		switch (k)
		{
			case 1:
				return kqi1(pi, n);
			case 2:
				return kqi2(pi, n, xi);
			case 3:
				return kqi3(pi, n, hi, hil);
			case 4:
				return kqi4(n, hi);
			case 5:
				return kqi5(xi, hi);
			default:
				return kqi1(pi, n);
		}
	}

	/**
	 * 取开闭值<br/>
	 * <p>
	 * </p>
	 * 
	 * @param src
	 *            源值
	 * @param min
	 *            左值
	 * @param max
	 *            右值
	 * @param emin
	 *            左侧是否为闭区间
	 * @param emax
	 *            右侧是否为闭区间
	 * @return 取值
	 */
	private static double interval(double src, double min, double max,
			boolean emin, boolean emax)
	{
		// 如果小于最小值
		if (src < min)
		{
			return min;
		}
		// 如果等于最小值且左边是闭区间
		if (emin && src == min)
		{
			return min;
		}
		// 右开闭处理
		if (src > max)
		{
			return max;
		}
		if (emax && src == max)
		{
			return max;
		}
		//
		return src;
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

	private static Record<String, Object> kqi1(double pi, int n)
	{
		String avgMsg = "根据#{style}方法1</a>公式平均值#{style}qi＝pi/n</a>，得出平均值#{style}"
				+ FMT_D.format(pi / n)
				+ "="
				+ FMT_D.format(pi)
				+ "/"
				+ n
				+ "</a>";
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("qi", pi / n);
		record.setColumn("avgMsg", avgMsg);

		return record;
	}

	private static Record<String, Object> kqi2(double pi, int n, double xi)
	{
		double f = pi % 6 * 0.04 + 0.2;
		f = interval(f, 0.2, 0.4, true, true);
		String avgMsg = "根据#{style}方法2</a>公式最高限价的权重#{style}f= mod(pi，6)*4%+20%</a>，得出权重系数#{style}f= mod("
				+ FMT_D.format(pi)
				+ "，6)*4%+20%</a></br>权重#{style}f="
				+ f
				+ "</a>,再依据公式#{style}qi=pi/n*(100%-f)+ xi*f</a>，得出加权平均值#{style}qi= "
				+ FMT_D.format(pi)
				+ "/"
				+ n
				+ "*(100%-"
				+ (int) (f * 100)
				+ "%)+ "
				+ FMT_D.format(xi)
				+ "*"
				+ f
				+ "</a>平均值#{style}qi="
				+ FMT_D.format(pi / n * (1 - f) + xi * f) + "</a>";
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("qi", pi / n * (1 - f) + xi * f);
		record.setColumn("avgMsg", avgMsg);

		return record;
	}

	private static Record<String, Object> kqi3(double pi, int n,
			LinkedList<Double> hi, List<Record<String, Object>> hil)
	{
		// 当n≤8时，从Ｈi去掉一个最高和一家最低报价后，取其余投标价计算平均值qi；
		if (8 >= n)
		{
			hi.removeLast();
			hi.removeFirst();
			String avgMsg = "由于#{style}有效家数≤8家</a>，从有效投标总价去掉一个最高和一家最低报价后，取其余投标价计算平均值qi，#{style}qi="
					+ FMT_D.format(avg(hi)) + "</a>";
			Record<String, Object> record = new RecordImpl<String, Object>();
			record.setColumn("qi", avg(hi));
			record.setColumn("avgMsg", avgMsg);

			return record;
		}
		if (9 <= n && n <= 16)
		{
			int count = (int) Math.round(hi.size() * 0.7);
			List<Double> temp = Utils.randomList(pi, n, hil, count);
			String avgMsg = "当#{style}9≤有效家数≤16</a>时，从有效投标总价队列Ｈi中随机抽出70%的投标人（四舍五入）的投标价，再计算平均值qi，#{style}qi="
					+ FMT_D.format(avg(temp)) + "</a>";
			Record<String, Object> record = new RecordImpl<String, Object>();
			record.setColumn("qi", avg(temp));
			record.setColumn("avgMsg", avgMsg);

			return record;
		}
		// 当n>16时, 从队列Ｈi中随机抽出50％的投标人（四舍五入）的投标价计算平均值。方法如上，只需将70%改为５０％即可。
		int count = (int) Math.round(hi.size() * 0.5);
		List<Double> temp = Utils.randomList(pi, n, hil, count);
		String avgMsg = "#{style}有效家数>16</a>。时, 从有效投标总价队列Ｈi中随机抽出50％的投标人（四舍五入）的投标价计算平均值。，再计算平均值qi，#{style}qi="
				+ FMT_D.format(avg(temp)) + "</a>";
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("qi", avg(temp));
		record.setColumn("avgMsg", avgMsg);

		return record;
	}

	private static Record<String, Object> kqi4(int n, LinkedList<Double> hi)
	{
		// 当n≤10时，从队列Ｈi中去掉一个最高和最低报价后，取其余投标价计算平均值qi
		if (n <= 10)
		{
			hi.removeLast();
			hi.removeFirst();
			String avgMsg = "当有#{style}效家数≤10</a>时，从有效投标总价队列Ｈi中去掉一个最高和最低报价后，取其余投标价计算平均值qi，#{style}qi="
					+ FMT_D.format(avg(hi)) + "</a>";
			Record<String, Object> record = new RecordImpl<String, Object>();
			record.setColumn("qi", avg(hi));
			record.setColumn("avgMsg", avgMsg);

			return record;
		}
		// 当n>10时，从队列Ｈi中通过先去掉一个最低报价后，再去掉一个最高报价的顺序进行循环，取中间8个投标价计算平均值pi。
		while (true)
		{
			hi.removeFirst();
			if (hi.size() == 8)
			{
				break;
			}
			hi.removeLast();
			if (hi.size() == 8)
			{
				break;
			}
		}
		String avgMsg = "当#{style}有效家数10</a>时，从有效投标总价队列Ｈi中通过先去掉一个最低报价后，再去掉一个最高报价的顺序进行循环，取中间8个投标价计算平均值qi，#{style}qi="
				+ FMT_D.format(avg(hi)) + "</a>";
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("qi", avg(hi));
		record.setColumn("avgMsg", avgMsg);

		return record;
	}

	private static Record<String, Object> kqi5(double xi, LinkedList<Double> hi)
	{
		// 计算队列Ｈi中投标价的平均值pi，再将低于最高限价xi且高于该平均值qi的投标价再一次进行平均。[1000.0,1006.0]
		double pi = avg(hi);
		double avg = 0D;
		if (1 == hi.size())
		{
			avg = pi;
		}
		List<Double> temp = new LinkedList<Double>();
		for (Double h : hi)
		{
			// 这个地方需要讨论
			if (xi > h && h >= pi)
			{
				temp.add(h);
			}
		}

		if (temp.size() == 0)
		{
			avg = 0;
		}
		else
		{
			avg = avg(temp);
		}
		String avgMsg = "计算队列Ｈi中投标价的平均值#{style}" + FMT_D.format(pi)
				+ "</a>，再将低于最高限价#{style}" + FMT_D.format(xi)
				+ "</a>且高于该平均值#{style}" + FMT_D.format(pi)
				+ "</a>的投标价再一次进行平均,得到新的平均值qi，#{style}qi=" + FMT_D.format(avg)
				+ "</a>";
		Record<String, Object> record = new RecordImpl<String, Object>();
		record.setColumn("qi", avg);
		record.setColumn("avgMsg", avgMsg);

		return record;
	}

	/**
	 * 
	 * 获取评标基准价计算的详细信息<br/>
	 * <p>
	 * 获取评标基准价计算的详细信息
	 * </p>
	 * 
	 * @param tpid
	 * @param sid
	 * @return
	 * @throws ValidateException
	 * @throws DAOException
	 */
	public static Record<String, Object> getBenchamarkInfoByParam(String tpid,
			String sid) throws ValidateException, DAOException
	{
		ActiveRecordDAO dao = getActiveRecordDAO();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.put("tpid", tpid);
		param.put("sid", sid);
		// 先查询对应标段
		Record<String, Object> section = dao.auto()
				.table(TableName.EKB_T_SECTION_INFO)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_SECTION_ID = #{sid}").get(param);

		setBenchmarkInfo(section, tpid, sid);
		return section;
	}

	/**
	 * 
	 * 将评标基准价信息添加到标段<br/>
	 * <p>
	 * 将评标基准价信息添加到标段
	 * </p>
	 * 
	 * @param section
	 * @throws DAOException
	 */
	public static void setBenchmarkInfo(Record<String, Object> section,
			String tpid, String sid) throws DAOException
	{
		logger.debug(LogUtils.format("将评标基准价信息添加到标段", section, tpid, sid));
		ActiveRecordDAO dao = getActiveRecordDAO();
		Record<String, Object> param = new RecordImpl<String, Object>();
		param.setColumn("tpid", tpid);
		param.setColumn("sid", sid);

		// 备注列的下浮系数信息
		StringBuilder dowMsg = new StringBuilder();

		Double avg = null;
		List<Record<String, Object>> evaluation = dao.statement().selectList(
				"fjs_gsgl_common.getEvaluation", param);
		if (CollectionUtils.isEmpty(evaluation))
		{
			return;
		}
		String vjson = evaluation.get(0).getString("V_JSON_OBJ");

		// 查询AA、A级的投标人信息
		int validBidder = dao
				.auto()
				.table(TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
				.setCondition("AND", "N_VALID_OFFER = 1")
				.setCondition("AND",
						"(V_BIDDER_NO LIKE '1-%' OR V_BIDDER_NO LIKE '2-%')")
				.count(param);
		// 有效报价的投标人信息
		int validPriceNum = dao.auto()
				.table(TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
				.setCondition("AND", "N_VALID_OFFER = 1").count(param);
		// 进入评标基准价数量
		int validNum = dao.auto()
				.table(TableName.EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION)
				.setCondition("AND", "V_TPID = #{tpid}")
				.setCondition("AND", "V_BID_SECTION_ID = #{sid}")
				.setCondition("AND", "N_VALID_OFFER = 1")
				.setCondition("AND", "N_VALID_GRADE = 1").count(param);

		// 有效报价总和
		Double allPrice = evaluation.get(0).getDouble("N_VALID_SUM_PRICE");
		// 最高限价
		Double maxPrice = section.getDouble("N_CONTROL_PRICE");
		// 有效家数
		int n = validBidder;
		logger.debug(LogUtils.format(
				"构建评标基准价说明（AA、A级，有效报价，进入评标基准价数量，有效报价总和，最高限价）", validBidder,
				validPriceNum, validNum, allPrice, maxPrice));
		// 构建评标基准价说明
		if (validPriceNum > 5)
		{
			dowMsg.append("1.进入基准价投标企业#{style}" + evaluation.size()
					+ "</a>家，有效投标家数：#{style}" + validPriceNum
					+ "</a>家，AA、A级：#{style}" + validBidder
					+ "</a>家，其他等级：#{style}" + (validPriceNum - validBidder)
					+ "</a>家，");

			JSONObject jobj = JSON.parseObject(vjson);
			JSONArray jarr = jobj.getJSONArray("extract");
			// 若有值，则AA,A级企业少于有效家数的2/3，需要从Gi中抽取
			if (jarr.size() > 0)
			{
				dowMsg.append("由于AA、A级#{style}" + validBidder
						+ "</a>家少于有效报价家数的#{style}2/3</a>，所以从 Gi抽取有效家数#{style}"
						+ jarr.size() + "</a>家。抽取过程如下：</br>");
				String temPrice = null;
				String temAllPrice = null;
				for (int i = 0; i < jarr.size(); i++)
				{
					JSONObject json = new JSONObject();
					json = jarr.getJSONObject(i);
					if (StringUtils.isNotEmpty(temPrice))
					{
						dowMsg.append("第"
								+ (i + 1)
								+ "次：#{style}【"
								+ (Long.parseLong(temAllPrice.replaceAll(",",
										"")) - Long.parseLong(temPrice
										.replaceAll(",", ""))) + "="
								+ temAllPrice + "-" + temPrice
								+ "</a>所以#{style}" + json.getString("number")
								+ "=" + json.getString("publicity") + "</a>】"
								+ "抽取的投标人编号是：#{style}"
								+ json.getString("bidderNo")
								+ "</a>，投标报价为：#{style}"
								+ json.getString("price") + "</a></br>");
					}
					else
					{
						dowMsg.append("第" + (i + 1)
								+ "次：【进入评标基准价有效报价总和：#{style}"
								+ json.getString("allPrice") + "</a>所以#{style}"
								+ json.getString("number") + "="
								+ json.getString("publicity") + "</a>】"
								+ "抽取的投标人编号是：#{style}"
								+ json.getString("bidderNo")
								+ "</a>，投标报价为：#{style}"
								+ json.getString("price") + "</a></br>");
					}
					temPrice = json.getString("price");
					temAllPrice = json.getString("allPrice");

				}
				n = validBidder + jarr.size();
				dowMsg.append("，此时有效家数#{style}" + n + "大于等于有效报价家数"
						+ validPriceNum + "的2/3</a>，结束抽取");
			}
			else
			{
				dowMsg.append("AA、A级企业大于等于有效投标家数的2/3");
			}
			if (validNum > 5)
			{
				dowMsg.append("</br>2.进入评标基准价计算的企业家数#{style}" + validNum
						+ "</a>家，");
				dowMsg.append("因有效企业大于#{style}5家</a>");
				dowMsg.append("依据公式#{style}k=mod(pi，5)+ 1</a>，计算公式为#{style}k = mod("
						+ FMT_D.format(allPrice) + " ， 5) + 1</a>");
				dowMsg.append("得出方法#{style}k="
						+ evaluation.get(0).getString("N_METHOD") + "</a></br>");
			}
			else
			{
				dowMsg.append("</br>2.进入评标基准价计算的企业家数#{style}" + validNum
						+ "</a>家，");
				dowMsg.append("因有效企业小于等于#{style}5家</a>");
				dowMsg.append("依据公式#{style}k=mod(pi，3)+ 1</a>，计算公式为#{style}k = mod("
						+ FMT_D.format(allPrice) + " ， 3) + 1</a>");
				dowMsg.append("得出方法#{style}k="
						+ evaluation.get(0).getString("N_METHOD") + "</a></br>");
			}
		}
		else
		{
			n = validPriceNum;
			dowMsg.append("1.因有效投标报价家数少于等于#{style}5</a>家，则所有投标企业都参加基准价的计算</br>");
			dowMsg.append("2.抽取有效投标报价企业家数#{style}" + validNum + "</a>家，");
			dowMsg.append("依据公式#{style}k=mod(pi，3)+ 1</a>，计算公式为#{style}k = mod("
					+ FMT_D.format(allPrice) + " ， 3) + 1</a>");
			dowMsg.append("得出方法#{style}k="
					+ evaluation.get(0).getString("N_METHOD") + "</a></br>");
		}

		String avgMsg = getAvgMsg(n, evaluation.get(0), maxPrice, evaluation);
		dowMsg.append(avgMsg);

		// 方法5的平均值取高于最低限价且高于最高限价的投标价平均值
		if (evaluation.get(0).getInteger("N_METHOD") == 5)
		{
			avg = evaluation.get(0).getDouble("N_DU_PRICE_AVG");
		}
		else if (evaluation.get(0).getInteger("N_METHOD") == 2)
		{
			avg = evaluation.get(0).getDouble("N_WEIGHTING_AVG");
		}
		else
		{
			avg = evaluation.get(0).getDouble("N_AVG");
		}

		String down = null;
		Double nDown = evaluation.get(0).getDouble("N_DOWN");
		// 下浮系数
		if (avg >= maxPrice * 0.95)
		{
			down = "</br>4.若#{style}"
					+ FMT_D.format(avg)
					+ "≥"
					+ FMT_D.format(maxPrice)
					+ "＊95％</a>, 则以0.5%为一档，在1%至4%之间确定下浮系数f1，下浮系数计算公式#{style}f1＝mod(qi，7)*0.5%+1%</a>，即：#{style}"
					+ nDown + "＝mod(" + FMT_D.format(avg)
					+ "，7)*0.5%+1%</a></br>";
		}
		else if (maxPrice * 0.95 > avg && avg > maxPrice * 0.9)
		{
			down = "</br>4.若#{style}"
					+ FMT_D.format(maxPrice)
					+ "＊90％<"
					+ FMT_D.format(avg)
					+ "<="
					+ FMT_D.format(maxPrice)
					+ "＊95％</a>, 则以0.5%为一档，在-1%至2%之间确定下浮系数f1，下浮系数计算公式#{style}f1＝mod(qi，7)*0.5%-1%</a>,即：#{style}"
					+ nDown + "＝mod(" + FMT_D.format(avg)
					+ "，7)*0.5%-1%</a></br>";
		}
		else
		{
			// 修改日期（2018.02.20）根据2017.12.28的勘误，由下浮系数f1＝mod(qi，7)*0.5%-3%修改为f1＝mod(qi，7)*0.5%-4%
			down = "</br>4.若#{style}"
					+ FMT_D.format(maxPrice)
					+ "＊90％≥"
					+ FMT_D.format(avg)
					+ "</a>, 则以0.5%为一档，在-4%至-1%之间确定下浮系数f1，下浮系数计算公式#{style}f1＝mod(qi，7)*0.5%-4%</a>,即：#{style}"
					+ nDown + "＝mod(" + FMT_D.format(avg)
					+ "，7)*0.5%-4%</a></br>";
		}
		dowMsg.append(down);
		if (nDown >= 0)
		{
			dowMsg.append("5.评标基准价计算公式#{style}ri=qi*(1-f1)</a>，评标基准价#{style}"
					+ FMT_D.format(evaluation.get(0).getDouble(
							"N_EVALUATION_PRICE")) + "=" + FMT_D.format(avg)
					+ "*(1-" + nDown + ")</a>");
		}
		else
		{
			dowMsg.append("5.评标基准价计算公式#{style}ri=qi*(1-f1)</a>，评标基准价#{style}"
					+ FMT_D.format(evaluation.get(0).getDouble(
							"N_EVALUATION_PRICE")) + "=" + FMT_D.format(avg)
					+ "*(1+" + Math.abs(nDown) + ")</a>");
		}

		// StringBuilder remark = new StringBuilder();
		// remark.append("有效家数为" + validBidder + "家，");
		// remark.append("最高限价为 "
		// + FMT_D.format(section.getDouble("N_CONTROL_PRICE")) + "，");
		// remark.append("最高限价的85%为"
		// + FMT_D.format(section.getDouble("N_CONTROL_PRICE") * 0.85)
		// + "，");
		// remark.append("最高限价的90%为"
		// + FMT_D.format(section.getDouble("N_CONTROL_PRICE") * 0.90)
		// + "，");
		// remark.append("最高限价的95%为"
		// + FMT_D.format(section.getDouble("N_CONTROL_PRICE") * 0.95)
		// + "，");
		//
		// remark.append(dowMsg);

		// 评标办法
		section.setColumn("METHOD", evaluation.get(0).getColumn("N_METHOD"));
		// 评标基准价
		section.setColumn("EVALUATION", evaluation);
		// 有效投标家数
		section.setColumn("VALIDNUM", validNum);

		// 备注
		section.setColumn("REMARK", replaceStyle(dowMsg.toString()));
		section.setColumn("AVG", avg);
		// 最低限价
		section.setColumn("LESS", section.getDouble("N_CONTROL_PRICE") * 0.85);
		logger.debug(LogUtils.format("成功将评标基准价信息添加到标段"));
	}

	/**
	 * 
	 * 获取平均值的信息<br/>
	 * <p>
	 * 获取平均值的信息
	 * </p>
	 * 
	 * @param num
	 * @param record
	 * @param maxPrice
	 * @param hil
	 * @return
	 */
	public static String getAvgMsg(int num, Record<String, Object> record,
			double maxPrice, List<Record<String, Object>> hil)
	{
		logger.debug(LogUtils.format("获取平均值的信息"));
		LinkedList<Double> hi = new LinkedList<Double>();
		Double d = null;
		for (Record<String, Object> r : hil)
		{
			d = r.getDouble("N_PRICE");
			hi.add(d);
		}

		Collections.sort(hi);

		switch (record.getInteger("N_METHOD"))
		{
			case 1:
				return kqi1(num, record);
			case 2:
				return kqi2(num, record, maxPrice);
			case 3:
				return kqi3(num, record);
			case 4:
				return kqi4(num, record);
			case 5:
				return kqi5(num, record, maxPrice);
			default:
				return kqi1(num, record);
		}
	}

	/**
	 * 
	 * 方法一<br/>
	 * <p>
	 * </p>
	 * 
	 * @param num
	 * @param record
	 * @return
	 */
	private static String kqi1(int num, Record<String, Object> record)
	{
		logger.debug(LogUtils.format("方法一", num, record));
		String avgMsg = "3.根据方法1公式平均值#{style}qi＝pi/n</a>，得出平均值#{style}"
				+ FMT_D.format(record.getDouble("N_AVG")) + "="
				+ FMT_D.format(record.getDouble("N_VALID_SUM_PRICE")) + "/"
				+ num + "</a>";
		return avgMsg;
	}

	/**
	 * 
	 * 方法二<br/>
	 * <p>
	 * </p>
	 * 
	 * @param num
	 * @param record
	 * @param maxPrice
	 * @return
	 */
	private static String kqi2(int num, Record<String, Object> record,
			double maxPrice)
	{
		logger.debug(LogUtils.format("方法二", num, record, maxPrice));
		String avgMsg = "3.根据#{style}方法2</a>公式最高限价的权重#{style}f= mod(pi，6)*4%+20%</a>，得出权重系数#{style}f= mod("
				+ FMT_D.format(record.getDouble("N_VALID_SUM_PRICE"))
				+ "，6)*4%+20%</a>，权重#{style}f="
				+ record.getDouble("N_CONTROL_WEIGHT")
				+ "</a>,再依据公式#{style}qi=pi/n*(100%-f)+ xi*f</a>，得出加权平均值#{style}qi= "
				+ FMT_D.format(record.getDouble("N_VALID_SUM_PRICE"))
				+ "/"
				+ num
				+ "*(100%-"
				+ record.getDouble("N_CONTROL_WEIGHT")
				* 100
				+ "%)+ "
				+ FMT_D.format(maxPrice)
				+ "*"
				+ record.getDouble("N_CONTROL_WEIGHT")
				+ "</a>加权平均值#{style}qi="
				+ FMT_D.format(record.getDouble("N_WEIGHTING_AVG")) + "</a>";
		return avgMsg;
	}

	/**
	 * 
	 * 方法三<br/>
	 * 
	 * @param num
	 * @param record
	 * @return
	 */
	private static String kqi3(int num, Record<String, Object> record)
	{
		logger.debug(LogUtils.format("方法三", num, record));
		// 当n≤8时，从Ｈi去掉一个最高和一家最低报价后，取其余投标价计算平均值qi；
		if (8 >= num)
		{
			String avgMsg = "3.由于有效家数#{style}≤8</a>家，从有效投标总价去掉一个最高和一家最低报价后，取其余投标价计算平均值qi，#{style}qi="
					+ FMT_D.format(record.getDouble("N_AVG")) + "</a>";
			return avgMsg;
		}
		if (9 <= num && num <= 16)
		{

			String avgMsg = "3.当#{style}9≤有效家数≤16</a>时，从有效投标总价队列Ｈi中随机抽出70%的投标人（四舍五入）的投标价，再计算平均值qi，#{style}qi="
					+ FMT_D.format(record.getDouble("N_AVG")) + "</a>";
			return avgMsg;
		}
		// 当n>16时, 从队列Ｈi中随机抽出50％的投标人（四舍五入）的投标价计算平均值。方法如上，只需将70%改为５０％即可。
		String avgMsg = "3.有#{style}效家数>16</a>时, 从有效投标总价队列Ｈi中随机抽出50％的投标人（四舍五入）的投标价计算平均值。，再计算平均值qi，#{style}qi="
				+ FMT_D.format(record.getDouble("N_AVG")) + "</a>";
		return avgMsg;
	}

	/**
	 * 
	 * 方法四<br/>
	 * 
	 * @param num
	 * @param record
	 * @return
	 */
	private static String kqi4(int num, Record<String, Object> record)
	{
		logger.debug(LogUtils.format("方法四", num, record));

		// 当n≤10时，从队列Ｈi中去掉一个最高和最低报价后，取其余投标价计算平均值qi
		if (num <= 10)
		{
			String avgMsg = "3.当#{style}有效家数≤10</a>时，从有效投标总价队列Ｈi中去掉一个最高和最低报价后，取其余投标价计算平均值qi，#{style}qi="
					+ FMT_D.format(record.getDouble("N_AVG")) + "</a>";
			return avgMsg;
		}
		// 当n>10时，从队列Ｈi中通过先去掉一个最低报价后，再去掉一个最高报价的顺序进行循环，取中间8个投标价计算平均值pi。
		String avgMsg = "3.当#{style}有效家数10</a>时，从有效投标总价队列Ｈi中通过先去掉一个最低报价后，再去掉一个最高报价的顺序进行循环，取中间8个投标价计算平均值qi，#{style}qi="
				+ FMT_D.format(record.getDouble("N_AVG")) + "</a>";
		return avgMsg;
	}

	/**
	 * 
	 * 方法五<br/>
	 * <p>
	 * 方法五
	 * </p>
	 * 
	 * @param num
	 * @param record
	 * @param maxPrice
	 * @return
	 */
	private static String kqi5(int num, Record<String, Object> record,
			double maxPrice)
	{
		logger.debug(LogUtils.format("方法五", num, record, maxPrice));
		String avgMsg = "3.计算队列Ｈi中投标价的平均值#{style}"
				+ FMT_D.format(record.getDouble("N_ALLPRICE_AVG"))
				+ "</a>，再将低于最高限价#{style}" + FMT_D.format(maxPrice)
				+ "</a>且高于该平均值#{style}"
				+ FMT_D.format(record.getDouble("N_ALLPRICE_AVG"))
				+ "</a>的投标价再一次进行平均,得到新的平均值qi，#{style}qi="
				+ FMT_D.format(record.getDouble("N_DU_PRICE_AVG")) + "</a>";
		return avgMsg;
	}
}
