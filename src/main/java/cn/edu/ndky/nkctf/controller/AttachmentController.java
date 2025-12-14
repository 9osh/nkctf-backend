package cn.edu.ndky.nkctf.controller;

import cn.edu.ndky.nkctf.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 附件下载控制器
 */
@Tag(name = "附件", description = "附件下载接口")
@Slf4j
@RestController
@RequestMapping("/attachments")
public class AttachmentController {

  /**
   * 附件存储路径，支持 classpath: 前缀或绝对路径
   */
  @Value("${attachment.storage-path:classpath:static/attachments}")
  private String storagePath;

  @Operation(summary = "下载附件", description = "根据文件路径下载附件")
  @GetMapping("/download")
  public ResponseEntity<Resource> downloadAttachment(
      @Parameter(description = "文件路径，如 crypto/base64_challenge.txt")
      @RequestParam String path) {

    // 路径安全检查：防止路径遍历攻击
    if (path == null || path.isBlank()) {
      throw new BusinessException(400, "文件路径不能为空");
    }

    // 规范化路径并检查是否包含路径遍历
    String normalizedPath = path.replace("\\", "/");
    if (normalizedPath.contains("..") || normalizedPath.startsWith("/")) {
      log.warn("检测到非法文件路径访问尝试: {}", path);
      throw new BusinessException(400, "非法文件路径");
    }

    try {
      Resource resource = loadResource(normalizedPath);

      if (!resource.exists() || !resource.isReadable()) {
        throw new BusinessException(404, "文件不存在");
      }

      // 获取文件名
      String filename = Paths.get(normalizedPath).getFileName().toString();

      // 确定 Content-Type
      MediaType mediaType = determineMediaType(filename);

      // 构建响应
      return ResponseEntity.ok()
          .contentType(mediaType)
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + encodeFilename(filename) + "\"")
          .body(resource);

    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("文件下载失败: {}", path, e);
      throw new BusinessException(500, "文件下载失败");
    }
  }

  /**
   * 加载资源文件
   */
  private Resource loadResource(String relativePath) throws IOException {
    if (storagePath.startsWith("classpath:")) {
      // 从 classpath 加载
      String basePath = storagePath.substring("classpath:".length());
      String fullPath = basePath + "/" + relativePath;
      return new ClassPathResource(fullPath);
    } else {
      // 从文件系统加载
      Path basePath = Paths.get(storagePath).toAbsolutePath().normalize();
      Path filePath = basePath.resolve(relativePath).normalize();

      // 确保文件在存储目录内（防止路径遍历）
      if (!filePath.startsWith(basePath)) {
        log.warn("检测到路径遍历攻击尝试: {}", relativePath);
        throw new BusinessException(400, "非法文件路径");
      }

      return new FileSystemResource(filePath);
    }
  }

  /**
   * 根据文件扩展名确定 Content-Type
   */
  private MediaType determineMediaType(String filename) {
    String lowerName = filename.toLowerCase();
    if (lowerName.endsWith(".txt")) {
      return MediaType.TEXT_PLAIN;
    } else if (lowerName.endsWith(".zip")) {
      return MediaType.APPLICATION_OCTET_STREAM;
    } else if (lowerName.endsWith(".pdf")) {
      return MediaType.APPLICATION_PDF;
    } else if (lowerName.endsWith(".png")) {
      return MediaType.IMAGE_PNG;
    } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
      return MediaType.IMAGE_JPEG;
    } else if (lowerName.endsWith(".py")) {
      return MediaType.TEXT_PLAIN;
    } else if (lowerName.endsWith(".c") || lowerName.endsWith(".h")) {
      return MediaType.TEXT_PLAIN;
    } else {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  /**
   * URL 编码文件名（处理中文文件名）
   */
  private String encodeFilename(String filename) {
    return URLEncoder.encode(filename, StandardCharsets.UTF_8)
        .replace("+", "%20");
  }
}
