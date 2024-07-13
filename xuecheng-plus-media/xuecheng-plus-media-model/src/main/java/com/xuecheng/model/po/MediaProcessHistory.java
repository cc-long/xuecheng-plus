package com.xuecheng.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
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
@TableName("media_process_history")
@ApiModel(value="MediaProcessHistory对象", description="")
public class MediaProcessHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "文件标识")
    private String fileId;

    @ApiModelProperty(value = "文件名称")
    private String filename;

    @ApiModelProperty(value = "存储源")
    private String bucket;

    @ApiModelProperty(value = "状态,1:未处理，2：处理成功  3处理失败")
    private String status;

    @ApiModelProperty(value = "上传时间")
    private LocalDateTime createDate;

    @ApiModelProperty(value = "完成时间")
    private LocalDateTime finishDate;

    @ApiModelProperty(value = "媒资文件访问地址")
    private String url;

    @ApiModelProperty(value = "失败次数")
    private Integer failCount;

    @ApiModelProperty(value = "文件路径")
    private String filePath;

    @ApiModelProperty(value = "失败原因")
    private String errormsg;


}
