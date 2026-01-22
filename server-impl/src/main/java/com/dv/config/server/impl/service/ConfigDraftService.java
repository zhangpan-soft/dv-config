package com.dv.config.server.impl.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dv.config.server.event.ConfigPublishEvent;
import com.dv.config.server.impl.dto.DraftDiffVO;
import com.dv.config.server.impl.entity.Config;
import com.dv.config.server.impl.entity.ConfigDraft;
import com.dv.config.server.impl.entity.ConfigHistory;
import com.dv.config.server.impl.mapper.ConfigDraftMapper;
import com.dv.config.server.impl.mapper.ConfigHistoryMapper;
import com.dv.config.server.impl.mapper.ConfigMapper;
import com.dv.config.server.impl.security.UserProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConfigDraftService {

    @Resource
    private ConfigMapper configMapper;
    @Resource
    private ConfigDraftMapper configDraftMapper;
    @Resource
    private ConfigHistoryMapper configHistoryMapper;
    @Resource
    private UserProvider userProvider;
    @Resource
    private ApplicationEventPublisher eventPublisher;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String CONFIG_KEY_PATTERN = "com.dv.config.server.impl.gateway.ConfigGatewayImpl_config_%s";
    private static final int MAX_HISTORY_VERSIONS = 10;

    public void saveDraft(ConfigDraft draft) {
        draft.setUpdateBy(userProvider.getUserId());
        draft.setUpdateTime(LocalDateTime.now());
        
        ConfigDraft existing = configDraftMapper.selectOne(Wrappers.lambdaQuery(ConfigDraft.class)
                .eq(ConfigDraft::getNamespace, draft.getNamespace())
                .eq(ConfigDraft::getKey, draft.getKey()));

        if (existing != null) {
            draft.setId(existing.getId());
            draft.setCreateTime(existing.getCreateTime());
            draft.setCreateBy(existing.getCreateBy());
            configDraftMapper.updateById(draft);
        } else {
            draft.setCreateBy(userProvider.getUserId());
            draft.setCreateTime(LocalDateTime.now());
            configDraftMapper.insert(draft);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDrafts(List<ConfigDraft> drafts) {
        Set<String> namespaces = drafts.stream().map(ConfigDraft::getNamespace).collect(Collectors.toSet());
        Set<String> keys = drafts.stream().map(ConfigDraft::getKey).collect(Collectors.toSet());
        List<Config> configs = configMapper.selectList(Wrappers.lambdaQuery(Config.class).in(Config::getNamespace, namespaces).eq(Config::getKey, keys));
        Map<String, Config> collect = configs.stream().collect(Collectors.toMap(e -> e.getNamespace() + "_" + e.getKey(), e -> e));

        for (ConfigDraft draft : drafts) {
            if (draft.getConfigId()==null){
                Config exist = collect.get(draft.getNamespace() + "_" + draft.getKey());
                if (exist != null) {
                    draft.setConfigId(exist.getId());
                }
            }
            saveDraft(draft);
        }
    }

    public List<ConfigDraft> listDrafts() {
        return configDraftMapper.selectList(Wrappers.lambdaQuery(ConfigDraft.class)
                .orderByDesc(ConfigDraft::getUpdateTime));
    }
    
    public List<DraftDiffVO> getDiffs() {
        List<ConfigDraft> drafts = listDrafts();
        List<DraftDiffVO> diffs = new ArrayList<>();
        
        for (ConfigDraft draft : drafts) {
            DraftDiffVO diff = new DraftDiffVO();
            diff.setKey(draft.getKey());
            diff.setNamespace(draft.getNamespace());
            diff.setDiffType(draft.getOperationType());
            diff.setNewValue(draft.getValue());
            
            if ("UPDATE".equals(draft.getOperationType()) || "DELETE".equals(draft.getOperationType())) {
                Config original = null;
                if (draft.getConfigId() != null) {
                    original = configMapper.selectById(draft.getConfigId());
                } else {
                    original = configMapper.selectOne(Wrappers.lambdaQuery(Config.class)
                            .eq(Config::getNamespace, draft.getNamespace())
                            .eq(Config::getKey, draft.getKey()));
                }
                
                if (original != null) {
                    diff.setOldValue(original.getValue());
                }
            }
            
            diffs.add(diff);
        }
        return diffs;
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishAll() {
        List<ConfigDraft> drafts = configDraftMapper.selectList(null);
        if (drafts.isEmpty()) {
            return;
        }

        String version = "V" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String userId = userProvider.getUserId();
        Set<String> affectedNamespaces = new HashSet<>();

        for (ConfigDraft draft : drafts) {
            affectedNamespaces.add(draft.getNamespace());
            
            Config config = new Config();
            BeanUtils.copyProperties(draft, config);
            config.setUpdateBy(userId);
            config.setUpdateTime(LocalDateTime.now());

            if ("ADD".equals(draft.getOperationType())) {
                config.setCreateBy(userId);
                config.setCreateTime(LocalDateTime.now());
                configMapper.insert(config);
                draft.setConfigId(config.getId());
            } else if ("UPDATE".equals(draft.getOperationType())) {
                config.setId(draft.getConfigId());
                configMapper.updateById(config);
            } else if ("DELETE".equals(draft.getOperationType())) {
                configMapper.deleteById(draft.getConfigId());
            }

            saveHistory(draft, version, userId);
            configDraftMapper.deleteById(draft.getId());
        }
        
        trimHistoryVersions();

        for (String namespace : affectedNamespaces) {
            stringRedisTemplate.delete(String.format(CONFIG_KEY_PATTERN, namespace));
        }

        eventPublisher.publishEvent(new ConfigPublishEvent(this, affectedNamespaces));
    }

    private void saveHistory(ConfigDraft draft, String version, String userId) {
        ConfigHistory history = new ConfigHistory();
        BeanUtils.copyProperties(draft, history);
        history.setConfigId(draft.getConfigId());
        history.setVersion(version);
        history.setCreateBy(userId);
        history.setCreateTime(LocalDateTime.now());
        configHistoryMapper.insert(history);
    }
    
    private void trimHistoryVersions() {
        List<Object> versions = configHistoryMapper.selectObjs(Wrappers.<ConfigHistory>query()
                .select("DISTINCT version")
                .orderByDesc("version"));
        
        if (versions.size() > MAX_HISTORY_VERSIONS) {
            List<String> versionList = versions.stream().map(Object::toString).collect(Collectors.toList());
            List<String> keepVersions = versionList.subList(0, MAX_HISTORY_VERSIONS);
            
            configHistoryMapper.delete(Wrappers.lambdaQuery(ConfigHistory.class)
                    .notIn(ConfigHistory::getVersion, keepVersions));
        }
    }
    
    public void discardDraft(Long draftId) {
        configDraftMapper.deleteById(draftId);
    }

    public List<ConfigHistory> listHistory(Long configId) {
        return configHistoryMapper.selectList(Wrappers.lambdaQuery(ConfigHistory.class)
                .eq(ConfigHistory::getConfigId, configId)
                .orderByDesc(ConfigHistory::getCreateTime));
    }
    
    public List<ConfigHistory> listAllHistory() {
        return listAllHistory(null);
    }
    
    public List<ConfigHistory> listAllHistory(String version) {
        return configHistoryMapper.selectList(Wrappers.lambdaQuery(ConfigHistory.class)
                .eq(version != null && !version.trim().isEmpty(), ConfigHistory::getVersion, version != null ? version.trim() : null)
                .orderByDesc(ConfigHistory::getCreateTime)
                .last("LIMIT 1000"));
    }

    public void rollback(Long historyId) {
        ConfigHistory history = configHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new RuntimeException("History not found");
        }
        createRollbackDraft(history);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void rollbackBatch(List<Long> historyIds) {
        if (historyIds == null || historyIds.isEmpty()) return;
        List<ConfigHistory> histories = configHistoryMapper.selectByIds(historyIds);
        for (ConfigHistory history : histories) {
            createRollbackDraft(history);
        }
    }
    
    private void createRollbackDraft(ConfigHistory history) {
        ConfigDraft draft = new ConfigDraft();
        BeanUtils.copyProperties(history, draft);
        draft.setId(null);
        
        Config current = configMapper.selectById(history.getConfigId());
        if (current == null) {
            draft.setOperationType("ADD");
            draft.setConfigId(null); 
        } else {
            draft.setOperationType("UPDATE");
            draft.setConfigId(current.getId());
        }
        
        draft.setDescription("Rollback to version " + history.getVersion());
        saveDraft(draft);
    }
}
