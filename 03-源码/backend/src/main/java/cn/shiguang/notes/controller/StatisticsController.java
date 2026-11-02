package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.application.StatisticsApplicationService;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** Statistics HTTP boundary. */
@RestController
@RequestMapping("/api/v1")
public class StatisticsController extends ControllerSupport {

  private final StatisticsApplicationService use;

  public StatisticsController(StatisticsApplicationService use) {
    this.use = use;
  }

  @GetMapping("/statistics/me")
  public ApiResponse<DashboardVO> authorStats(
      @RequestParam(defaultValue = "7") int days, HttpServletRequest r) {
    return ApiResponse.ok(use.dashboard(actor(r), "AUTHOR", days));
  }

  @GetMapping("/admin/statistics/platform")
  public ApiResponse<DashboardVO> platformStats(
      @RequestParam(defaultValue = "7") int days, HttpServletRequest r) {
    return ApiResponse.ok(use.dashboard(actor(r), "PLATFORM", days));
  }

  @GetMapping("/admin/statistics/governance")
  public ApiResponse<DashboardVO> governanceStats(
      @RequestParam(defaultValue = "7") int days, HttpServletRequest r) {
    return ApiResponse.ok(use.dashboard(actor(r), "GOVERNANCE", days));
  }

  @GetMapping("/statistics/me/ranking")
  public ApiResponse<PageVO<RankVO>> authorRank(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.ranking(actor(r), false, cursor, size));
  }

  @GetMapping("/admin/statistics/ranking")
  public ApiResponse<PageVO<RankVO>> platformRank(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.ranking(actor(r), true, cursor, size));
  }
}
