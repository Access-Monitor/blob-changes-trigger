package cloudcomputing.accessmonitor.service;

import com.google.gson.Gson;

public class JsonParserService {

  public static <T> T fromJson(String json, Class<T> toParse) {
    return new Gson().fromJson(json, toParse);
  }

  public static <T> String toJson(T toParse) {
    return new Gson().toJson(toParse, toParse.getClass());
  }

}
