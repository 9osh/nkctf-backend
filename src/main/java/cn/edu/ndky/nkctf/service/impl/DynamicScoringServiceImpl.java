package cn.edu.ndky.nkctf.service.impl;

import cn.edu.ndky.nkctf.entity.Challenge;
import cn.edu.ndky.nkctf.entity.Competition;
import cn.edu.ndky.nkctf.entity.Submission;
import cn.edu.ndky.nkctf.mapper.*;
import cn.edu.ndky.nkctf.service.DynamicScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 动态积分服务实现
 *
 * <p>使用 CTFd 风格的抛物线动态积分公式</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicScoringServiceImpl implements DynamicScoringService {

  /**
   * 一血奖励比例 (10%)
   */
  private static final double FIRST_BLOOD_BONUS_RATE = 0.10;

  /**
   * 二血奖励比例 (5%)
   */
  private static final double SECOND_BLOOD_BONUS_RATE = 0.05;

  /**
   * 三血奖励比例 (3%)
   */
  private static final double THIRD_BLOOD_BONUS_RATE = 0.03;

  /**
   * 题目级别锁前缀
   */
  private static final String CHALLENGE_LOCK_PREFIX = "nkctf:lock:challenge:";

  /**
   * 锁超时时间（秒）
   */
  private static final long LOCK_TIMEOUT_SECONDS = 30;

  private final SubmissionMapper submissionMapper;
  private final CompetitionMapper competitionMapper;
  private final CompetitionTeamMapper competitionTeamMapper;
  private final CompetitionUserMapper competitionUserMapper;
  private final CompetitionChallengeMapper competitionChallengeMapper;
  private final ChallengeMapper challengeMapper;
  private final StringRedisTemplate stringRedisTemplate;

  @Override
  public int calculateCurrentPoints(Challenge challenge, int solveCount) {
    if (!isDynamicScoring(challenge)) {
      return challenge.getPoints() != null ? challenge.getPoints() : 0;
    }

    int maxPoints = challenge.getMaxPoints() != null ? challenge.getMaxPoints() : 500;
    int minPoints = challenge.getMinPoints() != null ? challenge.getMinPoints() : 100;
    int decay = challenge.getDecay() != null ? challenge.getDecay() : 20;

    if (decay <= 0) {
      decay = 1;
    }

    // CTFd 公式: value = ((minPoints - maxPoints) / decay²) × solves² + maxPoints
    double value = ((double) (minPoints - maxPoints) / (decay * decay)) * (solveCount * solveCount)
        + maxPoints;

    // 确保不低于最小值，并向上取整
    int result = (int) Math.ceil(Math.max(minPoints, value));

    log.debug("动态积分计算: challenge={}, solves={}, maxPoints={}, minPoints={}, decay={}, result={}",
        challenge.getId(), solveCount, maxPoints, minPoints, decay, result);

    return result;
  }

  @Override
  public int calculateFirstBloodBonus(int basePoints, int solveRank) {
    double bonusRate = switch (solveRank) {
      case 1 -> FIRST_BLOOD_BONUS_RATE;
      case 2 -> SECOND_BLOOD_BONUS_RATE;
      case 3 -> THIRD_BLOOD_BONUS_RATE;
      default -> 0.0;
    };

    return (int) Math.ceil(basePoints * bonusRate);
  }

  @Override
  public int getNextSolveRank(Long competitionId, Long challengeId, boolean isTeamCompetition) {
    Integer currentSolves;
    if (isTeamCompetition) {
      currentSolves = submissionMapper.countCompetitionChallengeSolvesByTeam(
          competitionId, challengeId);
    } else {
      currentSolves = submissionMapper.countCompetitionChallengeSolvesByUser(
          competitionId, challengeId);
    }
    return (currentSolves != null ? currentSolves : 0) + 1;
  }

  @Override
  @Transactional
  public void recalculateChallengeScores(Long competitionId, Long challengeId,
      boolean isTeamCompetition) {
    // 获取题目级别的分布式锁
    String lockKey = CHALLENGE_LOCK_PREFIX + competitionId + ":" + challengeId;
    if (!tryLock(lockKey)) {
      log.warn("无法获取题目锁，跳过重新计算: competition={}, challenge={}", competitionId, challengeId);
      return;
    }

    try {
      doRecalculateChallengeScores(competitionId, challengeId, isTeamCompetition);
    } finally {
      unlock(lockKey);
    }
  }

  /**
   * 实际执行积分重新计算（在锁保护下）
   */
  private void doRecalculateChallengeScores(Long competitionId, Long challengeId,
      boolean isTeamCompetition) {
    log.info("开始重新计算竞赛 {} 中题目 {} 的积分", competitionId, challengeId);

    Challenge challenge = challengeMapper.selectById(challengeId);
    if (challenge == null) {
      log.warn("题目 {} 不存在，跳过重新计算", challengeId);
      return;
    }

    // 获取该题目的所有正确提交记录
    List<Submission> correctSubmissions = submissionMapper.getCorrectSubmissionsForChallenge(
        competitionId, challengeId);

    if (correctSubmissions.isEmpty()) {
      log.debug("竞赛 {} 中题目 {} 无正确提交，无需重新计算", competitionId, challengeId);
      return;
    }

    int totalSolves = correctSubmissions.size();

    // 根据总解题数计算当前分值
    int currentPoints = calculateCurrentPoints(challenge, totalSolves);

    log.debug("题目 {} 当前解题数: {}, 当前分值: {}", challengeId, totalSolves, currentPoints);

    // 重新计算每个提交的积分
    for (Submission submission : correctSubmissions) {
      // 保留原有的首杀排名（first_blood_rank），但奖励根据新基础分重新计算
      Integer preservedRank = submission.getFirstBloodRank();

      // 计算旧的总积分
      int oldBasePoints = submission.getPointsAwarded() != null ? submission.getPointsAwarded() : 0;
      int oldBonus = submission.getFirstBloodBonus() != null ? submission.getFirstBloodBonus() : 0;
      int oldTotal = oldBasePoints + oldBonus;

      // 根据保留的排名计算新奖励（奖励随基础分变化）
      int newBonus = 0;
      if (preservedRank != null && preservedRank >= 1 && preservedRank <= 3) {
        newBonus = calculateFirstBloodBonus(currentPoints, preservedRank);
      }

      int newTotal = currentPoints + newBonus;
      int pointsDiff = newTotal - oldTotal;

      // 更新提交记录：保持原有 rank，更新基础分和奖励
      submissionMapper.updatePointsAwarded(submission.getId(), currentPoints, preservedRank, newBonus);

      // 更新参赛者积分（只更新差值）
      if (pointsDiff != 0) {
        if (isTeamCompetition && submission.getTeamId() != null) {
          competitionTeamMapper.addTeamScore(competitionId, submission.getTeamId(), pointsDiff);
        } else if (submission.getUserId() != null) {
          competitionUserMapper.addUserScore(competitionId, submission.getUserId(), pointsDiff);
        }
      }

      log.debug("更新提交 {}: rank={}, basePoints={}, bonus={}, diff={}",
          submission.getId(), preservedRank, currentPoints, newBonus, pointsDiff);
    }

    log.info("竞赛 {} 中题目 {} 的积分重新计算完成，共 {} 条提交", competitionId, challengeId, totalSolves);
  }

  /**
   * 尝试获取分布式锁
   */
  private boolean tryLock(String key) {
    Boolean success = stringRedisTemplate.opsForValue()
        .setIfAbsent(key, "1", Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));
    return Boolean.TRUE.equals(success);
  }

  /**
   * 释放分布式锁
   */
  private void unlock(String key) {
    stringRedisTemplate.delete(key);
  }

  @Override
  @Transactional
  public void recalculateCompetitionScores(Long competitionId) {
    log.info("开始重新计算竞赛 {} 的所有积分", competitionId);

    Competition competition = competitionMapper.selectById(competitionId);
    if (competition == null) {
      log.warn("竞赛 {} 不存在，跳过重新计算", competitionId);
      return;
    }

    boolean isTeamCompetition = Boolean.TRUE.equals(competition.getIsTeamCompetition());

    // 重置所有参赛者的积分为 0
    if (isTeamCompetition) {
      competitionTeamMapper.resetAllScores(competitionId);
    } else {
      competitionUserMapper.resetAllScores(competitionId);
    }

    // 获取竞赛中的所有题目
    List<Long> challengeIds = competitionChallengeMapper.getCompetitionChallengeIds(competitionId);

    for (Long challengeId : challengeIds) {
      Challenge challenge = challengeMapper.selectById(challengeId);
      if (challenge == null) {
        continue;
      }

      // 获取该题目的所有正确提交记录
      List<Submission> correctSubmissions = submissionMapper.getCorrectSubmissionsForChallenge(
          competitionId, challengeId);

      if (correctSubmissions.isEmpty()) {
        continue;
      }

      int totalSolves = correctSubmissions.size();
      int currentPoints = calculateCurrentPoints(challenge, totalSolves);

      for (Submission submission : correctSubmissions) {
        // 保留原有的首杀排名（first_blood_rank），但奖励根据新基础分重新计算
        Integer preservedRank = submission.getFirstBloodRank();

        // 根据保留的排名计算新奖励（奖励随基础分变化）
        int bonus = 0;
        if (preservedRank != null && preservedRank >= 1 && preservedRank <= 3) {
          bonus = calculateFirstBloodBonus(currentPoints, preservedRank);
        }

        int totalPoints = currentPoints + bonus;

        // 更新提交记录：保持原有 rank，更新基础分和奖励
        submissionMapper.updatePointsAwarded(submission.getId(), currentPoints, preservedRank, bonus);

        // 累加积分到参赛者
        if (isTeamCompetition && submission.getTeamId() != null) {
          competitionTeamMapper.addTeamScore(competitionId, submission.getTeamId(), totalPoints);
        } else if (submission.getUserId() != null) {
          competitionUserMapper.addUserScore(competitionId, submission.getUserId(), totalPoints);
        }
      }
    }

    log.info("竞赛 {} 的积分重新计算完成", competitionId);
  }
}
