package vn.kalapa.ekyc.handlers;

import android.os.AsyncTask;

import vn.kalapa.ekyc.utils.Common;

public class GetAllLanguageHandler extends AsyncTask<String, Void, String> {

    public GetAllLanguageHandler() {
    }

    @Override
    protected String doInBackground(String... params) {
        try {
//            if (!Common.Companion.isOnline(c)) return "-1";
            return Common.Companion.getDynamicLanguage(params[0], null);
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
