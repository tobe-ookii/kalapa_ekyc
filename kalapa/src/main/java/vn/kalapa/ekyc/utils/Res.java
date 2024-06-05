package vn.kalapa.ekyc.utils;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;

public class Res extends Resources {
    private String mainColor;
    private String mainTextColor;
    private String background;

    public Res(Resources original, String mainColor, String mainTextColor, String background) {
        super(original.getAssets(), original.getDisplayMetrics(), original.getConfiguration());
        this.mainColor = mainColor;
        this.mainTextColor = mainTextColor;
        this.background = background;
    }

    @Override
    public int getColor(int id) throws NotFoundException {
        return getColor(id, null);
    }

    @Override
    public int getColor(int id, Theme theme) throws NotFoundException {
        switch (getResourceEntryName(id)) {
            case "mainColor":
                // You can change the return value to an instance field that loads from SharedPreferences.
                return Color.parseColor(mainColor); // used as an example. Change as needed.
            case "textColor":
                return Color.parseColor(mainTextColor); // used as an example. Change as needed.
            case "background":
                return Color.parseColor(background); // used as an example. Change as needed.
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return super.getColor(id, theme);
                }
                return super.getColor(id);
        }
    }

}
