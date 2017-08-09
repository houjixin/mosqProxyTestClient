package com.xyz.mosq.proxy.tester.start;

import com.xyz.service.data.ConnectionInfo;
import com.xyz.service.data.RegResult;
import com.xyz.service.mosqproxy.client.MosqProxyClient;

public class Starter
{
	//private static final String FILE_PATH = "conf/mosqProxy_tester.conf";
	private static String serverHost = "11.12.112.201";
	private static int serverPort = 6888;
	public static void main(String[] args)
	{
		MosqProxyClient mosqProxyClient = new MosqProxyClient();
		if(!mosqProxyClient.init(serverHost, serverPort)){
			System.exit(0);
		}
		String appId = "xyz_tester";
		String userId = "user-001";
		String devId = "android-0098";
		long logIndex= 9901l;
		String caller = "mosq-proxy-client-tester";
		//------获取一个连接
		ConnectionInfo conInfo = mosqProxyClient.getConnection(logIndex, caller);
		if(conInfo == null){
			System.out.println("conInfo:=null");
			System.exit(0);
		}
		System.out.println("conInfo:" + conInfo.toOutSimpleString());

		//绑定连接
		logIndex = 9902l;
		String bindRes = mosqProxyClient.bindConnId(logIndex, caller, appId, userId, conInfo.getConnId(), devId, false);
		System.out.println("bindRes:" + bindRes);
	}

}
