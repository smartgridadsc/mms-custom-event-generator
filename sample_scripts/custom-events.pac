refine flow MMS_Flow += {
  function rule_function():bool
  %{
    string domainID_itemID_from_csv_line_2 = "WAGO61850ServerLogicalDevice_GGIO17$CO$SPCSO2$Oper";
    if(concatenated_domain_item_id==domainID_itemID_from_csv_line_2 && is_request && this->connection()->upflow()->current_pdu_data_pair_vector.size() != 0){
      bool value = (this->connection()->upflow()->current_pdu_data_pair_vector[0].first == "true") ? true : false;
      BifEvent::generate_SCADA_Q2C_Sync_Activated(connection()->bro_analyzer(),
                                      connection()->bro_analyzer()->Conn(),
                                      invoke_id,
                                      this->connection()->upflow()->service,
                                      value
                                      );
    }

    string domainID_itemID_from_csv_line_3 = "MIED2PROT_LLN0$Measurement";
    if(concatenated_domain_item_id==domainID_itemID_from_csv_line_3 && !is_request && this->connection()->upflow()->current_pdu_data_pair_vector.size() != 0){
      double value = std::stod(this->connection()->upflow()->current_pdu_data_pair_vector[15].first);
      BifEvent::generate_MIED2_Phase_Angle(connection()->bro_analyzer(),
                                      connection()->bro_analyzer()->Conn(),
                                      invoke_id,
                                      this->connection()->upflow()->service,
                                      value
                                      );
    }

    string domainID_itemID_from_csv_line_4 = "MIED2CTRL_V16GGIO1$ST$Ind3$stVal";
    if(concatenated_domain_item_id==domainID_itemID_from_csv_line_4 && !is_request && this->connection()->upflow()->current_pdu_data_pair_vector.size() != 0){
      bool value = (this->connection()->upflow()->current_pdu_data_pair_vector[0].first == "true") ? true : false;
      BifEvent::generate_Q2C_In_Sync(connection()->bro_analyzer(),
                                      connection()->bro_analyzer()->Conn(),
                                      invoke_id,
                                      this->connection()->upflow()->service,
                                      value
                                      );
    }


    return false;
  %}
}
