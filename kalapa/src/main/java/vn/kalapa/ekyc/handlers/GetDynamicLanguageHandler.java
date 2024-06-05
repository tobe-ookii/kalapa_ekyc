package vn.kalapa.ekyc.handlers;

import android.content.Context;
import android.os.AsyncTask;

import vn.kalapa.ekyc.utils.Common;

public class GetDynamicLanguageHandler extends AsyncTask<String, Void, String> {
    Context c;

    public GetDynamicLanguageHandler(Context context) {
        this.c = context;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            if (!Common.Companion.isOnline(c)) return "-1";
            return Common.Companion.getDynamicLanguage(params[0],params[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

}
