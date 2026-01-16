package com.dv.config.api.impl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dv.config.api.impl.dto.ConfigDraftBatchDTO;
import com.dv.config.api.impl.dto.DraftDiffVO;
import com.dv.config.api.impl.dto.RouteDraftDTO;
import com.dv.config.api.impl.dto.RouteVO;
import com.dv.config.api.impl.entity.*;
import com.dv.config.api.impl.mapper.ConfigMapper;
import com.dv.config.api.impl.mapper.RouteMapper;
import com.dv.config.api.impl.service.ConfigDraftService;
import com.dv.config.api.impl.service.RouteDraftService;
import com.dv.config.common.JsonUtil;
import com.dv.config.common.crypto.CryptoProperties;
import com.dv.config.common.crypto.CryptoUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private ConfigMapper configMapper;
    @Resource
    private RouteMapper routeMapper;
    @Resource
    private ConfigDraftService configDraftService;
    @Resource
    private RouteDraftService routeDraftService;
    @Resource
    private CryptoProperties cryptoProperties;

    // ================== Config ==================

    @GetMapping("/config")
    public String configList(@RequestParam(required = false) String keyword, Model model) {
        LambdaQueryWrapper<Config> query = Wrappers.lambdaQuery(Config.class);
        if (StringUtils.hasText(keyword)) {
            query.and(q -> q.like(Config::getKey, keyword)
                    .or().like(Config::getNamespace, keyword)
                    .or().like(Config::getValue, keyword)
                    .or().like(Config::getDescription, keyword));
        }
        List<Config> configs = configMapper.selectList(query);
        model.addAttribute("configs", configs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeMenu", "config");
        return "config/list";
    }

    @GetMapping("/config/drafts")
    public String configDrafts(Model model) {
        List<ConfigDraft> drafts = configDraftService.listDrafts();
        model.addAttribute("drafts", drafts);
        model.addAttribute("activeMenu", "config-drafts");
        return "config/drafts";
    }
    
    @GetMapping("/config/diff")
    public String configDiff(Model model) {
        List<DraftDiffVO> diffs = configDraftService.getDiffs();
        model.addAttribute("diffs", diffs);
        model.addAttribute("activeMenu", "config-drafts"); // 归属到 Drafts 菜单
        return "config/diff";
    }
    
    @GetMapping("/config/history")
    public String configHistoryList(Model model) {
        List<ConfigHistory> historyList = configDraftService.listAllHistory();
        model.addAttribute("historyList", historyList);
        model.addAttribute("activeMenu", "config-history");
        return "config/history";
    }

    @PostMapping("/config/save")
    public String saveConfigDraft(ConfigDraft draft, RedirectAttributes redirectAttributes) {
        saveConfigDraftLogic(draft);
        redirectAttributes.addFlashAttribute("message", "Draft saved successfully!");
        return "redirect:/admin/config";
    }
    
    @PostMapping("/config/save/api")
    @ResponseBody
    public ResponseEntity<?> saveConfigDraftApi(ConfigDraft draft) {
        saveConfigDraftLogic(draft);
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    private void saveConfigDraftLogic(ConfigDraft draft) {
        // 自动判断是否加密
        if (CryptoUtil.isEncrypted(draft.getValue())) {
            draft.setEncrypted(true);
        }
        
        if ("UPDATE".equals(draft.getOperationType()) || "DELETE".equals(draft.getOperationType())) {
            if (draft.getConfigId() == null) {
                Config exist = configMapper.selectOne(Wrappers.lambdaQuery(Config.class)
                        .eq(Config::getNamespace, draft.getNamespace())
                        .eq(Config::getKey, draft.getKey()));
                if (exist != null) {
                    draft.setConfigId(exist.getId());
                }
            }
        }
        configDraftService.saveDraft(draft);
    }
    
    @PostMapping("/config/saveBatch")
    public String saveConfigDraftBatch(ConfigDraftBatchDTO batchDTO, RedirectAttributes redirectAttributes) {
        saveConfigDraftBatchLogic(batchDTO);
        redirectAttributes.addFlashAttribute("message", "Batch drafts saved successfully!");
        return "redirect:/admin/config";
    }
    
    @PostMapping("/config/saveBatch/api")
    @ResponseBody
    public ResponseEntity<?> saveConfigDraftBatchApi(ConfigDraftBatchDTO batchDTO) {
        saveConfigDraftBatchLogic(batchDTO);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private void saveConfigDraftBatchLogic(ConfigDraftBatchDTO batchDTO) {
        if (batchDTO.getDrafts() != null && !batchDTO.getDrafts().isEmpty()) {
            List<ConfigDraft> validDrafts = batchDTO.getDrafts().stream()
                    .filter(d -> d.getKey() != null && !d.getKey().isEmpty())
                    .peek(d -> {
                        if (CryptoUtil.isEncrypted(d.getValue())) {
                            d.setEncrypted(true);
                        }
                    })
                    .toList();
            configDraftService.saveDrafts(validDrafts);
        }
    }
    
    @PostMapping("/config/encrypt")
    @ResponseBody
    public ResponseEntity<?> encryptConfig(@RequestParam String value) {
        if (!StringUtils.hasText(value)) {
            return ResponseEntity.badRequest().body("Value cannot be empty");
        }
        try {
            String encrypted = CryptoUtil.encrypt(value, cryptoProperties.getMasterKey(), cryptoProperties.getIterations());
            return ResponseEntity.ok(Map.of("encrypted", encrypted));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Encryption failed: " + e.getMessage());
        }
    }

    @PostMapping("/config/publish")
    public String publishConfig(RedirectAttributes redirectAttributes) {
        configDraftService.publishAll();
        redirectAttributes.addFlashAttribute("message", "All drafts published successfully!");
        return "redirect:/admin/config";
    }
    
    @GetMapping("/config/discard/{id}")
    public String discardConfigDraft(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        configDraftService.discardDraft(id);
        redirectAttributes.addFlashAttribute("message", "Draft discarded.");
        return "redirect:/admin/config/drafts";
    }
    
    @GetMapping("/config/history/{configId}")
    @ResponseBody
    public List<ConfigHistory> configHistory(@PathVariable Long configId) {
        return configDraftService.listHistory(configId);
    }
    
    @PostMapping("/config/rollback/{historyId}")
    public String rollbackConfig(@PathVariable Long historyId, RedirectAttributes redirectAttributes) {
        configDraftService.rollback(historyId);
        redirectAttributes.addFlashAttribute("message", "Rollback draft created!");
        return "redirect:/admin/config";
    }
    
    @PostMapping("/config/rollbackBatch")
    @ResponseBody
    public ResponseEntity<?> rollbackConfigBatch(@RequestParam("historyIds") List<Long> historyIds) {
        configDraftService.rollbackBatch(historyIds);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ================== Route ==================

    @GetMapping("/route")
    public String routeList(@RequestParam(required = false) String keyword, Model model) {
        LambdaQueryWrapper<Route> query = Wrappers.lambdaQuery(Route.class);
        if (StringUtils.hasText(keyword)) {
            query.and(q -> q.like(Route::getId, keyword)
                    .or().like(Route::getUri, keyword)
                    .or().like(Route::getDescription, keyword));
        }
        List<Route> routes = routeMapper.selectList(query);
        List<RouteVO> routeVOs = routes.stream().map(RouteVO::from).collect(Collectors.toList());
        model.addAttribute("routes", routeVOs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeMenu", "route");
        return "route/list";
    }
    
    @GetMapping("/route/drafts")
    public String routeDrafts(Model model) {
        List<RouteDraft> drafts = routeDraftService.listDrafts();
        model.addAttribute("drafts", drafts);
        model.addAttribute("activeMenu", "route-drafts");
        return "route/drafts";
    }
    
    @GetMapping("/route/diff")
    public String routeDiff(Model model) {
        List<DraftDiffVO> diffs = routeDraftService.getDiffs();
        model.addAttribute("diffs", diffs);
        model.addAttribute("activeMenu", "route-drafts");
        return "route/diff";
    }
    
    @GetMapping("/route/history")
    public String routeHistoryList(Model model) {
        List<RouteHistory> historyList = routeDraftService.listAllHistory();
        model.addAttribute("historyList", historyList);
        model.addAttribute("activeMenu", "route-history");
        return "route/history";
    }

    @PostMapping("/route/save")
    public String saveRouteDraft(RouteDraftDTO dto, RedirectAttributes redirectAttributes) {
        saveRouteDraftLogic(dto);
        redirectAttributes.addFlashAttribute("message", "Route draft saved successfully!");
        return "redirect:/admin/route";
    }
    
    @PostMapping("/route/save/api")
    @ResponseBody
    public ResponseEntity<?> saveRouteDraftApi(RouteDraftDTO dto) {
        saveRouteDraftLogic(dto);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private void saveRouteDraftLogic(RouteDraftDTO dto) {
        RouteDraft draft = new RouteDraft();
        BeanUtils.copyProperties(dto, draft);
        
        if (StringUtils.hasText(dto.getPredicatesJson())) {
            try {
                draft.setPredicates(JsonUtil.fromJson(dto.getPredicatesJson(), new TypeReference<>() {}));
            } catch (Exception e) {
            }
        }
        if (StringUtils.hasText(dto.getFiltersJson())) {
            try {
                draft.setFilters(JsonUtil.fromJson(dto.getFiltersJson(), new TypeReference<>() {}));
            } catch (Exception e) {
            }
        }
        if (StringUtils.hasText(dto.getMetadataJson())) {
            try {
                draft.setMetadata(JsonUtil.fromJson(dto.getMetadataJson(), new TypeReference<>() {}));
            } catch (Exception e) {
            }
        }

        routeDraftService.saveDraft(draft);
    }

    @PostMapping("/route/publish")
    public String publishRoute(RedirectAttributes redirectAttributes) {
        routeDraftService.publishAll();
        redirectAttributes.addFlashAttribute("message", "All route drafts published successfully!");
        return "redirect:/admin/route";
    }
    
    @GetMapping("/route/discard/{id}")
    public String discardRouteDraft(@PathVariable String id, RedirectAttributes redirectAttributes) {
        routeDraftService.discardDraft(id);
        redirectAttributes.addFlashAttribute("message", "Route draft discarded.");
        return "redirect:/admin/route/drafts";
    }
    
    @GetMapping("/route/history/{routeId}")
    @ResponseBody
    public List<RouteHistory> routeHistory(@PathVariable String routeId) {
        return routeDraftService.listHistory(routeId);
    }
    
    @PostMapping("/route/rollback/{historyId}")
    public String rollbackRoute(@PathVariable Long historyId, RedirectAttributes redirectAttributes) {
        routeDraftService.rollback(historyId);
        redirectAttributes.addFlashAttribute("message", "Rollback draft created!");
        return "redirect:/admin/route";
    }
    
    @PostMapping("/route/rollbackBatch")
    @ResponseBody
    public ResponseEntity<?> rollbackRouteBatch(@RequestParam("historyIds") List<Long> historyIds) {
        routeDraftService.rollbackBatch(historyIds);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
