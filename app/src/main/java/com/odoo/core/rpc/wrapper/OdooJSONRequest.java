package com.odoo.core.rpc.wrapper;

import android.util.Log;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;

public class OdooJSONRequest extends com.android.volley.toolbox.JsonObjectRequest{
    public static final String TAG = OdooJSONRequest.class.getName();

    private static final String SET_COOKIE_KEY = "Set-Cookie";
    private static final String COOKIE_KEY = "Cookie";
    private static final String SESSION_COOKIE = "session_id";
    private static final String RESULT_KEY = "result";

    public OdooJSONRequest(String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(url, jsonRequest, listener, errorListener);
    }

    /* (non-Javadoc)
     * @see com.android.volley.toolbox.StringRequest#parseNetworkResponse(com.android.volley.NetworkResponse)
     */
    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        Response<JSONObject> JSONResponse = super.parseNetworkResponse(response);

        Map<String, String> headers = response.headers;
        if (headers.containsKey(SET_COOKIE_KEY)
                && headers.get(SET_COOKIE_KEY).startsWith(SESSION_COOKIE)) {
            JSONObject result = new JSONObject();
            try {
                result = JSONResponse.result.getJSONObject(RESULT_KEY);
                String cookie = headers.get(SET_COOKIE_KEY);
                if (cookie.length() > 0) {
                    String[] splitCookie = cookie.split(";");
                    String[] splitSessionId = splitCookie[0].split("=");
                    result.put(SESSION_COOKIE, splitSessionId[1]);
                }
            } catch (JSONException e){
                Log.e(TAG, result.toString());
            }
        }
        Log.i(TAG, JSONResponse.result.toString());
        return JSONResponse;
    }

}
