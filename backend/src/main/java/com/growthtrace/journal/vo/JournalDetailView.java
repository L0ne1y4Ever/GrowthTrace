package com.growthtrace.journal.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JournalDetailView {

    private JournalView journal;

    /** 抽取记录；尚未抽取时为 null。 */
    private ExtractionView extraction;
}
