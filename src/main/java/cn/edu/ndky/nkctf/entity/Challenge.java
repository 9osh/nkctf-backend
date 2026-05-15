package cn.edu.ndky.nkctf.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目实体
 */
@Data
@TableName("challenge")
public class Challenge {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String title;

  /**
   * 简短描述（列表展示）
   */
  private String description;

  /**
   * 详细内容（详情展示）
   */
  private String content;

  private String category;

  private String difficulty;

  private Integer points;

  /**
   * 计分类型: STATIC-固定分值, DYNAMIC-动态积分 (仅竞赛模式)
   */
  private String scoringType;

  /**
   * 动态积分最大值 (初始分值)
   */
  private Integer maxPoints;

  /**
   * 动态积分最小值 (下限)
   */
  private Integer minPoints;

  /**
   * 衰减参数 (达到最小值所需的解题数)
   */
  private Integer decay;

  /**
   * 出题人
   */
  private String author;

  private String flag;

  private Boolean isDynamic;

  private String dockerImage;

  /**
   * 动态容器对外服务端口；为空时使用平台配置 {@code docker.container.container-port}
   */
  private Integer dockerPort;

  /**
   * 附件下载链接
   */
  private String attachmentUrl;

  /**
   * 附件文件名
   */
  private String attachmentName;

  private Boolean enabled;

  @TableLogic
  private Integer deleted;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;
}
