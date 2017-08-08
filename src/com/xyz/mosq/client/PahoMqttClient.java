package com.xyz.mosq.client;


import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xyz.mosq.proxy.tester.common.BasicException;
import com.xyz.mosq.proxy.tester.common.DefaultValues;
import com.xyz.mosq.proxy.tester.pojo.TopicAndMqttMessage;
import com.xyz.service.common.Tools;

public class PahoMqttClient implements MqttCallback
{
	public String getClassName() {return "PahoMqttClient";}
	private static Logger logger = LoggerFactory.getLogger(PahoMqttClient.class);
	private final String PROTOCOL = "tcp://";
//	private static final String PROTOCOL = "ssl://";
	private final int KEEP_ALIVE_INTERVAL = 60;
	private String userId;
	private boolean CLEAN_SESSION = true;
	private boolean isWorking = false;
	private MqttClient mqttClient;

	private String brokerUrl;

	private String password;
	
	private String userName;

	private MqttConnectOptions 	conOpt;
	
	private String[] topicFilters;
	

	private int[] qos;
	
	private boolean terminateMe = false;

	private int reconInterval = DefaultValues.RECONNECT_INTERVAL;
	
	public boolean isWorking() 
	{
		return isWorking;
	}
	public void setReconInterval(long logIndex, int ti)
	{
		reconInterval = ti;
	}
	

	private MqttPublishThread m_publishThread;
	

	
	public String getClientId()
	{
		return userId;
	}
	
	/**
	 * 函数名称：init 函数功能：初始化MqttClient对象
	 * 
	 * @author houjixin
	 * @param logIndex 日志索引
	 * @param flag 当前mqtt client的标�?
	 * @param host mqttClient将要连接的mosquitto的主机地�?
	 * @param port mqttClient将要连接的mosquitto的监听端�?
	 * @param clientId 当前的mqttClient的ID
	 * @param keepALive 当前的mqttClient的keepalive
	 * @param workTo 当前的mqttClient的ID的工作超时时�?
	 * @return boolean 初始化成功返回true，初始化失败返回false
	 * */
	public boolean init(long logIndex, String flag, String host, int port, String clientId, int keepALive, int workTo, int initTo)
	{
		String logFlag = getClassName() + ".init";
		this.userName = DefaultValues.MOSQ_USR;
		this.password = DefaultValues.MOSQ_PWD;
		userId = clientId;
		
		brokerUrl = PROTOCOL + host + ":" + port;
		
		MqttDefaultFilePersistence dataStore = null;
		String persistanceDir = makePersistanceDir();
		if (persistanceDir != null)
			dataStore = new MqttDefaultFilePersistence();

		conOpt = new MqttConnectOptions();
		conOpt.setCleanSession(CLEAN_SESSION);
		conOpt.setUserName(this.userName);
		conOpt.setPassword(this.password.toCharArray());
		conOpt.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
		
		try
		{
			mqttClient = new MqttClient(brokerUrl, userId, dataStore);
		}
		catch (MqttException e)
		{
			logger.warn("[lid:{}][{}] exception happened! detail:{}", logIndex, logFlag, e);
			return false;
		}
		mqttClient.setCallback(this);

		m_publishThread = new MqttPublishThread(userId);
		m_publishThread.start();
		return true;
	}

	public boolean startUp(long logIndex) throws Exception
	{
		String logFlag = "PahoMqttClient.startUp";
		terminateMe = false;
		mqttClient.connect(conOpt);

		if (topicFilters != null && topicFilters.length != 0)
			mqttClient.subscribe(topicFilters, qos);
		isWorking = true;
		logger.info("[lid:{}][{}] client:{} connect {} suc!", logIndex, logFlag, userId, brokerUrl);
		return true;
	}
	

	public void publishAsyn(String topicName, int qos, byte[] payload, boolean retain)
	{
		MqttMessage message = null;
		if (payload != null) message = new MqttMessage(payload);
		else message = new MqttMessage();
		message.setQos(qos);
		message.setRetained(retain);
		
		TopicAndMqttMessage tam = new TopicAndMqttMessage(topicName, message);
		m_publishThread.scheduleToPublish(tam);
	}
	

	public void publishAsyn(String topicName, int qos, byte[] payload)
	{
		publishAsyn(topicName, qos, payload, false);
	}

	/**
	 * 向指定topic发�?�?��消息
	 * 
	 * @param logIndex 调用索引
	 * @param topic
	 * @param payload 消息的内�?
	 * @return int 0为成功，-1为失�?
	 * */
	public int pub(long logIndex, String topic, String payload)
	{
		publishAsyn(topic, DefaultValues.MQTT_DEFAULT_QOS, payload.getBytes(), false);
		return 0;
	}
	
	public void publishAsyn(long logIndex, String topicName, int qos, String payload)
	{
		publishAsyn(topicName, qos, payload.getBytes(), false);
	}
	public void shutDown(long logIndex)
	{
		String logFlag = "PahoMqttClient.shutdown";
		try
		{
			if (mqttClient != null)
			{
				terminateMe = true;
				if (mqttClient.isConnected())
					mqttClient.disconnect();
				m_publishThread.stopThread();
				mqttClient = null;
			}
			isWorking = true;
		}
		catch (MqttException ex)
		{
			logger.warn("[lid:{}][{}] user: {},Disconnect [{}] fail! detail:{}", logIndex, logFlag, userId, brokerUrl, ex);
		}
	}
	

	public boolean isConnected(long logIndex)
	{
		return (mqttClient != null) && (mqttClient.isConnected());
	}
	
	
	public boolean disConnected(long logIndex)
	{
		String logFlag = "PahoMqttClient.disConnected";
		if(mqttClient == null)
		{
			return true;
		}
		try
		{
			mqttClient.disconnect();
			isWorking = false;
		}
		catch (MqttException ex)
		{
			logger.warn("[lid:{}][{}] exceptiong happened!user: {},Disconnect [{}] fail! detail:{}", logIndex, logFlag, userId, brokerUrl, ex);
			return false;
		}
		
		return true;
	}
	
	
	public void unsubscribe(long logIndex, String topic)
	{
		String logFlag = "PahoMqttClient.unsubscribe";
		try
		{
			mqttClient.unsubscribe(topic);
		}
		catch (MqttException ex)
		{
			logger.warn("[lid:{}][{}] exceptiong happened!user: {},Disconnect [{}] fail! detail:{}", logIndex, logFlag, userId, brokerUrl, ex);
		}
	}

	public int getQueueSize()
	{
		return this.m_publishThread.getQueueSize();
	}
	

	private String makePersistanceDir()
	{
		return null;
	}

	


	@Override
	public void deliveryComplete(IMqttDeliveryToken token)
	{

//		if (m_logger.isTraceEnabled())
//			m_logger.trace("deliveryComplete: " + token.getMessageId());
	}


	

	class MqttPublishThread extends Thread
	{
		private ConcurrentLinkedQueue<TopicAndMqttMessage> msgQueue = new ConcurrentLinkedQueue<TopicAndMqttMessage>();
		

		private Object m_notifyObject = new Object();
		private String userId;

		private boolean m_userStop = false;
		
		public MqttPublishThread(String userId)
		{
			setName(userId);
			this.userId = userId;
		}
		

		public void stopThread()
		{
			m_userStop = true;
			synchronized (m_notifyObject)
			{
				m_notifyObject.notifyAll();
			}
		}
		

		public void scheduleToPublish(TopicAndMqttMessage tam)
		{
			msgQueue.add(tam);
			synchronized (m_notifyObject)
			{
				m_notifyObject.notifyAll();
			}
		}

		public int getQueueSize()
		{
			return msgQueue.size();
		}
		
		@Override
		public void run()
		{
			long logIndex = Tools.getSerial();
			String logFlag = "MqttPublishThread.run";
			while (!m_userStop)
			{
				TopicAndMqttMessage tam = msgQueue.poll();
				if (tam != null)
				{
					try
					{
						if (isConnected(logIndex))
							mqttClient.publish(tam.getTopic(), tam.getMqttMessage());
						else
							logger.warn("[lid:{}][{}] userid:{}, Reconnect MQTT:{} failed!", logIndex, logFlag, userId, brokerUrl);
					}
					catch (Exception ex)
					{
						logger.warn("[lid:{}][{}] exception happened! detail:{}", logIndex, logFlag, ex);
					}
				}
				else
				{
					synchronized (m_notifyObject)
					{
						try
						{
							m_notifyObject.wait(10000);
						}
						catch (InterruptedException e)
						{
							logger.warn("[lid:{}][{}] exception happened! detail:{}", logIndex, logFlag, e);
						}
					}
				}
			}
		}

	}
	
	@Override
	public void connectionLost(Throwable cause)
	{
		if (terminateMe)
			return;
		boolean success = false;
		int reconnectCount = 0;
		isWorking = false;
		long logIndex = Tools.getSerial();
		String logFlag = "PahoMqttClient.connectionLost";
		try
		{
			while ((!terminateMe) && (!success) && (!isConnected()))
			{
				reconnectCount++;
				try
				{
					success = startUp(logIndex);
					if (success)
					{
						logger.debug("[lid:{}][{}] userid: {}, Recon Mosq[{}] suc! retry={}", logIndex, logFlag, userId, brokerUrl, reconnectCount);
						break;
					}
					if (reconnectCount == 1)
					{
						logger.warn("[lid:{}][{}] userid: {}, Recon Mosq[{}] fail! retry={}", logIndex, logFlag, userId, brokerUrl, reconnectCount);
					}
				}
				catch (Exception e)
				{
					if (reconnectCount == 1)
					{
						logger.warn("[lid:{}][{}] userid: {}, Recon Mosq[{}] fail! retry={}", logIndex, logFlag, userId, brokerUrl, reconnectCount);
					}
				}
				Tools.sleep(logIndex, reconInterval);
			}

		}
		catch (Exception ex)
		{
			logger.warn("[lid:{}][{}] exception happened! userid: {}, Reconnect MQTT[{}] failed! retry={}, detail:{}", logIndex, logFlag, userId, brokerUrl,
					reconnectCount, ex);
			return;
		}
	}

	public boolean isConnected()
	{
		return mqttClient.isConnected();
	}

	@Override
	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		String logFlag = "PahoMqttClient.messageArrived";
		long logIndex = Tools.getSerial();
		try
		{
			logger.debug("[lid:{}][{}] recv msg! client id:{}; topic={}, msg:{}", logIndex, logFlag, userId, topic, message.toString());
				
		}
		catch (Exception ex)
		{
			logger.error("[lid:{}][{}] userid: {} Process message fail! TOPIC=[{}], MSG=[{}], detail:{}", logIndex, logFlag, userId, topic, message.toString(), BasicException.getStackTrace(ex));
			return;
		}
	}

}
