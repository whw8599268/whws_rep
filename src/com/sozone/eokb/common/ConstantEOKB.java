/**
 * 包名：com.sozone.ebidpb.common
 * 文件名：Constant.java<br/>
 * 创建时间：2017-8-1 上午10:37:10<br/>
 * 创建者：zouye<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.common;

/**
 * 常量定义<br/>
 * <p>
 * 常量定义<br/>
 * </p>
 * Time：2017-8-1 上午10:37:10<br/>
 * 
 * @author zouye
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ConstantEOKB
{

	/**
	 * 缺省的字符集
	 */
	String DEFAULT_CHARSET = "UTF-8";

	/**
	 * GBK
	 */
	String GBK_CHARSET = "GBK";

	/**
	 * 评标数据源变量名
	 */
	String PB_DB_ID_VAR = "PB_DB_ID";

	/**
	 * 默认解密时长,单位毫秒
	 */
	long DEFAULT_DECRYPT_TIME = 60 * 60 * 1000;

	/**
	 * 确认时长（5分钟）
	 */
	long CONFIRM_DURATION = 5 * 60 * 1000;
	/**
	 * 确认时长（10分钟）
	 */
	long CONFIRM_DURATION_TEN = 10 * 60 * 1000;

	/**
	 * 招标人开标列表显示 据开标时间分钟数
	 */
	int BIDOPEN_TIME_DISTANCE = 30;

	/**
	 * 招标项目信息SESSION KEY
	 */
	String TENDER_PROJECT_INFO_SESSION_KEY = "TENDER_PROJECT_INFO";

	/**
	 * 招标项目流程信息SESSION KEY
	 */
	String TENDER_PROJECT_FLOW_INFO_SESSION_KEY = "TENDER_PROJECT_FLOW_INFO";

	/**
	 * 视频是否可用状态SESSION KEY
	 */
	String VIDEO_STATUS_SESSION_KEY = "VIDEO_STATUS";

	/**
	 * 即时通讯是否可用状态SESSION KEY
	 */
	String IM_STATUS_SESSION_KEY = "IM_STATUS";

	/**
	 * 云视睿博聊天是否可用状态SESSION KEY
	 */
	String NTV_STATUS_SESSION_KEY = "NTV_STATUS";

	/**
	 * 云视睿博地址SESSION KEY
	 */
	String NTV_URL_SESSION_KEY = "NTV_URL";

	/**
	 * 电子唱标是否可用状态SESSION KEY
	 */
	String SING_STATUS_SESSION_KEY = "SING_STATUS";

	/**
	 * 结束开标按钮是否可显示SESSION KEY
	 */
	String RE_END_STATUS_SESSION_KEY = "RE_END_STATUS";

	/**
	 * 开标状态SESSION KEY
	 */
	String BID_OPEN_STATUS_SESSION_KEY = "BID_OPEN_STATUS";

	/**
	 * 第一信封标示
	 */
	String FIRST_ENVELOPE_TAG = "111";

	/**
	 * 第二信封标示
	 */
	String SECOND_ENVELOPE_TAG = "112";

	/**
	 * 第三信封标示
	 */
	String THIRD_ENVELOPE_TAG = "";

	/**
	 * 共用第一信封标示
	 */
	String SHARE_FIRST_ENVELOPE_TAG = "611";

	/**
	 * 投标文件信封标识
	 */
	String BIDDER_DOC_ENVELOPE_TAG = "113";

	/**
	 * 厦门房建市信封标识
	 */
	String XM_FJSZ_BIDDER_DOC_TAG = "115";

	/**
	 * 评标基准价标识
	 */
	String BSPM_BUS_FLAG_TYPE = "BSPM-TYPE";

	/**
	 * 下浮系数标识
	 */
	String LC_BUS_FLAG_TYPE = "LC-TYPE";

	/**
	 * 最高权重标识
	 */
	String HW_BUS_FLAG_TYPE = "HW-TYPE";

	/**
	 * E值标识
	 */
	String EV_BUS_FLAG_TYPE = "EV-TYPE";

	/**
	 * 计算评标基准价标识
	 */
	String CBSP_BUS_FLAG_TYPE = "CBSP-TYPE";

	/**
	 * 三层解密环节标识
	 */
	String THREE_PARTS_DECRYPT_V3 = "3DECRYPT-V3";

	/**
	 * 三层解密环节标识
	 */
	String THREE_PARTS_DECRYPT = "THREE-PARTS-DECRYPT";

	/**
	 * 招标文件解压路径目录名
	 */
	String TENDER_DOC_UNPACK_DIR_NAME = "TENDER_DOC_UNPACK";

	/**
	 * 评标办法JSON
	 */
	String PB_METHOD_JSON_INFO_SESSION_KEY = "PB_METHOD_JSON_INFO";

	/**
	 * 厦门房建市政投标人编号要求标识
	 */
	String XMFJSZ_BIDDER_NO_BUS_FLAG_TYPE = "XMFJSZ_BIDDER_NO_EXTRACT";

	/**
	 * 普通公路监理费率
	 */
	String IS_APPRAISAL_PRICE_RATE = "IS_APPRAISAL_PRICE_RATE";

	/**
	 * A:表示投标文件生成
	 */
	String TB_TYPE_A = "A";

	/**
	 * B:表示投标文件投递
	 */
	String TB_TYPE_B = "B";

	/**
	 * C:表示投标文件暂存
	 */
	String TB_TYPE_C = "C";

	/**
	 * 厦门房建市政K值标识
	 */
	String K_VALUE = "K_VALUE";

	/**
	 * 厦门房建市政K值整数位标识
	 */
	String K_VALUE_1 = "K_VALUE1";

	/**
	 * 厦门房建市政K值小数后一位标识
	 */
	String K_VALUE_2 = "K_VALUE2";

	/**
	 * 厦门房建市政K值小数后两位标识
	 */
	String K_VALUE_3 = "K_VALUE3";

	/**
	 * 厦门房建市政评标基准价标识
	 */
	String BENCHMARK = "BENCHMARK";

	/**
	 * 厦门房建市政电子唱标标识
	 */
	String XMFJSZ_SING = "XMFJSZ_SING";

	/**
	 * 厦门房建市政监理综评估法方法标识
	 */
	String XMFJSZ_JL_METHOD = "XMFJSZ_JL_METHOD";

	/**
	 * 第一信封备注标识
	 */
	String FIRST_REMARK = "FIRST_REMARK";

	/**
	 * 第二信封备注标识
	 */
	String SECOND_REMARK = "SECOND_REMARK";

	/**
	 * 第三信封备注标识
	 */
	String THIRD_REMARK = "THIRD_REMARK";

	/**
	 * 推送投标人信息标识
	 */
	String PUSH_BIDDER_INFO = "PUSH_BIDDER_INFO";

	// -----------------------------------------------
	/**
	 * 房建市政投标人代表号生产规则
	 */
	String BIDDER_NO_RULE = "BIDDER_NO_GENERATE_RULE";

	/**
	 * 
	 * 开标流程code<br/>
	 * <p>
	 * 开标流程code<br/>
	 * </p>
	 * Time：2018-1-3 上午9:33:15<br/>
	 * 
	 * @author Administrator
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	public interface EOKBFlowCode
	{
		/**
		 * 信用等级标识
		 */
		String DYXF_CREDIT = "DYXF_credit";

		/**
		 * 电子摇号标识
		 */
		String DYXF_ELECTRONICS = "DYXF_electronics";

		/**
		 * 开标结果标识
		 */
		String DYXF_OFFER = "DYXF_offer";
		/**
		 * 评审结果标识
		 */
		String DYXF_REVIEW = "DYXF_review";

		/**
		 * 第一信封
		 */
		String FIRST_ENVELOPE = "firstEnvelope";

		/**
		 * 第二信封
		 */
		String SECOND_ENVELOPE = "secondEnvelope";

		/**
		 * 第三信封
		 */
		String THIRD_ENVELOPE = "thirdEnvelope";

		/**
		 * 第二信封开标结果标识
		 */
		String DEXF_OFFER = "DEXF_offer";

		/**
		 * 评标基准价标识
		 */
		String DEXF_PRICE = "DEXF_price";

		/**
		 * 第一信封开标结束标识
		 */
		String FIRST_OVER = "2-1";

		/**
		 * 第二信封开标结束标识
		 */
		String SECOND_OVER = "2-2";

		/**
		 * 开标完毕标识
		 */
		String BID_OVER = "2";

		/**
		 * 空
		 * 
		 * @return 空
		 */
		String toString();
	}

	/**
	 * 
	 * 评标办法code<br/>
	 * <p>
	 * 评标办法code<br/>
	 * </p>
	 * Time：2017-9-16 下午10:36:38<br/>
	 * 
	 * @author Administrator
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	public interface EOKBBemCode
	{
		/**
		 * 福建省_高速公路_路基施工_合理低价法_v1
		 */
		String FJS_GSGL_LJSG_HLDJF_V1 = "fjs_gsgl_ljsg_hldjf_v1";
		/**
		 * 福建省_高速公路_勘察设计_综合评估法_v1
		 */
		String FJS_GSGL_KCSJ_ZHPGF1_V1 = "fjs_gsgl_kcsj_zhpgf1_v1";
		/**
		 * 福建省_高速公路_勘察设计_综合评估法_v2
		 */
		String FJS_GSGL_KCSJ_ZHPGF1_V2 = "fjs_gsgl_kcsj_zhpgf1_v2";
		/**
		 * 福建省_高速公路_勘察设计_勘察监理_综合评估法_v1
		 */
		String FJS_GSGL_KCJL_SJSC_ZHPGF1_V1 = "fjs_gsgl_kcjl_sjsc_zhpgf1_v1";
		/**
		 * 福建省_高速公路_勘察设计_勘察监理_综合评估法_v2
		 */
		String FJS_GSGL_KCJL_SJSC_ZHPFF_V2 = "fjs_gsgl_kcjl_sjsc_zhpff_v2";
		/**
		 * 福建省_高速公路_机电施工-合理低价法_v1
		 */
		String FJS_GSGL_JDSG_HLDJF_V1 = "fjs_gsgl_jdsg_hldjf_v1";
		/**
		 * 福建省_水运工程_勘察设计-合理低价法_v1
		 */
		String FJS_SYGC_KCSJ_HLDJF_V1 = "fjs_sygc_kcsj_hldjf_v1";
		/**
		 * 福建省_水运工程_施工_合理低价法_v1
		 */
		String FJS_SYGC_GCSG_HLDJF_V1 = "fjs_sygc_gcsg_hldjf_v1";
		/**
		 * 福建省_水运工程_施工监理_合理低价法_v1
		 */
		String FJS_SYGC_GCSGJL_HLDJF_V1 = "fjs_sygc_gcsgjl_hldjf_v1";
		/**
		 * 福建省_高速公路_土建监理_合理低价法_v1
		 */
		String FJS_GSGL_TJSGJL_HLDJF_V1 = "fjs_gsgl_tjsgjl_hldjf_v1";
		/**
		 * 福建省_高速公路_土建监理_合理低价法_v2
		 */
		String FJS_GSGL_TJSGJL_HLDJF_V2 = "fjs_gsgl_tjsgjl_hldjf_v2";
		/**
		 * 福建省_高速公路_机电监理_合理低价法_v1
		 */
		String FJS_GSGL_JDJL_HLDFJ_V1 = "fjs_gsgl_jdjl_hldfj_v1";
		/**
		 * 福建省_高速公路_试验检测服务_合理低价法_v1
		 */
		String FJS_GSGL_SYJCFW_HLDJF_V1 = "fjs_gsgl_syjcfw_hldjf_v1";
		/**
		 * 福建省_高速公路_交工（竣工）_综合评分法_v1
		 */
		String FJS_GSGL_JGJG_ZHPFF_V1 = "fjs_gsgl_jgjg_zhpff_v1";
		/**
		 * 福建省_高速公路_交工竣工_验收前质量检测_综合评分法_v1
		 */
		String FJS_GSGL_JGJG_YSQZLJC_ZHPFF_V1 = "fjs_gsgl_jgjg_ysqzljc_zhpff_v1";
		/**
		 * 福建省_高速公路_交工竣工_验收前质量检测_综合评分法_v2
		 */
		String FJS_GSGL_JGJG_YSQZLJC_ZHPFF_V2 = "fjs_gsgl_jgjg_ysqzljc_zhpff_v2";
		/**
		 * 福建省_高速公路_路面工程施工_合理低价法_v1
		 */
		String FJS_GSGL_LMSG_HLDJF_V1 = "fjs_gsgl_lmsg_hldjf_v1";

		/**
		 * 福建省_普通公路_工程施工_合理低价法_v1
		 */
		String FJS_PTGL_GCSG_HLDJF_V1 = "fjs_ptgl_gcsg_hldjf_v1";
		/**
		 * 福建省_普通公路_工程施工_合理低价法_v2（2018-05新范本）
		 */
		String FJS_PTGL_GCSG_HLDJF_V2 = "fjs_ptgl_gcsg_hldjf_v2";
		/**
		 * 福建省_普通公路_工程施工_固定价随机抽取中标人(简易招标办法)_v2（2018-05新范本）
		 */
		String FJS_PTGL_GCSG_GDSJCQZBR_V2 = "fjs_ptgl_gcsg_gdsjcqzbr_v2";
		/**
		 * 福建省_普通公路_工程施工_合理低价法+信用分_v2（2018-05新范本）
		 */
		String FJS_PTGL_GCSG_HLDJFXYF_V2 = "fjs_ptgl_gcsg_hldjfxyf_v2";
		/**
		 * 福建省_普通公路_工程施工_合理低价法+信用分_v3（2018-05新范本）
		 */
		String FJS_PTGL_GCSG_HLDJFXYF_V3 = "fjs_ptgl_gcsg_hldjfxyf_v3";
		/**
		 * 福建省普通公路工程勘察设计-综合评估法I_v1
		 */
		String FJS_PTGL_KCSJ_ZHPGF1_V1 = "fjs_ptgl_kcsj_zhpgf1_v1";
		/**
		 * 福建省_普通公路_工程勘察设计_综合评估法I_V2
		 */
		String FJS_PTGL_KCSJ_ZHPGF1_V2 = "fjs_ptgl_kcsj_zhpgf1_v2";
		/**
		 * 福建省普通公路工程勘察设计-合理低价+信用分_v1
		 */
		String FJS_PTGL_KCSJ_HLDJFXYF_V1 = "fjs_ptgl_kcsj_hldjfxyf_v1";
		/**
		 * 福建省普通公路工程勘察设计-合理低价+信用分_v2
		 */
		String FJS_PTGL_KCSJ_HLDJFXYF_V2 = "fjs_ptgl_kcsj_hldjfxyf_v2";
		/**
		 * 福建省普通公路工程施工监理-合理低价+信用分_v1
		 */
		String FJS_PTGL_SGJL_HLDJF_V1 = "fjs_ptgl_sgjl_hldjf_v1";
		/**
		 * 福建省普通公路工程施工监理-合理低价+信用分_v2
		 */
		String FJS_PTGL_SGJL_HLDJFXYF_V2 = "FJS_PTGL_SGJL_HLDJFXYF_V2";
		/**
		 * 福建省普通公路工程施工监理-合理低价+信用分_v3
		 */
		String FJS_PTGL_SGJL_HLDJFXYF_V3 = "FJS_PTGL_SGJL_HLDJFXYF_V3";
		/**
		 * 福建省普通囯省道养护大中修工程施工_合理低价（90分）+信用分（10分）_v1
		 */
		String FJS_PTGL_GDYH_HLDJFXYF_V1 = "fjs_ptgl_gdyh_hldjfxyf_v1";
		/**
		 * 福建省_普通公路_国道养护_固定价随机抽取中标人法_v1
		 */
		String FJS_PTGL_GDYH_GDJSJCQZBHXRF_V1 = "fjs_ptgl_gdyh_gdjsjcqzbhxrf_v1";

		/**
		 * 福建省_普通公路_国道养护_合理低价法_v2
		 */
		String FJS_PTGL_GDYH_HLDJF_V2 = "fjs_ptgl_gdyh_hldjf_v2";

		/**
		 * 福建省_普通公路_国道养护_合理低价法+信用分_v2
		 */
		String FJS_PTGL_GDYH_HLDJFXYF_V2 = "fjs_ptgl_gdyh_hldjfxyf_v2";
		/**
		 * 福建省_普通公路_通用版_v1
		 */
		String FJS_PTGL_TYB_V1 = "fjs_ptgl_tyb_v1";
		/**
		 * 福建省水运工程试验检测_合理低价法_v1
		 */
		String FJS_SYGC_SYJC_HLDJF_V1 = "fjs_sygc_syjc_hldjf_v1";
		/**
		 * 福建省_水运工程_通用版_v1
		 */
		String FJS_SYGC_TYB_V1 = "fjs_sygc_tyb_v1";

		/**
		 * 厦门市_房屋建筑_施工_经评审的最低投标价中标法A类_v1
		 */
		String XMS_FWJZ_SG_JPSDZDTBJZBF_A_V1 = "xms_fwjz_sg_jpsdzdtbjzbf_A_v1";
		/**
		 * 厦门市_房屋建筑_施工_经评审的最低投标价中标法B类_v1
		 */
		String XMS_FWJZ_SG_JPSDZDTBJZBF_B_V1 = "xms_fwjz_sg_jpsdzdtbjzbf_B_v1";
		/**
		 * 厦门市_房屋建筑_施工_综合评估法A类_v1
		 */
		String XMS_FWJZ_SG_ZHPGF_A_V1 = "xms_fwjz_sg_zhpgf_A_v1";
		/**
		 * 厦门市_房屋建筑_施工_综合评估法B类_v1
		 */
		String XMS_FWJZ_SG_ZHPGF_B_V1 = "xms_fwjz_sg_zhpgf_B_v1";
		/**
		 * 厦门市_房屋建筑_施工_简易评标法_v1
		 */
		String XMS_FWJZ_SG_JYPBF_V1 = "xms_fwjz_sg_jypbf_v1";
		/**
		 * 厦门市_房屋建筑_施工_经评审的最低投标价中标法A类_v2
		 */
		String XMS_FWJZ_SG_JPSDZDTBJZBF_A_V2 = "xms_fwjz_sg_jpsdzdtbjzbf_A_v2";
		/**
		 * 厦门市_房屋建筑_施工_经评审的最低投标价中标法B类_v2
		 */
		String XMS_FWJZ_SG_JPSDZDTBJZBF_B_V2 = "xms_fwjz_sg_jpsdzdtbjzbf_B_v2";
		/**
		 * 厦门市_房屋建筑_施工_综合评估法A类_v2
		 */
		String XMS_FWJZ_SG_ZHPGF_A_V2 = "xms_fwjz_sg_zhpgf_A_v2";
		/**
		 * 厦门市_房屋建筑_施工_综合评估法B类_v2
		 */
		String XMS_FWJZ_SG_ZHPGF_B_V2 = "xms_fwjz_sg_zhpgf_B_v2";
		/**
		 * 厦门市_房屋建筑_施工_简易评标法_v2
		 */
		String XMS_FWJZ_SG_JYPBF_V2 = "xms_fwjz_sg_jypbf_v2";
		/**
		 * 厦门市_房屋建筑_监理_综合评估法_v1
		 */
		String XMS_FWJZ_JL_ZHPGF_V1 = "xms_fwjz_jl_zhpgf_v1";
		/**
		 * 厦门市_房屋建筑_监理_经评审的选取法_v1
		 */
		String XMS_FWJZ_JL_JPSDXQF_V1 = "xms_fwjz_jl_jpsdxqf_v1";
		/**
		 * 厦门市_房屋建筑_勘察_简易评估法_资格后审_v1
		 */
		String XMS_FWJZ_KC_JYPGF_ZGHS_V1 = "xms_fwjz_kc_jypgf_zghs_v1";
		/**
		 * 厦门市_房屋建筑_勘察_简易评估法_资格预审_前_v1
		 */
		String XMS_FWJZ_KC_JYPGF_ZGHS_V1_1 = "xms_fwjz_kc_jypgf_zghs_v1_1";
		/**
		 * 厦门市_房屋建筑_勘察_简易评估法_资格预审_后_v1
		 */
		String XMS_FWJZ_KC_JYPGF_ZGHS_V1_2 = "xms_fwjz_kc_jypgf_zghs_v1_2";
		/**
		 * 厦门市_房屋建筑_勘察_综合评估法_资格后审_v1
		 */
		String XMS_FWJZ_KC_ZHPGF_ZGHS_V1 = "xms_fwjz_kc_zhpgf_zghs_v1";
		/**
		 * 厦门市_房屋建筑_勘察_综合评估法_资格后审_前_v1
		 */
		String XMS_FWJZ_KC_ZHPGF_ZGHS_V1_1 = "xms_fwjz_kc_zhpgf_zghs_v1_1";
		/**
		 * 厦门市_房屋建筑_勘察_综合评估法_资格后审_后_v1
		 */
		String XMS_FWJZ_KC_ZHPGF_ZGHS_V1_2 = "xms_fwjz_kc_zhpgf_zghs_v1_2";
		/**
		 * 厦门市_房屋建筑_勘察_简易评估法_v2
		 */
		String XMS_FWJZ_KC_JYPGF_V2 = "xms_fwjz_kc_jypgf_v2";
		/**
		 * 厦门市_房屋建筑_勘察_简易评估法_前_v2
		 */
		String XMS_FWJZ_KC_JYPGF_V2_1 = "xms_fwjz_kc_jypgf_v2_1";
		/**
		 * 厦门市_房屋建筑_勘察_简易评估法_后_v2
		 */
		String XMS_FWJZ_KC_JYPGF_V2_2 = "xms_fwjz_kc_jypgf_v2_2";
		/**
		 * 厦门市_房屋建筑_勘察_综合评估法_资格后审_v2
		 */
		String XMS_FWJZ_KC_ZHPGF_V2 = "xms_fwjz_kc_zhpgf_v2";
		/**
		 * 厦门市_房屋建筑_勘察_综合评估法_资格后审_前_v2
		 */
		String XMS_FWJZ_KC_ZHPGF_V2_1 = "xms_fwjz_kc_zhpgf_v2_1";
		/**
		 * 厦门市_房屋建筑_勘察_综合评估法_资格后审_后_v2
		 */
		String XMS_FWJZ_KC_ZHPGF_V2_2 = "xms_fwjz_kc_zhpgf_v2_2";
		/**
		 * 厦门市_房屋建筑_设备_综合评估法_v1
		 */
		String XMS_FWJZ_SB_ZHPGF_V1 = "xms_fwjz_sb_zhpgf_v1";
		/**
		 * 厦门市_房屋建筑_设备_经评审的最低投标价法_v1
		 */
		String XMS_FWJZ_SB_JPSDZDTBJF_V1 = "xms_fwjz_sb_jpsdzdtbjf_v1";
		/**
		 * 厦门市_房屋建筑_材料_综合评估法_v1
		 */
		String XMS_FWJZ_CL_ZHPGF_V1 = "xms_fwjz_cl_zhpgf_v1";
		/**
		 * 厦门市_房屋建筑_材料_经评审的最低投标价法_v1
		 */
		String XMS_FWJZ_CL_JPSDZDTBJF_V1 = "xms_fwjz_cl_jpsdzdtbjf_v1";
		/**
		 * 厦门市_房屋建筑_设计13版_记名投票法_v1
		 */
		String XMS_FWJZ_SJ_JMTPF_V1 = "xms_fwjz_sj_jmtpf_v1";
		/**
		 * 厦门市_房屋建筑_设计13版_记名投票法_资格预审_前_v1
		 */
		String XMS_FWJZ_SJ_JMTPF_V1_1 = "xms_fwjz_sj_jmtpf_v1_1";
		/**
		 * 厦门市_房屋建筑_设计13版_记名投票法_资格预审_后_v1
		 */
		String XMS_FWJZ_SJ_JMTPF_V1_2 = "xms_fwjz_sj_jmtpf_v1_2";
		/**
		 * 厦门市_房屋建筑_设计_排序法_资格后审_v1
		 */
		String XMS_FWJZ_SJ_PXF_V1 = "xms_fwjz_sj_pxf_v1";
		/**
		 * 厦门市_房屋建筑_设计_排序法_资格预审_前_v1
		 */
		String XMS_FWJZ_SJ_PXF_V1_1 = "xms_fwjz_sj_pxf_v1_1";
		/**
		 * 厦门市_房屋建筑_设计_排序法_资格预审_后_v1
		 */
		String XMS_FWJZ_SJ_PXF_V1_2 = "xms_fwjz_sj_pxf_v1_2";
		/**
		 * 厦门市_房屋建筑_设计13版_记名投票法_v2
		 */
		String XMS_FWJZ_SJ_JMTPF_V2 = "xms_fwjz_sj_jmtpf_v2";
		/**
		 * 厦门市_房屋建筑_设计13版_记名投票法_资格预审_前_v2
		 */
		String XMS_FWJZ_SJ_JMTPF_V2_1 = "xms_fwjz_sj_jmtpf_v2_1";
		/**
		 * 厦门市_房屋建筑_设计13版_记名投票法_资格预审_后_v2
		 */
		String XMS_FWJZ_SJ_JMTPF_V2_2 = "xms_fwjz_sj_jmtpf_v2_2";
		/**
		 * 厦门市_房屋建筑_设计_排序法_资格后审_v2
		 */
		String XMS_FWJZ_SJ_PXF_V2 = "xms_fwjz_sj_pxf_v2";
		/**
		 * 厦门市_房屋建筑_设计_排序法_资格预审_前_v2
		 */
		String XMS_FWJZ_SJ_PXF_V2_1 = "xms_fwjz_sj_pxf_v2_1";
		/**
		 * 厦门市_房屋建筑_设计_排序法_资格预审_后_v2
		 */
		String XMS_FWJZ_SJ_PXF_V2_2 = "xms_fwjz_sj_pxf_v2_2";
		/**
		 * 厦门市_房屋建筑_设计_综合评估法_2018版_v2
		 */
		String XMS_FWJZ_SJ_ZHPGF_V2 = "xms_fwjz_sj_zhpgf_v2";
		/**
		 * 厦门市_房屋建筑_设计_综合评估法_2018版_前_v2
		 */
		String XMS_FWJZ_SJ_ZHPGF_V2_1 = "xms_fwjz_sj_zhpgf_v2_1";
		/**
		 * 厦门市_房屋建筑_设计_综合评估法_2018版_后_v2
		 */
		String XMS_FWJZ_SJ_ZHPGF_V2_2 = "xms_fwjz_sj_zhpgf_v2_2";
		/**
		 * 厦门市_房屋建筑_园林绿化_经评审的最低投标价中标法B类_v1
		 */
		String XMS_FWJZ_YLLH_JPSDZDTBJZBF_B_V1 = "xms_fwjz_yllh_jpsdzdtbjzbf_B_v1";
		/**
		 * 厦门市_房屋建筑_园林绿化_简易评标法_v1
		 */
		String XMS_FWJZ_YLLH_JYPBF_V1 = "xms_fwjz_yllh_jypbf_v1";
		/**
		 * 厦门市_房屋建筑_通用版_v1
		 */
		String XMS_FWJZ_TYB_V1 = "xms_fwjz_tyb_v1";
		/**
		 * 厦门市_房屋建筑_通用版_资格预审_前_v1
		 */
		String XMS_FWJZ_TYB_V1_1 = "xms_fwjz_tyb_v1_1";
		/**
		 * 厦门市_房屋建筑_通用版_资格预审_后_v1
		 */
		String XMS_FWJZ_TYB_V1_2 = "xms_fwjz_tyb_v1_2";
		/**
		 * 厦门市_房屋建筑_监理_简易评标法_V2
		 */
		String XMS_FWJZ_JL_JYPBF_V2 = "xms_fwjz_jl_jypbf_v2";
		/**
		 * 厦门市_房屋建筑_监理_简易评标法_V2
		 */
		String XMS_FWJZ_JL_ZHPGF_V2 = "xms_fwjz_jl_zhpgf_v2";
		/**
		 * 厦门市_房屋建筑_集美小项目_V1
		 */
		String XMS_FWJZ_JMXXM_TYB_V1 = "xms_fwjz_jmxxm_tyb_v1";
		/**
		 * 厦门市_房屋建筑_翔安小项目_V1
		 */
		String XMS_FWJZ_TAXXM_TYB_V1 = "xms_fwjz_taxxm_tyb_v1";

		/**
		 * 厦门市_市政工程_施工_经评审的最低投标价中标法A类_v1
		 */
		String XMS_SZGC_SG_JPSDZDTBJZBF_A_V1 = "xms_szgc_sg_jpsdzdtbjzbf_A_v1";
		/**
		 * 厦门市_市政工程_施工_经评审的最低投标价中标法B类_v1
		 */
		String XMS_SZGC_SG_JPSDZDTBJZBF_B_V1 = "xms_szgc_sg_jpsdzdtbjzbf_B_v1";
		/**
		 * 厦门市_市政工程_施工_综合评估法A类_v1
		 */
		String XMS_SZGC_SG_ZHPGF_A_V1 = "xms_szgc_sg_zhpgf_A_v1";
		/**
		 * 厦门市_市政工程_施工_综合评估法B类_v1
		 */
		String XMS_SZGC_SG_ZHPGF_B_V1 = "xms_szgc_sg_zhpgf_B_v1";
		/**
		 * 厦门市_市政工程_施工_简易评标法_v1
		 */
		String XMS_SZGC_SG_JYPBF_V1 = "xms_szgc_sg_jypbf_v1";
		/**
		 * 厦门市_市政工程_施工_经评审的最低投标价中标法A类_v2
		 */
		String XMS_SZGC_SG_JPSDZDTBJZBF_A_V2 = "xms_szgc_sg_jpsdzdtbjzbf_A_v2";
		/**
		 * 厦门市_市政工程_施工_经评审的最低投标价中标法B类_v2
		 */
		String XMS_SZGC_SG_JPSDZDTBJZBF_B_V2 = "xms_szgc_sg_jpsdzdtbjzbf_B_v2";
		/**
		 * 厦门市_市政工程_施工_综合评估法A类_v2
		 */
		String XMS_SZGC_SG_ZHPGF_A_V2 = "xms_szgc_sg_zhpgf_A_v2";
		/**
		 * 厦门市_市政工程_施工_综合评估法B类_v2
		 */
		String XMS_SZGC_SG_ZHPGF_B_V2 = "xms_szgc_sg_zhpgf_B_v2";
		/**
		 * 厦门市_市政工程_施工_简易评标法_v2
		 */
		String XMS_SZGC_SG_JYPBF_V2 = "xms_szgc_sg_jypbf_v2";
		/**
		 * 厦门市_市政工程_监理_综合评估法_v1
		 */
		String XMS_SZGC_JL_ZHPGF_V1 = "xms_szgc_jl_zhpgf_v1";
		/**
		 * 厦门市_市政工程_监理_经评审的选取法_v1
		 */
		String XMS_SZGC_JL_JPSDXQF_V1 = "xms_szgc_jl_jpsdxqf_v1";
		/**
		 * 厦门市_市政工程_勘察_简易评估法_资格后审_v1
		 */
		String XMS_SZGC_KC_JYPGF_ZGHS_V1 = "xms_szgc_kc_jypgf_zghs_v1";
		/**
		 * 厦门市_市政工程_勘察_简易评估法_资格预审_前_v1
		 */
		String XMS_SZGC_KC_JYPGF_ZGHS_V1_1 = "xms_szgc_kc_jypgf_zghs_v1_1";
		/**
		 * 厦门市_市政工程_勘察_简易评估法_资格预审_后_v1
		 */
		String XMS_SZGC_KC_JYPGF_ZGHS_V1_2 = "xms_szgc_kc_jypgf_zghs_v1_2";
		/**
		 * 厦门市_市政工程_勘察_综合评估法-资格后审_v1
		 */
		String XMS_SZGC_KC_ZHPGF_ZGHS_V1 = "xms_szgc_kc_zhpgf_zghs_v1";
		/**
		 * 厦门市_市政工程_勘察_综合评估法-资格预审_v1
		 */
		String XMS_SZGC_KC_ZHPGF_ZGHS_V1_1 = "xms_szgc_kc_zhpgf_zghs_v1_1";
		/**
		 * 厦门市_市政工程_勘察_综合评估法-资格预审_v1
		 */
		String XMS_SZGC_KC_ZHPGF_ZGHS_V1_2 = "xms_szgc_kc_zhpgf_zghs_v1_2";
		/**
		 * 厦门市_市政工程_勘察_简易评估法_资格后审_v2
		 */
		String XMS_SZGC_KC_JYPGF_V2 = "xms_szgc_kc_jypgf_v2";
		/**
		 * 厦门市_市政工程_勘察_简易评估法_资格预审_前_v2
		 */
		String XMS_SZGC_KC_JYPGF_V2_1 = "xms_szgc_kc_jypgf_v2_1";
		/**
		 * 厦门市_市政工程_勘察_简易评估法_资格预审_后_v2
		 */
		String XMS_SZGC_KC_JYPGF_V2_2 = "xms_szgc_kc_jypgf_v2_2";
		/**
		 * 厦门市_市政工程_勘察_综合评估法-资格后审_v2
		 */
		String XMS_SZGC_KC_ZHPGF_V2 = "xms_szgc_kc_zhpgf_v2";
		/**
		 * 厦门市_市政工程_勘察_综合评估法-资格预审_v2
		 */
		String XMS_SZGC_KC_ZHPGF_V2_1 = "xms_szgc_kc_zhpgf_v2_1";
		/**
		 * 厦门市_市政工程_勘察_综合评估法-资格预审_v2
		 */
		String XMS_SZGC_KC_ZHPGF_V2_2 = "xms_szgc_kc_zhpgf_v2_2";
		/**
		 * 厦门市_市政工程_设备_综合评估法_v1
		 */
		String XMS_SZGC_SB_ZHPGF_V1 = "xms_szgc_sb_zhpgf_v1";
		/**
		 * 厦门市_市政工程_设备_经评审的最低投标价法_v1
		 */
		String XMS_SZGC_SB_JPSDZDTBJF_V1 = "xms_szgc_sb_jpsdzdtbjf_v1";
		/**
		 * 厦门市_市政工程_材料_综合评估法_v1
		 */
		String XMS_SZGC_CL_ZHPGF_V1 = "xms_szgc_cl_zhpgf_v1";
		/**
		 * 厦门市_市政工程_材料_经评审的最低投标价法_v1
		 */
		String XMS_SZGC_CL_JPSDZDTBJF_V1 = "xms_szgc_cl_jpsdzdtbjf_v1";
		/**
		 * 厦门市_市政工程_设计15版_记名投票法_v1
		 */
		String XMS_SZGC_SJ_JMTPF_V1 = "xms_szgc_sj_jmtpf_v1";
		/**
		 * 厦门市_市政工程_设计15版_记名投票法_资格预审_前_v1
		 */
		String XMS_SZGC_SJ_JMTPF_V1_1 = "xms_szgc_sj_jmtpf_v1_1";
		/**
		 * 厦门市_市政工程_设计15版_记名投票法_资格预审_后_v1
		 */
		String XMS_SZGC_SJ_JMTPF_V1_2 = "xms_szgc_sj_jmtpf_v1_2";
		/**
		 * 厦门市_市政工程_设计15版_记名投票法_v2
		 */
		String XMS_SZGC_SJ_JMTPF_V2 = "xms_szgc_sj_jmtpf_v2";
		/**
		 * 厦门市_市政工程_设计15版_记名投票法_资格预审_前_v2
		 */
		String XMS_SZGC_SJ_JMTPF_V2_1 = "xms_szgc_sj_jmtpf_v2_1";
		/**
		 * 厦门市_市政工程_设计15版_记名投票法_资格预审_后_v2
		 */
		String XMS_SZGC_SJ_JMTPF_V2_2 = "xms_szgc_sj_jmtpf_v2_2";
		/**
		 * 厦门市_市政工程_设计_综合评估法_2018版_v2
		 */
		String XMS_SZGC_SJ_ZHPGF_V2 = "xms_szgc_sj_zhpgf_v2";
		/**
		 * 厦门市_市政工程_设计_综合评估法_2018版_前_v2
		 */
		String XMS_SZGC_SJ_ZHPGF_V2_1 = "xms_szgc_sj_zhpgf_v2_1";
		/**
		 * 厦门市_市政工程_设计_综合评估法_2018版_后_v2
		 */
		String XMS_SZGC_SJ_ZHPGF_V2_2 = "xms_szgc_sj_zhpgf_v2_2";
		/**
		 * 厦门市_市政工程_园林绿化_经评审的最低投标价中标法B类_v1
		 */
		String XMS_SZGC_YLLH_JPSDZDTBJZBF_B_V1 = "xms_szgc_yllh_jpsdzdtbjzbf_B_v1";
		/**
		 * 厦门市_市政工程_园林绿化_简易评标法_v1
		 */
		String XMS_SZGC_YLLH_JYPBF_V1 = "xms_szgc_yllh_jypbf_v1";
		/**
		 * 厦门市_市政工程_通用版_v1
		 */
		String XMS_SZGC_TYB_V1 = "xms_szgc_tyb_v1";
		/**
		 * 厦门市_市政工程_通用版_资格预审_前_v1
		 */
		String XMS_SZGC_TYB_V1_1 = "xms_szgc_tyb_v1_1";
		/**
		 * 厦门市_市政工程_通用版_资格预审_后_v1
		 */
		String XMS_SZGC_TYB_V1_2 = "xms_szgc_tyb_v1_2";
		/**
		 * 厦门市_市政工程_监理_简易评标法_V2
		 */
		String XMS_SZGC_JL_JYPBF_V2 = "xms_szgc_jl_jypbf_v2";
		/**
		 * 厦门市_市政工程_监理_简易评标法_V2
		 */
		String XMS_SZGC_JL_ZHPGF_V2 = "xms_szgc_jl_zhpgf_v2";
		/**
		 * 厦门市_房屋建筑_集美小项目_V1
		 */
		String XMS_SZGC_JMXXM_TYB_V1 = "xms_szgc_jmxxm_tyb_v1";
		/**
		 * 厦门市_房屋建筑_集美小项目_V1
		 */
		String XMS_SZGC_TAXXM_TYB_V1 = "xms_szgc_taxxm_tyb_v1";

		/**
		 * 厦门市_房建市政_通用工具
		 */
		String XMS_FJSZ_COMMON = "xms_fjsz_common";
		/**
		 * 福建省_水运工程_通用工具
		 */
		String FJS_SYGC_COMMON = "fjs_sygc_common";
		/**
		 * 福建省_普通公路_通用工具
		 */
		String FJS_PTGL_COMMON = "fjs_ptgl_common";
		/**
		 * 福建省_高速公路_通用工具
		 */
		String FJS_GSGL_COMMON = "fjs_gsgl_common";

		/**
		 * 空
		 * 
		 * @return 空
		 */
		String toString();

	}

	/**
	 * 系统运行参数key<br/>
	 * <p>
	 * </p>
	 * Time：2016-11-1 上午11:41:38<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	public interface SysParamKey
	{

		/**
		 * 运维管理员ID
		 */
		String PM_ADMIN_ID = "aeolus.eokb.pm.admin.role.id";

		/**
		 * 区域编码
		 */
		String AREA_CODE_KEY = "aeolus.eokb.sync.otpd.area.code";

		/**
		 * 开标会议室编号
		 */
		String BID_ROOM_NO_KEY = "aeolus.eokb.sync.otpd.bid.room.no";

		// **********************************************************************
		/**
		 * 运维人员电话号码
		 */
		String MSG_SWITCH_KEY = "aeolus.message.switch";

		/**
		 * 运维人员电话号码
		 */
		String PERSON_PHONES_KEY = "aeolus.peration.maintenance.person.phones";

		/**
		 * 视频运维人员电话号码
		 */
		String VIDEO_PERSON_PHONES_KEY = "aeolus.video.peration.maintenance.person.phones";

		/**
		 * 解密时间前缀
		 */
		String DECRYPT_TIME_KEY_PREFIX = "aeolus.eokb.decrypt.time.";

		/**
		 * 开标文件保存的路径
		 */
		String EBIDKB_FILE_PATH_URL = "aeolus.ebidKB.file.path.url";
		/**
		 * 电子唱标speech的路径
		 */
		String EBIDKB_SING_PATH_URL = "aeolus.ebidKB.sing.path.url";
		/**
		 * 解密文件临时路径
		 */
		String EBIDKB_DECRYPTFILE_TEMP_PATH_URL = "aeolus.ebidKB.decryptFile.temp.path.url";
		/**
		 * 解密文件存放路径
		 */
		String EBIDKB_DECRYPTFILE_PATH_URL = "aeolus.ebidKB.decryptFile.path.url";

		/**
		 * 开标地点
		 */
		String EDE_BID_OPEN_ADDRESS = "aeolus.ede.bid.open.address";

		/**
		 * EDE数据来源用户账号
		 */
		String EDE_USER_ID_KEY = "aeolus.ede.user.id";

		/**
		 * EDE数据来源用户密钥
		 */
		String EDE_USER_PWD_KEY = "aeolus.ede.user.pwd";

		/**
		 * 开标列表获取地址
		 */
		String EDE_OPENID_LIST_URL_KEY = "aeolus.ede.openbid.list.${type}url";

		/**
		 * 获取项目对应的标段信息
		 */
		String EDE_SECTION_LIST_URL = "aeolus.ede.section.list.${type}url";
		
		/**
		 * 获取手机版对应的项目列表
		 */
		String EDE_MOBILE_BID_LIST = "aeolus.ede.mobile.bid.list.${type}url";

		/**
		 * 获取企业投标信息
		 */
		String EDE_ENTBID_INFO_URL = "aeolus.ede.entbid.info.${type}url";

		/**
		 * 获取企业投标数量
		 */
		String EDE_ENTBID_COUNT_URL = "aeolus.ede.entbid.count.${type}url";

		/**
		 * 企业投标文件存放地址
		 */
		String EDE_ENTBID_FILE_PATH = "aeolus.ede.entbid.file.path";

		/**
		 * 下载招标文件
		 */
		String EDE_DOWNLOAD_TENDER_FILE_URL = "aeolus.ede.download.tender.file.${type}url";

		/**
		 * 补遗文件列表获取
		 */
		String EDE_DOCU_QUES_FILE_LIST_URL = "aeolus.ede.docu.ques.list.${type}url";

		/**
		 * 补遗文件下载
		 */
		String EDE_DOWNLOAD_QUES_FILE_URL = "aeolus.ede.download.ques.file.${type}url";

		/**
		 * 控制价文件列表获取
		 */
		String EDE_PRICE_FILE_LIST_URL = "aeolus.ede.price.list.${type}url";

		/**
		 * 控制价文件下载
		 */
		String EDE_DOWNLOAD_PRICE_FILE_URL = "aeolus.ede.download.price.file.${type}url";

		/**
		 * 关联企业信息
		 */
		String EDE_ASSOCIATED_ENT_URL = "aeolus.ede.associate.ent.${type}url";

		/**
		 * 推送投标人信息接口
		 */
		String PUSH_BIDDERS_URL = "aeolus.ede.push.bidders.${type}url";

		/**
		 * 推送项目经理
		 */
		String PUSH_BIDDERS_LEADER = "aeolus.ede.push.bidders.leader";

		/**
		 * 推送开标视频信地址到交易平台
		 */
		String PUSH_VIDEO_URL = "aeolus.ede.push.video.url";

		/**
		 * 开标视频信地址
		 */
		String EOV_VIDEO_URL = "aeolus.eov.video.url";

		/**
		 * 开标视频信息
		 */
		String EOV_VIDEO_INFO = "aeolus.eov.video.info";
		/**
		 * 开标视频JSP地址
		 */
		String EOV_VIDEO_JSP = "aeolus.eov.video.url.jsp";
		/**
		 * CDN地址
		 */
		String CDN_VIDEO_URL = "aeolus.cdn.video.url";
		/**
		 * 视频录制地址
		 */
		String REC_VIDEO_URL = "aeolus.rec.video.url";
		/**
		 * 开标视频是否可用标识
		 */
		String EOV_VIDEO_STATUS = "aeolus.eov.video.url.status";
		/**
		 * 开标聊天是否可用标识
		 */
		String EOKB_IM_STATUS = "aeolus.eokb.im.status";

		/**
		 * 云视睿博聊天是否可用标识
		 */
		String EOKB_NTV_STATUS = "aeolus.eokb.ntv.status";

		/**
		 * 云视睿博通讯地址
		 */
		String EOKB_NTV_URL = "aeolus.eokb.ntv.url";

		/**
		 * 电子唱标是否可用标识
		 */
		String EOKB_SING_STATUS = "aeolus.eokb.sing.status";

		/**
		 * 结束按钮是否可以在结束开标后显示
		 */
		String EOKB_RE_END_STATUS = "aeolus.eokb.re.end.status";

		// -----------------------------------------------------------
		/**
		 * 服务器端获取用户基本信息
		 */
		String AS_CURRENT_USER_INFO_URL_KEY = "aeolus.oauth2.as.current.user.info.url";

		/**
		 * 服务器端获取证书基本信息
		 */
		String AS_CURRENT_CERT_INFO_URL_KEY = "aeolus.oauth2.as.current.cert.info.url";

		/**
		 * 使用当前软证书解密key
		 */
		String AS_CURRENT_CERT_DECRYPT_URL_KEY = "aeolus.oauth2.as.current.cert.decrypt.url";

		/**
		 * 使用领域证书解密key
		 */
		String AS_FIELD_CERT_DECRYPT_URL_KEY = "aeolus.oauth2.as.field.cert.decrypt.url";

		/**
		 * 获取三方证书列表
		 */
		String AS_GET_THREE_PARTY_CERT_URL_KEY = "aeolus.oauth2.as.three.party.cers.url";

		/**
		 * 验证证书有效性
		 */
		String AS_VALIDATE_FIELD_CERT_URL_KEY = "aeolus.oauth2.as.sozone.validate.url";

		/**
		 * 当前系统主页,即用户登录后重定向的页面
		 */
		String MAIN_PAGE_KEY = "aeolus.eobd.main.page";

		/**
		 * 用户登录后要跳转到的基础路径
		 */
		String MAIN_FORWARD_URL_KEY = "aeolus.eobd.main.forward.url";

		// --------------------------------------

		/**
		 * flash socket 安全策略文件服务端口
		 */
		String FLASH_SOCKET_SECURITY_POLICY_FILE_PORT = "aeolus.flash.socket.security.policy.file.port";

		/**
		 * 评标数据源ID 需要跟数据库名一致
		 */
		String PB_DB_ID_KEY = "sozone.ebidkb.pbdb.id";

		// --------------内网的云盾开放式授权登录参数------------------

		/**
		 * 客户端回调地址
		 */
		String REDIRECT_URI_KEY = "aeolus.oauth2.client.intranet.redirect.uri";

		/**
		 * 客户端ID
		 */
		String CLIENT_ID_KEY = "aeolus.oauth2.client.intranet.id";

		/**
		 * 客户端密钥
		 */
		String CLIENT_SECRET_KEY = "aeolus.oauth2.client.intranet.secret";

		/**
		 * 清标zip存放路径
		 */
		String SYSTEM_INSPECT_FILE_PATH = "aeolus.ede.inspect.zip.file.path";

		// --------------------------------------------------------
		/**
		 * 生成二维码地址
		 */
		String RS_GENERATE_QR_CODE_URL_KEY = "aeolus.oauth2.rs.generate.qr.code.url";

		/**
		 * 轮询二维码地址
		 */
		String RS_MONITOR_QR_CODE_URL_KEY = "aeolus.oauth2.rs.monitor.qr.code.url";

		// ----------------------------------------------------------
		// 手机

		/**
		 * 手机获取证书信息地址
		 */
		String MOBILE_GET_CERT_INFO_URL_KEY = "aeolus.oauth2.mobile.get.cert.info.url";

		/**
		 * 手机获取证书对应的用户信息地址
		 */
		String MOBILE_GET_USER_INFO_URL_KEY = "aeolus.oauth2.mobile.get.user.info.url";

		/**
		 * 空
		 * 
		 * @return 空
		 */
		String toString();
	}

	/**
	 * 表名常量定义<br/>
	 * <p>
	 * 表名常量定义<br/>
	 * </p>
	 * Time：2016-5-10 下午3:10:12<br/>
	 * 
	 * @author zouye
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	public interface TableName
	{
		// --------------------流程-------------------
		/**
		 * 开标流程信息
		 */
		String FLOW_INFO = "EKB_T_FLOW_INFO";

		/**
		 * 开标流程节点信息
		 */
		String FLOW_NODE_INFO = "EKB_T_FLOW_NODE_INFO";

		/**
		 * 招标项目流程节点
		 */
		String TENDER_PROJECT_NODE = "EKB_T_TENDER_PROJECT_FLOW_NODE_INFO";

		/**
		 * 招标项目流程视图
		 */
		String TENDER_PROJECT_FLOW_VIEW = "EKB_V_TENDER_PROJECT_FLOW_INFO";

		/**
		 * 招标项目开标流程节点状态时间表
		 */
		String TENDER_PROJECT_NODE_STATUS_TIME = "EKB_T_TPFN_STATUS_TIME";

		/**
		 * 招标项目流程节点数据表
		 */
		String TENDER_PROJECT_NODE_DATA = "EKB_T_TPFN_DATA_INFO";

		// --------------------开放式授权--------------
		/**
		 * 用户证书信息视图
		 */
		String CERT_CA_USER_VIEW = "V_CERT_USER_INFO";

		/**
		 * 证书表
		 */
		String CERT_INFO = "T_CERT_INFO";

		/**
		 * CA用户信息表
		 */
		String CA_USER_INFO = "T_CA_USER_INFO";

		/**
		 * 解密临时表
		 */
		String DECRYPT_TEMP = "EKB_T_DECRYPT_TEMP";

		/**
		 * 解压记录表
		 */
		String UNPACK_RECORD = "EKB_T_BIDDER_DOC_UNPACK_RECORD";

		// -----------------------------------------
		/** 通用表 ************/
		/**
		 * 招标项目信息表
		 */
		String EKB_T_TENDER_PROJECT_INFO = "EKB_T_TENDER_PROJECT_INFO";
		/**
		 * 投标人信息表
		 */
		String EKB_T_TENDER_LIST = "EKB_T_TENDER_LIST";
		/**
		 * 标段信息表
		 */
		String EKB_T_SECTION_INFO = "EKB_T_SECTION_INFO";
		/**
		 * 签到表
		 */
		String EKB_T_SIGN_IN = "EKB_T_SIGN_IN";
		/**
		 * 解密表
		 */
		String EKB_T_DECRYPT_INFO = "EKB_T_DECRYPT_INFO";
		/**
		 * 解密表（日志）
		 */
		String EKB_T_DECRYPT_INFO_LOG = "EKB_T_DECRYPT_INFO_LOG";
		/**
		 * 投标文件信息表
		 */
		String EKB_T_TBIMPORTBIDDING = "EKB_T_TBIMPORTBIDDING";
		/**
		 * 检查模块表
		 */
		String EKB_T_CHECK_MODEL = "EKB_T_CHECK_MODEL";
		/**
		 * 数据确认表
		 */
		String EKB_T_CHECK_DATA = "EKB_T_CHECK_DATA";
		/**
		 * 关联企业表
		 */
		String EKB_T_CORRELATE_ENTER = "EKB_T_CORRELATE_ENTER";
		/**
		 * 投标人联系方式表
		 */
		String EKB_T_BIDDER_PHONE = "EKB_T_BIDDER_PHONE";

		/** 福建省_高速公路_路基施工_合理低价法 ************/
		/**
		 * 投标报价（有效报价）通过汇总表
		 */
		String EKB_T_FJS_GSGL_LJSG_HLDJF_VALID_OFFER = "EKB_T_FJS_GSGL_LJSG_HLDJF_VALID_OFFER";
		/**
		 * 评标基准价计算记录表
		 */
		String EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION = "EKB_T_FJS_GSGL_LJSG_HLDJF_EVALUATION";
		/**
		 * 系统公告表
		 */
		String EKB_T_NOTICE = "EKB_T_NOTICE";

		/**
		 * 开标异议表
		 */
		String EKB_T_DISSENT = "EKB_T_DISSENT";

		/**
		 * 开标操作日志表
		 */
		String EKB_T_OPERATION_LOG = "EKB_T_OPERATION_LOG";

		/**
		 * 企业信用分
		 */
		String COMPANY_CREDIT_SCORE = "EKB_T_COMPANY_CREDIT_SCORE_INFO";

		/**
		 * 行业平均信用分
		 */
		String INDUSTRY_AVG_CREDIT_SCORE = "EKB_T_INDUSTRY_AVG_CREDIT_SCORE_INFO";
		/**
		 * 电子摇号结果
		 */
		String EKB_T_ELECTRONICS = "EKB_T_ELECTRONICS";
		/**
		 * 投标人球号标
		 */
		String EKB_T_BIDDER_NO = "EKB_T_BIDDER_NO";
		/**
		 * 云视睿博即时聊天信息表
		 */
		String EKB_T_NTV_MESSAG_MSG = "EKB_T_NTV_MESSAG_MSG";

		/**
		 * 开标室信息表
		 */
		String EKB_T_VIDEO_INFO = "EKB_T_VIDEO_INFO";

		/**
		 * 流程节点表
		 */
		String EKB_T_TENDER_PROJECT_FLOW_NODE_INFO = "EKB_T_TENDER_PROJECT_FLOW_NODE_INFO";

		/**
		 * 流程节点数据表
		 */
		String EKB_T_TPFN_DATA_INFO = "EKB_T_TPFN_DATA_INFO";

		// -------操作日志
		/**
		 * 操作日志表
		 */
		String OPERATION_LOG = "T_OPERATION_LOG";

		// -----------------------------------------------------
		/**
		 * 最新解密临时信息表
		 */
		String DECRYPT_TEMP_DATA = "EKB_T_BDDU_RECORD";

		/**
		 * 解密解压状态视图
		 */
		String DECRYPT_UNPACK_STATUS_VIEW = "EKB_V_DECRYPT_STATUS";

		/**
		 * 字典表
		 */
		String T_SYS_DICT = "T_SYS_DICT";

		/**
		 * 空
		 * 
		 * @return 空
		 */
		String toString();
	}

	/**
	 * 空
	 * 
	 * @return 空
	 */
	String toString();

	/**
	 * 
	 * 招标项目的类型定义<br/>
	 * <p>
	 * 招标项目的类型定义<br/>
	 * </p>
	 * Time：2017-9-20 下午2:29:09<br/>
	 * 
	 * @author wanghw
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	public interface TENDERPROJECT_APP_TYPE
	{
		/**
		 * 高速公路
		 */
		String FJS_GSGL_TYPE = "10";

		/**
		 * 普通公路
		 */
		String FJS_PTGL_TYPE = "20";

		/**
		 * 港航水运
		 */
		String FJS_SYGC_TYPE = "30";

		/**
		 * 厦门房屋建筑
		 */
		String XMS_FWJZ_TYPE = "A01";

		/**
		 * 厦门市政工程
		 */
		String XMS_SZGC_TYPE = "A02";

		/**
		 * 空
		 * 
		 * @return 空
		 */
		String toString();
	}

	/**
	 * 
	 * 解密状态定义<br/>
	 * <p>
	 * 解密状态定义<br/>
	 * </p>
	 * Time：2017-9-23 上午9:54:54<br/>
	 * 
	 * @author wanghw
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	public interface DECRYPT_STATUS
	{
		/**
		 * 解密成功
		 */
		int DECRYPT_SUCCESS = 1;
		/**
		 * 解密失败
		 */
		int DECRYPT_FALSE = 2;

		/**
		 * 未解密
		 */
		int DECRYPT_NOT = 0;

		/**
		 * 空
		 * 
		 * @return 空
		 */
		String toString();
	}
}
