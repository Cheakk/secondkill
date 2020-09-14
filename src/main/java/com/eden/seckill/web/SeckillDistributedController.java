package com.eden.seckill.web;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eden.seckill.common.entity.Result;
import com.eden.seckill.common.redis.RedisUtil;
import com.eden.seckill.queue.activemq.Producer;
import com.eden.seckill.queue.activemq.RocketMqConfig;
import com.eden.seckill.queue.redis.RedisSender;
import com.eden.seckill.service.ISeckillDistributedService;
import com.eden.seckill.service.ISeckillService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
@Api(tags ="分布式秒杀")
@RestController
@RequestMapping("/seckillDistributed")
@Slf4j
public class SeckillDistributedController {
	private static int corePoolSize = Runtime.getRuntime().availableProcessors();
	//调整队列数 拒绝服务
	private static ThreadPoolExecutor executor  = new ThreadPoolExecutor(corePoolSize, corePoolSize+1, 10l, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(10000));
	
	@Autowired
	private ISeckillService seckillService;
	@Autowired
	private ISeckillDistributedService seckillDistributedService;
	@Autowired
	private RedisSender redisSender;
	
	@Autowired
	private Producer producer;

	@Autowired
	private RedisUtil redisUtil;
	
	@ApiOperation(value="秒杀一(Rediss分布式锁)",nickname="科帮网")
	@PostMapping("/startRedisLock")
	public Result startRedisLock(long seckillId){
		seckillService.deleteSeckill(seckillId);
		final long killId =  seckillId;
		log.info("开始秒杀一");
		for(int i=0;i<1000;i++){
			final long userId = i;
			Runnable task = new Runnable() {
				@Override
				public void run() {
					Result result = seckillDistributedService.startSeckilRedisLock(killId, userId);
					log.info("用户:{}{}",userId,result.get("msg"));
				}
			};
			executor.execute(task);
		}
		try {
			Thread.sleep(15000);
			Long  seckillCount = seckillService.getSeckillCount(seckillId);
			log.info("一共秒杀出{}件商品",seckillCount);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return Result.ok();
	}
	@ApiOperation(value="秒杀二(zookeeper分布式锁)",nickname="科帮网")
	@PostMapping("/startZkLock")
	public Result startZkLock(long seckillId){
		seckillService.deleteSeckill(seckillId);
		final long killId =  seckillId;
		log.info("开始秒杀二");
		for(int i=0;i<10000;i++){
			final long userId = i;
			Runnable task = new Runnable() {
				@Override
				public void run() {
					Result result = seckillDistributedService.startSeckilZksLock(killId, userId);
					log.info("用户:{}{}",userId,result.get("msg"));
				}
			};
			executor.execute(task);
		}
		try {
			Thread.sleep(10000);
			Long  seckillCount = seckillService.getSeckillCount(seckillId);
			log.info("一共秒杀出{}件商品",seckillCount);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return Result.ok();
	}
	@ApiOperation(value="秒杀三(Redis分布式队列-订阅监听)",nickname="科帮网")
	@PostMapping("/startRedisQueue")
	public Result startRedisQueue(long seckillId){
		redisUtil.cacheValue(seckillId+"", null);//秒杀结束
		seckillService.deleteSeckill(seckillId);
		final long killId =  seckillId;
		log.info("开始秒杀三");
		for(int i=0;i<1000;i++){
			final long userId = i;
			Runnable task = new Runnable() {
				@Override
				public void run() {
					if(redisUtil.getValue(killId+"")==null){
						//思考如何返回给用户信息ws
						redisSender.sendChannelMess("seckill",killId+";"+userId);
					}else{
						//秒杀结束
					}
				}
			};
			executor.execute(task);
		}
		try {
			Thread.sleep(10000);
			redisUtil.cacheValue(killId+"", null);
			Long  seckillCount = seckillService.getSeckillCount(seckillId);
			log.info("一共秒杀出{}件商品",seckillCount);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return Result.ok();
	}
	@ApiOperation(value="秒杀五(ActiveMQ分布式队列)",nickname="科帮网")
	@PostMapping("/startActiveMQQueue")
	public Result startActiveMQQueue(long seckillId){
		seckillService.deleteSeckill(seckillId);
		final long killId =  seckillId;
		log.info("开始秒杀五");
		for(int i=0;i<1000;i++){
			final long userId = i;
			Runnable task = new Runnable() {
				@Override
				public void run() {
					if(redisUtil.getValue(killId+"")==null){
						// 创建生产信息
						Message message = new Message(RocketMqConfig.TOPIC, "testtag", (seckillId +";"+ userId).getBytes());
						// 发送
						try {
							SendResult sendResult = producer.getProducer().send(message);
						} catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else{
						//秒杀结束
					}
				}
			};
			executor.execute(task);
		}
		try {
			Thread.sleep(10000);
			redisUtil.cacheValue(killId+"", null);
			Long  seckillCount = seckillService.getSeckillCount(seckillId);
			log.info("一共秒杀出{}件商品",seckillCount);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return Result.ok();
	}
}
