package vn.kalapa.ekyc.handlers;

import android.content.Context;
import android.os.AsyncTask;

import vn.kalapa.ekyc.utils.Common;

public class GetModelPositionHandler extends AsyncTask<String, Void, String> {
    Context c;

    public GetModelPositionHandler(Context context) {
        this.c = context;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            if (!Common.Companion.isOnline(c)) return "";
            return Common.Companion.getModelPosition(params[0]);
        } catch (Exception e) {
            e.printStackTrace();
//            Toast.makeText(c, "Connect Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return "";
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

}
