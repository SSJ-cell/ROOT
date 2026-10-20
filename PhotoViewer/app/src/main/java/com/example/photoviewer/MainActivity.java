package com.example.photoviewer; // ★ 본인 프로젝트 패키지명 확인 ★

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback; // 추가
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView textView;

    String site_url = "http://127.0.0.1:8000/";
    JSONObject post_json;
    String imageUrl = null;

    CloadImage taskDownload;

    // 뒤로 가기 시간 계산용 변수
    private long backPressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);

        // [핵심] 최신 안드로이드 OS 대응 뒤로가기 콜백 등록
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (System.currentTimeMillis() > backPressedTime + 2000) {
                    backPressedTime = System.currentTimeMillis();
                    Toast.makeText(MainActivity.this, "'뒤로' 버튼을 한 번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
                } else {
                    finish(); // 앱 완전 종료
                }
            }
        });
    }

    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download 시작", Toast.LENGTH_SHORT).show();
    }

    public void onClickUpload(View v) {
        PutPost taskUpload = new PutPost();
        taskUpload.execute(site_url + "api_root/Post/");
        Toast.makeText(getApplicationContext(), "Upload 시도 중...", Toast.LENGTH_SHORT).show();
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {
        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            List<Bitmap> bitmapList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();

                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);

                    for (int i = 0; i < aryJson.length(); i++) {
                        post_json = (JSONObject) aryJson.get(i);

                        imageUrl = post_json.optString("image", "null");

                        if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
                            imageUrl = imageUrl.replace("127.0.0.1", "127.0.0.1");
                            imageUrl = imageUrl.replace("10.255.168.64", "127.0.0.1");
                            imageUrl = imageUrl.replace("localhost", "127.0.0.1");

                            URL myImageUrl = new URL(imageUrl);
                            HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();

                            InputStream imgStream = imgConn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);

                            if (imageBitmap != null) {
                                bitmapList.add(imageBitmap);
                            }
                            imgStream.close();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return bitmapList;
        }

        @Override
        protected void onPostExecute(List<Bitmap> images) {
            if (images.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText("이미지 로드 성공! (총 " + images.size() + "장)");

                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                ImageAdapter adapter = new ImageAdapter(images);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private class PutPost extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                String jsonInputString = "{\"author\": 1, \"title\": \"안드로이드 업로드\", \"text\": \"앱에서 작성된 테스트 글입니다.\"}";

                java.io.OutputStream os = conn.getOutputStream();
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    return "업로드 성공!";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "업로드 실패";
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            onClickDownload(null);
        }
    }
}