package com.dv.config.server.impl.dto;

import com.dv.config.server.impl.entity.ConfigDraft;
import lombok.Data;

import java.util.List;

@Data
public class ConfigDraftBatchDTO {
    private List<ConfigDraft> drafts;
}
