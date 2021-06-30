global enable_check = F;
global sync_complete = F;
global prev_value:double = -1;
global threshold_lo:double = 3.0;
global threshold_hi:double = 5.0;

event write_request(c: connection, invoke_id: count, identifier: string, data: string_vec, datatype: index_vec)
{
	if (identifier == "WAGO61850ServerLogicalDevice_GGIO17$CO$SPCSO2$Oper") 
	{
		if (data[0] == "true")
		{
			print fmt("SCADA_Q2C_Sync_Activated, invokeId:%s, result:%s, network time:%s", invoke_id, data[0] ,network_time());
			enable_check = T;
		}
	}
}

event read_response(c: connection, invoke_id: count, identifier: string, data: string_vec, datatype: index_vec)
{
	if (identifier == "MIED2CTRL_V16GGIO1$ST$Ind3$stVal")
	{
		if (data[0] == "true")
		{
			print fmt("q2c_in_sync, invokeId:%s, result:%s, network time:%s", invoke_id, data[0] ,network_time());
			sync_complete = T;
		}
	}
	if (identifier == "MIED2PROT_LLN0$Measurement")
	{
		if (enable_check == T && sync_complete == F)
		{
			if (prev_value == -1)
			{
				prev_value = to_double(data[15]);
			}
			else
			{
				local diff = |to_double(data[15]) - prev_value|;
				if (diff < threshold_lo || diff > threshold_hi)
				{
					print fmt("Timestamp: %s, [Alert!!!] diff: %s, invoke_id: %s", current_time(), diff, invoke_id);
				}
				prev_value = to_double(data[15]);
			}
		}
	}
}
