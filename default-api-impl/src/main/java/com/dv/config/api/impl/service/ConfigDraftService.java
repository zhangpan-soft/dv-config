package com.dv.config.api.impl.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dv.config.api.event.ConfigPublishEvent;
import com.dv.config.api.impl.dto.DraftDiffVO;
import com.dv.config.api.impl.entity.Config;
import com.dv.config.api.impl.entity.ConfigDraft;
import com.dv.config.api.impl.entity.ConfigHistory;
import com.dv.config.api.impl.mapper.ConfigDraftMapper;
import com.dv.config.api.impl.mapper.ConfigHistoryMapper;
import com.dv.config.api.impl.mapper.ConfigMapper;
import com.dv.config.api.impl.security.UserProvider;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private static final String CONFIG_KEY_PATTERN = "com.dv.config.api.impl.gateway.ConfigGatewayImpl_config_%s";

    /**
     * 保存单个草稿
     */
    public void saveDraft(ConfigDraft draft) {
        draft.setUpdateBy(userProvider.getUserId());
        draft.setUpdateTime(LocalDateTime.now());
        
        // 检查是否存在相同 namespace + key 的草稿
        ConfigDraft existing = configDraftMapper.selectOne(Wrappers.lambdaQuery(ConfigDraft.class)
                .eq(ConfigDraft::getNamespace, draft.getNamespace())
                .eq(ConfigDraft::getKey, draft.getKey()));

        if (existing != null) {
            // 更新现有草稿
            draft.setId(existing.getId());
            draft.setCreateTime(existing.getCreateTime());
            draft.setCreateBy(existing.getCreateBy());
            configDraftMapper.updateById(draft);
        } else {
            // 新增草稿
            draft.setCreateBy(userProvider.getUserId());
            draft.setCreateTime(LocalDateTime.now());
            configDraftMapper.insert(draft);
        }
    }

    /**
     * 批量保存草稿
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveDrafts(List<ConfigDraft> drafts) {
        for (ConfigDraft draft : drafts) {
            // 简单的逻辑：如果是修改，先查出原配置ID
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
            saveDraft(draft);
        }
    }

    /**
     * 获取所有草稿
     */
    public List<ConfigDraft> listDrafts() {
        return configDraftMapper.selectList(Wrappers.lambdaQuery(ConfigDraft.class)
                .orderByDesc(ConfigDraft::getUpdateTime));
    }
    
    /**
     * 获取草稿差异
     */
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

    /**
     * 发布所有草稿
     */
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
                // 回填 config_id 到 draft，以便记录历史
                draft.setConfigId(config.getId());
            } else if ("UPDATE".equals(draft.getOperationType())) {
                config.setId(draft.getConfigId());
                configMapper.updateById(config);
            } else if ("DELETE".equals(draft.getOperationType())) {
                configMapper.deleteById(draft.getConfigId());
            }

            // 记录历史
            saveHistory(draft, version, userId);
            
            // 删除草稿
            configDraftMapper.deleteById(draft.getId());
        }

        // 清理缓存
        for (String namespace : affectedNamespaces) {
            stringRedisTemplate.delete(String.format(CONFIG_KEY_PATTERN, namespace));
        }

        // 发布事件，通知 Server 模块刷新
        eventPublisher.publishEvent(new ConfigPublishEvent(this, affectedNamespaces));
    }

    private void saveHistory(ConfigDraft draft, String version, String userId) {
        ConfigHistory history = new ConfigHistory();
        BeanUtils.copyProperties(draft, history);
        history.setConfigId(draft.getConfigId()); // 确保有 configId
        history.setVersion(version);
        history.setCreateBy(userId);
        history.setCreateTime(LocalDateTime.now());
        // historyId 自增，不需要设置
        configHistoryMapper.insert(history);
    }
    
    /**
     * 丢弃草稿
     */
    public void discardDraft(Long draftId) {
        configDraftMapper.deleteById(draftId);
    }

    /**
     * 获取指定配置的历史记录
     */
    public List<ConfigHistory> listHistory(Long configId) {
        return configHistoryMapper.selectList(Wrappers.lambdaQuery(ConfigHistory.class)
                .eq(ConfigHistory::getConfigId, configId)
                .orderByDesc(ConfigHistory::getCreateTime));
    }
    
    /**
     * 获取所有历史记录 (限制最近 1000 条)
     */
    public List<ConfigHistory> listAllHistory() {
        return configHistoryMapper.selectList(Wrappers.lambdaQuery(ConfigHistory.class)
                .orderByDesc(ConfigHistory::getCreateTime)
                .last("LIMIT 1000"));
    }

    /**
     * 回滚到指定历史版本
     */
    public void rollback(Long historyId) {
        ConfigHistory history = configHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new RuntimeException("History not found");
        }
        createRollbackDraft(history);
    }
    
    /**
     * 批量回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackBatch(List<Long> historyIds) {
        if (historyIds == null || historyIds.isEmpty()) return;
        List<ConfigHistory> histories = configHistoryMapper.selectBatchIds(historyIds);
        for (ConfigHistory history : histories) {
            createRollbackDraft(history);
        }
    }
    
    private void createRollbackDraft(ConfigHistory history) {
        ConfigDraft draft = new ConfigDraft();
        BeanUtils.copyProperties(history, draft);
        draft.setId(null); // 新草稿
        
        // 检查当前配置是否存在
        Config current = configMapper.selectById(history.getConfigId());
        if (current == null) {
            // 如果当前配置不存在（已被删除），则回滚操作为 ADD
            draft.setOperationType("ADD");
            // 确保 configId 为空，因为是新增
            draft.setConfigId(null); 
        } else {
            // 如果当前配置存在，则回滚操作为 UPDATE
            draft.setOperationType("UPDATE");
            draft.setConfigId(current.getId());
        }

        draft.setDescription("Rollback to version " + history.getVersion());
        saveDraft(draft);
    }
}
