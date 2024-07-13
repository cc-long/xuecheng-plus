package com.xuecheng.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author CCL
 * @since 2024-07-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mq_message")
@ApiModel(value="MqMessage对象", description="")
public class MqMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "消息id")
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    @ApiModelProperty(value = "消息类型代码")
    private String messageType;

    @ApiModelProperty(value = "关联业务信息")
    private String businessKey1;

    @ApiModelProperty(value = "关联业务信息")
    private String businessKey2;

    @ApiModelProperty(value = "关联业务信息")
    private String businessKey3;

    @ApiModelProperty(value = "消息队列主机")
    private String mqHost;

    @ApiModelProperty(value = "消息队列端口")
    private Integer mqPort;

    @ApiModelProperty(value = "消息队列虚拟主机")
    private String mqVirtualhost;

    @ApiModelProperty(value = "队列名称")
    private String mqQueue;

    @ApiModelProperty(value = "通知次数")
    private Integer informNum;

    @ApiModelProperty(value = "处理状态，0:初始，1:成功")
    private String state;

    @ApiModelProperty(value = "回复失败时间")
    private LocalDateTime returnfailureDate;

    @ApiModelProperty(value = "回复成功时间")
    private LocalDateTime returnsuccessDate;

    @ApiModelProperty(value = "回复失败内容")
    private String returnfailureMsg;

    @ApiModelProperty(value = "最近通知时间")
    private LocalDateTime informDate;

    @ApiModelProperty(value = "阶段1处理状态, 0:初始，1:成功")
    private String stageState1;

    @ApiModelProperty(value = "阶段2处理状态, 0:初始，1:成功")
    private String stageState2;

    @ApiModelProperty(value = "阶段3处理状态, 0:初始，1:成功")
    private String stageState3;

    @ApiModelProperty(value = "阶段4处理状态, 0:初始，1:成功")
    private String stageState4;


}
