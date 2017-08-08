package com.xyz.mosq.proxy.tester.start;

import com.xyz.service.mosqproxy.client.MosqProxyClient;
import com.xyz.service.pojo.RegResult;

public class Starter
{
	//private static final String FILE_PATH = "conf/mosqProxy_tester.conf";
	private static String serverHost = "11.12.112.201";
	private static int serverPort = 5812;
	public static void main(String[] args)
	{
		MosqProxyClient mosqProxyClient = new MosqProxyClient();
		if(!mosqProxyClient.init(serverHost, serverPort)){
			System.exit(0);
		}
		String appId = "atp";
		String userId = "user-001";
		String devId = "android";
		RegResult regRes= mosqProxyClient.registerConnection(appId, userId, devId);
		System.out.println("res:" + regRes.getValue());

	}

}
