package com.eden.seckill.queue.activemq;

import java.io.UnsupportedEncodingException;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.eden.seckill.common.entity.Result;
import com.eden.seckill.common.enums.SeckillStatEnum;
import com.eden.seckill.common.redis.RedisUtil;
import com.eden.seckill.common.webSocket.WebSocketServer;
import com.eden.seckill.service.ISeckillService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Consumer {
	/**
	 * 消费者实体对象
	 */
	private DefaultMQPushConsumer consumer;
	/**
	 * 消费者组
	 */
	public static final String CONSUMER_GROUP = "test_consumer";
	/**
	 * 通过构造函数 实例化对象
	 */
	@Autowired
	private ISeckillService seckillService;
	@Autowired
	private RedisUtil redisUtil;

	public Consumer() {

		consumer = new DefaultMQPushConsumer(CONSUMER_GROUP);
		consumer.setNamesrvAddr(RocketMqConfig.NAME_SERVER);
		// 消费模式:一个新的订阅组第一次启动从队列的最后位置开始消费 后续再启动接着上次消费的进度开始消费
		consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
		// 订阅主题和 标签（ * 代表所有标签)下信息
		try {
			try {
				consumer.subscribe("seckillorder", "*");
			} catch (MQClientException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				log.info("subscribe消费报错，错误为："+e1.getMessage());
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			log.info("subscribe消费报错，错误为："+e1.getMessage());
		}
		// //注册消费的监听 并在此监听中消费信息，并返回消费的状态信息
		consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
			// msgs中只收集同一个topic，同一个tag，并且key相同的message
			// 会把不同的消息分别放置到不同的队列中
			try {
				for (Message msg : msgs) {
					// 消费者获取消息 这里只输出 不做后面逻辑处理
					String body = new String(msg.getBody(), "utf-8");
					/**
					 * 收到通道的消息之后执行秒杀操作(超卖)
					 */
					String[] array = body.split(";");
					Result result = seckillService.startSeckilDBPCC_TWO(Long.parseLong(array[0]),
							Long.parseLong(array[1]));
					if (result.equals(Result.ok(SeckillStatEnum.SUCCESS))) {
						WebSocketServer.sendInfo(array[0], "秒杀成功");
					} else {
						WebSocketServer.sendInfo(array[0], "秒杀失败");
						redisUtil.cacheValue(array[0], "ok");
					}
					log.info("Consumer-获取消息-主题topic为={}, 消费消息为={}", msg.getTopic(), body);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		});
		try {
			consumer.start();
		} catch (MQClientException e) {
			// TODO Auto-generated catch block
			log.info("start消费报错，错误为："+e.getMessage());
		}
		System.out.println("消费者 启动成功=======");
	}
}
