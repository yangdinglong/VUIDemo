package com.roobo.vuidemo;


import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by chengyijun on 2018/9/12.
 */

public class AIResultParser {
    private static final String TAG = AIResultParser.class.getSimpleName();

    public static String parserContextFromAIResultJSON(String resultJson) {
        if (resultJson != null) {
            try {
                JSONObject jsonObject = new JSONObject(resultJson);
                JSONObject aiJsonObject = jsonObject.optJSONObject("ai");
                JSONObject semanticJsonObject = aiJsonObject.optJSONObject("semantic");
                JSONObject contextJsonObject = semanticJsonObject.optJSONObject("outputContext");
                JSONArray array = new JSONArray();
                array.put(contextJsonObject);
                return array.toString();
            } catch (JSONException e) {
                Log.e(TAG, "parser error!");
            }
        }
        return "";
    }

    public static String parserQueryFromAIResultJSON(String resultJson) {
        String query = "";
        if (resultJson != null) {
            try {
                JSONObject jsonObject = new JSONObject(resultJson);
                JSONObject aiJsonObject = jsonObject.optJSONObject("ai");
                if (aiJsonObject != null) {
                    query = aiJsonObject.optString("query");

                }

            } catch (JSONException e) {
                Log.e(TAG, "parser error!");
            }
        }
        return query;
    }

    public static String parserHintFromAIResultJSON(String resultJson) {
        String hint = "";
        if (resultJson != null) {
            try {
                JSONObject jsonObject = new JSONObject(resultJson);
                JSONObject aiJsonObject = jsonObject.optJSONObject("ai");
                if (aiJsonObject != null) {
                    JSONArray resultArray = aiJsonObject.optJSONArray("results");
                    if (resultArray != null && resultArray.getJSONObject(0) != null) {
                        hint = resultArray.getJSONObject(0).optString("hint");
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, "parser error!");
            }
        }
        return hint;
    }

    public static String parserMP3UrlFromAIResultJSON(String resultJson) {
        String audioUrl = "";
        if (resultJson != null) {
            try {
                JSONObject jsonObject = new JSONObject(resultJson);
                JSONObject aiJsonObject = jsonObject.optJSONObject("ai");
                if (aiJsonObject != null) {
                    JSONArray resultArray = aiJsonObject.optJSONArray("results");
                    if (resultArray != null && resultArray.getJSONObject(0) != null) {
                        JSONObject dataJsonObject = resultArray.getJSONObject(0).optJSONObject("data");
                        if (dataJsonObject != null) {
                            audioUrl = dataJsonObject.optString("audio");
                        }
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, "parser error!");
            }
        }
        return audioUrl;
    }

    public static boolean isExitPlayer(String resultJson) {
        if (!TextUtils.isEmpty(resultJson)) {
            try {
                JSONObject jsonObject = new JSONObject(resultJson);
                JSONObject aiJsonObject = jsonObject.optJSONObject("ai");
                if (aiJsonObject != null) {
                    JSONObject semanticJsonObject = aiJsonObject.optJSONObject("semantic");
                    if (semanticJsonObject != null) {
                        if ("Exit".equals(semanticJsonObject.optString("action"))) {
                            return true;
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "parser error!");
            }
        }
        return false;
    }

    public static boolean isStartPlayer(String resultJson) {
        if (!TextUtils.isEmpty(resultJson)) {
            try {
                JSONObject jsonObject = new JSONObject(resultJson);
                JSONObject aiJsonObject = jsonObject.optJSONObject("ai");
                if (aiJsonObject != null) {
                    JSONObject semanticJsonObject = aiJsonObject.optJSONObject("semantic");
                    if (semanticJsonObject != null) {
                        String action = semanticJsonObject.getString("action");
                        if ("Play".equals(action)||"GetPoetryByTitle".equals(action)) {
                            return true;
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "parser error!");
            }
        }
        return false;
    }
    public static boolean isStartPlayer2(String resultJson) {
        if (!TextUtils.isEmpty(resultJson)) {
            try {
                JSONObject jsonObject = new JSONObject(resultJson);
                JSONObject aiJsonObject = jsonObject.optJSONObject("ai");
                if (aiJsonObject != null) {
                    JSONObject semanticJsonObject = aiJsonObject.optJSONObject("semantic");
                    if (semanticJsonObject != null) {
                        String action = semanticJsonObject.getString("action");
                        if ("GetPoetryByTitle".equals(action)) {
                            return true;
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "parser error!");
            }
        }
        return false;
    }
}
