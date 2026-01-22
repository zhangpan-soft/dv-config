package com.dv.config.server.impl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dv.config.api.crypto.CryptoProperties;
import com.dv.config.api.crypto.CryptoUtil;
import com.dv.config.api.dto.IResponse;
import com.dv.config.api.json.JsonUtil;
import com.dv.config.server.impl.convertor.RouteConvertor;
import com.dv.config.server.impl.dto.ConfigDraftBatchDTO;
import com.dv.config.server.impl.dto.DraftDiffVO;
import com.dv.config.server.impl.dto.RouteDraftDTO;
import com.dv.config.server.impl.dto.RouteVO;
import com.dv.config.server.impl.entity.*;
import com.dv.config.server.impl.mapper.ConfigMapper;
import com.dv.config.server.impl.mapper.RouteMapper;
import com.dv.config.server.impl.service.ConfigDraftService;
import com.dv.config.server.impl.service.RouteDraftService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
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

    @Value("${config.admin.ui.public-path:}")
    private String publicPath;

    // SPA 入口
    @GetMapping({"", "/", "/config/**", "/route/**"})
    public String index(Model model) {
        model.addAttribute("publicPath", publicPath);
        return "admin/index";
    }

    // ================== Config API ==================

    @GetMapping("/api/config/list")
    @ResponseBody
    public IResponse<IPage<Config>> configListApi(
            @RequestParam(required = false, name = "keyword") String keyword,
            @RequestParam(defaultValue = "1", name = "page") Integer page,
            @RequestParam(defaultValue = "10", name = "size") Integer size) {
        
        LambdaQueryWrapper<Config> query = Wrappers.lambdaQuery(Config.class);
        if (StringUtils.hasText(keyword)) {
            query.and(q -> q.like(Config::getKey, keyword)
                    .or().like(Config::getNamespace, keyword)
                    .or().like(Config::getValue, keyword)
                    .or().like(Config::getDescription, keyword));
        }
        query.orderByAsc(Config::getId);
        
        // 手动拼接 LIMIT 语句实现分页
        query.last("LIMIT " + (page - 1) * size + ", " + size);
        
        List<Config> records = configMapper.selectList(query);
        
        // 构造 Page 对象返回，虽然没有 total，但前端只需要 records
        Page<Config> resultPage = new Page<>(page, size);
        resultPage.setRecords(records);
        resultPage.setSearchCount(false); // 明确标记未进行 count 查询
        
        return IResponse.ok(resultPage);
    }

    @GetMapping("/api/config/drafts")
    @ResponseBody
    public IResponse<List<ConfigDraft>> configDraftsApi() {
        return IResponse.ok(configDraftService.listDrafts());
    }
    
    @GetMapping("/api/config/diff")
    @ResponseBody
    public IResponse<List<DraftDiffVO>> configDiffApi() {
        return IResponse.ok(configDraftService.getDiffs());
    }
    
    @GetMapping("/api/config/history")
    @ResponseBody
    public IResponse<List<ConfigHistory>> configHistoryListApi() {
        return IResponse.ok(configDraftService.listAllHistory());
    }

    @PostMapping("/api/config/save")
    @ResponseBody
    public IResponse<?> saveConfigDraftApi(@RequestBody ConfigDraft draft) {
        try {
            saveConfigDraftLogic(draft);
            return IResponse.ok();
        } catch (Exception e) {
            log.error("Failed to save config draft", e);
            return IResponse.fail(500, e.getMessage());
        }
    }
    
    private void saveConfigDraftLogic(ConfigDraft draft) {
        draft.setEncrypted(CryptoUtil.isEncrypted(draft.getValue()));
        if (draft.getConfigId() == null) {
            Config exist = configMapper.selectOne(Wrappers.lambdaQuery(Config.class)
                    .eq(Config::getNamespace, draft.getNamespace())
                    .eq(Config::getKey, draft.getKey()));
            if (exist != null) {
                draft.setConfigId(exist.getId());
            }
        }
        configDraftService.saveDraft(draft);
    }
    
    @PostMapping("/api/config/saveBatch")
    @ResponseBody
    public IResponse<?> saveConfigDraftBatchApi(@RequestBody ConfigDraftBatchDTO batchDTO) {
        try {
            saveConfigDraftBatchLogic(batchDTO);
            return IResponse.ok();
        } catch (Exception e) {
            log.error("Failed to save config draft batch", e);
            return IResponse.fail(500, e.getMessage());
        }
    }

    private void saveConfigDraftBatchLogic(ConfigDraftBatchDTO batchDTO) {
        if (batchDTO.getDrafts() != null && !batchDTO.getDrafts().isEmpty()) {
            List<ConfigDraft> validDrafts = batchDTO.getDrafts().stream()
                    .filter(d -> d.getKey() != null && !d.getKey().isEmpty())
                    .peek(d -> d.setEncrypted(CryptoUtil.isEncrypted(d.getValue())))
                    .toList();
            configDraftService.saveDrafts(validDrafts);
        }
    }
    
    @PostMapping("/api/config/encrypt")
    @ResponseBody
    public IResponse<?> encryptConfig(@RequestParam(name = "value") String value) {
        if (!StringUtils.hasText(value)) {
            return IResponse.fail(400, "Value cannot be empty");
        }
        try {
            String encrypted = CryptoUtil.encrypt(value, cryptoProperties.getMasterKey(), cryptoProperties.getIterations());
            return IResponse.ok(Map.of("encrypted", encrypted));
        } catch (Exception e) {
            return IResponse.fail(500, "Encryption failed: " + e.getMessage());
        }
    }

    @PostMapping("/api/config/publish")
    @ResponseBody
    public IResponse<?> publishConfig() {
        configDraftService.publishAll();
        return IResponse.ok();
    }
    
    @GetMapping("/api/config/discard/{id}")
    @ResponseBody
    public IResponse<?> discardConfigDraft(@PathVariable(name = "id") Long id) {
        configDraftService.discardDraft(id);
        return IResponse.ok();
    }
    
    @GetMapping("/api/config/history/{configId}")
    @ResponseBody
    public IResponse<List<ConfigHistory>> configHistory(@PathVariable(name = "configId") Long configId) {
        return IResponse.ok(configDraftService.listHistory(configId));
    }
    
    @PostMapping("/api/config/rollback/{historyId}")
    @ResponseBody
    public IResponse<?> rollbackConfig(@PathVariable(name = "historyId") Long historyId) {
        configDraftService.rollback(historyId);
        return IResponse.ok();
    }
    
    @PostMapping("/api/config/rollbackBatch")
    @ResponseBody
    public IResponse<?> rollbackConfigBatch(@RequestBody List<Long> historyIds) {
        configDraftService.rollbackBatch(historyIds);
        return IResponse.ok();
    }

    // ================== Route API ==================

    @GetMapping("/api/route/list")
    @ResponseBody
    public IResponse<IPage<RouteVO>> routeListApi(
            @RequestParam(required = false, name = "keyword") String keyword,
            @RequestParam(defaultValue = "1", name = "page") Integer page,
            @RequestParam(defaultValue = "10", name = "size") Integer size) {
        
        LambdaQueryWrapper<Route> query = Wrappers.lambdaQuery(Route.class);
        if (StringUtils.hasText(keyword)) {
            query.and(q -> q.like(Route::getId, keyword)
                    .or().like(Route::getUri, keyword)
                    .or().like(Route::getDescription, keyword));
        }
        query.orderByAsc(Route::getOrderNum);
        
        // 手动拼接 LIMIT 语句实现分页
        query.last("LIMIT " + (page - 1) * size + ", " + size);
        
        List<Route> records = routeMapper.selectList(query);
        
        // Convert to VO
        List<RouteVO> voList = records.stream()
                .map(RouteConvertor.INSTANCE::toVo)
                .toList();
                
        // 构造 Page 对象返回
        Page<RouteVO> voPage = new Page<>(page, size);
        voPage.setRecords(voList);
        voPage.setSearchCount(false);
        
        return IResponse.ok(voPage);
    }

    @GetMapping("/api/route/drafts")
    @ResponseBody
    public IResponse<List<RouteDraft>> routeDraftsApi() {
        return IResponse.ok(routeDraftService.listDrafts());
    }
    
    @GetMapping("/api/route/diff")
    @ResponseBody
    public IResponse<List<DraftDiffVO>> routeDiffApi() {
        return IResponse.ok(routeDraftService.getDiffs());
    }

    @GetMapping("/api/route/history")
    @ResponseBody
    public IResponse<List<RouteHistory>> routeHistoryListApi() {
        return IResponse.ok(routeDraftService.listAllHistory());
    }

    @PostMapping("/api/route/save")
    @ResponseBody
    public IResponse<?> saveRouteDraftApi(@RequestBody RouteDraftDTO dto) {
        try {
            saveRouteDraftLogic(dto);
            return IResponse.ok();
        } catch (Exception e) {
            log.error("Failed to save route draft", e);
            return IResponse.fail(500, e.getMessage());
        }
    }

    private void saveRouteDraftLogic(RouteDraftDTO dto) {
        RouteDraft draft = new RouteDraft();
        BeanUtils.copyProperties(dto, draft);
        
        // 显式解析 JSON，并处理异常
        if (StringUtils.hasText(dto.getPredicatesJson())) {
            try {
                draft.setPredicates(JsonUtil.fromJson(dto.getPredicatesJson(), new TypeReference<>() {}));
            } catch (Exception e) {
                log.error("Invalid Predicates JSON: {}", dto.getPredicatesJson(), e);
                throw new IllegalArgumentException("Invalid Predicates JSON format");
            }
        }
        if (StringUtils.hasText(dto.getFiltersJson())) {
            try {
                draft.setFilters(JsonUtil.fromJson(dto.getFiltersJson(), new TypeReference<>() {}));
            } catch (Exception e) {
                log.error("Invalid Filters JSON: {}", dto.getFiltersJson(), e);
                throw new IllegalArgumentException("Invalid Filters JSON format");
            }
        }
        if (StringUtils.hasText(dto.getMetadataJson())) {
            try {
                draft.setMetadata(JsonUtil.fromJson(dto.getMetadataJson(), new TypeReference<>() {}));
            } catch (Exception e) {
                log.error("Invalid Metadata JSON: {}", dto.getMetadataJson(), e);
                throw new IllegalArgumentException("Invalid Metadata JSON format");
            }
        }

        routeDraftService.saveDraft(draft);
    }

    @PostMapping("/api/route/publish")
    @ResponseBody
    public IResponse<?> publishRoute() {
        routeDraftService.publishAll();
        return IResponse.ok();
    }
    
    @GetMapping("/api/route/discard/{id}")
    @ResponseBody
    public IResponse<?> discardRouteDraft(@PathVariable(name = "id") String id) {
        routeDraftService.discardDraft(id);
        return IResponse.ok();
    }
    
    @GetMapping("/api/route/history/{routeId}")
    @ResponseBody
    public IResponse<List<RouteHistory>> routeHistory(@PathVariable(name = "routeId") String routeId) {
        return IResponse.ok(routeDraftService.listHistory(routeId));
    }
    
    @PostMapping("/api/route/rollback/{historyId}")
    @ResponseBody
    public IResponse<?> rollbackRoute(@PathVariable(name = "historyId") Long historyId) {
        routeDraftService.rollback(historyId);
        return IResponse.ok();
    }
    
    @PostMapping("/api/route/rollbackBatch")
    @ResponseBody
    public IResponse<?> rollbackRouteBatch(@RequestBody List<Long> historyIds) {
        routeDraftService.rollbackBatch(historyIds);
        return IResponse.ok();
    }
}
