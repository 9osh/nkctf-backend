package cn.edu.ndky.nkctf.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建题目请求 DTO
 */
@Data
public class CreateChallengeRequest {

  @NotBlank(message = "标题不能为空")
  @Size(max = 200, message = "标题长度不能超过 200 字符")
  private String title;

  @Size(max = 500, message = "简述长度不能超过 500 字符")
  private String description;

  private String content;

  @NotBlank(message = "分类不能为空")
  private String category;

  @NotBlank(message = "难度不能为空")
  private String difficulty;

  @NotNull(message = "分数不能为空")
  @Min(value = 1, message = "分数最小为 1")
  private Integer points;

  @Size(max = 100, message = "作者长度不能超过 100 字符")
  private String author;

  /**
   * 静态 flag（isDynamic=false 时必填）
   */
  private String flag;

  /**
   * 是否为动态容器题目（默认 false）
   */
  private Boolean isDynamic;

  /**
   * Docker 镜像名（isDynamic=true 时必填）
   */
  private String dockerImage;

  /**
   * 是否启用（默认 false）
   */
  private Boolean enabled;

  // ========== 动态积分配置（仅竞赛模式） ==========

  /**
   * 计分类型: STATIC (默认), DYNAMIC (仅竞赛模式)
   */
  private String scoringType;

  /**
   * 动态积分最大值 (初始分值) - 竞赛模式使用
   */
  @Min(value = 1, message = "maxPoints 最小为 1")
  private Integer maxPoints;

  /**
   * 动态积分最小值 (下限) - 竞赛模式使用
   */
  @Min(value = 1, message = "minPoints 最小为 1")
  private Integer minPoints;

  /**
   * 衰减参数 (达到最小值所需的解题数)
   */
  @Min(value = 1, message = "decay 最小为 1")
  private Integer decay;
}
