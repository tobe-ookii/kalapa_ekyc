package vn.kalapa.demo.network.convert

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

public class JsonConvertFactory : Converter.Factory() {
    override fun responseBodyConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        return JsonResponseBodyConverter<Any>()
    }
}