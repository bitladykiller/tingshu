package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.DescribeMediaInfosRequest;
import com.tencentcloudapi.vod.v20180717.models.DescribeMediaInfosResponse;
import com.tencentcloudapi.vod.v20180717.models.MediaInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Override
    public Map<String,Object>uploadTrack(MultipartFile file)
    {

        String tempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(),
                        file);
        VodUploadClient client =
                new VodUploadClient(vodConstantProperties.getSecretId(),
                        vodConstantProperties.getSecretKey());
        VodUploadRequest request = new VodUploadRequest();
        request.setMediaFilePath(tempPath);
        request.setProcedure(vodConstantProperties.getProcedure());
        try
        {
            VodUploadResponse response = client.upload(vodConstantProperties.getRegion(),
                    request);
            HashMap<String, Object> map = new HashMap<>();
            map.put("mediaFileId", response.getFileId());
            map.put("mediaUrl", response.getMediaUrl());
            return map;
        } catch (Exception e)
        {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

    @SneakyThrows
    @Override
    public TrackMediaInfoVo getmediaInfoByFileId(String mediaFileId)
    {
        Credential cred = new Credential(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
        // 实例化要请求产品的client对象,clientProfile是可选的
        VodClient client = new VodClient(cred, vodConstantProperties.getRegion());
        // 实例化一个请求对象,每个接口都会对应一个request对象
        DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
        //  设置当前fileIds
        req.setFileIds(new String[]{mediaFileId});
        // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
        DescribeMediaInfosResponse response = client.DescribeMediaInfos(req);
        log.info("声音详细返回结果：{}", JSON.toJSONString(response));
        //  判断对象不为空
        if (response.getMediaInfoSet().length > 0)
        {
            //  获取到
            MediaInfo mediaInfo = response.getMediaInfoSet()[0];
            //  创建流媒体信息对象
            TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
            trackMediaInfoVo.setDuration(mediaInfo.getMetaData().getDuration());
            trackMediaInfoVo.setSize(mediaInfo.getMetaData().getSize());
            trackMediaInfoVo.setMediaUrl(mediaInfo.getBasicInfo().getMediaUrl());
            trackMediaInfoVo.setType(mediaInfo.getBasicInfo().getType());
            //  返回数据
            return trackMediaInfoVo;
        }
        return null;
    }
}
