package net.prestalife.inlang.intentions

import com.intellij.javascript.debugger.LOG
import com.intellij.lang.javascript.modules.JSImportPlaceInfo
import com.intellij.lang.javascript.modules.imports.JSImportCandidatesBase
import com.intellij.lang.javascript.modules.imports.providers.JSCandidatesProcessor
import com.intellij.lang.javascript.modules.imports.providers.JSImportCandidatesProvider


class InlangImportCandidatesProvider(private val placeInfo: JSImportPlaceInfo) : JSImportCandidatesBase(placeInfo) {

    override fun processCandidates(ref: String, processor: JSCandidatesProcessor) {
        LOG.info("Processing candidates for $ref")
    }

    companion object : JSImportCandidatesProvider.CandidatesFactory {

        override fun createProvider(placeInfo: JSImportPlaceInfo): JSImportCandidatesProvider =
            InlangImportCandidatesProvider(placeInfo)

    }
}