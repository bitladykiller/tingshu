package com.atguigu.tingshu.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService
{

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitService rabbitService;

    @Override
    public BigDecimal getTrackBreakSecond(Long userId,
                                          Long trackId)
    {
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query,
                UserListenProcess.class,
                MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS,
                        userId));
        if (null != userListenProcess)
        {
            return userListenProcess.getBreakSecond();
        }
        return new BigDecimal("0");
    }

    @Override
    public void updateListenProcess(Long userId,
                                    UserListenProcessVo userListenProcessVo)
    {
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
        UserListenProcess userListenProcess = this.mongoTemplate.findOne(query,
                UserListenProcess.class,
                MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS,
                        userId));
        if (null != userListenProcess)
        {
            userListenProcess.setUpdateTime(new Date());
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            mongoTemplate.save(userListenProcess,
                    MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS,
                            userId));
        } else
        {
            userListenProcess = new UserListenProcess();
            BeanUtils.copyProperties(userListenProcessVo,
                    userListenProcess);
            userListenProcess.setId(ObjectId.get().toString());
            userListenProcess.setUserId(userId);
            userListenProcess.setIsShow(1);
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(new Date());
            mongoTemplate.save(userListenProcess,
                    MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS,
                            userId));
        }
        String key = "user:track:" + userId;
        Boolean isExist = redisTemplate.opsForValue().getBit(key,
                userListenProcessVo.getTrackId());
        if (!isExist)
        {
            redisTemplate.opsForValue().setBit(key,
                    userListenProcessVo.getTrackId(),
                    true);

            redisTemplate.expire(key,
                    24 * 60 * 60,
                    TimeUnit.SECONDS);

            TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
            trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-",
                    ""));
            trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
            trackStatMqVo.setTrackId(userListenProcessVo.getTrackId());
            trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
            trackStatMqVo.setCount(1);
            rabbitService.sendMessage(MqConst.EXCHANGE_TRACK,
                    MqConst.ROUTING_TRACK_STAT_UPDATE,
                    JSON.toJSONString(trackStatMqVo));
        }
    }

    @Override
    public Map<String, Object> getLatelyTrack(Long userId)
    {
        Query query = Query.query(Criteria.where("userId").is(userId));
        Sort sort = Sort.by(Sort.Direction.DESC,
                "updateTime");
        query.with(sort);
        UserListenProcess userListenProcess = mongoTemplate.findOne(query,
                UserListenProcess.class);
        if (null == userListenProcess)
            return null;
        Map<String, Object> map = new HashMap<>();
        map.put("albumId",
                userListenProcess.getAlbumId());
        map.put("trackId",
                userListenProcess.getTrackId());
        return map;
    }
}
