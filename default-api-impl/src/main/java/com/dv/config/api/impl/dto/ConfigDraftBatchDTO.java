package com.dv.config.api.impl.dto;

import com.dv.config.api.impl.entity.ConfigDraft;
import lombok.Data;

import java.util.List;

@Data
public class ConfigDraftBatchDTO {
    private List<ConfigDraft> drafts;
}
