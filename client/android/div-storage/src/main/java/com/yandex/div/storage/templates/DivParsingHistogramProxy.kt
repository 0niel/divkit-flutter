package com.yandex.div.storage.templates

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.yandex.div.core.annotations.Mockable
import com.yandex.div.json.ParsingEnvironment
import com.yandex.div.json.TemplateParsingEnvironment
import com.yandex.div.core.DivKit
import com.yandex.div.data.DivParsingEnvironment
import com.yandex.div.histogram.DivParsingHistogramReporter
import com.yandex.div2.DivData
import com.yandex.div2.DivTemplate
import org.json.JSONObject
import javax.inject.Inject

/**
 * Proxy that wraps [DivData.invoke] and [DivParsingEnvironment.parseTemplatesWithResult]
 * to measure and report histograms.
 */
@Mockable
internal class DivParsingHistogramProxy internal constructor(
        initReporter: () -> DivParsingHistogramReporter
) {

    @Inject
    constructor(context: Context) : this({ DivKit.getInstance(context).parsingHistogramReporter })

    private val reporter by lazy(initReporter)

    /**
     * Wraps [DivData.invoke].
     */
    fun createDivData(
            env: ParsingEnvironment,
            json: JSONObject,
            componentName: String?
    ): DivData = reporter.measureDataParsing(json, componentName) {
        DivData(env, json)
    }

    /**
     * Wraps [DivParsingEnvironment.parseTemplatesWithResultAndDependencies].
     */
    fun parseTemplatesWithResultsAndDependencies(
            env: DivParsingEnvironment,
            templates: JSONObject,
            componentName: String?
    ): TemplateParsingEnvironment.TemplateParsingResult<DivTemplate> {
        return reporter.measureTemplatesParsing(templates, componentName) {
            env.parseTemplatesWithResultAndDependencies(templates)
        }
    }
}
