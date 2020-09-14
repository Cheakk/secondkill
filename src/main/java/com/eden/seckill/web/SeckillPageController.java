package com.eden.seckill.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.eden.seckill.common.entity.Result;
import com.eden.seckill.common.entity.Seckill;
import com.eden.seckill.common.utils.HttpClient;
import com.eden.seckill.common.utils.IPUtils;
import com.eden.seckill.queue.activemq.Producer;
import com.eden.seckill.queue.activemq.RocketMqConfig;
import com.eden.seckill.service.ISeckillService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "秒杀商品")
@RestController
@RequestMapping("/seckillPage")
public class SeckillPageController {
	
	@Autowired
	private ISeckillService seckillService;
	
	@Autowired
	private Producer producer;
	
	@Autowired
	private HttpClient httpClient;
	@Value("${qq.captcha.url}")
	private String url;
	@Value("${qq.captcha.aid}")
	private String aid;
	@Value("${qq.captcha.AppSecretKey}")
	private String appSecretKey;
	
	
	@ApiOperation(value = "秒杀商品列表", nickname = "小柒2012")
	@PostMapping("/list")
	public Result list() {
		//返回JSON数据、前端VUE迭代即可
		List<Seckill>  List = seckillService.getSeckillList();
		return Result.ok(List);
	}
	
	@RequestMapping("/startSeckill")
    public Result  startSeckill(String ticket,String randstr,HttpServletRequest request,@RequestParam ("userid")int userid) throws Exception{
        HttpMethod method =HttpMethod.POST;
        MultiValueMap<String, String> params= new LinkedMultiValueMap<String, String>();
        params.add("aid", aid);
        params.add("AppSecretKey", appSecretKey);
        params.add("Ticket", ticket);
        params.add("Randstr", randstr);
        params.add("UserIP", IPUtils.getIpAddr(request));
//        String msg = httpClient.client(url,method,params);
        String msg = "{\"response\":\"1\",\"evil_level\":\"0\",\"err_msg\":\"OK\"}";
        /**
         * response: 1:验证成功，0:验证失败，100:AppSecretKey参数校验错误[required]
         * evil_level:[0,100]，恶意等级[optional]
         * err_msg:验证错误信息[optional]
         */
        //{"response":"1","evil_level":"0","err_msg":"OK"}
        JSONObject json = JSONObject.parseObject(msg);
        String response = (String) json.get("response");
        if("1".equals(response)){
        	// 创建生产信息
			Message message = new Message(RocketMqConfig.TOPIC, "testtag", (1000 +";"+ userid).getBytes());
			// 发送
			SendResult sendResult = producer.getProducer().send(message);
        	return Result.ok();
        }else{
        	return Result.error("验证失败");
        }
    }
}
