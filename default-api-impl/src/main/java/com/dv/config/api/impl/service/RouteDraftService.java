package com.dv.config.api.impl.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dv.config.api.event.RoutePublishEvent;
import com.dv.config.api.impl.dto.DraftDiffVO;
import com.dv.config.api.impl.dto.RouteVO;
import com.dv.config.api.impl.entity.Route;
import com.dv.config.api.impl.entity.RouteDraft;
import com.dv.config.api.impl.entity.RouteHistory;
import com.dv.config.api.impl.mapper.RouteDraftMapper;
import com.dv.config.api.impl.mapper.RouteHistoryMapper;
import com.dv.config.api.impl.mapper.RouteMapper;
import com.dv.config.api.impl.security.UserProvider;
import com.dv.config.common.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RouteDraftService {

    @Resource
    private RouteMapper routeMapper;
    @Resource
    private RouteDraftMapper routeDraftMapper;
    @Resource
    private RouteHistoryMapper routeHistoryMapper;
    @Resource
    private UserProvider userProvider;
    @Resource
    private ApplicationEventPublisher eventPublisher;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String ROUTE_KEY = "com.dv.config.api.impl.gateway.RouteGatewayImpl_route";
    private static final int MAX_HISTORY_VERSIONS = 10;

    public void saveDraft(RouteDraft draft) {
        draft.setUpdateBy(userProvider.getUserId());
        draft.setUpdateTime(LocalDateTime.now());
        
        RouteDraft existing = routeDraftMapper.selectById(draft.getId());
        if (existing != null) {
            draft.setCreateTime(existing.getCreateTime());
            draft.setCreateBy(existing.getCreateBy());
            routeDraftMapper.updateById(draft);
        } else {
            draft.setCreateBy(userProvider.getUserId());
            draft.setCreateTime(LocalDateTime.now());
            routeDraftMapper.insert(draft);
        }
    }

    public List<RouteDraft> listDrafts() {
        return routeDraftMapper.selectList(Wrappers.lambdaQuery(RouteDraft.class)
                .orderByDesc(RouteDraft::getUpdateTime));
    }
    
    public List<DraftDiffVO> getDiffs() {
        List<RouteDraft> drafts = listDrafts();
        List<DraftDiffVO> diffs = new ArrayList<>();
        
        for (RouteDraft draft : drafts) {
            DraftDiffVO diff = new DraftDiffVO();
            diff.setKey(draft.getId());
            diff.setDiffType(draft.getOperationType());
            
            Route tempRoute = new Route();
            BeanUtils.copyProperties(draft, tempRoute);
            diff.setNewValue(JsonUtil.toJson(RouteVO.from(tempRoute)));
            
            if ("UPDATE".equals(draft.getOperationType()) || "DELETE".equals(draft.getOperationType())) {
                Route original = routeMapper.selectById(draft.getId());
                if (original != null) {
                    diff.setOldValue(JsonUtil.toJson(RouteVO.from(original)));
                }
            }
            
            diffs.add(diff);
        }
        return diffs;
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishAll() {
        List<RouteDraft> drafts = routeDraftMapper.selectList(null);
        if (drafts.isEmpty()) {
            return;
        }

        String version = "V" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String userId = userProvider.getUserId();

        for (RouteDraft draft : drafts) {
            Route route = new Route();
            BeanUtils.copyProperties(draft, route);
            route.setUpdateBy(userId);
            route.setUpdateTime(LocalDateTime.now());

            if ("ADD".equals(draft.getOperationType())) {
                route.setCreateBy(userId);
                route.setCreateTime(LocalDateTime.now());
                routeMapper.insert(route);
            } else if ("UPDATE".equals(draft.getOperationType())) {
                routeMapper.updateById(route);
            } else if ("DELETE".equals(draft.getOperationType())) {
                routeMapper.deleteById(draft.getId());
            }

            saveHistory(draft, version, userId);
            routeDraftMapper.deleteById(draft.getId());
        }
        
        trimHistoryVersions();

        stringRedisTemplate.delete(ROUTE_KEY);

        eventPublisher.publishEvent(new RoutePublishEvent(this));
    }

    private void saveHistory(RouteDraft draft, String version, String userId) {
        RouteHistory history = new RouteHistory();
        BeanUtils.copyProperties(draft, history);
        history.setRouteId(draft.getId());
        history.setVersion(version);
        history.setCreateBy(userId);
        history.setCreateTime(LocalDateTime.now());
        routeHistoryMapper.insert(history);
    }
    
    private void trimHistoryVersions() {
        // 修改为按 version 排序
        List<Object> versions = routeHistoryMapper.selectObjs(Wrappers.<RouteHistory>query()
                .select("DISTINCT version")
                .orderByDesc("version"));
        
        if (versions.size() > MAX_HISTORY_VERSIONS) {
            List<String> versionList = versions.stream().map(Object::toString).collect(Collectors.toList());
            List<String> keepVersions = versionList.subList(0, MAX_HISTORY_VERSIONS);
            
            routeHistoryMapper.delete(Wrappers.lambdaQuery(RouteHistory.class)
                    .notIn(RouteHistory::getVersion, keepVersions));
        }
    }
    
    public void discardDraft(String draftId) {
        routeDraftMapper.deleteById(draftId);
    }

    public List<RouteHistory> listHistory(String routeId) {
        return routeHistoryMapper.selectList(Wrappers.lambdaQuery(RouteHistory.class)
                .eq(RouteHistory::getRouteId, routeId)
                .orderByDesc(RouteHistory::getCreateTime));
    }
    
    public List<RouteHistory> listAllHistory() {
        return listAllHistory(null);
    }
    
    public List<RouteHistory> listAllHistory(String version) {
        return routeHistoryMapper.selectList(Wrappers.lambdaQuery(RouteHistory.class)
                .eq(version != null && !version.trim().isEmpty(), RouteHistory::getVersion, version != null ? version.trim() : null)
                .orderByDesc(RouteHistory::getCreateTime)
                .last("LIMIT 1000"));
    }

    public void rollback(Long historyId) {
        RouteHistory history = routeHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new RuntimeException("History not found");
        }
        createRollbackDraft(history);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void rollbackBatch(List<Long> historyIds) {
        if (historyIds == null || historyIds.isEmpty()) return;
        List<RouteHistory> histories = routeHistoryMapper.selectBatchIds(historyIds);
        for (RouteHistory history : histories) {
            createRollbackDraft(history);
        }
    }
    
    private void createRollbackDraft(RouteHistory history) {
        RouteDraft draft = new RouteDraft();
        BeanUtils.copyProperties(history, draft);
        draft.setId(history.getRouteId());
        
        Route current = routeMapper.selectById(history.getRouteId());
        if (current == null) {
            draft.setOperationType("ADD");
        } else {
            draft.setOperationType("UPDATE");
        }

        draft.setDescription("Rollback to version " + history.getVersion());
        saveDraft(draft);
    }
}
