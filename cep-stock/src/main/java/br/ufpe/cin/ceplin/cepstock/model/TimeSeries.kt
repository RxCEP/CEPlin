package br.ufpe.cin.ceplin.cepstock.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class TimeSeriesPoint(
        @SerializedName("datetime") val dateTime: Date,
        @SerializedName("open") val open: Double,
        @SerializedName("high") val high: Double,
        @SerializedName("low") val low: Double,
        @SerializedName("close") val close: Double,
        @SerializedName("volume") val volume: Long
)

data class TimeSeries(
        @SerializedName("symbol") val symbol: String,
        @SerializedName("time_series") val timeSeries: List<TimeSeriesPoint>
)
