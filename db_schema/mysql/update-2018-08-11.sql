/*修改Mysql事务隔离级别**/
SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

#ekb_t_bddu_record 
ALTER TABLE ekb_t_bddu_record ENGINE=InnoDB; 
#ekb_t_bidder_doc_unpack_record 
ALTER TABLE ekb_t_bidder_doc_unpack_record ENGINE=InnoDB; 
#ekb_t_bidder_no 
ALTER TABLE ekb_t_bidder_no ENGINE=InnoDB; 
#ekb_t_bidder_phone 
ALTER TABLE ekb_t_bidder_phone ENGINE=InnoDB; 
#ekb_t_check_data 
ALTER TABLE ekb_t_check_data ENGINE=InnoDB; 
#ekb_t_check_model 
ALTER TABLE ekb_t_check_model ENGINE=InnoDB; 
#ekb_t_company_credit_score_info 
ALTER TABLE ekb_t_company_credit_score_info ENGINE=InnoDB; 
#ekb_t_correlate_enter 
ALTER TABLE ekb_t_correlate_enter ENGINE=InnoDB; 
#ekb_t_decrypt_info 
ALTER TABLE ekb_t_decrypt_info ENGINE=InnoDB; 
#ekb_t_decrypt_temp 
ALTER TABLE ekb_t_decrypt_temp ENGINE=InnoDB; 
#ekb_t_dissent 
ALTER TABLE ekb_t_dissent ENGINE=InnoDB; 
#ekb_t_electronics 
ALTER TABLE ekb_t_electronics ENGINE=InnoDB; 
#ekb_t_fjs_gsgl_ljsg_hldjf_evaluation 
ALTER TABLE ekb_t_fjs_gsgl_ljsg_hldjf_evaluation ENGINE=InnoDB; 
#ekb_t_fjs_gsgl_ljsg_hldjf_valid_offer 
ALTER TABLE ekb_t_fjs_gsgl_ljsg_hldjf_valid_offer ENGINE=InnoDB; 
#ekb_t_flow_info 
ALTER TABLE ekb_t_flow_info ENGINE=InnoDB; 
#ekb_t_flow_node_info 
ALTER TABLE ekb_t_flow_node_info ENGINE=InnoDB; 
#ekb_t_im_msg 
ALTER TABLE ekb_t_im_msg ENGINE=InnoDB; 
#ekb_t_im_room 
ALTER TABLE ekb_t_im_room ENGINE=InnoDB; 
#ekb_t_industry_avg_credit_score_info 
ALTER TABLE ekb_t_industry_avg_credit_score_info ENGINE=InnoDB; 
#ekb_t_notice 
ALTER TABLE ekb_t_notice ENGINE=InnoDB; 
#ekb_t_ntv_messag_msg 
ALTER TABLE ekb_t_ntv_messag_msg ENGINE=InnoDB;  
#ekb_t_section_info 
ALTER TABLE ekb_t_section_info ENGINE=InnoDB; 
#ekb_t_sign_in 
ALTER TABLE ekb_t_sign_in ENGINE=InnoDB; 
#ekb_t_tbimportbidding 
ALTER TABLE ekb_t_tbimportbidding ENGINE=InnoDB; 
#ekb_t_tender_list 
ALTER TABLE ekb_t_tender_list ENGINE=InnoDB; 
#ekb_t_tender_project_flow_node_info 
ALTER TABLE ekb_t_tender_project_flow_node_info ENGINE=InnoDB; 
#ekb_t_tender_project_info 
ALTER TABLE ekb_t_tender_project_info ENGINE=InnoDB; 
#ekb_t_tpfn_data_info 
ALTER TABLE ekb_t_tpfn_data_info ENGINE=InnoDB; 
#ekb_t_tpfn_status_time 
ALTER TABLE ekb_t_tpfn_status_time ENGINE=InnoDB; 
#ekb_t_video_info 
ALTER TABLE ekb_t_video_info ENGINE=InnoDB; 
#t_attach_file_info 
ALTER TABLE t_attach_file_info ENGINE=InnoDB; 
#t_attach_file_reference 
ALTER TABLE t_attach_file_reference ENGINE=InnoDB; 
#t_attach_type_info 
ALTER TABLE t_attach_type_info ENGINE=InnoDB; 
#t_attach_type_role 
ALTER TABLE t_attach_type_role ENGINE=InnoDB; 
#t_ca_user_info 
ALTER TABLE t_ca_user_info ENGINE=InnoDB; 
#t_cert_info 
ALTER TABLE t_cert_info ENGINE=InnoDB; 
#t_operation_log 
ALTER TABLE t_operation_log ENGINE=InnoDB; 
#t_sys_dict 
ALTER TABLE t_sys_dict ENGINE=InnoDB; 
#t_sys_func_info 
ALTER TABLE t_sys_func_info ENGINE=InnoDB; 
#t_sys_param 
ALTER TABLE t_sys_param ENGINE=InnoDB; 
#t_sys_resource_info 
ALTER TABLE t_sys_resource_info ENGINE=InnoDB; 
#t_sys_role_func 
ALTER TABLE t_sys_role_func ENGINE=InnoDB; 
#t_sys_role_info 
ALTER TABLE t_sys_role_info ENGINE=InnoDB; 
#t_sys_role_resource 
ALTER TABLE t_sys_role_resource ENGINE=InnoDB; 
#t_sys_user_base 
ALTER TABLE t_sys_user_base ENGINE=InnoDB; 
#t_sys_user_func 
ALTER TABLE t_sys_user_func ENGINE=InnoDB; 
#t_sys_user_resource 
ALTER TABLE t_sys_user_resource ENGINE=InnoDB; 
#t_sys_user_role 
ALTER TABLE t_sys_user_role ENGINE=InnoDB; 

