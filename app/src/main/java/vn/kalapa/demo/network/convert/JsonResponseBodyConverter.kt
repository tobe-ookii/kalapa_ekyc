package vn.kalapa.demo.network.convert


import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Converter
import java.io.BufferedReader

public class JsonResponseBodyConverter<T> : Converter<ResponseBody, T> {
    override fun convert(value: ResponseBody): T? {
        // Create a new buffered reader and String Builder
        val reader = BufferedReader(value.charStream())
        val stringBuilder = StringBuilder()

        // Check if the line we are reading is not null
        var inputLine: String?
        do {
            inputLine = reader.readLine()
            stringBuilder.append(inputLine)
        } while (inputLine != null)

        reader.close()
        value.close()

        val obj = JSONObject(stringBuilder.toString())
        return obj as T
    }
}