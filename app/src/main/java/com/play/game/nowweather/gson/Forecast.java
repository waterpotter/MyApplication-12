package com.play.game.nowweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Administrator on 2017/7/21.
 */

public class Forecast {

    public String date;

    @SerializedName("tmp")
    public Temperature temperature;
    @SerializedName("cond")
    public More more;

    public class Temperature{

        public String max;
        public String min;
    }

    public class More{
        @SerializedName("txt_id")
        public String info;
    }
}
