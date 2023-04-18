package com.imtilab.bittracer.test.evidence

import com.imtilab.bittracer.model.EvidenceContext

interface EvidenceWriter {
    void write(EvidenceContext context)
}
