global enable_check = F;
global sync_complete = F;
global prev_value:double = -1;
global threshold_lo:double = 4.0;
global threshold_hi:double = 5.0;

event SCADA_Q2C_Sync_Activated(c: connection, invoke_id: int, service: int, value: bool)
{
	if (value == T)
	{
		print fmt("SCADA_Q2C_Sync_Activated, invokeId:%s, result:%s, network time:%s, service:%s", invoke_id, value ,network_time(), service);
		enable_check = T;
	}
}

event MIED2_Phase_Angle(c: connection, invoke_id:int , service: int, value: double)
{
	if (enable_check == T && sync_complete == F)
	{
		if (prev_value == -1)
		{
			prev_value = value;
		}
		else
		{
			local diff = |value - prev_value|;
			if (diff < threshold_lo || diff > threshold_hi)
			{
				print fmt("Timestamp: %s, [Alert!!!] diff: %s, invoke_id: %s", current_time(), diff, invoke_id);
			}
			prev_value = value;
		}
	}
}

event Q2C_In_Sync(c: connection, invoke_id:int, service:int, value: bool)
{	
	if (value == T)
	{
		sync_complete = T;
		print fmt("Q2C_In_Sync, invokeId:%s, result:%s, network time:%s, service:%s", invoke_id, value ,network_time(), service);
	}
}

