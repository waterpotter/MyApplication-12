package com.play.game.nowweather.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.play.game.nowweather.R;
import com.play.game.nowweather.db.City;
import com.play.game.nowweather.db.Country;
import com.play.game.nowweather.db.Province;
import com.play.game.nowweather.util.HttpUtil;
import com.play.game.nowweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2017/7/21.
 */

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTRY = 2;
    private TextView titleText;
    private Button backbutton;
    private ListView listview;

    private ArrayAdapter<String> adapter;

    private List<String> datalist = new ArrayList<>();

    private List<Province> provinceList;
    private List<City> cityList;
    private List<Country> counList;

    private Province selectedProvince;
    private City selectedCity;

    private int currentLevel;
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backbutton = (Button) view.findViewById(R.id.back_button);
        listview = (ListView) view.findViewById(R.id.list_view);


        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, datalist);
        listview.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(i);
                    quaryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(i);
                    quaryCounties();
                }else if(currentLevel==LEVEL_COUNTRY){
                    String weatherId = counList.get(i).getWeatherId();

                    if(getActivity() instanceof MainActivity){

                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id",weatherId);
                    startActivity(intent);
                    getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity activity=(WeatherActivity)getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swiperefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);

                    }

                }
            }
        });

        backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel == LEVEL_COUNTRY) {
                    quaryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });

        queryProvinces();
    }

    private void queryProvinces() {
        titleText.setText("中国");
        backbutton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            datalist.clear();
            for (Province province : provinceList) {
                    datalist.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listview.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            String address="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }



    private void quaryCounties() {
        titleText.setText(selectedCity.getCityName());
        backbutton.setVisibility(View.VISIBLE);
        counList=DataSupport.where("cityid=?",String.valueOf(selectedCity.getId())).find(Country.class);
        if(counList.size()>0){
            datalist.clear();
            for (Country country:counList) {
                datalist.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listview.setSelection(0);
            currentLevel=LEVEL_COUNTRY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"country");
        }

    }

    private void quaryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backbutton.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceid=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            datalist.clear();
            for (City city:cityList) {
                datalist.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listview.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }


    }
    private void queryFromServer(String address,final String type) {

        showProgressDialog();

        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result=false;
                if("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result=Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if("country".equals(type)){
                    result=Utility.handleCountryResponse(responseText,selectedCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                quaryCities();
                            }else if("country".equals(type)){
                                quaryCounties();
                            }
                        }
                    });
                }


            }
        });

    }

    private void closeProgressDialog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if(progressDialog==null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
}
