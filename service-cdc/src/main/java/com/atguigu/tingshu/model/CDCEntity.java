package com.atguigu.tingshu.model;

import lombok.Data;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

import javax.persistence.Column;

@Data
public class CDCEntity
{
    @Column(name = "id")
    private Long id;
}